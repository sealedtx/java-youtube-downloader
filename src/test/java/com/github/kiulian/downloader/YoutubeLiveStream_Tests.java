package com.github.kiulian.downloader;

import com.github.kiulian.downloader.model.Extension;
import com.github.kiulian.downloader.model.VideoDetails;
import com.github.kiulian.downloader.model.YoutubeVideo;
import com.github.kiulian.downloader.model.formats.AudioVideoFormat;
import com.github.kiulian.downloader.model.formats.Format;
import com.github.kiulian.downloader.model.formats.VideoFormat;
import com.github.kiulian.downloader.model.quality.AudioQuality;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

// TODO: add tests for different itags

@DisplayName("Youtube downloader tests")
class YoutubeLiveStream_Tests {

    private static final String LIVE_TEST_1 = "Zp9zBmwK4uQ";
    private static final String WAS_LIVE_TEST_BUT_NOT_PUBLIC = "IsMZPeaD6k0";
    private static final String WAS_LIVE_TEST_BUT_PUBLIC = "IsMZPeaD6k0"; //"Aymrnzianf0";
    @Test
    @DisplayName("We should be able to get the HLS Stream URL for a live stream")
    void getLiveStreamHLS_Success() {
        YoutubeDownloader downloader = new YoutubeDownloader();

        assertDoesNotThrow(() -> {
            YoutubeVideo video = downloader.getVideo(LIVE_TEST_1);

            VideoDetails details = video.details();
            assertEquals(LIVE_TEST_1, details.videoId(), "videoId should be " + LIVE_TEST_1);

            List<Format> formats = video.formats();
            assertFalse(formats.isEmpty(), "formats should not be empty");
            assertTrue(video.details().isLive(), "this should be a live video");
            assertNotNull(video.details().liveUrl(), "there should be a live video url");
            assertTrue(isReachable(video.details().liveUrl()), "url should be reachable");
        });
    }

    @Test
    @DisplayName("We should be able to get formats for a video that was live")
    void getWasLiveFormats_Success() {
        YoutubeDownloader downloader = new YoutubeDownloader();

        assertDoesNotThrow(() -> {
            YoutubeVideo video = downloader.getVideo(WAS_LIVE_TEST_BUT_PUBLIC);

            VideoDetails details = video.details();
            assertEquals(WAS_LIVE_TEST_BUT_PUBLIC, details.videoId(), "videoId should be " + WAS_LIVE_TEST_BUT_PUBLIC);
            assertTrue(details.isLiveContent(), "videoId was live ");
            assertFalse(details.thumbnails().isEmpty(), "thumbnails should be not empty");
            assertNotEquals(0, details.lengthSeconds(), "length should not be 0");
            assertNotEquals(0, details.viewCount(), "viewCount should not be 0");

            List<Format> formats = video.formats();
            assertFalse(formats.isEmpty(), "formats should not be empty");

        });
    }

    private static boolean isReachable(String url) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(1000);
            connection.setReadTimeout(1000);
            connection.setRequestMethod("HEAD");
            int responseCode = connection.getResponseCode();
            return (200 <= responseCode && responseCode <= 399);
        } catch (IOException exception) {
            return false;
        }
    }
}


//https://manifest.googlevideo.com/api/manifest/hls_variant/expire/1591840371/ei/EjrhXtf6O8LK1wKVyoToDw/ip/188.192.58.15/id/IsMZPeaD6k0.1/source/yt_live_broadcast/requiressl/yes/hfr/1/playlist_duration/30/manifest_duration/30/maudio/1/gcr/de/vprv/1/go/1/keepalive/yes/dover/11/itag/0/playlist_type/DVR/sparams/expire%2Cei%2Cip%2Cid%2Csource%2Crequiressl%2Chfr%2Cplaylist_duration%2Cmanifest_duration%2Cmaudio%2Cgcr%2Cvprv%2Cgo%2Citag%2Cplaylist_type/sig/AOq0QJ8wRgIhAJNhfRbmfWN817cAC6Pe7BtGt5ROFXKA_6yyllMZIY56AiEAiNsrmugWn_IJTL07r_kI4vaZUCo9YjTjty8VkS8s-EE%3D/file/index.m3u8
