package com.github.kiulian.downloader.downloader.proxy;

import java.net.Authenticator;
import java.net.PasswordAuthentication;

public class ProxyAuthenticator extends Authenticator {

    private static volatile ProxyAuthenticator instance;

    private final ProxyCredentials proxyCredentials;

    public ProxyAuthenticator(ProxyCredentials proxyCredentials) {
        this.proxyCredentials = proxyCredentials;
    }

    @Override
    public PasswordAuthentication getPasswordAuthentication() {
        return proxyCredentials.getAuthentication(getRequestingHost(), getRequestingPort());
    }

    public static synchronized void setDefault(ProxyAuthenticator authenticator) {
        instance = authenticator;
        Authenticator.setDefault(instance);
    }

    public static synchronized ProxyAuthenticator getDefault() {
        return instance;
    }

    public static void addAuthentication(String host, int port, String userName, String password) {
        if (instance == null) {
            throw new NullPointerException("ProxyAuthenticator instance is null. Use ProxyAuthenticator.setDefault() to init");
        }
        instance.proxyCredentials.addAuthentication(host, port, userName, password);
    }

}
