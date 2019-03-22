package com.github.kiulian.downloader;

import java.io.File;

public interface YoutubeDownloadCallback {

    void onDownloading(int progress);

    void onFinished(File file);

    void onError(Throwable throwable);
}
