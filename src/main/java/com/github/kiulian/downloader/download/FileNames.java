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
    private static final int MAX_LENGTH = 150;

    private FileNames() {
    }

    /**
     * A name that is valid on Windows, macOS and Linux: no path separators or reserved characters, no reserved
     * Windows names, no trailing dots or spaces, at most 150 characters. Never empty.
     */
    public static String safe(String name) {
        String safe = UNSAFE.matcher(name == null ? "" : name).replaceAll("_").trim();
        if (safe.length() > MAX_LENGTH) {
            safe = safe.substring(0, MAX_LENGTH);
            if (Character.isHighSurrogate(safe.charAt(safe.length() - 1))) {
                safe = safe.substring(0, safe.length() - 1);
            }
        }
        safe = safe.replaceAll("[. ]+$", "");
        if (safe.isEmpty() || safe.chars().allMatch(c -> c == '.')) {
            safe = "video";
        }
        if (WINDOWS_RESERVED.matcher(safe).matches()) {
            safe = "_" + safe;
        }
        return safe;
    }

    /** {@code safe(title) + "." + extension}, for example {@code Me at the zoo.m4a}. */
    public static String of(String title, Format format) {
        return safe(title) + "." + format.extension().value().toLowerCase(Locale.ROOT);
    }
}
