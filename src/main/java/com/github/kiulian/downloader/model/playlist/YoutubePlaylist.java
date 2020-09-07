package com.github.kiulian.downloader.model.playlist;

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


import java.util.List;

import com.github.kiulian.downloader.model.Filter;

public class YoutubePlaylist {

    private PlaylistDetails details;
    private List<PlaylistVideoDetails> videos;

    public YoutubePlaylist(PlaylistDetails details, List<PlaylistVideoDetails> videos) {
        this.details = details;
        this.videos = videos;
    }

    public PlaylistDetails details() {
        return details;
    }

    public List<PlaylistVideoDetails> videos() {
        return videos;
    }

    public PlaylistVideoDetails findVideoById(String videoId) {
        for (PlaylistVideoDetails video : videos) {
            if (video.videoId().equals(videoId))
                return video;
        }
        return null;
    }

    public List<PlaylistVideoDetails> findVideos(Filter<PlaylistVideoDetails> filter) {
        return filter.select(videos);
    }
}
