package com.github.kiulian.downloader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base class of all errors of this library.
 * <p>
 * {@link #attempts()} lists what was tried and why each try failed, for example
 * {@code visionos: bot check (Sign in to confirm you're not a bot)}. Include it in bug reports.
 */
public class YoutubeException extends RuntimeException {

    private final List<String> attempts;

    public YoutubeException(String message, List<String> attempts, Throwable cause) {
        super(withAttempts(message, attempts), cause);
        this.attempts = Collections.unmodifiableList(new ArrayList<>(attempts));
    }

    public YoutubeException(String message, Throwable cause) {
        this(message, Collections.emptyList(), cause);
    }

    /** One line per failed try, in the order of the tries. */
    public List<String> attempts() {
        return attempts;
    }

    private static String withAttempts(String message, List<String> attempts) {
        return attempts.isEmpty() ? message : message + " [" + String.join("; ", attempts) + "]";
    }

    /**
     * YouTube answered with an HTTP error status.
     */
    public static class HttpException extends YoutubeException {
        private final int status;

        public HttpException(int status, String message, Throwable cause) {
            super(message, cause);
            this.status = status;
        }

        public int status() {
            return status;
        }
    }

    /**
     * The video cannot be played: removed, private, region-locked, age-restricted and similar.
     * {@link #reason()} is YouTube's own explanation.
     */
    public static class VideoUnavailableException extends YoutubeException {
        private final String reason;

        public VideoUnavailableException(String videoId, String reason, List<String> attempts) {
            super("Video " + videoId + " is unavailable: " + reason, attempts, null);
            this.reason = reason;
        }

        public String reason() {
            return reason;
        }
    }

    /**
     * YouTube asked to "sign in to confirm you're not a bot" for every client that was tried. Usually helps:
     * update the library, wait, or use another network. See the README.
     */
    public static class BotCheckException extends YoutubeException {
        public BotCheckException(String videoId, List<String> attempts) {
            super("YouTube asked for a bot check for video " + videoId, attempts, null);
        }
    }

    /**
     * A format could not be downloaded: HTTP error, expired URL, broken connection after all retries,
     * wrong size, or a problem with the target file. A partial file is never left behind.
     */
    public static class DownloadException extends YoutubeException {
        public DownloadException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * A YouTube response did not have the expected structure. Usually means YouTube changed something;
     * please report it with {@link #attempts()}.
     */
    public static class ExtractionException extends YoutubeException {
        public ExtractionException(String message, List<String> attempts, Throwable cause) {
            super(message, attempts, cause);
        }
    }
}
