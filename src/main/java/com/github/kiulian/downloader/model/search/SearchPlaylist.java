package com.github.kiulian.downloader.model.search;

import com.github.kiulian.downloader.model.Thumbnail;

import java.util.List;

/**
 * A playlist in search results. Get its videos with {@code playlist(playlistId())}.
 */
public final class SearchPlaylist extends SearchItem {

    private final String playlistId;
    private final String author;
    private final String channelId;
    private final long videoCount;

    public SearchPlaylist(String playlistId, String title, String author, String channelId, long videoCount,
                          List<Thumbnail> thumbnails) {
        super(title, thumbnails);
        this.playlistId = playlistId;
        this.author = author;
        this.channelId = channelId;
        this.videoCount = videoCount;
    }

    @Override
    public String id() {
        return playlistId;
    }

    public String playlistId() {
        return playlistId;
    }

    public String author() {
        return author;
    }

    public String channelId() {
        return channelId;
    }

    /** 0 if YouTube did not show it, for example for mixes. */
    public long videoCount() {
        return videoCount;
    }
}
