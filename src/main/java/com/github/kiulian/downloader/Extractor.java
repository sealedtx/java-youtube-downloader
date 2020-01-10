package com.github.kiulian.downloader;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Extractor {

    public static final Pattern YT_PLAYER_CONFIG = Pattern.compile(";ytplayer\\.config = (\\{.*?});");

    public static JSONObject getYtPlayerConfig(String html) throws YoutubeException.BadPageException {
        Matcher matcher = YT_PLAYER_CONFIG.matcher(html);

        if (matcher.find()) {
            return JSON.parseObject(matcher.group(1));
        }

        throw new YoutubeException.BadPageException("Could not parse web page");
    }

    public static String getJsUrl(JSONObject config) {
        return "https://youtube.com" + config.getJSONObject("assets").getString("js");
    }

    public static String loadUrl(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.121 Safari/537.36");
        connection.setRequestProperty("Accept-Language", "en-US,en;");

        BufferedReader in = new BufferedReader(new InputStreamReader(
                connection.getInputStream()));

        StringBuilder sb = new StringBuilder();
        String inputLine;
        while ((inputLine = in.readLine()) != null)
            sb.append(inputLine).append('\n');
        in.close();

        return sb.toString();
    }

}
