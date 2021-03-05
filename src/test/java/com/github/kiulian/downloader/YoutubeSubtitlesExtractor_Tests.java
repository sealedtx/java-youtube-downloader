package com.github.kiulian.downloader;

import com.github.kiulian.downloader.model.Extension;
import com.github.kiulian.downloader.model.YoutubeVideo;
import com.github.kiulian.downloader.model.subtitles.SubtitlesInfo;
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
            YoutubeVideo video = downloader.getVideo(N3WPORT_ID);

            List<SubtitlesInfo> subtitlesInfos = video.subtitles();
            assertFalse(subtitlesInfos.isEmpty(), "subtitles info should not be empty");
        });
    }

    @Test
    @DisplayName("extracting subtitles by video id should work")
    void getSubtitlesInfo_ExtractSubtitles_Success() {
        YoutubeDownloader downloader = new YoutubeDownloader();

        assertDoesNotThrow(() -> {
            List<SubtitlesInfo> subtitlesInfos = downloader.getVideoSubtitles(N3WPORT_ID);
            assertFalse(subtitlesInfos.isEmpty(), "subtitles info should not be empty");
        });
    }

    @Test
    @DisplayName("getDownloadUrl should return url with specified query params")
    void getDownloadUrl_Success() {
        YoutubeDownloader downloader = new YoutubeDownloader();
        assertDoesNotThrow(() -> {
            List<SubtitlesInfo> subtitlesInfos = downloader.getVideoSubtitles(N3WPORT_ID);
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
