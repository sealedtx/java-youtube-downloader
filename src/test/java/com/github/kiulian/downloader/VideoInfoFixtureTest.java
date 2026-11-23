package com.github.kiulian.downloader;

import com.github.kiulian.downloader.YoutubeException.VideoUnavailableException;
import com.github.kiulian.downloader.model.subtitles.SubtitleTrack;
import com.github.kiulian.downloader.model.videos.VideoInfo;
import com.github.kiulian.downloader.model.videos.formats.Format;
import com.github.kiulian.downloader.testing.Corpus;
import com.github.kiulian.downloader.testing.FakeYoutube;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The whole pipeline on recorded YouTube answers. Re-record with {@code mvn test -Precord}.
 */
class VideoInfoFixtureTest {

    private final FakeYoutube youtube = FakeYoutube.fromFixtures();
    private final YoutubeDownloader yt = YoutubeDownloader.builder().transport(youtube).build();

    @Test
    void meAtTheZoo() {
        VideoInfo video = yt.videoInfo("https://www.youtube.com/watch?v=" + Corpus.ZOO);

        assertEquals("visionos", video.client());
        assertEquals("Me at the zoo", video.details().title());
        assertEquals("jawed", video.details().author());
        assertEquals(19, video.details().lengthSeconds());
        assertTrue(video.details().viewCount() > 300_000_000L);
        assertFalse(video.details().thumbnails().isEmpty());
        assertTrue(video.warnings().isEmpty(), video.warnings().toString());

        assertFalse(video.formats().isEmpty());
        for (Format format : video.formats()) {
            assertTrue(format.url().startsWith("https://") && format.url().contains(".googlevideo.com/"), format.url());
            assertEquals("visionos", format.client());
        }
        Format audio = video.formats().bestAudio().orElseThrow(AssertionError::new);
        assertTrue(audio.hasAudio() && !audio.hasVideo());
        assertFalse(audio.isDrc(), "plain audio wins over the volume-compressed copy");
        assertTrue(audio.contentLength() > 0);
        assertEquals(240, video.formats().bestVideo().orElseThrow(AssertionError::new).height());

        assertFalse(video.subtitles().isEmpty());
        SubtitleTrack track = video.subtitles().get(0);
        assertTrue(track.url().startsWith("https://www.youtube.com/api/timedtext"));
    }

    @Test
    void openMovieWithManySubtitles() {
        VideoInfo video = yt.videoInfo(Corpus.SINTEL);

        assertEquals(888, video.details().lengthSeconds());
        assertTrue(video.details().keywords().contains("sintel"));
        assertEquals(872, video.formats().bestVideo().orElseThrow(AssertionError::new).height());
        assertEquals("avc1.4D401F", video.formats().byItag(136).orElseThrow(AssertionError::new).codecs());
        assertEquals(30, video.subtitles().size());
    }

    @Test
    void madeForKidsVideoWithoutAChallengeSolver() {
        VideoInfo video = yt.videoInfo(Corpus.MADE_FOR_KIDS);

        assertEquals("android_sdkless", video.client());
        assertTrue(video.formats().size() >= 10, video.formats().size() + " formats");
    }

    @Test
    void privateVideoIsUnavailableWithEveryAttempt() {
        VideoUnavailableException error = assertThrows(VideoUnavailableException.class, () -> yt.videoInfo(Corpus.PRIVATE));

        assertEquals("This video is private", error.reason());
        assertEquals(4, error.attempts().size());
        assertTrue(error.attempts().get(3).startsWith("tv_samsung: skipped, needs a challenge solver"));
        assertEquals(Arrays.asList("VISIONOS:" + Corpus.PRIVATE, "ANDROID_VR:" + Corpus.PRIVATE, "ANDROID:" + Corpus.PRIVATE),
                youtube.playerCalls());
    }

    @Test
    void missingVideoIsUnavailable() {
        VideoUnavailableException error = assertThrows(VideoUnavailableException.class, () -> yt.videoInfo(Corpus.MISSING));

        assertEquals("This video is unavailable", error.reason());
        assertTrue(error.getMessage().contains("visionos: ERROR (This video is unavailable)"), error.getMessage());
    }

    @Test
    void asyncGivesTheSameAnswer() throws ExecutionException, InterruptedException {
        try (YoutubeDownloader downloader = yt) {
            assertEquals("Me at the zoo", downloader.videoInfoAsync(Corpus.ZOO).get().details().title());
        }
    }

    @Test
    void asyncFailuresCompleteTheFuture() {
        ExecutionException error = assertThrows(ExecutionException.class, () -> yt.videoInfoAsync(Corpus.MISSING).get());
        assertTrue(error.getCause() instanceof VideoUnavailableException);
    }

    @Test
    void rejectsWhatIsNotAVideo() {
        assertThrows(IllegalArgumentException.class, () -> yt.videoInfo("https://example.com/watch?v=" + Corpus.ZOO));
        assertTrue(youtube.playerCalls().isEmpty());
    }

    @Test
    void visitorDataIsFetchedOnce() {
        yt.videoInfo(Corpus.ZOO);
        yt.videoInfo(Corpus.SINTEL);
        assertEquals(1, youtube.visitorDataCalls());
    }
}
