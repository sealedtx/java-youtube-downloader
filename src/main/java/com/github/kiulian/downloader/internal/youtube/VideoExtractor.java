package com.github.kiulian.downloader.internal.youtube;

import com.github.kiulian.downloader.YoutubeException;
import com.github.kiulian.downloader.YoutubeException.BotCheckException;
import com.github.kiulian.downloader.YoutubeException.HttpException;
import com.github.kiulian.downloader.YoutubeException.VideoUnavailableException;
import com.github.kiulian.downloader.client.ClientProfile;
import com.github.kiulian.downloader.internal.http.HttpStatusException;
import com.github.kiulian.downloader.internal.json.JsonException;
import com.github.kiulian.downloader.model.videos.VideoDetails;
import com.github.kiulian.downloader.model.videos.VideoInfo;
import com.github.kiulian.downloader.model.videos.formats.Format;
import com.github.kiulian.downloader.model.videos.formats.Formats;

import java.io.IOException;
import com.github.kiulian.downloader.internal.youtube.PlayerScripts.PlayerScript;
import com.github.kiulian.downloader.js.ChallengeSolutions;
import com.github.kiulian.downloader.js.ChallengeSolver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
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
    private final PlayerScripts playerScripts;
    private final ChallengeSolver solver;

    public VideoExtractor(Innertube innertube, List<ClientProfile> clients) {
        this(innertube, clients, null, null);
    }

    /** @param solver {@code null} to skip clients that need the JS player */
    public VideoExtractor(Innertube innertube, List<ClientProfile> clients, PlayerScripts playerScripts, ChallengeSolver solver) {
        if (clients.isEmpty()) {
            throw new IllegalArgumentException("at least one client profile is needed");
        }
        this.innertube = innertube;
        this.clients = new ArrayList<>(clients);
        this.playerScripts = playerScripts;
        this.solver = solver;
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
            if (client.requiresJsPlayer() && solver == null) {
                attempts.add(client.name() + ": skipped, needs a challenge solver (for example EjsSolver.node())");
                continue;
            }
            PlayerResponse player;
            PlayerScript script = null;
            try {
                if (client.requiresJsPlayer()) {
                    script = playerScripts.current();
                    player = new PlayerResponse(innertube.player(client, videoId, script.signatureTimestamp));
                } else {
                    player = new PlayerResponse(innertube.player(client, videoId));
                }
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
                VideoInfo info = toVideoInfo(player, videoId, client, warnings, script);
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
                    partial = toVideoInfo(player, videoId, client, new ArrayList<>(attempts), script);
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

    private VideoInfo toVideoInfo(PlayerResponse player, String videoId, ClientProfile client, List<String> warnings, PlayerScript script) {
        boolean live = player.details(videoId).isLive();
        List<Format> formats;
        if (script == null) {
            formats = player.formats(client.name(), warnings);
        } else {
            long now = System.currentTimeMillis();
            Set<String> n = new HashSet<>();
            Set<String> signatures = new HashSet<>();
            player.collectChallenges(n, signatures);
            ChallengeSolutions solutions;
            try {
                solutions = solver.solve(script.id, script.code, n, signatures);
            } catch (IOException | RuntimeException e) {
                warnings.add(client.name() + ": challenge solver failed (" + e.getMessage() + ")");
                solutions = new ChallengeSolutions(Collections.emptyMap(), Collections.emptyMap());
            }
            formats = player.formats(client.name(), warnings, solutions, player.availableAtMillis(now));
        }
        if (live && !formats.isEmpty()) {
            warnings.add(client.name() + ": live stream, formats left out; use hlsManifestUrl()");
            formats = new ArrayList<>();
        }
        VideoDetails details = player.details(videoId);
        if (player.lacksMetadata()) {
            try {
                details = player.details(videoId, innertube.next(videoId));
            } catch (IOException | RuntimeException e) {
                warnings.add(client.name() + ": metadata incomplete, the watch page data failed (" + e.getMessage() + ")");
            }
        }
        return new VideoInfo(details, new Formats(formats), player.subtitles(),
                player.hlsManifestUrl(), player.dashManifestUrl(), client.name(), warnings);
    }
}
