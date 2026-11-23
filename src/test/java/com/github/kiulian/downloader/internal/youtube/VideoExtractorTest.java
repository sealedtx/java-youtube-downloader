package com.github.kiulian.downloader.internal.youtube;

import com.github.kiulian.downloader.YoutubeException;
import com.github.kiulian.downloader.YoutubeException.BotCheckException;
import com.github.kiulian.downloader.YoutubeException.HttpException;
import com.github.kiulian.downloader.client.ClientProfiles;
import com.github.kiulian.downloader.internal.http.Http;
import com.github.kiulian.downloader.internal.http.RetryPolicy;
import com.github.kiulian.downloader.model.videos.VideoInfo;
import com.github.kiulian.downloader.testing.FakeYoutube;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Client chain and classification on small hand-written player answers.
 */
class VideoExtractorTest {

    private static final String ID = "jNQXAC9IVRw";
    private static final String BOT = status("LOGIN_REQUIRED", "Sign in to confirm you’re not a bot");
    private static final String GOOD = ok(format(140, "https://rr1.googlevideo.com/videoplayback?itag=140", "audio/mp4"));

    @Test
    void fallsBackToTheNextClient() {
        FakeYoutube youtube = new FakeYoutube((client, id, call) -> client.equals("VISIONOS") ? status("UNPLAYABLE", "nope") : GOOD);

        VideoInfo video = extractor(youtube).videoInfo(ID);

        assertEquals("android_vr", video.client());
        assertEquals(1, video.formats().size());
        assertEquals(Arrays.asList("visionos: UNPLAYABLE (nope)"), video.warnings());
    }

    @Test
    void renewsVisitorDataOnceAfterABotCheck() {
        FakeYoutube youtube = new FakeYoutube((client, id, call) -> call == 1 ? BOT : GOOD);

        VideoInfo video = extractor(youtube).videoInfo(ID);

        assertEquals("visionos", video.client());
        assertEquals(2, youtube.visitorDataCalls());
        assertEquals(Arrays.asList("VISIONOS:" + ID, "VISIONOS:" + ID), youtube.playerCalls());
    }

    @Test
    void botCheckEverywhereIsABotCheckException() {
        FakeYoutube youtube = new FakeYoutube((client, id, call) -> BOT);

        BotCheckException error = assertThrows(BotCheckException.class, () -> extractor(youtube).videoInfo(ID));
        assertEquals(5, error.attempts().size(), error.attempts().toString());
    }

    @Test
    void sabrOnlyAnswerGivesMetadataWithWarnings() {
        String sabrOnly = ok("{\"itag\":18,\"mimeType\":\"video/mp4\"},{\"itag\":140,\"mimeType\":\"audio/mp4\"}");
        FakeYoutube youtube = new FakeYoutube((client, id, call) -> sabrOnly);

        VideoInfo video = extractor(youtube).videoInfo(ID);

        assertTrue(video.formats().isEmpty());
        assertEquals("Test video", video.details().title());
        assertTrue(video.warnings().contains("visionos: skipped 2 formats without URL (SABR only)"), video.warnings().toString());
        assertTrue(video.warnings().contains("android_vr: no downloadable formats"), video.warnings().toString());
    }

    @Test
    void skipsCipheredAndForeignFormats() {
        String answer = ok(format(140, "https://rr1.googlevideo.com/videoplayback?itag=140", "audio/mp4")
                + ",{\"itag\":251,\"signatureCipher\":\"s=abc&url=x\",\"mimeType\":\"audio/webm\"}"
                + "," + format(18, "https://evil.example/videoplayback", "video/mp4")
                + "," + format(22, "http://rr1.googlevideo.com/videoplayback", "video/mp4"));
        FakeYoutube youtube = new FakeYoutube((client, id, call) -> answer);

        VideoInfo video = extractor(youtube).videoInfo(ID);

        assertEquals(1, video.formats().size());
        assertTrue(video.warnings().contains("visionos: skipped 1 formats that need a challenge solver"));
        assertTrue(video.warnings().contains("visionos: skipped 2 formats with URLs outside YouTube hosts"));
    }

    @Test
    void liveStreamsGiveTheHlsManifest() {
        String live = "{\"playabilityStatus\":{\"status\":\"OK\"},\"videoDetails\":{\"videoId\":\"" + ID + "\",\"isLive\":true},"
                + "\"streamingData\":{\"hlsManifestUrl\":\"https://manifest.googlevideo.com/api/manifest/hls_variant/x\","
                + "\"adaptiveFormats\":[" + format(140, "https://rr1.googlevideo.com/videoplayback", "audio/mp4") + "]}}";
        FakeYoutube youtube = new FakeYoutube((client, id, call) -> live);

        VideoInfo video = extractor(youtube).videoInfo(ID);

        assertTrue(video.details().isLive());
        assertTrue(video.formats().isEmpty());
        assertEquals("https://manifest.googlevideo.com/api/manifest/hls_variant/x", video.hlsManifestUrl());
    }

    @Test
    void unsafeManifestUrlsAreDropped() {
        String answer = "{\"playabilityStatus\":{\"status\":\"OK\"},\"videoDetails\":{\"isLive\":true},"
                + "\"streamingData\":{\"hlsManifestUrl\":\"file:///etc/passwd\"}}";
        assertNull(extractor(new FakeYoutube((client, id, call) -> answer)).videoInfo(ID).hlsManifestUrl());
    }

    @Test
    void httpErrorsEverywhereAreAnHttpException() {
        FakeYoutube youtube = new FakeYoutube((client, id, call) -> null);

        HttpException error = assertThrows(HttpException.class, () -> extractor(youtube).videoInfo(ID));
        assertEquals(404, error.status());
    }

    @Test
    void brokenJsonIsAnAttemptNotACrash() {
        FakeYoutube youtube = new FakeYoutube((client, id, call) -> client.equals("VISIONOS") ? "<html>" : GOOD);

        VideoInfo video = extractor(youtube).videoInfo(ID);

        assertEquals("android_vr", video.client());
        assertTrue(video.warnings().get(0).startsWith("visionos: invalid JSON"), video.warnings().toString());
    }

    @Test
    void reportsEveryAttemptWhenNothingWorks() {
        FakeYoutube youtube = new FakeYoutube((client, id, call) -> "{}");

        YoutubeException error = assertThrows(YoutubeException.class, () -> extractor(youtube).videoInfo(ID));
        assertEquals(Arrays.asList("visionos: no status (no reason given)", "android_vr: no status (no reason given)",
                "android_sdkless: no status (no reason given)", "tv_samsung: skipped, needs a challenge solver (for example EjsSolver.node())"), error.attempts());
    }

    @Test
    void fillsMissingMetadataFromTheWatchPageDataNotFromRecommendations() {
        String withoutTitle = "{\"playabilityStatus\":{\"status\":\"OK\"},\"videoDetails\":{\"videoId\":\"" + ID + "\",\"lengthSeconds\":\"60\"},"
                + "\"endscreen\":{\"endscreenRenderer\":{\"elements\":[{\"endscreenElementRenderer\":{\"title\":{\"runs\":[{\"text\":\"Another video\"}]}}}]}},"
                + "\"streamingData\":{\"adaptiveFormats\":[" + format(140, "https://rr1.googlevideo.com/videoplayback?itag=140", "audio/mp4") + "]}}";
        String next = "{\"contents\":{\"twoColumnWatchNextResults\":{\"results\":{\"results\":{\"contents\":["
                + "{\"videoPrimaryInfoRenderer\":{\"title\":{\"runs\":[{\"text\":\"Test title\"}]},"
                + "\"viewCount\":{\"videoViewCountRenderer\":{\"viewCount\":{\"simpleText\":\"102,250 views\"}}}}},"
                + "{\"videoSecondaryInfoRenderer\":{\"owner\":{\"videoOwnerRenderer\":{\"title\":{\"runs\":[{\"text\":\"Test channel\"}]}}}}}]}}},"
                + "\"secondaryResults\":{\"x\":{\"videoPrimaryInfoRenderer\":{\"title\":{\"simpleText\":\"Wrong\"}}}}}}";
        FakeYoutube youtube = new FakeYoutube((FakeYoutube.Router) request -> request.url().contains("/youtubei/v1/next") ? next : withoutTitle);

        VideoInfo video = extractor(youtube).videoInfo(ID);

        assertEquals("Test title", video.details().title());
        assertEquals("Test channel", video.details().author());
        assertEquals(102_250, video.details().viewCount());
        assertEquals(60, video.details().lengthSeconds());
    }

    private static VideoExtractor extractor(FakeYoutube youtube) {
        return new VideoExtractor(new Innertube(new Http(youtube, RetryPolicy.NONE)), ClientProfiles.defaults());
    }

    private static String status(String status, String reason) {
        return "{\"playabilityStatus\":{\"status\":\"" + status + "\",\"reason\":\"" + reason + "\"}}";
    }

    private static String ok(String formats) {
        return "{\"playabilityStatus\":{\"status\":\"OK\"},\"videoDetails\":{\"videoId\":\"" + ID + "\",\"title\":\"Test video\"},"
                + "\"streamingData\":{\"adaptiveFormats\":[" + formats + "]}}";
    }

    private static String format(int itag, String url, String mimeType) {
        return "{\"itag\":" + itag + ",\"url\":\"" + url + "\",\"mimeType\":\"" + mimeType + "\",\"audioQuality\":\"AUDIO_QUALITY_MEDIUM\"}";
    }
}
