package com.github.kiulian.downloader.parser;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.kiulian.downloader.Config;
import com.github.kiulian.downloader.YoutubeException;
import com.github.kiulian.downloader.YoutubeException.BadPageException;
import com.github.kiulian.downloader.downloader.Downloader;
import com.github.kiulian.downloader.downloader.YoutubeCallback;
import com.github.kiulian.downloader.downloader.request.*;
import com.github.kiulian.downloader.downloader.response.Response;
import com.github.kiulian.downloader.downloader.response.ResponseImpl;
import com.github.kiulian.downloader.downloader.response.Webpage;
import com.github.kiulian.downloader.extractor.Extractor;
import com.github.kiulian.downloader.model.playlist.PlaylistInfo;
import com.github.kiulian.downloader.model.search.*;
import com.github.kiulian.downloader.model.subtitles.SubtitlesInfo;
import com.github.kiulian.downloader.model.videos.VideoDetails;
import com.github.kiulian.downloader.model.videos.VideoInfo;
import com.github.kiulian.downloader.model.videos.formats.AudioFormat;
import com.github.kiulian.downloader.model.videos.formats.Format;
import com.github.kiulian.downloader.model.videos.formats.VideoWithAudioFormat;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class TitkotParserImpl implements Parser {

    private final Config config;
    private final Downloader downloader;
    private final Extractor extractor;

    public TitkotParserImpl(Config config, Downloader downloader, Extractor extractor) {
        this.config = config;
        this.downloader = downloader;
        this.extractor = extractor;
        config.setHeader("Referer", "https://www.tiktok.com/");
    }

    @Override
    public Response<VideoInfo> parseVideo(RequestVideoInfo request) {
        if (request.isAsync()) {
            ExecutorService executorService = config.getExecutorService();
            Future<VideoInfo> result = executorService.submit(() -> parseVideo(request.getVideoId(), request.getCallback()));
            return ResponseImpl.fromFuture(result);
        }
        try {
            VideoInfo result = parseVideo(request.getVideoId(), request.getCallback());
            return ResponseImpl.from(result);
        } catch (YoutubeException e) {
            return ResponseImpl.error(e);
        }
    }

    private VideoInfo parseVideo(String videoId, YoutubeCallback<VideoInfo> callback) throws YoutubeException {
        VideoInfo videoInfo = parseVideoWeb(videoId, callback);
        if (callback != null) {
            callback.onFinished(videoInfo);
        }
        return videoInfo;
    }

    private VideoInfo parseVideoWeb(String videoId, YoutubeCallback<VideoInfo> callback) throws YoutubeException {
        String htmlUrl = videoId;
        Response<Webpage> webpageResponse = downloader.downloadWebpageFull(new RequestWebpage(videoId));
        if (!webpageResponse.ok()) {
            YoutubeException e = new YoutubeException.DownloadException(String.format("Could not load url: %s, exception: %s", htmlUrl, webpageResponse.error().getMessage()));
            if (callback != null) {
                callback.onError(e);
            }
            throw e;
        }
        final Webpage webpage = webpageResponse.data();
        final List<String> cookies = webpage.getCookies();

        String tt_chain_token = cookies.stream().filter(it -> it.startsWith("tt_chain_token")).findFirst().orElse("");
        config.setHeader("Cookie", tt_chain_token);

        String html = webpage.getPayload();
        JSONObject playerConfig;
        try {
            playerConfig = extractor.extractPlayerConfigFromHtml(html);
        } catch (YoutubeException e) {
            if (callback != null) {
                callback.onError(e);
            }
            throw e;
        }

        if (!playerConfig.containsKey("itemInfo")) {
            YoutubeException e = new BadPageException("itemInfo not found");
            if (callback != null) {
                callback.onError(e);
            }
            throw e;
        }

        final JSONObject shareMeta = playerConfig.getJSONObject("shareMeta");
        final JSONObject itemStruct = playerConfig.getJSONObject("itemInfo").getJSONObject("itemStruct");
        final JSONObject video = itemStruct.getJSONObject("video");
        final JSONObject music = itemStruct.getJSONObject("music");

        VideoDetails videoDetails = parseVideoDetails(shareMeta, itemStruct);
        List<Format> formats = parseFormats(video, music);
        //        List<SubtitlesInfo> subtitlesInfo = parseCaptions(playerResponse);
        return new VideoInfo(videoDetails, formats, Collections.emptyList());
    }

    private List<Format> parseFormats(JSONObject video, JSONObject music) {
        List<Format> formats = new ArrayList<>();
        formats.add(parseVideoFormat(video));
        formats.add(parseAudioFormat(music));
        return formats;
    }

    private Format parseVideoFormat(JSONObject video) {
        JSONObject json = new JSONObject();
        json.put("url", video.getString("playAddr"));
        json.put("bitrate", video.getString("bitrate"));
        json.put("approxDurationMs", video.getIntValue("duration") * 1000);
        json.put("mimeType", video.getString("format"));
        json.put("width", video.getIntValue("width"));
        json.put("height", video.getIntValue("height"));
        json.put("quality", video.getString("videoQuality"));
        json.put("averageBitrate", video.getString("bitrate"));
        return new VideoWithAudioFormat(json);
    }

    private Format parseAudioFormat(JSONObject music) {
        JSONObject json = new JSONObject();
        json.put("url", music.getString("playUrl"));
        json.put("approxDurationMs", music.getIntValue("duration") * 1000);
        if (music.containsKey("preciseDuration")) {
            double preciseDuration = music.getJSONObject("preciseDuration").getDoubleValue("preciseDuration");
            json.put("approxDurationMs", (long) (preciseDuration * 1000));
        }
        if (music.getString("playUrl").contains("audio_mpeg")) {
            json.put("mimeType", "mp3");
        }
        return new AudioFormat(json);
    }

    private VideoDetails parseVideoDetails(JSONObject shareMeta, JSONObject itemStruct) {
        JSONObject video = itemStruct.getJSONObject("video");
        JSONObject videoDetails = new JSONObject();
        videoDetails.put("title", shareMeta.getString("title"));
        videoDetails.put("shortDescription", shareMeta.getString("desc"));
        if (itemStruct.containsKey("stats")) {
            JSONObject stats = itemStruct.getJSONObject("stats");
            videoDetails.put("viewCount", stats.getIntValue("playCount"));
        }
        videoDetails.put("lengthSeconds", video.getIntValue("duration"));
        videoDetails.put("videoId", itemStruct.getString("id"));
        if (itemStruct.containsKey("author")) {
            JSONObject author = itemStruct.getJSONObject("author");
            videoDetails.put("author", author.getString("nickname"));
        }
        if (itemStruct.containsKey("textExtra")) {
            JSONArray textExtra = itemStruct.getJSONArray("textExtra");
            JSONArray keywords = textExtra.stream().map(it -> ((JSONObject) it).getString("hashtagName")).collect(Collectors.toCollection(JSONArray::new));
            videoDetails.put("keywords", keywords);
        }
        return new VideoDetails(videoDetails, null);
    }


    @Override
    public Response<PlaylistInfo> parsePlaylist(RequestPlaylistInfo request) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public Response<PlaylistInfo> parseChannelsUploads(RequestChannelUploads request) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public Response<List<SubtitlesInfo>> parseSubtitlesInfo(RequestSubtitlesInfo request) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public Response<SearchResult> parseSearchResult(RequestSearchResult request) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public Response<SearchResult> parseSearchContinuation(RequestSearchContinuation request) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public Response<SearchResult> parseSearcheable(RequestSearchable request) {
        throw new RuntimeException("not implemented");
    }

}
