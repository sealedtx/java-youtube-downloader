package com.github.kiulian.downloader.model.playlist;

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

import java.util.Collections;
import java.util.List;

import com.alibaba.fastjson.JSONObject;
import com.github.kiulian.downloader.YoutubeException;
import com.github.kiulian.downloader.model.AbstractVideo;
import com.github.kiulian.downloader.model.formats.Format;
import com.github.kiulian.downloader.model.subtitles.SubtitlesInfo;
import com.github.kiulian.downloader.parser.Parser;

public class PlaylistVideo extends AbstractVideo<PlaylistVideoDetails> {

    private Parser parser;
    private boolean fetched = false;
    private YoutubeException fetchError;

    public PlaylistVideo(PlaylistVideoDetails details, Parser parser) {
        super(details);
        this.parser = parser;
    }

    @Override
    public List<Format> formats() {
        if (!fetched && fetchError == null) {
            try {
                basicFetch();
            } catch (YoutubeException e) {}
        }
        return super.formats();
    }

    @Override
    public List<SubtitlesInfo> subtitles() {
        if (!fetched && fetchError == null) {
            try {
                basicFetch();
            } catch (YoutubeException e) {}
        }
        return super.subtitles();
    }

    public void fetch() throws YoutubeException {
        if (!fetched) {
            basicFetch();
        }
    }

    public boolean isFetched() {
        return fetched;
    }

    public boolean hasError() {
        return fetchError != null;
    }

    public YoutubeException getFetchError() {
        return fetchError;
    }

    @SuppressWarnings("unchecked")
    private void basicFetch() throws YoutubeException {
        try {
            String htmlUrl = "https://www.youtube.com/watch?v=" + details().videoId();
            JSONObject ytPlayerConfig = parser.getPlayerConfig(htmlUrl);
            formats = parser.parseFormats(ytPlayerConfig);
            subtitlesInfo = parser.getSubtitlesInfoFromCaptions(ytPlayerConfig);
            fetched = true;
            fetchError = null;
        } catch (YoutubeException e) {
            formats = Collections.EMPTY_LIST;
            subtitlesInfo = Collections.EMPTY_LIST;
            fetchError = e;
            throw e;
        }
    }
}
