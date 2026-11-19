package com.github.kiulian.downloader.client;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Client profiles that work without a JS runtime, cookies or PO tokens (values from yt-dlp, checked live on
 * 2026-11-19). Re-check them with {@code mvn test -Plive} when YouTube starts answering with a bot check.
 */
public final class ClientProfiles {

    /** Apple Vision Pro app. Not available for "made for kids" videos. */
    public static final ClientProfile VISIONOS = ClientProfile.builder("visionos", "VISIONOS", 101)
            .version("1.02")
            .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 15_7_3) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/26.0 Safari/605.1.15")
            .context("deviceMake", "Apple")
            .context("deviceModel", "RealityDevice17,1")
            .context("osName", "visionOS")
            .context("osVersion", "26.5.23O471")
            .build();

    /**
     * Meta Quest app. Versions above 1.65 may get SABR-only formats; since 2026-08 some formats (itag 18) get 403.
     */
    public static final ClientProfile ANDROID_VR = ClientProfile.builder("android_vr", "ANDROID_VR", 28)
            .version("1.65.10")
            .userAgent("com.google.android.apps.youtube.vr.oculus/1.65.10 (Linux; U; Android 12L; eureka-user Build/SQ3A.220605.009.A1) gzip")
            .context("deviceMake", "Oculus")
            .context("deviceModel", "Quest 3")
            .context("androidSdkVersion", 32)
            .context("osName", "Android")
            .context("osVersion", "12L")
            .build();

    /**
     * Samsung TV app. Needs the JS player: used only with a {@code ChallengeSolver} (for example
     * {@code EjsSolver.node()}). Serves "made for kids" videos, which visionos and android_vr do not.
     * Ciphered formats become valid only after the pre-roll ad; the downloader waits for that.
     */
    public static final ClientProfile TV_SAMSUNG = ClientProfile.builder("tv_samsung", "TVHTML5", 7)
            .version("5.20260707")
            .userAgent("Mozilla/5.0 (SMART-TV; Linux; Tizen 2.4.0) AppleWebKit/538.1 (KHTML, like Gecko) Version/2.4.0 TV Safari/538.1")
            .context("deviceMake", "Samsung")
            .context("deviceModel", "UKS9800")
            .context("osName", "Tizen")
            .context("osVersion", "2.4.0")
            .requiresJsPlayer(true)
            .supportsCookies(true)
            .build();

    /**
     * Desktop web client. Used for playlists, channels and search, not for video formats (web gets SABR only).
     */
    public static final ClientProfile WEB = ClientProfile.builder("web", "WEB", 1)
            .version("2.20260708.00.00")
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
            .supportsCookies(true)
            .build();

    private static final List<ClientProfile> DEFAULTS = Collections.unmodifiableList(Arrays.asList(VISIONOS, ANDROID_VR, TV_SAMSUNG));

    private ClientProfiles() {
    }

    /**
     * The default order: the first client that returns downloadable formats wins. Clients that need the JS player
     * are skipped when no challenge solver is set.
     */
    public static List<ClientProfile> defaults() {
        return DEFAULTS;
    }
}
