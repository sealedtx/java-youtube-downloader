package com.github.kiulian.downloader.model.search;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

public class SearchResultShelfDetails implements SearchResultItem {

    private String title;
    private List<SearchResultVideoDetails> videos;

    public SearchResultShelfDetails() {}

    public SearchResultShelfDetails(JSONObject json) {
        title = json.getJSONObject("title").getString("simpleText");
        JSONArray jsonItems = json.getJSONObject("content").getJSONObject("verticalListRenderer").getJSONArray("items");
        videos = new ArrayList<>(jsonItems.size());
        for (int i = 0; i < jsonItems.size(); i++) {
            videos.add(new SearchResultVideoDetails(jsonItems.getJSONObject(i).getJSONObject("videoRenderer"), false));
        }
    }

    @Override
    public boolean isShelf() {
        return true;
    }

    @Override
    public String title() {
        return title;
    }

    public List<SearchResultVideoDetails> videos() {
        return videos;
    }

}
