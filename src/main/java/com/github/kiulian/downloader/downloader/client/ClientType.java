package com.github.kiulian.downloader.downloader.client;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

public class ClientType {
    public static final ClientType WEB = new ClientType("WEB", "2.20220918", baseJson());
    public static final ClientType MWEB = new ClientType("MWEB", "2.20220918", baseJson());
    public static final ClientType ANDROID = new ClientType("ANDROID", "17.36.4", baseJson(), queryParam("context/client", "androidSdkVersion", "30"));
    public static final ClientType IOS = new ClientType("IOS", "17.36.4", baseJson());
    public static final ClientType TVHTML5 = new ClientType("TVHTML5", "7.20220918", baseJson());
    public static final ClientType TVLITE = new ClientType("TVLITE", "2", baseJson());
    public static final ClientType TVANDROID = new ClientType("TVANDROID", "1.0", baseJson(), queryParam("context/client", "androidSdkVersion", "30"));
    public static final ClientType XBOXONEGUIDE = new ClientType("XBOXONEGUIDE", "1.0", baseJson());
    public static final ClientType ANDROID_CREATOR = new ClientType("ANDROID_CREATOR", "22.36.102", baseJson(), queryParam("context/client", "androidSdkVersion", "30"));
    public static final ClientType IOS_CREATOR = new ClientType("IOS_CREATOR", "22.36.102", baseJson());
    public static final ClientType TVAPPLE = new ClientType("TVAPPLE", "1.0", baseJson());
    public static final ClientType ANDROID_KIDS = new ClientType("ANDROID_KIDS", "7.36.1", baseJson(), queryParam("context/client", "androidSdkVersion", "30"));
    public static final ClientType IOS_KIDS = new ClientType("IOS_KIDS", "7.36.1", baseJson());
    public static final ClientType ANDROID_MUSIC = new ClientType("ANDROID_MUSIC", "5.26.1", baseJson(), queryParam("context/client", "androidSdkVersion", "30"));
    public static final ClientType ANDROID_TV = new ClientType("ANDROID_TV", "2.19.1.303051424", baseJson(), queryParam("context/client", "androidSdkVersion", "30"));
    public static final ClientType IOS_MUSIC = new ClientType("IOS_MUSIC", "5.26.1", baseJson());
    public static final ClientType MWEB_TIER_2 = new ClientType("MWEB_TIER_2", "9.20220918", baseJson());
    public static final ClientType ANDROID_VR = new ClientType("ANDROID_VR", "1.37", baseJson(), queryParam("context/client", "androidSdkVersion", "30"));
    public static final ClientType ANDROID_UNPLUGGED = new ClientType("ANDROID_UNPLUGGED", "6.36", baseJson(), queryParam("context/client", "androidSdkVersion", "30"));
    public static final ClientType ANDROID_TESTSUITE = new ClientType("ANDROID_TESTSUITE", "1.9", baseJson(), queryParam("context/client", "androidSdkVersion", "30"));
    public static final ClientType WEB_MUSIC_ANALYTICS = new ClientType("WEB_MUSIC_ANALYTICS", "0.2", baseJson());
    public static final ClientType IOS_UNPLUGGED = new ClientType("IOS_UNPLUGGED", "6.36", baseJson());
    public static final ClientType ANDROID_LITE = new ClientType("ANDROID_LITE", "3.26.1", baseJson(), queryParam("context/client", "androidSdkVersion", "30"));
    public static final ClientType IOS_EMBEDDED_PLAYER = new ClientType("IOS_EMBEDDED_PLAYER", "2.4", baseJson());
    public static final ClientType WEB_UNPLUGGED = new ClientType("WEB_UNPLUGGED", "1.20220918", baseJson());
    public static final ClientType WEB_EXPERIMENTS = new ClientType("WEB_EXPERIMENTS", "1", baseJson());
    public static final ClientType TVHTML5_CAST = new ClientType("TVHTML5_CAST", "1.1", baseJson());
    public static final ClientType ANDROID_EMBEDDED_PLAYER = new ClientType("ANDROID_EMBEDDED_PLAYER", "17.36.4", baseJson(), queryParam("context/client", "androidSdkVersion", "30"));
    public static final ClientType WEB_EMBEDDED_PLAYER = new ClientType("WEB_EMBEDDED_PLAYER", "9.20220918", baseJson());
    public static final ClientType TVHTML5_AUDIO = new ClientType("TVHTML5_AUDIO", "2.0", baseJson());
    public static final ClientType TV_UNPLUGGED_CAST = new ClientType("TV_UNPLUGGED_CAST", "0.1", baseJson());
    public static final ClientType TVHTML5_KIDS = new ClientType("TVHTML5_KIDS", "3.20220918", baseJson());
    public static final ClientType WEB_HEROES = new ClientType("WEB_HEROES", "0.1", baseJson());
    public static final ClientType WEB_MUSIC = new ClientType("WEB_MUSIC", "1.0", baseJson());
    public static final ClientType WEB_CREATOR = new ClientType("WEB_CREATOR", "1.20220918", baseJson());
    public static final ClientType TV_UNPLUGGED_ANDROID = new ClientType("TV_UNPLUGGED_ANDROID", "1.37", baseJson(), queryParam("context/client", "androidSdkVersion", "30"));
    public static final ClientType IOS_LIVE_CREATION_EXTENSION = new ClientType("IOS_LIVE_CREATION_EXTENSION", "17.36.4", baseJson());
    public static final ClientType TVHTML5_UNPLUGGED = new ClientType("TVHTML5_UNPLUGGED", "6.36", baseJson());
    public static final ClientType IOS_MESSAGES_EXTENSION = new ClientType("IOS_MESSAGES_EXTENSION", "17.36.4", baseJson());
    public static final ClientType WEB_REMIX = new ClientType("WEB_REMIX", "1.20220918", baseJson());
    public static final ClientType IOS_UPTIME = new ClientType("IOS_UPTIME", "1.0", baseJson());
    public static final ClientType WEB_UNPLUGGED_ONBOARDING = new ClientType("WEB_UNPLUGGED_ONBOARDING", "0.1", baseJson());
    public static final ClientType WEB_UNPLUGGED_OPS = new ClientType("WEB_UNPLUGGED_OPS", "0.1", baseJson());
    public static final ClientType WEB_UNPLUGGED_PUBLIC = new ClientType("WEB_UNPLUGGED_PUBLIC", "0.1", baseJson());
    public static final ClientType TVHTML5_VR = new ClientType("TVHTML5_VR", "0.1", baseJson());
    public static final ClientType ANDROID_TV_KIDS = new ClientType("ANDROID_TV_KIDS", "1.19.1", baseJson(), queryParam("context/client", "androidSdkVersion", "30"));
    public static final ClientType TVHTML5_SIMPLY = new ClientType("TVHTML5_SIMPLY", "1.0", baseJson());
    public static final ClientType WEB_KIDS = new ClientType("WEB_KIDS", "2.20220918", baseJson());
    public static final ClientType MUSIC_INTEGRATIONS = new ClientType("MUSIC_INTEGRATIONS", "0.1", baseJson());
    public static final ClientType TVHTML5_YONGLE = new ClientType("TVHTML5_YONGLE", "0.1", baseJson());
    public static final ClientType GOOGLE_ASSISTANT = new ClientType("GOOGLE_ASSISTANT", "0.1", baseJson());
    public static final ClientType TVHTML5_SIMPLY_EMBEDDED_PLAYER = new ClientType("TVHTML5_SIMPLY_EMBEDDED_PLAYER", "2.0", baseJson());
    public static final ClientType WEB_INTERNAL_ANALYTICS = new ClientType("WEB_INTERNAL_ANALYTICS", "0.1", baseJson());
    public static final ClientType WEB_PARENT_TOOLS = new ClientType("WEB_PARENT_TOOLS", "1.20220918", baseJson());
    public static final ClientType GOOGLE_MEDIA_ACTIONS = new ClientType("GOOGLE_MEDIA_ACTIONS", "0.1", baseJson());
    public static final ClientType WEB_PHONE_VERIFICATION = new ClientType("WEB_PHONE_VERIFICATION", "1.0.0", baseJson());
    public static final ClientType IOS_PRODUCER = new ClientType("IOS_PRODUCER", "0.1", baseJson());
    public static final ClientType TVHTML5_FOR_KIDS = new ClientType("TVHTML5_FOR_KIDS", "7.20220918", baseJson());
    public static final ClientType GOOGLE_LIST_RECS = new ClientType("GOOGLE_LIST_RECS", "0.1", baseJson());
    public static final ClientType MEDIA_CONNECT_FRONTEND = new ClientType("MEDIA_CONNECT_FRONTEND", "0.1", baseJson());


    private final String body;
    private final String version;
    private final String name;

    public ClientType(String name, String version, JSONObject body, QueryParameter... parameters) {
        this.name = name;
        this.version = version;
        JSONObject client = body.getJSONObject("context").getJSONObject("client");
        client.fluentPut("clientName", name);
        client.fluentPut("clientVersion", version);
        JSONObject cur = body;
        for (QueryParameter param : parameters) {
            for (String p : param.path) {
                cur = cur.getJSONObject(p);
            }
            cur.fluentPut(param.key, param.value);

        }
        this.body = body.toJSONString();

    }

    public ClientType(String name, String version, JSONObject body) {
        this.name = name;
        this.version = version;
        JSONObject client = body.getJSONObject("context").getJSONObject("client");
        client.fluentPut("clientName", name);
        client.fluentPut("clientVersion", version);
        this.body = body.toJSONString();

    }

    public String getVersion() {
        return version;
    }

    public String getName() {
        return name;
    }

    public String getBodyString() {
        return body;
    }

    public JSONObject getBody() {
        return JSON.parseObject(body);
    }

    private static JSONObject baseJson() {
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

    public static QueryParameter queryParam(String path, String key, String value) {
        return new QueryParameter(path, key, value);
    }

    public static QueryParameter queryParam(String key, String value) {
        return new QueryParameter(key, value);
    }

    public static class QueryParameter {
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
