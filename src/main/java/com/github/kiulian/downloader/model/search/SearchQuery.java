package com.github.kiulian.downloader.model.search;

import com.github.kiulian.downloader.model.search.field.SearchField;
import com.github.kiulian.downloader.model.search.field.SortField;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * What to search for. Immutable; every method returns a copy.
 *
 * <pre>{@code
 * SearchQuery.of("nasa").filter(TypeField.VIDEO, UploadDateField.WEEK).sortBy(SortField.VIEW_COUNT)
 * }</pre>
 */
public final class SearchQuery {

    private final String text;
    private final List<SearchField> filters;
    private final SortField sort;
    private final boolean exact;

    private SearchQuery(String text, List<SearchField> filters, SortField sort, boolean exact) {
        this.text = text;
        this.filters = Collections.unmodifiableList(filters);
        this.sort = sort;
        this.exact = exact;
    }

    public static SearchQuery of(String text) {
        if (Objects.requireNonNull(text, "text").trim().isEmpty()) {
            throw new IllegalArgumentException("search text is empty");
        }
        return new SearchQuery(text, new ArrayList<>(), null, false);
    }

    /**
     * Adds filters: {@code TypeField}, {@code UploadDateField}, {@code DurationField}, {@code FeatureField},
     * {@code FormatField}. A later filter of the same kind replaces an earlier one.
     */
    public SearchQuery filter(SearchField... fields) {
        List<SearchField> copy = new ArrayList<>(filters);
        copy.addAll(Arrays.asList(fields));
        return new SearchQuery(text, copy, sort, exact);
    }

    public SearchQuery sortBy(SortField sort) {
        return new SearchQuery(text, new ArrayList<>(filters), Objects.requireNonNull(sort, "sort"), exact);
    }

    /** Search exactly for the text; YouTube does not correct spelling then. */
    public SearchQuery exact(boolean exact) {
        return new SearchQuery(text, new ArrayList<>(filters), sort, exact);
    }

    public String text() {
        return text;
    }

    public List<SearchField> filters() {
        return filters;
    }

    /** {@code null} for YouTube's default (relevance). */
    public SortField sort() {
        return sort;
    }

    public boolean isExact() {
        return exact;
    }

    @Override
    public String toString() {
        return "SearchQuery{" + text + ", filters=" + filters + ", sort=" + sort + ", exact=" + exact + "}";
    }
}
