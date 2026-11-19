package com.github.kiulian.downloader.model.playlist;

/**
 * Metadata of a playlist. Numbers YouTube did not show are 0, texts are {@code null}.
 */
public final class PlaylistDetails {

    private final String playlistId;
    private final String title;
    private final String description;
    private final String author;
    private final String channelId;
    private final long videoCount;
    private final long viewCount;

    public PlaylistDetails(String playlistId, String title, String description, String author, String channelId,
                           long videoCount, long viewCount) {
        this.playlistId = playlistId;
        this.title = title;
        this.description = description;
        this.author = author;
        this.channelId = channelId;
        this.videoCount = videoCount;
        this.viewCount = viewCount;
    }

    public String playlistId() {
        return playlistId;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    /** Channel name, {@code null} for playlists without an owner (for example albums). */
    public String author() {
        return author;
    }

    public String channelId() {
        return channelId;
    }

    /** Number of videos YouTube shows in the header; it can differ from what the playlist really yields. */
    public long videoCount() {
        return videoCount;
    }

    public long viewCount() {
        return viewCount;
    }

    @Override
    public String toString() {
        return "PlaylistDetails{" + playlistId + ", " + title + ", " + videoCount + " videos}";
    }
}
