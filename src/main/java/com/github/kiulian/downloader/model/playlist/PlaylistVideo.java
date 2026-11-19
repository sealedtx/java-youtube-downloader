package com.github.kiulian.downloader.model.playlist;

import com.github.kiulian.downloader.model.Thumbnail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A video as listed in a playlist. Get formats with {@code videoInfo(videoId())}.
 */
public final class PlaylistVideo {

    private final String videoId;
    private final String title;
    private final String author;
    private final String channelId;
    private final long lengthSeconds;
    private final int index;
    private final List<Thumbnail> thumbnails;

    public PlaylistVideo(String videoId, String title, String author, String channelId, long lengthSeconds, int index,
                         List<Thumbnail> thumbnails) {
        this.videoId = videoId;
        this.title = title;
        this.author = author;
        this.channelId = channelId;
        this.lengthSeconds = lengthSeconds;
        this.index = index;
        this.thumbnails = Collections.unmodifiableList(new ArrayList<>(thumbnails));
    }

    public String videoId() {
        return videoId;
    }

    public String title() {
        return title;
    }

    public String author() {
        return author;
    }

    public String channelId() {
        return channelId;
    }

    /** 0 if unknown, for example for live streams. */
    public long lengthSeconds() {
        return lengthSeconds;
    }

    /** Position in the playlist, starting at 1. */
    public int index() {
        return index;
    }

    public List<Thumbnail> thumbnails() {
        return thumbnails;
    }

    @Override
    public String toString() {
        return index + ". " + videoId + " " + title;
    }
}
