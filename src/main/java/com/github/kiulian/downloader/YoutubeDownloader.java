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

import com.alibaba.fastjson.JSONObject;
import com.github.kiulian.downloader.cipher.CachedCipherFactory;
import com.github.kiulian.downloader.cipher.CipherFactory;
import com.github.kiulian.downloader.extractor.DefaultExtractor;
import com.github.kiulian.downloader.extractor.Extractor;
import com.github.kiulian.downloader.model.*;
import com.github.kiulian.downloader.model.formats.Format;
import com.github.kiulian.downloader.parser.DefaultParser;
import com.github.kiulian.downloader.parser.Parser;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class YoutubeDownloader {

    public static final char[] ILLEGAL_FILENAME_CHARACTERS = {'/', '\n', '\r', '\t', '\0', '\f', '`', '?', '*', '\\', '<', '>', '|', '\"', ':'};

    public interface DownloadCallback {

        void onDownloading(int progress);

        void onFinished(File file);

        void onError(Throwable throwable);
    }

    private Parser parser;

    public YoutubeDownloader() {
        Extractor extractor = new DefaultExtractor();
        CipherFactory cipherFactory = new CachedCipherFactory(extractor);
        this.parser = new DefaultParser(extractor, cipherFactory);
    }
    public YoutubeDownloader(Parser parser) {
        this.parser = parser;
    }

    public YoutubeVideo getVideo(String videoId) throws YoutubeException, IOException {
        String htmlUrl = "https://www.youtube.com/watch?v=" + videoId;

        // get player config from web page
        JSONObject ytPlayerConfig = parser.getPlayerConfig(htmlUrl);

        // get video details
        VideoDetails videoDetails = parser.getVideoDetails(ytPlayerConfig);

        // parse formats;
        List<Format> formats = parser.parseFormats(ytPlayerConfig);
        return new YoutubeVideo(videoDetails, formats);
    }

}
