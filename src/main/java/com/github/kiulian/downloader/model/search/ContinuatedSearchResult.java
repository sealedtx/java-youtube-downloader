package com.github.kiulian.downloader.model.search;

import java.util.List;

public class ContinuatedSearchResult extends SearchResult {

    private SearchContinuation continuation;

    public ContinuatedSearchResult(long estimatedResults, List<SearchResultItem> items, SearchContinuation continuation) {
        super(estimatedResults, items);
        this.continuation = continuation;
    }

    public boolean hasNext() {
        return true;
    }

    public SearchContinuation continuation() {
        return continuation;
    }
}
