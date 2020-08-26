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

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.github.kiulian.downloader.YoutubeException;
import com.github.kiulian.downloader.model.Extension;
import com.github.kiulian.downloader.model.Utils;
import com.github.kiulian.downloader.model.formats.AudioFormat;
import com.github.kiulian.downloader.model.formats.Format;
import com.github.kiulian.downloader.model.formats.VideoFormat;
import com.github.kiulian.downloader.model.quality.VideoQuality;

public class YoutubePlaylist {

    private PlaylistDetails details;
    private List<PlaylistVideo> videos;

    public YoutubePlaylist(PlaylistDetails details, List<PlaylistVideo> videos) {
        this.details = details;
        this.videos = videos;
    }

    public PlaylistDetails details() {
        return details;
    }

    public List<PlaylistVideo> videos() {
        return videos;
    }

    public PlaylistVideo findVideoById(String videoId) {
        for (PlaylistVideo video : videos) {
            if (video.details().videoId().equals(videoId))
                return video;
        }
        return null;
    }

    public PlaylistVideo findVideoByIndex(int index) {
        for (PlaylistVideo video : videos) {
            if (video.details().index() == index)
                return video;
        }
        return null;
    }

    public void fetchVideos() throws YoutubeException {
        for (PlaylistVideo video : videos) {
            video.fetch();
        }
    }

    public List<PlaylistVideo> findVideos(Predicate<PlaylistVideo> filter) {
        return videos.stream().filter(filter).collect(Collectors.toList());
    }

    public List<PlaylistVideo> findVideosWithFormat(Predicate<Format> filter) {
        return videos.stream()
                .filter(video -> video.findFormat(filter) != null)
                .collect(Collectors.toList());
    }

    // Filtered selection examples
    
    public List<PlaylistVideo> findVideosWithItagFormat(int itag) {
        return findVideosWithFormat(format -> format.itag().id() == itag);
    }

    public List<PlaylistVideo> findVideosWithVideoQuality(VideoQuality videoQuality) {
        return findVideos(video -> !video.findVideoWithQuality(videoQuality).isEmpty());
    }

    public List<PlaylistVideo> videosWithAudioFormats() {
        return findVideosWithFormat(format -> format instanceof AudioFormat);
    }

    // Downloads the first format matching the filter of each video.
    public List<File> download(File outDir, Predicate<Format> filter) throws IOException, YoutubeException {
        File playlistDir = playlistDir(outDir);
        List<File> files = new LinkedList<>();
        for (PlaylistVideo video : videos()) {
            Format format = video.findFormat(filter);
            if (format != null) {
                files.add(download(video, format, playlistDir));
            }
        }
        return files;
    }

    public File download(PlaylistVideo video, Format format, File outDir) throws IOException, YoutubeException {
        return basicDownload(video, format, outDir);
    }

    // Filtered download examples
    
    public List<File> downloadByItag(File outDir, int itag) throws IOException, YoutubeException {
        return download(outDir, format -> format.itag().id() == itag);
    }

    public List<File> downloadVideosWithQuality(File outDir, VideoQuality videoQuality) throws IOException, YoutubeException {
        return download(outDir, format -> format instanceof VideoFormat && ((VideoFormat) format).videoQuality() == videoQuality);
    }
    
    public List<File> downloadAudiosWithExtension(File outDir, Extension extension) throws IOException, YoutubeException {
        return download(outDir, format -> format instanceof AudioFormat && format.extension() == extension);
    }

    public File playlistDir(File outDir) throws IOException {
        return new File(outDir, Utils.removeIllegalChars(details.title()));
    }

    private File basicDownload(PlaylistVideo video, Format format, File outDir) throws IOException, YoutubeException {
        return video.download(format, outDir, videoFileName(video));
    }

    private String videoFileName(PlaylistVideo video) {
        int leftPad = Integer.toString(details.videoCount()).length();
        return String.format("%0" + leftPad + "d", video.details().index()) + "-" + Utils.removeIllegalChars(details.title());
    }
}
