package com.github.kiulian.downloader.download;

import com.github.kiulian.downloader.model.videos.formats.Format;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Safe file names from video titles.
 */
public final class FileNames {

    private static final Pattern UNSAFE = Pattern.compile("[\\\\/:*?\"<>|\\p{Cntrl}]");
    private static final Pattern WINDOWS_RESERVED = Pattern.compile("(?i)(con|prn|aux|nul|com[1-9]|lpt[1-9])(\\..*)?");
    // 255 bytes per name on most file systems, minus room for the extension and the temporary ".<random>.part"
    private static final int MAX_UTF8_BYTES = 200;

    private FileNames() {
    }

    /**
     * A name that is valid on Windows, macOS and Linux: no path separators or reserved characters, no reserved
     * Windows names, no trailing dots or spaces, at most 200 bytes in UTF-8 (whole characters). Never empty.
     */
    public static String safe(String name) {
        String safe = truncateUtf8(UNSAFE.matcher(name == null ? "" : name).replaceAll("_").trim(), MAX_UTF8_BYTES);
        safe = safe.replaceAll("[. ]+$", "");
        if (safe.isEmpty() || safe.chars().allMatch(c -> c == '.')) {
            safe = "video";
        }
        if (WINDOWS_RESERVED.matcher(safe).matches()) {
            safe = "_" + safe;
        }
        return safe;
    }

    private static String truncateUtf8(String text, int maxBytes) {
        int bytes = 0;
        int end = 0;
        while (end < text.length()) {
            int codePoint = text.codePointAt(end);
            int size = codePoint < 0x80 ? 1 : codePoint < 0x800 ? 2 : codePoint < 0x10000 ? 3 : 4;
            if (bytes + size > maxBytes) {
                break;
            }
            bytes += size;
            end += Character.charCount(codePoint);
        }
        return text.substring(0, end);
    }

    /** {@code safe(title) + "." + extension}, for example {@code Me at the zoo.m4a}. */
    public static String of(String title, Format format) {
        return safe(title) + "." + format.extension().value().toLowerCase(Locale.ROOT);
    }
}
