package com.github.kiulian.downloader.model.videos.formats;

import com.alibaba.fastjson.JSONObject;
import com.github.kiulian.downloader.model.Extension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Format {

    private static final Logger LOGGER = LoggerFactory.getLogger(Format.class);

    public static final String AUDIO = "audio";
    public static final String VIDEO = "video";
    public static final String AUDIO_VIDEO = "audio/video";

    private final boolean isAdaptive;

    protected final Itag itag;
    protected final String url;
    protected final String mimeType;
    protected final Extension extension;
    protected final Integer bitrate;
    protected final Long contentLength;
    protected final Long lastModified;
    protected final Long approxDurationMs;
    protected final String clientVersion;

    protected Format(JSONObject json, boolean isAdaptive, String clientVersion) {
        this.isAdaptive = isAdaptive;
        this.clientVersion = clientVersion;

        Itag itag;
        try {
            itag = Itag.valueOf("i" + json.getInteger("itag"));
        } catch (IllegalArgumentException e) {
            LOGGER.error("Format init failed", e);
            itag = Itag.unknown;
            itag.setId(json.getIntValue("itag"));
        }
        this.itag = itag;

        url = json.getString("url").replace("\\u0026", "&");
        mimeType = json.getString("mimeType");
        bitrate = json.getInteger("bitrate");
        contentLength = json.getLong("contentLength");
        lastModified = json.getLong("lastModified");
        approxDurationMs = json.getLong("approxDurationMs");

        if (mimeType == null || mimeType.isEmpty()) {
            extension = Extension.UNKNOWN;
        } else if (mimeType.contains(Extension.MPEG4.value())) {
            if (this instanceof AudioFormat)
                extension = Extension.M4A;
            else
                extension = Extension.MPEG4;
        } else if (mimeType.contains(Extension.WEBM.value())) {
            if (this instanceof AudioFormat)
                extension = Extension.WEBA;
            else
                extension = Extension.WEBM;
        } else if (mimeType.contains(Extension.FLV.value())) {
            extension = Extension.FLV;
        } else if (mimeType.contains(Extension._3GP.value())) {
            extension = Extension._3GP;
        } else {
            extension = Extension.UNKNOWN;
        }
    }

    public abstract String type();

    public boolean isAdaptive() {
        return isAdaptive;
    }

    public String clientVersion() {
        return clientVersion;
    }

    public Itag itag() {
        return itag;
    }

    public Integer bitrate() {
        return bitrate;
    }

    public String mimeType() {
        return mimeType;
    }

    public String url() {
        return url;
    }

    public Long contentLength() {
        return contentLength;
    }

    public long lastModified() {
        return lastModified;
    }

    public Long duration() {
        return approxDurationMs;
    }

    public Extension extension() {
        return extension;
    }
}
