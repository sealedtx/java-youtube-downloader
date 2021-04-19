package com.github.kiulian.downloader.downloader.proxy;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ProxyAuthenticator extends Authenticator {

    private static volatile ProxyAuthenticator instance;

    private static final Map<String, PasswordAuthentication> credentials = new ConcurrentHashMap<>();

    @Override
    public PasswordAuthentication getPasswordAuthentication() {
        String key = getRequestingHost() + ":" + getRequestingPort();
        return credentials.get(key);
    }

    public static synchronized void setDefault(ProxyAuthenticator authenticator) {
        instance = authenticator;
        Authenticator.setDefault(authenticator);
    }

    public static synchronized ProxyAuthenticator getAuthenticator() {
        return instance;
    }

    public static void addAuthentication(String host, int port, String userName, String password) {
        credentials.put(host+":"+port, new PasswordAuthentication(userName, password.toCharArray()));
    }

}
