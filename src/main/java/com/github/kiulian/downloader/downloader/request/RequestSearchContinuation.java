package com.github.kiulian.downloader.downloader.request;

import com.github.kiulian.downloader.model.search.*;

public class RequestSearchContinuation extends Request<RequestSearchContinuation, SearchResult> {

    private final SearchContinuation continuation;

    public RequestSearchContinuation(SearchResult result) {
        if (!result.hasContinuation()) {
            throw new IllegalArgumentException("Search result must have a continuation");
        }
        this.continuation = ((ContinuatedSearchResult) result).continuation();
    }

    public SearchContinuation continuation() {
        return continuation;
    }
}
