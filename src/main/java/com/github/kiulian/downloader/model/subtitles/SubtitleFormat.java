package com.github.kiulian.downloader.model.subtitles;

/**
 * Subtitle formats YouTube can deliver.
 */
public enum SubtitleFormat {
    /** WebVTT, for browsers and most players. */
    VTT("vtt"),
    /** SubRip. */
    SRT("srt"),
    /** TTML (XML). */
    TTML("ttml"),
    /** YouTube's JSON format with word timing. */
    JSON3("json3"),
    /** YouTube's XML formats; srv1 is the simplest. */
    SRV1("srv1"),
    SRV2("srv2"),
    SRV3("srv3");

    private final String value;

    SubtitleFormat(String value) {
        this.value = value;
    }

    /** Value of YouTube's {@code fmt} parameter; also a fitting file extension. */
    public String value() {
        return value;
    }
}
