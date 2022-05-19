package com.github.kiulian.downloader.downloader.request;

import com.github.kiulian.downloader.model.search.SearchResult;
import com.github.kiulian.downloader.model.search.query.Searchable;

public class RequestSearchable extends Request<RequestSearchable, SearchResult> {

    private final String searchPath;

    public RequestSearchable(Searchable searchable) {
        this.searchPath = searchable.searchPath();
    }

    public String searchPath() {
        return searchPath;
    }

}
