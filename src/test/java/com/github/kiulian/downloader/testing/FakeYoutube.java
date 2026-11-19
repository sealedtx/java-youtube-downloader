package com.github.kiulian.downloader.testing;

import com.github.kiulian.downloader.http.HttpTransport;
import com.github.kiulian.downloader.internal.json.Json;
import com.github.kiulian.downloader.internal.json.JsonNode;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.zip.GZIPInputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Offline stand-in for YouTube: answers {@code sw.js_data} with fixed visitor data and player requests with
 * recorded fixtures or with a handler.
 */
public final class FakeYoutube implements HttpTransport {

    public static final String VISITOR_DATA = "CgtGaXh0dXJlVmlzaXQ%3D";

    /** Answer for one player request: JSON text, or {@code null} for HTTP 404. */
    public interface PlayerHandler {
        String player(String clientName, String videoId, int call) throws IOException;
    }

    private final PlayerHandler handler;
    private final List<String> playerCalls = new CopyOnWriteArrayList<>();
    private volatile int visitorDataCalls;

    public FakeYoutube(PlayerHandler handler) {
        this.handler = handler;
    }

    /** Player answers from {@code src/test/resources/fixtures/video/<id>/player-<client>.json.gz}. */
    public static FakeYoutube fromFixtures() {
        return new FakeYoutube((clientName, videoId, call) -> fixture("video/" + videoId + "/player-" + clientName.toLowerCase() + ".json.gz"));
    }

    public static String fixture(String path) throws IOException {
        InputStream in = FakeYoutube.class.getResourceAsStream("/fixtures/" + path);
        if (in == null) {
            throw new IOException("no fixture " + path + "; record fixtures with mvn test -Precord");
        }
        try (InputStream gz = new GZIPInputStream(in)) {
            return new String(gz.readAllBytes(), UTF_8);
        }
    }

    /** "CLIENT:videoId" for each player request, in order. */
    public List<String> playerCalls() {
        return playerCalls;
    }

    public int visitorDataCalls() {
        return visitorDataCalls;
    }

    @Override
    public Response send(Request request) throws IOException {
        String url = request.url();
        if (url.endsWith("/sw.js_data")) {
            visitorDataCalls++;
            return response(200, ")]}'\n[[\"yt.sw.adr\",null,[[[null,null,null,null,null,null,null,null,null,null,null,null,null,\""
                    + VISITOR_DATA + "\"]]]]]");
        }
        if (url.contains("/youtubei/v1/player")) {
            JsonNode body = Json.parse(new String(request.body(), UTF_8));
            String clientName = body.at("context", "client", "clientName").asString();
            String videoId = body.get("videoId").asString();
            playerCalls.add(clientName + ":" + videoId);
            String json = handler.player(clientName, videoId, playerCalls.size());
            return json == null ? response(404, "") : response(200, json);
        }
        return response(404, "");
    }

    private static Response response(int status, String body) {
        InputStream in = new ByteArrayInputStream(body.getBytes(UTF_8));
        return new Response() {
            @Override
            public int status() {
                return status;
            }

            @Override
            public String header(String name) {
                return null;
            }

            @Override
            public InputStream body() {
                return in;
            }

            @Override
            public void close() {
            }
        };
    }
}
