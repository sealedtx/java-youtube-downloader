package com.github.kiulian.downloader.model.search;

import com.github.kiulian.downloader.model.Thumbnail;

import java.util.List;

/**
 * A channel in search results. Get its uploads with {@code channelUploads(channelId())}.
 */
public final class SearchChannel extends SearchItem {

    private final String channelId;
    private final String handle;
    private final long subscriberCount;
    private final String description;

    public SearchChannel(String channelId, String title, String handle, long subscriberCount, String description,
                         List<Thumbnail> thumbnails) {
        super(title, thumbnails);
        this.channelId = channelId;
        this.handle = handle;
        this.subscriberCount = subscriberCount;
        this.description = description;
    }

    @Override
    public String id() {
        return channelId;
    }

    public String channelId() {
        return channelId;
    }

    /** For example {@code @NASA}, or {@code null}. */
    public String handle() {
        return handle;
    }

    /** Approximate ("15.1M subscribers"), 0 if hidden. */
    public long subscriberCount() {
        return subscriberCount;
    }

    public String description() {
        return description;
    }
}
