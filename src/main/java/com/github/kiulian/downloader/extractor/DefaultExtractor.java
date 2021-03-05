package com.github.kiulian.downloader.extractor;


import com.github.kiulian.downloader.YoutubeException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DefaultExtractor implements Extractor {
    private static final List<Pattern> YT_PLAYER_CONFIG_PATTERNS = Arrays.asList(
            Pattern.compile(";ytplayer\\.config = (\\{.*?\\})\\;ytplayer"),
            Pattern.compile(";ytplayer\\.config = (\\{.*?\\})\\;"),
            Pattern.compile("ytInitialPlayerResponse\\s*=\\s*(\\{.+?\\})\\;var meta")
    );
    private static final List<Pattern> YT_INITIAL_DATA_PATTERNS = Arrays.asList(
            Pattern.compile("window\\[\"ytInitialData\"\\] = (\\{.*?\\});"),
            Pattern.compile("ytInitialData = (\\{.*?\\});")
    );

    private static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.121 Safari/537.36";
    private static final String DEFAULT_ACCEPT_LANG = "en-US,en;";
    private static final int DEFAULT_RETRY_ON_FAILURE = 3;

    private Map<String, String> requestProperties = new HashMap<String, String>();
    private int retryOnFailure = DEFAULT_RETRY_ON_FAILURE;

    public DefaultExtractor() {
        setRequestProperty("User-Agent", DEFAULT_USER_AGENT);
        setRequestProperty("Accept-language", DEFAULT_ACCEPT_LANG);
    }

    @Override
    public void setRequestProperty(String key, String value) {
        requestProperties.put(key, value);
    }

    @Override
    public void setRetryOnFailure(int retryOnFailure) {
        if (retryOnFailure < 0)
            throw new IllegalArgumentException("retry count should be > 0");
        this.retryOnFailure = retryOnFailure;
    }

    @Override
    public String extractYtPlayerConfig(String html) throws YoutubeException {
        for (Pattern pattern : YT_PLAYER_CONFIG_PATTERNS) {
            Matcher matcher = pattern.matcher(html);

            if (matcher.find()) {
                return matcher.group(1);
            }
        }

        throw new YoutubeException.BadPageException("Could not parse web page");
    }

    @Override
    public String extractYtInitialData(String html) throws YoutubeException {
        for (Pattern pattern : YT_INITIAL_DATA_PATTERNS) {
            Matcher matcher = pattern.matcher(html);

            if (matcher.find()) {
                return matcher.group(1);
            }
        }

        throw new YoutubeException.BadPageException("Could not parse web page");
    }

    @Override
    public String loadUrl(String url) throws YoutubeException {
        return loadUrl(url, null, "GET");
    }

    @Override
    public String loadUrl(String url, String data, String method) throws YoutubeException {
        int retryCount = retryOnFailure;
        String errorMsg = "";
        while (retryCount-- >= 0) {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setRequestMethod(method);
                for (Map.Entry<String, String> entry : requestProperties.entrySet()) {
                    connection.setRequestProperty(entry.getKey(), entry.getValue());
                }
                if (data != null) {
                    connection.setDoOutput(true);
                    try (OutputStreamWriter outputWriter = new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8)){
                        outputWriter.write(data);
                        outputWriter.flush();
                    }
                }
                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    errorMsg = String.format("Could not load url: %s, response code: %d", url, responseCode);
                    continue;
                }

                BufferedReader in = new BufferedReader(new InputStreamReader(
                        connection.getInputStream(), "UTF-8"));

                StringBuilder sb = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null)
                    sb.append(inputLine).append('\n');
                in.close();
                return sb.toString();
            } catch (IOException e) {
                errorMsg = String.format("Could not load url: %s, exception: %s", url, e.getMessage());
            }
        }
        throw new YoutubeException.VideoUnavailableException(errorMsg);
    }
}
