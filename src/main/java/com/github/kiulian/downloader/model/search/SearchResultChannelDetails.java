package com.github.kiulian.downloader.model.search;

import com.alibaba.fastjson.JSONObject;
import com.github.kiulian.downloader.model.Utils;

public class SearchResultChannelDetails extends AbstractSearchResultList {

    private final String channelId;
    private final String videoCountText;
    private final String subscriberCountText;
    private final String description;

    public SearchResultChannelDetails(JSONObject json) {
        super(json);
        channelId = json.getString("channelId");
        videoCountText = Utils.parseRuns(json.getJSONObject("videoCountText"));
        if (json.containsKey("subscriberCountText")) {
            subscriberCountText = json.getJSONObject("subscriberCountText").getString("simpleText");
        } else {
            subscriberCountText = null;
        }
        description = Utils.parseRuns(json.getJSONObject("descriptionSnippet"));
        thumbnails = Utils.parseThumbnails(json.getJSONObject("thumbnail"));
    }

    @Override
    public SearchResultItemType type() {
        return SearchResultItemType.CHANNEL;
    }

    @Override
    public SearchResultChannelDetails asChannel() {
        return this;
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
