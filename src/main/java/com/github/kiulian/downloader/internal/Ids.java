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
    // same prefixes as yt-dlp; private lists (WL, LL) need a login and are not accepted
    private static final Pattern PLAYLIST_ID = Pattern.compile("(?:PL|EC|UU|FL|RD|UL|TL|PU|OLAK5uy_)[0-9A-Za-z_-]{10,}");
    private static final Pattern CHANNEL_ID = Pattern.compile("UC[A-Za-z0-9_-]{22}");
    private static final Pattern HANDLE = Pattern.compile("@[\\p{L}\\p{N}._-]{3,30}");

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

    /**
     * Playlist id from an id or a URL with a {@code list} parameter (playlist, watch or music URLs).
     *
     * @throws IllegalArgumentException if the input is neither
     */
    public static String playlistId(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Playlist id or URL is null");
        }
        String text = input.trim();
        if (PLAYLIST_ID.matcher(text).matches()) {
            return text;
        }
        URI uri = youtubeUri(text);
        String id = uri == null ? null : queryParam(uri.getRawQuery(), "list");
        if (id != null && PLAYLIST_ID.matcher(id).matches()) {
            return id;
        }
        throw new IllegalArgumentException("Not a YouTube playlist id or URL: " + shorten(input));
    }

    /**
     * A channel id ({@code UC...}) if the input holds one directly, otherwise the channel URL that YouTube has to
     * resolve ({@code @handle}, {@code /c/name}, {@code /user/name}).
     *
     * @return {@code [channelId, null]} or {@code [null, urlToResolve]}
     * @throws IllegalArgumentException if the input is not a channel
     */
    public static String[] channel(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Channel id, handle or URL is null");
        }
        String text = input.trim();
        if (CHANNEL_ID.matcher(text).matches()) {
            return new String[]{text, null};
        }
        if (HANDLE.matcher(text).matches()) {
            return new String[]{null, "https://www.youtube.com/" + text};
        }
        URI uri = youtubeUri(text);
        String path = uri == null || uri.getRawPath() == null ? "" : uri.getRawPath();
        Matcher channelPath = Pattern.compile("^/channel/(UC[A-Za-z0-9_-]{22})(?:/.*)?$").matcher(path);
        if (channelPath.matches()) {
            return new String[]{channelPath.group(1), null};
        }
        Matcher named = Pattern.compile("^/(@[^/]+|c/[^/]+|user/[^/]+)(?:/.*)?$").matcher(path);
        if (named.matches()) {
            return new String[]{null, "https://www.youtube.com/" + named.group(1)};
        }
        throw new IllegalArgumentException("Not a YouTube channel id, handle or URL: " + shorten(input));
    }

    private static URI youtubeUri(String text) {
        try {
            URI uri = new URI(text.contains("://") ? text : "https://" + text);
            String host = uri.getHost();
            String scheme = uri.getScheme();
            if (host == null || !("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme))) {
                return null;
            }
            return isYoutubeHost(host.toLowerCase(Locale.ROOT)) ? uri : null;
        } catch (URISyntaxException e) {
            return null;
        }
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
