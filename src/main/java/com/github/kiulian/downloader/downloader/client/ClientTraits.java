package com.github.kiulian.downloader.downloader.client;

import com.github.kiulian.downloader.model.videos.formats.Itag;
import com.github.kiulian.downloader.model.videos.quality.AudioQuality;
import com.github.kiulian.downloader.model.videos.quality.VideoQuality;

public class ClientTraits {
    private final ClientType type;
    private VideoQuality minVideoQuality ;
    private VideoQuality maxVideoQuality ;
    private AudioQuality minAudioQuality;
    private AudioQuality maxAudioQuality;

    private int clientPriority= -1;
    public ClientTraits(ClientType type){this.type=type;}
    public ClientTraits minVideoQuality(VideoQuality quality){
        this.minVideoQuality=quality;
        return this;
    }
    public ClientTraits maxVideoQuality(VideoQuality quality){
        this.maxVideoQuality=quality;
        return this;
    }
    public ClientTraits minAudioQuality(AudioQuality quality){
        this.minAudioQuality=quality;
        return this;
    }
    public ClientTraits maxAudioQuality(AudioQuality quality){
        this.maxAudioQuality=quality;
        return this;
    }
    public ClientTraits priority(int p){
        this.clientPriority=p;
        return this;
    }


    public ClientType getType() {
        return type;
    }
    public int getClientPriority() {
        return clientPriority;
    }

    public VideoQuality getMinVideoQuality() {
        return minVideoQuality;
    }

    public VideoQuality getMaxVideoQuality() {
        return maxVideoQuality;
    }

    public AudioQuality getMinAudioQuality() {
        return minAudioQuality;
    }

    public AudioQuality getMaxAudioQuality() {
        return maxAudioQuality;
    }
}
