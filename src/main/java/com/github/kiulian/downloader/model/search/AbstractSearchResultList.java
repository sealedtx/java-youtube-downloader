package com.github.kiulian.downloader.model.search;

import java.util.List;

import com.alibaba.fastjson.JSONObject;
import com.github.kiulian.downloader.model.Utils;

public abstract class AbstractSearchResultList implements SearchResultItem {

    private String title;
    protected List<String> thumbnails;
    private String author;

    public AbstractSearchResultList() {}

    public AbstractSearchResultList(JSONObject json) {
        title = json.getJSONObject("title").getString("simpleText");
        author = Utils.parseRuns(json.getJSONObject("shortBylineText"));
    }

    @Override
    public String title() {
        return title;
    }

    public List<String> thumbnails() {
        return thumbnails;
    }

    public String author() {
        return author;
    }
}
