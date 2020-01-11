package com.github.kiulian.downloader.model.quality;

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

public enum VideoQuality {
    unknown,
    highres, // 3072p
    hd2880p,
    hd2160,
    hd1440,
    hd1080,
    hd720,
    large, // 480p
    medium, // 360p
    small, // 240p
    tiny, // 144p
    noVideo

}
