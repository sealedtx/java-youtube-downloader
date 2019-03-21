package com.github.kiulian.downloader.model.formats;

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

import com.alibaba.fastjson.JSONObject;

public class AudioVideoFormat extends Format {

    private String qualityLabel;
    private int width;
    private int height;
    private String audioQuality;
    private int audioSampleRate;

    public AudioVideoFormat(JSONObject json) throws NullPointerException {
        super(json);
        qualityLabel = json.getString("qualityLabel");
        width = json.getInteger("width");
        height = json.getInteger("height");
        audioQuality = json.getString("audioQuality");
        audioSampleRate = json.getInteger("audioSampleRate");
    }

    public String qualityLabel() {
        return qualityLabel;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public String audioQuality() {
        return audioQuality;
    }

    public int audioSampleRate() {
        return audioSampleRate;
    }
}
