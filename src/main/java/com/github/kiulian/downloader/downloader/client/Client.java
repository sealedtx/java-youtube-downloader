package com.github.kiulian.downloader.downloader.client;

import com.alibaba.fastjson.JSONObject;

public interface Client {
    static Client of(String name, String version, JSONObject body, QueryParameter... parameters) {
        final JSONObject client = body.getJSONObject("context").getJSONObject("client");

        client.fluentPut("clientName", name);
        client.fluentPut("clientVersion", version);
        JSONObject cur = body;
        for (final QueryParameter param: parameters) {
            for (final String p: param.path) {
                cur = cur.getJSONObject(p);
            }
            cur.fluentPut(param.key, param.value);
        }

        return new Client() {
            final String n = name;
            final String v = version;
            final String b = body.toJSONString();
            final JSONObject j = body;


            @Override
            public String getName() {
                return this.n;
            }

            @Override
            public String getVersion() {
                return this.v;
            }

            @Override
            public JSONObject getBody() {
                return this.j;
            }

            @Override
            public String getBodyString() {
                return this.b;
            }
        };
    }

    static Client of(String name, String version, JSONObject body) {
        return of(name, version, body, new QueryParameter[0]);
    }


    String getName();

    String getVersion();

    String getBodyString();

    JSONObject getBody();

    static JSONObject baseJson() {
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
        final JSONObject client = new JSONObject().fluentPut("hl", "en").fluentPut("gl", "US");
        final JSONObject context = new JSONObject().fluentPut("client", client);
        return new JSONObject().fluentPut("context", context);
    }

    static QueryParameter queryParam(String path, String key, String value) {
        return new QueryParameter(path, key, value);
    }

    static QueryParameter queryParam(String key, String value) {
        return new QueryParameter(key, value);
    }

    class QueryParameter {
        final String[] path;
        final String value;
        final String key;

        QueryParameter(String path, String key, String value) {
            this.path = path.split("/");
            this.value = value;
            this.key = key;
        }

        QueryParameter(String key, String value) {
            this("", key, value);
        }
    }
}
