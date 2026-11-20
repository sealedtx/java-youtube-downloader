package com.github.kiulian.downloader.model.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * One page of search results (about 20 items). Items inside shelves ("Shorts", "Latest from ...") are listed in
 * place, in the order YouTube shows them; ads are left out.
 */
public final class SearchResult {

    private final SearchQuery query;
    private final List<SearchItem> items;
    private final long estimatedResults;
    private final String correctedQuery;
    private final List<String> refinements;
    private final Supplier<SearchResult> nextPage;

    /**
     * @param nextPage loads the next page, or {@code null} on the last page
     */
    public SearchResult(SearchQuery query, List<SearchItem> items, long estimatedResults, String correctedQuery,
                        List<String> refinements, Supplier<SearchResult> nextPage) {
        this.query = query;
        this.items = Collections.unmodifiableList(new ArrayList<>(items));
        this.estimatedResults = estimatedResults;
        this.correctedQuery = correctedQuery;
        this.refinements = Collections.unmodifiableList(new ArrayList<>(refinements));
        this.nextPage = nextPage;
    }

    public SearchQuery query() {
        return query;
    }

    public List<SearchItem> items() {
        return items;
    }

    /** Videos and shorts of this page. */
    public List<SearchVideo> videos() {
        return ofType(SearchVideo.class);
    }

    public List<SearchChannel> channels() {
        return ofType(SearchChannel.class);
    }

    public List<SearchPlaylist> playlists() {
        return ofType(SearchPlaylist.class);
    }

    /** YouTube's rough total. */
    public long estimatedResults() {
        return estimatedResults;
    }

    /**
     * The query YouTube searched instead ("Showing results for ..."), or {@code null}. To get results for the
     * original text, search again with {@code query().exact(true)}.
     */
    public String correctedQuery() {
        return correctedQuery;
    }

    /** Related queries YouTube suggests, possibly empty. */
    public List<String> refinements() {
        return refinements;
    }

    public boolean hasNextPage() {
        return nextPage != null;
    }

    /**
     * Loads the next page. Each call sends a request.
     *
     * @throws NoSuchElementException on the last page
     */
    public SearchResult nextPage() {
        if (nextPage == null) {
            throw new NoSuchElementException("no more search results");
        }
        return nextPage.get();
    }

    private <T extends SearchItem> List<T> ofType(Class<T> type) {
        return items.stream().filter(type::isInstance).map(type::cast).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return "SearchResult{" + query.text() + ", " + items.size() + " items}";
    }
}
