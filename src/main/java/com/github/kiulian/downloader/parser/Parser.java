package com.github.kiulian.downloader.parser;


import com.alibaba.fastjson.JSONObject;
import com.github.kiulian.downloader.YoutubeException;
import com.github.kiulian.downloader.cipher.CipherFactory;
import com.github.kiulian.downloader.extractor.Extractor;
import com.github.kiulian.downloader.model.VideoDetails;
import com.github.kiulian.downloader.model.formats.Format;
import com.github.kiulian.downloader.model.playlist.PlaylistDetails;
import com.github.kiulian.downloader.model.playlist.PlaylistVideoDetails;
import com.github.kiulian.downloader.model.subtitles.SubtitlesInfo;

import java.util.List;

public interface Parser {

    Extractor getExtractor();

    CipherFactory getCipherFactory();

    /* Video */

    JSONObject getPlayerConfig(String htmlUrl) throws YoutubeException;

    String getClientVersion(JSONObject config);

    VideoDetails getVideoDetails(JSONObject config);

    String getJsUrl(JSONObject config) throws YoutubeException;

    List<SubtitlesInfo> getSubtitlesInfoFromCaptions(JSONObject config);

    List<SubtitlesInfo> getSubtitlesInfo(String videoId) throws YoutubeException;

    List<Format> parseFormats(JSONObject json) throws YoutubeException;

    /* Playlist */

    JSONObject getInitialData(String htmlUrl) throws YoutubeException;

    PlaylistDetails getPlaylistDetails(String playlistId, JSONObject initialData);

    List<PlaylistVideoDetails> getPlaylistVideos(JSONObject initialData, int videoCount) throws YoutubeException;
}
