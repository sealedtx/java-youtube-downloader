java-youtube-downloader
============

[![](https://jitpack.io/v/sealedtx/java-youtube-downloader.svg)](https://jitpack.io/#sealedtx/java-youtube-downloader)

Java library to get YouTube video metadata, formats, playlists, channel uploads, search results and subtitles, and
to download videos and audio. Java 11+, no dependencies besides `slf4j-api`.

YouTube changes its internals often. The library is built to cope: it tries several app identities ("client
profiles") in order, skips what it cannot read instead of failing, and tells you why (`warnings()`, `attempts()`).
Client profiles are plain data, so when YouTube blocks one you can update it yourself without waiting for a release.

**WARNING**: The YouTube terms of service do not allow downloading videos
([Terms of Service - II. Prohibitions](https://developers.google.com/youtube/terms/api-services-terms-of-service)).
Downloading videos may violate copyrights. This project is for educational purposes only; do not use it to violate any laws.

## What works

| Feature | Status | Notes |
|---|---|---|
| Video info: metadata, formats, subtitle tracks | works | up to 2160p; live streams give an HLS manifest |
| Download (file or stream) | works | chunked range requests, resume after broken connections, progress, cancel |
| Playlists, channel uploads | works | lazy paging; channel by id, `@handle`, `/c/`, `/user/` URL |
| Search | works | filters, sort, pages, shorts, spelling correction |
| Subtitles | works | VTT, SRT, TTML, JSON3, SRV1-3 |
| "Made for kids" videos | with a JS solver | needs Node.js 22+, see [JS challenges](#js-challenges) |
| Age-restricted videos | with cookies and a JS solver | see [Logged-in requests](#logged-in-requests-cookies) |
| Machine-translated subtitles | not working | YouTube answers HTTP 429 since 2026-11, also when logged in |
| Formats of the web client | not supported | the web client gets SABR streaming only |

## Install

Maven:
```xml
<repositories>
  <repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
  </repository>
</repositories>

<dependency>
  <groupId>com.github.sealedtx</groupId>
  <artifactId>java-youtube-downloader</artifactId>
  <version>4.0.0-rc1</version>
</dependency>
```

Gradle:
```gradle
repositories {
    maven { url 'https://jitpack.io' }
}
dependencies {
    implementation 'com.github.sealedtx:java-youtube-downloader:4.0.0-rc1'
}
```

The library logs through SLF4J; add any SLF4J backend if you want its logs.

## Quick start

```java
YoutubeDownloader yt = YoutubeDownloader.create();

VideoInfo video = yt.videoInfo("https://youtu.be/jNQXAC9IVRw");    // an id or any YouTube video URL
System.out.println(video.details().title());                       // Me at the zoo

Format audio = video.formats().bestAudio().orElseThrow();
Path file = yt.download(audio, Paths.get("music", FileNames.of(video.details().title(), audio)));
```

`YoutubeDownloader` is thread-safe; create one and reuse it. `YoutubeException` exceptions are unchecked .

## Video info

```java
VideoInfo video = yt.videoInfo("jNQXAC9IVRw");

VideoDetails details = video.details();
details.title(); details.author(); details.channelId(); details.lengthSeconds(); details.viewCount();
details.description(); details.keywords(); details.thumbnails(); details.isLive(); details.isLiveContent();

video.hlsManifestUrl();   // live streams
video.subtitles();        // subtitle tracks, see below
video.client();           // which client profile answered
video.warnings();         // what was skipped and why, for example "skipped 14 formats without URL (SABR only)"
```

Formats:
```java
Formats formats = video.formats();

formats.bestVideo();      // highest resolution, then frame rate, then bitrate (Optional)
formats.bestAudio();      // main audio track first, without volume compression, then bitrate
formats.bestMuxed();      // video and audio in one file; YouTube offers few of these, often only 360p
formats.videoOnly(); formats.audioOnly(); formats.muxed();
formats.byItag(140);
formats.filter(f -> f.extension() == Extension.WEBM && f.height() >= 1080);

Format f = formats.bestVideo().orElseThrow();
f.itag(); f.mimeType(); f.codecs(); f.extension(); f.width(); f.height(); f.fps(); f.qualityLabel();
f.bitrate(); f.contentLength(); f.audioTrackName(); f.isDefaultAudioTrack(); f.url();
```

**Note**: This library does not mux, most high-quality formats hold video *or* audio. 
Combine them with any muxing tool (for example `ffmpeg`) if you need one file.

Format URLs expire after some hours.

## Download

```java
yt.download(format, Paths.get("video.mp4"));

yt.download(format, Paths.get("video.mp4"), DownloadOptions.defaults()
        .overwrite(true)                         // default: fail if the file exists
        .chunkSize(5 * 1024 * 1024)               // default 10 MB per range request
        .onProgress(p -> System.out.println(p.percent() + "% " + p.bytesPerSecond() + " B/s")));

yt.download(format, outputStream, DownloadOptions.defaults());   // the stream is not closed

CompletableFuture<Path> future = yt.downloadAsync(format, file, DownloadOptions.defaults());
future.cancel(true);                             // stops the download
```

Data goes to `<file>.part` and is moved into place when complete, so a failed or cancelled download never leaves a
broken file. After a broken connection the download continues at the first missing byte.

## Async

```java
yt.videoInfoAsync(id).thenAccept(video -> ...);
```
Async calls run on a daemon thread pool owned by the downloader (`close()` stops it), or on your executor:
`YoutubeDownloader.builder().executor(myExecutor)`.

## Playlists and channels

```java
Playlist playlist = yt.playlist("https://www.youtube.com/playlist?list=PL2aBZuCeDwlRbJhDIkKXilZRz9ur4VVAA");
playlist.details().title(); playlist.details().author(); playlist.details().videoCount();

for (PlaylistVideo v : playlist.videos()) {      // pages are loaded while you iterate
    System.out.println(v.index() + ". " + v.title() + " (" + v.lengthSeconds() + " s)");
}
playlist.stream().limit(50).forEach(...);        // loads only the first page
List<PlaylistVideo> all = playlist.allVideos();

Playlist uploads = yt.channelUploads("@NASA");   // also "UC...", ".../channel/UC...", ".../c/NASA", ".../user/..."
```

Get formats of a playlist video with `yt.videoInfo(v.videoId())`.

## Search

```java
SearchResult result = yt.search("nasa artemis");
result.videos();         // videos and shorts (isShort()), with author, channel, length, views
result.channels();       // with handle and subscriber count
result.playlists();
result.estimatedResults();

SearchResult next = result.nextPage();            // check hasNextPage() first

yt.search(SearchQuery.of("nasa")
        .filter(TypeField.VIDEO, DurationField.UNDER_4_MINUTES, UploadDateField.WEEK)
        .sortBy(SortField.VIEW_COUNT));

SearchResult typo = yt.search("rick astly never gona give");
typo.correctedQuery();                           // "rick astley never gonna give"
yt.search(typo.query().exact(true));             // search exactly the given text
```

Filters: `TypeField`, `UploadDateField`, `DurationField`, `FeatureField`, `FormatField`; sort: `SortField`.

## Subtitles

```java
SubtitleTrack track = video.subtitles().get(0);
track.languageCode(); track.name(); track.isAutoGenerated(); track.isTranslatable();

String vtt = yt.subtitles(track, SubtitleFormat.VTT);       // also SRT, TTML, JSON3, SRV1, SRV2, SRV3
String url = track.url(SubtitleFormat.SRT);                  // for another download tool
```
`yt.subtitles(track, format, "uk")` asks YouTube for a machine translation; YouTube currently answers it with HTTP 429.

## Errors

| Exception | Meaning | What to do |
|---|---|---|
| `VideoUnavailableException` | removed, private, region-locked, age-restricted; `reason()` is YouTube's text | nothing, or cookies for age-restricted videos |
| `PlaylistUnavailableException` | the playlist does not exist or is private | |
| `ChannelUnavailableException` | unknown channel | check the handle or URL |
| `BotCheckException` | "Sign in to confirm you're not a bot" for every client | update the library or the client profiles, wait, or use another network |
| `HttpException` | an HTTP error after the retries; `status()` | |
| `DownloadException` | download failed, URL expired, file exists, size wrong | get the video info again for expired URLs |
| `YoutubeException` | base class; anything else, such as network errors | |

Every exception has `attempts()`: one line per client that was tried and why it failed, for example
`[visionos: LOGIN_REQUIRED (Sign in to confirm your age), android_vr: ...]`. Please include it in bug reports.

## Configuration

```java
YoutubeDownloader yt = YoutubeDownloader.builder()
        .proxy("127.0.0.1", 8080)                // or proxy(host, port, user, password)
        .retries(3)                              // I/O errors, timeouts, 429 and 5xx; default 2
        .clients(List.of(ClientProfiles.VISIONOS, ClientProfiles.ANDROID_VR))
        .challengeSolver(EjsSolver.node())       // see JS challenges
        .cookies(YoutubeCookies.fromNetscapeFile(Paths.get("cookies.txt")))
        .executor(myExecutor)
        .build();

YoutubeDownloader other = yt.toBuilder().proxy("10.0.0.1", 3128).build();   // a variant
```

Proxy credentials are sent to the proxy only and never set globally. For Basic auth with `https` through a proxy
the JDK needs `-Djdk.http.auth.tunneling.disabledSchemes=` (empty), or use your own transport.

### Client profiles

A client profile is one YouTube app identity: name, version, user agent and device fields. Default order:
`visionos`, `android_vr`, then `tv_samsung` (only with a JS solver). When YouTube starts to reject a profile
(bot check, HTTP 400), compare with [yt-dlp's clients](https://github.com/yt-dlp/yt-dlp/blob/master/yt_dlp/extractor/youtube/_base.py)
and update it yourself:

```java
ClientProfile visionos = ClientProfiles.VISIONOS.toBuilder().version("1.03").build();
YoutubeDownloader yt = YoutubeDownloader.builder().clients(List.of(visionos, ClientProfiles.ANDROID_VR)).build();
```

### Your own HTTP stack

Implement `HttpTransport` (one method) to use OkHttp, custom proxies or logging. The library handles retries and
status codes on top of it. A sketch for OkHttp:

```java
HttpTransport okhttp = request -> {
    Request.Builder b = new Request.Builder().url(request.url());
    request.headers().forEach(b::header);
    byte[] body = request.body();
    b.method(request.method(), body == null ? null : RequestBody.create(body, null));   // OkHttp 4
    Response r = client.newCall(b.build()).execute();
    return new HttpTransport.Response() {
        public int status() { return r.code(); }
        public String header(String name) { return r.header(name); }
        public InputStream body() { return r.body().byteStream(); }
        public void close() { r.close(); }
    };
};
YoutubeDownloader yt = YoutubeDownloader.builder().transport(okhttp).build();
```

The default `UrlConnectionTransport` has a 10 s connect and a 30 s read timeout
(`UrlConnectionTransport.builder().readTimeout(...)` to change them) and sends only `http`/`https` requests.

## JS challenges

Some clients get stream URLs protected by challenges that only YouTube's own player JavaScript can solve. The
library runs [yt-dlp's EJS scripts](https://github.com/yt-dlp/ejs) (bundled, hash-checked, Unlicense) in an
external JavaScript runtime:

```java
EjsSolver solver = EjsSolver.detect();            // Node.js 22+ on the PATH, or null
YoutubeDownloader yt = YoutubeDownloader.builder().challengeSolver(solver).build();
```

- `EjsSolver.node()` runs `node --permission -`: YouTube's code gets no file system or process access.
- `EjsSolver.command(List.of("deno", "run", "--no-remote", "--no-npm", "--no-config", "-"))` for another runtime.
- This enables the `tv_samsung` profile: a second, independent way to get formats, and "made for kids" videos.
- Its ciphered URLs work only after the pre-roll ad (a few seconds); `download(...)` waits for that by itself
  (`Format.availableAtMillis()`).
- JVM only. On Android implement `ChallengeSolver`, for example with a WebView.

## Logged-in requests (cookies)

Cookies of a logged-in account unlock age-restricted videos. They are sent only to youtube.com and only by
profiles that support them (`tv_samsung`, `web`), so a JS solver is needed as well:

```java
YoutubeDownloader yt = YoutubeDownloader.builder()
        .cookies(YoutubeCookies.fromNetscapeFile(Paths.get("cookies.txt")))   // or fromHeader("a=1; b=2")
        .challengeSolver(EjsSolver.node())
        .build();
```

Export `cookies.txt` with a browser extension (for example "Get cookies.txt LOCALLY") from a private window: log in,
open `https://www.youtube.com/robots.txt` in a new tab, close the YouTube tab, export, then close the private window
so the browser does not rotate the cookies.

**Cookies are a password**: whoever has them is logged in as you; keep the file private and never commit it.
YouTube may block accounts used for automated downloads, so use a secondary account.

## Android

The library works on Android with a recent Android Gradle Plugin (Java 11 bytecode). For proxies with passwords
and for the JS challenges, provide your own `HttpTransport` (for example OkHttp) and `ChallengeSolver`.

## Development

See [CONTRIBUTING.md](docs/CONTRIBUTING.md):
