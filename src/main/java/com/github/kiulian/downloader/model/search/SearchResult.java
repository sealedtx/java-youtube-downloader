package com.github.kiulian.downloader.model.search;

import java.util.LinkedList;
import java.util.List;

public class SearchResult {

    private long estimatedResults;
    private List<SearchResultItem> items;

    public SearchResult(long estimatedResults, List<SearchResultItem> items) {
        super();
        this.estimatedResults = estimatedResults;
        this.items = items;
    }

    public List<SearchResultVideoDetails> videos() {
        List<SearchResultVideoDetails> videos = new LinkedList<>();
        for (SearchResultItem item : items) {
            if (item.isVideo()) {
                videos.add(item.asVideo());
            }
        }
        return videos;
    }

    public List<SearchResultChannelDetails> channels() {
        List<SearchResultChannelDetails> channels = new LinkedList<>();
        for (SearchResultItem item : items) {
            if (item.isChannel()) {
                channels.add(item.asChannel());
            }
        }
        return channels;
    }

    public List<SearchResultPlaylistDetails> playlists() {
        List<SearchResultPlaylistDetails> videos = new LinkedList<>();
        for (SearchResultItem item : items) {
            if (item.isPlaylist()) {
                videos.add(item.asPlaylist());
            }
        }
        return videos;
    }

    public List<SearchResultShelfDetails> shelves() {
        List<SearchResultShelfDetails> videos = new LinkedList<>();
        for (SearchResultItem item : items) {
            if (item.isShelf()) {
                videos.add(item.asShelf());
            }
        }
        return videos;
    }

    public boolean hasNext() {
        return false;
    }

    public long estimatedResults() {
        return estimatedResults;
    }

    public List<SearchResultItem> items() {
        return items;
    }
}
