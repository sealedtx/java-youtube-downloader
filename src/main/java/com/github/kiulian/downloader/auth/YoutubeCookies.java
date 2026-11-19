package com.github.kiulian.downloader.auth;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Cookies of a YouTube login, for videos that need an account (age-restricted, private shared with you,
 * members-only). They are sent only to youtube.com and only by client profiles that support cookies.
 * <p>
 * Cookies are a password: whoever has them is logged in as you. Keep the file private. YouTube may block accounts
 * used for automated downloads, so prefer a secondary account. Export from a private browser window and close it
 * right away, otherwise the browser rotates the cookies and the exported ones stop working.
 */
public final class YoutubeCookies {

    private final Map<String, String> cookies;

    private YoutubeCookies(Map<String, String> cookies) {
        this.cookies = Collections.unmodifiableMap(cookies);
    }

    /**
     * Reads a {@code cookies.txt} in Netscape format, as written by browser extensions and by yt-dlp. Only cookies
     * of youtube.com that have not expired are kept.
     */
    public static YoutubeCookies fromNetscapeFile(Path file) throws IOException {
        return fromNetscape(new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
    }

    /** {@link #fromNetscapeFile(Path)} for the text of such a file. */
    public static YoutubeCookies fromNetscape(String text) {
        Map<String, String> cookies = new LinkedHashMap<>();
        long now = System.currentTimeMillis() / 1000;
        for (String raw : text.split("\r?\n")) {
            String line = raw.startsWith("#HttpOnly_") ? raw.substring("#HttpOnly_".length()) : raw;
            if (line.trim().isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] fields = line.split("\t");
            if (fields.length < 7) {
                continue;
            }
            String domain = fields[0].toLowerCase(Locale.ROOT);
            long expires = parseLong(fields[4]);
            if (!isYoutubeDomain(domain) || (expires > 0 && expires < now)) {
                continue;
            }
            cookies.put(fields[5], fields[6]);
        }
        return new YoutubeCookies(cookies);
    }

    /** From a {@code Cookie} header value as copied from the browser's developer tools: {@code a=1; b=2}. */
    public static YoutubeCookies fromHeader(String header) {
        Map<String, String> cookies = new LinkedHashMap<>();
        for (String part : Objects.requireNonNull(header, "header").split(";")) {
            int eq = part.indexOf('=');
            if (eq > 0) {
                cookies.put(part.substring(0, eq).trim(), part.substring(eq + 1).trim());
            }
        }
        return new YoutubeCookies(cookies);
    }

    /** True if the cookies look like a login: {@code LOGIN_INFO} and one of the {@code SAPISID} cookies. */
    public boolean isLoggedIn() {
        return cookies.containsKey("LOGIN_INFO") && (sapisid() != null || cookies.containsKey("__Secure-1PAPISID"));
    }

    /** Value of one cookie, or {@code null}. */
    public String get(String name) {
        return cookies.get(name);
    }

    /** Names of the cookies (values are secrets and are not listed). */
    public List<String> names() {
        return Collections.unmodifiableList(new java.util.ArrayList<>(cookies.keySet()));
    }

    /** The {@code Cookie} header value. */
    public String header() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> cookie : cookies.entrySet()) {
            if (sb.length() > 0) {
                sb.append("; ");
            }
            sb.append(cookie.getKey()).append('=').append(cookie.getValue());
        }
        return sb.toString();
    }

    /** {@code SAPISID}, or {@code __Secure-3PAPISID} which YouTube itself falls back to. */
    public String sapisid() {
        String sapisid = cookies.get("SAPISID");
        return sapisid != null ? sapisid : cookies.get("__Secure-3PAPISID");
    }

    @Override
    public String toString() {
        return "YoutubeCookies" + cookies.keySet() + (isLoggedIn() ? " (logged in)" : "");
    }

    private static boolean isYoutubeDomain(String domain) {
        String host = domain.startsWith(".") ? domain.substring(1) : domain;
        return host.equals("youtube.com") || host.endsWith(".youtube.com");
    }

    private static long parseLong(String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
