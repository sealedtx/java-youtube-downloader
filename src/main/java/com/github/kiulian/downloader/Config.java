package com.github.kiulian.downloader;

import com.github.kiulian.downloader.downloader.proxy.ProxyAuthenticator;

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
    private ExecutorService executorService;
    private Proxy proxy;

    public Config() {
        this.headers = new HashMap<>();
        this.maxRetries = DEFAULT_RETRY_ON_FAILURE;
        this.executorService = Executors.newCachedThreadPool(threadFactory);

        header("User-Agent", DEFAULT_USER_AGENT);
        header("Accept-language", DEFAULT_ACCEPT_LANG);
    }

    public Config maxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
        return this;
    }

    public Config header(String key, String value) {
        headers.put(key, value);
        return this;
    }

    public Config proxy(String host, int port) {
        proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
        return this;
    }

    public Config proxy(String host, int port, String userName, String password) {
        if (ProxyAuthenticator.getAuthenticator() == null) {
            throw new NullPointerException("ProxyAuthenticator is not inited. Use ProxyAuthenticator.setProxyAuthenticator() if you need proxy authentication");
        }
        proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
        ProxyAuthenticator.addAuthentication(host, port, userName, password);
        return this;
    }

    public Config executorService(ExecutorService executorService) {
        this.executorService = executorService;
        return this;
    }

    public void proxyAuthenticator(ProxyAuthenticator authenticator) {
        ProxyAuthenticator.setDefault(authenticator);
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public Proxy getProxy() {
        return proxy;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }
}
