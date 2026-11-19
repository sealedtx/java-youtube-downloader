package com.github.kiulian.downloader.internal.youtube;

import com.github.kiulian.downloader.YoutubeException;
import com.github.kiulian.downloader.YoutubeException.HttpException;
import com.github.kiulian.downloader.internal.SearchParams;
import com.github.kiulian.downloader.internal.http.HttpStatusException;
import com.github.kiulian.downloader.internal.json.JsonException;
import com.github.kiulian.downloader.internal.json.JsonNode;
import com.github.kiulian.downloader.model.search.SearchChannel;
import com.github.kiulian.downloader.model.search.SearchItem;
import com.github.kiulian.downloader.model.search.SearchPlaylist;
import com.github.kiulian.downloader.model.search.SearchQuery;
import com.github.kiulian.downloader.model.search.SearchResult;
import com.github.kiulian.downloader.model.search.SearchVideo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Search through innertube {@code search}; the first page and the following pages go the same way.
 * <p>
 * Items are found by renderer name; unknown kinds and ads are skipped, so new YouTube widgets do not break the
 * result.
 */
public final class SearchExtractor {

    private static final Set<String> ITEM_KEYS = new HashSet<>(Arrays.asList(
            "videoRenderer", "channelRenderer", "playlistRenderer", "lockupViewModel", "shortsLockupViewModel",
            "reelItemRenderer"));
    // ads and "key moments" carousels hold videos that are not search hits
    private static final Set<String> IGNORE = new HashSet<>(Arrays.asList(
            "adSlotRenderer", "promotedSparklesWebRenderer", "horizontalCardListRenderer"));
    private static final Pattern VIDEO_ID = Pattern.compile("[A-Za-z0-9_-]{11}");

    private final Innertube innertube;

    public SearchExtractor(Innertube innertube) {
        this.innertube = innertube;
    }

    public SearchResult search(SearchQuery query) {
        String params = SearchParams.encode(query.sort(), query.filters(), query.isExact());
        JsonNode page;
        try {
            page = innertube.search(query.text(), params);
        } catch (IOException | JsonException e) {
            throw failure("Could not search for \"" + query.text() + "\"", e);
        }
        JsonNode area = page.at("contents", "twoColumnSearchResultsRenderer", "primaryContents")
                .or(page.get("contents"));
        return result(query, page, area, null);
    }

    private SearchResult result(SearchQuery query, JsonNode page, JsonNode area, String currentToken) {
        List<SearchItem> items = new ArrayList<>();
        for (JsonNode node : Renderers.collect(area, ITEM_KEYS, IGNORE)) {
            SearchItem item = item(node);
            if (item != null) {
                items.add(item);
            }
        }
        String token = Renderers.continuationToken(area);
        // a page that points to itself would loop forever
        Supplier<SearchResult> next = token == null || token.equals(currentToken) ? null : () -> nextPage(query, token);
        JsonNode corrected = page.findFirst("showingResultsForRenderer").get("correctedQuery");
        List<String> refinements = new ArrayList<>();
        for (JsonNode card : page.findAll("searchRefinementCardRenderer")) {
            String text = Texts.text(card.get("query"));
            if (text != null) {
                refinements.add(text);
            }
        }
        return new SearchResult(query, items, page.get("estimatedResults").asLong(0), Texts.text(corrected), refinements, next);
    }

    private SearchResult nextPage(SearchQuery query, String token) {
        JsonNode page;
        try {
            page = innertube.continuation("search", token);
        } catch (IOException | JsonException e) {
            throw failure("Could not load the next search page for \"" + query.text() + "\"", e);
        }
        JsonNode area = page.get("onResponseReceivedCommands").or(page.get("continuationContents"));
        return result(query, page, area, token);
    }

    /** {@code null} for kinds that are not videos, channels or playlists, or for broken entries. */
    static SearchItem item(JsonNode node) {
        JsonNode video = node.get("videoRenderer");
        if (video.isPresent()) {
            String id = video.get("videoId").asString();
            if (!isVideoId(id)) {
                return null;
            }
            String[] owner = Renderers.owner(video.get("ownerText").or(video.get("longBylineText")));
            boolean live = false;
            for (JsonNode badge : video.findAll("metadataBadgeRenderer")) {
                live |= "BADGE_STYLE_TYPE_LIVE_NOW".equals(badge.get("style").asString());
            }
            return new SearchVideo(id, Texts.text(video.get("title")), owner[0], owner[1],
                    Renderers.parseDuration(Texts.text(video.get("lengthText"))),
                    Renderers.parseCount(Texts.text(video.get("viewCountText"))),
                    Texts.text(video.get("publishedTimeText")), live, false,
                    Renderers.thumbnails(video.at("thumbnail", "thumbnails")));
        }
        JsonNode shortItem = node.get("shortsLockupViewModel").or(node.get("reelItemRenderer"));
        if (shortItem.isPresent()) {
            String id = shortItem.findFirst("videoId").asString();
            if (!isVideoId(id)) {
                return null;
            }
            JsonNode overlay = shortItem.get("overlayMetadata");
            String title = Texts.text(overlay.get("primaryText")) != null ? Texts.text(overlay.get("primaryText")) : Texts.text(shortItem.get("headline"));
            String views = Texts.text(overlay.get("secondaryText")) != null ? Texts.text(overlay.get("secondaryText")) : Texts.text(shortItem.get("viewCountText"));
            return new SearchVideo(id, title, null, null, 0, Renderers.parseCount(views), null, false, true,
                    Renderers.thumbnails(shortItem.findFirst("sources").or(shortItem.at("thumbnail", "thumbnails"))));
        }
        JsonNode channel = node.get("channelRenderer");
        if (channel.isPresent()) {
            String id = channel.get("channelId").asString();
            if (id == null || !id.startsWith("UC")) {
                return null;
            }
            // YouTube swaps these fields around: the handle and the subscriber count are found by content
            String handle = null;
            long subscribers = 0;
            for (String field : new String[]{"subscriberCountText", "videoCountText"}) {
                String text = Texts.text(channel.get(field));
                if (text == null) {
                    continue;
                }
                if (text.startsWith("@")) {
                    handle = text;
                } else if (text.toLowerCase().contains("subscriber")) {
                    subscribers = Renderers.parseCount(text);
                }
            }
            return new SearchChannel(id, Texts.text(channel.get("title")), handle, subscribers,
                    Texts.text(channel.get("descriptionSnippet")), Renderers.thumbnails(channel.at("thumbnail", "thumbnails")));
        }
        JsonNode lockup = node.get("lockupViewModel");
        if (lockup.isPresent()) {
            String type = lockup.get("contentType").asString("");
            String id = lockup.get("contentId").asString();
            JsonNode meta = lockup.at("metadata", "lockupMetadataViewModel");
            String[] owner = Renderers.owner(meta);
            String badge = lockup.findFirst("thumbnailBadgeViewModel").get("text").asString();
            if ((type.equals("LOCKUP_CONTENT_TYPE_PLAYLIST") || type.equals("LOCKUP_CONTENT_TYPE_PODCAST")) && id != null) {
                return new SearchPlaylist(id, Texts.text(meta.get("title")), owner[0], owner[1],
                        badge != null && badge.toLowerCase().contains("video") ? Renderers.parseCount(badge) : 0,
                        Renderers.thumbnails(lockup.findFirst("sources")));
            }
            if (type.equals("LOCKUP_CONTENT_TYPE_VIDEO") && isVideoId(id)) {
                return new SearchVideo(id, Texts.text(meta.get("title")), owner[0], owner[1], Renderers.parseDuration(badge),
                        0, null, false, false, Renderers.thumbnails(lockup.findFirst("sources")));
            }
            return null;
        }
        JsonNode playlist = node.get("playlistRenderer");
        if (playlist.isPresent() && playlist.get("playlistId").asString() != null) {
            String[] owner = Renderers.owner(playlist.get("shortBylineText").or(playlist.get("longBylineText")));
            return new SearchPlaylist(playlist.get("playlistId").asString(), Texts.text(playlist.get("title")), owner[0], owner[1],
                    playlist.get("videoCount").asLong(0), Renderers.thumbnails(playlist.findFirst("thumbnails")));
        }
        return null;
    }

    private static boolean isVideoId(String id) {
        return id != null && VIDEO_ID.matcher(id).matches();
    }

    private static YoutubeException failure(String message, Exception cause) {
        if (cause instanceof HttpStatusException) {
            return new HttpException(((HttpStatusException) cause).status(), message + ": " + cause.getMessage(), cause);
        }
        return new YoutubeException(message + ": " + cause.getMessage(), cause);
    }
}
