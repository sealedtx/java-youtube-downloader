package com.github.kiulian.downloader;

import com.github.kiulian.downloader.client.ClientProfile;
import com.github.kiulian.downloader.client.ClientProfiles;
import com.github.kiulian.downloader.download.DownloadOptions;
import com.github.kiulian.downloader.http.HttpTransport;
import com.github.kiulian.downloader.http.UrlConnectionTransport;
import com.github.kiulian.downloader.internal.Ids;
import com.github.kiulian.downloader.internal.download.FormatDownloader;
import com.github.kiulian.downloader.internal.http.Http;
import com.github.kiulian.downloader.internal.http.RetryPolicy;
import com.github.kiulian.downloader.internal.youtube.Innertube;
import com.github.kiulian.downloader.internal.youtube.PlaylistExtractor;
import com.github.kiulian.downloader.internal.youtube.SearchExtractor;
import com.github.kiulian.downloader.internal.youtube.SubtitleDownloader;
import com.github.kiulian.downloader.model.subtitles.SubtitleFormat;
import com.github.kiulian.downloader.model.subtitles.SubtitleTrack;
import com.github.kiulian.downloader.model.search.SearchQuery;
import com.github.kiulian.downloader.model.search.SearchResult;
import com.github.kiulian.downloader.internal.youtube.VideoExtractor;
import com.github.kiulian.downloader.model.playlist.Playlist;
import com.github.kiulian.downloader.model.videos.VideoInfo;
import com.github.kiulian.downloader.model.videos.formats.Format;

import java.io.OutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Entry point of the library. Thread-safe; create one and reuse it.
 *
 * <pre>{@code
 * YoutubeDownloader yt = YoutubeDownloader.create();
 * VideoInfo video = yt.videoInfo("https://youtu.be/jNQXAC9IVRw");
 * Format audio = video.formats().bestAudio().orElseThrow();
 * }</pre>
 *
 * Errors are unchecked {@link YoutubeException}s; async methods complete exceptionally with them.
 */
public final class YoutubeDownloader implements AutoCloseable {

    private final Builder settings;
    private final VideoExtractor videos;
    private final PlaylistExtractor playlists;
    private final SearchExtractor searches;
    private final SubtitleDownloader subtitles;
    private final FormatDownloader downloads;
    private ExecutorService ownExecutor;

    private YoutubeDownloader(Builder builder) {
        this.settings = builder.copy();
        RetryPolicy retryPolicy = RetryPolicy.retries(builder.retries);
        Http http = new Http(builder.transport(), retryPolicy);
        Innertube innertube = new Innertube(http, builder.webClient);
        this.videos = new VideoExtractor(innertube, builder.clients);
        this.playlists = new PlaylistExtractor(innertube);
        this.searches = new SearchExtractor(innertube);
        this.subtitles = new SubtitleDownloader(http);
        Map<String, String> userAgents = new HashMap<>();
        for (ClientProfile client : builder.clients) {
            userAgents.put(client.name(), client.userAgent());
        }
        this.downloads = new FormatDownloader(http, retryPolicy, userAgents, Innertube.WEB_USER_AGENT);
    }

    /** Downloader with default settings. */
    public static YoutubeDownloader create() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    /** A builder with the settings of this downloader, to make a variant (for example with another proxy). */
    public Builder toBuilder() {
        return settings.copy();
    }

    /**
     * Metadata, formats and subtitle tracks of a video.
     *
     * @param videoIdOrUrl an 11-character id or any YouTube video URL
     * @throws IllegalArgumentException if it is neither
     * @throws YoutubeException.VideoUnavailableException if YouTube says the video cannot be played
     * @throws YoutubeException.BotCheckException if every client got a bot check
     */
    public VideoInfo videoInfo(String videoIdOrUrl) {
        return videos.videoInfo(Ids.videoId(videoIdOrUrl));
    }

    /** {@link #videoInfo(String)} on the executor. */
    public CompletableFuture<VideoInfo> videoInfoAsync(String videoIdOrUrl) {
        String videoId = Ids.videoId(videoIdOrUrl);
        return async(() -> videos.videoInfo(videoId));
    }

    /**
     * A playlist; its videos are loaded page by page while you iterate.
     *
     * @param playlistIdOrUrl a playlist id or any YouTube URL with a {@code list} parameter
     * @throws IllegalArgumentException if it is neither
     * @throws YoutubeException.PlaylistUnavailableException if the playlist does not exist or is private
     */
    public Playlist playlist(String playlistIdOrUrl) {
        return playlists.playlist(Ids.playlistId(playlistIdOrUrl));
    }

    /**
     * The uploads of a channel as a playlist.
     *
     * @param channel channel id ({@code UC...}), handle ({@code @NASA}) or channel URL ({@code /channel/}, {@code /@},
     *                {@code /c/}, {@code /user/})
     * @throws YoutubeException.ChannelUnavailableException if YouTube does not know the channel
     */
    public Playlist channelUploads(String channel) {
        return playlists.channelUploads(channel);
    }

    /** Search with YouTube's default settings. */
    public SearchResult search(String text) {
        return search(SearchQuery.of(text));
    }

    /**
     * One page of search results; {@link SearchResult#nextPage()} loads the next one.
     *
     * <pre>{@code
     * yt.search(SearchQuery.of("nasa").filter(TypeField.VIDEO, DurationField.UNDER_4_MINUTES).sortBy(SortField.VIEW_COUNT))
     * }</pre>
     */
    public SearchResult search(SearchQuery query) {
        return searches.search(query);
    }

    /**
     * The text of a subtitle track, for example {@code yt.subtitles(video.subtitles().get(0), SubtitleFormat.SRT)}.
     */
    public String subtitles(SubtitleTrack track, SubtitleFormat format) {
        return subtitles.text(track, format, null);
    }

    /**
     * The text of a subtitle track, machine-translated by YouTube to {@code translateTo} (a language code such as
     * {@code uk}). Note: since 2026-11 YouTube refuses translations for the default clients with HTTP 429.
     */
    public String subtitles(SubtitleTrack track, SubtitleFormat format, String translateTo) {
        return subtitles.text(track, format, Objects.requireNonNull(translateTo, "translateTo"));
    }

    /** Downloads the format to {@code file} with default options. */
    public Path download(Format format, Path file) {
        return download(format, file, DownloadOptions.defaults());
    }

    /**
     * Downloads the format to {@code file}. The data goes to {@code file + ".part"} first and is moved into place
     * when complete, so a failed or cancelled download never leaves a broken file. A name for the file:
     * {@link com.github.kiulian.downloader.download.FileNames#of(String, Format)}.
     *
     * @throws YoutubeException.DownloadException on HTTP errors, an expired URL, a wrong size or if the file exists
     */
    public Path download(Format format, Path file, DownloadOptions options) {
        return downloads.toFile(format, file, options);
    }

    /** Downloads the format into the stream; the stream is not closed. On failure it may hold part of the data. */
    public void download(Format format, OutputStream out, DownloadOptions options) {
        downloads.toStream(format, out, options);
    }

    /** {@link #download(Format, Path, DownloadOptions)} on the executor. {@code cancel(true)} stops it. */
    public CompletableFuture<Path> downloadAsync(Format format, Path file, DownloadOptions options) {
        return async(() -> downloads.toFile(format, file, options));
    }

    private <T> CompletableFuture<T> async(Supplier<T> work) {
        Task<T> task = new Task<>();
        executor().execute(() -> {
            if (task.isDone()) {
                return;
            }
            task.worker = Thread.currentThread();
            try {
                task.complete(work.get());
            } catch (Throwable e) {
                task.completeExceptionally(e);
            } finally {
                task.worker = null;
                Thread.interrupted(); // leave a clean pool thread after a cancel
            }
        });
        return task;
    }

    /** A future whose {@code cancel} interrupts the running work, which plain CompletableFuture does not. */
    private static final class Task<T> extends CompletableFuture<T> {
        volatile Thread worker;

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            boolean cancelled = super.cancel(mayInterruptIfRunning);
            Thread running = worker;
            if (cancelled && running != null) {
                running.interrupt();
            }
            return cancelled;
        }
    }

    /** Stops the executor this downloader created for async calls; a custom executor is left alone. */
    @Override
    public synchronized void close() {
        if (ownExecutor != null) {
            ownExecutor.shutdown();
            ownExecutor = null;
        }
    }

    private synchronized Executor executor() {
        if (settings.executor != null) {
            return settings.executor;
        }
        if (ownExecutor == null) {
            AtomicInteger number = new AtomicInteger();
            ownExecutor = Executors.newCachedThreadPool(task -> {
                Thread thread = new Thread(task, "yt-downloader-" + number.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            });
        }
        return ownExecutor;
    }

    public static final class Builder {
        private HttpTransport transport;
        private String proxyHost;
        private int proxyPort;
        private String proxyUser;
        private String proxyPassword;
        private int retries = 2;
        private List<ClientProfile> clients = ClientProfiles.defaults();
        private ClientProfile webClient = ClientProfiles.WEB;
        private Executor executor;

        private Builder() {
        }

        /** Your own HTTP stack. Proxy settings of this builder then do not apply. */
        public Builder transport(HttpTransport transport) {
            this.transport = Objects.requireNonNull(transport, "transport");
            return this;
        }

        /** HTTP proxy for the default transport. */
        public Builder proxy(String host, int port) {
            this.proxyHost = Objects.requireNonNull(host, "host");
            this.proxyPort = port;
            return this;
        }

        /** HTTP proxy with credentials for the default transport; they are sent to the proxy only. */
        public Builder proxy(String host, int port, String user, String password) {
            proxy(host, port);
            this.proxyUser = Objects.requireNonNull(user, "user");
            this.proxyPassword = Objects.requireNonNull(password, "password");
            return this;
        }

        /** How often a failed request is sent again (I/O errors, timeouts, 429 and 5xx). Default 2. */
        public Builder retries(int retries) {
            if (retries < 0) {
                throw new IllegalArgumentException("retries must not be negative");
            }
            this.retries = retries;
            return this;
        }

        /** Client profiles to try, in order. Default {@link ClientProfiles#defaults()}. */
        public Builder clients(List<ClientProfile> clients) {
            if (clients.isEmpty()) {
                throw new IllegalArgumentException("at least one client profile is needed");
            }
            this.clients = Collections.unmodifiableList(new ArrayList<>(clients));
            return this;
        }

        /** Client profile for playlists, channels and search. Default {@link ClientProfiles#WEB}. */
        public Builder webClient(ClientProfile webClient) {
            this.webClient = Objects.requireNonNull(webClient, "webClient");
            return this;
        }

        /** Executor for async methods. Default: a daemon thread pool owned by the downloader. */
        public Builder executor(Executor executor) {
            this.executor = Objects.requireNonNull(executor, "executor");
            return this;
        }

        public YoutubeDownloader build() {
            if (transport != null && proxyHost != null) {
                throw new IllegalStateException("proxy settings apply only to the default transport; configure the proxy in your transport");
            }
            return new YoutubeDownloader(this);
        }

        private HttpTransport transport() {
            if (transport != null) {
                return transport;
            }
            UrlConnectionTransport.Builder builder = UrlConnectionTransport.builder();
            if (proxyHost != null) {
                builder.proxy(proxyHost, proxyPort);
            }
            if (proxyUser != null) {
                builder.proxyCredentials(proxyUser, proxyPassword);
            }
            return builder.build();
        }

        private Builder copy() {
            Builder copy = new Builder();
            copy.transport = transport;
            copy.proxyHost = proxyHost;
            copy.proxyPort = proxyPort;
            copy.proxyUser = proxyUser;
            copy.proxyPassword = proxyPassword;
            copy.retries = retries;
            copy.clients = clients;
            copy.webClient = webClient;
            copy.executor = executor;
            return copy;
        }
    }
}
