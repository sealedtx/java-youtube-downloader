package com.github.kiulian.downloader;


import com.alibaba.fastjson.JSONObject;
import com.github.kiulian.downloader.cipher.CipherFunction;
import com.github.kiulian.downloader.model.VideoDetails;
import com.github.kiulian.downloader.model.YoutubeVideo;
import com.github.kiulian.downloader.model.formats.Format;
import com.github.kiulian.downloader.model.playlist.PlaylistDetails;
import com.github.kiulian.downloader.model.playlist.PlaylistVideoDetails;
import com.github.kiulian.downloader.model.playlist.YoutubePlaylist;
import com.github.kiulian.downloader.model.subtitles.SubtitlesInfo;
import com.github.kiulian.downloader.parser.DefaultParser;
import com.github.kiulian.downloader.parser.Parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;
import java.util.Scanner;

public class YoutubeDownloader {

    private Parser parser;

    public YoutubeDownloader() {
        this.parser = new DefaultParser();
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
        ytPlayerConfig.put("yt-downloader-videoId", videoId);

        VideoDetails videoDetails = parser.getVideoDetails(ytPlayerConfig);

        List<Format> formats = parser.parseFormats(ytPlayerConfig);

        List<SubtitlesInfo> subtitlesInfo = parser.getSubtitlesInfoFromCaptions(ytPlayerConfig);

        String clientVersion = parser.getClientVersion(ytPlayerConfig);

        return new YoutubeVideo(videoDetails, formats, subtitlesInfo, clientVersion);
    }

    public YoutubePlaylist getPlaylist(String playlistId) throws YoutubeException {
        String htmlUrl = "https://www.youtube.com/playlist?list=" + playlistId;

        JSONObject ytInitialData = parser.getInitialData(htmlUrl);
        if (!ytInitialData.containsKey("metadata")) {
            throw new YoutubeException.BadPageException("Invalid initial data json");
        }

        PlaylistDetails playlistDetails = parser.getPlaylistDetails(playlistId, ytInitialData);

        List<PlaylistVideoDetails> videos = parser.getPlaylistVideos(ytInitialData, playlistDetails.videoCount());

        return new YoutubePlaylist(playlistDetails, videos);
    }

    public YoutubePlaylist getUploadsPlaylist(String channelId) throws IOException, YoutubeException {
        URL channelLink;
        if (channelId.length() == 24 && channelId.startsWith("UC")) {
            channelLink = new URL("https://www.youtube.com/channel/" + channelId + "/videos?view=57");
        } else {
            channelLink = new URL("https://www.youtube.com/c/" + channelId + "/videos?view=57");
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(channelLink.openStream()));
        String line;
        while ((line = br.readLine()) != null) {
            Scanner scan = new Scanner(line);
            scan.useDelimiter("list=");
            while (scan.hasNext()) {
                String pId = scan.next();
                if (pId.startsWith("UU")) {
                    return getPlaylist(pId.substring(0, 24));
                }
            }
        }
        throw new YoutubeException.BadPageException("Upload Playlist not found");
    }

    public List<SubtitlesInfo> getVideoSubtitles(String videoId) throws YoutubeException {
        return parser.getSubtitlesInfo(videoId);
    }
}
