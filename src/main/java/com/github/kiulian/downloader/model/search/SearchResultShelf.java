package com.github.kiulian.downloader.model.search;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

public class SearchResultShelf implements SearchResultItem {

    private final String title;
    private final List<SearchResultVideoDetails> videos;

    public SearchResultShelf(JSONObject json) {
        title = json.getJSONObject("title").getString("simpleText");
        JSONObject jsonContent = json.getJSONObject("content");
        
        // verticalListRenderer / horizontalMovieListRenderer
        String contentRendererKey = jsonContent.keySet().iterator().next();
        boolean isMovieShelf = contentRendererKey.contains("Movie");
        JSONArray jsonItems = jsonContent.getJSONObject(contentRendererKey).getJSONArray("items");
        videos = new ArrayList<>(jsonItems.size());
        for (int i = 0; i < jsonItems.size(); i++) {
            JSONObject jsonItem = jsonItems.getJSONObject(i);
            String itemRendererKey = jsonItem.keySet().iterator().next();
            videos.add(new SearchResultVideoDetails(jsonItem.getJSONObject(itemRendererKey), isMovieShelf));
        }
    }

    @Override
    public SearchResultItemType type() {
        return SearchResultItemType.SHELF;
    }

    @Override
    public SearchResultShelf asShelf() {
        return this;
    }

    @Override
    public String title() {
        return title;
    }

    public List<SearchResultVideoDetails> videos() {
        return videos;
    }

}
