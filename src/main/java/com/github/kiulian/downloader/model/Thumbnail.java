package com.github.kiulian.downloader.model;

/**
 * A preview image. Width and height are 0 if YouTube did not send them.
 */
public final class Thumbnail {

    private final String url;
    private final int width;
    private final int height;

    public Thumbnail(String url, int width, int height) {
        this.url = url;
        this.width = width;
        this.height = height;
    }

    public String url() {
        return url;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    @Override
    public String toString() {
        return width + "x" + height + " " + url;
    }
}
