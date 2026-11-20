package com.github.kiulian.downloader.auth;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YoutubeCookiesTest {

    private static final String FILE = "# Netscape HTTP Cookie File\n"
            + "# This is a generated file! Do not edit.\n\n"
            + ".youtube.com\tTRUE\t/\tTRUE\t4102444800\tSAPISID\tSAPI-123\n"
            + "#HttpOnly_.youtube.com\tTRUE\t/\tTRUE\t4102444800\tLOGIN_INFO\tlogin-abc\n"
            + ".youtube.com\tTRUE\t/\tTRUE\t1000000000\tOLD\texpired\n"
            + "music.youtube.com\tFALSE\t/\tTRUE\t0\tSESSION\tsession-cookie\n"
            + ".google.com\tTRUE\t/\tTRUE\t4102444800\tSID\tgoogle-secret\n"
            + ".notyoutube.com\tTRUE\t/\tTRUE\t4102444800\tEVIL\tx\n"
            + "broken line without tabs\n";

    @Test
    void keepsOnlyValidYoutubeCookies() {
        YoutubeCookies cookies = YoutubeCookies.fromNetscape(FILE);

        assertEquals(Arrays.asList("SAPISID", "LOGIN_INFO", "SESSION"), cookies.names());
        assertEquals("login-abc", cookies.get("LOGIN_INFO"));
        assertNull(cookies.get("SID"), "google.com cookies are not for youtube.com");
        assertEquals("SAPISID=SAPI-123; LOGIN_INFO=login-abc; SESSION=session-cookie", cookies.header());
        assertTrue(cookies.isLoggedIn());
    }

    @Test
    void readsACopiedCookieHeader() {
        YoutubeCookies cookies = YoutubeCookies.fromHeader("PREF=f6=40000000; __Secure-3PAPISID=THREE-3P ;LOGIN_INFO=x");

        assertEquals("THREE-3P", cookies.sapisid(), "falls back to __Secure-3PAPISID like YouTube");
        assertTrue(cookies.isLoggedIn());
        assertFalse(YoutubeCookies.fromHeader("PREF=f6=40000000").isLoggedIn());
    }

    @Test
    void neverPrintsValues() {
        String text = YoutubeCookies.fromNetscape(FILE).toString();
        assertFalse(text.contains("SAPI-123") || text.contains("login-abc"), text);
    }
}
