package com.github.kiulian.downloader.internal.http;

import java.io.IOException;

/**
 * A response with a status other than 2xx. The message holds the masked URL and the start of the body.
 */
public final class HttpStatusException extends IOException {

    private final int status;
    private final String retryAfter;
    private final String bodySnippet;

    HttpStatusException(int status, String method, String redactedUrl, String retryAfter, String bodySnippet) {
        super("HTTP " + status + " for " + method + " " + redactedUrl + (bodySnippet.isEmpty() ? "" : ": " + bodySnippet));
        this.status = status;
        this.retryAfter = retryAfter;
        this.bodySnippet = bodySnippet;
    }

    public int status() {
        return status;
    }

    /** Raw {@code Retry-After} header, or {@code null}. */
    public String retryAfter() {
        return retryAfter;
    }

    /** Start of the response body, whitespace collapsed; may be empty. */
    public String bodySnippet() {
        return bodySnippet;
    }
}
