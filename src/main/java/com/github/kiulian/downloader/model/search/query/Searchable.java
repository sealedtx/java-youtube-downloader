package com.github.kiulian.downloader.model.search.query;

import com.alibaba.fastjson.JSONObject;

public abstract class Searchable {

    protected final String query;
    protected final String searchPath;

    protected abstract String extractQuery(JSONObject json);
    protected abstract String extractSearchPath(JSONObject json);

    public Searchable(JSONObject json) {
        super();
        this.query = extractQuery(json);
        this.searchPath = extractSearchPath(json);
    }

    public String query() {
        return query;
    }

    public String searchPath() {
        return searchPath;
    }

}
