package com.github.kiulian.downloader.model.playlist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A playlist. Its videos are loaded page by page while you iterate, so even very long playlists do not sit in
 * memory and you pay only for the pages you read. Each pass over {@link #videos()} loads the pages again.
 * Loading a later page can fail with a {@code YoutubeException} during the iteration.
 */
public final class Playlist {

    private final PlaylistDetails details;
    private final Iterable<PlaylistVideo> videos;
    private final List<String> warnings;

    public Playlist(PlaylistDetails details, Iterable<PlaylistVideo> videos, List<String> warnings) {
        this.details = details;
        this.videos = videos;
        this.warnings = Collections.unmodifiableList(new ArrayList<>(warnings));
    }

    public PlaylistDetails details() {
        return details;
    }

    /** All videos, loaded lazily page by page. */
    public Iterable<PlaylistVideo> videos() {
        return videos;
    }

    /** Lazy stream of all videos: {@code stream().limit(50)} loads only the first page. */
    public Stream<PlaylistVideo> stream() {
        return StreamSupport.stream(videos.spliterator(), false);
    }

    /** Loads every page now. */
    public List<PlaylistVideo> allVideos() {
        List<PlaylistVideo> all = new ArrayList<>();
        videos.forEach(all::add);
        return all;
    }

    /** Problems with the first page that did not fail the request, such as a fallback to the web page. */
    public List<String> warnings() {
        return warnings;
    }

    @Override
    public String toString() {
        return "Playlist{" + details + "}";
    }
}
