package com.github.kiulian.tools;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.github.kiulian.downloader.Config;
import com.github.kiulian.downloader.YoutubeException;
import com.github.kiulian.downloader.downloader.Downloader;
import com.github.kiulian.downloader.downloader.DownloaderImpl;
import com.github.kiulian.downloader.downloader.YoutubeCallback;
import com.github.kiulian.downloader.downloader.request.RequestVideoInfo;
import com.github.kiulian.downloader.downloader.request.RequestWebpage;
import com.github.kiulian.downloader.downloader.response.Response;
import com.github.kiulian.downloader.downloader.response.ResponseImpl;
import com.github.kiulian.downloader.model.videos.VideoDetails;
import com.github.kiulian.downloader.model.videos.VideoInfo;
import com.github.kiulian.downloader.model.videos.formats.Format;
import com.github.kiulian.downloader.model.videos.formats.VideoWithAudioFormat;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NhkParser {

    static String API_KEY = "EJfK8jdS57GqlupFgAfAAwr573q01y6k";
    static String API_URL_TEMPLATE = "https://api.nhk.or.jp/nhkworld/%sod%slist/v7a/%s/%s/%s/all%s.json";
    static String BASE_URL_REGEX = "https?:\\/\\/www3\\.nhk\\.or\\.jp\\/nhkworld\\/(?<lang>[a-z]{2})\\/ondemand";
    static String TYPE_REGEX = "\\/(?<type>video|audio)\\/";

    static String VALID_URL = String.format("%s%s(?<id>\\d{7}|[^\\/]+?-\\d{8}-[0-9a-z]+)\\/?", BASE_URL_REGEX, TYPE_REGEX);

    private final Config config;
    private final Downloader downloader;
    private final PikselParser pikselParser;

    public NhkParser(PikselParser pikselParser, Config config, Downloader downloader) {
        this.pikselParser = pikselParser;
        this.config = config;
        this.downloader = downloader;
    }

    public Response<VideoInfo> parseVideo(RequestVideoInfo request) {
        if (request.isAsync()) {
            ExecutorService executorService = config.getExecutorService();
            Future<VideoInfo> result = executorService.submit(() -> parseVideoInternal(request));
            return ResponseImpl.fromFuture(result);
        }
        try {
            VideoInfo result = parseVideoInternal(request);
            return ResponseImpl.from(result);
        } catch (YoutubeException e) {
            return ResponseImpl.error(e);
        }
    }

    private VideoInfo parseVideoInternal(RequestVideoInfo request) throws YoutubeException {
        String url = request.getVideoId();
        YoutubeCallback<VideoInfo> callback = request.getCallback();

        Pattern pattern = Pattern.compile(VALID_URL);

        Matcher matcher = pattern.matcher(url);
        if (!matcher.matches()) {
            YoutubeException e = new YoutubeException.BadPageException(String.format("Not valid url: %s", url));
            if (callback != null) {
                callback.onError(e);
            }
            throw e;
        }

        String lang = matcher.group("lang");
        String mType = matcher.group("type");
        String episodeId = matcher.group("id");
        if (isDigit(episodeId)) {
            episodeId = episodeId.substring(0, 4) + "-" + episodeId.substring(4);
        }
        String videoId = episodeId + "-" + lang;

        boolean isVideo = mType.equals("video");
        if (!isVideo) {
            YoutubeException e = new YoutubeException.BadPageException("Parsing audio not supported yet");
            if (callback != null) {
                callback.onError(e);
            }
            throw e;
        }

        JSONObject episode = callApi(episodeId, lang, isVideo, true, episodeId.substring(4).equals("9999"), callback)
                .getJSONObject(0);
        VideoDetails videoDetails = parseVideoDetails(episode, videoId);

        String vodId = episode.getString("vod_id");
        String pikselVideoUrl = "https://player.piksel.com/v/refid/nhkworld/prefid/" + vodId;

        RequestVideoInfo requestPikselParse = new RequestVideoInfo(pikselVideoUrl);
        Response<VideoInfo> pikselResponse = pikselParser.parseVideo(requestPikselParse);
        if (pikselResponse.ok()) {
            // TODO: update PikselParser videoDetails with additional data from NhkParser (more thumbnails)
            return pikselResponse.data();
        }
        Throwable error = pikselResponse.error();
        YoutubeException e = new YoutubeException.BadPageException("Could not parse piksel video: " + error.getMessage());
        if (callback != null) {
            callback.onError(e);
        }
        throw e;
    }

    private JSONArray callApi(String episodeId, String lang, boolean isVideo, boolean isEpisode, boolean isClip, YoutubeCallback<VideoInfo> callback) throws YoutubeException {
        String htmlUrl = String.format(API_URL_TEMPLATE,
                (isVideo ? "v" : "r"),
                (isClip ? "clip" : "esd"),
                (isEpisode ? "episode" : "program"),
                episodeId,
                lang,
                (isVideo ? "/all" : ""));
        htmlUrl += "?apikey=" + API_KEY;

        Response<String> response = downloader.downloadWebpage(new RequestWebpage(htmlUrl));
        if (!response.ok()) {
            YoutubeException e = new YoutubeException.DownloadException(String.format("Could not load url: %s, exception: %s", htmlUrl, response.error().getMessage()));
            if (callback != null) {
                callback.onError(e);
            }
            throw e;
        }

        String resultingString = response.data();
        try {
            JSONObject responseJson = JSON.parseObject(resultingString);
            return responseJson.getJSONObject("data").getJSONArray("episodes");
        } catch (JSONException ignore) {
            YoutubeException e = new YoutubeException.BadPageException("Could not parse json response for url: " + htmlUrl + " response: " + resultingString);
            if (callback != null) {
                callback.onError(e);
            }
            throw e;
        }
    }

    private VideoDetails parseVideoDetails(JSONObject episode, String videoId) {
        String title = episode.containsKey("sub_title_clean") ? episode.getString("sub_title_clean") : episode.getString("sub_title");
        String series = episode.containsKey("title_clean") ? episode.getString("title_clean") : episode.getString("title");
        String description = episode.containsKey("description_clean") ? episode.getString("description_clean") : episode.getString("description");

        JSONArray thumbnails = new JSONArray();
        HashMap<String, double[]> thumbnailsSizes = new HashMap<>();
        thumbnailsSizes.put("", new double[]{640, 360});
        thumbnailsSizes.put("_l", new double[]{1280, 720});
        thumbnailsSizes.forEach((prefix, size) -> {
            String imagePath = episode.getString("image" + prefix);
            if (imagePath != null) {
                thumbnails.add(new JSONObject()
                        .fluentPut("url", "https://www3.nhk.or.jp" + imagePath)
                );
            }
        });

        JSONObject info = new JSONObject();
        info.put("videoId", videoId);
        info.put("lengthSeconds", episode.getIntValue("movie_duration"));
        info.put("title", series != null ? series + " - " + title : title);
        info.put("keywords", episode.getJSONArray("tags"));
        info.put("shortDescription", description);
        info.put("thumbnail",  new JSONObject().fluentPut("thumbnails", thumbnails));
        return new VideoDetails(info, null);
    }

    private static boolean isDigit(String numberString) {
        try {
            Integer.parseInt(numberString);
            return true;
        } catch (NumberFormatException ignore) {
            return false;
        }
    }

    // example of usage
    public static void main(String[] args) {
        String url = "https://www3.nhk.or.jp/nhkworld/en/ondemand/video/6045005/";

        Config config = new Config.Builder().build();
        DownloaderImpl downloader = new DownloaderImpl(config);
        PikselParser pikselParser = new PikselParser(config, downloader);
        NhkParser nhkParser = new NhkParser(pikselParser, config, downloader);

        RequestVideoInfo request = new RequestVideoInfo(url);
        Response<VideoInfo> videoInfoResponse = nhkParser.parseVideo(request);
        if (videoInfoResponse.ok()) {
            VideoInfo videoInfo = videoInfoResponse.data();
            List<Format> formats = videoInfo.formats();
            for (Format value : formats) {
                VideoWithAudioFormat format = (VideoWithAudioFormat) value;
                System.out.println(format.url());
                System.out.println(format.width() + "x" + format.height());
            }
        }
    }
}
