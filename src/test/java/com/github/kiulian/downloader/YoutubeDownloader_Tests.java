package com.github.kiulian.downloader;

import com.github.kiulian.downloader.model.Extension;
import com.github.kiulian.downloader.model.YoutubeVideo;
import com.github.kiulian.downloader.model.formats.Format;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.github.kiulian.downloader.TestUtils.ME_AT_THE_ZOO_ID;
import static com.github.kiulian.downloader.TestUtils.isReachable;
import static org.junit.jupiter.api.Assertions.*;

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
    @DisplayName("setRetryOnFailure should throw exception for invalid values")
    void setRetryOnFailure_InvalidRetryCount_ThrowsException() {
        YoutubeDownloader downloader = new YoutubeDownloader();
        assertThrows(IllegalArgumentException.class, () -> {
            downloader.setParserRetryOnFailure(-1);
        });
    }


}
