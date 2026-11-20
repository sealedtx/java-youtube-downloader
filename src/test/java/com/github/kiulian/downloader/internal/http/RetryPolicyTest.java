package com.github.kiulian.downloader.internal.http;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetryPolicyTest {

    private static final IOException RESET = new IOException("connection reset");

    @Test
    void backoffDoublesWithJitterUpToTheCap() {
        RetryPolicy policy = RetryPolicy.retries(10);
        long[] base = {500, 1000, 2000, 4000, 8000, 10_000, 10_000};
        for (int i = 0; i < base.length; i++) {
            long delay = policy.delayBeforeRetry(i + 1, RESET);
            long max = Math.min(10_000, base[i] + base[i] / 4);
            assertTrue(delay >= base[i] && delay <= max, "retry " + (i + 1) + " waited " + delay);
        }
    }

    @Test
    void givesUpAfterTheLastRetry() {
        assertTrue(RetryPolicy.retries(2).delayBeforeRetry(2, RESET) >= 0);
        assertEquals(-1, RetryPolicy.retries(2).delayBeforeRetry(3, RESET));
        assertEquals(-1, RetryPolicy.NONE.delayBeforeRetry(1, RESET));
    }

    @Test
    void retriesTimeoutsButNotInterrupts() {
        RetryPolicy policy = RetryPolicy.retries(1);
        assertTrue(policy.delayBeforeRetry(1, new SocketTimeoutException("read timed out")) >= 0);
        assertEquals(-1, policy.delayBeforeRetry(1, new InterruptedIOException()));
    }

    @Test
    void neverRetriesBadUrlsOrTooLargeBodies() {
        RetryPolicy policy = RetryPolicy.retries(1);
        assertEquals(-1, policy.delayBeforeRetry(1, new MalformedURLException("no protocol")));
        assertEquals(-1, policy.delayBeforeRetry(1, new Http.ResponseTooLargeException(1)));
    }

    @ParameterizedTest
    @ValueSource(ints = {408, 429, 500, 502, 503, 504})
    void retriesTransientStatuses(int status) {
        assertTrue(RetryPolicy.retries(1).delayBeforeRetry(1, statusError(status)) >= 0);
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403, 404, 410, 501})
    void doesNotRetryOtherStatuses(int status) {
        assertEquals(-1, RetryPolicy.retries(1).delayBeforeRetry(1, statusError(status)));
    }

    @Test
    void parsesRetryAfter() {
        long now = 1_700_000_000_000L;
        assertEquals(3000, RetryPolicy.retryAfterMillis("3", now));
        assertEquals(0, RetryPolicy.retryAfterMillis(" 0 ", now));
        assertEquals(-1, RetryPolicy.retryAfterMillis("-1", now));
        assertEquals(-1, RetryPolicy.retryAfterMillis("soon", now));
        assertEquals(-1, RetryPolicy.retryAfterMillis(null, now));
        String inFiveSeconds = DateTimeFormatter.RFC_1123_DATE_TIME.format(Instant.ofEpochMilli(now + 5000).atZone(ZoneOffset.UTC));
        assertEquals(5000, RetryPolicy.retryAfterMillis(inFiveSeconds, now));
        assertEquals(Long.MAX_VALUE, RetryPolicy.retryAfterMillis("99999999999999999", now));
    }

    private static HttpStatusException statusError(int status) {
        return new HttpStatusException(status, "GET", "https://www.youtube.com/", null, "");
    }
}
