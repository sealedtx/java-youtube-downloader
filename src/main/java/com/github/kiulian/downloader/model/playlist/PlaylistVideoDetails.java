package com.github.kiulian.downloader.model.playlist;

import com.alibaba.fastjson.JSONObject;
import com.github.kiulian.downloader.model.AbstractListVideoDetails;

public class PlaylistVideoDetails extends AbstractListVideoDetails {

    private int index;
    private boolean isPlayable;

    public PlaylistVideoDetails(JSONObject json) {
        super(json);
        if (!thumbnails().isEmpty()) {
            // Otherwise, contains "/hqdefault.jpg?"
            isLive = thumbnails().get(0).contains("/hqdefault_live.jpg?");
        }

        if (json.containsKey("index")) {
            index = json.getJSONObject("index").getIntValue("simpleText");
        }
        isPlayable = json.getBooleanValue("isPlayable");
    }

    @Override
    protected boolean isDownloadable() {
        return isPlayable && super.isDownloadable();
    }

    public int index() {
        return index;
    }

    public boolean isPlayable() {
        return isPlayable;
    }
}
