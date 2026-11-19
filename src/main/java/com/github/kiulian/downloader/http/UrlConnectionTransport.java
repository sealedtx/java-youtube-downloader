package com.github.kiulian.downloader.http;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URL;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.zip.GZIPInputStream;

/**
 * Default {@link HttpTransport} on top of {@link HttpURLConnection}.
 * <ul>
 * <li>Only {@code http} and {@code https} URLs are sent.</li>
 * <li>Asks for gzip and decompresses it, unless the request sets its own {@code Accept-Encoding}.</li>
 * <li>Proxy credentials are given to each connection; the JVM-wide {@link Authenticator#setDefault} is never
 * touched. Note: for {@code https} through a proxy the JDK disables Basic proxy auth by default; allow it with
 * {@code -Djdk.http.auth.tunneling.disabledSchemes=} or plug in another transport.</li>
 * </ul>
 */
public final class UrlConnectionTransport implements HttpTransport {

    private final int connectTimeoutMillis;
    private final int readTimeoutMillis;
    private final Proxy proxy;
    private final Authenticator proxyAuthenticator;

    private UrlConnectionTransport(Builder builder) {
        this.connectTimeoutMillis = builder.connectTimeoutMillis;
        this.readTimeoutMillis = builder.readTimeoutMillis;
        this.proxy = builder.proxy;
        this.proxyAuthenticator = builder.proxyAuthenticator;
    }

    /** Transport with a 10 s connect timeout, a 30 s read timeout and the system proxy settings. */
    public static UrlConnectionTransport create() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Response send(Request request) throws IOException {
        URL url = new URL(request.url());
        String protocol = url.getProtocol();
        if (!"https".equals(protocol) && !"http".equals(protocol)) {
            throw new MalformedURLException("Unsupported URL scheme: " + protocol);
        }
        HttpURLConnection connection = (HttpURLConnection) (proxy != null ? url.openConnection(proxy) : url.openConnection());
        connection.setConnectTimeout(connectTimeoutMillis);
        connection.setReadTimeout(readTimeoutMillis);
        connection.setInstanceFollowRedirects(true);
        connection.setUseCaches(false);
        if (proxyAuthenticator != null) {
            connection.setAuthenticator(proxyAuthenticator);
        }
        connection.setRequestMethod(request.method());

        boolean ownEncoding = false;
        for (Map.Entry<String, String> header : request.headers().entrySet()) {
            connection.setRequestProperty(header.getKey(), header.getValue());
            ownEncoding |= header.getKey().equalsIgnoreCase("Accept-Encoding");
        }
        if (!ownEncoding) {
            connection.setRequestProperty("Accept-Encoding", "gzip");
        }

        byte[] body = request.body();
        if (body != null) {
            connection.setDoOutput(true);
            connection.setFixedLengthStreamingMode(body.length);
            try (OutputStream out = connection.getOutputStream()) {
                out.write(body);
            }
        }

        int status = connection.getResponseCode();
        InputStream in = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
        if (in == null) {
            in = new ByteArrayInputStream(new byte[0]);
        }
        if (!ownEncoding && "gzip".equalsIgnoreCase(connection.getContentEncoding())) {
            in = gunzip(in);
        }
        return new UrlConnectionResponse(status, connection, in);
    }

    // Some servers send "Content-Encoding: gzip" with an empty body; GZIPInputStream would fail on it.
    private static InputStream gunzip(InputStream in) throws IOException {
        PushbackInputStream pushback = new PushbackInputStream(in, 1);
        int first = pushback.read();
        if (first == -1) {
            return pushback;
        }
        pushback.unread(first);
        return new GZIPInputStream(pushback);
    }

    public static final class Builder {
        private int connectTimeoutMillis = 10_000;
        private int readTimeoutMillis = 30_000;
        private Proxy proxy;
        private Authenticator proxyAuthenticator;

        private Builder() {
        }

        /** Time to open a connection; zero means no limit. */
        public Builder connectTimeout(Duration timeout) {
            this.connectTimeoutMillis = toMillis(timeout);
            return this;
        }

        /** Longest wait for the next bytes from the server; zero means no limit. */
        public Builder readTimeout(Duration timeout) {
            this.readTimeoutMillis = toMillis(timeout);
            return this;
        }

        public Builder proxy(Proxy proxy) {
            this.proxy = Objects.requireNonNull(proxy, "proxy");
            return this;
        }

        /** HTTP proxy; the host name is resolved when connecting. */
        public Builder proxy(String host, int port) {
            return proxy(new Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved(host, port)));
        }

        /** Credentials that are sent to the proxy only, never to the servers behind it. */
        public Builder proxyCredentials(String user, String password) {
            this.proxyAuthenticator = new ProxyAuthenticator(Objects.requireNonNull(user, "user"),
                    Objects.requireNonNull(password, "password").toCharArray());
            return this;
        }

        public UrlConnectionTransport build() {
            return new UrlConnectionTransport(this);
        }

        private static int toMillis(Duration timeout) {
            long millis = Objects.requireNonNull(timeout, "timeout").toMillis();
            if (millis < 0 || millis > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("timeout out of range: " + timeout);
            }
            return (int) millis;
        }
    }

    private static final class ProxyAuthenticator extends Authenticator {
        private final String user;
        private final char[] password;

        ProxyAuthenticator(String user, char[] password) {
            this.user = user;
            this.password = password;
        }

        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            if (getRequestorType() != RequestorType.PROXY) {
                return null;
            }
            return new PasswordAuthentication(user, password.clone());
        }
    }

    private static final class UrlConnectionResponse implements Response {
        private final int status;
        private final HttpURLConnection connection;
        private final InputStream body;

        UrlConnectionResponse(int status, HttpURLConnection connection, InputStream body) {
            this.status = status;
            this.connection = connection;
            this.body = body;
        }

        @Override
        public int status() {
            return status;
        }

        @Override
        public String header(String name) {
            return connection.getHeaderField(name);
        }

        @Override
        public InputStream body() {
            return body;
        }

        @Override
        public void close() throws IOException {
            body.close();
        }
    }
}
