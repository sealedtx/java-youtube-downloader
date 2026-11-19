package com.github.kiulian.downloader.internal.youtube;

import com.github.kiulian.downloader.YoutubeException;
import com.github.kiulian.downloader.YoutubeException.ChannelUnavailableException;
import com.github.kiulian.downloader.YoutubeException.HttpException;
import com.github.kiulian.downloader.YoutubeException.PlaylistUnavailableException;
import com.github.kiulian.downloader.internal.Ids;
import com.github.kiulian.downloader.internal.http.HttpStatusException;
import com.github.kiulian.downloader.internal.json.JsonException;
import com.github.kiulian.downloader.internal.json.JsonNode;
import com.github.kiulian.downloader.model.playlist.Playlist;
import com.github.kiulian.downloader.model.playlist.PlaylistDetails;
import com.github.kiulian.downloader.model.playlist.PlaylistVideo;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Playlists and channel uploads through innertube {@code browse}, with the web page as fallback for the first page.
 * <p>
 * Items are found by renderer name anywhere in the list part of the answer, so YouTube can move them around.
 * Known item shapes: {@code lockupViewModel} (since 2025) and {@code playlistVideoRenderer} (before).
 */
public final class PlaylistExtractor {

    private static final Set<String> ITEM_KEYS = new HashSet<>(Arrays.asList("lockupViewModel", "playlistVideoRenderer"));
    private static final Pattern COUNT = Pattern.compile("^([\\d.,\\s]+)\\s+(videos?|views?)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern VIDEO_ID = Pattern.compile("[A-Za-z0-9_-]{11}");

    private final Innertube innertube;

    public PlaylistExtractor(Innertube innertube) {
        this.innertube = innertube;
    }

    public Playlist playlist(String playlistId) {
        List<String> warnings = new ArrayList<>();
        JsonNode page;
        try {
            page = innertube.browse("VL" + playlistId);
        } catch (IOException | JsonException apiError) {
            try {
                page = innertube.initialData("/playlist?list=" + playlistId);
                warnings.add("browse API failed (" + apiError.getMessage() + "), used the web page");
            } catch (IOException | JsonException pageError) {
                throw failure("Could not load playlist " + playlistId,
                        Arrays.asList("browse: " + apiError.getMessage(), "web page: " + pageError.getMessage()), apiError);
            }
        }
        String alert = errorAlert(page);
        if (alert != null) {
            throw new PlaylistUnavailableException(playlistId, alert);
        }

        List<JsonNode> firstItems = Renderers.collect(page.get("contents"), ITEM_KEYS);
        String firstToken = Renderers.continuationToken(page.get("contents"));
        if (firstItems.isEmpty()) {
            warnings.add("no videos found on the first page; the playlist may be empty or YouTube changed the page");
        }
        PlaylistDetails details = details(playlistId, page);
        Iterable<PlaylistVideo> videos = () -> new Pages(playlistId, firstItems, firstToken);
        return new Playlist(details, videos, warnings);
    }

    public Playlist channelUploads(String channel) {
        String[] parsed = Ids.channel(channel);
        String channelId = parsed[0];
        if (channelId == null) {
            try {
                channelId = innertube.resolveUrl(parsed[1]).at("endpoint", "browseEndpoint", "browseId").asString();
            } catch (IOException | JsonException e) {
                throw new ChannelUnavailableException(channel, e);
            }
            if (channelId == null || !channelId.startsWith("UC")) {
                throw new ChannelUnavailableException(channel, null);
            }
        }
        // the uploads playlist of channel UCxyz is UUxyz
        return playlist("UU" + channelId.substring(2));
    }

    private static PlaylistDetails details(String playlistId, JsonNode page) {
        JsonNode metadata = page.at("metadata", "playlistMetadataRenderer");
        String title = metadata.get("title").asString();
        if (title == null) {
            title = page.at("header", "pageHeaderRenderer", "pageTitle").asString();
        }
        String author = null;
        String channelId = null;
        long videoCount = 0;
        long viewCount = 0;
        for (JsonNode area : Arrays.asList(page.get("header"), page.get("sidebar"))) {
            for (String text : Renderers.strings(area)) {
                Matcher matcher = COUNT.matcher(text.trim());
                if (matcher.matches()) {
                    long value = Renderers.parseCount(matcher.group(1));
                    if (matcher.group(2).toLowerCase().startsWith("video") && videoCount == 0) {
                        videoCount = value;
                    } else if (matcher.group(2).toLowerCase().startsWith("view") && viewCount == 0) {
                        viewCount = value;
                    }
                }
            }
            if (author == null) {
                String[] owner = Renderers.owner(area);
                author = owner[0];
                channelId = owner[1];
            }
        }
        return new PlaylistDetails(playlistId, title, metadata.get("description").asString(), author, channelId, videoCount, viewCount);
    }

    private static String errorAlert(JsonNode page) {
        for (JsonNode alert : page.findAll("alertRenderer")) {
            if ("ERROR".equals(alert.get("type").asString())) {
                String text = Texts.text(alert.get("text"));
                return text != null ? text : "unknown error";
            }
        }
        return null;
    }

    /** {@code null} for items that are not playable videos, such as playlist links or removed entries. */
    static PlaylistVideo video(JsonNode item, int index) {
        JsonNode lockup = item.get("lockupViewModel");
        if (lockup.isPresent()) {
            if (!"LOCKUP_CONTENT_TYPE_VIDEO".equals(lockup.get("contentType").asString())) {
                return null;
            }
            String videoId = lockup.get("contentId").asString();
            if (videoId == null || !VIDEO_ID.matcher(videoId).matches()) {
                return null;
            }
            JsonNode meta = lockup.at("metadata", "lockupMetadataViewModel");
            String[] owner = Renderers.owner(meta);
            String duration = lockup.findFirst("thumbnailBadgeViewModel").get("text").asString();
            return new PlaylistVideo(videoId, Texts.text(meta.get("title")), owner[0], owner[1], Renderers.parseDuration(duration),
                    index, Renderers.thumbnails(lockup.at("contentImage", "thumbnailViewModel", "image", "sources")));
        }
        JsonNode legacy = item.get("playlistVideoRenderer");
        String videoId = legacy.get("videoId").asString();
        if (videoId == null || !VIDEO_ID.matcher(videoId).matches()) {
            return null;
        }
        return new PlaylistVideo(videoId, Texts.text(legacy.get("title")), Texts.text(legacy.get("shortBylineText")),
                legacy.get("shortBylineText").findFirst("browseId").asString(), legacy.get("lengthSeconds").asLong(0),
                index, Renderers.thumbnails(legacy.at("thumbnail", "thumbnails")));
    }

    private static YoutubeException failure(String message, List<String> attempts, Exception cause) {
        if (cause instanceof HttpStatusException) {
            return new HttpException(((HttpStatusException) cause).status(), message + ": " + String.join("; ", attempts), cause);
        }
        return new YoutubeException(message, attempts, cause);
    }

    /** Walks the pages of one playlist; each iterator starts again at the first page. */
    private final class Pages implements Iterator<PlaylistVideo> {
        private final String playlistId;
        private final Deque<JsonNode> buffer;
        private final Set<String> seenTokens = new HashSet<>();
        private String token;
        private PlaylistVideo next;
        private int index;

        Pages(String playlistId, List<JsonNode> firstItems, String firstToken) {
            this.playlistId = playlistId;
            this.buffer = new ArrayDeque<>(firstItems);
            this.token = firstToken;
        }

        @Override
        public boolean hasNext() {
            while (next == null) {
                if (buffer.isEmpty() && !loadNextPage()) {
                    return false;
                }
                JsonNode item = buffer.poll();
                if (item != null) {
                    next = video(item, index + 1);
                    if (next != null) {
                        index++;
                    }
                }
            }
            return true;
        }

        @Override
        public PlaylistVideo next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            PlaylistVideo video = next;
            next = null;
            return video;
        }

        private boolean loadNextPage() {
            while (token != null && seenTokens.add(token)) {
                JsonNode page;
                try {
                    page = innertube.continuation("browse", token);
                } catch (IOException | JsonException e) {
                    throw failure("Could not load the next page of playlist " + playlistId, Collections.singletonList(String.valueOf(e.getMessage())), e);
                }
                JsonNode area = page.get("onResponseReceivedActions").or(page.get("continuationContents"));
                token = Renderers.continuationToken(area);
                buffer.addAll(Renderers.collect(area, ITEM_KEYS));
                if (!buffer.isEmpty()) {
                    return true;
                }
            }
            return false;
        }
    }
}
