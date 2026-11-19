package com.github.kiulian.downloader.model.videos.formats;

import com.github.kiulian.downloader.model.videos.quality.AudioQuality;
import com.github.kiulian.downloader.model.videos.quality.VideoQuality;

import java.util.Objects;

/**
 * One downloadable stream of a video: video only, audio only, or both (muxed).
 * <p>
 * Numbers that YouTube did not send are 0. URLs expire after some hours; fetch the video info again then.
 */
public final class Format {

    private final int itag;
    private final String url;
    private final String mimeType;
    private final String codecs;
    private final Extension extension;
    private final boolean adaptive;
    private final int bitrate;
    private final int averageBitrate;
    private final long contentLength;
    private final long durationMillis;
    private final int width;
    private final int height;
    private final int fps;
    private final String qualityLabel;
    private final VideoQuality videoQuality;
    private final AudioQuality audioQuality;
    private final int audioSampleRate;
    private final int audioChannels;
    private final String audioTrackId;
    private final String audioTrackName;
    private final boolean defaultAudioTrack;
    private final boolean drc;
    private final String client;

    private Format(Builder b) {
        this.itag = b.itag;
        this.url = Objects.requireNonNull(b.url, "url");
        this.mimeType = b.mimeType == null ? "" : b.mimeType;
        this.codecs = codecsOf(this.mimeType);
        this.extension = Extension.fromMimeType(this.mimeType);
        this.adaptive = b.adaptive;
        this.bitrate = b.bitrate;
        this.averageBitrate = b.averageBitrate;
        this.contentLength = b.contentLength;
        this.durationMillis = b.durationMillis;
        this.width = b.width;
        this.height = b.height;
        this.fps = b.fps;
        this.qualityLabel = b.qualityLabel;
        this.videoQuality = b.videoQuality != null ? b.videoQuality : VideoQuality.unknown;
        this.audioQuality = b.audioQuality != null ? b.audioQuality : AudioQuality.unknown;
        this.audioSampleRate = b.audioSampleRate;
        this.audioChannels = b.audioChannels;
        this.audioTrackId = b.audioTrackId;
        this.audioTrackName = b.audioTrackName;
        this.defaultAudioTrack = b.defaultAudioTrack;
        this.drc = b.drc;
        this.client = b.client;
    }

    public static Builder builder(int itag, String url) {
        return new Builder(itag, url);
    }

    /** YouTube format id. The same itag can appear more than once, for example for each audio language. */
    public int itag() {
        return itag;
    }

    /** What the library knows about this itag, or {@code null} for an itag it does not know. */
    public Itag knownItag() {
        return Itag.of(itag);
    }

    public String url() {
        return url;
    }

    /** For example {@code video/mp4; codecs="avc1.64001F"}. */
    public String mimeType() {
        return mimeType;
    }

    /** Codecs from the MIME type, for example {@code avc1.64001F} or {@code avc1.42001E, mp4a.40.2}. */
    public String codecs() {
        return codecs;
    }

    public Extension extension() {
        return extension;
    }

    /** True for DASH streams that hold only video or only audio. */
    public boolean isAdaptive() {
        return adaptive;
    }

    public boolean hasVideo() {
        return mimeType.startsWith("video/");
    }

    public boolean hasAudio() {
        return mimeType.startsWith("audio/") || audioQuality != AudioQuality.unknown || audioChannels > 0;
    }

    /** Bits per second at the peak. */
    public int bitrate() {
        return bitrate;
    }

    /** Average bits per second. */
    public int averageBitrate() {
        return averageBitrate;
    }

    /** Size in bytes, 0 if unknown. */
    public long contentLength() {
        return contentLength;
    }

    public long durationMillis() {
        return durationMillis;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int fps() {
        return fps;
    }

    /** For example {@code 720p60}; {@code null} for audio. */
    public String qualityLabel() {
        return qualityLabel;
    }

    public VideoQuality videoQuality() {
        return videoQuality;
    }

    public AudioQuality audioQuality() {
        return audioQuality;
    }

    public int audioSampleRate() {
        return audioSampleRate;
    }

    public int audioChannels() {
        return audioChannels;
    }

    /** Id of the audio language track, or {@code null} if the video has one track. */
    public String audioTrackId() {
        return audioTrackId;
    }

    /** For example {@code English (United States) original}, or {@code null}. */
    public String audioTrackName() {
        return audioTrackName;
    }

    /** True if this is the main audio track, also when the video has only one. */
    public boolean isDefaultAudioTrack() {
        return defaultAudioTrack;
    }

    /** True for audio with dynamic range compression (stable volume). */
    public boolean isDrc() {
        return drc;
    }

    /** Name of the client profile that returned this format. */
    public String client() {
        return client;
    }

    @Override
    public String toString() {
        String quality = hasVideo() ? (qualityLabel != null ? qualityLabel : height + "p") : audioQuality.name();
        return "Format{itag=" + itag + ", " + mimeType + ", " + quality + "}";
    }

    private static String codecsOf(String mimeType) {
        int start = mimeType.indexOf("codecs=\"");
        if (start < 0) {
            return "";
        }
        start += "codecs=\"".length();
        int end = mimeType.indexOf('"', start);
        return end < 0 ? mimeType.substring(start) : mimeType.substring(start, end);
    }

    public static final class Builder {
        private final int itag;
        private final String url;
        private String mimeType;
        private boolean adaptive;
        private int bitrate;
        private int averageBitrate;
        private long contentLength;
        private long durationMillis;
        private int width;
        private int height;
        private int fps;
        private String qualityLabel;
        private VideoQuality videoQuality;
        private AudioQuality audioQuality;
        private int audioSampleRate;
        private int audioChannels;
        private String audioTrackId;
        private String audioTrackName;
        private boolean defaultAudioTrack = true;
        private boolean drc;
        private String client;

        private Builder(int itag, String url) {
            this.itag = itag;
            this.url = url;
        }

        public Builder mimeType(String mimeType) { this.mimeType = mimeType; return this; }
        public Builder adaptive(boolean adaptive) { this.adaptive = adaptive; return this; }
        public Builder bitrate(int bitrate) { this.bitrate = bitrate; return this; }
        public Builder averageBitrate(int averageBitrate) { this.averageBitrate = averageBitrate; return this; }
        public Builder contentLength(long contentLength) { this.contentLength = contentLength; return this; }
        public Builder durationMillis(long durationMillis) { this.durationMillis = durationMillis; return this; }
        public Builder width(int width) { this.width = width; return this; }
        public Builder height(int height) { this.height = height; return this; }
        public Builder fps(int fps) { this.fps = fps; return this; }
        public Builder qualityLabel(String qualityLabel) { this.qualityLabel = qualityLabel; return this; }
        public Builder videoQuality(VideoQuality videoQuality) { this.videoQuality = videoQuality; return this; }
        public Builder audioQuality(AudioQuality audioQuality) { this.audioQuality = audioQuality; return this; }
        public Builder audioSampleRate(int audioSampleRate) { this.audioSampleRate = audioSampleRate; return this; }
        public Builder audioChannels(int audioChannels) { this.audioChannels = audioChannels; return this; }
        public Builder audioTrack(String id, String name, boolean isDefault) {
            this.audioTrackId = id;
            this.audioTrackName = name;
            this.defaultAudioTrack = isDefault;
            return this;
        }
        public Builder drc(boolean drc) { this.drc = drc; return this; }
        public Builder client(String client) { this.client = client; return this; }

        public Format build() {
            return new Format(this);
        }
    }
}
