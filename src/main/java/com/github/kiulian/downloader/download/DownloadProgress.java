package com.github.kiulian.downloader.download;

/**
 * Progress of one download.
 */
public final class DownloadProgress {

    private final long bytesDone;
    private final long totalBytes;
    private final long elapsedMillis;

    public DownloadProgress(long bytesDone, long totalBytes, long elapsedMillis) {
        this.bytesDone = bytesDone;
        this.totalBytes = totalBytes;
        this.elapsedMillis = elapsedMillis;
    }

    public long bytesDone() {
        return bytesDone;
    }

    /** Expected size, 0 if unknown. */
    public long totalBytes() {
        return totalBytes;
    }

    /** 0 to 100, or -1 if the size is unknown. */
    public int percent() {
        return totalBytes > 0 ? (int) (bytesDone * 100 / totalBytes) : -1;
    }

    /** Average speed since the start. */
    public long bytesPerSecond() {
        return elapsedMillis > 0 ? bytesDone * 1000 / elapsedMillis : 0;
    }

    @Override
    public String toString() {
        return bytesDone + "/" + totalBytes + " bytes";
    }
}
