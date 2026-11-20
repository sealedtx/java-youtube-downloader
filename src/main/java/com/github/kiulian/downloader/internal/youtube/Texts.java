package com.github.kiulian.downloader.internal.youtube;

import com.github.kiulian.downloader.internal.json.JsonNode;

/**
 * YouTube text objects: {@code {"simpleText": ...}}, {@code {"runs": [{"text": ...}]}} or {@code {"content": ...}}.
 */
public final class Texts {

    private Texts() {
    }

    /** The text, or {@code null} if the node holds none of the known shapes. */
    public static String text(JsonNode node) {
        if (node.isString()) {
            return node.asString();
        }
        String simple = node.get("simpleText").asString();
        if (simple != null) {
            return simple;
        }
        if (node.get("runs").isArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode run : node.get("runs").items()) {
                sb.append(run.get("text").asString(""));
            }
            return sb.toString();
        }
        return node.get("content").asString();
    }
}
