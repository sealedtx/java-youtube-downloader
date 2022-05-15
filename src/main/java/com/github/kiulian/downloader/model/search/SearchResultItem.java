package com.github.kiulian.downloader.model.search;

public interface SearchResultItem {

    String title();

    default boolean isVideo() {
        return false;
    }

    default boolean isChannel() {
        return false;
    }

    default boolean isPlaylist() {
        return false;
    }

    default boolean isShelf() {
        return false;
    }

    default SearchResultVideoDetails asVideo() {
        if (isVideo()) {
            return (SearchResultVideoDetails) this;
        } else {
            return null;
        }
    }

    default SearchResultChannelDetails asChannel() {
        if (isChannel()) {
            return (SearchResultChannelDetails) this;
        } else {
            return null;
        }
    }

    default SearchResultPlaylistDetails asPlaylist() {
        if (isChannel()) {
            return (SearchResultPlaylistDetails) this;
        } else {
            return null;
        }
    }

    default SearchResultShelfDetails asShelf() {
        if (isShelf()) {
            return (SearchResultShelfDetails) this;
        } else {
            return null;
        }
    }
}
