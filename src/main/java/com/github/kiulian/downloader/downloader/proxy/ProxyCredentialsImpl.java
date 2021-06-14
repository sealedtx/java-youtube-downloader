package com.github.kiulian.downloader.downloader.proxy;

import java.net.PasswordAuthentication;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ProxyCredentialsImpl implements ProxyCredentials {

    private final Map<String, PasswordAuthentication> credentials = new ConcurrentHashMap<>();

    @Override
    public PasswordAuthentication getAuthentication(String host, int port) {
        String key = host + ":" + port;
        return credentials.get(key);
    }

    @Override
    public void addAuthentication(String host, int port, String userName, String password) {
        credentials.put(host + ":" + port, new PasswordAuthentication(userName, password.toCharArray()));
    }
}
