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


public class PlaylistDetails {

    private String playlistId;
    private String title;
    private String author;
    private int videoCount;
    private int views;

    public PlaylistDetails() {
    }

    public PlaylistDetails(String playlistId, String title, String author, int videoCount, int views) {
        super();
        this.playlistId = playlistId;
        this.title = title;
        this.author = author;
        this.videoCount = videoCount;
        this.views = views;
    }

    public String playlistId() {
        return playlistId;
    }

    public String title() {
        return title;
    }

    public String author() {
        return author;
    }
    
    public int videoCount() {
        return videoCount;
    }
    
    public int views() {
        return views;
    }
}
