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

public class Extension {

    public static final Extension MP4 = new Extension("mp4");
    public static final Extension WEBM = new Extension("webm");
    public static final Extension THREEGP = new Extension("3gp");
    public static final Extension FLV = new Extension("flv");
    public static final Extension HLS = new Extension("hls");
    public static final Extension M4A = new Extension("m4a");
    public static final Extension UNKNOWN = new Extension("unknown");

    private final String value;

    private Extension(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
