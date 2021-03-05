package com.github.kiulian.downloader;

import com.alibaba.fastjson.JSONObject;
import com.github.kiulian.downloader.model.Extension;
import com.github.kiulian.downloader.model.YoutubeVideo;
import com.github.kiulian.downloader.model.formats.Format;
import com.github.kiulian.downloader.model.subtitles.OnSubtitlesDownloadListener;
import com.github.kiulian.downloader.model.subtitles.SubtitlesInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.github.kiulian.downloader.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Tests downloading youtube videos")
class YoutubeDownloader_Tests {

    @Test
    @DisplayName("download video sync should be successful")
    void downloadVideo_Sync_Success() {
        YoutubeDownloader downloader = new YoutubeDownloader();

        assertDoesNotThrow(() -> {
            YoutubeVideo video = downloader.getVideo(ME_AT_THE_ZOO_ID);

            int itag = 18;
            Format format = video.findFormatByItag(itag);
            assertNotNull(format, "findFormatByItag should return not null format");

            assertTrue(isReachable(format.url()), "url should be reachable");

            File outDir = new File("videos");
            assertDoesNotThrow(() -> {
                File file = video.download(format, outDir);
                assertTrue(outDir.exists(), "output directory should be created");

                assertTrue(file.exists(), "file should be downloaded");

                Extension extension = format.extension();
                assertTrue(file.getName().endsWith(extension.value()), "file name should ends with: " + extension.value());

                assertTrue(file.length() > 0, "file should be not empty");
            });

        });
    }

    @Test
    @DisplayName("download video async should be cancelable")
    void downloadVideo_AsyncCancel_Canceled() {
        YoutubeDownloader downloader = new YoutubeDownloader();

        assertDoesNotThrow(() -> {
            YoutubeVideo video = downloader.getVideo(ME_AT_THE_ZOO_ID);

            int itag = 18;
            Format format = video.findFormatByItag(itag);
            assertNotNull(format, "findFormatByItag should return not null format");

            assertTrue(isReachable(format.url()), "url should be reachable");

            File outDir = new File("videos");
            assertDoesNotThrow(() -> {
                OnYoutubeDownloadListener callback = Mockito.mock(OnYoutubeDownloadListener.class);

                Future<File> future = video.downloadAsync(format, outDir, callback);
                assertTrue(outDir.exists(), "output directory should be created");

                Thread.sleep(10);
                boolean canceled = future.cancel(true);
                assertTrue(canceled, "future task should be canceled");

                assertThrows(CancellationException.class, future::get);

                Thread.sleep(100);
                verify(callback, times(1)).onError(any(CancellationException.class));
            });

        });
    }

    @Test
    @DisplayName("download video should work async future")
    void downloadVideo_AsyncFuture_Success() {
        YoutubeDownloader downloader = new YoutubeDownloader();

        assertDoesNotThrow(() -> {
            YoutubeVideo video = downloader.getVideo(ME_AT_THE_ZOO_ID);

            int itag = 18;
            Format format = video.findFormatByItag(itag);
            assertNotNull(format, "findFormatByItag should return not null format");

            assertTrue(isReachable(format.url()), "url should be reachable");

            File outDir = new File("videos");
            assertDoesNotThrow(() -> {
                Future<File> future = video.downloadAsync(format, outDir);

                File file = future.get(5, TimeUnit.SECONDS);
                assertTrue(outDir.exists(), "output directory should be created");

                assertTrue(file.exists(), "file should be downloaded");

                Extension extension = format.extension();
                assertTrue(file.getName().endsWith(extension.value()), "file name should ends with: " + extension.value());

                assertTrue(file.length() > 0, "file should be not empty");
            });

        });
    }

    @Test
    @DisplayName("download video should work async callback")
    void downloadVideo_AsyncCallback_Success() {
        YoutubeDownloader downloader = new YoutubeDownloader();

        assertDoesNotThrow(() -> {
            YoutubeVideo video = downloader.getVideo(ME_AT_THE_ZOO_ID);

            int itag = 18;
            Format format = video.findFormatByItag(itag);
            assertNotNull(format, "findFormatByItag should return not null format");

            assertTrue(isReachable(format.url()), "url should be reachable");

            File outDir = new File("videos");

            char[] name = new char[200]; // Youtube title max length - 100 characters + file extension + duplication number
            video.downloadAsync(format, outDir, new OnYoutubeDownloadListener() {
                @Override
                public void onDownloading(int progress) {
                    System.out.printf("Downloading %d%%\n", progress);
                }

                @Override
                public void onFinished(File file) {
                    System.arraycopy(file.getName().toCharArray(), 0, name, 0, file.getName().length());
                    System.out.printf("Finished %s\n", file.getPath());
                }

                @Override
                public void onError(Throwable throwable) {
                    System.out.printf("Error %s\n", throwable.getMessage());
                }

            });

            int timeout = 5; // seconds
            while (--timeout > 0) {
                Thread.sleep(1000);
                if (!new String(name).trim().isEmpty())
                    break;
            }

            File file = new File(outDir, new String(name).trim());

            assertTrue(outDir.exists(), "output directory should be created");

            assertTrue(file.exists(), "file should be downloaded");

            Extension extension = format.extension();
            assertTrue(file.getName().endsWith(extension.value()), "file name should ends with: " + extension.value());

            assertTrue(file.length() > 0, "file should be not empty");
        });
    }

    @Test
    @DisplayName("download video sync with specified output file name should be successful")
    void downloadVideo_SyncSpecifiedName_Success() {
        YoutubeDownloader downloader = new YoutubeDownloader();
        String fileName = "myAwesomeName";
        assertDoesNotThrow(() -> {
            YoutubeVideo video = downloader.getVideo(ME_AT_THE_ZOO_ID);

            int itag = 18;
            Format format = video.findFormatByItag(itag);
            assertNotNull(format, "findFormatByItag should return not null format");

            assertTrue(isReachable(format.url()), "url should be reachable");

            File outDir = new File("videos");
            assertDoesNotThrow(() -> {
                File file = video.download(format, outDir, fileName);
                assertTrue(outDir.exists(), "output directory should be created");

                assertTrue(file.exists(), "file should be downloaded");
                assertTrue(file.getName().contains(fileName), "file name should contains: " + fileName);

                Extension extension = format.extension();
                assertTrue(file.getName().endsWith(extension.value()), "file name should ends with: " + extension.value());

                assertTrue(file.length() > 0, "file should be not empty");
            });

        });
    }

    @Test
    @DisplayName("download video sync with specified output file name and overwrite flag should be successful")
    void downloadVideo_SyncSpecifiedNameOverwrite_Success() {
        YoutubeDownloader downloader = new YoutubeDownloader();
        String fileName = "myAwesomeName";
        assertDoesNotThrow(() -> {
            YoutubeVideo video = downloader.getVideo(ME_AT_THE_ZOO_ID);

            int itag = 18;
            Format format = video.findFormatByItag(itag);
            assertNotNull(format, "findFormatByItag should return not null format");

            assertTrue(isReachable(format.url()), "url should be reachable");

            File outDir = new File("videos");
            assertDoesNotThrow(() -> {
                File file = video.download(format, outDir, fileName, true);
                assertTrue(outDir.exists(), "output directory should be created");

                assertTrue(file.exists(), "file should be downloaded");
                String actualFileName = fileName + "." + format.extension().value();
                assertEquals(file.getName(), actualFileName, "file name should be: " + actualFileName);

                Extension extension = format.extension();
                assertTrue(file.getName().endsWith(extension.value()), "file name should ends with: " + extension.value());

                assertTrue(file.length() > 0, "file should be not empty");
            });

        });
    }

    @Test
    @DisplayName("download video should work async callback")
    void downloadVideo_AsyncCallbackSpecifiedName_Success() {
        YoutubeDownloader downloader = new YoutubeDownloader();

        String fileName = "myAwesomeName";
        assertDoesNotThrow(() -> {
            YoutubeVideo video = downloader.getVideo(ME_AT_THE_ZOO_ID);

            int itag = 18;
            Format format = video.findFormatByItag(itag);
            assertNotNull(format, "findFormatByItag should return not null format");

            assertTrue(isReachable(format.url()), "url should be reachable");

            File outDir = new File("videos");

            char[] name = new char[200]; // Youtube title max length - 100 characters + file extension + duplication number
            video.downloadAsync(format, outDir, fileName, new OnYoutubeDownloadListener() {
                @Override
                public void onDownloading(int progress) {
                    System.out.printf("Downloading %d%%\n", progress);
                }

                @Override
                public void onFinished(File file) {
                    System.arraycopy(file.getName().toCharArray(), 0, name, 0, file.getName().length());
                    System.out.printf("Finished %s\n", file.getPath());

                }

                @Override
                public void onError(Throwable throwable) {
                    System.out.printf("Error %s\n", throwable.getMessage());
                }

            });

            int timeout = 5; // seconds
            while (--timeout > 0) {
                Thread.sleep(1000);
                if (!new String(name).trim().isEmpty())
                    break;
            }

            File file = new File(outDir, new String(name).trim());

            assertTrue(outDir.exists(), "output directory should be created");

            assertTrue(file.exists(), "file should be downloaded");
            assertTrue(file.getName().contains(fileName), "file name should contains: " + fileName);

            Extension extension = format.extension();
            assertTrue(file.getName().endsWith(extension.value()), "file name should ends with: " + extension.value());

            assertTrue(file.length() > 0, "file should be not empty");
        });
    }

    @Test
    @DisplayName("download video should work async callback")
    void downloadVideo_AsyncCallbackSpecifiedNameOverwrite_Success() {
        YoutubeDownloader downloader = new YoutubeDownloader();

        String fileName = "myAwesomeName";
        assertDoesNotThrow(() -> {
            YoutubeVideo video = downloader.getVideo(ME_AT_THE_ZOO_ID);

            int itag = 18;
            Format format = video.findFormatByItag(itag);
            assertNotNull(format, "findFormatByItag should return not null format");

            assertTrue(isReachable(format.url()), "url should be reachable");

            File outDir = new File("videos");

            char[] name = new char[200]; // Youtube title max length - 100 characters + file extension + duplication number
            video.downloadAsync(format, outDir, fileName, true, new OnYoutubeDownloadListener() {
                @Override
                public void onDownloading(int progress) {
                    System.out.printf("Downloading %d%%\n", progress);
                }

                @Override
                public void onFinished(File file) {
                    System.arraycopy(file.getName().toCharArray(), 0, name, 0, file.getName().length());
                    System.out.printf("Finished %s\n", file.getPath());

                }

                @Override
                public void onError(Throwable throwable) {
                    System.out.printf("Error %s\n", throwable.getMessage());
                }

            });

            int timeout = 5; // seconds
            while (--timeout > 0) {
                Thread.sleep(1000);
                if (!new String(name).trim().isEmpty())
                    break;
            }

            File file = new File(outDir, new String(name).trim());

            assertTrue(outDir.exists(), "output directory should be created");

            assertTrue(file.exists(), "file should be downloaded");
            String actualFileName = fileName + "." + format.extension().value();
            assertEquals(file.getName(), actualFileName, "file name should be: " + actualFileName);

            Extension extension = format.extension();
            assertTrue(file.getName().endsWith(extension.value()), "file name should ends with: " + extension.value());

            assertTrue(file.length() > 0, "file should be not empty");
        });
    }

    @Test
    @DisplayName("download subtitle should be successful")
    void downloadSubtitle_Sync_Success() {
        YoutubeDownloader downloader = new YoutubeDownloader();
        assertDoesNotThrow(() -> {
            List<SubtitlesInfo> subtitlesInfo = downloader.getVideoSubtitles(N3WPORT_ID);

            for (SubtitlesInfo subtitleInfo : subtitlesInfo) {
                String subtitles = subtitleInfo.getSubtitles().download();
                assertFalse(subtitles.isEmpty(), "subtitles should not be empty");
            }
        });
    }

    @Test
    @DisplayName("download subtitles with wrong lang code should throw exception")
    void downloadSubtitle_SyncWrongLangCode_Error() {
        YoutubeDownloader downloader = new YoutubeDownloader();

        assertDoesNotThrow(() -> {
            List<SubtitlesInfo> subtitlesInfo = downloader.getVideoSubtitles(N3WPORT_ID);

            for (SubtitlesInfo info : subtitlesInfo) {
                SubtitlesInfo failedInfo = new SubtitlesInfo(info.getUrl().replace("lang=" + info.getLanguage(), "lang=not_a_code"), "not_a_code", false);
                assertThrows(YoutubeException.class, () -> {
                    failedInfo.getSubtitles().download();
                });
            }
        });
    }


    @Test
    @DisplayName("download subtitle async should be successful")
    void downloadSubtitle_Async_Success() {
        YoutubeDownloader downloader = new YoutubeDownloader();
        assertDoesNotThrow(() -> {
            List<SubtitlesInfo> subtitlesInfo = downloader.getVideoSubtitles(N3WPORT_ID);

            for (SubtitlesInfo info : subtitlesInfo) {
                Future<String> subtitleFuture = info.getSubtitles().downloadAsync();

                assertTimeout(Duration.ofSeconds(5), () -> {
                    String subtitles = subtitleFuture.get();
                    assertFalse(subtitles.isEmpty(), "subtitles should not be empty");
                });
            }
        });
    }

    @Test
    @DisplayName("download subtitles async with callback should call onFinished")
    void downloadSubtitle_AsyncWithCallback_Success() {
        OnSubtitlesDownloadListener callback = Mockito.mock(OnSubtitlesDownloadListener.class);

        YoutubeDownloader downloader = new YoutubeDownloader();

        assertDoesNotThrow(() -> {
            List<SubtitlesInfo> subtitlesInfo = downloader.getVideoSubtitles(N3WPORT_ID);

            for (SubtitlesInfo info : subtitlesInfo) {
                Future<String> subtitleFuture = info.getSubtitles().downloadAsync(callback);

                assertTimeout(Duration.ofSeconds(5), () -> {
                    String subtitles = subtitleFuture.get();
                    assertFalse(subtitles.isEmpty(), "subtitles should not be empty");
                });
            }

            verify(callback, times(subtitlesInfo.size())).onFinished(any());
            verify(callback, never()).onError(any());
        });
    }

    @Test
    @DisplayName("download subtitles async with callback and wrong lang code should call onError")
    void downloadSubtitle_AsyncWithCallbackWrongLangCode_Error() throws InterruptedException {
        OnSubtitlesDownloadListener callback = Mockito.mock(OnSubtitlesDownloadListener.class);

        YoutubeDownloader downloader = new YoutubeDownloader();

        assertDoesNotThrow(() -> {
            List<SubtitlesInfo> subtitlesInfo = downloader.getVideoSubtitles(N3WPORT_ID);

            for (SubtitlesInfo info : subtitlesInfo) {
                info = new SubtitlesInfo(info.getUrl().replace("lang=" + info.getLanguage(), "lang=not_a_code"), "not_a_code", false);
                Future<String> subtitleFuture = info.getSubtitles().downloadAsync(callback);

                assertTimeout(Duration.ofSeconds(5), () -> {
                    String subtitles = subtitleFuture.get();
                    assertNull(subtitles, "subtitles should be null");
                });

            }
            verify(callback, times(subtitlesInfo.size())).onError(any());
            verify(callback, never()).onFinished(any());
        });
    }

    @Test
    @DisplayName("download formatted subtitles should be successful")
    void downloadSubtitles_Formatted_Success() {
        YoutubeDownloader downloader = new YoutubeDownloader();
        assertDoesNotThrow(() -> {
            List<SubtitlesInfo> subtitlesInfo = downloader.getVideoSubtitles(N3WPORT_ID);

            for (SubtitlesInfo info : subtitlesInfo) {
                String subtitles = info.getSubtitles()
                        .formatTo(Extension.JSON3)
                        .download();
                assertFalse(subtitles.isEmpty(), "subtitles should not be empty");
                assertDoesNotThrow(() -> {
                    JSONObject.parseObject(subtitles);
                }, "subtitles should be formatted to json");
            }
        });
    }

    @Test
    @DisplayName("download formatted and translated subtitles should be successful")
    void downloadSubtitles_FormattedTranslated_Success() {
        YoutubeDownloader downloader = new YoutubeDownloader();
        assertDoesNotThrow(() -> {
            List<SubtitlesInfo> subtitlesInfo = downloader.getVideoSubtitles(N3WPORT_ID);

            for (SubtitlesInfo subtitleInfo : subtitlesInfo) {
                String subtitle = subtitleInfo.getSubtitles()
                        .formatTo(Extension.JSON3)
                        .translateTo("uk")
                        .download();
                assertFalse(subtitle.isEmpty(), "subtitles should not be empty");
                assertDoesNotThrow(() -> {
                    JSONObject.parseObject(subtitle);
                }, "subtitles should be formatted to json");
            }
        });
    }

    @Test
    @DisplayName("download formatted and translated subtitles should be successful")
    void downloadSubtitles_FormattedTranslatedFromCaptions_Success() {
        YoutubeDownloader downloader = new YoutubeDownloader();
        assertDoesNotThrow(() -> {
            YoutubeVideo video = downloader.getVideo(N3WPORT_ID);
            List<SubtitlesInfo> subtitlesInfo = video.subtitles();

            for (SubtitlesInfo subtitleInfo : subtitlesInfo) {
                String subtitle = subtitleInfo.getSubtitles()
                        .formatTo(Extension.JSON3)
                        .translateTo("uk")
                        .download();
                assertFalse(subtitle.isEmpty(), "subtitles should not be empty");
                assertDoesNotThrow(() -> {
                    JSONObject.parseObject(subtitle);
                }, "subtitles should be formatted to json");
            }
        });
    }

    @Test
    @DisplayName("setRetryOnFailure should throw exception for invalid values")
    void setRetryOnFailure_InvalidRetryCount_ThrowsException() {
        YoutubeDownloader downloader = new YoutubeDownloader();
        assertThrows(IllegalArgumentException.class, () -> {
            downloader.setParserRetryOnFailure(-1);
        });
    }


}
