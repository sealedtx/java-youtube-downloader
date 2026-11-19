package com.github.kiulian.downloader.internal;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Strict parsing of YouTube ids from ids and URLs.
 */
public final class Ids {

    private static final Pattern VIDEO_ID = Pattern.compile("[A-Za-z0-9_-]{11}");
    private static final Pattern VIDEO_ID_IN_PATH = Pattern.compile("^/(?:shorts|live|embed|v|e)/([^/]+)");

    private Ids() {
    }

    /**
     * Video id from an id or a URL: watch, youtu.be, shorts, live and embed URLs on youtube.com, its subdomains
     * (www, m, music, ...) and youtube-nocookie.com, with or without scheme.
     *
     * @throws IllegalArgumentException if the input is neither
     */
    public static String videoId(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Video id or URL is null");
        }
        String text = input.trim();
        if (VIDEO_ID.matcher(text).matches()) {
            return text;
        }
        String id = videoIdFromUrl(text);
        if (id != null && VIDEO_ID.matcher(id).matches()) {
            return id;
        }
        throw new IllegalArgumentException("Not a YouTube video id or URL: " + shorten(input));
    }

    private static String videoIdFromUrl(String text) {
        URI uri;
        try {
            uri = new URI(text.contains("://") ? text : "https://" + text);
        } catch (URISyntaxException e) {
            return null;
        }
        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (host == null || !("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme))) {
            return null;
        }
        host = host.toLowerCase(Locale.ROOT);
        String path = uri.getRawPath() == null ? "" : uri.getRawPath();
        if (host.equals("youtu.be")) {
            return firstPathSegment(path);
        }
        if (!isYoutubeHost(host)) {
            return null;
        }
        if (path.equals("/watch") || path.equals("/watch/")) {
            return queryParam(uri.getRawQuery(), "v");
        }
        Matcher matcher = VIDEO_ID_IN_PATH.matcher(path);
        return matcher.find() ? matcher.group(1) : null;
    }

    static boolean isYoutubeHost(String host) {
        return host.equals("youtube.com") || host.endsWith(".youtube.com")
                || host.equals("youtube-nocookie.com") || host.endsWith(".youtube-nocookie.com");
    }

    private static String firstPathSegment(String path) {
        int start = path.startsWith("/") ? 1 : 0;
        int end = path.indexOf('/', start);
        return end < 0 ? path.substring(start) : path.substring(start, end);
    }

    // Ids need no URL decoding: their characters are never encoded.
    private static String queryParam(String rawQuery, String name) {
        if (rawQuery == null) {
            return null;
        }
        for (String pair : rawQuery.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0 && pair.substring(0, eq).equals(name)) {
                return pair.substring(eq + 1);
            }
        }
        return null;
    }

    private static String shorten(String input) {
        return input.length() <= 100 ? input : input.substring(0, 100) + "...";
    }
}
