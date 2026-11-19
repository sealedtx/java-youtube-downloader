package com.github.kiulian.downloader.testing;

import com.github.kiulian.downloader.http.HttpTransport;
import com.github.kiulian.downloader.internal.json.Json;
import com.github.kiulian.downloader.internal.json.JsonNode;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.zip.GZIPInputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Offline stand-in for YouTube. Answers {@code sw.js_data} with fixed visitor data and everything else through a
 * router: recorded fixtures by default, or hand-written answers in tests.
 */
public final class FakeYoutube implements HttpTransport {

    public static final String VISITOR_DATA = "CgtGaXh0dXJlVmlzaXQ%3D";

    /** Answer for one player request: JSON text, or {@code null} for HTTP 404. */
    public interface PlayerHandler {
        String player(String clientName, String videoId, int call) throws IOException;
    }

    /** Answer for any request: body text, or {@code null} for HTTP 404; may throw to simulate a broken connection. */
    public interface Router {
        String answer(Request request) throws IOException;
    }

    private final Router router;
    private final List<String> playerCalls = new CopyOnWriteArrayList<>();
    private final List<String> requests = new CopyOnWriteArrayList<>();
    private volatile int visitorDataCalls;

    public FakeYoutube(Router router) {
        this.router = router;
    }

    /** Hand-written player answers; other requests are 404. */
    public FakeYoutube(PlayerHandler handler) {
        this(new Router() {
            private int calls;

            @Override
            public synchronized String answer(Request request) throws IOException {
                if (!request.url().contains("/youtubei/v1/player")) {
                    return null;
                }
                JsonNode body = Json.parse(new String(request.body(), UTF_8));
                return handler.player(body.at("context", "client", "clientName").asString(), body.get("videoId").asString(), ++calls);
            }
        });
    }

    /** Recorded answers from {@code src/test/resources/fixtures}; see {@link #fixturePath(Request)}. */
    public static FakeYoutube fromFixtures() {
        return new FakeYoutube((Router) request -> {
            String path = fixturePath(request);
            return path == null ? null : fixture(path);
        });
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

    /**
     * Where the answer to a request is stored, or {@code null} for requests that are not recorded:
     * {@code video/<id>/player-<client>.json.gz}, {@code browse/<browseId>.json.gz},
     * {@code browse/continuation-<hash>.json.gz}, {@code resolve/<hash>.json.gz}, {@code page/<path>.html.gz}.
     */
    public static String fixturePath(Request request) {
        String url = request.url();
        if (url.contains("/youtubei/v1/")) {
            JsonNode body = Json.parse(new String(request.body(), UTF_8));
            if (url.contains("/youtubei/v1/player")) {
                return "video/" + body.get("videoId").asString() + "/player-"
                        + body.at("context", "client", "clientName").asString().toLowerCase() + ".json.gz";
            }
            String endpoint = url.substring(url.indexOf("/youtubei/v1/") + 13, url.indexOf('?')).replace('/', '_');
            if (body.has("continuation")) {
                return endpoint + "/continuation-" + hash(body.get("continuation").asString()) + ".json.gz";
            }
            if (body.has("browseId")) {
                return endpoint + "/" + body.get("browseId").asString() + ".json.gz";
            }
            return endpoint + "/" + hash(Json.write(body.get("url").or(body.get("query")))) + ".json.gz";
        }
        if (url.endsWith("/sw.js_data")) {
            return null;
        }
        String path = url.substring(url.indexOf(".com/") + 5);
        try {
            path = URLDecoder.decode(path, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
        return "page/" + path.replaceAll("[^A-Za-z0-9_-]", "_") + ".html.gz";
    }

    /** "CLIENT:videoId" for each player request, in order. */
    public List<String> playerCalls() {
        return playerCalls;
    }

    /** Fixture path (or URL) of every request, in order. */
    public List<String> requests() {
        return requests;
    }

    public int visitorDataCalls() {
        return visitorDataCalls;
    }

    @Override
    public Response send(Request request) throws IOException {
        if (request.url().endsWith("/sw.js_data")) {
            visitorDataCalls++;
            return response(200, ")]}'\n[[\"yt.sw.adr\",null,[[[null,null,null,null,null,null,null,null,null,null,null,null,null,\""
                    + VISITOR_DATA + "\"]]]]]");
        }
        if (request.url().contains("/youtubei/v1/player")) {
            JsonNode body = Json.parse(new String(request.body(), UTF_8));
            playerCalls.add(body.at("context", "client", "clientName").asString() + ":" + body.get("videoId").asString());
        }
        String path = fixturePath(request);
        requests.add(path != null ? path : request.url());
        String answer = router.answer(request);
        return answer == null ? response(404, "") : response(200, answer);
    }

    static String hash(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-1").digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                hex.append(String.format("%02x", digest[i]));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static Response response(int status, String body) {
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
