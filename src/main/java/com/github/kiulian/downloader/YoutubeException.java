package com.github.kiulian.downloader;


@SuppressWarnings("serial")
public abstract class YoutubeException extends Exception {
    private YoutubeException(String message) {
        super(message);
    }

    public static class VideoUnavailableException extends YoutubeException {

        public VideoUnavailableException(String message) {
            super(message);
        }
    }

    public static class BadPageException extends YoutubeException {

        public BadPageException(String message) {
            super(message);
        }
    }

    public static class UnknownFormatException extends YoutubeException {

        public UnknownFormatException(String message) {
            super(message);
        }

    }

    public static abstract class DownloadUnavailableException extends YoutubeException {

        private DownloadUnavailableException(String message) {
            super(message);
        }
    }

    public static class LiveVideoException extends DownloadUnavailableException {

        public LiveVideoException(String message) {
            super(message);
        }
    }

    public static class RestrictedVideoException extends DownloadUnavailableException {

        public RestrictedVideoException(String message) {
            super(message);
        }
    }

    public static class CipherException extends YoutubeException {

        public CipherException(String message) {
            super(message);
        }
    }

    public static class NetworkException extends YoutubeException {

        public NetworkException(String message) {
            super(message);
        }
    }

    public static class SubtitlesException extends YoutubeException {

        public SubtitlesException(String message) {
            super(message);
        }
    }
}
