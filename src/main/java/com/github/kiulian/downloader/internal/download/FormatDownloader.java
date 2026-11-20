package com.github.kiulian.downloader.internal.download;

import com.github.kiulian.downloader.YoutubeException.DownloadException;
import com.github.kiulian.downloader.download.DownloadOptions;
import com.github.kiulian.downloader.download.DownloadProgress;
import com.github.kiulian.downloader.http.HttpTransport.Request;
import com.github.kiulian.downloader.http.HttpTransport.Response;
import com.github.kiulian.downloader.internal.http.Http;
import com.github.kiulian.downloader.internal.http.HttpStatusException;
import com.github.kiulian.downloader.internal.http.RetryPolicy;
import com.github.kiulian.downloader.model.videos.formats.Format;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Downloads formats with range requests. After a broken connection it continues at the next missing byte, so
 * retries never duplicate data. Files are written to {@code <name>.part} and moved into place when complete.
 */
public final class FormatDownloader {

    private static final int BUFFER_SIZE = 64 * 1024;
    private static final Pattern EXPIRE = Pattern.compile("[?&]expire=(\\d{1,12})(?:&|$)");

    private final Http http;
    private final RetryPolicy retryPolicy;
    private final Map<String, String> userAgents;
    private final String defaultUserAgent;

    /**
     * @param userAgents user agent per client profile name; googlevideo expects the one of the client that got the URL
     */
    public FormatDownloader(Http http, RetryPolicy retryPolicy, Map<String, String> userAgents, String defaultUserAgent) {
        this.http = http;
        this.retryPolicy = retryPolicy;
        this.userAgents = userAgents;
        this.defaultUserAgent = defaultUserAgent;
    }

    public Path toFile(Format format, Path file, DownloadOptions options) {
        checkNotExpired(format);
        if (!options.overwrite() && Files.exists(file)) {
            throw new DownloadException("File already exists: " + file + "; use DownloadOptions.overwrite(true)", null);
        }
        Path part = null;
        try {
            Path dir = file.toAbsolutePath().getParent();
            Files.createDirectories(dir);
            // unique and created new: parallel downloads of the same target never share it, and no existing file is touched
            part = Files.createTempFile(dir, file.getFileName() + ".", ".part");
            try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(part, StandardOpenOption.WRITE))) {
                copy(format, out, options);
            }
            move(part, file, options.overwrite());
            return file;
        } catch (IOException e) {
            throw failure(format, e);
        } finally {
            if (part != null) {
                deleteQuietly(part);
            }
        }
    }

    public void toStream(Format format, OutputStream out, DownloadOptions options) {
        checkNotExpired(format);
        try {
            copy(format, out, options);
            out.flush();
        } catch (IOException e) {
            throw failure(format, e);
        }
    }

    private void copy(Format format, OutputStream out, DownloadOptions options) throws IOException {
        long wait = format.availableAtMillis() - System.currentTimeMillis();
        if (wait > 0) {
            sleep(wait); // the URL answers 403 until the pre-roll ad could be skipped
        }
        Transfer transfer = new Transfer(format.contentLength(), options);
        if (transfer.total <= 0) {
            // unknown size: one plain request, nothing to resume from
            try (Response response = http.open(request(format, format.url()))) {
                transfer.pump(response.body(), out, Long.MAX_VALUE);
            }
            return;
        }
        int failures = 0;
        while (transfer.done < transfer.total) {
            long end = Math.min(transfer.total, transfer.done + options.chunkSize()) - 1;
            try (Response response = http.open(request(format, withRange(format.url(), transfer.done, end)))) {
                long expected = end - transfer.done + 1;
                long got = transfer.pump(response.body(), out, expected);
                if (got < expected) {
                    throw new IOException("Connection ended after " + got + " of " + expected + " bytes");
                }
                failures = 0; // the retry budget is per chunk, not per file
            } catch (IOException e) {
                failures++;
                long delay = retryPolicy.delayBeforeRetry(failures, e);
                if (delay < 0) {
                    throw e;
                }
                sleep(delay);
            }
        }
    }

    private Request request(Format format, String url) {
        String userAgent = format.client() != null ? userAgents.getOrDefault(format.client(), defaultUserAgent) : defaultUserAgent;
        return Request.get(url).header("User-Agent", userAgent).header("Accept-Encoding", "identity").build();
    }

    static String withRange(String url, long first, long last) {
        return url + (url.indexOf('?') >= 0 ? "&" : "?") + "range=" + first + "-" + last;
    }

    private static void checkNotExpired(Format format) {
        Matcher matcher = EXPIRE.matcher(format.url());
        if (matcher.find()) {
            Instant expires = Instant.ofEpochSecond(Long.parseLong(matcher.group(1)));
            if (Instant.now().isAfter(expires)) {
                throw new DownloadException("The URL of format " + format.itag() + " expired at " + expires
                        + "; get the video info again", null);
            }
        }
    }

    private static DownloadException failure(Format format, IOException e) {
        String message = "Could not download format " + format.itag() + ": " + e.getMessage();
        if (e instanceof HttpStatusException && ((HttpStatusException) e).status() == 403) {
            message += " (the URL may have expired or YouTube blocked this client; get the video info again or use another client)";
        }
        if (e instanceof InterruptedIOException && Thread.currentThread().isInterrupted()) {
            message = "Download of format " + format.itag() + " was cancelled";
        }
        return new DownloadException(message, e);
    }

    private static void move(Path part, Path file, boolean overwrite) throws IOException {
        if (!overwrite) {
            // a hard link fails atomically if the file exists, even when another download finishes at the same moment
            try {
                Files.createLink(file, part);
                return; // the part file is deleted by the caller
            } catch (FileAlreadyExistsException e) {
                throw e;
            } catch (UnsupportedOperationException | IOException noHardLinks) {
                Files.move(part, file); // file systems without hard links: checks, then renames
                return;
            }
        }
        try {
            Files.move(part, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(part, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void deleteQuietly(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
            // nothing more to do
        }
    }

    private static void sleep(long millis) throws InterruptedIOException {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InterruptedIOException("Interrupted while waiting to retry");
        }
    }

    private static final class Transfer {
        final long total;
        final DownloadOptions options;
        final long start = System.currentTimeMillis();
        final byte[] buffer = new byte[BUFFER_SIZE];
        long done;

        Transfer(long total, DownloadOptions options) {
            this.total = total;
            this.options = options;
        }

        /** Copies at most {@code limit} bytes and counts them, even if reading fails half way. */
        long pump(InputStream in, OutputStream out, long limit) throws IOException {
            long copied = 0;
            while (copied < limit) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedIOException("Download cancelled");
                }
                int n = in.read(buffer, 0, (int) Math.min(buffer.length, limit - copied));
                if (n == -1) {
                    break;
                }
                out.write(buffer, 0, n);
                copied += n;
                done += n;
                options.onProgress().accept(new DownloadProgress(done, Math.max(total, 0), System.currentTimeMillis() - start));
            }
            return copied;
        }
    }
}
