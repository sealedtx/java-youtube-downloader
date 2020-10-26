package com.github.kiulian.downloader.model;


import java.util.ArrayList;
import java.util.List;

import com.alibaba.fastjson.JSONObject;
import com.github.kiulian.downloader.YoutubeException;

public class VideoDetails extends AbstractVideoDetails {

    private List<String> keywords;
    private String shortDescription;
    private long viewCount;
    private int averageRating;
    private boolean isLiveContent;
    private String liveUrl;

    public VideoDetails() {
    }

    public VideoDetails(JSONObject json, String liveHLSUrl) {
        super(json);
        title = json.getString("title");
        author = json.getString("author");
        isLive = json.getBooleanValue("isLive");

        keywords = json.containsKey("keywords") ? json.getJSONArray("keywords").toJavaList(String.class) : new ArrayList<String>();
        shortDescription = json.getString("shortDescription");
        averageRating = json.getIntValue("averageRating");
        viewCount = json.getLongValue("viewCount");
        isLiveContent = json.getBooleanValue("isLiveContent");
        liveUrl = liveHLSUrl;
    }

    @Override
    protected void checkDownload() throws YoutubeException.LiveVideoException {
        if (isLive || (isLiveContent && lengthSeconds() == 0))
            throw new YoutubeException.LiveVideoException("Can not download live stream");
    }

    public List<String> keywords() {
        return keywords;
    }

    public String description() {
        return shortDescription;
    }

    public long viewCount() {
        return viewCount;
    }

    public int averageRating() {
        return averageRating;
    }

    public boolean isLiveContent() {
        return isLiveContent;
    }

    public String liveUrl() {
        return liveUrl;
    }
}
