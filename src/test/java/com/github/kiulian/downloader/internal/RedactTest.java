package com.github.kiulian.downloader.internal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RedactTest {

    @Test
    void masksSecretsAndKeepsEverythingElse() {
        String url = "https://rr1.googlevideo.com/videoplayback?expire=1&ip=203.0.113.7&id=o-abc&itag=251&sig=AOq0QJ8&lsig=XYZ&pot=Mn0k&n=abc";
        assertEquals("https://rr1.googlevideo.com/videoplayback?expire=1&ip=***&id=o-abc&itag=251&sig=***&lsig=***&pot=***&n=abc",
                Redact.url(url));
    }

    @Test
    void ignoresCaseAndKeepsTheFragment() {
        assertEquals("https://www.youtube.com/youtubei/v1/player?KEY=***#frag",
                Redact.url("https://www.youtube.com/youtubei/v1/player?KEY=AIzaSy#frag"));
    }

    @Test
    void leavesParametersThatOnlyEndWithASecretNameAlone() {
        String url = "https://example.com/?zip=1&clip=2&keys=3&passkey=4";
        assertEquals(url, Redact.url(url));
    }

    @Test
    void keepsNull() {
        assertNull(Redact.url(null));
    }
}
