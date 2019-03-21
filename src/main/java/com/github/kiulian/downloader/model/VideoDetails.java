package com.github.kiulian.downloader.model;

/*-
 * #
 * Java youtube video and audio downloader
 *
 * Copyright (C) 2019 Igor Kiulian
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #
 */

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class VideoDetails {

    private String videoId;
    private String title;
    private int lengthSeconds;
    private List<String> keywords;
    private String shortDescription;
    private List<String> thumbnails;
    private String author;
    private int viewCount;
    private int averageRating;
    private boolean isLiveContent;

    public VideoDetails(String videoId) {
        this.videoId = videoId;
        this.title = videoId;
    }

    public void setDetails(JSONObject json) {
        title = json.getString("title");
        lengthSeconds = json.getInteger("lengthSeconds");
        keywords = json.getJSONArray("keywords").toJavaList(String.class);
        shortDescription = json.getString("shortDescription");
        JSONArray jsonThumbnails = json.getJSONObject("thumbnail").getJSONArray("thumbnails");
        thumbnails = new ArrayList<>(jsonThumbnails.size());
        for (int i = 0; i < jsonThumbnails.size(); i++) {
            JSONObject jsonObject = jsonThumbnails.getJSONObject(i);
            if (jsonObject.containsKey("url"))
                thumbnails.add(jsonObject.getString("url"));
        }
        averageRating = json.getInteger("averageRating");
        viewCount = json.getInteger("viewCount");
        author = json.getString("author");
        isLiveContent = json.getBoolean("isLiveContent");
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

    public List<String> keywords() {
        return keywords;
    }

    public String shortDescription() {
        return shortDescription;
    }

    public List<String> thumbnails() {
        return thumbnails;
    }

    public String author() {
        return author;
    }

    public int viewCount() {
        return viewCount;
    }

    public int getAverageRating() {
        return averageRating;
    }

    public boolean liveContent() {
        return isLiveContent;
    }
}
