package com.github.kiulian.downloader.model.search;

import com.github.kiulian.downloader.model.Thumbnail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One search hit: {@link SearchVideo}, {@link SearchChannel} or {@link SearchPlaylist}.
 */
public abstract class SearchItem {

    private final String title;
    private final List<Thumbnail> thumbnails;

    SearchItem(String title, List<Thumbnail> thumbnails) {
        this.title = title;
        this.thumbnails = Collections.unmodifiableList(new ArrayList<>(thumbnails));
    }

    /** Video id, channel id or playlist id. */
    public abstract String id();

    public String title() {
        return title;
    }

    public List<Thumbnail> thumbnails() {
        return thumbnails;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + id() + ", " + title + "}";
    }
}
