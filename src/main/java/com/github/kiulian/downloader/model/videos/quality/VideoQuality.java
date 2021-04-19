package com.github.kiulian.downloader.model.videos.quality;

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
    unknown(0),
    noVideo(0),
    tiny(1),
    small(2), // 240p
    medium(3), // 360p
    large(4), // 480p
    hd720(5),
    hd1080(6),
    hd1440(7),
    hd2160(8),
    hd2880p(9),
    highres(10); // 3072p

    private final Integer order;

    VideoQuality(int order) {
        this.order = order;
    }

    public int compare(VideoQuality quality) {
        if (this == quality) return 0;
        return order.compareTo(quality.order);
    }

}
