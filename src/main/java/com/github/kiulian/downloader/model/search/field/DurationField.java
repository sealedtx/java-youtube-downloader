package com.github.kiulian.downloader.model.search.field;

public enum DurationField implements SearchField {
    UNDER_4_MINUTES(3, 1),
    OVER_20_MINUTES(3, 2),
    FROM_4_TO_20_MINUTES(3, 3);

    private final byte[] data;

    private DurationField(int... data) {
        this.data = SearchField.convert(data);
    }

    @Override
    public byte[] data() {
        return data;
    }
}
