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
import com.github.kiulian.downloader.model.quality.VideoQuality;

public class VideoFormat extends Format {

    private final int fps;
    private final VideoQuality videoQuality;
    private final String qualityLabel;
    private final int width;
    private final int height;
    private final long lastModified;

    public VideoFormat(JSONObject json) throws NullPointerException {
        super(json);
        fps = json.getInteger("fps");
        videoQuality = VideoQuality.valueOf(json.getString("quality"));
        qualityLabel = json.getString("qualityLabel");
        width = json.getInteger("width");
        height = json.getInteger("height");
        lastModified = json.getLong("lastModified");
    }

    @Override
    public String type() {
        return "video";
    }

    public int fps() {
        return fps;
    }

    public VideoQuality videoQuality() {
        return videoQuality;
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

    public long lastModified() {
        return lastModified;
    }
}
