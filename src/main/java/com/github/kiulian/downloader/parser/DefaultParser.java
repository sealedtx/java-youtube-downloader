package com.github.kiulian.downloader.parser;

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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.kiulian.downloader.YoutubeException;
import com.github.kiulian.downloader.cipher.CachedCipherFactory;
import com.github.kiulian.downloader.cipher.Cipher;
import com.github.kiulian.downloader.cipher.CipherFactory;
import com.github.kiulian.downloader.extractor.DefaultExtractor;
import com.github.kiulian.downloader.extractor.Extractor;
import com.github.kiulian.downloader.model.Itag;
import com.github.kiulian.downloader.model.VideoDetails;
import com.github.kiulian.downloader.model.formats.AudioFormat;
import com.github.kiulian.downloader.model.formats.AudioVideoFormat;
import com.github.kiulian.downloader.model.formats.Format;
import com.github.kiulian.downloader.model.formats.VideoFormat;
import com.github.kiulian.downloader.model.playlist.PlaylistDetails;
import com.github.kiulian.downloader.model.playlist.PlaylistVideo;
import com.github.kiulian.downloader.model.subtitles.SubtitlesInfo;

public class DefaultParser implements Parser {
    private static final Pattern subtitleLangCodeRegex = Pattern.compile("lang_code=\"(.{2,3})\"");
    private static final Pattern textNumberRegex = Pattern.compile("[0-9, ']+");

    private Extractor extractor;
    private CipherFactory cipherFactory;

    public DefaultParser() {
        this.extractor = new DefaultExtractor();
        this.cipherFactory = new CachedCipherFactory(extractor);
    }

    @Override
    public Extractor getExtractor() {
        return extractor;
    }

    @Override
    public CipherFactory getCipherFactory() {
        return cipherFactory;
    }

    @Override
    public JSONObject getPlayerConfig(String htmlUrl) throws YoutubeException {
        String html = extractor.loadUrl(htmlUrl);

        String ytPlayerConfig = extractor.extractYtPlayerConfig(html);
        try {
            return JSON.parseObject(ytPlayerConfig);
        } catch (Exception e) {
            throw new YoutubeException.BadPageException("Could not parse player config json");
        }
    }

    @Override
    public String getJsUrl(JSONObject config) throws YoutubeException {
        if (!config.containsKey("assets"))
            throw new YoutubeException.BadPageException("Could not extract js url: assets not found");
        return "https://youtube.com" + config.getJSONObject("assets").getString("js");
    }

    @Override
    public VideoDetails getVideoDetails(JSONObject config) {
        JSONObject args = config.getJSONObject("args");
        JSONObject playerResponse = args.getJSONObject("player_response");

        if (playerResponse.containsKey("videoDetails")) {
            JSONObject videoDetails = playerResponse.getJSONObject("videoDetails");
            String liveHLSUrl = null;
            if (videoDetails.getBooleanValue("isLive")) {
                if (playerResponse.containsKey("streamingData")) {
                    liveHLSUrl = playerResponse.getJSONObject("streamingData").getString("hlsManifestUrl");
                }
            }
            return new VideoDetails(videoDetails, liveHLSUrl);
        }

        return new VideoDetails();
    }

    @Override
    public List<SubtitlesInfo> getSubtitlesInfoFromCaptions(JSONObject config) {
        JSONObject args = config.getJSONObject("args");
        JSONObject playerResponse = args.getJSONObject("player_response");

        if (!playerResponse.containsKey("captions")) {
            return Collections.emptyList();
        }
        JSONObject captions = playerResponse.getJSONObject("captions");

        JSONObject playerCaptionsTracklistRenderer = captions.getJSONObject("playerCaptionsTracklistRenderer");
        if (playerCaptionsTracklistRenderer == null || playerCaptionsTracklistRenderer.isEmpty()) {
            return Collections.emptyList();
        }

        JSONArray captionsArray = playerCaptionsTracklistRenderer.getJSONArray("captionTracks");
        if (captionsArray == null || captionsArray.isEmpty()) {
            return Collections.emptyList();
        }

        List<SubtitlesInfo> subtitlesInfo = new ArrayList<>();
        for (int i = 0; i < captionsArray.size(); i++) {
            JSONObject subtitleInfo = captionsArray.getJSONObject(i);
            String language = subtitleInfo.getString("languageCode");
            String url = subtitleInfo.getString("baseUrl");
            String vssId = subtitleInfo.getString("vssId");

            if (language != null && url != null && vssId != null) {
                boolean isAutoGenerated = vssId.startsWith("a.");
                subtitlesInfo.add(new SubtitlesInfo(url, language, isAutoGenerated));
            }
        }
        return subtitlesInfo;
    }

    @Override
    public List<SubtitlesInfo> getSubtitlesInfo(String videoId) throws YoutubeException {
        String xmlUrl = "https://video.google.com/timedtext?hl=en&type=list&v=" + videoId;

        String subtitlesXml = extractor.loadUrl(xmlUrl);

        Matcher matcher = subtitleLangCodeRegex.matcher(subtitlesXml);

        if (!matcher.find()) {
            return Collections.emptyList();
        }

        List<SubtitlesInfo> subtitlesInfo = new ArrayList<>();
        do {
            String language = matcher.group(1);
            String url = String.format("https://www.youtube.com/api/timedtext?lang=%s&v=%s",
                    language, videoId);
            subtitlesInfo.add(new SubtitlesInfo(url, language, false));
        } while (matcher.find());

        return subtitlesInfo;
    }

    @Override
    public List<Format> parseFormats(JSONObject config) throws YoutubeException {
        JSONObject args = config.getJSONObject("args");
        JSONObject playerResponse = args.getJSONObject("player_response");

        if (!playerResponse.containsKey("streamingData")) {
            throw new YoutubeException.BadPageException("Streaming data not found");
        }

        JSONObject streamingData = playerResponse.getJSONObject("streamingData");
        JSONArray jsonFormats = new JSONArray();
        if (streamingData.containsKey("formats")) {
            jsonFormats.addAll(streamingData.getJSONArray("formats"));
        }
        if (streamingData.containsKey("adaptiveFormats")) {
            jsonFormats.addAll(streamingData.getJSONArray("adaptiveFormats"));
        }

        List<Format> formats = new ArrayList<>(jsonFormats.size());
        for (int i = 0; i < jsonFormats.size(); i++) {
            JSONObject json = jsonFormats.getJSONObject(i);
            if ("FORMAT_STREAM_TYPE_OTF".equals(json.getString("type")))
                continue; // unsupported otf formats which cause 404 not found
            try {
                Format format = parseFormat(json, config);
                formats.add(format);
            } catch (YoutubeException.CipherException e) {
                throw e;
            } catch (YoutubeException e) {
                System.err.println("Error parsing format: " + json);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return formats;
    }
    
    @Override
    public JSONObject getInitialData(String htmlUrl) throws YoutubeException {
        String html = extractor.loadUrl(htmlUrl);

        String ytInitialData = extractor.extractYtInitialData(html);
        try {
            return JSON.parseObject(ytInitialData);
        } catch (Exception e) {
            throw new YoutubeException.BadPageException("Could not parse initial data json");
        }
    }
    
    @Override
    public PlaylistDetails getPlaylistDetails(String playlistId, JSONObject initialData) {
        String title, author;
        JSONArray sideBarItems, stats;
        int videoCount, views;
        
        title = initialData.getJSONObject("metadata")
                .getJSONObject("playlistMetadataRenderer")
                .getString("title");
        sideBarItems = initialData.getJSONObject("sidebar").getJSONObject("playlistSidebarRenderer").getJSONArray("items");
        author = sideBarItems.getJSONObject(1)
                .getJSONObject("playlistSidebarSecondaryInfoRenderer")
                .getJSONObject("videoOwner")
                .getJSONObject("videoOwnerRenderer")
                .getJSONObject("title")
                .getJSONArray("runs")
                .getJSONObject(0)
                .getString("text");
        stats = sideBarItems.getJSONObject(0)
                .getJSONObject("playlistSidebarPrimaryInfoRenderer")
                .getJSONArray("stats");
        videoCount = extractNumber(stats.getJSONObject(0).getJSONArray("runs").getJSONObject(0).getString("text"));
        views = extractNumber(stats.getJSONObject(1).getString("simpleText"));
        
        return new PlaylistDetails(playlistId, title, author, videoCount, views);
    }

    @Override
    public List<PlaylistVideo> getPlaylistVideos(JSONObject initialData, int count) throws YoutubeException {
        JSONObject content;
        List<PlaylistVideo> videos;
        
        try {
            content = initialData.getJSONObject("contents")
                    .getJSONObject("twoColumnBrowseResultsRenderer")
                    .getJSONArray("tabs").getJSONObject(0)
                    .getJSONObject("tabRenderer")
                    .getJSONObject("content")
                    .getJSONObject("sectionListRenderer")
                    .getJSONArray("contents").getJSONObject(0)
                    .getJSONObject("itemSectionRenderer")
                    .getJSONArray("contents").getJSONObject(0)
                    .getJSONObject("playlistVideoListRenderer");
        } catch (NullPointerException e) {
            throw new YoutubeException.BadPageException("Playlist initial data not found");
        }
        if (count > 0) {
            videos = new ArrayList<>(count);
        } else {
            videos = new LinkedList<>();
        }
        populatePlaylist(content, videos);
        return videos;
    }
    
    private Format parseFormat(JSONObject json, JSONObject config) throws YoutubeException {
        if (json.containsKey("signatureCipher")) {
            JSONObject jsonCipher = new JSONObject();
            String[] cipherData = json.getString("signatureCipher").replace("\\u0026", "&").split("&");
            for (String s : cipherData) {
                String[] keyValue = s.split("=");
                jsonCipher.put(keyValue[0], keyValue[1]);
            }
            if (!jsonCipher.containsKey("url")) {
                throw new YoutubeException.BadPageException("Could not found url in cipher data");
            }
            String urlWithSig = jsonCipher.getString("url");
            try {
                urlWithSig = URLDecoder.decode(urlWithSig, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            if (urlWithSig.contains("signature")
                    || (!jsonCipher.containsKey("s") && (urlWithSig.contains("&sig=") || urlWithSig.contains("&lsig=")))) {
                // do nothing, this is pre-signed videos with signature
            } else {
                String s = jsonCipher.getString("s");
                try {
                    s = URLDecoder.decode(s, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                String jsUrl = getJsUrl(config);
                Cipher cipher = cipherFactory.createCipher(jsUrl);

                String signature = cipher.getSignature(s);
                String decipheredUrl = urlWithSig + "&sig=" + signature;
                json.put("url", decipheredUrl);
            }
        }

        Itag itag;
        try {
            itag = Itag.valueOf("i" + json.getInteger("itag"));
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            itag = Itag.unknown;
            itag.setId(json.getIntValue("itag"));
        }

        boolean hasVideo = itag.isVideo() || json.containsKey("quality");
        boolean hasAudio = itag.isAudio() || json.containsKey("audioQuality");

        if (hasVideo && hasAudio)
            return new AudioVideoFormat(json);
        else if (hasVideo)
            return new VideoFormat(json);
        else if (hasAudio)
            return new AudioFormat(json);

        throw new YoutubeException.UnknownFormatException("unknown format with itag " + itag.id());
    }
    
    private void populatePlaylist(JSONObject content, List<PlaylistVideo> videos) throws YoutubeException {
        String continuation;
        JSONArray contents;
        
        contents = content.getJSONArray("contents");
        if (content.containsKey("continuations")) {
            continuation = content.getJSONArray("continuations")
                    .getJSONObject(0)
                    .getJSONObject("nextContinuationData")
                    .getString("continuation");
        } else {
            continuation = null;
        }
        for (int i = 0; i < contents.size(); i++) {
            videos.add(new PlaylistVideo(contents.getJSONObject(i).getJSONObject("playlistVideoRenderer")));
        }
        if (continuation != null) {
            loadPlaylistContinuation(continuation, videos);
        }
    }
    
    private void loadPlaylistContinuation(String continuation, List<PlaylistVideo> videos) throws YoutubeException {
        String url, html;
        JSONArray response;
        JSONObject content;
        
        url = "https://www.youtube.com/browse_ajax?ctoken=" + continuation
                + "&continuation=" + continuation;
        
        getExtractor().setRequestProperty("X-YouTube-Client-Name", "1");
        getExtractor().setRequestProperty("X-YouTube-Client-Version", "2.20200720.00.02");
        html = getExtractor().loadUrl(url);
        
        try {
            response = JSON.parseArray(html);
        } catch (Exception e) {
            throw new YoutubeException.BadPageException("Could not parse playlist continuation json");
        }
        content = response.getJSONObject(1)
                .getJSONObject("response")
                .getJSONObject("continuationContents")
                .getJSONObject("playlistVideoListContinuation");
        populatePlaylist(content, videos);
    }
    
    private static int extractNumber(String text) {
        Matcher matcher = textNumberRegex.matcher(text);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(0).replaceAll("[, ']", ""));
        }
        return 0;
    }
}
