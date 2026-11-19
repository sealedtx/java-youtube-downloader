package com.github.kiulian.downloader.internal.youtube;

import com.github.kiulian.downloader.auth.YoutubeCookies;
import com.github.kiulian.downloader.client.ClientProfiles;
import com.github.kiulian.downloader.http.HttpTransport.Request;
import com.github.kiulian.downloader.internal.http.Http;
import com.github.kiulian.downloader.internal.http.RetryPolicy;
import com.github.kiulian.downloader.internal.json.Json;
import com.github.kiulian.downloader.js.ChallengeSolutions;
import com.github.kiulian.downloader.model.subtitles.SubtitleFormat;
import com.github.kiulian.downloader.model.subtitles.SubtitleTrack;
import com.github.kiulian.downloader.testing.FakeYoutube;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CookieAuthTest {

    private static final YoutubeCookies COOKIES = YoutubeCookies.fromHeader(
            "SAPISID=SAPI-123; __Secure-1PAPISID=ONE-1P; __Secure-3PAPISID=THREE-3P; LOGIN_INFO=x");

    @Test
    void sapisidHashLikeTheWebPlayer() {
        // sha1("1700000000 <sid> https://www.youtube.com") for each cookie
        assertEquals("SAPISIDHASH 1700000000_abea7b84fb88e7ef11846e06172fd8cd0730a0a8"
                        + " SAPISID1PHASH 1700000000_b0cd4a31b98036afecf88d81d2cf7b5770476928"
                        + " SAPISID3PHASH 1700000000_cf4597ef69ff57347c4bc2dd28890ac6eaaa5872",
                CookieAuth.authorization(COOKIES, "https://www.youtube.com", 1_700_000_000L));
        assertNull(CookieAuth.authorization(YoutubeCookies.fromHeader("PREF=x"), "https://www.youtube.com", 1));
    }

    @Test
    void cookiesGoOnlyToClientsThatSupportThem() {
        List<Request> requests = new CopyOnWriteArrayList<>();
        FakeYoutube youtube = new FakeYoutube((FakeYoutube.Router) request -> {
            requests.add(request);
            if (request.url().endsWith("/iframe_api")) {
                return "'https:\\/\\/www.youtube.com\\/s\\/player\\/abcd1234\\/www-widgetapi.vflset'";
            }
            if (request.url().contains("/s/player/")) {
                return "var c={signatureTimestamp:20774};";
            }
            return "{\"playabilityStatus\":{\"status\":\"LOGIN_REQUIRED\",\"reason\":\"Sign in to confirm your age\"}}";
        });
        Http http = new Http(youtube, RetryPolicy.NONE);
        Innertube innertube = new Innertube(http, ClientProfiles.WEB, COOKIES);
        VideoExtractor extractor = new VideoExtractor(innertube, ClientProfiles.defaults(), new PlayerScripts(http),
                (playerId, code, n, sig) -> new ChallengeSolutions(Collections.emptyMap(), Collections.emptyMap()));

        try {
            extractor.videoInfo("jNQXAC9IVRw");
        } catch (RuntimeException expected) {
            // every client says login required; only the headers matter here
        }

        for (Request request : requests) {
            Map<String, String> headers = request.headers();
            if (!request.url().contains("/youtubei/v1/player")) {
                continue;
            }
            String client = Json.parse(new String(request.body(), UTF_8)).at("context", "client", "clientName").asString();
            boolean cookieClient = client.equals("TVHTML5");
            assertEquals(cookieClient, headers.containsKey("Cookie"), client);
            assertEquals(cookieClient, headers.containsKey("Authorization"), client);
            if (cookieClient) {
                assertTrue(headers.get("Authorization").startsWith("SAPISIDHASH "));
                assertEquals("https://www.youtube.com", headers.get("X-Origin"));
            }
        }
        assertTrue(requests.stream().anyMatch(r -> r.url().contains("/youtubei/v1/player") && r.headers().containsKey("Cookie")));
    }

    @Test
    void subtitleCookiesOnlyForYoutubeHosts() {
        List<Request> requests = new CopyOnWriteArrayList<>();
        FakeYoutube youtube = new FakeYoutube((FakeYoutube.Router) request -> {
            requests.add(request);
            return "WEBVTT";
        });
        Http http = new Http(youtube, RetryPolicy.NONE);
        SubtitleDownloader subtitles = new SubtitleDownloader(http, new Innertube(http, ClientProfiles.WEB, COOKIES));

        subtitles.text(new SubtitleTrack("en", "English", false, true, "https://www.youtube.com/api/timedtext?v=x"), SubtitleFormat.VTT, null);
        subtitles.text(new SubtitleTrack("en", "English", false, true, "https://rr1.googlevideo.com/api/timedtext?v=x"), SubtitleFormat.VTT, null);

        assertTrue(requests.get(0).headers().containsKey("Cookie"));
        assertFalse(requests.get(1).headers().containsKey("Cookie"));
    }
}
