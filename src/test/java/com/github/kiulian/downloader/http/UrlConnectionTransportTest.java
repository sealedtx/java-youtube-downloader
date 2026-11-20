package com.github.kiulian.downloader.http;

import com.github.kiulian.downloader.http.HttpTransport.Request;
import com.github.kiulian.downloader.http.HttpTransport.Response;
import com.github.kiulian.downloader.testing.TestHttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.zip.GZIPOutputStream;

import static com.github.kiulian.downloader.testing.TestHttpServer.respond;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UrlConnectionTransportTest {

    private final UrlConnectionTransport transport = UrlConnectionTransport.create();
    private TestHttpServer server;

    @BeforeEach
    void startServer() throws IOException {
        server = new TestHttpServer();
    }

    @AfterEach
    void stopServer() {
        server.close();
    }

    @Test
    void sendsMethodHeadersAndBody() throws IOException {
        List<String> seen = new CopyOnWriteArrayList<>();
        server.handle("/echo", exchange -> {
            seen.add(exchange.getRequestMethod());
            seen.add(exchange.getRequestHeaders().getFirst("X-Test"));
            seen.add(new String(exchange.getRequestBody().readAllBytes(), UTF_8));
            respond(exchange, 200, "ok");
        });

        Request request = Request.post(server.url("/echo"), "{\"a\":1}".getBytes(UTF_8)).header("X-Test", "yes").build();
        try (Response response = transport.send(request)) {
            assertEquals(200, response.status());
            assertEquals("ok", read(response));
        }
        assertEquals(Arrays.asList("POST", "yes", "{\"a\":1}"), seen);
    }

    @Test
    void asksForGzipAndDecompressesIt() throws IOException {
        List<String> acceptEncoding = new CopyOnWriteArrayList<>();
        server.handle("/gzip", exchange -> {
            acceptEncoding.add(exchange.getRequestHeaders().getFirst("Accept-Encoding"));
            exchange.getResponseHeaders().add("Content-Encoding", "gzip");
            respond(exchange, 200, gzip("hello gzip"));
        });

        try (Response response = transport.send(Request.get(server.url("/gzip")).build())) {
            assertEquals("hello gzip", read(response));
        }
        assertEquals(Collections.singletonList("gzip"), acceptEncoding);
    }

    @Test
    void leavesTheBodyAloneWhenTheCallerSetsAcceptEncoding() throws IOException {
        byte[] compressed = gzip("raw");
        server.handle("/gzip", exchange -> {
            exchange.getResponseHeaders().add("Content-Encoding", "gzip");
            respond(exchange, 200, compressed);
        });

        Request request = Request.get(server.url("/gzip")).header("Accept-Encoding", "gzip").build();
        try (Response response = transport.send(request)) {
            assertArrayEquals(compressed, response.body().readAllBytes());
        }
    }

    @Test
    void handlesAnEmptyGzipBody() throws IOException {
        server.handle("/empty", exchange -> {
            exchange.getResponseHeaders().add("Content-Encoding", "gzip");
            respond(exchange, 200, new byte[0]);
        });

        try (Response response = transport.send(Request.get(server.url("/empty")).build())) {
            assertEquals("", read(response));
        }
    }

    @Test
    void returnsErrorStatusesWithTheirBody() throws IOException {
        server.handle("/missing", exchange -> respond(exchange, 404, "nope"));

        try (Response response = transport.send(Request.get(server.url("/missing")).build())) {
            assertEquals(404, response.status());
            assertEquals("nope", read(response));
        }
    }

    @Test
    void failsWhenTheServerIsTooSlow() {
        server.handle("/slow", exchange -> {
            try {
                Thread.sleep(2_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            respond(exchange, 200, "late");
        });
        UrlConnectionTransport impatient = UrlConnectionTransport.builder().readTimeout(Duration.ofMillis(200)).build();

        assertThrows(SocketTimeoutException.class, () -> impatient.send(Request.get(server.url("/slow")).build()).close());
    }

    @Test
    void refusesNonHttpUrls() {
        assertThrows(MalformedURLException.class, () -> transport.send(Request.get("file:///etc/hosts").build()));
    }

    @Test
    void sendsProxyCredentialsToTheProxy() throws IOException {
        List<String> proxyAuth = new CopyOnWriteArrayList<>();
        server.handle("/", exchange -> {
            String auth = exchange.getRequestHeaders().getFirst("Proxy-Authorization");
            proxyAuth.add(String.valueOf(auth));
            if (auth == null) {
                exchange.getResponseHeaders().add("Proxy-Authenticate", "Basic realm=\"test\"");
                respond(exchange, 407, "");
            } else {
                respond(exchange, 200, exchange.getRequestURI().toString());
            }
        });
        UrlConnectionTransport viaProxy = UrlConnectionTransport.builder()
                .proxy("127.0.0.1", server.port())
                .proxyCredentials("user", "secret")
                .build();

        try (Response response = viaProxy.send(Request.get("http://example.invalid/hello").build())) {
            assertEquals(200, response.status());
            assertEquals("http://example.invalid/hello", read(response));
        }
        String expected = "Basic " + Base64.getEncoder().encodeToString("user:secret".getBytes(UTF_8));
        assertEquals(Arrays.asList("null", expected), proxyAuth);
    }

    @Test
    void neverSendsProxyCredentialsToServers() throws IOException {
        List<String> serverAuth = new CopyOnWriteArrayList<>();
        server.handle("/private", exchange -> {
            serverAuth.add(String.valueOf(exchange.getRequestHeaders().getFirst("Authorization")));
            exchange.getResponseHeaders().add("WWW-Authenticate", "Basic realm=\"test\"");
            respond(exchange, 401, "");
        });
        UrlConnectionTransport withCredentials = UrlConnectionTransport.builder().proxyCredentials("user", "secret").build();

        try (Response response = withCredentials.send(Request.get(server.url("/private")).build())) {
            assertEquals(401, response.status());
        }
        assertEquals(Collections.singletonList("null"), serverAuth);
    }

    @Test
    void cookiesFollowRedirectsOnlyOnTheSameHost() throws IOException {
        List<String> cookiesSeen = new CopyOnWriteArrayList<>();
        try (TestHttpServer other = new TestHttpServer()) {
            other.handle("/", exchange -> {
                cookiesSeen.add("other:" + exchange.getRequestHeaders().getFirst("Cookie"));
                respond(exchange, 200, "other host");
            });
            server.handle("/same", exchange -> {
                exchange.getResponseHeaders().add("Location", "/target");
                respond(exchange, 302, "");
            });
            server.handle("/target", exchange -> {
                cookiesSeen.add("target:" + exchange.getRequestHeaders().getFirst("Cookie"));
                respond(exchange, 200, "same host");
            });
            server.handle("/away", exchange -> {
                exchange.getResponseHeaders().add("Location", other.url("/steal"));
                respond(exchange, 302, "");
            });

            try (Response same = transport.send(Request.get(server.url("/same")).header("Cookie", "SID=secret").build())) {
                assertEquals("same host", read(same));
            }
            try (Response away = transport.send(Request.get(server.url("/away")).header("Cookie", "SID=secret").build())) {
                assertEquals(302, away.status(), "not followed to another host");
            }
            try (Response plain = transport.send(Request.get(server.url("/away")).build())) {
                assertEquals("other host", read(plain), "requests without secrets follow redirects as usual");
            }
        }
        assertEquals(Arrays.asList("target:SID=secret", "other:null"), cookiesSeen);
    }

    @Test
    void aBodyThatIsNotGzipFailsCleanly() {
        server.handle("/fake", exchange -> {
            exchange.getResponseHeaders().add("Content-Encoding", "gzip");
            respond(exchange, 200, "plain text");
        });

        assertThrows(IOException.class, () -> transport.send(Request.get(server.url("/fake")).build()));
    }

    private static String read(Response response) throws IOException {
        return new String(response.body().readAllBytes(), UTF_8);
    }

    private static byte[] gzip(String text) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (GZIPOutputStream out = new GZIPOutputStream(bytes)) {
            out.write(text.getBytes(UTF_8));
        }
        return bytes.toByteArray();
    }
}
