package com.github.kiulian.downloader.model.videos;




import com.github.kiulian.downloader.model.Filter;
import com.github.kiulian.downloader.model.videos.formats.AudioFormat;
import com.github.kiulian.downloader.model.videos.formats.VideoWithAudioFormat;
import com.github.kiulian.downloader.model.videos.formats.Format;
import com.github.kiulian.downloader.model.videos.formats.VideoFormat;
import com.github.kiulian.downloader.model.subtitles.SubtitlesInfo;

import java.util.LinkedList;
import java.util.List;

public class VideoInfo {

    private final VideoDetails videoDetails;
    private final List<Format> formats;
    private final List<SubtitlesInfo> subtitlesInfo;

    public VideoInfo(VideoDetails videoDetails, List<Format> formats, List<SubtitlesInfo> subtitlesInfo) {
        this.videoDetails = videoDetails;
        this.formats = formats;
        this.subtitlesInfo = subtitlesInfo;
    }

    public VideoDetails details() {
        return videoDetails;
    }

    public List<Format> formats() {
        return formats;
    }

    public List<SubtitlesInfo> subtitlesInfo() {
        return subtitlesInfo;
    }

    public List<Format> findFormats(Filter<Format> filter) {
        return filter.select(formats);
    }

    public Format findFormatByItag(int itag) {
        for (Format format : formats) {
            if (format.itag().id() == itag)
                return format;
        }
        return null;
    }

    public List<VideoWithAudioFormat> videoWithAudioFormats() {
        List<VideoWithAudioFormat> find = new LinkedList<>();

        for (Format format : formats) {
            if (format instanceof VideoWithAudioFormat) {
                find.add((VideoWithAudioFormat) format);
            }
        }
        return find;
    }

    public VideoFormat bestVideoWithAudioFormat() {
        VideoFormat bestFormat = null;
        for (Format format : formats) {
            if (!(format instanceof VideoWithAudioFormat)) {
                continue;
            }
            VideoFormat videoFormat = (VideoFormat) format;
            if (bestFormat == null || videoFormat.videoQuality().compare(bestFormat.videoQuality()) > 0) {
                bestFormat = videoFormat;
            }
        }
        return bestFormat;
    }

    public List<VideoFormat> videoFormats() {
        List<VideoFormat> find = new LinkedList<>();

        for (Format format : formats) {
            if (format instanceof VideoFormat) {
                find.add((VideoFormat) format);
            }
        }
        return find;
    }

    public VideoFormat bestVideoFormat() {
        VideoFormat bestFormat = null;
        for (Format format : formats) {
            if (!(format instanceof VideoFormat)) {
                continue;
            }
            VideoFormat videoFormat = (VideoFormat) format;
            if (bestFormat == null || videoFormat.videoQuality().compare(bestFormat.videoQuality()) > 0) {
                bestFormat = videoFormat;
            }
        }
        return bestFormat;
    }

    public List<AudioFormat> audioFormats() {
        List<AudioFormat> find = new LinkedList<>();

        for (Format format : formats) {
            if (!(format instanceof AudioFormat)) {
                continue;
            }
            find.add((AudioFormat) format);
        }
        return find;
    }

    public AudioFormat bestAudioFormat() {
        AudioFormat bestFormat = null;
        for (Format format : formats) {
            if (!(format instanceof AudioFormat)) {
                continue;
            }
            AudioFormat audioFormat = (AudioFormat) format;
            if (bestFormat == null || audioFormat.audioQuality().compare(bestFormat.audioQuality()) > 0) {
                bestFormat = audioFormat;
            }
        }
        return bestFormat;
    }


}
