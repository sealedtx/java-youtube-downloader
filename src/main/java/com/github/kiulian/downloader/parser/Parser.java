package com.github.kiulian.downloader.parser;

import java.util.List;

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

import com.alibaba.fastjson.JSONObject;
import com.github.kiulian.downloader.YoutubeException;
import com.github.kiulian.downloader.cipher.CipherFactory;
import com.github.kiulian.downloader.extractor.Extractor;
import com.github.kiulian.downloader.model.VideoDetails;
import com.github.kiulian.downloader.model.formats.Format;
import com.github.kiulian.downloader.model.playlist.PlaylistDetails;
import com.github.kiulian.downloader.model.playlist.PlaylistVideo;
import com.github.kiulian.downloader.model.subtitles.SubtitlesInfo;

public interface Parser {

    Extractor getExtractor();

    CipherFactory getCipherFactory();
    
    /* Video */

    JSONObject getPlayerConfig(String htmlUrl) throws YoutubeException;
    
    VideoDetails getVideoDetails(JSONObject config);

    String getJsUrl(JSONObject config) throws YoutubeException;

    List<SubtitlesInfo> getSubtitlesInfoFromCaptions(JSONObject config);

    List<SubtitlesInfo> getSubtitlesInfo(String videoId) throws YoutubeException;

    List<Format> parseFormats(JSONObject json) throws YoutubeException;
    
    /* Playlist */
    
    JSONObject getInitialData(String htmlUrl) throws YoutubeException;
    
    PlaylistDetails getPlaylistDetails(String playlistId, JSONObject initialData);
    
    List<PlaylistVideo> getPlaylistVideos(JSONObject initialData, int size)  throws YoutubeException;
}
