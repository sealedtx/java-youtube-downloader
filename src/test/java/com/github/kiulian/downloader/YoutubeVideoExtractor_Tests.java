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

import java.util.List;

import static com.github.kiulian.downloader.TestUtils.*;
import static com.github.kiulian.downloader.TestUtils.ME_AT_THE_ZOO_ID;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Tests extracting metadata for youtube videos")
public class YoutubeVideoExtractor_Tests {

    @Test
    @DisplayName("getVideo should be successful for default videos without signature")
    void getVideo_WithoutSignature_Success() {
        YoutubeDownloader downloader = new YoutubeDownloader();

        assertDoesNotThrow(() -> {
            YoutubeVideo video = downloader.getVideo(ME_AT_THE_ZOO_ID);

            VideoDetails details = video.details();
            assertEquals(ME_AT_THE_ZOO_ID, details.videoId(), "videoId should be " + ME_AT_THE_ZOO_ID);

            String title = "Me at the zoo";
            assertEquals(title, details.title(), "title should be " + title);
            assertFalse(details.thumbnails().isEmpty(), "thumbnails should be not empty");
            assertNotEquals(0, details.lengthSeconds(), "length should not be 0");
            assertNotEquals(0, details.viewCount(), "viewCount should not be 0");

            List<Format> formats = video.formats();
            assertFalse(formats.isEmpty(), "formats should not be empty");

            int itag = 18;
            Format format = video.findFormatByItag(itag);
            assertNotNull(format, "findFormatByItag should return not null format");
            assertTrue(format instanceof AudioVideoFormat, "format with itag " + itag + " should be instance of AudioVideoFormat");
            assertEquals(itag, format.itag().id(), "itag should be " + itag);

            int expectedWidth = 320;
            Integer width = ((AudioVideoFormat) format).width();
            assertNotNull(width, "width should not be null");
            assertEquals(expectedWidth, width.intValue(), "format with itag " + itag + " should have width " + expectedWidth);

            int expectedHeight = 240;
            Integer height = ((AudioVideoFormat) format).height();
            assertNotNull(height, "height should not be null");
            assertEquals(expectedHeight, height.intValue(), "format with itag " + itag + " should have height " + expectedHeight);

            AudioQuality expectedAudioQuality = AudioQuality.low;
            assertEquals(expectedAudioQuality, ((AudioVideoFormat) format).audioQuality(), "audioQuality should be " + expectedAudioQuality.name());

            String expectedMimeType = "video/mp4";
            assertTrue(format.mimeType().contains(expectedMimeType), "mimetype should be " + expectedMimeType);

            Extension expectedExtension = Extension.MP4;
            assertEquals(expectedExtension, format.extension(), "extension should be " + expectedExtension.value());

            String expectedLabel = "240p";
            assertEquals(expectedLabel, ((AudioVideoFormat) format).qualityLabel(), "qualityLable should be " + expectedLabel);

            assertNotNull(format.url(), "url should not be null");

            assertTrue(isReachable(format.url()), "url should be reachable");
        });
    }

    @Test
    @DisplayName("getVideo should be successful for default videos with signature")
    void getVideo_WithSignature_Success() {
        YoutubeDownloader downloader = new YoutubeDownloader();

        assertDoesNotThrow(() -> {
            YoutubeVideo video = downloader.getVideo(DESPACITO_ID);

            VideoDetails details = video.details();
            assertEquals(DESPACITO_ID, details.videoId(), "videoId should be " + DESPACITO_ID);

            String title = "Luis Fonsi - Despacito ft. Daddy Yankee";
            assertEquals(title, details.title(), "title should be " + title);
            assertFalse(details.thumbnails().isEmpty(), "thumbnails should be not empty");
            assertNotEquals(0, details.lengthSeconds(), "length should not be 0");
            assertNotEquals(0, details.viewCount(), "viewCount should not be 0");

            List<Format> formats = video.formats();
            assertFalse(formats.isEmpty(), "formats should not be empty");

            int itag = 137;
            Format format = video.findFormatByItag(itag);
            assertNotNull(format, "findFormatByItag should return not null format");
            assertTrue(format instanceof VideoFormat, "format with itag " + itag + " should be instance of AudioVideoFormat");
            assertEquals(itag, format.itag().id(), "itag should be " + itag);

            assertNotEquals(0, ((VideoFormat) format).fps(), "fps should not be 0");

            int expectedWidth = 1920;
            Integer width = ((VideoFormat) format).width();
            assertNotNull(width, "width should not be null");
            assertEquals(expectedWidth, width.intValue(), "format with itag " + itag + " should have width " + expectedWidth);

            int expectedHeight = 1080;
            Integer height = ((VideoFormat) format).height();
            assertNotNull(height, "height should not be null");
            assertEquals(expectedHeight, height.intValue(), "format with itag " + itag + " should have height " + expectedHeight);

            String expectedMimeType = "video/mp4";
            assertTrue(format.mimeType().contains(expectedMimeType), "mimetype should be " + expectedMimeType);

            Extension expectedExtension = Extension.MP4;
            assertEquals(expectedExtension, format.extension(), "extension should be " + expectedExtension.value());

            String expectedLabel = "1080p";
            assertEquals(expectedLabel, ((VideoFormat) format).qualityLabel(), "qualityLable should be " + expectedLabel);

            assertNotNull(format.url(), "url should not be null");

            assertTrue(isReachable(format.url()), "url should be reachable");
        });
    }

    @Test
    @DisplayName("getVideo should throws exception for unavailable video")
    void getVideo_Unavailable_ThrowsException() {
        YoutubeDownloader downloader = new YoutubeDownloader();
        String unavailableVideoId = "12345678901";
        assertThrows(YoutubeException.BadPageException.class, () -> {
            downloader.getVideo(unavailableVideoId);
        });
    }

    @Test
    @DisplayName("addInitialFunctionPattern should add regex with priority to initialFunctionPatterns")
    void addInitialFunctionPattern_Success() {
        YoutubeDownloader downloader = new YoutubeDownloader();
        downloader.addCipherFunctionPattern(0, "([a-zA-Z0-9$]+)\\s*=\\s*function\\(\\s*a\\s*\\)\\s*\\{\\s*a\\s*=\\s*a\\.split\\(\\s*\"\"\\s*\\)");
        String videoId = "SmM0653YvXU";

        assertThrows(YoutubeException.CipherException.class, () -> {
            YoutubeVideo video = downloader.getVideo(videoId);
        }, "getVideo should throw CipherException if initial function patterns has wrong priority");

        downloader.addCipherFunctionPattern(0, "\\b([a-zA-Z0-9$]{2})\\s*=\\s*function\\(\\s*a\\s*\\)\\s*\\{\\s*a\\s*=\\s*a\\.split\\(\\s*\"\"\\s*\\)");
        assertDoesNotThrow(() -> {
            YoutubeVideo video = downloader.getVideo(videoId);
        }, "getVideo should not throw exception if initial function patterns has correct priority");
    }

}
