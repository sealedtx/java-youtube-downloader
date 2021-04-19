package com.github.kiulian.downloader;

import com.github.kiulian.downloader.downloader.request.RequestVideoInfo;
import com.github.kiulian.downloader.downloader.response.Response;
import com.github.kiulian.downloader.model.videos.VideoDetails;
import com.github.kiulian.downloader.model.videos.VideoInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import static com.github.kiulian.downloader.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests extracting metadata for youtube live streams")
class YoutubeLiveStreamExtractor_Tests {

    @Test
    @DisplayName("We should be able to get the HLS Stream URL for a live stream")
    void getLiveStreamHLS_Success() {
        YoutubeDownloader downloader = new YoutubeDownloader();

        assertDoesNotThrow(() -> {
            Response<VideoInfo> response = downloader.getVideoInfo(new RequestVideoInfo(LIVE_ID));
            assertTrue(response.ok());
            VideoInfo video = response.data();
            VideoDetails details = video.details();

            assertEquals(LIVE_ID, details.videoId(), "videoId should be " + LIVE_ID);

            assertTrue(details.isLive(), "this should be a live video");
            assertNotNull(details.liveUrl(), "there should be a live video url");
            assertTrue(isReachable(details.liveUrl()), "url should be reachable");
        });
    }

    @Test
    @DisplayName("We should be able to get formats for a video that was live")
    void getWasLiveFormats_Success() {
        YoutubeDownloader downloader = new YoutubeDownloader();

        assertDoesNotThrow(() -> {
            Response<VideoInfo> response = downloader.getVideoInfo(new RequestVideoInfo(WAS_LIVE_ID));
            assertTrue(response.ok());
            VideoInfo video = response.data();
            VideoDetails details = video.details();

            assertEquals(WAS_LIVE_ID, details.videoId(), "videoId should be " + WAS_LIVE_ID);
            assertTrue(details.isLiveContent(), "videoId was live ");
            assertFalse(details.thumbnails().isEmpty(), "thumbnails should be not empty");
            assertNotEquals(0, details.lengthSeconds(), "length should not be 0");
            assertNotEquals(0, details.viewCount(), "viewCount should not be 0");
        });
    }

}