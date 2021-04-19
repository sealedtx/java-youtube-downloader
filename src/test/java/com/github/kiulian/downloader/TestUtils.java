package com.github.kiulian.downloader;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

class TestUtils {
    static final String ME_AT_THE_ZOO_ID = "jNQXAC9IVRw"; // me at the zoo
    static final String N3WPORT_ID = "DFdOcVpRhWI"; // N3WPORT - Alive (feat. Neoni) [NCS Release]

    static final String LIVE_ID = "5qap5aO4i9A";
    static final String WAS_LIVE_ID = "boSGRDYm92E";

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

    static void clean(File directory) {
        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                clean(file);
            }
            file.delete();
        }
    }
}
