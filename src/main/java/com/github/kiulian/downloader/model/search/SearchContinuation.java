package com.github.kiulian.downloader.model.search;

public class SearchContinuation {

    private String token;
    private String clientVersion;
    private String clickTrackingParameters;

    public SearchContinuation(String token, String clientVersion, String clickTrackingParameters) {
        this.token = token;
        this.clientVersion = clientVersion;
        this.clickTrackingParameters = clickTrackingParameters;
    }

    public String token() {
        return token;
    }

    public String clientVersion() {
        return clientVersion;
    }

    public String clickTrackingParameters() {
        return clickTrackingParameters;
    }
}
