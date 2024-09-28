package com.github.kiulian.downloader.downloader.client;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

public class ClientType {

    public static final ClientType WEB = new ClientType("WEB","2.20210714",base_json());
    public static final ClientType MWEB = new ClientType("MWEB","2.20220918",base_json());
    public static final ClientType IOS = new ClientType("IOS","17.36.4",base_json());
    public static final ClientType ANDROID = new ClientType("ANDROID","17.36.4",base_json(),
            queryParam("context/client","androidSdkVersion","30"));
    public static final ClientType ANDROID_TV = new ClientType("ANDROID_TV","2.19.1.303051424",base_json(),
            queryParam("context/client","androidSdkVersion","30"));
    public static final ClientType ANDROID_MUSIC = new ClientType("ANDROID_MUSIC","5.26.1",base_json(),
            queryParam("context/client","androidSdkVersion","30"));



    private final String body;
    private final String version;
    private final String name;
    public ClientType(String name, String version, JSONObject body, QueryParameter... parameters){
        this.name =name;
        this.version=version;
        JSONObject client = body.getJSONObject("context").getJSONObject("client");
        client.fluentPut("clientName",name);
        client.fluentPut("clientVersion",version);
        JSONObject cur=body;
        for (QueryParameter param:parameters) {
            for (String p:param.path) {
                cur=cur.getJSONObject(p);
            }
            cur.fluentPut(param.key,param.value);

        }
        this.body = body.toJSONString();

    }
    public ClientType(String name, String version, JSONObject body){
        this.name=name;
        this.version=version;
        JSONObject client = body.getJSONObject("context").getJSONObject("client");
        client.fluentPut("clientName",name);
        client.fluentPut("clientVersion",version);
        this.body = body.toJSONString();

    }
    public String getVersion() {
        return version;
    }
    public String getName(){return name;}
    public String getBodyString(){return body;}
    public JSONObject getBody() {
        return JSON.parseObject(body);
    }
    public static JSONObject base_json(){
        /*
{
    "context": {
        "client": {
            "hl": "en",
            "gl": "US"
        }
    }
}
        */
        JSONObject client = new JSONObject().fluentPut("hl", "en").fluentPut("gl", "US");
        JSONObject context = new JSONObject().fluentPut("client", client);
        return new JSONObject().fluentPut("context", context);
    }
    public static QueryParameter queryParam(String path, String key, String value){
        return new QueryParameter(path,key,value);
    }
    public static QueryParameter queryParam(String key, String value){
        return new QueryParameter(key,value);
    }
    public static class QueryParameter{
        final String[] path;
        final String value;
        final String key;
        QueryParameter(String path,String key,String value){
            this.path=path.split("/");
            this.value=value;
            this.key=key;
        }
        QueryParameter(String key,String value){
            this("",key,value);

        }
    }
}
