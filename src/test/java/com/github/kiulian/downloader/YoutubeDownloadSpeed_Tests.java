package com.github.kiulian.downloader;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.jupiter.api.*;

import com.github.kiulian.downloader.model.YoutubeVideo;
import com.github.kiulian.downloader.model.formats.Format;

public class YoutubeDownloadSpeed_Tests extends TestUtils {
    private static final File outDir = new File("videos");

    // Zooming out from earth
    private static final String ZOOM_OUT_ID = "DgqAAE9Aagc";

    // Make download pauses obvious
    private static final OnYoutubeDownloadListener listener = new OnYoutubeDownloadListener() {

        @Override
        public void onFinished(File file) {}
        
        @Override
        public void onError(Throwable throwable) {}
        
        @Override
        public void onDownloading(int progress) {
            System.out.print(".");
        }
    };

    protected YoutubeDownloader downloader;
    private static Method straightMethod;
    private static Method byPartMethod;

    @BeforeAll
    static void initReflectMethods() throws NoSuchMethodException, SecurityException {
        straightMethod = YoutubeVideo.class.getDeclaredMethod("downloadStraight", Format.class, OutputStream.class, OnYoutubeDownloadListener.class);
        straightMethod.setAccessible(true);
        byPartMethod = YoutubeVideo.class.getDeclaredMethod("downloadByPart", Format.class, OutputStream.class, OnYoutubeDownloadListener.class);
        byPartMethod.setAccessible(true);
    }

    @BeforeEach
    void initDownloader() {
        this.downloader = new YoutubeDownloader();
        if (!outDir.isDirectory()) {
            outDir.mkdirs();
        }
    }

    @AfterEach
    void cleanOutDir() {
        if (outDir.isDirectory()) {
            clean(outDir);
        }
    }

    @Test
    @DisplayName("download speed test should be successful")
    void downloadSpeed_Success() {
        
        assertDoesNotThrow(() -> {
            YoutubeVideo video = downloader.getVideo(ZOOM_OUT_ID);
            testSpeed(video, 18);
            testSpeed(video, 135);
        });
    }

    private static void testSpeed(YoutubeVideo video, int itag) {
        final Format format = video.findFormatByItag(itag);
        assertNotNull(format, "findFormatByItag should return not null format");
        
        assertDoesNotThrow(() -> {
            
            long straightTime = System.currentTimeMillis();
            File straightFile = download(video, format, false);
            straightTime = System.currentTimeMillis() - straightTime;
            System.out.println(" " + straightTime + "ms");
            
            long byPartTime = System.currentTimeMillis();
            File byPartFile = download(video, format, true);
            byPartTime = System.currentTimeMillis() - byPartTime;
            System.out.println(" " + byPartTime + "ms");
            
            assertSameContent(straightFile, byPartFile);
            
            if (format.isAdaptive()) {
                double ratio = (double) straightTime / (double) byPartTime;
                assertTrue(byPartTime < straightTime, "Download by part should be faster for format: " + format.itag());
                System.out.println("Download by part is " + String.format("%.2f", ratio) + " faster for format " + format.itag());
            } else {
                double ratio = (double) byPartTime / (double) straightTime;
                assertTrue(byPartTime > straightTime, "Straight download should be faster for format: " + format.itag());
                System.out.println("Straight download is " + String.format("%.2f", ratio) + " faster for format " + format.itag());
            }
        });
    }

    private static File download(YoutubeVideo video, Format format, boolean isByPart) throws IOException, YoutubeException {
        System.out.print(isByPart ? "By part " : "Straight");
        File outputFile = new File(outDir, video.details().title() + "_" + (isByPart ? "bypart" : "straight") + format.itag().id() + "." + format.extension().value());
        try (OutputStream os = new FileOutputStream(outputFile)) {
            Method method = isByPart ? byPartMethod : straightMethod;
            method.invoke(video, format, os, listener);
            listener.onFinished(outputFile);
            return outputFile;
        } catch (SecurityException | IllegalAccessException | IllegalArgumentException e) {
            fail(e);
            return null;
        } catch (InvocationTargetException e) {
            listener.onError(e.getTargetException());
            if (e.getTargetException() instanceof YoutubeException) {
                throw (YoutubeException) e.getTargetException();
            } else if (e.getTargetException() instanceof IOException) {
                throw (IOException) e.getTargetException();
            }
            fail(e);
            return null;
        }
    }

    private static void assertSameContent(File left, File right) throws IOException {
        assertEquals(left.length(), right.length());
        
        try (
                InputStream leftIs = new FileInputStream(left);
                InputStream rightIs = new FileInputStream(right);
        ) {
            int leftRead, rightRead;
            byte[] leftBuf = new byte[4096];
            byte[] rightBuf = new byte[4096];
            
            while ((leftRead = leftIs.read(leftBuf)) != -1) {
                rightRead = rightIs.read(rightBuf);
                // Local disk access, buffers should always be equal
                assertEquals(leftRead, rightRead);
                assertArrayEquals(leftBuf, rightBuf);
            }
        }
    }
}
