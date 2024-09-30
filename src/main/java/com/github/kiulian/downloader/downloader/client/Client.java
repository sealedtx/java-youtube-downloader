package com.github.kiulian.downloader.downloader.client;

import com.alibaba.fastjson.JSONObject;

public class Client{
    private final ClientType type;
    private final int priority;
    Client(ClientType type){
        this(type,0);
    }
    Client(ClientType type,int priority){
        this.type=type;
        this.priority=priority;

    }



    public ClientType getType() {
        return type;
    }

    public int getPriority() {
        return priority;
    }
}