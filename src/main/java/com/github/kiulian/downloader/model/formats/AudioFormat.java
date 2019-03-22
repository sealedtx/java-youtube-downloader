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
import com.github.kiulian.downloader.model.quality.AudioQuality;

public class AudioFormat extends Format {

    private final AudioQuality audioQuality;
    private final Integer audioSampleRate;

    public AudioFormat(JSONObject json) throws Exception {
        super(json);
        audioQuality = AudioQuality.valueOf(json.getString("audioQuality").split("_")[2].toLowerCase());
        audioSampleRate = json.getInteger("audioSampleRate");
    }

    @Override
    public String type() {
        return "audio";
    }

    public AudioQuality audioQuality() {
        return audioQuality;
    }

    public Integer audioSampleRate() {
        return audioSampleRate;
    }
}
