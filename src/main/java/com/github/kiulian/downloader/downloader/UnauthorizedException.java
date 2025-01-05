package com.github.kiulian.downloader.downloader;

import java.io.IOException;

public class UnauthorizedException extends IOException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
