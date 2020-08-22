package com.github.kiulian.downloader.model;

/*-
 * #
 * Java youtube video and audio downloader
 *
 * Copyright (C) 2020 Igor Kiulian
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


import java.util.ArrayList;
import java.util.List;

import com.alibaba.fastjson.JSONObject;

public class VideoDetails extends AbstractVideoDetails {

    private List<String> keywords;
    private String shortDescription;
    private long viewCount;
    private int averageRating;
    private boolean isLiveContent;
    private boolean isLive;
    private String liveUrl;

    public VideoDetails() {
    }

    public VideoDetails(JSONObject json, String liveHLSUrl) {
        super(json);
        keywords = json.containsKey("keywords") ? json.getJSONArray("keywords").toJavaList(String.class) : new ArrayList<String>();
        shortDescription = json.getString("shortDescription");
        averageRating = json.getIntValue("averageRating");
        viewCount = json.getLongValue("viewCount");
        isLiveContent = json.getBooleanValue("isLiveContent");
        isLive = json.getBooleanValue("isLive");
        liveUrl = liveHLSUrl;
    }
    
    @Override
    protected String extractAuthor(JSONObject json) {
        return json.getString("author");
    }

    @Override
    protected String extractTitle(JSONObject json) {
        return json.getString("title");
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

    public boolean isLive() {
        return isLive;
    }

    public boolean isLiveContent() {
        return isLiveContent;
    }

    public String liveUrl() {
        return liveUrl;
    }
}
