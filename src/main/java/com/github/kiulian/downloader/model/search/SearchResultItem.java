package com.github.kiulian.downloader.model.search;

public interface SearchResultItem extends SearchResultElement {

    SearchResultItemType type();

    default SearchResultVideoDetails asVideo() {
        throw new UnsupportedOperationException();
    }

    default SearchResultChannelDetails asChannel() {
        throw new UnsupportedOperationException();
    }

    default SearchResultPlaylistDetails asPlaylist() {
        throw new UnsupportedOperationException();
    }

    default SearchResultShelf asShelf() {
        throw new UnsupportedOperationException();
    }
}
