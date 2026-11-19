package com.github.kiulian.downloader.internal.youtube;

import com.github.kiulian.downloader.internal.json.JsonNode;
import com.github.kiulian.downloader.model.Thumbnail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helpers shared by the extractors of YouTube's list pages (playlists, search).
 */
final class Renderers {

    private static final Pattern COUNT = Pattern.compile("([\\d][\\d.,]*)\\s*([KMB])?", Pattern.CASE_INSENSITIVE);

    private Renderers() {
    }

    /**
     * Objects that hold one of {@code keys}, in document order. Matches are not searched inside; subtrees under
     * {@code ignore} keys (ads, for example) are skipped.
     */
    static List<JsonNode> collect(JsonNode area, Set<String> keys, Set<String> ignore) {
        List<JsonNode> found = new ArrayList<>();
        collect(area, keys, ignore, found);
        return found;
    }

    private static void collect(JsonNode node, Set<String> keys, Set<String> ignore, List<JsonNode> found) {
        if (node.isArray()) {
            for (JsonNode item : node.items()) {
                collect(item, keys, ignore, found);
            }
            return;
        }
        for (String key : node.keys()) {
            if (keys.contains(key)) {
                found.add(node);
                return;
            }
        }
        for (String key : node.keys()) {
            if (!ignore.contains(key)) {
                collect(node.get(key), keys, ignore, found);
            }
        }
    }

    static List<JsonNode> collect(JsonNode area, Set<String> keys) {
        return collect(area, keys, Collections.emptySet());
    }

    /** First continuation token in a {@code continuationItemViewModel} or {@code continuationItemRenderer}. */
    static String continuationToken(JsonNode area) {
        List<JsonNode> containers = new ArrayList<>(area.findAll("continuationItemViewModel"));
        containers.addAll(area.findAll("continuationItemRenderer"));
        for (JsonNode container : containers) {
            String token = container.findFirst("token").asString();
            if (token != null) {
                return token;
            }
        }
        return null;
    }

    /**
     * Channel name and id of the first channel link below {@code area}: the new {@code text.commandRuns} shape,
     * the old {@code runs[].navigationEndpoint} shape or {@code videoOwnerRenderer}.
     *
     * @return {@code [name, channelId]}, each may be {@code null}
     */
    static String[] owner(JsonNode area) {
        for (JsonNode text : area.findAll("text")) {
            String content = text.get("content").asString();
            for (JsonNode run : text.get("commandRuns").items()) {
                String browseId = run.findFirst("browseId").asString();
                if (content != null && browseId != null && browseId.startsWith("UC")) {
                    int start = run.get("startIndex").asInt(0);
                    int end = Math.min(content.length(), start + run.get("length").asInt(content.length()));
                    String name = start < end ? content.substring(start, end) : content;
                    return new String[]{name.startsWith("by ") ? name.substring(3) : name, browseId};
                }
            }
        }
        for (JsonNode run : area.findAll("runs")) {
            for (JsonNode part : run.items()) {
                String browseId = part.at("navigationEndpoint", "browseEndpoint", "browseId").asString();
                if (browseId != null && browseId.startsWith("UC")) {
                    return new String[]{part.get("text").asString(), browseId};
                }
            }
        }
        JsonNode owner = area.findFirst("videoOwnerRenderer");
        if (owner.isPresent()) {
            return new String[]{Texts.text(owner.get("title")), owner.findFirst("browseId").asString()};
        }
        return new String[]{null, null};
    }

    static List<Thumbnail> thumbnails(JsonNode list) {
        List<Thumbnail> thumbnails = new ArrayList<>();
        for (JsonNode t : list.items()) {
            String url = t.get("url").asString();
            if (url != null) {
                thumbnails.add(new Thumbnail(url, t.get("width").asInt(0), t.get("height").asInt(0)));
            }
        }
        return thumbnails;
    }

    /** {@code 5:38} or {@code 1:02:03} to seconds; anything else (LIVE, UPCOMING) is 0. */
    static long parseDuration(String text) {
        if (text == null || !text.trim().matches("\\d+(:\\d{1,2}){1,2}")) {
            return 0;
        }
        long seconds = 0;
        for (String part : text.trim().split(":")) {
            seconds = seconds * 60 + Long.parseLong(part);
        }
        return seconds;
    }

    /**
     * Number from English YouTube texts: {@code 2,917,237 views}, {@code 2.8M views}, {@code 15.1K subscribers},
     * {@code 22 videos}. Abbreviated values are approximate. 0 if there is no number.
     */
    static long parseCount(String text) {
        if (text == null) {
            return 0;
        }
        Matcher matcher = COUNT.matcher(text);
        if (!matcher.find()) {
            return 0;
        }
        String suffix = matcher.group(2);
        if (suffix == null) {
            String digits = matcher.group(1).replaceAll("\\D", "");
            try {
                return digits.isEmpty() ? 0 : Long.parseLong(digits);
            } catch (NumberFormatException tooLong) {
                return 0;
            }
        }
        double value;
        try {
            value = Double.parseDouble(matcher.group(1).replace(",", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
        switch (suffix.toUpperCase(Locale.ROOT)) {
            case "K":
                return Math.round(value * 1_000);
            case "M":
                return Math.round(value * 1_000_000);
            default:
                return Math.round(value * 1_000_000_000);
        }
    }

    /** All string values below {@code node}, plus the joined text of every {@code runs} object. */
    static List<String> strings(JsonNode node) {
        List<String> found = new ArrayList<>();
        collectStrings(node, found);
        return found;
    }

    private static void collectStrings(JsonNode node, List<String> found) {
        if (node.isString()) {
            found.add(node.asString());
        } else if (node.isArray()) {
            for (JsonNode item : node.items()) {
                collectStrings(item, found);
            }
        } else {
            for (String key : node.keys()) {
                collectStrings(node.get(key), found);
            }
        }
        if (node.has("runs")) {
            String joined = Texts.text(node);
            if (joined != null) {
                found.add(joined);
            }
        }
    }
}
