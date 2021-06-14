package com.github.kiulian.downloader;

import com.alibaba.fastjson.JSONObject;
import com.github.kiulian.downloader.downloader.YoutubeCallback;
import com.github.kiulian.downloader.downloader.YoutubeProgressCallback;
import com.github.kiulian.downloader.downloader.request.*;
import com.github.kiulian.downloader.downloader.response.Response;
import com.github.kiulian.downloader.downloader.response.ResponseStatus;
import com.github.kiulian.downloader.model.Extension;
import com.github.kiulian.downloader.model.videos.VideoInfo;
import com.github.kiulian.downloader.model.videos.formats.Format;
import com.github.kiulian.downloader.model.subtitles.SubtitlesInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.github.kiulian.downloader.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Tests downloading youtube videos")
class YoutubeDownloader_Tests {

    private List<SubtitlesInfo> getSubtitles() {
        Response<List<SubtitlesInfo>> response = downloader.getSubtitlesInfo(new RequestSubtitlesInfo(N3WPORT_ID));
        assertTrue(response.ok());
        return response.data();
    }

    private Format getFormat() {
        Response<VideoInfo> response = downloader.getVideoInfo(new RequestVideoInfo(ME_AT_THE_ZOO_ID));
        assertTrue(response.ok());
        VideoInfo video = response.data();

        int itag = 18;
        return video.findFormatByItag(itag);
    }

    private void validateFileDownloadAndDelete(Format format, File file) {
        validateFileDownloadAndDelete(format, file, null, false);
    }

    private void validateFileDownloadAndDelete(Format format, File file, String fileName) {
        validateFileDownloadAndDelete(format, file, fileName, false);
    }

    private void validateFileDownloadAndDelete(Format format, File file, String fileName, boolean overwrite) {
        assertTrue(file.exists(), "file should be downloaded");

        Extension extension = format.extension();
        assertTrue(file.getName().endsWith(extension.value()), "file name should ends with: " + extension.value());

        assertTrue(file.length() > 0, "file should be not empty");
        if (fileName != null) {
            if (overwrite) {
                String actualFileName = fileName + "." + format.extension().value();
                assertEquals(file.getName(), actualFileName, "file name should be: " + actualFileName);
            } else {
                assertTrue(file.getName().contains(fileName), "file name should contains: " + fileName);
            }
        }
        file.delete();
    }

    private YoutubeDownloader downloader;

    @BeforeEach
    void initDownloader() {
        downloader = new YoutubeDownloader();
    }

    @Test
    @DisplayName("download video should work")
    void downloadVideo_Success() {
        File outDir = new File("videos");
        String fileName = "myAwesomeName";

        Format format = getFormat();
        assertNotNull(format, "findFormatByItag should return not null format");
        assertTrue(isReachable(format.url()), "url should be reachable");

        assertDoesNotThrow(() -> {
            Response<File> responseFile = downloader.downloadVideo(new RequestVideoDownload(format).saveTo(outDir));
            assertTrue(responseFile.ok());

            File file = responseFile.data();
            validateFileDownloadAndDelete(format, file);
        }, "download video sync should work");

        assertDoesNotThrow(() -> {
            Response<File> responseFile = downloader.downloadVideo(new RequestVideoDownload(format).saveTo(outDir).async());
            assertEquals(ResponseStatus.downloading, responseFile.status());

            File file = responseFile.data(5, TimeUnit.SECONDS);
            validateFileDownloadAndDelete(format, file);
        }, "download video should work async future");

        assertDoesNotThrow(() -> {

            RequestVideoDownload request = new RequestVideoDownload(format).saveTo(outDir).renameTo(fileName);
            Response<File> responseFile = downloader.downloadVideo(request);
            assertTrue(responseFile.ok());

            File file = responseFile.data();
            validateFileDownloadAndDelete(format, file, fileName);
        }, "download video sync with specified output file name should work");

        assertDoesNotThrow(() -> {
            RequestVideoDownload request = new RequestVideoDownload(format).saveTo(outDir).renameTo(fileName).overwriteIfExists(true);
            Response<File> responseFile = downloader.downloadVideo(request);
            assertTrue(responseFile.ok());

            File file = responseFile.data();
            validateFileDownloadAndDelete(format, file, fileName, true);
        }, "download video sync with specified output file name and overwrite flag should work");


        assertDoesNotThrow(() -> {
            YoutubeProgressCallback<File> mockCallback = mock(YoutubeProgressCallback.class);
            RequestVideoDownload request = new RequestVideoDownload(format).callback(mockCallback).async();
            Response<File> responseFile = downloader.downloadVideo(request);

            assertTimeout(Duration.ofSeconds(5), () -> {
                assertTrue(responseFile.ok());
                File file = responseFile.data();

                validateFileDownloadAndDelete(format, file);
            });

            verify(mockCallback, atLeastOnce()).onFinished(any(File.class));
            verify(mockCallback, atLeastOnce()).onDownloading(anyInt());
        }, "download video async with callback should work");

        assertDoesNotThrow(() -> {
            RequestVideoDownload request = new RequestVideoDownload(format).renameTo(fileName).async();
            Response<File> responseFile = downloader.downloadVideo(request);

            assertTimeout(Duration.ofSeconds(5), () -> {
                assertTrue(responseFile.ok());
                File file = responseFile.data();

                validateFileDownloadAndDelete(format, file, fileName);
            });

        }, "download video async with specified output file name should work");

        assertDoesNotThrow(() -> {
            RequestVideoDownload request = new RequestVideoDownload(format).renameTo(fileName).overwriteIfExists(true).async();
            Response<File> responseFile = downloader.downloadVideo(request);

            assertTimeout(Duration.ofSeconds(5), () -> {
                assertTrue(responseFile.ok());
                File file = responseFile.data();

                validateFileDownloadAndDelete(format, file, fileName, true);
            });
        }, "download video async with specified output file name and overwrite flag should work");
    }

    @Test
    @DisplayName("download subtitle should work")
    void downloadSubtitle_Success() {
        List<SubtitlesInfo> subtitlesInfo = getSubtitles();
        assertFalse(subtitlesInfo.isEmpty(), "subtitles should be not empty");

        assertDoesNotThrow(() -> {
            for (SubtitlesInfo subtitleInfo : subtitlesInfo) {
                RequestSubtitlesDownload request = new RequestSubtitlesDownload(subtitleInfo);
                Response<String> responseSubtitle = downloader.downloadSubtitle(request);
                assertTrue(responseSubtitle.ok());
                assertFalse(responseSubtitle.data().isEmpty(), "subtitles should not be empty");
            }
        }, "download subtitle should work");

        assertDoesNotThrow(() -> {
            for (SubtitlesInfo info : subtitlesInfo) {
                SubtitlesInfo subtitleInfo = new SubtitlesInfo(info.getUrl().replace("lang=" + info.getLanguage(), "lang=not_a_code"), "not_a_code", false);
                RequestSubtitlesDownload request = new RequestSubtitlesDownload(subtitleInfo);
                Response<String> responseSubtitle = downloader.downloadSubtitle(request);
                assertFalse(responseSubtitle.ok());
                assertNotNull(responseSubtitle.error(), "error should be not null");
            }
        }, "download subtitles with wrong lang code should throw exception");

        assertDoesNotThrow(() -> {
            for (SubtitlesInfo subtitleInfo : subtitlesInfo) {
                RequestWebpage request = new RequestSubtitlesDownload(subtitleInfo)
                        .async();
                Response<String> responseSubtitle = downloader.downloadSubtitle(request);

                assertTimeout(Duration.ofSeconds(5), () -> {
                    String subtitles = responseSubtitle.data();
                    assertNotNull(subtitles, "subtitles should be not null");
                    assertFalse(subtitles.isEmpty(), "subtitles should not be empty");
                });
            }
        }, "download subtitle async should work");

        assertDoesNotThrow(() -> {
            YoutubeCallback<String> callback = Mockito.mock(YoutubeCallback.class);

            for (SubtitlesInfo subtitleInfo : subtitlesInfo) {
                RequestWebpage request = new RequestSubtitlesDownload(subtitleInfo)
                        .callback(callback)
                        .async();
                Response<String> responseSubtitle = downloader.downloadSubtitle(request);

                assertTimeout(Duration.ofSeconds(5), () -> {
                    String subtitles = responseSubtitle.data();
                    assertNotNull(subtitles, "subtitles should be not null");
                    assertFalse(subtitles.isEmpty(), "subtitles should not be empty");
                });
            }

            verify(callback, times(subtitlesInfo.size())).onFinished(any());
            verify(callback, never()).onError(any());
        }, "download subtitles async with callback should call onFinished");

        assertDoesNotThrow(() -> {
            YoutubeCallback<String> callback = Mockito.mock(YoutubeCallback.class);

            for (SubtitlesInfo subtitleInfo : subtitlesInfo) {
                subtitleInfo = new SubtitlesInfo(subtitleInfo.getUrl().replace("lang=" + subtitleInfo.getLanguage(), "lang=not_a_code"), "not_a_code", false);

                RequestWebpage request = new RequestSubtitlesDownload(subtitleInfo)
                        .callback(callback)
                        .async();
                Response<String> responseSubtitle = downloader.downloadSubtitle(request);

                assertTimeout(Duration.ofSeconds(5), () -> {
                    assertNotNull(responseSubtitle.error(), "error should be not null");
                    assertNull(responseSubtitle.data(), "subtitles should be null");
                });

            }
            verify(callback, times(subtitlesInfo.size())).onError(any());
            verify(callback, never()).onFinished(any());
        }, "download subtitles async with callback and wrong lang code should call onError");

        assertDoesNotThrow(() -> {
            for (SubtitlesInfo subtitleInfo : subtitlesInfo) {
                RequestSubtitlesDownload request = new RequestSubtitlesDownload(subtitleInfo)
                        .formatTo(Extension.JSON3);
                Response<String> responseSubtitle = downloader.downloadSubtitle(request);
                assertTrue(responseSubtitle.ok());
                String subtitles = responseSubtitle.data();
                assertFalse(subtitles.isEmpty(), "subtitles should not be empty");
                assertDoesNotThrow(() -> {
                    JSONObject.parseObject(subtitles);
                }, "subtitles should be formatted to json");
            }
        }, "download formatted subtitles should work");

        assertDoesNotThrow(() -> {
            for (SubtitlesInfo subtitleInfo : subtitlesInfo) {
                RequestSubtitlesDownload request = new RequestSubtitlesDownload(subtitleInfo)
                        .formatTo(Extension.JSON3)
                        .translateTo("uk");
                Response<String> responseSubtitle = downloader.downloadSubtitle(request);
                assertTrue(responseSubtitle.ok());
                String subtitles = responseSubtitle.data();
                assertFalse(subtitles.isEmpty(), "subtitles should not be empty");
                assertDoesNotThrow(() -> {
                    JSONObject.parseObject(subtitles);
                }, "subtitles should be formatted to json");
            }
        }, "download formatted and translated subtitles should work");
    }

    //    @Test // currently disabled because even on youtube.com translate feature does not work
    @DisplayName("download formatted and translated subtitles should work")
    void downloadSubtitles_FormattedTranslatedFromCaptions_Success() {
        YoutubeDownloader downloader = new YoutubeDownloader();
        assertDoesNotThrow(() -> {
            Response<VideoInfo> response = downloader.getVideoInfo(new RequestVideoInfo(N3WPORT_ID));
            assertTrue(response.ok());
            VideoInfo video = response.data();
            List<SubtitlesInfo> subtitlesInfo = video.subtitlesInfo();

            for (SubtitlesInfo subtitleInfo : subtitlesInfo) {
                RequestSubtitlesDownload request = new RequestSubtitlesDownload(subtitleInfo)
                        .formatTo(Extension.JSON3)
                        .translateTo("uk");
                Response<String> responseSubtitle = downloader.downloadSubtitle(request);
                assertEquals(ResponseStatus.completed, responseSubtitle.status());
                assertTrue(responseSubtitle.ok());
                String subtitle = responseSubtitle.data();
                assertFalse(subtitle.isEmpty(), "subtitles should not be empty");
                assertDoesNotThrow(() -> {
                    JSONObject.parseObject(subtitle);
                }, "subtitles should be formatted to json");
            }
        });
    }


}
