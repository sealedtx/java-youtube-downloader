package com.github.kiulian.downloader.model.search;

import java.util.LinkedList;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

public class SearchResultPlaylistDetails extends AbstractSearchResultList {

    private String playlistId;
    private int videoCount;

    public SearchResultPlaylistDetails() {}

    public SearchResultPlaylistDetails(JSONObject json) {
        super(json);
        playlistId = json.getString("playlistId");
        JSONArray thumbnailsGroups = json.getJSONArray("thumbnails");
        thumbnails = new LinkedList<>();
        for (int i = 0; i < thumbnailsGroups.size(); i++) {
            JSONArray jsonThumbnails = thumbnailsGroups.getJSONObject(i).getJSONArray("thumbnails");
            for (int j = 0; j < jsonThumbnails.size(); j++) {
                JSONObject jsonObject = jsonThumbnails.getJSONObject(j);
                if (jsonObject.containsKey("url"))
                    thumbnails.add(jsonObject.getString("url"));
            }
        }
        if (json.containsKey("videoCount")) {
            videoCount = Integer.parseInt(json.getString("videoCount"));
        }
    }

    @Override
    public boolean isPlaylist() {
        return true;
    }

    public String playlistId() {
        return playlistId;
    }

    public int videoCount() {
        return videoCount;
    }
}
