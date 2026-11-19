package com.github.kiulian.downloader.internal.http;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.concurrent.ThreadLocalRandom;

/**
 * When and how long to wait before sending a failed request again.
 * <p>
 * Retries I/O errors, timeouts and the statuses 408, 429, 500, 502, 503, 504, with exponential backoff and
 * jitter. A {@code Retry-After} header wins over the backoff; if it asks for more than the maximum delay the
 * request is not retried. Other statuses, bad URLs, interrupts and too large bodies are never retried.
 */
public final class RetryPolicy {

    public static final RetryPolicy NONE = new RetryPolicy(0, 0, 0);

    private final int maxRetries;
    private final long baseDelayMillis;
    private final long maxDelayMillis;

    public RetryPolicy(int maxRetries, long baseDelayMillis, long maxDelayMillis) {
        if (maxRetries < 0 || baseDelayMillis < 0 || maxDelayMillis < baseDelayMillis) {
            throw new IllegalArgumentException("invalid retry policy: " + maxRetries + ", " + baseDelayMillis + ", " + maxDelayMillis);
        }
        this.maxRetries = maxRetries;
        this.baseDelayMillis = baseDelayMillis;
        this.maxDelayMillis = maxDelayMillis;
    }

    /** {@code maxRetries} retries; the wait starts at 0.5 s and doubles up to 10 s. */
    public static RetryPolicy retries(int maxRetries) {
        return new RetryPolicy(maxRetries, 500, 10_000);
    }

    public int maxRetries() {
        return maxRetries;
    }

    /**
     * Milliseconds to wait before retry number {@code retry} (1-based) after {@code failure}, or -1 to give up.
     */
    long delayBeforeRetry(int retry, IOException failure) {
        if (retry > maxRetries || Thread.currentThread().isInterrupted()) {
            return -1;
        }
        if (failure instanceof Http.ResponseTooLargeException || failure instanceof MalformedURLException) {
            return -1;
        }
        if (failure instanceof InterruptedIOException && !(failure instanceof SocketTimeoutException)) {
            return -1;
        }
        if (failure instanceof HttpStatusException) {
            HttpStatusException error = (HttpStatusException) failure;
            if (!isRetryable(error.status())) {
                return -1;
            }
            long retryAfter = retryAfterMillis(error.retryAfter(), System.currentTimeMillis());
            if (retryAfter >= 0) {
                return retryAfter <= maxDelayMillis ? retryAfter : -1;
            }
        }
        long delay = Math.min(maxDelayMillis, baseDelayMillis << Math.min(retry - 1, 30));
        long jitter = ThreadLocalRandom.current().nextLong(delay / 4 + 1);
        return Math.min(maxDelayMillis, delay + jitter);
    }

    static boolean isRetryable(int status) {
        return status == 408 || status == 429 || status == 500 || status == 502 || status == 503 || status == 504;
    }

    /** {@code Retry-After} in milliseconds from {@code nowMillis}, or -1 if absent or invalid. */
    static long retryAfterMillis(String header, long nowMillis) {
        if (header == null) {
            return -1;
        }
        String value = header.trim();
        try {
            long seconds = Long.parseLong(value);
            if (seconds < 0) {
                return -1;
            }
            return seconds > Long.MAX_VALUE / 1000 ? Long.MAX_VALUE : seconds * 1000;
        } catch (NumberFormatException notSeconds) {
            // may be an HTTP date
        }
        try {
            long at = ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli();
            return Math.max(0, at - nowMillis);
        } catch (DateTimeParseException e) {
            return -1;
        }
    }
}
