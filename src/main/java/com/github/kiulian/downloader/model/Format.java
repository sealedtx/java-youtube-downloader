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

import com.alibaba.fastjson.JSONObject;
import com.github.kiulian.downloader.Constants;

public class Format {

    private int itag;
    private String url;
    private String mimeType;
    private String extension;
    private String quality;
    private int bitrate;
    private int averageBitrate;
    private int approxDurationMs;
    private long contentLength;
    private long lastModified;

    public Format(JSONObject json) throws NullPointerException {
        itag = json.getInteger("itag");
        url = json.getString("url").replace("\\u0026", "&");
        mimeType = json.getString("mimeType");
        quality = json.getString("quality");
        bitrate = json.getInteger("bitrate");
        averageBitrate = json.getInteger("averageBitrate");
        approxDurationMs = json.getInteger("approxDurationMs");
        contentLength = json.getLong("contentLength");
        lastModified = json.getLong("lastModified");

        if (mimeType.contains(Constants.MP4))
            extension = Constants.MP4;
        else if (mimeType.contains(Constants.WEBM))
            extension = Constants.WEBM;
        else if (mimeType.contains(Constants.FLV))
            extension = Constants.FLV;
        else if (mimeType.contains(Constants.HLS))
            extension = Constants.HLS;
        else if (mimeType.contains(Constants.THREEGP))
            extension = Constants.THREEGP;
        else if (mimeType.contains(Constants.M4A))
            extension = Constants.MP4;
        else
            extension = Constants.UNKNOWN;

    }

    public int itag() {
        return itag;
    }

    public int bitrate() {
        return bitrate;
    }

    public int averageBitrate() {
        return averageBitrate;
    }

    public String mimeType() {
        return mimeType;
    }

    public int approxDurationMs() {
        return approxDurationMs;
    }

    public String url() {
        return url;
    }

    public String qality() {
        return quality;
    }

    public long contentLength() {
        return contentLength;
    }

    public long lastModified() {
        return lastModified;
    }

    public String extension() {
        return extension;
    }
}
