package com.github.kiulian.downloader.parser;

import com.alibaba.fastjson.JSONObject;
import com.github.kiulian.downloader.YoutubeException;
import com.github.kiulian.downloader.model.VideoDetails;
import com.github.kiulian.downloader.model.formats.Format;

import java.io.IOException;
import java.util.List;

public interface Parser {

    JSONObject getPlayerConfig(String htmlUrl) throws IOException, YoutubeException;

    VideoDetails getVideoDetails(JSONObject config);

    String getJsUrl(JSONObject config) throws YoutubeException;

    List<Format> parseFormats(JSONObject json) throws YoutubeException;
}
