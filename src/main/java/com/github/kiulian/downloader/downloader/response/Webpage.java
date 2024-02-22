package com.github.kiulian.downloader.downloader.response;

import java.util.List;

public class Webpage {

    final String payload;
    final List<String> cookies;

    public Webpage(String payload, List<String> cookies) {
        this.payload = payload;
        this.cookies = cookies;
    }

    public String getPayload() {
        return payload;
    }

    public List<String> getCookies() {
        return cookies;
    }
}
