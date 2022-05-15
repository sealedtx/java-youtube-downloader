package com.github.kiulian.downloader.model.search.field;

public enum SortField {
    // Default
    RELEVANCE(0),

    RATING(1),
    UPLOAD_DATE(2),
    VIEW_COUNT(3);

    private final byte value;

    private SortField(int value) {
        this.value = (byte) value;
    }

    public byte value() {
        return value;
    }
}
