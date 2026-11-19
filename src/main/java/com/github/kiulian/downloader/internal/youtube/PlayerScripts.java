package com.github.kiulian.downloader.internal.youtube;

import com.github.kiulian.downloader.http.HttpTransport.Request;
import com.github.kiulian.downloader.internal.http.Http;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The current YouTube player JavaScript: its id (from the small {@code iframe_api} script), its code and its
 * {@code signatureTimestamp}. The id is checked again after an hour; the code is downloaded only for a new id.
 */
public final class PlayerScripts {

    private static final long CHECK_INTERVAL_MILLIS = 60 * 60 * 1000;
    private static final Pattern PLAYER_ID = Pattern.compile("player\\\\?/([a-zA-Z0-9_-]{8})\\\\?/");
    private static final Pattern STS = Pattern.compile("(?:signatureTimestamp|sts)\\s*:\\s*(\\d{5})");

    private final Http http;
    private PlayerScript current;
    private long checkedAt;

    public PlayerScripts(Http http) {
        this.http = http;
    }

    public synchronized PlayerScript current() throws IOException {
        if (current != null && System.currentTimeMillis() - checkedAt < CHECK_INTERVAL_MILLIS) {
            return current;
        }
        String api = http.text(Request.get(Innertube.ORIGIN + "/iframe_api").header("User-Agent", Innertube.WEB_USER_AGENT).build());
        Matcher id = PLAYER_ID.matcher(api);
        if (!id.find()) {
            throw new IOException("No player id in iframe_api");
        }
        if (current == null || !current.id.equals(id.group(1))) {
            // the "main" variant, the one yt-dlp's solver is tested against
            String url = Innertube.ORIGIN + "/s/player/" + id.group(1) + "/player_ias.vflset/en_US/base.js";
            String code = http.text(Request.get(url).header("User-Agent", Innertube.WEB_USER_AGENT).build());
            Matcher sts = STS.matcher(code);
            if (!sts.find()) {
                throw new IOException("No signatureTimestamp in player " + id.group(1));
            }
            current = new PlayerScript(id.group(1), code, Integer.parseInt(sts.group(1)));
        }
        checkedAt = System.currentTimeMillis();
        return current;
    }

    public static String id(PlayerScript script) {
        return script.id;
    }

    public static String code(PlayerScript script) {
        return script.code;
    }

    public static int signatureTimestamp(PlayerScript script) {
        return script.signatureTimestamp;
    }

    /** The {@code n} values and ciphered signatures of a player answer. */
    public static void collectChallenges(com.github.kiulian.downloader.internal.json.JsonNode playerAnswer,
                                         java.util.Set<String> n, java.util.Set<String> signatures) {
        new PlayerResponse(playerAnswer).collectChallenges(n, signatures);
    }

    public static final class PlayerScript {
        final String id;
        final String code;
        final int signatureTimestamp;

        PlayerScript(String id, String code, int signatureTimestamp) {
            this.id = id;
            this.code = code;
            this.signatureTimestamp = signatureTimestamp;
        }
    }
}
