package com.github.kiulian.downloader.internal.youtube;

import com.github.kiulian.downloader.YoutubeException;
import com.github.kiulian.downloader.YoutubeException.BotCheckException;
import com.github.kiulian.downloader.YoutubeException.HttpException;
import com.github.kiulian.downloader.YoutubeException.VideoUnavailableException;
import com.github.kiulian.downloader.client.ClientProfile;
import com.github.kiulian.downloader.internal.http.HttpStatusException;
import com.github.kiulian.downloader.internal.json.JsonException;
import com.github.kiulian.downloader.model.videos.VideoInfo;
import com.github.kiulian.downloader.model.videos.formats.Format;
import com.github.kiulian.downloader.model.videos.formats.Formats;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Video info: asks the client profiles in order and takes the first answer with downloadable formats.
 * <p>
 * Every failed try is kept as an attempt. If no client gives formats but one gave the metadata, that partial
 * result is returned with warnings; otherwise the most specific error is thrown.
 */
public final class VideoExtractor {

    private final Innertube innertube;
    private final List<ClientProfile> clients;

    public VideoExtractor(Innertube innertube, List<ClientProfile> clients) {
        if (clients.isEmpty()) {
            throw new IllegalArgumentException("at least one client profile is needed");
        }
        this.innertube = innertube;
        this.clients = new ArrayList<>(clients);
    }

    public VideoInfo videoInfo(String videoId) {
        List<String> attempts = new ArrayList<>();
        VideoInfo partial = null;
        String unavailableReason = null;
        boolean botCheck = false;
        HttpStatusException httpError = null;
        IOException ioError = null;
        boolean visitorDataRenewed = false;

        for (int i = 0; i < clients.size(); i++) {
            ClientProfile client = clients.get(i);
            PlayerResponse player;
            try {
                player = new PlayerResponse(innertube.player(client, videoId));
            } catch (HttpStatusException e) {
                attempts.add(client.name() + ": HTTP " + e.status());
                httpError = e;
                continue;
            } catch (IOException e) {
                attempts.add(client.name() + ": " + e);
                ioError = e;
                continue;
            } catch (JsonException e) {
                attempts.add(client.name() + ": invalid JSON (" + e.getMessage() + ")");
                continue;
            }

            if (player.isBotCheck() && !visitorDataRenewed) {
                // the cached session may be the cause; try this client once more with a new one
                attempts.add(client.name() + ": bot check, retrying with new visitor data");
                innertube.resetVisitorData();
                visitorDataRenewed = true;
                i--;
                continue;
            }

            String status = player.status();
            if (status.equals("OK")) {
                List<String> warnings = new ArrayList<>(attempts);
                VideoInfo info = toVideoInfo(player, videoId, client, warnings);
                if (!info.formats().isEmpty() || info.details().isLive()) {
                    return info;
                }
                attempts.add(client.name() + ": no downloadable formats");
                if (partial == null) {
                    partial = info;
                }
            } else if (status.equals("LIVE_STREAM_OFFLINE")) {
                attempts.add(client.name() + ": " + player.reason());
                if (partial == null) {
                    partial = toVideoInfo(player, videoId, client, new ArrayList<>(attempts));
                }
            } else if (player.isBotCheck()) {
                botCheck = true;
                attempts.add(client.name() + ": bot check (" + player.reason() + ")");
            } else {
                unavailableReason = player.reason();
                attempts.add(client.name() + ": " + (status.isEmpty() ? "no status" : status) + " (" + unavailableReason + ")");
            }
        }

        if (partial != null) {
            List<String> warnings = new ArrayList<>(partial.warnings());
            for (String attempt : attempts) {
                if (!warnings.contains(attempt)) {
                    warnings.add(attempt);
                }
            }
            return new VideoInfo(partial.details(), partial.formats(), partial.subtitles(),
                    partial.hlsManifestUrl(), partial.dashManifestUrl(), partial.client(), warnings);
        }
        if (unavailableReason != null) {
            throw new VideoUnavailableException(videoId, unavailableReason, attempts);
        }
        if (botCheck) {
            throw new BotCheckException(videoId, attempts);
        }
        if (httpError != null) {
            throw new HttpException(httpError.status(), "Could not get video info for " + videoId + ": " + httpError.getMessage(), httpError);
        }
        throw new YoutubeException("Could not get video info for " + videoId, attempts, ioError);
    }

    private static VideoInfo toVideoInfo(PlayerResponse player, String videoId, ClientProfile client, List<String> warnings) {
        boolean live = player.details(videoId).isLive();
        List<Format> formats = player.formats(client.name(), warnings);
        if (live && !formats.isEmpty()) {
            warnings.add(client.name() + ": live stream, formats left out; use hlsManifestUrl()");
            formats = new ArrayList<>();
        }
        return new VideoInfo(player.details(videoId), new Formats(formats), player.subtitles(),
                player.hlsManifestUrl(), player.dashManifestUrl(), client.name(), warnings);
    }
}
