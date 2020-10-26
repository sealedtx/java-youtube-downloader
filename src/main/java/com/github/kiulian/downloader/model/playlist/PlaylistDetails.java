package com.github.kiulian.downloader.model.playlist;


public class PlaylistDetails {

    private String playlistId;
    private String title;
    private String author;
    private int videoCount;
    private int viewCount;

    public PlaylistDetails(String playlistId, String title, String author, int videoCount, int viewCount) {
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

    public int videoCount() {
        return videoCount;
    }

    public int viewCount() {
        return viewCount;
    }
}
