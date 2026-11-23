package com.github.kiulian.downloader.internal.youtube;

import com.github.kiulian.downloader.client.ClientProfiles;
import com.github.kiulian.downloader.internal.http.Http;
import com.github.kiulian.downloader.internal.http.RetryPolicy;
import com.github.kiulian.downloader.internal.json.Json;
import com.github.kiulian.downloader.internal.json.JsonNode;
import com.github.kiulian.downloader.js.ChallengeSolutions;
import com.github.kiulian.downloader.js.ChallengeSolver;
import com.github.kiulian.downloader.model.videos.VideoInfo;
import com.github.kiulian.downloader.model.videos.formats.Format;
import com.github.kiulian.downloader.testing.FakeYoutube;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Clients that need the JS player, with a fake solver: player script, signatureTimestamp, URL rewriting, ad wait.
 */
class JsClientTest {

    private static final String ID = "Pmajp_gN00w";
    private static final String TV_ANSWER = "{\"playabilityStatus\":{\"status\":\"OK\"},\"videoDetails\":{\"videoId\":\"" + ID + "\",\"title\":\"Kids\"},"
            + "\"adPlacements\":[{\"adPlacementRenderer\":{\"config\":{\"adPlacementConfig\":{\"kind\":\"AD_PLACEMENT_KIND_START\"}},"
            + "\"renderer\":{\"instreamVideoAdRenderer\":{\"skipOffsetMilliseconds\":5000}}}},"
            + "{\"adPlacementRenderer\":{\"config\":{\"adPlacementConfig\":{\"kind\":\"AD_PLACEMENT_KIND_MILLISECONDS\"}},"
            + "\"renderer\":{\"instreamVideoAdRenderer\":{\"skipOffsetMilliseconds\":99000}}}}],"
            + "\"streamingData\":{\"formats\":[{\"itag\":18,\"mimeType\":\"video/mp4\",\"audioQuality\":\"AUDIO_QUALITY_LOW\","
            + "\"url\":\"https://rr1.googlevideo.com/videoplayback?itag=18&n=abc&x=1\"}],"
            + "\"adaptiveFormats\":[{\"itag\":140,\"mimeType\":\"audio/mp4\",\"audioQuality\":\"AUDIO_QUALITY_MEDIUM\","
            + "\"signatureCipher\":\"s=SIG%3D123&sp=sig&url=https%3A%2F%2Frr1.googlevideo.com%2Fvideoplayback%3Fitag%3D140%26n%3Dabc\"}]}}";

    private final List<JsonNode> tvBodies = new CopyOnWriteArrayList<>();
    private final AtomicInteger playerDownloads = new AtomicInteger();

    private final FakeYoutube youtube = new FakeYoutube((FakeYoutube.Router) request -> {
        String url = request.url();
        if (url.endsWith("/iframe_api")) {
            return "var scriptUrl = 'https:\\/\\/www.youtube.com\\/s\\/player\\/abcd1234\\/www-widgetapi.vflset\\/www-widgetapi.js';";
        }
        if (url.contains("/s/player/abcd1234/player_ias.vflset/en_US/base.js")) {
            playerDownloads.incrementAndGet();
            return "var config={signatureTimestamp:20774};";
        }
        JsonNode body = Json.parse(new String(request.body(), UTF_8));
        if (!"TVHTML5".equals(body.at("context", "client", "clientName").asString())) {
            return "{\"playabilityStatus\":{\"status\":\"ERROR\",\"reason\":\"This video is unavailable\"}}";
        }
        tvBodies.add(body);
        return TV_ANSWER;
    });

    @Test
    void solvesChallengesAndRewritesUrls() {
        RecordingSolver solver = new RecordingSolver(true);
        long before = System.currentTimeMillis();

        VideoInfo video = extractor(solver).videoInfo(ID);

        assertEquals("tv_samsung", video.client());
        assertEquals(20774, tvBodies.get(0).at("playbackContext", "contentPlaybackContext", "signatureTimestamp").asInt(0));
        assertEquals("abcd1234", solver.playerId);
        assertEquals(Collections.singleton("abc"), solver.n);
        assertEquals(Collections.singleton("SIG=123"), solver.signatures);

        Format muxed = video.formats().byItag(18).get();
        assertEquals("https://rr1.googlevideo.com/videoplayback?itag=18&n=N_abc&x=1", muxed.url());
        Format audio = video.formats().byItag(140).get();
        assertEquals("https://rr1.googlevideo.com/videoplayback?itag=140&n=N_abc&sig=321%3DGIS", audio.url());

        long wait = audio.availableAtMillis() - before;
        assertTrue(wait >= 5000 && wait < 6000, "only the pre-roll ad counts: " + wait);
    }

    @Test
    void unsolvedChallengesSkipTheFormats() {
        VideoInfo video = extractor(new RecordingSolver(false)).videoInfo(ID);

        assertTrue(video.formats().isEmpty());
        assertTrue(video.warnings().contains("tv_samsung: skipped 2 formats whose JS challenges were not solved"), video.warnings().toString());
    }

    @Test
    void downloadsThePlayerOnlyOnce() {
        VideoExtractor extractor = extractor(new RecordingSolver(true));
        extractor.videoInfo(ID);
        extractor.videoInfo(ID);

        assertEquals(1, playerDownloads.get());
    }

    @Test
    void anAdWithoutSkipOffsetDoesNotBlockForItsWholeLength() {
        String longAd = "{\"adPlacements\":[{\"adPlacementRenderer\":{\"config\":{\"adPlacementConfig\":{\"kind\":\"AD_PLACEMENT_KIND_START\"}},"
                + "\"renderer\":{\"instreamVideoAdRenderer\":{\"playerVars\":\"length_seconds=3600&x=1\"}}}}]}";

        long wait = new PlayerResponse(Json.parse(longAd)).availableAtMillis(1_000_000) - 1_000_000;

        assertEquals(PlayerResponse.MAX_AD_WAIT_MILLIS, wait);
    }

    private VideoExtractor extractor(ChallengeSolver solver) {
        Http http = new Http(youtube, RetryPolicy.NONE);
        return new VideoExtractor(new Innertube(http), ClientProfiles.defaults(), new PlayerScripts(http), solver);
    }

    /** n -> "N_" + n, signature -> reversed; or nothing at all. */
    private static final class RecordingSolver implements ChallengeSolver {
        final boolean solves;
        String playerId;
        Set<String> n;
        Set<String> signatures;

        RecordingSolver(boolean solves) {
            this.solves = solves;
        }

        @Override
        public ChallengeSolutions solve(String playerId, String playerCode, Set<String> n, Set<String> signatures) {
            this.playerId = playerId;
            this.n = n;
            this.signatures = signatures;
            Map<String, String> nMap = new HashMap<>();
            Map<String, String> sigMap = new HashMap<>();
            if (solves) {
                n.forEach(c -> nMap.put(c, "N_" + c));
                signatures.forEach(c -> sigMap.put(c, new StringBuilder(c).reverse().toString()));
            }
            return new ChallengeSolutions(nMap, sigMap);
        }
    }
}
