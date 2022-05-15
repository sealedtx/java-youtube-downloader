package com.github.kiulian.downloader.model.search.field;

public interface SearchField {

    byte[] data();
    String name();

    default int category() {
        return data()[0] & 0xff;
    }

    default int length() {
        return data().length;
    }

    static byte[] convert(int... data) {
        final byte[] bytes = new byte[data.length];
        bytes[0] = (byte) (data[0] * 8);
        for (int i = 1; i < data.length; i++) {
            bytes[i] = (byte) data[i];
        }
        return bytes;
    }
}
