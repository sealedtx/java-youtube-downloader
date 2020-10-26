package com.github.kiulian.downloader.model.subtitles;


public interface OnSubtitlesDownloadListener {
    void onFinished(String subtitles);

    void onError(Throwable throwable);
}
