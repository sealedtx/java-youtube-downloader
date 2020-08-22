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

public class YoutubePlaylist {

    private PlaylistDetails details;
    
    private List<PlaylistVideo> videos;

    public YoutubePlaylist(PlaylistDetails details, List<PlaylistVideo> videos) {
        this.details = details;
        this.videos = videos;
    }

    public PlaylistDetails details() {
        return details;
    }
    
    public List<PlaylistVideo> videos() {
        return videos;
    }

    public PlaylistVideo findVideoById(String videoId) {
        for (PlaylistVideo video : videos) {
            if (video.videoId().equals(videoId))
                return video;
        }
        return null;
    }
    
    public PlaylistVideo findVideoByIndex(int index) {
        for (PlaylistVideo video : videos) {
            if (video.index() == index)
                return video;
        }
        return null;
    }
}
