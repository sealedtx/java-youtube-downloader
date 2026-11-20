package com.github.kiulian.downloader.download;

import com.github.kiulian.downloader.model.videos.formats.Format;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileNamesTest {

    @Test
    void replacesCharactersThatAreNotAllowedInFileNames() {
        assertEquals("AC_DC_ Back in Black _Live_", FileNames.safe("AC/DC: Back in Black |Live|"));
        assertEquals("a_b_c", FileNames.safe("a\\b\nc"));
    }

    @Test
    void cannotEscapeTheDirectory() {
        assertEquals("_.._etc_passwd", FileNames.safe("/../etc/passwd"));
        assertEquals("video", FileNames.safe(".."));
        assertEquals("video", FileNames.safe("   "));
        assertEquals("video", FileNames.safe(null));
    }

    @Test
    void avoidsReservedWindowsNamesAndTrailingDots() {
        assertEquals("_CON", FileNames.safe("CON"));
        assertEquals("_nul.txt", FileNames.safe("nul.txt"));
        assertEquals("Wait for it", FileNames.safe("Wait for it..."));
    }

    @Test
    void keepsNamesShortWithoutBreakingCharacters() {
        StringBuilder title = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            title.append("😀");
        }
        String safe = FileNames.safe(title.toString());
        assertTrue(safe.getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= 200);
        assertTrue(safe.codePoints().allMatch(c -> c == "😀".codePointAt(0)));
    }

    @Test
    void limitsBytesNotCharacters() {
        StringBuilder title = new StringBuilder();
        for (int i = 0; i < 150; i++) {
            title.append('漢'); // 3 bytes each in UTF-8
        }
        String safe = FileNames.safe(title.toString());
        assertTrue(safe.getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= 200, safe.length() + " chars");
        assertEquals(66, safe.length());
    }

    @Test
    void addsTheExtensionOfTheFormat() {
        Format audio = Format.builder(140, "https://rr1.googlevideo.com/videoplayback").mimeType("audio/mp4").build();
        assertEquals("Me at the zoo.m4a", FileNames.of("Me at the zoo", audio));
    }
}
