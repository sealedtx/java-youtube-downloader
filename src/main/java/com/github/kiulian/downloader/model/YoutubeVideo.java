package com.github.kiulian.downloader.model;

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


import com.github.kiulian.downloader.OnYoutubeDownloadListener;
import com.github.kiulian.downloader.YoutubeException;
import com.github.kiulian.downloader.model.formats.AudioFormat;
import com.github.kiulian.downloader.model.formats.AudioVideoFormat;
import com.github.kiulian.downloader.model.formats.Format;
import com.github.kiulian.downloader.model.formats.VideoFormat;
import com.github.kiulian.downloader.model.quality.AudioQuality;
import com.github.kiulian.downloader.model.quality.VideoQuality;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import static com.github.kiulian.downloader.model.Utils.getOutputFile;

public class YoutubeVideo {

    private VideoDetails videoDetails;
    private List<Format> formats;

    public YoutubeVideo(VideoDetails videoDetails, List<Format> formats) {
        this.videoDetails = videoDetails;
        this.formats = formats;
    }

    public VideoDetails details() {
        return videoDetails;
    }

    public List<Format> formats() {
        return formats;
    }

    public Format findFormatByItag(int itag) {
        for (int i = 0; i < formats.size(); i++) {
            Format format = formats.get(i);
            if (format.itag().id() == itag)
                return format;
        }
        return null;
    }

    public List<AudioVideoFormat> videoWithAudioFormats() {
        List<AudioVideoFormat> find = new LinkedList<>();

        for (int i = 0; i < formats.size(); i++) {
            Format format = formats.get(i);
            if (format instanceof AudioVideoFormat)
                find.add((AudioVideoFormat) format);
        }
        return find;
    }

    public List<VideoFormat> videoFormats() {
        List<VideoFormat> find = new LinkedList<>();

        for (int i = 0; i < formats.size(); i++) {
            Format format = formats.get(i);
            if (format instanceof VideoFormat)
                find.add((VideoFormat) format);
        }
        return find;
    }

    public List<VideoFormat> findVideoWithQuality(VideoQuality videoQuality) {
        List<VideoFormat> find = new LinkedList<>();

        for (int i = 0; i < formats.size(); i++) {
            Format format = formats.get(i);
            if (format instanceof VideoFormat && ((VideoFormat) format).videoQuality() == videoQuality)
                find.add((VideoFormat) format);
        }
        return find;
    }

    public List<VideoFormat> findVideoWithExtension(Extension extension) {
        List<VideoFormat> find = new LinkedList<>();

        for (int i = 0; i < formats.size(); i++) {
            Format format = formats.get(i);
            if (format instanceof VideoFormat && format.extension().equals(extension))
                find.add((VideoFormat) format);
        }
        return find;
    }

    public List<AudioFormat> audioFormats() {
        List<AudioFormat> find = new LinkedList<>();

        for (int i = 0; i < formats.size(); i++) {
            Format format = formats.get(i);
            if (format instanceof AudioFormat)
                find.add((AudioFormat) format);
        }
        return find;
    }

    public List<AudioFormat> findAudioWithQuality(AudioQuality audioQuality) {
        List<AudioFormat> find = new LinkedList<>();

        for (int i = 0; i < formats.size(); i++) {
            Format format = formats.get(i);
            if (format instanceof AudioFormat && ((AudioFormat) format).audioQuality() == audioQuality)
                find.add((AudioFormat) format);
        }
        return find;
    }

    public List<AudioFormat> findAudioWithExtension(Extension extension) {
        List<AudioFormat> find = new LinkedList<>();

        for (int i = 0; i < formats.size(); i++) {
            Format format = formats.get(i);
            if (format instanceof AudioFormat && format.extension() == extension)
                find.add((AudioFormat) format);
        }
        return find;
    }

    public File download(Format format, File outDir) throws IOException, YoutubeException {
        if (videoDetails.isLive() || (videoDetails.isLiveContent() && videoDetails.lengthSeconds() == 0))
            throw new YoutubeException.LiveVideoException("Can not download live stream");

        File outputFile = getOutputFile(videoDetails, format, outDir);

        URL url = new URL(format.url());
        BufferedInputStream bis = new BufferedInputStream(url.openStream());
        FileOutputStream fis = new FileOutputStream(outputFile);
        byte[] buffer = new byte[4096];
        int count = 0;
        while ((count = bis.read(buffer, 0, 4096)) != -1) {
            fis.write(buffer, 0, count);
        }
        fis.close();
        bis.close();
        return outputFile;
    }

    public Future<File> downloadAsync(Format format, File outDir) throws YoutubeException.LiveVideoException, IOException {
        if (videoDetails.isLive())
            throw new YoutubeException.LiveVideoException("Can not download live stream");

        getOutputFile(videoDetails, format, outDir);

        FutureTask<File> future = new FutureTask<>(new Callable<File>() {
            @Override
            public File call() throws IOException, YoutubeException {
                return download(format, outDir);
            }
        });

        Thread thread = new Thread(future, "YtDownloader");
        thread.setDaemon(true);
        thread.start();
        return future;
    }

    public void downloadAsync(Format format, File outDir, OnYoutubeDownloadListener listener) throws IOException, YoutubeException {
        if (videoDetails.isLive() || (videoDetails.isLiveContent() && videoDetails.lengthSeconds() == 0))
            throw new YoutubeException.LiveVideoException("Can not download live stream");

        File outputFile = getOutputFile(videoDetails, format, outDir);

        URL url = new URL(format.url());
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URLConnection urlConnection = url.openConnection();
                    int contentLength = urlConnection.getContentLength();

                    try (BufferedInputStream bis = new BufferedInputStream(urlConnection.getInputStream())) {
                        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(outputFile))) {
                            double total = 0;
                            byte[] buffer = new byte[4096];
                            int count = 0;
                            int progress = 0;
                            while ((count = bis.read(buffer, 0, 4096)) != -1) {
                                bos.write(buffer, 0, count);
                                total += count;
                                int newProgress = (int) ((total / contentLength) * 100);
                                if (newProgress > progress) {
                                    progress = newProgress;
                                    listener.onDownloading(progress);
                                }
                            }

                            listener.onFinished(outputFile);
                        } catch (IOException e) {
                            listener.onError(e);
                        }
                    } catch (IOException e) {
                        listener.onError(e);
                    }
                } catch (IOException e) {
                    listener.onError(e);
                }
            }
        }, "YtDownloader");
        thread.setDaemon(true);
        thread.start();
    }

}
