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
import com.github.kiulian.downloader.model.subtitles.SubtitlesInfo;
import com.github.kiulian.downloader.model.videos.VideoDetails;
import com.github.kiulian.downloader.model.videos.VideoInfo;
import com.github.kiulian.downloader.model.videos.formats.Format;
import com.github.kiulian.downloader.model.videos.formats.VideoWithAudioFormat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PikselParser {

    static Pattern VALID_URL = Pattern.compile("(?x)https?://\n" +
            "        (?:\n" +
            "            (?:\n" +
            "                player\\.\n" +
            "                    (?:\n" +
            "                        olympusattelecom|\n" +
            "                        vibebyvista\n" +
            "                    )|\n" +
            "                (?:api|player)\\.multicastmedia|\n" +
            "                (?:api-ovp|player)\\.piksel\n" +
            "            )\\.com|\n" +
            "            (?:\n" +
            "                mz-edge\\.stream\\.co|\n" +
            "                movie-s\\.nhk\\.or\n" +
            "            )\\.jp|\n" +
            "            vidego\\.baltimorecity\\.gov\n" +
            "        )/v/(?:refid/(?<refid>[^/]+)/prefid/)?(?<id>[\\w-]+)");

    private static final List<Pattern> APP_TOKENS_PATTERNS = Arrays.asList(
            Pattern.compile("clientAPI\\s*:\\s*\"([^\"]+)\""),
            Pattern.compile("data-de-api-key\\s*=\\s*\"([^\"]+)\"")
    );

    static String API_URL_TEMPLATE = "http://player.piksel.com/ws/ws_%s/api/%s/mode/json/apiv/5";


    private final Config config;
    private final Downloader downloader;

    public PikselParser(Config config, Downloader downloader) {
        this.config = config;
        this.downloader = downloader;
    }

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

    private VideoInfo parseVideo(String url, YoutubeCallback<VideoInfo> callback) throws YoutubeException {
        Matcher urlMatcher = VALID_URL.matcher(url);
        if (!urlMatcher.matches()) {
            YoutubeException e = new YoutubeException.BadPageException(String.format("Not valid url: %s", url));
            if (callback != null) {
                callback.onError(e);
            }
            throw e;
        }

        Response<String> response = downloader.downloadWebpage(new RequestWebpage(url));
        if (!response.ok()) {
            YoutubeException e = new YoutubeException.DownloadException(String.format("Could not load url: %s, exception: %s", url, response.error().getMessage()));
            if (callback != null) {
                callback.onError(e);
            }
            throw e;
        }
        String html = response.data();

        String appToken = null;
        for (Pattern pattern : APP_TOKENS_PATTERNS) {
            Matcher matcher = pattern.matcher(html);
            if (matcher.find()) {
                appToken = matcher.group(1);
                break;
            }
        }
        if (appToken == null) {
            YoutubeException e = new YoutubeException.BadPageException("Could not find app token");
            if (callback != null) {
                callback.onError(e);
            }
            throw e;
        }

        String refId = urlMatcher.group("refid");
        String displayId = urlMatcher.group("id");

        HashMap<String, String> programParams = new HashMap<>();
        if (refId != null) {
            programParams.put("refid", refId);
            programParams.put("prefid", displayId);
        } else {
            programParams.put("v", displayId);
        }
        JSONObject program = callApi(appToken, "program", programParams, callback)
                .getJSONObject("WsProgramResponse")
                .getJSONObject("program");

        JSONObject videoData = program.getJSONObject("asset");
        String assetType = videoData.containsKey("assetType") ? videoData.getString("assetType") : videoData.getString("asset_type");

        List<Format> formats = new ArrayList<Format>();
        if (videoData.containsKey("assetFiles")) {
            JSONArray assetFiles = videoData.getJSONArray("assetFiles");
            for (int i = 0; i < assetFiles.size(); i++) {
                processAssetFile(assetFiles.getJSONObject(i), assetType, formats);
            }
        }
        processAssetFile(videoData.getJSONObject("referenceFile"), assetType, formats);

        if (formats.isEmpty()) {
            String assetId = videoData.containsKey("assetid") ? videoData.getString("assetid") : program.getString("assetid");
            HashMap<String, String> assetParams = new HashMap<>();
            assetParams.put("assetid", assetId);
            JSONArray assetFiles = callApi(appToken, "asset_file", assetParams, callback)
                    .getJSONObject("WsAssetFileResponse")
                    .getJSONArray("AssetFiles");
            for (int i = 0; i < assetFiles.size(); i++) {
                processAssetFile(assetFiles.getJSONObject(i), assetType, formats);
            }
        }

        VideoDetails videoDetails = parseVideoDetails(program);
        VideoInfo videoInfo = new VideoInfo(videoDetails, formats, Collections.emptyList());
        if (callback != null) {
            callback.onFinished(videoInfo);
        }
        return videoInfo;
    }

    private VideoDetails parseVideoDetails(JSONObject program) {
        JSONObject thumbnail = new JSONObject()
                .fluentPut("thumbnails", new JSONArray()
                        .fluentAdd(new JSONObject()
                                .fluentPut("url", program.getString("thumbnailUrl"))));
        return new VideoDetails(new JSONObject()
                .fluentPut("videoId", program.getString("uuid"))
                .fluentPut("lengthSeconds", program.getDoubleValue("duration"))
                .fluentPut("thumbnail", thumbnail)
                .fluentPut("title", program.getString("Title"))
                .fluentPut("shortDescription", program.getString("shortDescription"))
                .fluentPut("videoId", program.getString("uuid")), null);
    }

    private void processAssetFile(JSONObject assetFile, String assetType, List<Format> formats) {
        if (assetFile == null) {
            return;
        }
        String httpUrl = assetFile.getString("http_url");
        if (httpUrl == null) {
            return;
        }
        Integer tbr = null;
        Integer vbr = (Integer) assetFile.getOrDefault("videoBitrate", 1024);
        Integer abr = (Integer) assetFile.getOrDefault("audioBitrate", 1024);
        if (assetType.equals("video")) {
            tbr = vbr + abr;
        } else if (assetType.equals("audio")) {
            tbr = abr;
        }
        String mimeType = null;
        String filename = assetFile.getString("filename");
        int lastPointIndex = filename.lastIndexOf('.');
        if (lastPointIndex > 0) {
            String ext = filename.substring(lastPointIndex + 1);
            mimeType = assetType + "/" + ext;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        Long lastModified = null;
        try {
            lastModified = sdf.parse(assetFile.getString("datemod")).getTime();
        } catch (ParseException ignore) {
        }
        formats.add(new VideoWithAudioFormat(new JSONObject()
                .fluentPut("url", StringUtils.unescapeHtml3(httpUrl))
                .fluentPut("mimeType", mimeType)
                .fluentPut("bitrate", tbr)
                .fluentPut("averageBitrate", abr)
                .fluentPut("lastModified", lastModified)
                .fluentPut("approxDurationMs", assetFile.getDoubleValue("duration"))
                .fluentPut("width", assetFile.getInteger("videoWidth"))
                .fluentPut("height", assetFile.getInteger("videoHeight"))
                .fluentPut("contentLength", assetFile.getInteger("filesize")), false, "")
        );
    }

    private JSONObject callApi(String appToken, String resource, Map<String, String> params, YoutubeCallback<VideoInfo> callback) throws YoutubeException {
        String htmlUrl = String.format(API_URL_TEMPLATE, resource, appToken);

        int i = 0;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String prefix = i == 0 ? "?" : "&";
            htmlUrl += (prefix + entry.getKey() + "=" + entry.getValue());
            i += 1;
        }

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
            return responseJson.getJSONObject("response");
        } catch (JSONException ignore) {
            YoutubeException e = new YoutubeException.BadPageException("Could not parse json response for url: " + htmlUrl + " response: " + resultingString);
            if (callback != null) {
                callback.onError(e);
            }
            throw e;
        }
    }

    // example of usage
    public static void main(String[] args) {
        String url = "https://player.piksel.com/v/refid/nhkworld/prefid/nw_vod_v_en_6045_005_20220424125500_01_1650772930";

        Config config = new Config.Builder().build();
        PikselParser pikselParser = new PikselParser(config, new DownloaderImpl(config));

        RequestVideoInfo request = new RequestVideoInfo(url);
        Response<VideoInfo> videoInfoResponse = pikselParser.parseVideo(request);
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
