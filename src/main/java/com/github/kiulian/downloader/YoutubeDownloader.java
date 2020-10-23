package com.github.kiulian.downloader;

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
import com.github.kiulian.downloader.cipher.CipherFunction;
import com.github.kiulian.downloader.model.ProxyWrapper;
import com.github.kiulian.downloader.model.VideoDetails;
import com.github.kiulian.downloader.model.YoutubeVideo;
import com.github.kiulian.downloader.model.formats.Format;
import com.github.kiulian.downloader.model.playlist.PlaylistDetails;
import com.github.kiulian.downloader.model.playlist.PlaylistVideoDetails;
import com.github.kiulian.downloader.model.playlist.YoutubePlaylist;
import com.github.kiulian.downloader.model.subtitles.SubtitlesInfo;
import com.github.kiulian.downloader.parser.DefaultParser;
import com.github.kiulian.downloader.parser.Parser;

import java.util.List;

public class YoutubeDownloader {

    private Parser parser;

    public ProxyWrapper proxyWrapper;

    public YoutubeDownloader() {
        this.parser = new DefaultParser(this);
    }

    public YoutubeDownloader(ProxyWrapper proxyWrapper) {
        this.proxyWrapper = proxyWrapper;
        this.parser = new DefaultParser(this);
    }

    public YoutubeDownloader(Parser parser) {
        this.parser = parser;
    }

    public void setParserRequestProperty(String key, String value) {
        parser.getExtractor().setRequestProperty(key, value);
    }

    public void setParserRetryOnFailure(int retryOnFailure) {
        parser.getExtractor().setRetryOnFailure(retryOnFailure);
    }

    public void addCipherFunctionPattern(int priority, String regex) {
        parser.getCipherFactory().addInitialFunctionPattern(priority, regex);
    }

    public void addCipherFunctionEquivalent(String regex, CipherFunction function) {
        parser.getCipherFactory().addFunctionEquivalent(regex, function);
    }

    public YoutubeVideo getVideo(String videoId) throws YoutubeException {
        String htmlUrl = "https://www.youtube.com/watch?v=" + videoId;

        JSONObject ytPlayerConfig = parser.getPlayerConfig(htmlUrl);

        VideoDetails videoDetails = parser.getVideoDetails(ytPlayerConfig);

        List<Format> formats = parser.parseFormats(ytPlayerConfig);

        List<SubtitlesInfo> subtitlesInfo = parser.getSubtitlesInfoFromCaptions(ytPlayerConfig);
        
        String clientVersion = parser.getClientVersion(ytPlayerConfig);

        YoutubeVideo yt = new YoutubeVideo(videoDetails, formats, subtitlesInfo, clientVersion);
        yt.proxyWrapper = proxyWrapper;
        
        return yt;
    }

    public YoutubePlaylist getPlaylist(String playlistId) throws YoutubeException {
        String htmlUrl = "https://www.youtube.com/playlist?list=" + playlistId;

        JSONObject ytInitialData = parser.getInitialData(htmlUrl);
        
        PlaylistDetails playlistDetails = parser.getPlaylistDetails(playlistId, ytInitialData);

        List<PlaylistVideoDetails> videos = parser.getPlaylistVideos(ytInitialData, playlistDetails.videoCount());
        
        return new YoutubePlaylist(playlistDetails, videos);
    }

    public List<SubtitlesInfo> getVideoSubtitles(String videoId) throws YoutubeException {
        return parser.getSubtitlesInfo(videoId);
    }
}
