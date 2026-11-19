package com.github.kiulian.downloader.internal;

import java.util.regex.Pattern;

/**
 * Masks secrets before URLs go into messages or logs.
 */
public final class Redact {

    // sig/lsig/signature let anyone use the stream URL, pot is a PO token, ip is the user's address, key/token are credentials
    private static final Pattern SECRET_QUERY_PARAMS = Pattern.compile(
            "([?&](?:sig|lsig|signature|pot|ip|key|token|access_token)=)[^&#]*", Pattern.CASE_INSENSITIVE);

    private Redact() {
    }

    public static String url(String url) {
        return url == null ? null : SECRET_QUERY_PARAMS.matcher(url).replaceAll("$1***");
    }
}
