package com.github.kiulian.downloader.model.search;

import java.util.*;

import com.github.kiulian.downloader.model.search.query.*;

public class SearchResult {

    private final long estimatedResults;
    private final List<SearchResultItem> items;
    private final QuerySuggestion suggestion;
    private final QueryAutoCorrection autoCorrection;
    private final QueryRefinementList refinementList;
    

    public SearchResult(long estimatedResults, List<SearchResultItem> items,
            Map<QueryElementType, QueryElement> queryElements) {
        this.estimatedResults = estimatedResults;
        this.items = items;
        suggestion = (QuerySuggestion) queryElements.get(QueryElementType.SUGGESTION);
        autoCorrection = (QueryAutoCorrection) queryElements.get(QueryElementType.AUTO_CORRECTION);
        refinementList = (QueryRefinementList) queryElements.get(QueryElementType.REFINEMENT_LIST);
    }

	public QuerySuggestion suggestion() {
		return suggestion;
	}

	public QueryAutoCorrection autoCorrection() {
		return autoCorrection;
	}

	public QueryRefinementList refinements() {
		return refinementList;
	}

	public List<SearchResultVideoDetails> videos() {
        List<SearchResultVideoDetails> videos = new LinkedList<>();
        for (SearchResultItem item : items) {
            if (item.type() == ItemType.VIDEO) {
                videos.add(item.asVideo());
            }
        }
        return videos;
    }

    public List<SearchResultChannelDetails> channels() {
        List<SearchResultChannelDetails> channels = new LinkedList<>();
        for (SearchResultItem item : items) {
            if (item.type() == ItemType.CHANNEL) {
                channels.add(item.asChannel());
            }
        }
        return channels;
    }

    public List<SearchResultPlaylistDetails> playlists() {
        List<SearchResultPlaylistDetails> videos = new LinkedList<>();
        for (SearchResultItem item : items) {
            if (item.type() == ItemType.PLAYLIST) {
                videos.add(item.asPlaylist());
            }
        }
        return videos;
    }

    public List<SearchResultShelf> shelves() {
        List<SearchResultShelf> shelves = new LinkedList<>();
        for (SearchResultItem item : items) {
            if (item.type() == ItemType.SHELF) {
                shelves.add(item.asShelf());
            }
        }
        return shelves;
    }

    public boolean hasContinuation() {
        return false;
    }

    public long estimatedResults() {
        return estimatedResults;
    }

    public List<SearchResultItem> items() {
        return items;
    }
}
