package com.github.kiulian.downloader.downloader.client;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import static com.github.kiulian.downloader.downloader.client.Client.*;

public enum DefaultClients implements Client {
    ANDROID_VR("1.37", baseJson(), queryParam("context/client", "androidSdkVersion", "30")),
    WEB_HEROES("0.1", baseJson()),
    TVHTML5_VR("0.1", baseJson()),
    WEB_MUSIC_ANALYTICS("0.2", baseJson()),
    WEB_MUSIC("1.0", baseJson()),
    WEB("2.20220918", baseJson()),
    TVHTML5_SIMPLY("1.0", baseJson()),
    MWEB("2.20220918", baseJson()),
    WEB_REMIX("1.20220918", baseJson()),
    TVHTML5("7.20220918", baseJson()),
    TVHTML5_CAST("1.1", baseJson()),
    GOOGLE_LIST_RECS("0.1", baseJson()),
    IOS_EMBEDDED_PLAYER("2.4", baseJson()),
    ANDROID_TV("2.19.1.303051424", baseJson(), queryParam("context/client", "androidSdkVersion", "30")),
    IOS_MESSAGES_EXTENSION("17.36.4", baseJson()),
    ANDROID_EMBEDDED_PLAYER("17.36.4", baseJson(), queryParam("context/client", "androidSdkVersion", "30")),
    IOS_LIVE_CREATION_EXTENSION("17.36.4", baseJson()),
    WEB_PHONE_VERIFICATION("1.0.0", baseJson()),
    IOS_PRODUCER("0.1", baseJson()),
    WEB_EXPERIMENTS("1", baseJson()),
    TVANDROID("1.0", baseJson(), queryParam("context/client", "androidSdkVersion", "30")),
    MWEB_TIER_2("9.20220918", baseJson()),
    MUSIC_INTEGRATIONS("0.1", baseJson()),
    MEDIA_CONNECT_FRONTEND("0.1", baseJson()),
    IOS("17.36.4", baseJson()),
    TVHTML5_YONGLE("0.1", baseJson()),
    GOOGLE_ASSISTANT("0.1", baseJson()),
    XBOXONEGUIDE("1.0", baseJson()),
    WEB_INTERNAL_ANALYTICS("0.1", baseJson()),
    GOOGLE_MEDIA_ACTIONS("0.1", baseJson()),
    WEB_PARENT_TOOLS("1.20220918", baseJson()),
    IOS_MUSIC("5.26.1", baseJson()),
    ANDROID_MUSIC("5.26.1", baseJson(), queryParam("context/client", "androidSdkVersion", "30")),
    WEB_CREATOR("1.20220918", baseJson()),
    IOS_CREATOR("22.36.102", baseJson()),
    ANDROID_CREATOR("22.36.102", baseJson(), queryParam("context/client", "androidSdkVersion", "30")),
    ANDROID_LITE("3.26.1", baseJson(), queryParam("context/client", "androidSdkVersion", "30")),
    TVAPPLE("1.0", baseJson()),
    TVLITE("2", baseJson()),
    WEB_EMBEDDED_PLAYER("9.20220918", baseJson()),
    TVHTML5_SIMPLY_EMBEDDED_PLAYER("2.0", baseJson()),
    WEB_UNPLUGGED_OPS("0.1", baseJson()),
    WEB_UNPLUGGED("1.20220918", baseJson()),
    WEB_UNPLUGGED_ONBOARDING("0.1", baseJson()),
    TV_UNPLUGGED_CAST("0.1", baseJson()),
    TVHTML5_UNPLUGGED("6.36", baseJson()),
    ANDROID_UNPLUGGED("6.36", baseJson(), queryParam("context/client", "androidSdkVersion", "30")),
    TV_UNPLUGGED_ANDROID("1.37", baseJson(), queryParam("context/client", "androidSdkVersion", "30")),
    WEB_UNPLUGGED_PUBLIC("0.1", baseJson()),
    IOS_UNPLUGGED("6.36", baseJson()),
    IOS_UPTIME("1.0", baseJson()),
    IOS_KIDS("7.36.1", baseJson()),
    ANDROID_TV_KIDS("1.19.1", baseJson(), queryParam("context/client", "androidSdkVersion", "30")),
    TVHTML5_AUDIO("2.0", baseJson()),
    TVHTML5_FOR_KIDS("7.20220918", baseJson()),
    ANDROID_KIDS("7.36.1", baseJson(), queryParam("context/client", "androidSdkVersion", "30")),
    TVHTML5_KIDS("3.20220918", baseJson()),
    WEB_KIDS("2.20220918", baseJson()),
    ANDROID("17.36.4", baseJson(), queryParam("context/client", "androidSdkVersion", "30")),
    ANDROID_TESTSUITE("1.9", baseJson(), queryParam("context/client", "androidSdkVersion", "30")),
    ;

    public static final Client[] VALUES = values();
    private static Client DEFAULT = VALUES[0];

    private final String body;
    private final String version;

    public static void setDefaultClientType(Client client) {
        DEFAULT = client;
    }

    public static Client defaultClientType() {
        return DEFAULT;
    }

    DefaultClients(String version, JSONObject body) {
        this(version, body, new QueryParameter[0]);
    }

    DefaultClients(String version, JSONObject body, QueryParameter... parameters) {
        this.version = version;

        final JSONObject client = body.getJSONObject("context").getJSONObject("client");
        client.fluentPut("clientName", this.name());
        client.fluentPut("clientVersion", version);
        JSONObject cur = body;
        for (final QueryParameter param: parameters) {
            for (String p: param.path) {
                cur = cur.getJSONObject(p);
            }
            cur.fluentPut(param.key, param.value);

        }
        this.body = body.toJSONString();
    }

    public DefaultClients next() {
        final int index = this.ordinal() + 1;
        if (index >= VALUES.length) {
            return (DefaultClients) VALUES[0];
        }
        return (DefaultClients) VALUES[index];
    }

    public DefaultClients previous() {
        final int index = this.ordinal() - 1;
        if (index <= 0) {
            return (DefaultClients) VALUES[VALUES.length - 1];
        }
        return (DefaultClients) VALUES[index];
    }

    @Override
    public String getVersion() {
        return this.version;
    }

    @Override
    public String getName() {
        return this.name();
    }

    @Override
    public String getBodyString() {
        return this.body;
    }

    @Override
    public JSONObject getBody() {
        return JSON.parseObject(this.body);
    }
}
