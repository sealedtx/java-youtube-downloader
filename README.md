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

```java
// ------ INIT DOWNLOADER --------
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

// ------ REQUESTS --------
// each request accepts optional params that will override global config settings for current reequest
Request request = new Request(...)
        .maxRetries(...) 
        .proxy(...) 
        .header(...)
        .callback(...) // add callback for async processing
        .async(); // make request async     

// ------ VIDEO INFO --------
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

File outputDir = new File("my_videos");
Format format = videoFormats.get(0);

// sync downloading
RequestVideoDownload request = new RequestVideoDownload(format)
    // optional params    
    .saveTo(outputDir) // by default "videos" directory
    .renameTo("video") // by default file name will be same as video title on youtube
    .overwriteIfExists(true); // if false and file with such name already exits sufix will be added video(1).mp4
Response<File> response = downloader.downloadVideo(request);
File data = response.data();

// async downloading with callback
RequestVideoDownload request = new RequestVideoDownload(format)
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
Response<File> response = downloader.downloadVideo(request);
File data = response.data(); // will block current thread

// async downloading without callback
RequestVideoDownload request = new RequestVideoDownload(format).async();
Response<File> response = downloader.downloadVideo(request);
File data = response.data(20, TimeUnit.SECONDS); // will block current thread and may throw TimeoutExeption

// cancel downloading
response.cancel();

// live videos and streams
if (video.details().isLive()) {
    System.out.println("Live Stream HLS URL: " + video.details().liveUrl());
}

// ------ SUBTITLES --------
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

// ------ PLAYLISTS --------
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

// ------ CHANNEL UPLOADS --------
Response<PlaylistInfo> response = downloader.getChannelUploads(new RequestChannelUploads(channelId));
PlaylistInfo playlistInfo = response.data();
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
  <version>2.5.2</version>
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
  implementation 'com.github.sealedtx:java-youtube-downloader:2.5.2'
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