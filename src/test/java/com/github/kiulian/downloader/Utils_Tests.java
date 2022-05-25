package com.github.kiulian.downloader;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.github.kiulian.downloader.model.Utils;

public class Utils_Tests {

    @Test
    @DisplayName("parse seconds test should be successful")
    public void parseLengthSeconds_Success() {
        assertSeconds("1:32:54", 3600 + (32 * 60) + 54);
        assertSeconds("24", 24);
        assertSeconds("17:05", (17 * 60) + 5);
    }

    private static void assertSeconds(String text, int expected) {
        int actual = Utils.parseLengthSeconds(text);
        assertEquals(expected, actual, "Seconds: " + text);
    }

    @Test
    @DisplayName("parse view count test should be successful")
    public void parseViewCount_Success() {
        assertViewCount("2 865 063 views", 2_865_063L);
        assertViewCount("1 view", 1L);
    }

    private static void assertViewCount(String text, long expected) {
        long actual = Utils.parseViewCount(text);
        assertEquals(expected, actual, "Views: " + text);
    }
}
