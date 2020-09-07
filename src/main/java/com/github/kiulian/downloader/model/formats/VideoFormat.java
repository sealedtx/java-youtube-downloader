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
import com.github.kiulian.downloader.model.quality.VideoQuality;

public class VideoFormat extends Format {

    private final int fps;
    private final String qualityLabel;
    private final Integer width;
    private final Integer height;
    private final VideoQuality videoQuality;

    public VideoFormat(JSONObject json, boolean isAdaptive) {
        super(json, isAdaptive);
        fps = json.getInteger("fps");
        qualityLabel = json.getString("qualityLabel");
        if (json.containsKey("size")) {
            String[] split = json.getString("size").split("x");
            width = Integer.parseInt(split[0]);
            height = Integer.parseInt(split[1]);
        } else {
            width = json.getInteger("width");
            height = json.getInteger("height");
        }
        VideoQuality videoQuality = null;
        if (json.containsKey("quality")) {
            try {
                videoQuality = VideoQuality.valueOf(json.getString("quality"));
            } catch (IllegalArgumentException ignore) {
            }
        }
        this.videoQuality = videoQuality;
    }

    @Override
    public String type() {
        return VIDEO;
    }

    public int fps() {
        return fps;
    }

    public VideoQuality videoQuality() {
        return videoQuality != null ? videoQuality : itag.videoQuality();
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
