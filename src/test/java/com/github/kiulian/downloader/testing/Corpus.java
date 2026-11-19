package com.github.kiulian.downloader.testing;

import java.util.Arrays;
import java.util.List;

/**
 * Reference videos for recorded fixtures and live checks. Replace an id when YouTube removes the video.
 */
public final class Corpus {

    /** "Me at the zoo", the first YouTube video: tiny, two subtitle tracks, duplicate audio itags (DRC). */
    public static final String ZOO = "jNQXAC9IVRw";
    /** Music video with 12 subtitle tracks and up to 720p without a JS runtime. */
    public static final String N3WPORT = "DFdOcVpRhWI";
    /** Private video. */
    public static final String PRIVATE = "yZIXLfi8CZQ";
    /** Well-formed id of a video that does not exist. */
    public static final String MISSING = "aaaaaaaaaaa";

    public static final List<String> VIDEOS = Arrays.asList(ZOO, N3WPORT, PRIVATE, MISSING);

    /** #AskNASA: 22 videos, owner NASA, one page. */
    public static final String ASK_NASA = "PL2aBZuCeDwlRbJhDIkKXilZRz9ur4VVAA";
    /** Auto-generated album playlist without an owner: 13 tracks. */
    public static final String ALBUM = "OLAK5uy_kiTxC7OplYPiaW4EqzAdqmtguAY7s7f2g";
    /** Well-formed id of a playlist that does not exist. */
    public static final String MISSING_PLAYLIST = "PLaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    /** NASA: thousands of uploads, so their playlist has many pages. */
    public static final String NASA_HANDLE = "@NASA";
    public static final String NASA_CHANNEL_ID = "UCLA_DiR1FfKNvjuUpBHmylQ";

    private Corpus() {
    }
}
