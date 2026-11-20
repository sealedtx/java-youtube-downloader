package com.github.kiulian.downloader.model.search;

import com.github.kiulian.downloader.model.Thumbnail;

import java.util.List;

/**
 * A video or short in search results. Numbers YouTube did not show are 0, texts are {@code null}.
 */
public final class SearchVideo extends SearchItem {

    private final String videoId;
    private final String author;
    private final String channelId;
    private final long lengthSeconds;
    private final long viewCount;
    private final String publishedText;
    private final boolean live;
    private final boolean isShort;

    public SearchVideo(String videoId, String title, String author, String channelId, long lengthSeconds, long viewCount,
                       String publishedText, boolean live, boolean isShort, List<Thumbnail> thumbnails) {
        super(title, thumbnails);
        this.videoId = videoId;
        this.author = author;
        this.channelId = channelId;
        this.lengthSeconds = lengthSeconds;
        this.viewCount = viewCount;
        this.publishedText = publishedText;
        this.live = live;
        this.isShort = isShort;
    }

    @Override
    public String id() {
        return videoId;
    }

    public String videoId() {
        return videoId;
    }

    public String author() {
        return author;
    }

    public String channelId() {
        return channelId;
    }

    public long lengthSeconds() {
        return lengthSeconds;
    }

    /** Approximate for abbreviated numbers such as "2.8M views". */
    public long viewCount() {
        return viewCount;
    }

    /** As YouTube shows it, for example {@code 1 year ago}. */
    public String publishedText() {
        return publishedText;
    }

    public boolean isLive() {
        return live;
    }

    public boolean isShort() {
        return isShort;
    }
}
