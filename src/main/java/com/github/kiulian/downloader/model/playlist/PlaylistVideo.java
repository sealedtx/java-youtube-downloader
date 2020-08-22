package com.github.kiulian.downloader.model.playlist;

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


import com.alibaba.fastjson.JSONObject;
import com.github.kiulian.downloader.model.AbstractVideoDetails;

public class PlaylistVideo extends AbstractVideoDetails {
    
    private int index;
    private boolean playable;

    public PlaylistVideo() {
    }

    public PlaylistVideo(JSONObject json) {
        super(json);
        if (json.containsKey("index")) {
            index = json.getJSONObject("index").getIntValue("simpleText");
        }
        playable = json.getBooleanValue("isPlayable");
    }
    
    @Override
    protected String extractAuthor(JSONObject json) {
        if (!json.containsKey("shortBylineText")) {
            return null;
        }
        return json.getJSONObject("shortBylineText").getJSONArray("runs").getJSONObject(0).getString("text");
    }

    @Override
    protected String extractTitle(JSONObject json) {
        JSONObject jsonTitle = json.getJSONObject("title");
        String title = jsonTitle.getString("simpleText");
        if (title == null) {
            title = jsonTitle.getJSONArray("runs").getJSONObject(0).getString("text");
        }
        return title;
    }
    
    public int index() {
        return index;
    }
    
    public boolean playable() {
        return playable;
    }
}
