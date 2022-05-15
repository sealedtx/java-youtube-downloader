package com.github.kiulian.downloader.downloader.request;

import java.util.*;

import com.github.kiulian.downloader.base64.Base64Encoder;
import com.github.kiulian.downloader.model.search.SearchResult;
import com.github.kiulian.downloader.model.search.field.*;

public class RequestSearchResult extends Request<RequestSearchResult, SearchResult> {

    private final String query;
    private Map<Integer, SearchField> filterFields = new HashMap<>();
    private SortField sortField;

    public RequestSearchResult(String query) {
        super();
        this.query = query;
    }

    public String encodeParameters() {
        int filterLength = 0;
        List<SearchField> filters = null;
        if (!filterFields.isEmpty()) {
            filters = new ArrayList<>(filterFields.values());
            filters.sort((f1, f2) -> f1.category() - f2.category());
            for (SearchField filter : filters) {
                filterLength += filter.length();
            }
        }
        
        if (sortField == null && filterLength == 0) {
            return null;
        }
        
        int length = filterLength;
        if (sortField != null) {
            length += 2;
        }
        if (filters != null) {
            length += 2;
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
        
        String encoded = Base64Encoder.getInstance().encodeToString(bytes);
        return encoded.replace("=", "%253D");
    }

    public String query() {
        return query;
    }

    public RequestSearchResult select(SearchField... field) {
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
        return select(featuresField);
    }

    public RequestSearchResult sortBy(SortField sortField) {
        this.sortField = sortField;
        return this;
    }

    private void put(SearchField field) {
        filterFields.put(field.category(), field);
    }
}
