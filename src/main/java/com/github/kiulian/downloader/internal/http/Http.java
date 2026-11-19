package com.github.kiulian.downloader.internal.http;

import com.github.kiulian.downloader.http.HttpTransport;
import com.github.kiulian.downloader.http.HttpTransport.Request;
import com.github.kiulian.downloader.http.HttpTransport.Response;
import com.github.kiulian.downloader.internal.Redact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Sends requests through any {@link HttpTransport} with retries, status checks and a size limit for text bodies.
 */
public final class Http {

    /** Larger text bodies are rejected; YouTube pages and JSON responses stay far below. */
    public static final int DEFAULT_MAX_TEXT_BYTES = 32 * 1024 * 1024;

    private static final Logger LOG = LoggerFactory.getLogger(Http.class);
    private static final int SNIPPET_BYTES = 300;

    private final HttpTransport transport;
    private final RetryPolicy retryPolicy;
    private final Sleeper sleeper;
    private final int maxTextBytes;

    public Http(HttpTransport transport, RetryPolicy retryPolicy) {
        this(transport, retryPolicy, Thread::sleep, DEFAULT_MAX_TEXT_BYTES);
    }

    Http(HttpTransport transport, RetryPolicy retryPolicy, Sleeper sleeper, int maxTextBytes) {
        this.transport = Objects.requireNonNull(transport, "transport");
        this.retryPolicy = Objects.requireNonNull(retryPolicy, "retryPolicy");
        this.sleeper = sleeper;
        this.maxTextBytes = maxTextBytes;
    }

    /**
     * Sends the request, with retries, and returns the body of the 2xx response as UTF-8 text.
     * Failures while reading the body are retried too.
     *
     * @throws HttpStatusException for other statuses once retries are used up
     */
    public String text(Request request) throws IOException {
        return execute(request, response -> {
            try (Response r = response) {
                return readText(r.body(), maxTextBytes);
            }
        });
    }

    /**
     * Sends the request, with retries, and returns the open 2xx response; the caller reads and closes it.
     * Failures while reading the body are not retried here.
     *
     * @throws HttpStatusException for other statuses once retries are used up
     */
    public Response open(Request request) throws IOException {
        return execute(request, response -> response);
    }

    private <T> T execute(Request request, BodyReader<T> reader) throws IOException {
        for (int retry = 1; ; retry++) {
            IOException failure;
            try {
                Response response = transport.send(request);
                int status = response.status();
                if (status >= 200 && status < 300) {
                    return reader.read(response);
                }
                failure = statusError(request, response);
            } catch (IOException e) {
                failure = e;
            }
            long delay = retryPolicy.delayBeforeRetry(retry, failure);
            if (delay < 0) {
                throw failure;
            }
            LOG.debug("Retry {} of {} {} in {} ms after: {}", retry, request.method(), Redact.url(request.url()), delay, failure.toString());
            pause(delay);
        }
    }

    private void pause(long millis) throws InterruptedIOException {
        try {
            sleeper.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InterruptedIOException("Interrupted while waiting to retry");
        }
    }

    private static HttpStatusException statusError(Request request, Response response) {
        int status = response.status();
        String retryAfter = response.header("Retry-After");
        String snippet = "";
        try (Response r = response) {
            snippet = readSnippet(r.body());
        } catch (IOException ignored) {
            // the status is what matters
        }
        return new HttpStatusException(status, request.method(), Redact.url(request.url()), retryAfter, snippet);
    }

    static String readText(InputStream in, int maxBytes) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int n;
        while ((n = in.read(buffer)) != -1) {
            if (out.size() + n > maxBytes) {
                throw new ResponseTooLargeException(maxBytes);
            }
            out.write(buffer, 0, n);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    private static String readSnippet(InputStream in) throws IOException {
        byte[] buffer = new byte[SNIPPET_BYTES];
        int length = 0;
        int n;
        while (length < buffer.length && (n = in.read(buffer, length, buffer.length - length)) != -1) {
            length += n;
        }
        return new String(buffer, 0, length, StandardCharsets.UTF_8).replaceAll("\\s+", " ").trim();
    }

    /** A text body is larger than the limit. Never retried. */
    public static final class ResponseTooLargeException extends IOException {
        ResponseTooLargeException(int maxBytes) {
            super("Response body is larger than " + maxBytes + " bytes");
        }
    }

    interface Sleeper {
        void sleep(long millis) throws InterruptedException;
    }

    private interface BodyReader<T> {
        T read(Response response) throws IOException;
    }
}
