package com.github.kiulian.downloader.base64;

public interface Base64Encoder {
    String encodeToString(byte[] bytes);

    public static void setInstance(Base64Encoder encoder) {
        Instance.encoder = encoder;
    }

    public static Base64Encoder getInstance() {
        return Instance.encoder;
    }

    static class Instance {
        private static Base64Encoder encoder = new Base64EncoderImpl();
    }
}
