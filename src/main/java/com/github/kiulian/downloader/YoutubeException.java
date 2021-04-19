package com.github.kiulian.downloader;




public abstract class YoutubeException extends Exception {
    private YoutubeException(String message) {
        super(message);
    }

    public static class DownloadException extends YoutubeException {

        public DownloadException(String message) {
            super(message);
        }
    }

    public static class BadPageException extends YoutubeException {

        public BadPageException(String message) {
            super(message);
        }
    }

    public static class CipherException extends YoutubeException {

        public CipherException(String message) {
            super(message);
        }
    }

}
