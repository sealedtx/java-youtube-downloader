package com.github.kiulian.downloader.model.videos.formats;



import com.github.kiulian.downloader.model.videos.quality.AudioQuality;
import com.github.kiulian.downloader.model.videos.quality.VideoQuality;

public enum Itag {

    unknown {
        @Override
        public void setId(int id) {
            this.id = id;
        }
    },

    i5(VideoQuality.small, AudioQuality.low),
    i6(VideoQuality.small, AudioQuality.low),
    i13(VideoQuality.unknown, AudioQuality.low),
    i17(VideoQuality.tiny, AudioQuality.low),
    i18(VideoQuality.medium, AudioQuality.low),
    i22(VideoQuality.hd720, AudioQuality.medium),

    i34(VideoQuality.medium, AudioQuality.medium),
    i35(VideoQuality.large, AudioQuality.medium),
    i36(VideoQuality.tiny, AudioQuality.unknown),
    i37(VideoQuality.hd1080, AudioQuality.medium),
    i38(VideoQuality.highres, AudioQuality.medium),

    i43(VideoQuality.medium, AudioQuality.medium),
    i44(VideoQuality.large, AudioQuality.medium),
    i45(VideoQuality.hd720, AudioQuality.medium),
    i46(VideoQuality.hd1080, AudioQuality.medium),

    // 3D videos
    i82(VideoQuality.medium, AudioQuality.medium, true),
    i83(VideoQuality.large, AudioQuality.medium, true),
    i84(VideoQuality.hd720, AudioQuality.medium, true),
    i85(VideoQuality.hd1080, AudioQuality.medium, true),
    i100(VideoQuality.medium, AudioQuality.medium, true),
    i101(VideoQuality.large, AudioQuality.medium, true),
    i102(VideoQuality.hd720, AudioQuality.medium, true),

    // Apple HTTP Live Streaming
    i91(VideoQuality.tiny, AudioQuality.low),
    i92(VideoQuality.small, AudioQuality.low),
    i93(VideoQuality.medium, AudioQuality.medium),
    i94(VideoQuality.large, AudioQuality.medium),
    i95(VideoQuality.hd720, AudioQuality.high),
    i96(VideoQuality.hd1080, AudioQuality.high),
    i132(VideoQuality.small, AudioQuality.low),
    i151(VideoQuality.tiny, AudioQuality.low),

    // DASH mp4 video
    i133(VideoQuality.small),
    i134(VideoQuality.medium),
    i135(VideoQuality.large),
    i136(VideoQuality.hd720),
    i137(VideoQuality.hd1080),
    i138(VideoQuality.hd2160),
    i160(VideoQuality.tiny),
    i212(VideoQuality.large),
    i264(VideoQuality.hd1440),
    i266(VideoQuality.hd2160),
    i298(VideoQuality.hd720),
    i299(VideoQuality.hd1080),

    // DASH mp4 audio
    i139(AudioQuality.low),
    i140(AudioQuality.medium),
    i141(AudioQuality.high),
    i256(AudioQuality.unknown),
    i325(AudioQuality.unknown),
    i328(AudioQuality.unknown),

    // DASH webm video
    i167(VideoQuality.medium),
    i168(VideoQuality.large),
    i169(VideoQuality.hd720),
    i170(VideoQuality.hd1080),
    i218(VideoQuality.large),
    i219(VideoQuality.tiny),
    i242(VideoQuality.small),
    i243(VideoQuality.medium),
    i244(VideoQuality.large),
    i245(VideoQuality.large),
    i246(VideoQuality.large),
    i247(VideoQuality.hd720),
    i248(VideoQuality.hd1080),
    i271(VideoQuality.hd1440),
    i272(VideoQuality.highres),
    i278(VideoQuality.tiny),
    i302(VideoQuality.hd720),
    i303(VideoQuality.hd1080),
    i308(VideoQuality.hd1440),
    i313(VideoQuality.hd2160),
    i315(VideoQuality.hd2160),

    // DASH webm audio
    i171(AudioQuality.medium),
    i172(AudioQuality.high),

    // Dash webm audio with opus inside
    i249(AudioQuality.low),
    i250(AudioQuality.low),
    i251(AudioQuality.medium),

    // Dash webm hdr video
    i330(VideoQuality.tiny),
    i331(VideoQuality.small),
    i332(VideoQuality.medium),
    i333(VideoQuality.large),
    i334(VideoQuality.hd720),
    i335(VideoQuality.hd1080),
    i336(VideoQuality.hd1440),
    i337(VideoQuality.hd2160),

    // av01 video only formats
    i394(VideoQuality.tiny),
    i395(VideoQuality.small),
    i396(VideoQuality.medium),
    i397(VideoQuality.large),
    i398(VideoQuality.hd720),
    i399(VideoQuality.hd1080),
    i400(VideoQuality.hd1440),
    i401(VideoQuality.hd2160),
    i402(VideoQuality.hd2880p);

    protected int id;
    private VideoQuality videoQuality;
    private AudioQuality audioQuality;
    private boolean isVRor3D;

    Itag() {
    }

    Itag(VideoQuality videoQuality) {
        this(videoQuality, AudioQuality.noAudio, false);
    }

    Itag(AudioQuality audioQuality) {
        this(VideoQuality.noVideo, audioQuality, false);
    }

    Itag(VideoQuality videoQuality, AudioQuality audioQuality) {
        this(videoQuality, audioQuality, false);
    }

    Itag(VideoQuality videoQuality, AudioQuality audioQuality, boolean isVRor3D) {
        setId(Integer.parseInt(name().substring(1)));
        this.videoQuality = videoQuality;
        this.audioQuality = audioQuality;
        this.isVRor3D = isVRor3D;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }

    public VideoQuality videoQuality() {
        return videoQuality;
    }

    public AudioQuality audioQuality() {
        return audioQuality;
    }

    public boolean isVideo() {
        return this != unknown && videoQuality != VideoQuality.noVideo;
    }

    public boolean isAudio() {
        return this != unknown && audioQuality != AudioQuality.noAudio;
    }

    @Override
    public String toString() {
        return String.valueOf(id);
    }
}
