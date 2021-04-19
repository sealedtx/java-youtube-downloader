package com.github.kiulian.downloader;

import com.github.kiulian.downloader.downloader.request.RequestVideoInfo;
import com.github.kiulian.downloader.downloader.response.Response;
import com.github.kiulian.downloader.model.Extension;
import com.github.kiulian.downloader.model.videos.VideoDetails;
import com.github.kiulian.downloader.model.videos.VideoInfo;
import com.github.kiulian.downloader.model.videos.formats.*;
import com.github.kiulian.downloader.model.videos.quality.AudioQuality;
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
            Response<VideoInfo> response = downloader.getVideoInfo(new RequestVideoInfo(ME_AT_THE_ZOO_ID));
            assertTrue(response.ok());
            VideoInfo video = response.data();

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

            Extension expectedExtension = Extension.MPEG4;
            assertEquals(expectedExtension, format.extension(), "extension should be " + expectedExtension.value());

            String expectedLabel = "240p";
            assertEquals(expectedLabel, ((AudioVideoFormat) format).qualityLabel(), "qualityLable should be " + expectedLabel);

            assertNotNull(format.url(), "url should not be null");

            assertTrue(isReachable(format.url()), "url should be reachable");
        });
    }

    @Test
    @DisplayName("getVideo should throw exception for unavailable video")
    void getVideo_Unavailable_ThrowsException() {
        YoutubeDownloader downloader = new YoutubeDownloader();
        String unavailableVideoId = "12345678901";
        Response<VideoInfo> response = downloader.getVideoInfo(new RequestVideoInfo(unavailableVideoId));
        assertFalse(response.ok());
        assertTrue(response.error() instanceof YoutubeException.BadPageException);
    }

}
