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

    private static final List<ClientProfile> DEFAULTS = Collections.unmodifiableList(Arrays.asList(VISIONOS, ANDROID_VR));

    private ClientProfiles() {
    }

    /** The default order: the first client that returns downloadable formats wins. */
    public static List<ClientProfile> defaults() {
        return DEFAULTS;
    }
}
