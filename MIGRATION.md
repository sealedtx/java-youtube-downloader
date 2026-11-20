# Migrating from 3.x to 4.0

4.0 is a rewrite: YouTube had changed enough that 3.x no longer worked, and the old API mixed requests, callbacks
and futures in one type. The ideas stay the same; most calls get shorter.

Requirements: **Java 11** (3.x: Java 8). Dependencies: only `slf4j-api` (3.x also pulled fastjson and logback).

## Entry point and configuration

| 3.x | 4.0 |
|---|---|
| `new YoutubeDownloader()` | `YoutubeDownloader.create()` |
| `new YoutubeDownloader(new Config.Builder()...build())` | `YoutubeDownloader.builder()...build()` |
| `Config.Builder.maxRetries(n)` | `builder().retries(n)` (default 2; I/O errors, timeouts, 429, 5xx) |
| `Config.Builder.proxy(host, port[, user, pass])` | `builder().proxy(host, port[, user, pass])` (no global `Authenticator` any more) |
| `Config.Builder.executorService(es)` | `builder().executor(es)` |
| `Config.Builder.header(k, v)` | your own `HttpTransport` (see README) |
| `Config.Builder.enableCompression(b)` | always on in the default transport |
| `downloader.getConfig().setXxx(...)` | immutable; make a variant with `yt.toBuilder()...build()` |
| `ClientType`, `Clients.setHighestPriorityClientType(...)` | `ClientProfile` / `ClientProfiles`, order via `builder().clients(List)` |
| `Base64Encoder.setInstance(...)` | gone (uses `java.util.Base64`) |

## Requests and responses

| 3.x | 4.0 |
|---|---|
| `downloader.getVideoInfo(new RequestVideoInfo(id)).data()` | `yt.videoInfo(idOrUrl)` (any video URL works) |
| `request.async()` + `response.data()` | `yt.videoInfoAsync(id)` returns a `CompletableFuture` |
| `request.callback(new YoutubeCallback<>() {...})` | `future.whenComplete((result, error) -> ...)` |
| `response.ok()`, `response.error()`, `response.status()` | sync calls return the result or throw; futures complete with it |
| `response.cancel()` | `future.cancel(true)` |
| `request.clientType(ClientType.X)` | `yt.toBuilder().clients(List.of(profile)).build()` |
| `request.proxy(...)`, `request.maxRetries(...)` per request | a variant via `yt.toBuilder()` |

Errors are unchecked now:

| 3.x | 4.0 |
|---|---|
| `YoutubeException` (checked) inside `Response.error()` | `YoutubeException` (unchecked) and subclasses |
| `YoutubeException.BadPageException` | `VideoUnavailableException`, `PlaylistUnavailableException`, `BotCheckException`; pages YouTube changed give partial results with `warnings()` instead |
| `YoutubeException.DownloadException` | `HttpException`, `DownloadException` |
| `YoutubeException.CipherException` | gone; unsolvable formats are skipped and listed in `VideoInfo.warnings()` |

Every exception has `attempts()` with the reason per client profile.

## Video info and formats

| 3.x | 4.0 |
|---|---|
| `video.details()` | `video.details()` (`VideoDetails`: `title()`, `author()`, `lengthSeconds()`, `viewCount()`, ...) |
| `details.thumbnails()` (`List<String>`) | `details.thumbnails()` (`List<Thumbnail>` with url, width, height) |
| `details.liveUrl()` | `video.hlsManifestUrl()` |
| `details.averageRating()` | gone (YouTube no longer sends it) |
| `video.formats()` (`List<Format>`) | `video.formats()` (`Formats`; `.list()` for the plain list) |
| `video.videoWithAudioFormats()` | `formats.muxed()` |
| `video.videoFormats()` | `formats.videoOnly()` (or `filter(Format::hasVideo)` for all with video) |
| `video.audioFormats()` | `formats.audioOnly()` |
| `video.bestVideoWithAudioFormat()` / `bestVideoFormat()` / `bestAudioFormat()` | `formats.bestMuxed()` / `bestVideo()` / `bestAudio()` (`Optional`) |
| `video.findFormatByItag(18)` (nullable) | `formats.byItag(18)` (`Optional`) |
| `video.findFormats(new Filter<Format>() {...})` | `formats.filter(f -> ...)` |
| `VideoFormat`, `AudioFormat`, `VideoWithAudioFormat` | one `Format`; `hasVideo()`, `hasAudio()` |
| `format.itag()` (`Itag`, `unknown` for new ones) | `format.itag()` (`int`); `format.knownItag()` (`Itag` or `null`) |
| `format.duration()` | `format.durationMillis()` |
| `format.extension()` (class with constants) | `format.extension()` (enum `Extension`) |
| `video.subtitlesInfo()` | `video.subtitles()` (`SubtitleTrack`) |

Numbers that YouTube did not send are 0 instead of `null`.

## Downloads

| 3.x | 4.0 |
|---|---|
| `new RequestVideoFileDownload(format).saveTo(dir).renameTo(name)` | `yt.download(format, dir.resolve(FileNames.of(name, format)))` |
| `.overwriteIfExists(true)` | `DownloadOptions.defaults().overwrite(true)` |
| `.overwriteIfExists(false)` adds `(1)` to the name | fails with `DownloadException` if the file exists |
| `YoutubeProgressCallback.onDownloading(percent)` | `DownloadOptions.onProgress(p -> p.percent())` |
| `new RequestVideoStreamDownload(format, os)` | `yt.download(format, os, DownloadOptions.defaults())` |
| `.async()` | `yt.downloadAsync(format, file, options)` |

## Playlists and channels

| 3.x | 4.0 |
|---|---|
| `getPlaylistInfo(new RequestPlaylistInfo(id)).data()` | `yt.playlist(idOrUrl)` |
| `playlistInfo.videos()` (all, loaded up front) | `playlist.videos()` (lazy) or `playlist.allVideos()` |
| `PlaylistVideoDetails` | `PlaylistVideo` |
| `getChannelUploads(new RequestChannelUploads(id))` | `yt.channelUploads(idHandleOrUrl)` (`@handle` works) |

## Search

| 3.x | 4.0 |
|---|---|
| `new RequestSearchResult("q").type(TypeField.VIDEO).sortBy(...)` | `SearchQuery.of("q").filter(TypeField.VIDEO).sortBy(...)` |
| `.forceExactQuery(true)` | `.exact(true)` |
| `.during(...)`, `.uploadedThis(...)`, `.match(...)`, `.format(...)` | all through `.filter(...)` |
| `downloader.search(request).data()` | `yt.search(query)` or `yt.search("text")` |
| `downloader.searchContinuation(new RequestSearchContinuation(result))` | `result.nextPage()` |
| `result.hasContinuation()` | `result.hasNextPage()` |
| `result.autoCorrectedQuery()`, `result.isAutoCorrected()` | `result.correctedQuery()` (`null` if not corrected) |
| `result.suggestion()`, `RequestSearchable` | gone; use `correctedQuery()` or `refinements()` with a new `SearchQuery` |
| `SearchResultVideoDetails`, `...ChannelDetails`, `...PlaylistDetails` | `SearchVideo`, `SearchChannel`, `SearchPlaylist` |
| `result.shelves()`, `SearchResultShelf` | shelf items are listed in place in `items()` |

## Subtitles

| 3.x | 4.0 |
|---|---|
| `getSubtitlesInfo(new RequestSubtitlesInfo(id))` | `yt.videoInfo(id).subtitles()` (the old list endpoint is gone at YouTube) |
| `new RequestSubtitlesDownload(info).formatTo(Extension.JSON3)` + `downloadSubtitle(...)` | `yt.subtitles(track, SubtitleFormat.JSON3)` |
| `.translateTo("uk")` | `yt.subtitles(track, format, "uk")` (YouTube currently refuses with HTTP 429) |
| `request.getDownloadUrl()` | `track.url(format)` |

## New in 4.0

- `EjsSolver` / `ChallengeSolver`: YouTube's JS challenges, the `tv_samsung` profile and "made for kids" videos.
- `YoutubeCookies`: logged-in requests, age-restricted videos.
- `HttpTransport`: plug in your own HTTP stack.
- `VideoInfo.warnings()` and `YoutubeException.attempts()`: what was skipped or tried, and why.
