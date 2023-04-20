package com.github.kiulian.downloader.extractor;



import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.kiulian.downloader.YoutubeException;
import com.github.kiulian.downloader.downloader.Downloader;
import com.github.kiulian.downloader.downloader.request.RequestWebpage;
import com.github.kiulian.downloader.downloader.response.Response;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.github.kiulian.downloader.YoutubeException.STEP_EMBED_CLIENT_PLAYER_REQUEST;

public class ExtractorImpl implements Extractor {
    private static final String DEFAULT_CLIENT_VERSION = "2.20200720.00.02";

    private static final List<Pattern> YT_PLAYER_CONFIG_PATTERNS = Arrays.asList(
            Pattern.compile(";ytplayer\\.config = (\\{.*?\\})\\;ytplayer"),
            Pattern.compile(";ytplayer\\.config = (\\{.*?\\})\\;"),
            Pattern.compile("ytInitialPlayerResponse\\s*=\\s*(\\{.+?\\})\\s*\\;")
    );
    private static final List<Pattern> YT_INITIAL_DATA_PATTERNS = Arrays.asList(
            Pattern.compile("window\\[\"ytInitialData\"\\] = (\\{.*?\\});"),
            Pattern.compile("ytInitialData = (\\{.*?\\});")
    );

    private static final Pattern SUBTITLES_LANG_CODE_PATTERN = Pattern.compile("lang_code=\"(.{2,3})\"");
    private static final Pattern TEXT_NUMBER_REGEX = Pattern.compile("[0-9]+[0-9, ']*");
    private static final Pattern ASSETS_JS_REGEX = Pattern.compile("\"assets\":.+?\"js\":\\s*\"([^\"]+)\"");
    private static final Pattern EMB_JS_REGEX = Pattern.compile("\"jsUrl\":\\s*\"([^\"]+)\"");

    private final Downloader downloader;

    public ExtractorImpl(Downloader downloader) {
        this.downloader = downloader;
    }

    @Override
    public JSONObject extractInitialDataFromHtml(String html) throws YoutubeException {
        String ytInitialData = null;

        for (Pattern pattern : YT_INITIAL_DATA_PATTERNS) {
            Matcher matcher = pattern.matcher(html);
            if (matcher.find()) {
                ytInitialData = matcher.group(1);
            }
        }
        if (ytInitialData == null) {
            String message = "Could not find initial data on web page";
            throw new YoutubeException.ExtractionException(message, html);
        }
        try {
            return JSON.parseObject(ytInitialData);
        } catch (Exception e) {
            String message = "Initial data contains invalid json";
            throw new YoutubeException.ExtractionException(message, ytInitialData);
        }
    }

    @Override
    public JSONObject extractPlayerConfigFromHtml(String html) throws YoutubeException {
        String ytPlayerConfig = null;
        for (Pattern pattern : YT_PLAYER_CONFIG_PATTERNS) {
            Matcher matcher = pattern.matcher(html);

            if (matcher.find()) {
                ytPlayerConfig = matcher.group(1);
                break;
            }
        }
        if (ytPlayerConfig == null) {
            String message = "Could not find player config on web page";
            throw new YoutubeException.ExtractionException(message, html);
        }

        try {
            JSONObject config = JSON.parseObject(ytPlayerConfig);
            if (config.containsKey("args")) {
                return config;
            } else {
                return new JSONObject().fluentPut("args", new JSONObject().fluentPut("player_response", config));
            }
        } catch (Exception e) {
            String message = "Player config contains invalid json";
            throw new YoutubeException.ExtractionException(message, ytPlayerConfig);
        }
    }

    @Override
    public List<String> extractSubtitlesLanguagesFromXml(String xml) throws YoutubeException {
        Matcher matcher = SUBTITLES_LANG_CODE_PATTERN.matcher(xml);

        if (!matcher.find()) {
            String message = "Could not find any language code in subtitles xml";
            throw new YoutubeException.ExtractionException(message, xml);
        }

        List<String> languages = new ArrayList<>();
        do {
            String language = matcher.group(1);
            languages.add(language);
        } while (matcher.find());
        return languages;
    }

    @Override
    public String extractJsUrlFromConfig(JSONObject config, String videoId) throws YoutubeException {
        String js = null;
        if (config.containsKey("assets")) {
            js = config.getJSONObject("assets").getString("js");
        } else {
            // if assets not found - download embed webpage and search there
            String embedUrl = "https://www.youtube.com/embed/" + videoId;
            Response<String> response = downloader.downloadWebpage(new RequestWebpage(embedUrl));
            if (!response.ok()) {
                String message = String.format("Could not load url: %s, exception: %s", embedUrl, response.error().getMessage());
                throw new YoutubeException.DownloadException(message, STEP_EMBED_CLIENT_PLAYER_REQUEST, message);
            }

            String html = response.data();
            Matcher matcher = ASSETS_JS_REGEX.matcher(html);
            if (matcher.find()) {
                js = matcher.group(1).replace("\\", "");
            } else {
                matcher = EMB_JS_REGEX.matcher(html);
                if (matcher.find()) {
                    js = matcher.group(1).replace("\\", "");
                }
            }
        }
        if (js == null) {
            String message = "Could not extract js url: assets not found";
            throw new YoutubeException.ExtractionException(message, config.toJSONString());
        }
        return "https://youtube.com" + js;
    }

    @Override
    public String extractClientVersionFromContext(JSONObject context) {
        JSONArray trackingParams = context.getJSONArray("serviceTrackingParams");
        if (trackingParams == null) {
            return DEFAULT_CLIENT_VERSION;
        }
        for (int ti = 0; ti < trackingParams.size(); ti++) {
            JSONArray params = trackingParams.getJSONObject(ti).getJSONArray("params");
            for (int pi = 0; pi < params.size(); pi++) {
                if (params.getJSONObject(pi).getString("key").equals("cver")) {
                    return params.getJSONObject(pi).getString("value");
                }
            }
        }
        return DEFAULT_CLIENT_VERSION;
    }

    @Override
    public int extractIntegerFromText(String text) {
        Matcher matcher = TEXT_NUMBER_REGEX.matcher(text);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(0).replaceAll("[, ']", ""));
        }
        return 0;
    }

    @Override
    public long extractLongFromText(String text) {
        Matcher matcher = TEXT_NUMBER_REGEX.matcher(text);
        if (matcher.find()) {
            return Long.parseLong(matcher.group(0).replaceAll("[, ']", ""));
        }
        return 0;
    }

}
