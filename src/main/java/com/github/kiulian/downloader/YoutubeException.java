package com.github.kiulian.downloader;


public abstract class YoutubeException extends Exception {

    public static final String STEP_DOWNLOAD_JS_REQUEST = "STEP_DOWNLOAD_JS_REQUEST";
    public static final String STEP_ANDROID_CLIENT_PLAYER_REQUEST = "STEP_ANDROID_CLIENT_PLAYER_REQUEST";
    public static final String STEP_WEB_CLIENT_PLAYER_REQUEST = "STEP_WEB_CLIENT_PLAYER_REQUEST";
    public static final String STEP_EMBED_CLIENT_PLAYER_REQUEST = "STEP_EMBED_CLIENT_PLAYER_REQUEST";
    public static final String STEP_EXTRACTION = "STEP_EXTRACTION";
    public static final String STEP_PARSE_FORMATS = "STEP_PARSE_FORMATS";
    public static final String STEP_PLAYLIST_REQUEST = "STEP_PLAYLIST_REQUEST";
    public static final String STEP_PARSE_PLAYLIST = "STEP_PARSE_PLAYLIST";
    public static final String STEP_CHANNEL_REQUEST = "STEP_CHANNEL_REQUEST";
    public static final String STEP_SUBTITLES_REQUEST = "STEP_SUBTITLES_REQUEST";
    public static final String STEP_SEARCH_REQUEST = "STEP_SEARCH_REQUEST";
    public static final String STEP_PARSE_SEARCH = "STEP_PARSE_SEARCH";

    private String step;
    private String additionalData;

    private YoutubeException(String message) {
        this(message, null);
    }
    private YoutubeException(String message, String step) {
        this(message, step, null);
    }

    private YoutubeException(String message, String step, String additionalData) {
        super(message);
        this.step = step;
        this.additionalData = additionalData;
    }

    public String getStep() {
        return step;
    }

    public String getAdditionalData() {
        return additionalData;
    }

    public static class DownloadException extends YoutubeException {

        public DownloadException(String message) {
            this(message, null);
        }

        public DownloadException(String message, String step) {
            this(message, step, null);
        }

        public DownloadException(String message, String step, String additionalData) {
            super(message, step, additionalData);
        }
    }

    public static class ExtractionException extends YoutubeException {

        public ExtractionException(String message) {
            this(message, null);
        }

        public ExtractionException(String message, String additionalData) {
            super(message, STEP_EXTRACTION, additionalData);
        }
    }

    public static class BadPageException extends YoutubeException {

        public BadPageException(String message) {
            this(message, null);
        }

        public BadPageException(String message, String step) {
            this(message, step, null);
        }

        public BadPageException(String message, String step, String additionalData) {
            super(message, step, additionalData);
        }
    }

    public static class CipherException extends YoutubeException {

        public CipherException(String message) {
            super(message);
        }
    }

}
