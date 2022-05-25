package com.github.kiulian.downloader.downloader.request;

import java.util.*;

import com.github.kiulian.downloader.base64.Base64Encoder;
import com.github.kiulian.downloader.model.search.SearchResult;
import com.github.kiulian.downloader.model.search.field.*;

public class RequestSearchResult extends Request<RequestSearchResult, SearchResult> {

    private static final byte[] FORCED_DATA = new byte[] { 66, 2, 8, 1 };

    private final String query;
    private boolean forceExactQuery;
    private Map<Integer, SearchField> filterFields = new HashMap<>();
    private SortField sortField;

    public RequestSearchResult(String query) {
        super();
        this.query = query;
    }

    public String encodeParameters() {
        if (sortField == null && filterFields.isEmpty() && !forceExactQuery) {
            return null;
        }
        
        int filterLength = 0;
        List<SearchField> filters = null;
        if (!filterFields.isEmpty()) {
            filters = new ArrayList<>(filterFields.values());
            filters.sort((f1, f2) -> f1.category() - f2.category());
            for (SearchField filter : filters) {
                filterLength += filter.length();
            }
        }
        
        int length = filterLength;
        if (sortField != null) {
            length += 2;
        }
        if (filters != null) {
            length += 2;
        }
        if (forceExactQuery) {
            length += FORCED_DATA.length;
        }
        
        final byte[] bytes = new byte[length];
        int i = 0;
        if (sortField != null) {
            bytes[i++] = 8;
            bytes[i++] = sortField.value();
        }
        if (filters != null) {
            bytes[i++] = 18;
            bytes[i++] = (byte) filterLength;
            for (SearchField filter : filters) {
                System.arraycopy(filter.data(), 0, bytes, i, filter.length());
                i += filter.length();
            }
        }
        if (forceExactQuery) {
            System.arraycopy(FORCED_DATA, 0, bytes, i, FORCED_DATA.length);
        }
        
        String encoded = Base64Encoder.getInstance().encodeToString(bytes);
        return encoded.replace("=", "%253D");
    }

    public String query() {
        return query;
    }

    public RequestSearchResult forceExactQuery(boolean forceExactQuery) {
        this.forceExactQuery = forceExactQuery;
        return this;
    }

    public RequestSearchResult filter(SearchField... field) {
        for (SearchField filter : field) {
            filterFields.put(filter.category(), filter);
        }
        return this;
    }

    public RequestSearchResult uploadedThis(UploadDateField uploadDateField) {
        put(uploadDateField);
        return this;
    }

    public RequestSearchResult type(TypeField typeField) {
        put(typeField);
        return this;
    }

    public RequestSearchResult during(DurationField durationField) {
        put(durationField);
        return this;
    }

    public RequestSearchResult match(FeatureField... featuresField) {
        return filter(featuresField);
    }

    public RequestSearchResult format(FormatField... formatsField) {
        return filter(formatsField);
    }

    public RequestSearchResult sortBy(SortField sortField) {
        this.sortField = sortField;
        return this;
    }

    private void put(SearchField field) {
        filterFields.put(field.category(), field);
    }
}
