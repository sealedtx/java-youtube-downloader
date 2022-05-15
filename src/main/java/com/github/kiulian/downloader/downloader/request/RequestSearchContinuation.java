package com.github.kiulian.downloader.downloader.request;

import com.github.kiulian.downloader.model.search.ContinuatedSearchResult;
import com.github.kiulian.downloader.model.search.SearchResult;

public class RequestSearchContinuation extends Request<RequestSearchContinuation, SearchResult> {

    private ContinuatedSearchResult result;

    public RequestSearchContinuation(SearchResult result) {
        super();
        if (!result.hasNext()) {
            throw new IllegalArgumentException("Search result must be continuated");
        }
        this.result = (ContinuatedSearchResult) result;
    }

    public ContinuatedSearchResult result() {
        return result;
    }
}
