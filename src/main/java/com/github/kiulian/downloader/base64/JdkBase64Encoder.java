package com.github.kiulian.downloader.base64;

import java.util.Base64;

public class JdkBase64Encoder implements Base64Encoder {

    public static void setInstance() {
        Base64Encoder.setInstance(new JdkBase64Encoder());
    }

    @Override
    public String encodeToString(byte[] bytes) {
        return Base64.getUrlEncoder().encodeToString(bytes);
    }
}
