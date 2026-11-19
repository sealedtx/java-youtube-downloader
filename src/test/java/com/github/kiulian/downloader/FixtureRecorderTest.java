package com.github.kiulian.downloader;

import com.github.kiulian.downloader.client.ClientProfile;
import com.github.kiulian.downloader.client.ClientProfiles;
import com.github.kiulian.downloader.http.HttpTransport;
import com.github.kiulian.downloader.http.HttpTransport.Request;
import com.github.kiulian.downloader.http.UrlConnectionTransport;
import com.github.kiulian.downloader.internal.http.Http;
import com.github.kiulian.downloader.internal.http.RetryPolicy;
import com.github.kiulian.downloader.internal.json.Json;
import com.github.kiulian.downloader.internal.youtube.Innertube;
import com.github.kiulian.downloader.testing.Corpus;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.GZIPOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Re-records the offline fixtures from real YouTube: {@code mvn test -Precord}.
 * <p>
 * Saves the raw player answer of every default client for every corpus video, with the client IP masked.
 * Review the git diff afterwards: it shows what YouTube changed.
 */
@Tag("record")
class FixtureRecorderTest {

    private static final Path FIXTURES = Paths.get("src/test/resources/fixtures");

    @Test
    void recordPlayerResponses() throws IOException {
        Http http = new Http(UrlConnectionTransport.create(), RetryPolicy.retries(2));
        Innertube innertube = new Innertube(http);
        String ip = ownIp(UrlConnectionTransport.create());

        for (String videoId : Corpus.VIDEOS) {
            for (ClientProfile client : ClientProfiles.defaults()) {
                String json = innertube.player(client, videoId).toString();
                write("video/" + videoId + "/player-" + client.clientName().toLowerCase() + ".json.gz", maskIp(json, ip));
            }
        }
    }

    static String maskIp(String text, String ip) {
        String masked = text.replaceAll("([?&]ip=)[^&\"\\\\]*", "$10.0.0.0").replaceAll("(ip%3D)[^%&\"\\\\]*", "$10.0.0.0");
        return ip == null || ip.isEmpty() ? masked : masked.replace(ip, "0.0.0.0");
    }

    // sw.js_data also carries the caller's IP address; it must never end up in the repository.
    private static String ownIp(HttpTransport transport) throws IOException {
        try (HttpTransport.Response response = transport.send(Request.get(Innertube.ORIGIN + "/sw.js_data").build())) {
            String text = new String(response.body().readAllBytes(), UTF_8);
            return Json.parsePrefix(text, text.indexOf('[')).at(0, 2, 0, 0, 3).asString();
        }
    }

    private static void write(String path, String text) throws IOException {
        Path file = FIXTURES.resolve(path);
        Files.createDirectories(file.getParent());
        try (OutputStream out = new GZIPOutputStream(Files.newOutputStream(file))) {
            out.write(text.getBytes(UTF_8));
        }
    }
}
