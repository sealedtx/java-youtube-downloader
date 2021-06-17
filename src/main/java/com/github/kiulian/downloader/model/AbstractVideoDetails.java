package com.github.kiulian.downloader.model;


import java.util.ArrayList;
import java.util.List;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

public abstract class AbstractVideoDetails {

    protected String videoId;
    private int lengthSeconds;
    private List<String> thumbnails;

    // Subclass specific extraction
    protected String title;
    protected String author;
    protected boolean isLive;

    protected boolean isDownloadable() {
        return (!isLive() && lengthSeconds() != 0);
    }

    public AbstractVideoDetails() {
    }

    public AbstractVideoDetails(JSONObject json) {
        videoId = json.getString("videoId");
        lengthSeconds = json.getIntValue("lengthSeconds");
        JSONArray jsonThumbnails = json.getJSONObject("thumbnail").getJSONArray("thumbnails");
        thumbnails = new ArrayList<>(jsonThumbnails.size());
        for (int i = 0; i < jsonThumbnails.size(); i++) {
            JSONObject jsonObject = jsonThumbnails.getJSONObject(i);
            if (jsonObject.containsKey("url"))
                thumbnails.add(jsonObject.getString("url"));
        }
    }

    public String videoId() {
        return videoId;
    }

    public String title() {
        return title;
    }

    public int lengthSeconds() {
        return lengthSeconds;
    }

    public List<String> thumbnails() {
        return thumbnails;
    }

    public String author() {
        return author;
    }

    public boolean isLive() {
        return isLive;
    }
}
