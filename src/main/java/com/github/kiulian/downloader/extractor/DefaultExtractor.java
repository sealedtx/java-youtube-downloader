package com.github.kiulian.downloader.extractor;

/*-
 * #
 * Java youtube video and audio downloader
 *
 * Copyright (C) 2020 Igor Kiulian
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #
 */

import com.github.kiulian.downloader.YoutubeException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DefaultExtractor implements Extractor {
    private static final Pattern YT_PLAYER_CONFIG = Pattern.compile(";ytplayer\\.config = (\\{.*?\\});");

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
        Matcher matcher = YT_PLAYER_CONFIG.matcher(html);

        if (matcher.find()) {
            return matcher.group(1);
        }

        throw new YoutubeException.BadPageException("Could not parse web page");
    }

    @Override
    public String loadUrl(String url) throws YoutubeException {
        int retryCount = retryOnFailure;
        String errorMsg = "";
        while (retryCount-- >= 0) {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                for (Map.Entry<String, String> entry : requestProperties.entrySet()) {
                    connection.setRequestProperty(entry.getKey(), entry.getValue());
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
