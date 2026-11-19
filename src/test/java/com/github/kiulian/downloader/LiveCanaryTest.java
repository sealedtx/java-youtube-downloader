package com.github.kiulian.downloader;

import com.github.kiulian.downloader.YoutubeException.VideoUnavailableException;
import com.github.kiulian.downloader.client.ClientProfile;
import com.github.kiulian.downloader.client.ClientProfiles;
import com.github.kiulian.downloader.download.DownloadOptions;
import com.github.kiulian.downloader.download.FileNames;
import com.github.kiulian.downloader.http.HttpTransport.Request;
import com.github.kiulian.downloader.http.HttpTransport.Response;
import com.github.kiulian.downloader.http.UrlConnectionTransport;
import com.github.kiulian.downloader.model.playlist.Playlist;
import com.github.kiulian.downloader.model.videos.VideoInfo;
import com.github.kiulian.downloader.model.videos.formats.Format;
import com.github.kiulian.downloader.testing.Corpus;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Health matrix against real YouTube: {@code mvn test -Plive}. Writes {@code target/live-report.md}.
 * <p>
 * Every cell is checked on its own, so one broken client or video does not hide the others.
 * The test fails only if no client can give downloadable audio for the most basic video.
 */
@Tag("live")
class LiveCanaryTest {

    private static final List<String> VIDEOS = Arrays.asList(Corpus.ZOO, Corpus.N3WPORT);

    @Test
    void healthMatrix() throws IOException {
        List<String> rows = new ArrayList<>();
        rows.add("| client | video | status | formats | best video | subtitles | download 64 KB of best audio |");
        rows.add("|---|---|---|---|---|---|---|");
        boolean zooWorks = false;

        for (ClientProfile client : ClientProfiles.defaults()) {
            YoutubeDownloader yt = YoutubeDownloader.builder().clients(Collections.singletonList(client)).build();
            for (String videoId : VIDEOS) {
                String status;
                String formats = "-";
                String bestVideo = "-";
                String subtitles = "-";
                String download = "-";
                try {
                    VideoInfo video = yt.videoInfo(videoId);
                    status = video.warnings().isEmpty() ? "OK" : "OK, warnings: " + String.join("; ", video.warnings());
                    formats = String.valueOf(video.formats().size());
                    bestVideo = video.formats().bestVideo().map(f -> f.height() + "p").orElse("none");
                    subtitles = String.valueOf(video.subtitles().size());
                    Format audio = video.formats().bestAudio().orElse(null);
                    download = audio == null ? "no audio format" : rangeCheck(audio, client);
                    if (videoId.equals(Corpus.ZOO) && download.startsWith("OK")) {
                        zooWorks = true;
                    }
                } catch (RuntimeException e) {
                    status = e.getClass().getSimpleName() + ": " + e.getMessage();
                }
                rows.add("| " + client.name() + " | " + videoId + " | " + status.replace("|", "/") + " | " + formats
                        + " | " + bestVideo + " | " + subtitles + " | " + download + " |");
            }
        }
        rows.add("");
        rows.add("Full download: " + fullDownloadCheck());
        rows.add("");
        rows.add("Playlists: " + playlistCheck());
        rows.add("");
        rows.add("Unavailable videos: " + unavailableCheck());
        write(rows);
        assertTrue(zooWorks, "no client could download audio of " + Corpus.ZOO + "; see target/live-report.md");
    }

    private static String rangeCheck(Format format, ClientProfile client) {
        Request request = Request.get(format.url() + "&range=0-65535").header("User-Agent", client.userAgent()).build();
        try (Response response = UrlConnectionTransport.create().send(request)) {
            int bytes = count(response.body());
            return (response.status() == 200 && bytes > 0 ? "OK" : "FAIL") + " (HTTP " + response.status() + ", " + bytes + " B, itag " + format.itag() + ")";
        } catch (IOException e) {
            return "FAIL (" + e + ")";
        }
    }

    // whole best audio and best video of the zoo video through the public API, in small chunks to exercise ranges
    private static String fullDownloadCheck() {
        StringBuilder result = new StringBuilder();
        try (YoutubeDownloader yt = YoutubeDownloader.create()) {
            VideoInfo video = yt.videoInfo(Corpus.ZOO);
            Path dir = Files.createTempDirectory("yt-live");
            for (Format format : Arrays.asList(video.formats().bestAudio().orElse(null), video.formats().bestVideo().orElse(null))) {
                if (format == null) {
                    result.append("missing format; ");
                    continue;
                }
                long start = System.currentTimeMillis();
                Path file = yt.download(format, dir.resolve(FileNames.of(video.details().title(), format)),
                        DownloadOptions.defaults().chunkSize(256 * 1024).overwrite(true));
                long size = Files.size(file);
                result.append("itag ").append(format.itag()).append(size == format.contentLength() ? " OK " : " WRONG SIZE ")
                        .append(size).append(" B in ").append(System.currentTimeMillis() - start).append(" ms; ");
                Files.delete(file);
            }
        } catch (IOException | RuntimeException e) {
            result.append("FAIL ").append(e);
        }
        return result.toString();
    }

    private static String playlistCheck() {
        YoutubeDownloader yt = YoutubeDownloader.create();
        StringBuilder result = new StringBuilder();
        String[][] checks = {{"playlist", Corpus.ASK_NASA}, {"album", Corpus.ALBUM}, {"channel", Corpus.NASA_HANDLE}};
        for (String[] check : checks) {
            try {
                Playlist playlist = check[0].equals("channel") ? yt.channelUploads(check[1]) : yt.playlist(check[1]);
                long count = playlist.stream().limit(250).count();
                result.append(check[0]).append(" OK (").append(playlist.details().title()).append(", ").append(count)
                        .append(count == 250 ? "+" : "").append(" of ").append(playlist.details().videoCount()).append(" videos); ");
            } catch (RuntimeException e) {
                result.append(check[0]).append(" FAIL ").append(e.getClass().getSimpleName()).append(": ").append(e.getMessage()).append("; ");
            }
        }
        return result.toString();
    }

    private static String unavailableCheck() {
        YoutubeDownloader yt = YoutubeDownloader.create();
        StringBuilder result = new StringBuilder();
        for (String videoId : Arrays.asList(Corpus.PRIVATE, Corpus.MISSING)) {
            try {
                yt.videoInfo(videoId);
                result.append(videoId).append(" unexpectedly available; ");
            } catch (VideoUnavailableException e) {
                result.append(videoId).append(" OK (").append(e.reason()).append("); ");
            } catch (RuntimeException e) {
                result.append(videoId).append(" ").append(e.getClass().getSimpleName()).append("; ");
            }
        }
        return result.toString();
    }

    private static int count(InputStream in) throws IOException {
        byte[] buffer = new byte[8192];
        int total = 0;
        int n;
        while ((n = in.read(buffer)) != -1) {
            total += n;
        }
        return total;
    }

    private static void write(List<String> rows) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("# Live health matrix");
        lines.add("");
        lines.add("Run at " + Instant.now() + ".");
        lines.add("");
        lines.addAll(rows);
        Path report = Paths.get("target/live-report.md");
        Files.createDirectories(report.getParent());
        Files.write(report, lines, StandardCharsets.UTF_8);
        lines.forEach(System.out::println);
    }
}
