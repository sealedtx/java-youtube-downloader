package com.github.kiulian.downloader;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.*;

import com.github.kiulian.downloader.downloader.request.*;
import com.github.kiulian.downloader.model.search.SearchResult;

@DisplayName("Compare execution times of plain and gzip responses")
public class YoutubeCompressionSpeed_Tests extends TestUtils {

    private static interface Task {
        void process() throws Exception;
    }

    private void execute(String title, Task task) {
        long time;

        // Compression disabled
        downloader.getConfig().setCompressionEnabled(false);
        time = System.currentTimeMillis();
        assertDoesNotThrow(() -> task.process());
        long plainTime = System.currentTimeMillis() - time;

        // Compression enabled
        downloader.getConfig().setCompressionEnabled(true);
        time = System.currentTimeMillis();
        assertDoesNotThrow(() -> task.process());
        long gzipTime = System.currentTimeMillis() - time;

        double ratio = (double) gzipTime / (double) plainTime;
        System.out.println(title + " - " + gzipTime + "/" + plainTime + " > " + ratio);
//        assertTrue(ratio < 1, title + " - gzip execution should be faster than plain");
    }

    private YoutubeDownloader downloader;

    @BeforeEach
    void initDownloader() {
        this.downloader = new YoutubeDownloader();
    }

    @BeforeAll
    static void warmUp() {
        new YoutubeDownloader().getVideoInfo(new RequestVideoInfo(TestUtils.LIVE_ID)).data();
    }

    @Test
    @DisplayName("get video speed test should be successful")
    void getVideoSpeed_Success() {
        execute("get video", () -> {
            downloader.getVideoInfo(new RequestVideoInfo(TestUtils.ME_AT_THE_ZOO_ID)).data();
            downloader.getVideoInfo(new RequestVideoInfo(TestUtils.N3WPORT_ID)).data();
        });
    }

    @Test
    @DisplayName("get playlist speed test should be successful")
    void getPlaylistSpeed_Success() {
        execute("get playlist", () -> {
            downloader.getPlaylistInfo(new RequestPlaylistInfo(YoutubePlaylistTest.ASK_NASA_PLAYLIST_ID)).data();
            downloader.getPlaylistInfo(new RequestPlaylistInfo(YoutubePlaylistTest.LOTR_PLAYLIST_ID)).data();
        });
    }

    @Test
    @DisplayName("get channel speed test should be successful")
    void getChannelSpeed_Success() {
        execute("get channel", () -> {
            downloader.getChannelUploads(new RequestChannelUploads(YoutubeChannelUploads_Tests.CHANNELID)).data();
            downloader.getChannelUploads(new RequestChannelUploads(YoutubeChannelUploads_Tests.MUSICCHANNELID)).data();
        });
    }

    @Test
    @DisplayName("search speed test should be successful")
    void searchSpeed_Success() {
        execute("search", () -> {
            SearchResult result = downloader.search(new RequestSearchResult("nasa")).data();
            downloader.searchContinuation(new RequestSearchContinuation(result)).data();
            result = downloader.search(new RequestSearchResult("science")).data();
            downloader.searchContinuation(new RequestSearchContinuation(result)).data();
        });
    }
}
