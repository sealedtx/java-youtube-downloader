package com.github.kiulian.downloader.extractor;


import com.github.kiulian.downloader.YoutubeException;

import java.io.IOException;


public interface Extractor {

    String extractYtPlayerConfig(String html) throws YoutubeException;

    String loadUrl(String url) throws IOException;

}
