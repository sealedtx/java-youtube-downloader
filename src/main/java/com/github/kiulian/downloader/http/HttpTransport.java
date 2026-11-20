package com.github.kiulian.downloader.http;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Sends the HTTP requests of the library. Plug in your own implementation to use another HTTP stack (for example
 * OkHttp on Android), custom proxies, cookies or logging. The default is {@link UrlConnectionTransport}.
 * <p>
 * Implementations must be thread-safe. They report transport problems with {@link IOException} and return every
 * HTTP status, errors included, as a {@link Response}. Retries and status handling are done by the library.
 */
public interface HttpTransport {

    /**
     * Sends the request. The caller closes the returned response.
     */
    Response send(Request request) throws IOException;

    /**
     * Immutable HTTP request.
     */
    final class Request {
        private final String method;
        private final String url;
        private final Map<String, String> headers;
        private final byte[] body;

        private Request(Builder builder) {
            this.method = builder.method;
            this.url = builder.url;
            this.headers = Collections.unmodifiableMap(new LinkedHashMap<>(builder.headers));
            this.body = builder.body;
        }

        public static Builder get(String url) {
            return new Builder("GET", url, null);
        }

        public static Builder post(String url, byte[] body) {
            return new Builder("POST", url, Objects.requireNonNull(body, "body"));
        }

        public String method() {
            return method;
        }

        public String url() {
            return url;
        }

        /** Headers in insertion order, names as given. */
        public Map<String, String> headers() {
            return headers;
        }

        /** Copy of the body, or {@code null} if the request has none. */
        public byte[] body() {
            return body == null ? null : body.clone();
        }

        public static final class Builder {
            private final String method;
            private final String url;
            private final byte[] body;
            private final Map<String, String> headers = new LinkedHashMap<>();

            private Builder(String method, String url, byte[] body) {
                this.method = method;
                this.url = Objects.requireNonNull(url, "url");
                this.body = body == null ? null : body.clone();
            }

            public Builder header(String name, String value) {
                headers.put(Objects.requireNonNull(name, "name"), Objects.requireNonNull(value, "value"));
                return this;
            }

            public Request build() {
                return new Request(this);
            }
        }
    }

    /**
     * HTTP response. Close it to release the connection.
     */
    interface Response extends Closeable {

        int status();

        /** Value of the header with this name (case-insensitive), or {@code null}. */
        String header(String name);

        /** Body, already decompressed. Never {@code null}. */
        InputStream body();
    }
}
