package com.github.kiulian.downloader.model.formats;

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
import com.github.kiulian.downloader.model.quality.AudioQuality;

public class AudioVideoFormat extends AudioFormat {

    private final String qualityLabel;
    private final Integer width;
    private final Integer height;

    public AudioVideoFormat(JSONObject json) {
        super(json);
        qualityLabel = json.getString("qualityLabel");
        width = json.getInteger("width");
        height = json.getInteger("height");
    }

    @Override
    public String type() {
        return AUDIO_VIDEO;
    }

    public String qualityLabel() {
        return qualityLabel;
    }

    public Integer width() {
        return width;
    }

    public Integer height() {
        return height;
    }

}
