package com.github.kiulian.downloader.model;

import com.github.kiulian.downloader.model.quality.AudioQuality;
import com.github.kiulian.downloader.model.quality.VideoQuality;

public enum Itag {

    unknown {
        @Override
        public void setId(int id) {
            this.id = id;
        }
    },

    i5(VideoQuality.small, AudioQuality.unknown),
    i6(VideoQuality.small, AudioQuality.unknown),
    i17(VideoQuality.tiny, AudioQuality.unknown),
    i18(VideoQuality.medium, AudioQuality.unknown),
    i22(VideoQuality.hd720, AudioQuality.unknown),

    i34(VideoQuality.medium, AudioQuality.unknown),
    i35(VideoQuality.large, AudioQuality.unknown),
    i36(VideoQuality.tiny, AudioQuality.unknown),
    i37(VideoQuality.hd1080, AudioQuality.unknown),
    i38(VideoQuality.highres, AudioQuality.unknown),

    i43(VideoQuality.medium, AudioQuality.unknown),
    i44(VideoQuality.large, AudioQuality.unknown),
    i45(VideoQuality.hd720, AudioQuality.unknown),
    i46(VideoQuality.hd1080, AudioQuality.unknown),

    i82(VideoQuality.medium, AudioQuality.unknown, true),
    i83(VideoQuality.large, AudioQuality.unknown, true),
    i84(VideoQuality.hd720, AudioQuality.unknown, true),
    i85(VideoQuality.hd1080, AudioQuality.unknown, true),

    i92(VideoQuality.small, AudioQuality.unknown, true),
    i93(VideoQuality.medium, AudioQuality.unknown, true),
    i94(VideoQuality.large, AudioQuality.unknown, true),
    i95(VideoQuality.hd720, AudioQuality.unknown, true),
    i96(VideoQuality.hd1080, AudioQuality.unknown),

    i100(VideoQuality.medium, AudioQuality.unknown, true),
    i101(VideoQuality.large, AudioQuality.unknown, true),
    i102(VideoQuality.hd720, AudioQuality.unknown, true),

    i132(VideoQuality.small, AudioQuality.unknown),
    i133(VideoQuality.small),
    i134(VideoQuality.medium),
    i135(VideoQuality.large),
    i136(VideoQuality.hd720),
    i137(VideoQuality.hd1080),
    i138(VideoQuality.hd2160),
    i139(AudioQuality.low),
    i140(AudioQuality.medium),
    i141(AudioQuality.high),

    i151(VideoQuality.tiny, AudioQuality.unknown),

    i160(VideoQuality.tiny),
    i167(VideoQuality.medium),
    i168(VideoQuality.large),
    i169(VideoQuality.hd1080),

    i171(AudioQuality.medium),


    i218(VideoQuality.large),
    i219(VideoQuality.tiny),

    i242(VideoQuality.small),
    i243(VideoQuality.medium),
    i244(VideoQuality.large),
    i245(VideoQuality.large),
    i246(VideoQuality.large),
    i247(VideoQuality.hd720),
    i248(VideoQuality.hd1080),
    i249(AudioQuality.low),
    i250(AudioQuality.medium),
    i251(AudioQuality.medium),

    i264(VideoQuality.hd1440),
    i266(VideoQuality.hd2160),

    i271(VideoQuality.hd1440),
    i272(VideoQuality.highres),
    i278(VideoQuality.tiny),

    i298(VideoQuality.hd720),
    i299(VideoQuality.hd1080),

    i302(VideoQuality.hd720),
    i303(VideoQuality.hd1080),
    i308(VideoQuality.hd1440),

    i313(VideoQuality.hd2160),
    i315(VideoQuality.hd2160),

    i330(VideoQuality.tiny),
    i331(VideoQuality.small),
    i332(VideoQuality.medium),
    i333(VideoQuality.large),
    i334(VideoQuality.hd720),
    i335(VideoQuality.hd1080),
    i336(VideoQuality.hd1440),
    i337(VideoQuality.hd2160),


    i394(VideoQuality.tiny),
    i395(VideoQuality.small),
    i396(VideoQuality.medium),
    i397(VideoQuality.large),
    i398(VideoQuality.hd720),
    i399(VideoQuality.hd1080),


    ;

    protected int id;
    private VideoQuality videoQuality;
    private AudioQuality audioQuality;
    private boolean isVRor3D;

    Itag() {
        this.videoQuality = VideoQuality.unknown;
        this.audioQuality = AudioQuality.unknown;
        this.isVRor3D = false;
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
        return videoQuality != VideoQuality.noVideo;
    }

    public boolean isAudio() {
        return audioQuality != AudioQuality.noAudio;
    }

    @Override
    public String toString() {
        return String.valueOf(id);
    }
}
