package com.github.kiulian.downloader.internal.youtube;

import com.github.kiulian.downloader.auth.YoutubeCookies;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Headers for logged-in innertube requests: the cookies and the {@code SAPISIDHASH} authorization (same scheme as
 * the web player and yt-dlp).
 */
final class CookieAuth {

    private CookieAuth() {
    }

    static Map<String, String> headers(YoutubeCookies cookies, String origin, long epochSeconds) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Cookie", cookies.header());
        String authorization = authorization(cookies, origin, epochSeconds);
        if (authorization != null) {
            headers.put("Authorization", authorization);
            headers.put("X-Origin", origin);
            headers.put("X-Goog-AuthUser", "0");
        }
        return headers;
    }

    /** {@code SAPISIDHASH ts_sha1(ts sid origin)} for each SAPISID cookie present, or {@code null}. */
    static String authorization(YoutubeCookies cookies, String origin, long epochSeconds) {
        StringBuilder result = new StringBuilder();
        String[][] schemes = {
                {"SAPISIDHASH", cookies.sapisid()},
                {"SAPISID1PHASH", cookies.get("__Secure-1PAPISID")},
                {"SAPISID3PHASH", cookies.get("__Secure-3PAPISID")},
        };
        for (String[] scheme : schemes) {
            if (scheme[1] == null) {
                continue;
            }
            String timestamp = String.valueOf(epochSeconds);
            if (result.length() > 0) {
                result.append(' ');
            }
            result.append(scheme[0]).append(' ').append(timestamp).append('_').append(sha1(timestamp + " " + scheme[1] + " " + origin));
        }
        return result.length() == 0 ? null : result.toString();
    }

    private static String sha1(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-1").digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
