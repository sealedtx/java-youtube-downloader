package com.github.kiulian.downloader.testing;

import java.util.Arrays;
import java.util.List;

/**
 * Reference videos for recorded fixtures and live checks. Replace an id when YouTube removes the video.
 */
public final class Corpus {

    /** "Me at the zoo", the first YouTube video: tiny, two subtitle tracks, duplicate audio itags (DRC). */
    public static final String ZOO = "jNQXAC9IVRw";
    /** Sintel (Blender Foundation, CC-BY): 30 subtitle tracks and up to 872p without a JS runtime. */
    public static final String SINTEL = "eRsGyueVLvQ";
    /** Private video. */
    public static final String PRIVATE = "yZIXLfi8CZQ";
    /** Well-formed id of a video that does not exist. */
    public static final String MISSING = "aaaaaaaaaaa";

    public static final List<String> VIDEOS = Arrays.asList(ZOO, SINTEL, PRIVATE, MISSING);

    /** Search text whose results mix videos, shorts and a channel. */
    public static final String SEARCH = "nasa artemis";
    /** Misspelled search text that YouTube corrects. */
    public static final String SEARCH_TYPO = "big buk bunny blendr";

    // live checks only (checked 2026-11-23)
    /** Big Buck Bunny (Blender Foundation, CC-BY): up to 1080p, three subtitle tracks. */
    public static final String BUNNY = "YE7VzlLtp-4";
    /** Big Buck Bunny in 4K 60fps (Blender Foundation, CC-BY); the first result of {@link #SEARCH_TYPO}. */
    public static final String UHD = "aqz-KE-bpKQ";
    /** NASA, Artemis II launch trailer: 20 dubbed audio tracks with visionos. */
    public static final String MULTI_AUDIO = "Vg-EQ7MOu6I";
    /** NASA JPL short, vertical. */
    public static final String SHORT = "GZAm_gtnOts";
    /** NASA's Mariner Missions: about 4 hours. */
    public static final String LONG = "Qjx3u9_FZHo";
    /** U.S. Spacewalk: an ended live stream of 10 hours. */
    public static final String WAS_LIVE = "VlB_ZtDLAOQ";
    /** Age-restricted video: needs cookies and the JS solver (tv_samsung). */
    public static final String AGE_RESTRICTED = "AgGa7xohni8";
    /** NASA Space Place compilation: "made for kids", which the JS-less clients do not get. */
    public static final String MADE_FOR_KIDS = "Pmajp_gN00w";

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
