package com.github.kiulian.downloader;

import com.github.kiulian.downloader.downloader.request.RequestSubtitlesInfo;
import com.github.kiulian.downloader.downloader.request.RequestVideoInfo;
import com.github.kiulian.downloader.downloader.response.Response;
import com.github.kiulian.downloader.model.Extension;
import com.github.kiulian.downloader.model.subtitles.SubtitlesInfo;
import com.github.kiulian.downloader.model.videos.VideoInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.github.kiulian.downloader.TestUtils.N3WPORT_ID;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests extracting metadata for youtube subtitles")
public class YoutubeSubtitlesExtractor_Tests {

    @Test
    @DisplayName("extracting subtitles from video captions should work")
    void getSubtitlesInfo_ExtractFromCaptions_Success() {
        YoutubeDownloader downloader = new YoutubeDownloader();

        assertDoesNotThrow(() -> {
            Response<VideoInfo> response = downloader.getVideoInfo(new RequestVideoInfo(N3WPORT_ID));
            assertTrue(response.ok());
            VideoInfo video = response.data();

            List<SubtitlesInfo> subtitlesInfos = video.subtitles();
            assertFalse(subtitlesInfos.isEmpty(), "subtitles info should not be empty");
        });
    }

    @Test
    @DisplayName("extracting subtitles by video id should work")
    void getSubtitlesInfo_ExtractSubtitles_Success() {
        YoutubeDownloader downloader = new YoutubeDownloader();

        assertDoesNotThrow(() -> {
            Response<List<SubtitlesInfo>> response = downloader.getSubtitlesInfo(new RequestSubtitlesInfo(N3WPORT_ID));
            assertTrue(response.ok());
            List<SubtitlesInfo> subtitlesInfos = response.data();
            assertFalse(subtitlesInfos.isEmpty(), "subtitles info should not be empty");
        });
    }

    @Test
    @DisplayName("getDownloadUrl should return url with specified query params")
    void getDownloadUrl_Success() {
        YoutubeDownloader downloader = new YoutubeDownloader();
        assertDoesNotThrow(() -> {
            Response<List<SubtitlesInfo>> response = downloader.getSubtitlesInfo(new RequestSubtitlesInfo(N3WPORT_ID));
            assertTrue(response.ok());
            List<SubtitlesInfo> subtitlesInfos = response.data();
            for (SubtitlesInfo info : subtitlesInfos) {
                String downloadUrl = info.getSubtitles().getDownloadUrl();
                assertEquals(info.getUrl(), downloadUrl, "download url should be equals to info url");

                downloadUrl = info.getSubtitles()
                        .formatTo(Extension.JSON3)
                        .getDownloadUrl();
                assertTrue(downloadUrl.contains("&fmt=" + Extension.JSON3.value()), "download url should contains format query param");
            }
        });
    }
}