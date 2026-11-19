package com.github.kiulian.downloader.internal.youtube;

import com.github.kiulian.downloader.internal.json.JsonNode;
import com.github.kiulian.downloader.model.Thumbnail;
import com.github.kiulian.downloader.model.subtitles.SubtitleTrack;
import com.github.kiulian.downloader.model.videos.VideoDetails;
import com.github.kiulian.downloader.model.videos.formats.Format;
import com.github.kiulian.downloader.model.videos.quality.AudioQuality;
import com.github.kiulian.downloader.model.videos.quality.VideoQuality;

import com.github.kiulian.downloader.js.ChallengeSolutions;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;
import java.util.Locale;

/**
 * Reads the answer of {@code /youtubei/v1/player}. Every field is optional; a broken format is skipped, never fatal.
 */
final class PlayerResponse {

    private static final Pattern N_PARAM = Pattern.compile("([?&]n=)([^&]*)");

    private final JsonNode json;

    PlayerResponse(JsonNode json) {
        this.json = json;
    }

    /** {@code OK}, {@code LOGIN_REQUIRED}, {@code UNPLAYABLE}, {@code ERROR}, {@code LIVE_STREAM_OFFLINE}, ... */
    String status() {
        return json.at("playabilityStatus", "status").asString("");
    }

    String reason() {
        JsonNode status = json.get("playabilityStatus");
        String reason = status.get("reason").asString();
        if (reason == null) {
            reason = Texts.text(status.at("errorScreen", "playerErrorMessageRenderer", "reason"));
        }
        return reason != null ? reason : "no reason given";
    }

    boolean isBotCheck() {
        return status().equals("LOGIN_REQUIRED") && reason().toLowerCase(Locale.ROOT).contains("bot");
    }

    VideoDetails details(String videoId) {
        return details(videoId, JsonNode.missing());
    }

    /** True if the answer lacks the basic metadata (seen for age-restricted videos through the TV client). */
    boolean lacksMetadata() {
        return json.at("videoDetails", "title").asString() == null;
    }

    /**
     * Metadata; fields the player answer lacks are taken from an innertube {@code next} answer (the watch page
     * data), if given. Only the video's own primary and secondary info is read, never recommendations.
     */
    VideoDetails details(String videoId, JsonNode next) {
        JsonNode d = json.get("videoDetails");
        JsonNode primary = next.findFirst("videoPrimaryInfoRenderer");
        JsonNode secondary = next.findFirst("videoSecondaryInfoRenderer");
        String title = d.get("title").asString();
        if (title == null) {
            title = Texts.text(primary.get("title"));
        }
        String author = d.get("author").asString();
        if (author == null) {
            author = Texts.text(secondary.at("owner", "videoOwnerRenderer", "title"));
        }
        long views = d.get("viewCount").asLong(-1);
        if (views < 0) {
            views = Renderers.parseCount(Texts.text(primary.at("viewCount", "videoViewCountRenderer", "viewCount")));
        }
        String description = d.get("shortDescription").asString();
        if (description == null) {
            description = Texts.text(secondary.get("attributedDescription"));
        }
        List<String> keywords = new ArrayList<>();
        for (JsonNode keyword : d.get("keywords").items()) {
            if (keyword.isString()) {
                keywords.add(keyword.asString());
            }
        }
        return VideoDetails.builder(d.get("videoId").asString(videoId))
                .title(title)
                .author(author)
                .channelId(d.get("channelId").asString())
                .description(description)
                .keywords(keywords)
                .lengthSeconds(d.get("lengthSeconds").asLong(0))
                .viewCount(views)
                .thumbnails(thumbnails(d.at("thumbnail", "thumbnails")))
                .live(d.get("isLive").asBoolean(false))
                .liveContent(d.get("isLiveContent").asBoolean(false))
                .privateVideo(d.get("isPrivate").asBoolean(false))
                .build();
    }

    String hlsManifestUrl() {
        return trustedUrl(json.at("streamingData", "hlsManifestUrl").asString());
    }

    String dashManifestUrl() {
        return trustedUrl(json.at("streamingData", "dashManifestUrl").asString());
    }

    /** Formats that can be downloaded with plain requests; what was skipped goes into {@code warnings}. */
    List<Format> formats(String client, List<String> warnings) {
        return formats(client, warnings, null, 0);
    }

    /**
     * Formats, with ciphered signatures and {@code n} values replaced by {@code solutions} (if not {@code null});
     * formats whose challenges are unsolved are skipped. Every format gets {@code availableAtMillis}.
     */
    List<Format> formats(String client, List<String> warnings, ChallengeSolutions solutions, long availableAtMillis) {
        List<Format> formats = new ArrayList<>();
        int withoutUrl = 0;
        int ciphered = 0;
        int unsolved = 0;
        int untrusted = 0;
        int broken = 0;
        int otf = 0;
        for (String list : new String[]{"formats", "adaptiveFormats"}) {
            boolean adaptive = list.equals("adaptiveFormats");
            for (JsonNode f : json.at("streamingData", list).items()) {
                String url = f.get("url").asString();
                String cipher = f.get("signatureCipher").or(f.get("cipher")).asString();
                if (cipher != null) {
                    if (solutions == null) {
                        ciphered++;
                        continue;
                    }
                    Map<String, String> parts = query(cipher);
                    String signature = parts.get("s") == null ? null : solutions.signature(parts.get("s"));
                    if (signature == null || parts.get("url") == null) {
                        unsolved++;
                        continue;
                    }
                    url = parts.get("url") + "&" + parts.getOrDefault("sp", "signature") + "=" + encode(signature);
                }
                if (url == null) {
                    withoutUrl++;
                    continue;
                }
                if (solutions != null) {
                    String n = nParam(url);
                    if (n != null) {
                        String solved = solutions.n(n);
                        if (solved == null) {
                            unsolved++;
                            continue;
                        }
                        url = N_PARAM.matcher(url).replaceFirst("$1" + Matcher.quoteReplacement(encode(solved)));
                    }
                }
                if (trustedUrl(url) == null) {
                    untrusted++;
                    continue;
                }
                if ("FORMAT_STREAM_TYPE_OTF".equals(f.get("type").asString())) {
                    otf++;
                    continue;
                }
                int itag = f.get("itag").asInt(-1);
                if (itag < 0) {
                    broken++;
                    continue;
                }
                formats.add(format(f, itag, url, adaptive, client, availableAtMillis));
            }
        }
        warn(warnings, client, withoutUrl, "without URL (SABR only)");
        warn(warnings, client, ciphered, "that need a challenge solver");
        warn(warnings, client, unsolved, "whose JS challenges were not solved");
        warn(warnings, client, untrusted, "with URLs outside YouTube hosts");
        warn(warnings, client, otf, "of OTF stream type");
        warn(warnings, client, broken, "without itag");
        return formats;
    }

    /** {@code n} values and ciphered signatures of all formats, to solve them in one go. */
    void collectChallenges(Set<String> n, Set<String> signatures) {
        for (String list : new String[]{"formats", "adaptiveFormats"}) {
            for (JsonNode f : json.at("streamingData", list).items()) {
                String url = f.get("url").asString();
                String cipher = f.get("signatureCipher").or(f.get("cipher")).asString();
                if (cipher != null) {
                    Map<String, String> parts = query(cipher);
                    if (parts.get("s") != null) {
                        signatures.add(parts.get("s"));
                    }
                    url = parts.get("url");
                }
                String value = url == null ? null : nParam(url);
                if (value != null) {
                    n.add(value);
                }
            }
        }
    }

    /**
     * When the stream URLs start to work: after the pre-roll ad can be skipped (YouTube answers 403 before).
     * Same rule as yt-dlp: ads placed at the start, skip offset or else the ad length.
     */
    long availableAtMillis(long nowMillis) {
        long waitMillis = 0;
        List<JsonNode> ads = new ArrayList<>();
        for (JsonNode placement : json.get("adPlacements").items()) {
            if ("AD_PLACEMENT_KIND_START".equals(placement.at("adPlacementRenderer", "config", "adPlacementConfig", "kind").asString())) {
                ads.addAll(placement.findAll("instreamVideoAdRenderer"));
            }
        }
        for (JsonNode slot : json.get("adSlots").items()) {
            if ("SLOT_TRIGGER_EVENT_BEFORE_CONTENT".equals(slot.at("adSlotRenderer", "adSlotMetadata", "triggerEvent").asString())) {
                ads.addAll(slot.findAll("instreamVideoAdRenderer"));
            }
        }
        for (JsonNode ad : ads) {
            long millis = ad.get("skipOffsetMilliseconds").asLong(-1);
            if (millis < 0) {
                String length = query(ad.get("playerVars").asString("")).get("length_seconds");
                millis = length == null ? 0 : (long) (parseDouble(length) * 1000);
            }
            waitMillis = Math.max(waitMillis, millis);
        }
        return waitMillis > 0 ? nowMillis + waitMillis : 0;
    }

    private static String nParam(String url) {
        Matcher matcher = N_PARAM.matcher(url);
        return matcher.find() ? decode(matcher.group(2)) : null;
    }

    private static Map<String, String> query(String query) {
        Map<String, String> values = new HashMap<>();
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                values.put(decode(pair.substring(0, eq)), decode(pair.substring(eq + 1)));
            }
        }
        return values;
    }

    private static String decode(String value) {
        try {
            return URLDecoder.decode(value, "UTF-8");
        } catch (UnsupportedEncodingException | IllegalArgumentException e) {
            return value;
        }
    }

    private static String encode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    List<SubtitleTrack> subtitles() {
        List<SubtitleTrack> tracks = new ArrayList<>();
        for (JsonNode t : json.at("captions", "playerCaptionsTracklistRenderer", "captionTracks").items()) {
            String url = trustedUrl(t.get("baseUrl").asString());
            String language = t.get("languageCode").asString();
            if (url == null || language == null) {
                continue;
            }
            boolean auto = "asr".equals(t.get("kind").asString()) || t.get("vssId").asString("").startsWith("a.");
            tracks.add(new SubtitleTrack(language, Texts.text(t.get("name")), auto, t.get("isTranslatable").asBoolean(false), url));
        }
        return tracks;
    }

    private static Format format(JsonNode f, int itag, String url, boolean adaptive, String client, long availableAtMillis) {
        String mimeType = f.get("mimeType").asString("");
        Format.Builder builder = Format.builder(itag, url)
                .mimeType(mimeType)
                .adaptive(adaptive)
                .bitrate(f.get("bitrate").asInt(0))
                .averageBitrate(f.get("averageBitrate").asInt(0))
                .contentLength(f.get("contentLength").asLong(0))
                .durationMillis(f.get("approxDurationMs").asLong(0))
                .audioSampleRate(f.get("audioSampleRate").asInt(0))
                .audioChannels(f.get("audioChannels").asInt(0))
                .drc(f.get("isDrc").asBoolean(false))
                .client(client)
                .availableAtMillis(availableAtMillis);
        if (mimeType.startsWith("video/")) {
            builder.width(f.get("width").asInt(0))
                    .height(f.get("height").asInt(0))
                    .fps(f.get("fps").asInt(0))
                    .qualityLabel(f.get("qualityLabel").asString())
                    .videoQuality(videoQuality(f.get("quality").asString()));
        }
        String audioQuality = f.get("audioQuality").asString();
        if (audioQuality != null) {
            builder.audioQuality(audioQuality(audioQuality));
        }
        JsonNode track = f.get("audioTrack");
        if (track.isObject()) {
            builder.audioTrack(track.get("id").asString(), track.get("displayName").asString(),
                    track.get("audioIsDefault").asBoolean(false));
        }
        return builder.build();
    }

    private static VideoQuality videoQuality(String value) {
        if (value != null) {
            for (VideoQuality quality : VideoQuality.values()) {
                if (quality.name().equals(value)) {
                    return quality;
                }
            }
        }
        return VideoQuality.unknown;
    }

    // AUDIO_QUALITY_LOW -> low
    private static AudioQuality audioQuality(String value) {
        String name = value.substring(value.lastIndexOf('_') + 1).toLowerCase(Locale.ROOT);
        for (AudioQuality quality : AudioQuality.values()) {
            if (quality.name().equals(name)) {
                return quality;
            }
        }
        return AudioQuality.unknown;
    }

    private static List<Thumbnail> thumbnails(JsonNode list) {
        List<Thumbnail> thumbnails = new ArrayList<>();
        for (JsonNode t : list.items()) {
            String url = t.get("url").asString();
            if (url != null) {
                thumbnails.add(new Thumbnail(url, t.get("width").asInt(0), t.get("height").asInt(0)));
            }
        }
        return thumbnails;
    }

    private static void warn(List<String> warnings, String client, int count, String what) {
        if (count > 0) {
            warnings.add(client + ": skipped " + count + " formats " + what);
        }
    }

    /** The URL if it is https on a YouTube or Google video host, otherwise {@code null}. */
    static String trustedUrl(String url) {
        if (url == null) {
            return null;
        }
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (!"https".equalsIgnoreCase(uri.getScheme()) || host == null) {
                return null;
            }
            host = host.toLowerCase(Locale.ROOT);
            boolean trusted = host.endsWith(".googlevideo.com") || host.equals("youtube.com") || host.endsWith(".youtube.com");
            return trusted ? url : null;
        } catch (URISyntaxException e) {
            return null;
        }
    }
}
