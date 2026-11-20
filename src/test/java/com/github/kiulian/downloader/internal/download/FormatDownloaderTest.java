package com.github.kiulian.downloader.internal.download;

import com.github.kiulian.downloader.YoutubeDownloader;
import com.github.kiulian.downloader.YoutubeException.DownloadException;
import com.github.kiulian.downloader.download.DownloadOptions;
import com.github.kiulian.downloader.download.DownloadProgress;
import com.github.kiulian.downloader.http.UrlConnectionTransport;
import com.github.kiulian.downloader.internal.http.Http;
import com.github.kiulian.downloader.internal.http.RetryPolicy;
import com.github.kiulian.downloader.model.videos.formats.Format;
import com.github.kiulian.downloader.testing.TestHttpServer;
import com.sun.net.httpserver.HttpExchange;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.IntPredicate;

import static com.github.kiulian.downloader.testing.TestHttpServer.respond;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormatDownloaderTest {

    private static final byte[] MEDIA = new byte[3500];

    static {
        for (int i = 0; i < MEDIA.length; i++) {
            MEDIA[i] = (byte) (i * 31 + i / 7);
        }
    }

    @TempDir
    Path dir;

    private TestHttpServer server;
    private final List<String> ranges = new CopyOnWriteArrayList<>();
    private final List<String> userAgents = new CopyOnWriteArrayList<>();
    /** Request number (1-based) -> cut the connection after 300 bytes. */
    private volatile IntPredicate breakRequest = n -> false;
    private volatile int status = 200;
    private volatile long delayPer100BytesMillis;

    private final FormatDownloader downloader = new FormatDownloader(
            new Http(UrlConnectionTransport.create(), new RetryPolicy(2, 0, 0)), new RetryPolicy(2, 0, 0),
            Collections.singletonMap("visionos", "VisionUA/1.0"), "DefaultUA/1.0");

    @BeforeEach
    void startServer() throws IOException {
        server = new TestHttpServer().handle("/videoplayback", this::media);
    }

    @AfterEach
    void stopServer() {
        server.close();
    }

    @Test
    void downloadsInChunks() throws IOException {
        List<DownloadProgress> progress = new ArrayList<>();
        Path file = dir.resolve("a.m4a");

        downloader.toFile(format(MEDIA.length), file, DownloadOptions.defaults().chunkSize(1000).onProgress(progress::add));

        assertArrayEquals(MEDIA, Files.readAllBytes(file));
        assertEquals(Arrays.asList("0-999", "1000-1999", "2000-2999", "3000-3499"), ranges);
        assertEquals(100, progress.get(progress.size() - 1).percent());
        assertEquals(0, partFiles());
    }

    @Test
    void continuesAfterABrokenConnectionWithoutDuplicatingBytes() throws IOException {
        breakRequest = n -> n == 2;
        Path file = dir.resolve("a.m4a");

        downloader.toFile(format(MEDIA.length), file, DownloadOptions.defaults().chunkSize(1000));

        assertArrayEquals(MEDIA, Files.readAllBytes(file));
        // the retry starts at the first missing byte; later chunks shift by the same amount
        assertEquals(Arrays.asList("0-999", "1000-1999", "1300-2299", "2300-3299", "3300-3499"), ranges);
    }

    @Test
    void givesUpWhenTheConnectionKeepsBreaking() {
        breakRequest = n -> n >= 2;
        Path file = dir.resolve("a.m4a");

        assertThrows(DownloadException.class, () -> downloader.toFile(format(MEDIA.length), file, DownloadOptions.defaults().chunkSize(1000)));
        assertEquals(4, ranges.size(), "first chunk plus three tries of the second: " + ranges);
        assertFalse(Files.exists(file));
        assertEquals(0, partFiles());
    }

    @Test
    void unknownSizeIsOnePlainRequest() throws IOException {
        Path file = dir.resolve("a.mp4");

        downloader.toFile(format(0), file, DownloadOptions.defaults());

        assertArrayEquals(MEDIA, Files.readAllBytes(file));
        assertEquals(Collections.singletonList("none"), ranges);
    }

    @Test
    void forbiddenIsNotRetriedAndLeavesNoFile() {
        status = 403;
        Path file = dir.resolve("a.m4a");

        DownloadException error = assertThrows(DownloadException.class, () -> downloader.toFile(format(MEDIA.length), file, DownloadOptions.defaults()));
        assertTrue(error.getMessage().contains("expired or YouTube blocked this client"), error.getMessage());
        assertEquals(1, ranges.size());
        assertFalse(Files.exists(file));
        assertEquals(0, partFiles());
    }

    @Test
    void keepsAnExistingFileUnlessAskedToOverwrite() throws IOException {
        Path file = dir.resolve("a.m4a");
        Files.write(file, new byte[]{1, 2, 3});

        assertThrows(DownloadException.class, () -> downloader.toFile(format(MEDIA.length), file, DownloadOptions.defaults()));
        assertTrue(ranges.isEmpty());
        assertArrayEquals(new byte[]{1, 2, 3}, Files.readAllBytes(file));

        downloader.toFile(format(MEDIA.length), file, DownloadOptions.defaults().overwrite(true));
        assertArrayEquals(MEDIA, Files.readAllBytes(file));
    }

    @Test
    void expiredUrlFailsBeforeAnyRequest() {
        Format expired = Format.builder(140, server.url("/videoplayback?itag=140&expire=1000")).contentLength(MEDIA.length).build();

        DownloadException error = assertThrows(DownloadException.class, () -> downloader.toFile(expired, dir.resolve("a.m4a"), DownloadOptions.defaults()));
        assertTrue(error.getMessage().contains("expired"), error.getMessage());
        assertTrue(ranges.isEmpty());
    }

    @Test
    void downloadsIntoAStream() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        downloader.toStream(format(MEDIA.length), out, DownloadOptions.defaults().chunkSize(2000));

        assertArrayEquals(MEDIA, out.toByteArray());
    }

    @Test
    void sendsTheUserAgentOfTheClientThatGotTheUrl() {
        Format fromVisionos = Format.builder(140, server.url("/videoplayback?itag=140")).contentLength(MEDIA.length).client("visionos").build();

        downloader.toStream(fromVisionos, new ByteArrayOutputStream(), DownloadOptions.defaults());
        downloader.toStream(format(MEDIA.length), new ByteArrayOutputStream(), DownloadOptions.defaults());

        assertEquals(Arrays.asList("VisionUA/1.0", "DefaultUA/1.0"), userAgents);
    }

    @Test
    void waitsUntilTheFormatIsAvailable() {
        long start = System.currentTimeMillis();
        Format later = Format.builder(140, server.url("/videoplayback?itag=140")).contentLength(MEDIA.length)
                .availableAtMillis(start + 400).build();

        downloader.toStream(later, new ByteArrayOutputStream(), DownloadOptions.defaults());

        assertTrue(System.currentTimeMillis() - start >= 390, "did not wait");
        assertEquals(1, ranges.size());
    }

    @Test
    void cancelStopsTheDownloadAndRemovesThePartFile() throws Exception {
        delayPer100BytesMillis = 50;
        CountDownLatch started = new CountDownLatch(1);
        Path file = dir.resolve("slow.m4a");
        try (YoutubeDownloader yt = YoutubeDownloader.create()) {
            CompletableFuture<Path> future = yt.downloadAsync(format(MEDIA.length), file,
                    DownloadOptions.defaults().chunkSize(1000).onProgress(p -> started.countDown()));
            assertTrue(started.await(5, TimeUnit.SECONDS));

            assertTrue(future.cancel(true));
            assertTrue(future.isCancelled());
            waitUntil(() -> partFiles() == 0);
            assertFalse(Files.exists(file));
        }
    }

    @Test
    void parallelDownloadsOfTheSameFileNeverMixTheirBytes() throws Exception {
        Path file = dir.resolve("same.m4a");
        delayPer100BytesMillis = 2;
        List<Throwable> errors = new CopyOnWriteArrayList<>();
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            Thread thread = new Thread(() -> {
                try {
                    downloader.toFile(format(MEDIA.length), file, DownloadOptions.defaults().chunkSize(1000));
                } catch (DownloadException e) {
                    errors.add(e);
                }
            });
            threads.add(thread);
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join(10_000);
        }

        assertArrayEquals(MEDIA, Files.readAllBytes(file));
        assertEquals(1, errors.size(), "the second one finds the file already there");
        assertEquals(0, partFiles());
    }

    @Test
    void ignoresAnExpiryItCannotRead() {
        Format odd = Format.builder(140, server.url("/videoplayback?itag=140&expire=99999999999999999999")).contentLength(MEDIA.length).build();

        downloader.toStream(odd, new ByteArrayOutputStream(), DownloadOptions.defaults());
        assertEquals(1, ranges.size());
    }

    private long partFiles() {
        try (java.util.stream.Stream<Path> files = Files.list(dir)) {
            return files.filter(f -> f.getFileName().toString().endsWith(".part")).count();
        } catch (IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    private Format format(long contentLength) {
        return Format.builder(140, server.url("/videoplayback?itag=140")).mimeType("audio/mp4").contentLength(contentLength).build();
    }

    private void media(HttpExchange exchange) throws IOException {
        userAgents.add(exchange.getRequestHeaders().getFirst("User-Agent"));
        String query = exchange.getRequestURI().getQuery();
        String range = "none";
        int from = 0;
        int to = MEDIA.length - 1;
        for (String param : query.split("&")) {
            if (param.startsWith("range=")) {
                range = param.substring(6);
                String[] bounds = range.split("-");
                from = Integer.parseInt(bounds[0]);
                to = Math.min(MEDIA.length - 1, Integer.parseInt(bounds[1]));
            }
        }
        ranges.add(range);
        if (status != 200) {
            respond(exchange, status, "denied");
            return;
        }
        int length = to - from + 1;
        boolean cut = breakRequest.test(ranges.size());
        exchange.sendResponseHeaders(200, length);
        try (OutputStream out = exchange.getResponseBody()) {
            int limit = cut ? 300 : length;
            for (int sent = 0; sent < limit; sent += 100) {
                out.write(MEDIA, from + sent, Math.min(100, limit - sent));
                out.flush();
                if (delayPer100BytesMillis > 0) {
                    Thread.sleep(delayPer100BytesMillis);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (cut) {
            exchange.close();
        }
    }

    private static void waitUntil(java.util.function.BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5000;
        while (!condition.getAsBoolean() && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }
        assertTrue(condition.getAsBoolean(), "condition not reached in 5 s");
    }
}
