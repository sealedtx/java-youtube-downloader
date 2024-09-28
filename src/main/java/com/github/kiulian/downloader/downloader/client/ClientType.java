package com.github.kiulian.downloader.downloader.client;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

public enum ClientType {



    WEB("2.20210714",base_json()),
    ANDROID("17.36.4",base_json().getJSONObject("client").fluentPut("androidSdkVersion","30"));


    private final String context;
    private final String version;


    ClientType(String version, JSONObject context){
        //assert version!=null;
    this.version=version;
    JSONObject client = context.getJSONObject("client");
    client.fluentPut("clientName",name());
    client.fluentPut("clientVersion",version);
    this.context=context.toJSONString();
    }


    public String getVersion() {
        return version;
    }

    public String getContext() {
        return context;
    }



    private static final String BASE_CONTEXT =                     "{" +

            "  \"context\": {" +
            "    \"client\": {" +
            "      \"hl\": \"en\"," +
            "      \"gl\": \"US\"," +

            "    }" +
            "  }" +
            "}";

    private static JSONObject base_json(){
        return JSON.parseObject(BASE_CONTEXT);
    }





}
