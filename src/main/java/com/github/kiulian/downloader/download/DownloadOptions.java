package com.github.kiulian.downloader.download;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * How to download a format. Immutable; every setter returns a copy.
 */
public final class DownloadOptions {

    private static final DownloadOptions DEFAULTS = new DownloadOptions(10L * 1024 * 1024, false, progress -> {
    });

    private final long chunkSize;
    private final boolean overwrite;
    private final Consumer<DownloadProgress> onProgress;

    private DownloadOptions(long chunkSize, boolean overwrite, Consumer<DownloadProgress> onProgress) {
        this.chunkSize = chunkSize;
        this.overwrite = overwrite;
        this.onProgress = onProgress;
    }

    /** 10 MB chunks, no overwrite, no progress listener. */
    public static DownloadOptions defaults() {
        return DEFAULTS;
    }

    /** Bytes per range request. Big chunks are faster; YouTube may slow down single very long requests. */
    public DownloadOptions chunkSize(long bytes) {
        if (bytes <= 0) {
            throw new IllegalArgumentException("chunk size must be positive");
        }
        return new DownloadOptions(bytes, overwrite, onProgress);
    }

    /** Replace an existing file. Default: fail if the file exists. */
    public DownloadOptions overwrite(boolean overwrite) {
        return new DownloadOptions(chunkSize, overwrite, onProgress);
    }

    /** Called on the downloading thread after each block of bytes. Keep it fast. */
    public DownloadOptions onProgress(Consumer<DownloadProgress> listener) {
        return new DownloadOptions(chunkSize, overwrite, Objects.requireNonNull(listener, "listener"));
    }

    public long chunkSize() {
        return chunkSize;
    }

    public boolean overwrite() {
        return overwrite;
    }

    public Consumer<DownloadProgress> onProgress() {
        return onProgress;
    }
}
