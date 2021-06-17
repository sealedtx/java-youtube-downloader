package com.github.kiulian.downloader.model.videos.formats;




import com.alibaba.fastjson.JSONObject;
import com.github.kiulian.downloader.model.videos.quality.AudioQuality;

public class VideoWithAudioFormat extends VideoFormat {

    private final Integer averageBitrate;
    private final Integer audioSampleRate;
    private final AudioQuality audioQuality;

    public VideoWithAudioFormat(JSONObject json, boolean isAdaptive, String clientVersion) {
        super(json, isAdaptive, clientVersion);
        audioSampleRate = json.getInteger("audioSampleRate");
        averageBitrate = json.getInteger("averageBitrate");

        AudioQuality audioQuality = null;
        if (json.containsKey("audioQuality")) {
            String[] split = json.getString("audioQuality").split("_");
            String quality = split[split.length - 1].toLowerCase();
            try {
                audioQuality = AudioQuality.valueOf(quality);
            } catch (IllegalArgumentException ignore) {
            }
        }
        this.audioQuality = audioQuality;
    }

    @Override
    public String type() {
        return AUDIO_VIDEO;
    }

    public Integer averageBitrate() {
        return averageBitrate;
    }

    public AudioQuality audioQuality() {
        return audioQuality != null ? audioQuality : itag.audioQuality();
    }

    public Integer audioSampleRate() {
        return audioSampleRate;
    }

}
