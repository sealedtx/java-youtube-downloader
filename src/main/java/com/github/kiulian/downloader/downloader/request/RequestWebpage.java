package com.github.kiulian.downloader.downloader.request;


public class RequestWebpage extends RequestRaw<RequestWebpage> {

    protected final String url;
    private final String method;
    private final String body;

    public RequestWebpage(String url) {
        this(url, "GET", null);
    }

    public RequestWebpage(String url, String method, String body) {
        this.url = url;
        this.method = method;
        this.body = body;
    }

    @Override
    public String getDownloadUrl() {
        return url;
    }

    public String getMethod() {
        return method;
    }

    public String getBody() {
        return body;
    }
}
