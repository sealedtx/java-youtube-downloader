package com.github.kiulian.downloader.downloader.proxy;

import java.net.PasswordAuthentication;

public interface ProxyCredentials {

    void addAuthentication(String host, int port, String userName, String password);

    PasswordAuthentication getAuthentication(String host, int port);
}
