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
import com.github.kiulian.downloader.model.Extension;
import com.github.kiulian.downloader.model.Itag;

public abstract class Format {

    protected Itag itag;
    private final String url;
    private final String mimeType;
    private final Extension extension;
    private final Integer bitrate;
    private final Long contentLength;
    private final Long lastModified;

    protected Format(JSONObject json) throws Exception {
        try {
            itag = Itag.valueOf("i" + json.getInteger("itag"));
        } catch (ExceptionInInitializerError e) {
            e.printStackTrace();
            itag = Itag.unknown;
            itag.setId(json.getIntValue("itag"));
        }
        url = json.getString("url").replace("\\u0026", "&");
        mimeType = (String) json.getOrDefault("type", "");
        bitrate = json.getInteger("bitrate");
        contentLength = json.getLong("clen");
        lastModified = json.getLong("lmt");

        if (mimeType.contains(Extension.MP4.value()))
            extension = Extension.MP4;
        else if (mimeType.contains(Extension.WEBM.value()))
            extension = Extension.WEBM;
        else if (mimeType.contains(Extension.FLV.value()))
            extension = Extension.FLV;
        else if (mimeType.contains(Extension.HLS.value()))
            extension = Extension.HLS;
        else if (mimeType.contains(Extension.THREEGP.value()))
            extension = Extension.THREEGP;
        else if (mimeType.contains(Extension.M4A.value()))
            extension = Extension.MP4;
        else
            extension = Extension.UNKNOWN;

    }

    public abstract String type();

    public Itag itag() {
        return itag;
    }

    public Integer bitrate() {
        return bitrate;
    }

    public String mimeType() {
        return mimeType;
    }

    public String url() {
        return url;
    }

    public Long contentLength() {
        return contentLength;
    }

    public long lastModified() {
        return lastModified;
    }

    public Extension extension() {
        return extension;
    }
}
