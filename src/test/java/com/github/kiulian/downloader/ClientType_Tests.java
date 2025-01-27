package com.github.kiulian.downloader;


import com.alibaba.fastjson.JSONObject;
import com.github.kiulian.downloader.downloader.client.ClientType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ClientType_Tests {

    @Test
    void testConstructorWithoutParameters() {
        JSONObject baseJson = ClientType.baseJson();
        ClientType clientType = new ClientType("TestName", "1.0", baseJson);

        assertEquals("TestName", clientType.getName());
        assertEquals("1.0", clientType.getVersion());

        JSONObject client = baseJson.getJSONObject("context").getJSONObject("client");
        assertEquals("TestName", client.getString("clientName"));
        assertEquals("1.0", client.getString("clientVersion"));

        JSONObject parsedBody = clientType.getBody();
        assertEquals(baseJson, parsedBody);
    }

    @Test
    void testConstructorWithParameters() {
        JSONObject baseJson = ClientType.baseJson();
        ClientType.QueryParameter param1 = ClientType.queryParam("path1/path2", "key1", "value1");
        ClientType.QueryParameter param2 = ClientType.queryParam("path3", "key2", "value2");

        ClientType clientType = new ClientType("TestName", "1.0", baseJson, param1, param2);

        assertEquals("TestName", clientType.getName());
        assertEquals("1.0", clientType.getVersion());

        JSONObject client = baseJson.getJSONObject("context").getJSONObject("client");
        assertEquals("TestName", client.getString("clientName"));
        assertEquals("1.0", client.getString("clientVersion"));

        JSONObject path1 = baseJson.getJSONObject("path1");
        assertNotNull(path1);
        assertTrue(path1.containsKey("path2"));

        JSONObject path2 = path1.getJSONObject("path2");
        assertNotNull(path2);
        assertEquals("value1", path2.getString("key1"));

        JSONObject path3 = baseJson.getJSONObject("path3");
        assertNotNull(path3);
        assertEquals("value2", path3.getString("key2"));
    }

    @Test
    void testConstructorPathOnly() {
        JSONObject baseJson = ClientType.baseJson();
        ClientType.QueryParameter param1 = ClientType.queryParam("path1/path2/key1", "value1");
        ClientType.QueryParameter param2 = ClientType.queryParam("path3/key2", "value2");
        ClientType.QueryParameter param3 = ClientType.queryParam("key3", "value3");
        ClientType clientType = new ClientType("TestName", "1.0", baseJson, param1, param2, param3);
        assertEquals("TestName", clientType.getName());
        assertEquals("1.0", clientType.getVersion());
        JSONObject client = baseJson.getJSONObject("context").getJSONObject("client");
        assertEquals("TestName", client.getString("clientName"));
        assertEquals("1.0", client.getString("clientVersion"));
        JSONObject path1 = baseJson.getJSONObject("path1");
        assertNotNull(path1);
        assertTrue(path1.containsKey("path2"));

        JSONObject path2 = path1.getJSONObject("path2");
        assertNotNull(path2);
        assertEquals("value1", path2.getString("key1"));
        JSONObject path3 = baseJson.getJSONObject("path3");
        assertNotNull(path3);
        assertEquals("value2", path3.getString("key2"));

        assertEquals("value3", baseJson.getString("key3"));
    }

    @Test
    void testBaseJson() {
        JSONObject baseJson = ClientType.baseJson();

        JSONObject context = baseJson.getJSONObject("context");
        assertNotNull(context);

        JSONObject client = context.getJSONObject("client");
        assertNotNull(client);
        assertEquals("en", client.getString("hl"));
        assertEquals("US", client.getString("gl"));
    }
}
