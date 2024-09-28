package com.github.kiulian.downloader;

import com.github.kiulian.downloader.downloader.proxy.ProxyAuthenticator;
import com.github.kiulian.downloader.downloader.proxy.ProxyCredentials;
import com.github.kiulian.downloader.downloader.proxy.ProxyCredentialsImpl;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class Config {
    private static final ThreadFactory threadFactory = new ThreadFactory() {
        private static final String NAME_PREFIX = "yt-downloader-";
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        public Thread newThread(Runnable r) {
            SecurityManager s = System.getSecurityManager();
            ThreadGroup group = s != null ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();

            Thread thread = new Thread(group, r, NAME_PREFIX + threadNumber.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    };

    private static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.121 Safari/537.36";
    private static final String DEFAULT_ACCEPT_LANG = "en-US,en;";
    private static final int DEFAULT_RETRY_ON_FAILURE = 0;

    private Map<String, String> headers;
    private int maxRetries;
    private boolean compressionEnabled;
    private ExecutorService executorService;
    private Proxy proxy;

    private Config(Builder builder) {
        this.headers = builder.headers;
        this.maxRetries = builder.maxRetries;
        this.compressionEnabled = builder.compressionEnabled;
        this.executorService = builder.executorService;
        this.proxy = builder.proxy;
    }

    private Config() {
        this.headers = new HashMap<>();
        this.maxRetries = DEFAULT_RETRY_ON_FAILURE;
        this.compressionEnabled = true;
        this.executorService = Executors.newCachedThreadPool(threadFactory);

        setHeader("User-Agent", DEFAULT_USER_AGENT);
        setHeader("Accept-language", DEFAULT_ACCEPT_LANG);
    }

    static Config buildDefault() {
        return new Config();
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public void setCompressionEnabled(boolean enabled) {
        this.compressionEnabled = enabled;
    }

    public void setHeader(String key, String value) {
        headers.put(key, value);
    }

    public void setProxy(String host, int port) {
        proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
    }

    public void setProxy(String host, int port, String userName, String password) {
        if (ProxyAuthenticator.getDefault() == null) {
            ProxyAuthenticator.setDefault(new ProxyAuthenticator(new ProxyCredentialsImpl()));
        }
        proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
        ProxyAuthenticator.addAuthentication(host, port, userName, password);
    }

    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public void setProxyAuthenticator(ProxyCredentials credentials) {
        ProxyAuthenticator.setDefault(new ProxyAuthenticator(credentials));
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public boolean isCompressionEnabled() {
        return compressionEnabled;
    }

    public Proxy getProxy() {
        return proxy;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public static class Builder {
        private Map<String, String> headers = new HashMap<>();
        private int maxRetries = DEFAULT_RETRY_ON_FAILURE;
        private boolean compressionEnabled = true;
        private ExecutorService executorService;
        private Proxy proxy;

        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public Builder enableCompression(boolean enable) {
            this.compressionEnabled = enable;
            return this;
        }

        public Builder header(String key, String value) {
            headers.put(key, value);
            return this;
        }

        public Builder proxy(String host, int port) {
            proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
            return this;
        }

        public Builder proxy(String host, int port, String userName, String password) {
            if (ProxyAuthenticator.getDefault() == null) {
                ProxyAuthenticator.setDefault(new ProxyAuthenticator(new ProxyCredentialsImpl()));
            }
            proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
            ProxyAuthenticator.addAuthentication(host, port, userName, password);
            return this;
        }

        public Builder executorService(ExecutorService executorService) {
            this.executorService = executorService;
            return this;
        }

        public Builder proxyCredentialsManager(ProxyCredentials credentials) {
            ProxyAuthenticator.setDefault(new ProxyAuthenticator(credentials));
            return this;
        }

        public Config build() {
            if (executorService == null) {
                executorService = Executors.newCachedThreadPool(threadFactory);
            }
            return new Config(this);
        }

    }

}
