package com.github.kiulian.downloader.model.videos.formats;

import java.util.Locale;

/**
 * File extension of a format, derived from its MIME type.
 */
public enum Extension {
    MP4("mp4"),
    WEBM("webm"),
    M4A("m4a"),
    WEBA("weba"),
    THREE_GP("3gp"),
    FLV("flv"),
    UNKNOWN("bin");

    private final String value;

    Extension(String value) {
        this.value = value;
    }

    /** Extension without the dot, for example {@code mp4}. */
    public String value() {
        return value;
    }

    /** {@code audio/mp4} gives {@link #M4A}, {@code video/mp4} gives {@link #MP4}, and so on. */
    public static Extension fromMimeType(String mimeType) {
        if (mimeType == null) {
            return UNKNOWN;
        }
        String type = mimeType.toLowerCase(Locale.ROOT);
        int end = type.indexOf(';');
        type = (end < 0 ? type : type.substring(0, end)).trim();
        switch (type) {
            case "video/mp4":
                return MP4;
            case "audio/mp4":
                return M4A;
            case "video/webm":
                return WEBM;
            case "audio/webm":
                return WEBA;
            case "video/3gpp":
                return THREE_GP;
            case "video/x-flv":
                return FLV;
            default:
                return UNKNOWN;
        }
    }
}
