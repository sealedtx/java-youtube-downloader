package com.github.kiulian.downloader;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Base64;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.github.kiulian.downloader.base64.Base64EncoderImpl;

public class Base64_Tests {

    @Test
    @DisplayName("encode random bytes test should be successful")
    public void encodeRandomAndCheck_Success() {
        Random r = new Random();
        Base64.Encoder jdkEncoder = Base64.getUrlEncoder();
        Base64EncoderImpl ownEncoder = new Base64EncoderImpl();
        for (int i = 0; i < 1000; i++) {
            byte[] bytes = new byte[i];
            r.nextBytes(bytes);
            String expected = jdkEncoder.encodeToString(bytes);
            String actual = ownEncoder.encodeToString(bytes);
            assertEquals(expected, actual, "JDK and own encodings should be equal");
        }
    }

    @Test
    public void encode() {
        Random r = new Random();
        byte[] bytes = new byte[6];
        r.nextBytes(bytes);
        String expected = Base64.getUrlEncoder().encodeToString(bytes);
        String actual = new Base64EncoderImpl().encodeToString(bytes);
        assertEquals(expected, actual, "JDK and own encodings should be equal");
    }
}
