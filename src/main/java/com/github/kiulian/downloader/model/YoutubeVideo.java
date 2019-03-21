package com.github.kiulian.downloader.model;

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

import com.github.kiulian.downloader.Constants;
import com.github.kiulian.downloader.YoutubeDownloader;

import java.io.*;
import java.net.URL;
import java.util.List;

public class YoutubeVideo {

    private VideoDetails videoDetails;
    private List<Format> formats;

    public YoutubeVideo(VideoDetails videoDetails, List<Format> formats) {
        this.videoDetails = videoDetails;
        this.formats = formats;
    }

    public VideoDetails videoDetails() {
        return videoDetails;
    }

    public List<Format> formats() {
        return formats;
    }

    public File download(Format format, File outDir) throws IOException {
        if (!outDir.exists()) {
            boolean mkdirs = outDir.mkdirs();
            if (!mkdirs)
                throw new IOException("Could not create output directory: " + outDir);
        }

        String fileName = videoDetails.title() + "." + format.extension();
        File outputFile = new File(outDir, cleanFilename(fileName));

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

    public void downloadAsync(Format format, File outDir, YoutubeDownloader.DownloadCallback callback) throws IOException {
        if (!outDir.exists()) {
            boolean mkdirs = outDir.mkdirs();
            if (!mkdirs)
                throw new IOException("Could not create output directory: " + outDir);
        }

        URL url = new URL(format.url());

        Thread thread = new Thread(() -> {

            String fileName = videoDetails.title() + "." + format.extension();
            File outputFile = new File(outDir, cleanFilename(fileName));

            int i = 1;
            while (outputFile.exists()) {
                fileName = videoDetails.title()  + "(" + i++ + ")" +  "." + format.extension();
                outputFile = new File(outDir, cleanFilename(fileName));
            }

            try (BufferedInputStream bis = new BufferedInputStream(url.openStream())) {
                FileOutputStream fis = new FileOutputStream(outputFile);

                double total = 0;
                byte[] buffer = new byte[4096];
                int count = 0;
                while ((count = bis.read(buffer, 0, 4096)) != -1) {
                    fis.write(buffer, 0, count);
                    total += count;
                    double progress = ((total / format.contentLength()) * 100);
                    callback.onDownloading((int) progress);
                }
                fis.close();

                callback.onFinished(outputFile);
            } catch (IOException e) {
                callback.onError(e);
            }
        });
        thread.start();
    }


    private String cleanFilename(String filename) {
        for (char c : Constants.ILLEGAL_FILENAME_CHARACTERS) {
            filename = filename.replace(c, '_');
        }
        return filename;
    }

}
