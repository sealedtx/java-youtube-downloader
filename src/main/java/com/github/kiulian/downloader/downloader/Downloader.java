package com.github.kiulian.downloader.downloader;

import com.github.kiulian.downloader.downloader.request.RequestVideoDownload;
import com.github.kiulian.downloader.downloader.request.RequestWebpage;
import com.github.kiulian.downloader.downloader.response.Response;

import java.io.File;

public interface Downloader {

    Response<String> downloadWebpage(RequestWebpage request);

    Response<File> downloadVideoAsFile(RequestVideoDownload request);

    //TODO: implement download Video in-memory by accepting OutputStream

}
