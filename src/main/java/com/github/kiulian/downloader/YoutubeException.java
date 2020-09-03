package com.github.kiulian.downloader;

/*-
 * #
 * Java youtube video and audio downloader
 *
 * Copyright (C) 2020 Igor Kiulian
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #
 */


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
