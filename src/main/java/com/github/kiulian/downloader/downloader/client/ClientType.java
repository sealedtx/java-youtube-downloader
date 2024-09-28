package com.github.kiulian.downloader.downloader.client;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

public enum ClientType {



    WEB("2.20210714",base_json()),
    ANDROID("17.36.4",base_json(),
            queryParam("context/client","androidSdkVersion","30"));


    private final String body;
    private final String version;


    ClientType(String version, JSONObject body, QueryParameter... parameters){
        //assert version!=null;
    this.version=version;
    JSONObject client = body.getJSONObject("context").getJSONObject("client");
    client.fluentPut("clientName",name());
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


    public String getVersion() {
        return version;
    }

    public JSONObject getBody() {
        return JSON.parseObject(body);
    }


    private static JSONObject base_json(){
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
    private static QueryParameter queryParam(String path,String key, String value){
        return new QueryParameter(path,key,value);
    }
    private static QueryParameter queryParam(String key,String value){
        return new QueryParameter(key,value);
    }
    private static class QueryParameter{
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
