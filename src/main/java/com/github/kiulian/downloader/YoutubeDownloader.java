package com.github.kiulian.downloader;

/*-
 * #
 * Java youtube video and audio downloader
 *
 * Copyright (C) 2019 Igor Kiulian
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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.kiulian.downloader.model.*;
import com.github.kiulian.downloader.model.formats.AudioFormat;
import com.github.kiulian.downloader.model.formats.AudioVideoFormat;
import com.github.kiulian.downloader.model.formats.Format;
import com.github.kiulian.downloader.model.formats.VideoFormat;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class YoutubeDownloader {

    public static final char[] ILLEGAL_FILENAME_CHARACTERS = {'/', '\n', '\r', '\t', '\0', '\f', '`', '?', '*', '\\', '<', '>', '|', '\"', ':'};

    public interface DownloadCallback {

        void onDownloading(int progress);

        void onFinished(File file);

        void onError(Throwable throwable);
    }

    private static final String AUDIO = "audio";
    private static final String VIDEO = "video";
    private static final String CONFIG_START = "ytplayer.config = ";
    private static final String CONFIG_END = ";ytplayer.load";
    private static final String ERROR = "\"status\":\"ERROR\",\"reason\":\"";


    public static YoutubeVideo getVideo(String videoId) throws YoutubeException, IOException {
        String page = loadPage("https://www.youtube.com/watch?v=" + videoId);

        VideoDetails videoDetails = new VideoDetails(videoId);

        int start = page.indexOf(CONFIG_START);
        int end = page.indexOf(CONFIG_END);

        if (start == -1 || end == -1) {
            int errorIndex = page.indexOf(ERROR);
            if (errorIndex != -1) {
                String reason = page.substring(errorIndex + ERROR.length(), page.indexOf("\"", errorIndex + ERROR.length() + 1));
                throw new YoutubeException.VideoUnavailableException(reason);
            } else {
                throw new YoutubeException.BadPageException("Could not parse web page");
            }
        }
        String cfg = page.substring(start + CONFIG_START.length(), end);

        JSONArray jsonAdaptiveFormats;

        JSONArray adaptive_fmts;
        JSONArray url_encoded_fmt_stream_map;
        try {
            JSONObject config = JSON.parseObject(cfg);
            JSONObject args = config.getJSONObject("args");

            url_encoded_fmt_stream_map = args.getJSONObject("player_response").getJSONObject("streamingData").getJSONArray("formats");

            adaptive_fmts = args.getJSONObject("player_response").getJSONObject("streamingData").getJSONArray("adaptiveFormats");
            jsonAdaptiveFormats = parseAdaptiveFormats(adaptive_fmts);
            JSONObject player_response = JSON.parseObject(args.getString("player_response"));
            if (player_response.containsKey("videoDetails"))
                videoDetails.setDetails(player_response.getJSONObject("videoDetails"));
        } catch (Exception e) {
            throw new YoutubeException.BadPageException("Could not parse web page");
        }

        List<Format> formats = new ArrayList<>(jsonAdaptiveFormats.size() + 1);

        try {
            formats.add(new AudioVideoFormat((JSONObject)url_encoded_fmt_stream_map.get(0)));
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (int i = 0; i < jsonAdaptiveFormats.size(); i++) {
            JSONObject json = jsonAdaptiveFormats.getJSONObject(i);
            try {
                Itag itag = Itag.valueOf("i" + json.getInteger("itag"));

                if (itag.isVideo() && itag.isAudio())
                    formats.add(new AudioVideoFormat(json));
                else if (itag.isVideo())
                    formats.add(new VideoFormat(json));
                else if (itag.isAudio())
                    formats.add(new AudioFormat(json));
            } catch (IllegalArgumentException e) {
                System.err.println("Unknown itag " + json.getInteger("itag"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return new YoutubeVideo(videoDetails, formats);
    }

    private static String loadPage(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.121 Safari/537.36");
        connection.setRequestProperty("Accept-Language", "en-US,en;");

        BufferedReader in = new BufferedReader(new InputStreamReader(
                connection.getInputStream()));

        StringBuilder sb = new StringBuilder();
        String inputLine;
        while ((inputLine = in.readLine()) != null)
            sb.append(inputLine);
        in.close();

        return sb.toString();
    }

    private static JSONArray parseAdaptiveFormats(JSONArray adaptive_fmts) {
        JSONArray array = new JSONArray();

        for (Object e : adaptive_fmts) {
            JSONObject o = (JSONObject)e;
            String url = o.getString("url");
            if (url != null)
                array.add(o);
        }

        return array;
    }
}
