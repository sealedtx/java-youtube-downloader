package com.github.kiulian.downloader.model.playlist;


public class PlaylistDetails {

    private String playlistId;
    private String title;
    private String author;
    private long videoCount;
    private long viewCount;

    public PlaylistDetails(String playlistId, String title, String author, long videoCount, long viewCount) {
        super();
        this.playlistId = playlistId;
        this.title = title;
        this.author = author;
        this.videoCount = videoCount;
        this.viewCount = viewCount;
    }

    public String playlistId() {
        return playlistId;
    }

    public String title() {
        return title;
    }

    public String author() {
        return author;
    }

    public long videoCount() {
        return videoCount;
    }

    public long viewCount() {
        return viewCount;
    }
}
