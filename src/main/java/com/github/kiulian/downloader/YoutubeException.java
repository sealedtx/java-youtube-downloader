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


public class YoutubeException extends Exception {
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

    public static class FormatNotFoundException extends YoutubeException {

        public FormatNotFoundException(String message) {
            super(message);
        }

    }

    public static class LiveVideoException extends YoutubeException {

        public LiveVideoException(String message) {
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
}
