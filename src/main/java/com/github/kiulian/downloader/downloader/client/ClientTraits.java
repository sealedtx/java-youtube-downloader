package com.github.kiulian.downloader.downloader.client;

import com.alibaba.fastjson.JSONObject;
import com.github.kiulian.downloader.model.videos.quality.AudioQuality;
import com.github.kiulian.downloader.model.videos.quality.VideoQuality;

public class ClientTraits {
    private final ClientType type;
    private final VideoQuality minVideoQuality ;
    private final VideoQuality maxVideoQuality ;
    private final AudioQuality minAudioQuality;
    private final AudioQuality maxAudioQuality;
    private final int clientPriority;
    private final String body;

    public ClientTraits(TraitBuilder builder){
        this.type=builder.type;
        this.clientPriority=builder.clientPriority;
        this.minAudioQuality=builder.minAudioQuality;

        this.maxAudioQuality=builder.maxAudioQuality;
        this.minVideoQuality=builder.minVideoQuality;
        this.maxVideoQuality=builder.maxVideoQuality;
        this.body=type.getBody().toJSONString();
    }
    public String bodyString(){
        return body;
    }

    public JSONObject bodyJson(){
        return type.getBody();
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
    public static class TraitBuilder{
        private final ClientType type;
        public TraitBuilder(ClientType type){
            this.type=type;
        }
        private VideoQuality minVideoQuality ;
        private VideoQuality maxVideoQuality ;
        private AudioQuality minAudioQuality;
        private AudioQuality maxAudioQuality;
        private int clientPriority= -1;
        public TraitBuilder minVideoQuality(VideoQuality quality){
            this.minVideoQuality=quality;
            return this;
        }
        public TraitBuilder maxVideoQuality(VideoQuality quality){
            this.maxVideoQuality=quality;
            return this;
        }
        public TraitBuilder minAudioQuality(AudioQuality quality){
            this.minAudioQuality=quality;
            return this;
        }
        public TraitBuilder maxAudioQuality(AudioQuality quality){
            this.maxAudioQuality=quality;
            return this;
        }
        public TraitBuilder priority(int p){
            this.clientPriority=p;
            return this;
        }
    }

}
