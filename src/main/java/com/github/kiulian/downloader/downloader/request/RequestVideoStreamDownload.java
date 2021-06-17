package com.github.kiulian.downloader.downloader.request;

import com.github.kiulian.downloader.model.videos.formats.Format;

import java.io.OutputStream;

public class RequestVideoStreamDownload extends Request<RequestVideoStreamDownload, Void> {

    private final Format format;
    private final OutputStream outputStream;

    public RequestVideoStreamDownload(Format format, OutputStream outputStream) {
        this.format = format;
        this.outputStream = outputStream;
    }

    public Format getFormat() {
        return format;
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }
}
