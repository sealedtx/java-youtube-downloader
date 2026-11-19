package com.github.kiulian.downloader.model.videos;

import com.github.kiulian.downloader.model.subtitles.SubtitleTrack;
import com.github.kiulian.downloader.model.videos.formats.Formats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Everything the library found about a video.
 * <p>
 * The result can be partial: if formats could not be extracted, {@link #formats()} is empty and
 * {@link #warnings()} says why. Check the warnings before you report a bug.
 */
public final class VideoInfo {

    private final VideoDetails details;
    private final Formats formats;
    private final List<SubtitleTrack> subtitles;
    private final String hlsManifestUrl;
    private final String dashManifestUrl;
    private final String client;
    private final List<String> warnings;

    public VideoInfo(VideoDetails details, Formats formats, List<SubtitleTrack> subtitles,
                     String hlsManifestUrl, String dashManifestUrl, String client, List<String> warnings) {
        this.details = Objects.requireNonNull(details, "details");
        this.formats = Objects.requireNonNull(formats, "formats");
        this.subtitles = Collections.unmodifiableList(new ArrayList<>(subtitles));
        this.hlsManifestUrl = hlsManifestUrl;
        this.dashManifestUrl = dashManifestUrl;
        this.client = client;
        this.warnings = Collections.unmodifiableList(new ArrayList<>(warnings));
    }

    public VideoDetails details() {
        return details;
    }

    public Formats formats() {
        return formats;
    }

    public List<SubtitleTrack> subtitles() {
        return subtitles;
    }

    /** HLS manifest (live streams use it), or {@code null}. */
    public String hlsManifestUrl() {
        return hlsManifestUrl;
    }

    /** DASH manifest, or {@code null}. */
    public String dashManifestUrl() {
        return dashManifestUrl;
    }

    /** Name of the client profile whose answer was used. */
    public String client() {
        return client;
    }

    /** Things that went wrong without failing the request: skipped formats, clients that failed first. */
    public List<String> warnings() {
        return warnings;
    }

    @Override
    public String toString() {
        return "VideoInfo{" + details + ", " + formats.size() + " formats, client=" + client + "}";
    }
}
