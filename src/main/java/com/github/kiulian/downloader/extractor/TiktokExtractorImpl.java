package com.github.kiulian.downloader.extractor;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.kiulian.downloader.YoutubeException;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TiktokExtractorImpl implements Extractor {

    private final List<Pattern> TIKTOK_PLAYER_CONFIG_PATTERNS = Arrays.asList(
            Pattern.compile("<script\\s+id=\"__UNIVERSAL_DATA_FOR_REHYDRATION__\"\\s+type=\"application/json\">\\s*(.*?)\\s*</script>")
    );

    @Override
    public JSONObject extractInitialDataFromHtml(String html) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public JSONObject extractPlayerConfigFromHtml(String html) throws YoutubeException {
        String ytPlayerConfig = null;
        for (Pattern pattern : TIKTOK_PLAYER_CONFIG_PATTERNS) {
            Matcher matcher = pattern.matcher(html);

            if (matcher.find()) {
                ytPlayerConfig = matcher.group(1);
                break;
            }
        }
        if (ytPlayerConfig == null) {
            throw new YoutubeException.BadPageException("Could not find player config on web page");
        }

        JSONObject config;
        try {
            config = JSON.parseObject(ytPlayerConfig);
        } catch (Exception e) {
            throw new YoutubeException.BadPageException("Player config contains invalid json");
        }

        if (config.containsKey("__DEFAULT_SCOPE__")) {
            return config.getJSONObject("__DEFAULT_SCOPE__").getJSONObject("webapp.video-detail");
        }
        throw new YoutubeException.BadPageException("Could not find player config on web page");
    }

    @Override
    public List<String> extractSubtitlesLanguagesFromXml(String xml) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public String extractJsUrlFromConfig(JSONObject config, String videoId) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public String extractClientVersionFromContext(JSONObject context) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public int extractIntegerFromText(String text) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public long extractLongFromText(String text) {
        throw new RuntimeException("not implemented");
    }

}
