package com.github.kiulian.downloader;


import static org.junit.jupiter.api.Assertions.assertEquals;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.github.kiulian.downloader.downloader.client.ClientType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class ClientTest {

    static class DepthEntry {
        String path;
        JSONObject object;
    }
    static class WidthEntry{
        Pair[] pairs;
        JSONObject object;
    }
    static class Pair{
        String key,value;

        public Pair(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }
    static DepthEntry generateDepth(JSONObject o, Random random, int maxValue, int maxDepth){

        int depth = Math.max(1,random.nextInt(maxDepth));


        JSONObject cur =o;
        StringBuilder path = new StringBuilder();
        for(int i=0;i<depth;i++){
            String key = String.valueOf(random.nextInt(maxValue));
            path.append(key).append("/");
            JSONObject c = cur.getJSONObject(key);
            if(c==null){
                cur.fluentPut(key,(cur=new JSONObject()));
            }else{
                cur=c;
            }
        }
        path.deleteCharAt(path.length()-1);
        DepthEntry entry = new DepthEntry();
        entry.path=path.toString();
        entry.object=cur;
        return entry;
    }
    static WidthEntry generateWidth(JSONObject o,Random random,int maxWidth){
        int width = Math.max(1,random.nextInt(maxWidth));

        Pair[] pairs = new Pair[width];
        for(int i=0;i<width;i++){
            String kv= String.valueOf(i);
            pairs[i] = new Pair(kv,kv);
            o.fluentPut(kv,kv);
        }
        WidthEntry entry = new WidthEntry();
        entry.object=o;
        entry.pairs=pairs;
        return entry;
    }
    @Test
    @DisplayName("ClientType body should be equal to the source JSON when constructed with QueryParameters")
    public void queryParameter_Success(){
        final Random random = new Random();
        final int MAX_KEYVAL=100;
        final int MAX_DEPTH=5;
        final int MAX_WIDTH=5;
        final int ITERATION_COUNT=10;
        String TEST_NAME = "TestClient";
        String TEST_VER = "TestVersion";
        JSONObject o =ClientType.baseJson();
        JSONObject client = o.getJSONObject("context").getJSONObject("client");
        client.fluentPut("clientName", TEST_NAME);
        client.fluentPut("clientVersion", TEST_VER);

        List<ClientType.QueryParameter> paramsNoKey = new LinkedList<>();
        List<ClientType.QueryParameter> paramsWithKey= new LinkedList<>();
        for(int i=0;i<ITERATION_COUNT;i++){
            DepthEntry d= generateDepth(o,random,MAX_KEYVAL,MAX_DEPTH);
            WidthEntry w = generateWidth(d.object,random,MAX_WIDTH);
            for(Pair p : w.pairs){
                paramsNoKey.add(ClientType.queryParam(d.path+"/"+p.key,p.value));
                paramsWithKey.add(ClientType.queryParam(d.path, p.key,p.value));
            }
        }

        String generatedJSON = JSONObject.toJSONString(o, SerializerFeature.PrettyFormat);
        System.out.println(generatedJSON);
        System.out.println("Query parameters:");
        for(ClientType.QueryParameter p: paramsWithKey){
            System.out.println(String.join("/",p.path)+"/" + p.key+ "="+p.value);
        }
        ClientType clNoKey = new ClientType(TEST_NAME,TEST_VER,ClientType.baseJson(),paramsNoKey.toArray(new ClientType.QueryParameter[0]));
        ClientType clWithKey = new ClientType(TEST_NAME,TEST_VER,ClientType.baseJson(),paramsWithKey.toArray(new ClientType.QueryParameter[0]));
        assertEquals(o, clNoKey.getBody(),"ClientType JSON body (no key) should be equal to the generated one.");
        assertEquals(o, clWithKey.getBody(),"ClientType JSON body (with key) should be equal to the generated one.");

    }
}
