package com.github.kiulian.downloader.model.subtitles;


import com.github.kiulian.downloader.YoutubeException;
import com.github.kiulian.downloader.model.Extension;
import com.github.kiulian.downloader.model.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

public class Subtitles {

    private final String url;
    private Extension format;
    private String translationLanguage;

    Subtitles(String url) {
        this.url = url;
    }

    public Subtitles formatTo(Extension extension) {
        this.format = extension;
        return this;
    }

    public Subtitles translateTo(String language) {
        this.translationLanguage = language;
        return this;
    }

    public String getDownloadUrl() {
        String downloadUrl = url;
        if (format != null && format.isSubtitle()) {
            downloadUrl += "&fmt=" + format.value();
        }
        if (translationLanguage != null && !translationLanguage.isEmpty()) {
            downloadUrl += "&tlang=" + translationLanguage;
        }
        return downloadUrl;
    }

    public String download() throws YoutubeException {
        URL url;
        try {
            url = new URL(getDownloadUrl());
        } catch (MalformedURLException e) {
            throw new YoutubeException.SubtitlesException("Failed to download subtitle: Invalid url: " + e.getMessage());
        }

        StringBuilder result = new StringBuilder();
        BufferedReader br = null;
        try {
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            int responseCode = urlConnection.getResponseCode();
            if (responseCode != 200) {
                throw new YoutubeException.SubtitlesException("Failed to download subtitle: HTTP " + responseCode);
            }
            if (urlConnection.getContentLength() == 0) {
                throw new YoutubeException.SubtitlesException("Failed to download subtitle: Response is empty");
            }

            br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), "UTF-8"));
            String inputLine;
            while ((inputLine = br.readLine()) != null)
                result.append(inputLine).append('\n');
        } catch (IOException e) {
            throw new YoutubeException.SubtitlesException("Failed to download subtitle: " + e.getMessage());
        } finally {
            Utils.closeSilently(br);
        }

        return result.toString();
    }

    public Future<String> downloadAsync() {
        return downloadAsync(null);
    }

    public Future<String> downloadAsync(OnSubtitlesDownloadListener downloadListener) {
        FutureTask<String> future = new FutureTask<>(new Callable<String>() {
            @Override
            public String call() {
                String subtitles = null;
                try {
                    subtitles = download();
                    if (downloadListener != null) {
                        downloadListener.onFinished(subtitles);
                    }
                } catch (YoutubeException e) {
                    if (downloadListener != null) {
                        downloadListener.onError(e);
                    }
                }
                return subtitles;
            }
        });

        Thread thread = new Thread(future, "YtSubDownloader");
        thread.setDaemon(true);
        thread.start();
        return future;

    }

}
