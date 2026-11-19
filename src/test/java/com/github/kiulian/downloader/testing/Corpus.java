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

    private Corpus() {
    }
}
