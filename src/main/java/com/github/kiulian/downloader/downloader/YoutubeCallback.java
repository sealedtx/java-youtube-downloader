package com.github.kiulian.downloader.downloader;

public interface YoutubeCallback<T> {

    void onFinished(T data);

    void onError(Throwable throwable);
}
