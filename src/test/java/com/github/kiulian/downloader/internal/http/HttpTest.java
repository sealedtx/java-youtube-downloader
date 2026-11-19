package com.github.kiulian.downloader.internal.http;

import com.github.kiulian.downloader.http.HttpTransport;
import com.github.kiulian.downloader.http.HttpTransport.Request;
import com.github.kiulian.downloader.http.HttpTransport.Response;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpTest {

    private static final Request REQUEST = Request.get("https://www.youtube.com/youtubei/v1/player").build();

    private final List<Long> sleeps = new ArrayList<>();

    @Test
    void returnsTheTextOfASuccessfulResponse() throws IOException {
        ScriptedTransport transport = new ScriptedTransport(response(200, "héllo"));

        assertEquals("héllo", http(transport, RetryPolicy.retries(2)).text(REQUEST));
        assertEquals(1, transport.calls);
    }

    @Test
    void retriesServerErrorsUntilItWorks() throws IOException {
        ScriptedTransport transport = new ScriptedTransport(response(503, "busy"), response(200, "ok"));

        assertEquals("ok", http(transport, RetryPolicy.retries(2)).text(REQUEST));
        assertEquals(2, transport.calls);
        assertEquals(1, sleeps.size());
        assertTrue(sleeps.get(0) >= 500 && sleeps.get(0) <= 625, "backoff " + sleeps);
    }

    @Test
    void waitsAsLongAsRetryAfterSays() throws IOException {
        ScriptedTransport transport = new ScriptedTransport(response(429, "", "Retry-After", "2"), response(200, "ok"));

        assertEquals("ok", http(transport, RetryPolicy.retries(2)).text(REQUEST));
        assertEquals(Collections.singletonList(2000L), sleeps);
    }

    @Test
    void givesUpWhenRetryAfterIsTooLong() {
        ScriptedTransport transport = new ScriptedTransport(response(429, "", "Retry-After", "3600"));

        HttpStatusException error = assertThrows(HttpStatusException.class, () -> http(transport, RetryPolicy.retries(2)).text(REQUEST));
        assertEquals(429, error.status());
        assertEquals(1, transport.calls);
        assertTrue(sleeps.isEmpty());
    }

    @Test
    void doesNotRetryClientErrors() {
        ScriptedTransport transport = new ScriptedTransport(response(404, "not here"));

        HttpStatusException error = assertThrows(HttpStatusException.class, () -> http(transport, RetryPolicy.retries(2)).text(REQUEST));
        assertEquals(404, error.status());
        assertEquals(1, transport.calls);
    }

    @Test
    void retriesIoErrors() throws IOException {
        ScriptedTransport transport = new ScriptedTransport(new IOException("connection reset"), response(200, "ok"));

        assertEquals("ok", http(transport, RetryPolicy.retries(1)).text(REQUEST));
        assertEquals(2, transport.calls);
    }

    @Test
    void stopsAfterTheLastRetry() {
        ScriptedTransport transport = new ScriptedTransport(response(500, ""), response(500, ""), response(500, ""));

        HttpStatusException error = assertThrows(HttpStatusException.class, () -> http(transport, RetryPolicy.retries(2)).text(REQUEST));
        assertEquals(500, error.status());
        assertEquals(3, transport.calls);
        assertEquals(2, sleeps.size());
    }

    @Test
    void neverRetriesTooLargeBodies() {
        ScriptedTransport transport = new ScriptedTransport(response(200, "twenty characters!!!"));
        Http http = new Http(transport, RetryPolicy.retries(2), sleeps::add, 10);

        assertThrows(Http.ResponseTooLargeException.class, () -> http.text(REQUEST));
        assertEquals(1, transport.calls);
    }

    @Test
    void stopsWhenInterruptedWhileWaiting() {
        ScriptedTransport transport = new ScriptedTransport(response(503, ""));
        Http http = new Http(transport, RetryPolicy.retries(2), millis -> {
            throw new InterruptedException();
        }, Http.DEFAULT_MAX_TEXT_BYTES);

        assertThrows(InterruptedIOException.class, () -> http.text(REQUEST));
        assertTrue(Thread.interrupted(), "interrupt flag must be restored");
    }

    @Test
    void statusErrorMasksSecretsAndKeepsTheStartOfTheBody() {
        Request request = Request.get("https://rr1.googlevideo.com/videoplayback?ip=203.0.113.7&itag=18&sig=AOq0").build();
        ScriptedTransport transport = new ScriptedTransport(response(403, "{\"error\":\n  \"forbidden\"}"));

        HttpStatusException error = assertThrows(HttpStatusException.class, () -> http(transport, RetryPolicy.NONE).text(request));
        assertEquals("HTTP 403 for GET https://rr1.googlevideo.com/videoplayback?ip=***&itag=18&sig=***: {\"error\": \"forbidden\"}",
                error.getMessage());
    }

    @Test
    void openReturnsTheResponseUnread() throws IOException {
        ScriptedTransport transport = new ScriptedTransport(response(206, "partial bytes"));

        try (Response response = http(transport, RetryPolicy.NONE).open(REQUEST)) {
            assertEquals(206, response.status());
            assertEquals("partial bytes", new String(response.body().readAllBytes(), UTF_8));
        }
    }

    private Http http(HttpTransport transport, RetryPolicy policy) {
        return new Http(transport, policy, sleeps::add, Http.DEFAULT_MAX_TEXT_BYTES);
    }

    private static Response response(int status, String body, String... headerPairs) {
        Map<String, String> headers = new HashMap<>();
        for (int i = 0; i < headerPairs.length; i += 2) {
            headers.put(headerPairs[i].toLowerCase(Locale.ROOT), headerPairs[i + 1]);
        }
        InputStream in = new ByteArrayInputStream(body.getBytes(UTF_8));
        return new Response() {
            @Override
            public int status() {
                return status;
            }

            @Override
            public String header(String name) {
                return headers.get(name.toLowerCase(Locale.ROOT));
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

    /** Returns the given responses or throws the given exceptions, one per call. */
    private static final class ScriptedTransport implements HttpTransport {
        private final Deque<Object> steps;
        int calls;

        ScriptedTransport(Object... steps) {
            this.steps = new ArrayDeque<>(Arrays.asList(steps));
        }

        @Override
        public Response send(Request request) throws IOException {
            calls++;
            Object step = steps.removeFirst();
            if (step instanceof IOException) {
                throw (IOException) step;
            }
            return (Response) step;
        }
    }
}
