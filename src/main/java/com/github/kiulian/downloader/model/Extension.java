package com.github.kiulian.downloader.model;


public class Extension {
    // video
    public static final Extension MPEG4 = new Extension("mp4");
    public static final Extension WEBM = new Extension("webm");
    public static final Extension _3GP = new Extension("3gp");
    public static final Extension FLV = new Extension("flv");

    // audio
    public static final Extension M4A = new Extension("m4a");
    public static final Extension WEBA = new Extension("weba");

    // subtitles
    public static final Extension JSON3 = new Extension("json3");
    public static final Extension SUBRIP = new Extension("srt");
    public static final Extension TRANSCRIPT_V1 = new Extension("srv1");
    public static final Extension TRANSCRIPT_V2 = new Extension("srv2");
    public static final Extension TRANSCRIPT_V3 = new Extension("srv3");
    public static final Extension TTML = new Extension("ttml");
    public static final Extension WEBVTT = new Extension("vtt");

    // other
    public static final Extension UNKNOWN = new Extension("unknown");

    private final String value;

    private Extension(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public boolean isAudio() {
        return this.equals(M4A) || this.equals(WEBM);
    }

    public boolean isVideo() {
        return this.equals(MPEG4) || this.equals(WEBM) || this.equals(_3GP) || this.equals(FLV);
    }

    public boolean isSubtitle() {
        return this.equals(SUBRIP) || this.equals(TRANSCRIPT_V1) || this.equals(TRANSCRIPT_V2)
                || this.equals(TRANSCRIPT_V3) || this.equals(TTML) || this.equals(WEBVTT) || this.equals(JSON3);
    }
}
