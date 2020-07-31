package com.github.kiulian.downloader;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

class TestUtils {
    static final String ME_AT_THE_ZOO_ID = "jNQXAC9IVRw"; // me at the zoo
    static final String DESPACITO_ID = "kJQP7kiw5Fk"; // despacito

    static final String LIVE_ID = "5qap5aO4i9A";
    static final String WAS_LIVE_ID = "boSGRDYm92E";

    static final String NO_SUBTITLES_ID = "y9pfCQ5qQYY";

    static boolean isReachable(String url) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(1000);
            connection.setReadTimeout(1000);
            connection.setRequestMethod("HEAD");
            int responseCode = connection.getResponseCode();
            return (200 <= responseCode && responseCode <= 399);
        } catch (IOException exception) {
            return false;
        }
    }
}
