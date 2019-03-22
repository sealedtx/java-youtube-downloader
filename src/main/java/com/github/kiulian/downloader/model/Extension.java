package com.github.kiulian.downloader.model;

public class Extension {

    public static final Extension MP4 = new Extension("mp4");
    public static final Extension WEBM = new Extension("webm");
    public static final Extension THREEGP = new Extension("3gp");
    public static final Extension FLV = new Extension("flv");
    public static final Extension HLS = new Extension("hls");
    public static final Extension M4A = new Extension("m4a");
    public static final Extension UNKNOWN = new Extension("unknown");

    private final String value;

    private Extension(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}