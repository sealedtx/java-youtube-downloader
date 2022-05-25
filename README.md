java-youtube-downloader
============

[![](https://jitpack.io/v/sealedtx/java-youtube-downloader.svg)](https://jitpack.io/#sealedtx/java-youtube-downloader)

Simple java parser for retrieving youtube video metadata.

Library is **not stable**, because Youtube often changes web structure of its pages. I don't use this library regularly to find the errors. Thats why errors are fixed as soon as someone finds it and opens an issue. Feel free to report an error or sumbit a PR.

**WARNING**: Youtube API does not support a video download. In fact, it is prohibited - [Terms of Service - II. Prohibitions](https://developers.google.com/youtube/terms/api-services-terms-of-service).
<br>**WARNING**: Downloading videos may violate copyrights!
<br><br>This project is only for educational purposes. I urge not to use this project to violate any laws.

Usage
-------

### Configuration
```java
// init downloader with default config
YoutubeDownloader downloader = new YoutubeDownloader();
// or with custom config
Config config = new Config.Builder()
    .executorService(executorService) // for async requests, default Executors.newCachedThreadPool()
    .maxRetries(1) // retry on failure, default 0
    .header("Accept-language", "en-US,en;") // extra request header
    .proxy("192.168.0.1", 2005)
    .proxyCredentialsManager(proxyCredentials) // default ProxyCredentialsImpl
    .proxy("192.168.0.1", 2005, "login", "pass")
    .build();
YoutubeDownloader downloader = new YoutubeDownloader(config);

// or configure after init
Config config = downloader.getConfig();
config.setMaxRetries(0);
```

### Request
```java
// each request accepts optional params that will override global configuration
Request request = new Request(...)
        .maxRetries(...) 
        .proxy(...) 
        .header(...)
        .callback(...) // add callback for async processing
        .async(); // make request async
```

### Response
```java
Response<T> response = downloader.get...(request)

// get response status one of [downloading, completed, canceled, error]
ResponseStatus status = response.status();

// get reponse data 
// NOTE: will block current thread until completion if request is async        
T data = response.data(); 
// or get with timeout, may throw TimeoutException
T data = response.data(1, TimeUnit.SECONDS);

// cancel if request is async
boolean canceled = response.cancel();        

// get response error if request finished exceptionally
// NOTE: will block current thread until completion if request is async        
Throwable error = response.error();

// check if request finished successfully
// NOTE: will block current thread until completion if request is async        
boolean ok = response.ok();
```

### VideoInfo
```java
String videoId = "abc12345"; // for url https://www.youtube.com/watch?v=abc12345

// sync parsing
RequestVideoInfo request = new RequestVideoInfo(videoId);
Response<VideoInfo> response = downloader.getVideoInfo(request);
VideoInfo video = response.data();

// async parsing
RequestVideoInfo request = new RequestVideoInfo(videoId)
        .callback(new YoutubeCallback<VideoInfo>() {
            @Override
            public void onFinished(VideoInfo videoInfo) {
                System.out.println("Finished parsing");
            }
    
            @Override
            public void onError(Throwable throwable) {
                System.out.println("Error: " + throwable.getMessage());
            }
        })
        .async();
Response<VideoInfo> response = downloader.getVideoInfo(request);
VideoInfo video = response.data(); // will block thread

// video details
VideoDetails details = video.details();
System.out.println(details.title());
System.out.println(details.viewCount());
details.thumbnails().forEach(image -> System.out.println("Thumbnail: " + image));

// HLS url only for live videos and streams
if (video.details().isLive()) {
    System.out.println("Live Stream HLS URL: " + video.details().liveUrl());
}
        
// get videos formats only with audio
List<VideoWithAudioFormat> videoWithAudioFormats = video.videoWithAudioFormats();
videoWithAudioFormats.forEach(it -> {
    System.out.println(it.audioQuality() + ", " + it.videoQuality() + " : " + it.url());
});

// get all videos formats (may contain better quality but without audio) 
List<VideoFormat> videoFormats = video.videoFormats();
videoFormats.forEach(it -> {
    System.out.println(it.videoQuality() + " : " + it.url());
});

// get audio formats
List<AudioFormat> audioFormats = video.audioFormats();
audioFormats.forEach(it -> {
    System.out.println(it.audioQuality() + " : " + it.url());
});

// get best format
video.bestVideoWithAudioFormat();
video.bestVideoFormat();
video.bestAudioFormat();

// filtering formats
List<Format> formats = video.findFormats(new Filter<Format>() {
    @Override
    public boolean test(Format format) {
        return format.extension() == Extension.WEBM;
    }
});

// itags can be found here - https://gist.github.com/sidneys/7095afe4da4ae58694d128b1034e01e2
Format formatByItag = video.findFormatByItag(18); // return null if not found
if (formatByItag != null) {
    System.out.println(formatByItag.url());
}
```

### Downloading video
```java
File outputDir = new File("my_videos");
Format format = videoFormats.get(0);

// sync downloading
RequestVideoFileDownload request = new RequestVideoFileDownload(format)
    // optional params    
    .saveTo(outputDir) // by default "videos" directory
    .renameTo("video") // by default file name will be same as video title on youtube
    .overwriteIfExists(true); // if false and file with such name already exits sufix will be added video(1).mp4
Response<File> response = downloader.downloadVideoFile(request);
File data = response.data();

// async downloading with callback
RequestVideoFileDownload request = new RequestVideoFileDownload(format)
    .callback(new YoutubeProgressCallback<File>() {
        @Override
        public void onDownloading(int progress) {
            System.out.printf("Downloaded %d%%\n", progress);
        }
    
        @Override
        public void onFinished(File videoInfo) {
            System.out.println("Finished file: " + videoInfo);
        }
    
        @Override
        public void onError(Throwable throwable) {
            System.out.println("Error: " + throwable.getLocalizedMessage());
        }
    })
    .async();
Response<File> response = downloader.downloadVideoFile(request);
File data = response.data(); // will block current thread

// async downloading without callback
RequestVideoFileDownload request = new RequestVideoFileDownload(format).async();
Response<File> response = downloader.downloadVideoFile(request);
File data = response.data(20, TimeUnit.SECONDS); // will block current thread and may throw TimeoutExeption

// download in-memory to OutputStream
OutputStream os = new ByteArrayOutputStream();
RequestVideoStreamDownload request = new RequestVideoStreamDownload(format, os);
Response<Void> response = downloader.downloadVideoStream(request);
```

### Subtitles
```java
// you can get subtitles from video captions if you have already parsed video info
List<SubtitlesInfo> subtitlesInfo = video.subtitles(); // NOTE: includes auto-generated
// if you don't need video info, but just subtitles make this request instead
Response<List<SubtitlesInfo>> response = downloader.getSubtitlesInfo(new RequestSubtitlesInfo(videoId)); // NOTE: does not include auto-generated
List<SubtitlesInfo> subtitlesInfo = response.data();

for (SubtitlesInfo info : subtitles) {
    RequestSubtitlesDownload request = new RequestSubtitlesDownload(info)
            // optional
            .formatTo(Extension.JSON3)
            .translateTo("uk");
    // sync download
    Response<String> response = downloader.downloadSubtitle(request);
    String subtitlesString = response.data();

    // async download
    RequestSubtitlesDownload request = new RequestSubtitlesDownload(info)
            .callback(...) // optional
            .async();
    Response<String> response = downloader.downloadSubtitle(request);
    String subtitlesString = response.data(); // will block current thread

    // to download using external download manager
    String downloadUrl = request.getDownloadUrl();
}
```

### Playlists
```java
String playlistId = "abc12345"; // for url https://www.youtube.com/playlist?list=abc12345
RequestPlaylistInfo request = new RequestPlaylistInfo(playlistId);
Response<PlaylistInfo> response = downloader.getPlaylistInfo(request);
PlaylistInfo playlistInfo = response.data();

// playlist details
PlaylistDetails details = playlistInfo.details();
System.out.println(details.title());
System.out.println(details.videoCount());

// get video details
PlaylistVideoDetails videoDetails = playlistInfo.videos().get(0);
System.out.println(videoDetails.videoId());
System.out.println(videoDetails.title());
System.out.println(videoDetails.index());
```

### Channel uploads
```java
String channelId = "abc12345";  // for url https://www.youtube.com/channel/abc12345
// or 
String channelId = "someName";  // for url https://www.youtube.com/c/someName
RequestChannelUploads request = new RequestChannelUploads(channelId);
Response<PlaylistInfo> response = downloader.getChannelUploads(request);
PlaylistInfo playlistInfo = response.data();
```

### Search
```java
RequestSearchResult request = new RequestSearchResult("search query")
    // filters
    .type(TypeField.VIDEO)                 // Videos only
    .format(FormatField._3D,
        FormatField.HD)                    // 3D HD videos
    .match(FeatureField.SUBTITLES)         // with subtitles
    .during(DurationField.OVER_20_MINUTES) // more than 20 minutes videos
    .uploadedThis(UploadDateField.MONTH)   // uploaded this month

    // other parameters
    .forceExactQuery(true)                 // avoid auto correction
    .sortBy(SortField.VIEW_COUNT);         // results sorted by view count
// or
RequestSearchResult request = new RequestSearchResult("search query")
    .filter(
        TypeField.VIDEO,
        FormatField.HD,
        (...)
        UploadDateField.MONTH);

SearchResult result = downloader.search(request).data();

// retrieve next result (20 items max per continuation)
if (result.hasContinuation()) {
    RequestSearchContinuation nextRequest = new RequestSearchContinuation(result);
    SearchResult continuation = downloader.searchContinuation(nextRequest).data();
}

// a query is suggested, get its result
if (result.suggestion() != null) {
    System.out.println(result.suggestion().query()); // suggested query
    RequestSearchable suggestedRequest = new RequestSearchable(result.suggestion());
    SearchResult suggestedResult = downloader.search(suggestedRequest).data();
}

// query refinements
if (result.refinements() != null) {
    System.out.println(result.refinements().get(0).query()); // refinement query
    RequestSearchable refinedRequest = new RequestSearchable(result.refinements().get(0));
    SearchResult refinedResult = downloader.search(refinedRequest).data();
}

// the query has been auto corrected, force original query
if (result.isAutoCorrected()) {
	System.out.println(result.autoCorrectedQuery()); // corrected query
	SearchResult forcedResult = downloader.search(request.forceExactQuery(true)).data();    
}

// details
System.out.println(result.estimatedResults());

// items, 20 max per result (+ possible shelves on first result)
List<SearchResultItem> items = result.items();
List<SearchResultVideoDetails> videos = result.videos();
List<SearchResultChannelDetails> channels = result.channels();
List<SearchResultPlaylistDetails> playlists = result.playlists();
List<SearchResultShelf> shelves = result.shelves();

// item cast
SearchResultItem item = items.get(0);
switch (item.type()) {
case VIDEO:
    System.out.println(item.asVideo().description());
    break;
case SHELF:
    for (SearchResultVideoDetails video : item.asShelf().videos()) {
        System.out.println(video.author());
    }
    break;
(...)
}

// Base 64 (optional) : use another base 64 encoder for search parameters

// Classic JDK and Android API >= 26
Base64Encoder.setInstance(bytes -> Base64.getUrlEncoder().encodeToString(bytes));

// Android API < 26
Base64Encoder.setInstance(new Base64Encoder() {
    @Override
    public String encodeToString(byte[] bytes) {
        return Base64.encodeToString(bytes, Base64.URL_SAFE);
    }
};
```

Include
-------

### Maven

```xml
<repositories>
  <repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
  </repository>
</repositories>
```
```xml
<dependency>
  <groupId>com.github.sealedtx</groupId>
  <artifactId>java-youtube-downloader</artifactId>
  <version>3.1.0</version>
</dependency>
```

### Gradle

```gradle
allprojects {
  repositories {
    ...
    maven { url 'https://jitpack.io' }
  }
}
```
```gradle 
dependencies {
  implementation 'com.github.sealedtx:java-youtube-downloader:3.1.0'
}
```
### Android

```gradle
android {
  ...
  compileOptions {
    sourceCompatibility JavaVersion.VERSION_1_8
    targetCompatibility JavaVersion.VERSION_1_8
  }
  // For Kotlin projects
  kotlinOptions {
    jvmTarget = "1.8"
  }
}
```