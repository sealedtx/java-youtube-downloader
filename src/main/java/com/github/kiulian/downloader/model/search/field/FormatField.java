package com.github.kiulian.downloader.model.search.field;

public enum FormatField implements SearchField {

    _4K(14, 1),
    HD(4, 1),
    HDR(25, 1, 1),
    _360(15, 1),
    VR180(26, 1, 1),
    _3D(7, 1);

    private final byte[] data;

    private FormatField(int... data) {
        this.data = SearchField.convert(data);
    }

    @Override
    public byte[] data() {
        return data;
    }
}
