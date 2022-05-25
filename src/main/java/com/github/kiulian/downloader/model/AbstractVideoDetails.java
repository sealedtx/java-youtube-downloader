package com.github.kiulian.downloader.model;


import java.util.List;

import com.alibaba.fastjson.JSONObject;

public abstract class AbstractVideoDetails {

    protected String videoId;
    private List<String> thumbnails;

    // Subclass specific extraction
    protected int lengthSeconds;
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
        if (json.containsKey("lengthSeconds")) {
            lengthSeconds = json.getIntValue("lengthSeconds");
        }
        thumbnails = Utils.parseThumbnails(json.getJSONObject("thumbnail"));
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
