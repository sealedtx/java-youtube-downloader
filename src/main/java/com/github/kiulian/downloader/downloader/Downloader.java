package com.github.kiulian.downloader.downloader;

import com.github.kiulian.downloader.downloader.request.RequestVideoFileDownload;
import com.github.kiulian.downloader.downloader.request.RequestVideoStreamDownload;
import com.github.kiulian.downloader.downloader.request.RequestWebpage;
import com.github.kiulian.downloader.downloader.response.Response;

import java.io.File;

public interface Downloader {

    Response<String> downloadWebpage(RequestWebpage request);

    Response<File> downloadVideoAsFile(RequestVideoFileDownload request);

    Response<Void> downloadVideoAsStream(RequestVideoStreamDownload request);

}
