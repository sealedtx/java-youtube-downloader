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

public class AudioFormat extends Format {

    private String audioQuality;
    private int audioSampleRate;

    public AudioFormat(JSONObject json) throws NullPointerException {
        super(json);
        audioQuality = json.getString("audioQuality");
        audioSampleRate = json.getInteger("audioSampleRate");
    }

    public String audioQuality() {
        return audioQuality;
    }

    public int audioSampleRate() {
        return audioSampleRate;
    }
}
