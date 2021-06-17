package com.github.kiulian.downloader.downloader.request;

import com.github.kiulian.downloader.model.Extension;
import com.github.kiulian.downloader.model.subtitles.SubtitlesInfo;

public class RequestSubtitlesDownload extends RequestWebpage {

    private Extension format;
    private String translationLanguage;
    private boolean fromCaptions;

    public RequestSubtitlesDownload(SubtitlesInfo subtitlesInfo) {
        super(subtitlesInfo.getUrl());
        this.fromCaptions = subtitlesInfo.isFromCaptions();
    }

    public RequestSubtitlesDownload formatTo(Extension extension) {
        this.format = extension;
        return this;
    }

    public RequestSubtitlesDownload translateTo(String language) {
        if (fromCaptions) {
            this.translationLanguage = language;
        }
        return this;
    }

    @Override
    public String getDownloadUrl() {
        String downloadUrl = url;
        if (format != null && format.isSubtitle()) {
            downloadUrl += "&fmt=" + format.value();
        }
        if (translationLanguage != null && !translationLanguage.isEmpty()) {
            downloadUrl += "&tlang=" + translationLanguage;
        }
        return downloadUrl;
    }

}
