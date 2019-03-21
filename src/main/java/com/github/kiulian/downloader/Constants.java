package com.github.kiulian.downloader;

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

public class Constants {

    public static final char[] ILLEGAL_FILENAME_CHARACTERS = {'/', '\n', '\r', '\t', '\0', '\f', '`', '?', '*', '\\', '<', '>', '|', '\"', ':'};

    public static final String AUDIO = "audio";
    public static final String VIDEO = "video";

    public static final String MP4 = "mp4";
    public static final String WEBM = "webm";
    public static final String THREEGP = "3gp";
    public static final String FLV = "flv";
    public static final String HLS = "hls";
    public static final String M4A = "m4a";
    public static final String UNKNOWN = "unknown";

    public static final String HD1080 = "1080p";
    public static final String HD720 = "720p";
    public static final String LARGE480 = "480p";
    public static final String MEDIUM360 = "360p";
    public static final String SMALL240 = "240p";

}
