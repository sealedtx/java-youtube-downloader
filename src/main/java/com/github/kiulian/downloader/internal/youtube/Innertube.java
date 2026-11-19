package com.github.kiulian.downloader.internal.youtube;

import com.github.kiulian.downloader.client.ClientProfile;
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

    static final String WEB_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";

    private static final Logger LOG = LoggerFactory.getLogger(Innertube.class);
    private static final long VISITOR_DATA_TTL_MILLIS = 60 * 60 * 1000;
    private static final Pattern VISITOR_DATA_IN_PAGE = Pattern.compile("\"VISITOR_DATA\":\"([^\"]+)\"");

    private final Http http;
    private String visitorData;
    private long visitorDataTime;

    public Innertube(Http http) {
        this.http = http;
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
        String visitor = visitorData();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("context", context(client, visitor));
        body.put("videoId", videoId);
        body.put("contentCheckOk", true);
        body.put("racyCheckOk", true);
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
