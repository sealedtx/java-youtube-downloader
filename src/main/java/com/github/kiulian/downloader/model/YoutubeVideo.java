package com.github.kiulian.downloader.model;


import com.github.kiulian.downloader.OnYoutubeDownloadListener;
import com.github.kiulian.downloader.YoutubeException;
import com.github.kiulian.downloader.model.formats.AudioFormat;
import com.github.kiulian.downloader.model.formats.AudioVideoFormat;
import com.github.kiulian.downloader.model.formats.Format;
import com.github.kiulian.downloader.model.formats.VideoFormat;
import com.github.kiulian.downloader.model.quality.AudioQuality;
import com.github.kiulian.downloader.model.quality.VideoQuality;
import com.github.kiulian.downloader.model.subtitles.SubtitlesInfo;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import static com.github.kiulian.downloader.model.Utils.*;

public class YoutubeVideo {

    private static final int BUFFER_SIZE = 4096;
    private static final int PART_LENGTH = 2 * 1024 * 1024;

    private VideoDetails videoDetails;
    private List<Format> formats;
    private List<SubtitlesInfo> subtitlesInfo;
    private final String clientVersion;

    public YoutubeVideo(VideoDetails videoDetails, List<Format> formats, List<SubtitlesInfo> subtitlesInfo, String clientVersion) {
        this.videoDetails = videoDetails;
        this.formats = formats;
        this.subtitlesInfo = subtitlesInfo;
        this.clientVersion = clientVersion;
    }

    public VideoDetails details() {
        return videoDetails;
    }

    public List<Format> formats() {
        return formats;
    }

    public List<SubtitlesInfo> subtitles() {
        return subtitlesInfo;
    }

    public List<Format> findFormats(Filter<Format> filter) {
        return filter.select(formats);
    }

    public Format findFormatByItag(int itag) {
        for (int i = 0; i < formats.size(); i++) {
            Format format = formats.get(i);
            if (format.itag().id() == itag)
                return format;
        }
        return null;
    }

    public List<AudioVideoFormat> videoWithAudioFormats() {
        List<AudioVideoFormat> find = new LinkedList<>();

        for (int i = 0; i < formats.size(); i++) {
            Format format = formats.get(i);
            if (format instanceof AudioVideoFormat)
                find.add((AudioVideoFormat) format);
        }
        return find;
    }

    public List<VideoFormat> videoFormats() {
        List<VideoFormat> find = new LinkedList<>();

        for (int i = 0; i < formats.size(); i++) {
            Format format = formats.get(i);
            if (format instanceof VideoFormat)
                find.add((VideoFormat) format);
        }
        return find;
    }

    public List<VideoFormat> findVideoWithQuality(VideoQuality videoQuality) {
        List<VideoFormat> find = new LinkedList<>();

        for (int i = 0; i < formats.size(); i++) {
            Format format = formats.get(i);
            if (format instanceof VideoFormat && ((VideoFormat) format).videoQuality() == videoQuality)
                find.add((VideoFormat) format);
        }
        return find;
    }

    public List<VideoFormat> findVideoWithExtension(Extension extension) {
        List<VideoFormat> find = new LinkedList<>();

        for (int i = 0; i < formats.size(); i++) {
            Format format = formats.get(i);
            if (format instanceof VideoFormat && format.extension().equals(extension))
                find.add((VideoFormat) format);
        }
        return find;
    }

    public List<AudioFormat> audioFormats() {
        List<AudioFormat> find = new LinkedList<>();

        for (int i = 0; i < formats.size(); i++) {
            Format format = formats.get(i);
            if (format instanceof AudioFormat)
                find.add((AudioFormat) format);
        }
        return find;
    }

    public List<AudioFormat> findAudioWithQuality(AudioQuality audioQuality) {
        List<AudioFormat> find = new LinkedList<>();

        for (int i = 0; i < formats.size(); i++) {
            Format format = formats.get(i);
            if (format instanceof AudioFormat && ((AudioFormat) format).audioQuality() == audioQuality)
                find.add((AudioFormat) format);
        }
        return find;
    }

    public List<AudioFormat> findAudioWithExtension(Extension extension) {
        List<AudioFormat> find = new LinkedList<>();

        for (int i = 0; i < formats.size(); i++) {
            Format format = formats.get(i);
            if (format instanceof AudioFormat && format.extension() == extension)
                find.add((AudioFormat) format);
        }
        return find;
    }

    public InputStream download(Format format) throws IOException {
        return new URL(format.url()).openStream();
    }

    public File download(Format format, File outDir) throws IOException, YoutubeException {
        return download(format, outDir, videoDetails.title());
    }

    public File download(Format format, File outDir, String fileName) throws IOException, YoutubeException {
        return download(format, outDir, fileName, false);
    }

    public File download(Format format, File outDir, String fileName, boolean overwrite) throws IOException, YoutubeException {
        File outputFile = initDownload(format, outDir, fileName, overwrite);

        return basicDownload(format, outputFile, null);
    }

    public Future<File> downloadAsync(Format format, File outDir) throws YoutubeException.LiveVideoException, IOException {
        return downloadAsync(format, outDir, videoDetails.title());
    }

    public Future<File> downloadAsync(Format format, File outDir, String fileName) throws YoutubeException.LiveVideoException, IOException {
        return downloadAsync(format, outDir, fileName, false, null);
    }

    public Future<File> downloadAsync(Format format, File outDir, OnYoutubeDownloadListener listener) throws IOException, YoutubeException {
        return downloadAsync(format, outDir, videoDetails.title(), listener);
    }

    public Future<File> downloadAsync(Format format, File outDir, String fileName, OnYoutubeDownloadListener listener) throws IOException, YoutubeException {
        return downloadAsync(format, outDir, fileName, false, listener);
    }

    public Future<File> downloadAsync(final Format format, final File outDir, final String fileName, final boolean overwrite, final OnYoutubeDownloadListener listener) throws YoutubeException.LiveVideoException, IOException {
        File outputFile = initDownload(format, outDir, fileName, overwrite);

        FutureTask<File> future = new FutureTask<>(() -> basicDownload(format, outputFile, listener));

        Thread thread = new Thread(future, "YtDownloader");
        thread.setDaemon(true);
        thread.start();
        return future;
    }

    private File initDownload(Format format, File outDir, String fileName, boolean overwrite) throws IOException, YoutubeException.LiveVideoException {
        videoDetails.checkDownload();

        createOutDir(outDir);

        return getOutputFile(fileName, format, outDir, overwrite);
    }

    private File basicDownload(Format format, File outputFile, OnYoutubeDownloadListener listener) throws IOException {
        boolean exception = false;
        OutputStream os = null;
        try {
            os = new FileOutputStream(outputFile);
            if (format.isAdaptive() && format.contentLength() != null) {
                downloadByPart(format, os, listener);
            } else {
                downloadStraight(format, os, listener);
            }
            if (listener != null) {
                listener.onFinished(outputFile);
            }
        } catch (IOException | CancellationException e) {
            exception = true;
            if (listener != null) {
                listener.onError(e);
            }
            throw e;
        } finally {
            closeSilently(os);
            // try to delete file if exception occurred
            if (exception) {
                outputFile.delete();
            }
        }
        return outputFile;
    }

    // Downloads the format in one single request
    private void downloadStraight(Format format, OutputStream os, final OnYoutubeDownloadListener listener) throws IOException {
        URLConnection urlConnection = new URL(format.url()).openConnection();
        int contentLength = urlConnection.getContentLength();

        InputStream is = urlConnection.getInputStream();

        byte[] buffer = new byte[BUFFER_SIZE];
        if (listener == null) {
            copyAndCloseInput(is, os, buffer);
        } else {
            copyAndCloseInput(is, os, buffer, 0, contentLength, listener);
        }
    }

    // Downloads the format part by part, with as many requests as needed
    private void downloadByPart(Format format, OutputStream os, final OnYoutubeDownloadListener listener) throws IOException {
        long done = 0;
        int partNumber = 0;

        final String pathPrefix = "&cver=" + clientVersion + "&range=";
        final long contentLength = format.contentLength();
        byte[] buffer = new byte[BUFFER_SIZE];

        while (done < contentLength) {
            long toRead = PART_LENGTH;
            if (done + toRead > contentLength) {
                toRead = (int) (contentLength - done);
            }

            partNumber++;
            String partUrl = format.url() + pathPrefix
                    + done + "-" + (done + toRead - 1)    // range first-last byte positions
                    + "&rn=" + partNumber;                // part number
            URL url = new URL(partUrl);
            InputStream is = url.openStream();
            if (listener == null) {
                done += copyAndCloseInput(is, os, buffer);
            } else {
                done += copyAndCloseInput(is, os, buffer, done, contentLength, listener);
            }
        }
    }

    // Copies as many bytes as possible then closes input stream
    private static long copyAndCloseInput(InputStream is, OutputStream os, byte[] buffer, long offset, long totalLength, OnYoutubeDownloadListener listener) throws IOException {
        long done = 0;

        try {
            int read = 0;
            long lastProgress = offset == 0 ? 0 : (offset * 100) / totalLength;

            while ((read = is.read(buffer)) != -1) {
                if (Thread.interrupted()) {
                    throw new CancellationException("Downloading is canceled");
                }
                os.write(buffer, 0, read);
                done += read;
                long progress = ((offset + done) * 100) / totalLength;
                if (progress > lastProgress) {
                    listener.onDownloading((int) progress);
                    lastProgress = progress;
                }
            }
        } finally {
            closeSilently(is);
        }
        return done;
    }

    private static long copyAndCloseInput(InputStream is, OutputStream os, byte[] buffer) throws IOException {
        long done = 0;

        try {
            int count = 0;
            while ((count = is.read(buffer)) != -1) {
                if (Thread.interrupted()) {
                    throw new CancellationException("Downloading is canceled");
                }
                os.write(buffer, 0, count);
                done += count;
            }
        } finally {
            closeSilently(is);
        }
        return done;
    }

}
