package com.github.kiulian.downloader.internal;

import com.github.kiulian.downloader.model.search.field.SearchField;
import com.github.kiulian.downloader.model.search.field.SortField;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Encodes search sort and filters into YouTube's {@code params} value (a small protobuf, URL-safe base64).
 */
public final class SearchParams {

    private static final byte[] FORCE_EXACT_QUERY = {66, 2, 8, 1};

    private SearchParams() {
    }

    /**
     * @param sort             sort order or {@code null}
     * @param filters          filters; a later filter of the same category replaces an earlier one
     * @param forceExactQuery  ask YouTube not to auto-correct the query
     * @return the encoded value, or {@code null} when there is nothing to encode
     */
    public static String encode(SortField sort, Collection<? extends SearchField> filters, boolean forceExactQuery) {
        Map<Integer, SearchField> byCategory = new LinkedHashMap<>();
        for (SearchField filter : filters) {
            byCategory.put(filter.category(), filter);
        }
        if (sort == null && byCategory.isEmpty() && !forceExactQuery) {
            return null;
        }
        List<SearchField> sorted = new ArrayList<>(byCategory.values());
        sorted.sort(Comparator.comparingInt(SearchField::category));
        int filtersLength = 0;
        for (SearchField filter : sorted) {
            filtersLength += filter.length();
        }

        byte[] bytes = new byte[(sort != null ? 2 : 0) + (sorted.isEmpty() ? 0 : 2 + filtersLength)
                + (forceExactQuery ? FORCE_EXACT_QUERY.length : 0)];
        int i = 0;
        if (sort != null) {
            bytes[i++] = 8;
            bytes[i++] = sort.value();
        }
        if (!sorted.isEmpty()) {
            bytes[i++] = 18;
            bytes[i++] = (byte) filtersLength;
            for (SearchField filter : sorted) {
                System.arraycopy(filter.data(), 0, bytes, i, filter.length());
                i += filter.length();
            }
        }
        if (forceExactQuery) {
            System.arraycopy(FORCE_EXACT_QUERY, 0, bytes, i, FORCE_EXACT_QUERY.length);
        }
        return Base64.getUrlEncoder().encodeToString(bytes);
    }
}
