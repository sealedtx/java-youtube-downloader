package com.github.kiulian.downloader;

import com.github.kiulian.downloader.model.YoutubeVideo;
import com.github.kiulian.downloader.model.subtitles.SubtitlesInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.github.kiulian.downloader.TestUtils.DESPACITO_ID;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests extracting metadata for youtube subtitles")
public class YoutubeSubtitleExtractor_Tests {

    @Test
    void getSubtitlesInfo_ExtractFromCaptions_Success() {
        YoutubeDownloader downloader = new YoutubeDownloader();

        assertDoesNotThrow(() -> {
            YoutubeVideo video = downloader.getVideo(DESPACITO_ID);

            List<SubtitlesInfo> subtitlesInfos = video.subtitles();
            assertFalse(subtitlesInfos.isEmpty(), "subtitles info should not be empty");

        });
    }

    @Test
    void getSubtitlesInfo_ExtractSubtitles_Success() {
        YoutubeDownloader downloader = new YoutubeDownloader();

        assertDoesNotThrow(() -> {
            List<SubtitlesInfo> subtitlesInfos = downloader.getVideoSubtitles(DESPACITO_ID);
            assertFalse(subtitlesInfos.isEmpty(), "subtitles info should not be empty");
        });
    }
}
