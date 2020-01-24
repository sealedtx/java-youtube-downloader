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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// TODO: add tests for different itags

@DisplayName("Youtube downloader tests")
class YoutubeDownloader_Tests {

    @Test
    @DisplayName("getVideo should be successful for default videos without signature")
    void getVideo_WithoutSignature_Success() {
        YoutubeDownloader downloader = new YoutubeDownloader();
        String videoId = "jNQXAC9IVRw"; // me in the zoo
        String title = "Me at the zoo";

        assertDoesNotThrow(() -> {
            YoutubeVideo video = downloader.getVideo(videoId);

            VideoDetails details = video.details();
            assertEquals(videoId, details.videoId(), "videoId should be " + videoId);
            assertEquals(title, details.title(), "title should be " + title);
            assertFalse(details.thumbnails().isEmpty(), "thumbnails should be not empty");
            assertNotEquals(0, details.lengthSeconds(), "length should not be 0");
            assertNotEquals(0, details.viewCount(), "viewCount should not be 0");

            List<Format> formats = video.formats();
            assertFalse(formats.isEmpty(), "formats should not be empty");

            int itag = 43;
            Format format = video.findFormatByItag(itag);
            assertNotNull(format, "findFormatByItag should return not null format");
            assertTrue(format instanceof AudioVideoFormat, "format with itag " + itag + " should be instance of AudioVideoFormat");
            assertEquals(itag, format.itag().id(), "itag should be " + itag);

            Integer width = ((AudioVideoFormat) format).width();
            assertNotNull(width, "width should not be null");
            assertEquals(640, width.intValue(), "format with itag " + itag + " should have width " + 640);

            Integer height = ((AudioVideoFormat) format).height();
            assertNotNull(height, "height should not be null");
            assertEquals(360, height.intValue(), "format with itag " + itag + " should have width " + 360);

            assertEquals(AudioQuality.medium, ((AudioVideoFormat) format).audioQuality(), "audioQuality should be medium");

            assertTrue(format.mimeType().contains("video/webm"), "mimetype should be video/webm");
            assertEquals(Extension.WEBM, format.extension(), "extension should be webm");
            assertEquals("360p", ((AudioVideoFormat) format).qualityLabel(), "qualityLable should be 360p");

            assertNotNull(format.url(), "url should not be null");

            assertTrue(isReachable(format.url()));
        });
    }


    @Test
    @DisplayName("getVideo should be successful for default videos without signature")
    void getVideo_WithSignature_Success() {
        YoutubeDownloader downloader = new YoutubeDownloader();
        String videoId = "kJQP7kiw5Fk"; // despacito
        String title = "Luis Fonsi - Despacito ft. Daddy Yankee";

        assertDoesNotThrow(() -> {
            YoutubeVideo video = downloader.getVideo(videoId);

            VideoDetails details = video.details();
            assertEquals(videoId, details.videoId(), "videoId should be " + videoId);
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

            Integer width = ((VideoFormat) format).width();
            assertNotNull(width, "width should not be null");
            assertEquals(1920, width.intValue(), "format with itag " + itag + " should have width " + 1920);

            Integer height = ((VideoFormat) format).height();
            assertNotNull(height, "height should not be null");
            assertEquals(1080, height.intValue(), "format with itag " + itag + " should have width " + 1080);

            assertTrue(format.mimeType().contains("video/mp4"), "mimetype should be video/mp4");
            assertEquals(Extension.MP4, format.extension(), "extension should be mp4");
            assertEquals("1080p", ((VideoFormat) format).qualityLabel(), "qualityLable should be 1080p");

            assertNotNull(format.url(), "url should not be null");

            assertTrue(isReachable(format.url()));
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

    @Test
    @DisplayName("setRetryOnFailure should throw exception for invalid values")
    void setRetryOnFailure_InvalidRetryCount_ThrowsException() {
        YoutubeDownloader downloader = new YoutubeDownloader();
        assertThrows(IllegalArgumentException.class, () -> {
            downloader.setParserRetryOnFailure(-1);
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
