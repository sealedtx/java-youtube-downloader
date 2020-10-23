package com.github.kiulian.downloader;

import com.alibaba.fastjson.JSONObject;
import com.github.kiulian.downloader.model.ProxyWrapper;
import com.github.kiulian.downloader.model.VideoDetails;
import com.github.kiulian.downloader.parser.DefaultParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import static com.github.kiulian.downloader.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests extracting metadata for youtube live streams")
class YoutubeLiveStreamExtractor_Tests {

    YoutubeDownloader downloader = new YoutubeDownloader();

    @Test
    @DisplayName("We should be able to get the HLS Stream URL for a live stream")
    void getLiveStreamHLS_Success() {
        String htmlUrl = "https://www.youtube.com/watch?v=" + LIVE_ID;

        DefaultParser parser = new DefaultParser(downloader);

        assertDoesNotThrow(() -> {
            JSONObject ytPlayerConfig = parser.getPlayerConfig(htmlUrl);

            VideoDetails details = parser.getVideoDetails(ytPlayerConfig);

            assertEquals(LIVE_ID, details.videoId(), "videoId should be " + LIVE_ID);

            assertTrue(details.isLive(), "this should be a live video");
            assertNotNull(details.liveUrl(), "there should be a live video url");
            assertTrue(isReachable(details.liveUrl(), downloader.proxyWrapper), "url should be reachable");
        });
    }

    @Test
    @DisplayName("We should be able to get formats for a video that was live")
    void getWasLiveFormats_Success() {
        String htmlUrl = "https://www.youtube.com/watch?v=" + WAS_LIVE_ID;

        DefaultParser parser = new DefaultParser(downloader);

        assertDoesNotThrow(() -> {
            JSONObject ytPlayerConfig = parser.getPlayerConfig(htmlUrl);
            VideoDetails details = parser.getVideoDetails(ytPlayerConfig);

            assertEquals(WAS_LIVE_ID, details.videoId(), "videoId should be " + WAS_LIVE_ID);
            assertTrue(details.isLiveContent(), "videoId was live ");
            assertFalse(details.thumbnails().isEmpty(), "thumbnails should be not empty");
            assertNotEquals(0, details.lengthSeconds(), "length should not be 0");
            assertNotEquals(0, details.viewCount(), "viewCount should not be 0");
        });
    }

}