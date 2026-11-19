package com.github.kiulian.downloader;

import com.github.kiulian.downloader.YoutubeException.ChannelUnavailableException;
import com.github.kiulian.downloader.YoutubeException.PlaylistUnavailableException;
import com.github.kiulian.downloader.YoutubeException.VideoUnavailableException;
import com.github.kiulian.downloader.client.ClientProfile;
import com.github.kiulian.downloader.client.ClientProfiles;
import com.github.kiulian.downloader.download.DownloadOptions;
import com.github.kiulian.downloader.download.FileNames;
import com.github.kiulian.downloader.http.HttpTransport.Request;
import com.github.kiulian.downloader.http.HttpTransport.Response;
import com.github.kiulian.downloader.http.UrlConnectionTransport;
import com.github.kiulian.downloader.model.playlist.Playlist;
import com.github.kiulian.downloader.model.playlist.PlaylistVideo;
import com.github.kiulian.downloader.model.videos.VideoInfo;
import com.github.kiulian.downloader.model.videos.formats.Format;
import com.github.kiulian.downloader.testing.Corpus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentest4j.TestAbortedException;

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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks against real YouTube: {@code mvn test -Plive}. Writes {@code target/live-report.md}.
 * <p>
 * Every check is its own test, so Maven shows exactly what broke. Expectations have some slack (at least,
 * not exactly) so normal YouTube changes do not turn them red. Known limitations are reported as skipped.
 * If a reference video is gone, the failure says so: replace its id in {@link Corpus}.
 */
@Tag("live")
class LiveTest {

    private static final List<String> REPORT = Collections.synchronizedList(new ArrayList<>());
    private static final YoutubeDownloader YT = YoutubeDownloader.create();

    // ---- every client profile on its own

    static Stream<Arguments> clientsAndVideos() {
        return ClientProfiles.defaults().stream()
                .flatMap(client -> Stream.of(Corpus.ZOO, Corpus.MUSIC).map(video -> Arguments.of(client, video)));
    }

    @ParameterizedTest(name = "{0} gives downloadable audio and video for {1}")
    @MethodSource("clientsAndVideos")
    void clientHealth(ClientProfile client, String videoId) {
        check("client " + client.name() + " / " + videoId, () -> {
            VideoInfo video = YoutubeDownloader.builder().clients(Collections.singletonList(client)).build().videoInfo(videoId);
            Format audio = video.formats().bestAudio().orElseThrow(() -> new AssertionError("no audio format"));
            Format best = video.formats().bestVideo().orElseThrow(() -> new AssertionError("no video format"));
            String range = range(audio, client.userAgent(), 0, 65_535);
            assertTrue(range.startsWith("OK"), "first 64 KB of the best audio: " + range);
            return video.formats().size() + " formats, best " + best.height() + "p, " + video.subtitles().size() + " subtitles, " + range;
        });
    }

    // ---- video cases with the default client chain

    static Stream<VideoCase> videoCases() {
        return Stream.of(
                new VideoCase("first YouTube video", Corpus.ZOO, v -> {
                    assertEquals("Me at the zoo", v.details().title());
                    assertFalse(v.subtitles().isEmpty());
                }),
                new VideoCase("music video", Corpus.MUSIC, v -> {
                    assertTrue(v.formats().bestVideo().get().height() >= 1080, "best video below 1080p");
                    assertFalse(v.subtitles().isEmpty());
                }),
                new VideoCase("4K video", Corpus.UHD, v ->
                        assertTrue(v.formats().bestVideo().get().height() >= 1440, "best video " + v.formats().bestVideo().get().height() + "p")),
                new VideoCase("dubbed audio tracks", Corpus.MULTI_AUDIO, v -> {
                    Set<String> tracks = v.formats().audioOnly().stream().map(Format::audioTrackId).filter(t -> t != null).collect(Collectors.toSet());
                    assertTrue(tracks.size() >= 2, "audio tracks " + tracks);
                    assertTrue(v.formats().bestAudio().get().isDefaultAudioTrack(), "best audio is not the main track");
                }),
                new VideoCase("short (via /shorts/ URL)", "https://www.youtube.com/shorts/" + Corpus.SHORT, v -> {
                    Format best = v.formats().bestVideo().get();
                    assertTrue(best.height() > best.width(), "not vertical: " + best.width() + "x" + best.height());
                    assertTrue(v.details().lengthSeconds() <= 180);
                }),
                new VideoCase("long video, end of file", Corpus.LONG, v -> {
                    assertTrue(v.details().lengthSeconds() >= 3600);
                    Format audio = v.formats().bestAudio().get();
                    String range = range(audio, null, audio.contentLength() - 65_536, audio.contentLength() - 1);
                    assertTrue(range.startsWith("OK"), "last 64 KB: " + range);
                }),
                new VideoCase("ended live stream", Corpus.WAS_LIVE, v -> {
                    assertTrue(v.details().isLiveContent());
                    assertFalse(v.details().isLive());
                    assertFalse(v.formats().isEmpty());
                }),
                VideoCase.unavailable("private video", Corpus.PRIVATE),
                VideoCase.unavailable("missing video", Corpus.MISSING),
                VideoCase.knownLimitation("made for kids (not served to visionos/android_vr)", Corpus.MADE_FOR_KIDS)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("videoCases")
    void video(VideoCase c) {
        check("video " + c.name + " (" + c.input + ")", () -> {
            if (c.unavailable) {
                VideoUnavailableException e = assertThrows(VideoUnavailableException.class, () -> YT.videoInfo(c.input));
                return "unavailable as expected: " + e.reason();
            }
            VideoInfo video;
            try {
                video = YT.videoInfo(c.input);
            } catch (VideoUnavailableException e) {
                if (c.knownLimitation) {
                    throw new TestAbortedException("known limitation still there: " + e.reason());
                }
                throw new AssertionError("reference video looks removed (" + e.reason() + "); replace it in Corpus", e);
            }
            c.check.accept(video);
            return video.client() + ", " + video.formats().size() + " formats, best "
                    + video.formats().bestVideo().map(f -> f.height() + "p").orElse("none") + ", " + video.subtitles().size() + " subtitles";
        });
    }

    // ---- downloads through the public API

    @Test
    void fullDownloadOfAudioAndVideo() {
        check("full download " + Corpus.ZOO, () -> {
            VideoInfo video = YT.videoInfo(Corpus.ZOO);
            Path dir = Files.createTempDirectory("yt-live");
            StringBuilder result = new StringBuilder();
            for (Format format : Arrays.asList(video.formats().bestAudio().get(), video.formats().bestVideo().get())) {
                long start = System.currentTimeMillis();
                Path file = YT.download(format, dir.resolve(FileNames.of(video.details().title(), format)),
                        DownloadOptions.defaults().chunkSize(256 * 1024));
                assertEquals(format.contentLength(), Files.size(file), "size of itag " + format.itag());
                result.append("itag ").append(format.itag()).append(' ').append(Files.size(file)).append(" B in ")
                        .append(System.currentTimeMillis() - start).append(" ms; ");
                Files.delete(file);
            }
            return result.toString();
        });
    }

    // ---- playlists and channels

    @Test
    void playlistWithOwner() {
        check("playlist " + Corpus.ASK_NASA, () -> {
            Playlist playlist = YT.playlist(Corpus.ASK_NASA);
            List<PlaylistVideo> videos = playlist.allVideos();
            assertEquals("NASA", playlist.details().author());
            assertTrue(videos.size() >= 15, videos.size() + " videos");
            return playlist.details().title() + ", " + videos.size() + " videos";
        });
    }

    @Test
    void albumWithoutOwner() {
        check("album " + Corpus.ALBUM, () -> {
            Playlist album = YT.playlist(Corpus.ALBUM);
            List<PlaylistVideo> tracks = album.allVideos();
            assertNull(album.details().author());
            assertTrue(tracks.size() >= 10, tracks.size() + " tracks");
            return album.details().title() + ", " + tracks.size() + " tracks";
        });
    }

    @Test
    void channelUploadsAcrossPages() {
        check("channel " + Corpus.NASA_HANDLE, () -> {
            long start = System.currentTimeMillis();
            List<PlaylistVideo> videos = YT.channelUploads(Corpus.NASA_HANDLE).stream().limit(250).collect(Collectors.toList());
            assertEquals(250, videos.size());
            assertEquals(250, new HashSet<>(videos.stream().map(PlaylistVideo::videoId).collect(Collectors.toList())).size(), "duplicates");
            assertEquals(250, videos.get(249).index());
            return "250 uploads in " + (System.currentTimeMillis() - start) + " ms";
        });
    }

    @Test
    void missingPlaylistAndChannel() {
        check("missing playlist and channel", () -> {
            assertThrows(PlaylistUnavailableException.class, () -> YT.playlist(Corpus.MISSING_PLAYLIST));
            assertThrows(ChannelUnavailableException.class, () -> YT.channelUploads("@noSuchChannelHere1"));
            return "both unavailable as expected";
        });
    }

    // ---- helpers

    private static void check(String name, Check check) {
        try {
            REPORT.add("| OK | " + name + " | " + check.run().replace("|", "/") + " |");
        } catch (TestAbortedException e) {
            REPORT.add("| ⚠ | " + name + " | " + e.getMessage() + " |");
            throw e;
        } catch (Throwable e) {
            REPORT.add("| FAIL | " + name + " | " + String.valueOf(e.getMessage()).replace("|", "/").replace("\n", " ") + " |");
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            if (e instanceof Error) {
                throw (Error) e;
            }
            throw new AssertionError(e);
        }
    }

    private static String range(Format format, String userAgent, long first, long last) {
        Request request = Request.get(format.url() + "&range=" + first + "-" + last)
                .header("User-Agent", userAgent != null ? userAgent : ClientProfiles.VISIONOS.userAgent()).build();
        try (Response response = UrlConnectionTransport.create().send(request)) {
            long bytes = 0;
            byte[] buffer = new byte[8192];
            int n;
            InputStream in = response.body();
            while ((n = in.read(buffer)) != -1) {
                bytes += n;
            }
            boolean ok = response.status() == 200 && bytes == last - first + 1;
            return (ok ? "OK" : "FAIL") + " HTTP " + response.status() + ", " + bytes + " B, itag " + format.itag();
        } catch (IOException e) {
            return "FAIL " + e;
        }
    }

    @AfterAll
    static void writeReport() throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("# Live checks");
        lines.add("");
        lines.add("Run at " + Instant.now() + ".");
        lines.add("");
        lines.add("| result | check | details |");
        lines.add("|---|---|---|");
        synchronized (REPORT) {
            lines.addAll(REPORT.stream().sorted().collect(Collectors.toList()));
        }
        Path report = Paths.get("target/live-report.md");
        Files.createDirectories(report.getParent());
        Files.write(report, lines, StandardCharsets.UTF_8);
        lines.forEach(System.out::println);
    }

    private interface Check {
        String run() throws Exception;
    }

    private interface VideoCheck {
        void accept(VideoInfo video) throws Exception;
    }

    static final class VideoCase {
        final String name;
        final String input;
        final VideoCheck check;
        final boolean unavailable;
        final boolean knownLimitation;

        VideoCase(String name, String input, VideoCheck check) {
            this(name, input, check, false, false);
        }

        private VideoCase(String name, String input, VideoCheck check, boolean unavailable, boolean knownLimitation) {
            this.name = name;
            this.input = input;
            this.check = check;
            this.unavailable = unavailable;
            this.knownLimitation = knownLimitation;
        }

        static VideoCase unavailable(String name, String input) {
            return new VideoCase(name, input, v -> {
            }, true, false);
        }

        static VideoCase knownLimitation(String name, String input) {
            return new VideoCase(name, input, v -> {
            }, false, true);
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
