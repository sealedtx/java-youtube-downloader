package com.github.kiulian.downloader.model.search.field;

public enum FeatureField implements SearchField {

    // Format
    _4K(14, 1),
    HD(4, 1),
    HDR(25, 1, 1),
    _360(15, 1),
    VR180(26, 1, 1),
    _3D(7, 1),

    // Feature
    LIVE(8, 1),
    SUBTITLES(5, 1),
    CREATIVE_COMMONS(6, 1),
    LOCATION(23, 1, 1),
    PURCHASED(9, 1);

    private final byte[] data;

    private FeatureField(int... data) {
        this.data = SearchField.convert(data);
    }

    @Override
    public byte[] data() {
        return data;
    }
}
