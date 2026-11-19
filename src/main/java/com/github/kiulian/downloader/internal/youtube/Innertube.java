package com.github.kiulian.downloader.internal.youtube;

import com.github.kiulian.downloader.client.ClientProfile;
import com.github.kiulian.downloader.client.ClientProfiles;
import com.github.kiulian.downloader.http.HttpTransport.Request;
import com.github.kiulian.downloader.internal.http.Http;
import com.github.kiulian.downloader.internal.json.Json;
import com.github.kiulian.downloader.internal.json.JsonException;
import com.github.kiulian.downloader.internal.json.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Calls YouTube's internal API (innertube) as one of the client profiles, with an anonymous visitor session.
 */
public final class Innertube {

    public static final String ORIGIN = "https://www.youtube.com";

    public static final String WEB_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";

    private static final Logger LOG = LoggerFactory.getLogger(Innertube.class);
    private static final long VISITOR_DATA_TTL_MILLIS = 60 * 60 * 1000;
    private static final Pattern VISITOR_DATA_IN_PAGE = Pattern.compile("\"VISITOR_DATA\":\"([^\"]+)\"");
    private static final Pattern INITIAL_DATA = Pattern.compile("ytInitialData\"?\\]?\\s*=\\s*");

    private final Http http;
    private final ClientProfile webClient;
    private String visitorData;
    private long visitorDataTime;

    public Innertube(Http http) {
        this(http, ClientProfiles.WEB);
    }

    /** @param webClient profile for browse, resolve and search requests */
    public Innertube(Http http, ClientProfile webClient) {
        this.http = http;
        this.webClient = webClient;
    }

    /** {@code /youtubei/v1/browse} with a {@code browseId}, for example {@code VL<playlist id>}. */
    public JsonNode browse(String browseId) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("browseId", browseId);
        return webPost("browse", body);
    }

    /** {@code /youtubei/v1/search}; {@code params} holds filters and sort order, may be {@code null}. */
    public JsonNode search(String query, String params) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("query", query);
        if (params != null) {
            body.put("params", params);
        }
        return webPost("search", body);
    }

    /** Next page of a browse or search answer. */
    public JsonNode continuation(String endpoint, String token) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("continuation", token);
        return webPost(endpoint, body);
    }

    /** {@code /youtubei/v1/navigation/resolve_url}: turns a channel URL such as {@code /@NASA} into ids. */
    public JsonNode resolveUrl(String url) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("url", url);
        return webPost("navigation/resolve_url", body);
    }

    /** {@code ytInitialData} of a web page such as {@code /playlist?list=...}; fallback when the API fails. */
    public JsonNode initialData(String pathAndQuery) throws IOException {
        String html = http.text(Request.get(ORIGIN + pathAndQuery).header("User-Agent", webClient.userAgent())
                .header("Accept-Language", "en-US,en;q=0.9").build());
        Matcher matcher = INITIAL_DATA.matcher(html);
        if (!matcher.find()) {
            throw new IOException("No ytInitialData on " + pathAndQuery);
        }
        return Json.parsePrefix(html, matcher.end());
    }

    private JsonNode webPost(String endpoint, Map<String, Object> fields) throws IOException {
        String visitor = visitorData();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("context", context(webClient, visitor));
        body.putAll(fields);
        return post(endpoint, webClient, visitor, body);
    }

    /**
     * Anonymous session id. Without it YouTube answers most requests with a bot check.
     * Taken from the small {@code sw.js_data} endpoint, or from the home page if that fails; cached for an hour.
     */
    public synchronized String visitorData() throws IOException {
        if (visitorData != null && System.currentTimeMillis() - visitorDataTime < VISITOR_DATA_TTL_MILLIS) {
            return visitorData;
        }
        String fresh;
        try {
            fresh = visitorDataFromServiceWorker();
        } catch (IOException | JsonException e) {
            LOG.debug("visitor data from sw.js_data failed, trying the home page: {}", e.toString());
            fresh = visitorDataFromHomePage();
        }
        visitorData = fresh;
        visitorDataTime = System.currentTimeMillis();
        return fresh;
    }

    /** Forget the cached visitor data, for example after a bot check. */
    public synchronized void resetVisitorData() {
        visitorData = null;
    }

    // Response is ")]}'" followed by a JSON array; the visitor data sits at a fixed position.
    private String visitorDataFromServiceWorker() throws IOException {
        String text = http.text(Request.get(ORIGIN + "/sw.js_data").header("User-Agent", WEB_USER_AGENT).build());
        int start = text.indexOf('[');
        if (start < 0) {
            throw new IOException("sw.js_data has no JSON");
        }
        String value = Json.parsePrefix(text, start).at(0, 2, 0, 0, 13).asString();
        if (value == null || value.isEmpty()) {
            throw new IOException("sw.js_data has no visitor data");
        }
        return value;
    }

    private String visitorDataFromHomePage() throws IOException {
        String html = http.text(Request.get(ORIGIN + "/").header("User-Agent", WEB_USER_AGENT)
                .header("Accept-Language", "en-US,en;q=0.9").build());
        Matcher matcher = VISITOR_DATA_IN_PAGE.matcher(html);
        if (!matcher.find()) {
            throw new IOException("Could not find visitor data on the YouTube home page");
        }
        return matcher.group(1);
    }

    /** {@code /youtubei/v1/player} for one video as the given client. */
    public JsonNode player(ClientProfile client, String videoId) throws IOException {
        return player(client, videoId, null);
    }

    /**
     * Like {@link #player(ClientProfile, String)}, with the player's {@code signatureTimestamp}, which clients that
     * need the JS player must send (without it YouTube answers "The page needs to be reloaded").
     */
    public JsonNode player(ClientProfile client, String videoId, Integer signatureTimestamp) throws IOException {
        String visitor = visitorData();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("context", context(client, visitor));
        body.put("videoId", videoId);
        body.put("contentCheckOk", true);
        body.put("racyCheckOk", true);
        if (signatureTimestamp != null) {
            Map<String, Object> content = new LinkedHashMap<>();
            content.put("signatureTimestamp", signatureTimestamp);
            Map<String, Object> playback = new LinkedHashMap<>();
            playback.put("contentPlaybackContext", content);
            body.put("playbackContext", playback);
        }
        return post("player", client, visitor, body);
    }

    private JsonNode post(String endpoint, ClientProfile client, String visitor, Map<String, Object> body) throws IOException {
        Request request = Request.post(ORIGIN + "/youtubei/v1/" + endpoint + "?prettyPrint=false",
                        Json.write(body).getBytes(StandardCharsets.UTF_8))
                .header("Content-Type", "application/json")
                .header("User-Agent", client.userAgent())
                .header("Origin", ORIGIN)
                .header("X-YouTube-Client-Name", String.valueOf(client.clientId()))
                .header("X-YouTube-Client-Version", client.version())
                .header("X-Goog-Visitor-Id", visitor)
                .build();
        return Json.parse(http.text(request));
    }

    private static Map<String, Object> context(ClientProfile client, String visitorData) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("clientName", client.clientName());
        fields.put("clientVersion", client.version());
        fields.putAll(client.context());
        fields.put("hl", "en");
        fields.put("gl", "US");
        fields.put("visitorData", visitorData);
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("client", fields);
        return context;
    }
}
