package com.github.kiulian.downloader.testing;

import com.github.kiulian.downloader.http.HttpTransport;
import com.github.kiulian.downloader.internal.json.Json;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Sends requests to real YouTube and stores every successful answer at {@link FakeYoutube#fixturePath}, with the
 * caller's IP address masked, so that {@link FakeYoutube#fromFixtures()} can replay it.
 */
public final class RecordingTransport implements HttpTransport {

    private final HttpTransport real;
    private final Path fixtures;
    private final String ownIp;

    public RecordingTransport(HttpTransport real, Path fixtures, String ownIp) {
        this.real = real;
        this.fixtures = fixtures;
        this.ownIp = ownIp;
    }

    @Override
    public Response send(Request request) throws IOException {
        Response response = real.send(request);
        String path = FakeYoutube.fixturePath(request);
        if (path == null || response.status() != 200) {
            return response;
        }
        String body;
        try (Response r = response) {
            body = new String(r.body().readAllBytes(), UTF_8);
        }
        if (path.startsWith("page/")) {
            body = onlyInitialData(body);
        }
        Path file = fixtures.resolve(path);
        Files.createDirectories(file.getParent());
        try (OutputStream out = new GZIPOutputStream(Files.newOutputStream(file))) {
            out.write(maskIp(body, ownIp).getBytes(UTF_8));
        }
        return FakeYoutube.response(200, body);
    }

    // a web page is ~1 MB; only ytInitialData is read, so keep just that
    private static String onlyInitialData(String html) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("ytInitialData\"?\\]?\\s*=\\s*").matcher(html);
        if (!matcher.find()) {
            return html;
        }
        return "<script>var ytInitialData = " + Json.parsePrefix(html, matcher.end()) + ";</script>";
    }

    static String maskIp(String text, String ip) {
        String masked = text.replaceAll("([?&]ip=)[^&\"\\\\]*", "$10.0.0.0").replaceAll("(ip%3D)[^%&\"\\\\]*", "$10.0.0.0");
        return ip == null || ip.isEmpty() ? masked : masked.replace(ip, "0.0.0.0");
    }
}
