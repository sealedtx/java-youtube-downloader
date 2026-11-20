package com.github.kiulian.downloader.internal.youtube;

import com.github.kiulian.downloader.YoutubeException;
import com.github.kiulian.downloader.YoutubeException.HttpException;
import com.github.kiulian.downloader.http.HttpTransport.Request;
import com.github.kiulian.downloader.internal.Redact;
import com.github.kiulian.downloader.internal.http.Http;
import com.github.kiulian.downloader.internal.http.HttpStatusException;
import com.github.kiulian.downloader.model.subtitles.SubtitleFormat;
import com.github.kiulian.downloader.model.subtitles.SubtitleTrack;

import java.io.IOException;

/**
 * Downloads subtitle tracks as text.
 */
public final class SubtitleDownloader {

    private final Http http;
    private final Innertube innertube;

    public SubtitleDownloader(Http http, Innertube innertube) {
        this.http = http;
        this.innertube = innertube;
    }

    public String text(SubtitleTrack track, SubtitleFormat format, String translateTo) {
        String url = track.url(format, translateTo);
        if (PlayerResponse.trustedUrl(url) == null) {
            throw new IllegalArgumentException("Subtitle URL is not on a YouTube host: " + Redact.url(url));
        }
        try {
            Request.Builder request = Request.get(url).header("User-Agent", Innertube.WEB_USER_AGENT);
            // account cookies go to youtube.com only, never to video servers
            boolean youtubeHost = java.net.URI.create(url).getHost().toLowerCase(java.util.Locale.ROOT).endsWith("youtube.com");
            return http.text((youtubeHost ? innertube.withCookie(request) : request).build());
        } catch (HttpStatusException e) {
            String message = "Could not download subtitles " + track.languageCode() + ": " + e.getMessage();
            if (translateTo != null && e.status() == 429) {
                // seen since 2026-11 for the JS-less clients: plain tracks work, translations get 429
                message += " (YouTube currently refuses machine translations for this client; download the track"
                        + " without translation or translate it yourself)";
            }
            throw new HttpException(e.status(), message, e);
        } catch (IOException e) {
            throw new YoutubeException("Could not download subtitles " + track.languageCode() + ": " + e.getMessage(), e);
        }
    }
}
