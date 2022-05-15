package com.github.kiulian.downloader.model.search;

import java.util.ArrayList;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.kiulian.downloader.model.Utils;

public class SearchResultChannelDetails extends AbstractSearchResultList {

    private String channelId;
    private String videoCountText;
    private String subscriberCountText;
    private String description;

    public SearchResultChannelDetails() {}

    public SearchResultChannelDetails(JSONObject json) {
        super(json);
        channelId = json.getString("channelId");
        videoCountText = Utils.parseRuns(json.getJSONObject("videoCountText"));
        if (json.containsKey("subscriberCountText")) {
            subscriberCountText = json.getJSONObject("subscriberCountText").getString("simpleText");
        }
        description = Utils.parseRuns(json.getJSONObject("descriptionSnippet"));
        JSONArray jsonThumbnails = json.getJSONObject("thumbnail").getJSONArray("thumbnails");
        thumbnails = new ArrayList<>(jsonThumbnails.size());
        for (int i = 0; i < jsonThumbnails.size(); i++) {
            JSONObject jsonObject = jsonThumbnails.getJSONObject(i);
            if (jsonObject.containsKey("url"))
                thumbnails.add(jsonObject.getString("url"));
        }
    }

    @Override
    public boolean isChannel() {
        return true;
    }

    public String channelId() {
        return channelId;
    }

    public String videoCountText() {
        return videoCountText;
    }

    public String subscriberCountText() {
        return subscriberCountText;
    }

    public String description() {
        return description;
    }
}
