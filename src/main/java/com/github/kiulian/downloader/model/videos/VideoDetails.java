package com.github.kiulian.downloader.model.videos;

import com.github.kiulian.downloader.model.Thumbnail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Metadata of a video. Numbers that YouTube did not send are 0, texts are {@code null}.
 */
public final class VideoDetails {

    private final String videoId;
    private final String title;
    private final String author;
    private final String channelId;
    private final String description;
    private final List<String> keywords;
    private final long lengthSeconds;
    private final long viewCount;
    private final List<Thumbnail> thumbnails;
    private final boolean live;
    private final boolean liveContent;
    private final boolean privateVideo;

    private VideoDetails(Builder b) {
        this.videoId = b.videoId;
        this.title = b.title;
        this.author = b.author;
        this.channelId = b.channelId;
        this.description = b.description;
        this.keywords = Collections.unmodifiableList(new ArrayList<>(b.keywords));
        this.lengthSeconds = b.lengthSeconds;
        this.viewCount = b.viewCount;
        this.thumbnails = Collections.unmodifiableList(new ArrayList<>(b.thumbnails));
        this.live = b.live;
        this.liveContent = b.liveContent;
        this.privateVideo = b.privateVideo;
    }

    public static Builder builder(String videoId) {
        return new Builder(videoId);
    }

    public String videoId() {
        return videoId;
    }

    public String title() {
        return title;
    }

    /** Channel name. */
    public String author() {
        return author;
    }

    public String channelId() {
        return channelId;
    }

    public String description() {
        return description;
    }

    public List<String> keywords() {
        return keywords;
    }

    public long lengthSeconds() {
        return lengthSeconds;
    }

    public long viewCount() {
        return viewCount;
    }

    /** Smallest first, as YouTube sends them. */
    public List<Thumbnail> thumbnails() {
        return thumbnails;
    }

    /** True while the stream is live. */
    public boolean isLive() {
        return live;
    }

    /** True for live streams, also after they ended. */
    public boolean isLiveContent() {
        return liveContent;
    }

    public boolean isPrivate() {
        return privateVideo;
    }

    @Override
    public String toString() {
        return "VideoDetails{" + videoId + ", " + title + "}";
    }

    public static final class Builder {
        private final String videoId;
        private String title;
        private String author;
        private String channelId;
        private String description;
        private List<String> keywords = Collections.emptyList();
        private long lengthSeconds;
        private long viewCount;
        private List<Thumbnail> thumbnails = Collections.emptyList();
        private boolean live;
        private boolean liveContent;
        private boolean privateVideo;

        private Builder(String videoId) {
            this.videoId = Objects.requireNonNull(videoId, "videoId");
        }

        public Builder title(String title) { this.title = title; return this; }
        public Builder author(String author) { this.author = author; return this; }
        public Builder channelId(String channelId) { this.channelId = channelId; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder keywords(List<String> keywords) { this.keywords = Objects.requireNonNull(keywords); return this; }
        public Builder lengthSeconds(long lengthSeconds) { this.lengthSeconds = lengthSeconds; return this; }
        public Builder viewCount(long viewCount) { this.viewCount = viewCount; return this; }
        public Builder thumbnails(List<Thumbnail> thumbnails) { this.thumbnails = Objects.requireNonNull(thumbnails); return this; }
        public Builder live(boolean live) { this.live = live; return this; }
        public Builder liveContent(boolean liveContent) { this.liveContent = liveContent; return this; }
        public Builder privateVideo(boolean privateVideo) { this.privateVideo = privateVideo; return this; }

        public VideoDetails build() {
            return new VideoDetails(this);
        }
    }
}
