package com.github.kiulian.downloader.downloader;

public interface YoutubeProgressCallback<T> extends YoutubeCallback<T> {

    void onDownloading(int progress);

}
