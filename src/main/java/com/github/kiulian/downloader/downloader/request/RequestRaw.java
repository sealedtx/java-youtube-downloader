package com.github.kiulian.downloader.downloader.request;

public abstract class RequestRaw<T extends RequestRaw<T>> extends Request<T, String> {

    public abstract String getDownloadUrl();

}
