java-youtube-downloader
============

[![](https://jitpack.io/v/sealedtx/java-youtube-downloader.svg)](https://jitpack.io/#sealedtx/java-youtube-downloader)

Simple java parser for receiving youtube video meta info and download videos and audio of all available formats.

**Since 2.0.0** supports downloading videos with signature.
<br>**Since 2.2.0** supports retrieving HLS url for live videos.
<br>**Since 2.2.4** supports retrieving video subtitles.
<br>**Since 2.3.0** supports retrieving playlists.
<br>**Since 2.4.0** supports cancellation for async video downloading.

**Notice**: Youtube API does not support a video download. In fact, it is prohibited - [Terms of Service - II. Prohibitions](https://developers.google.com/youtube/terms/api-services-terms-of-service). 

This project is used only for educational purposes.

Usage
-------

```java
// init downloader
YoutubeDownloader downloader = new YoutubeDownloader();

// you can easly implement or extend default parsing logic 
YoutubeDownloader downloader = new YoutubeDownloader(new Parser()); 
// or just extend functionality via existing API
// cipher features
downloader.addCipherFunctionPattern(2, "\\b([a-zA-Z0-9$]{2})\\s*=\\s*function\\(\\s*a\\s*\\)\\s*\\{\\s*a\\s*=\\s*a\\.split\\(\\s*\"\"\\s*\\)");
downloader.addCipherFunctionEquivalent("some regex for js function", new CustomJavaFunction());
// extractor features
downloader.setParserRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.121 Safari/537.36");
downloader.setParserRetryOnFailure(1);

// parsing data
String videoId = "abc12345"; // for url https://www.youtube.com/watch?v=abc12345
YoutubeVideo video = downloader.getVideo(videoId);

// video details
VideoDetails details = video.details();
System.out.println(details.title());
...
System.out.println(details.viewCount());
details.thumbnails().forEach(image -> System.out.println("Thumbnail: " + image));

// get videos with audio
List<AudioVideoFormat> videoWithAudioFormats = video.videoWithAudioFormats();
videoWithAudioFormats.forEach(it -> {
    System.out.println(it.audioQuality() + " : " + it.url());
});

// filtering only video formats
List<VideoFormat> videoFormats = video.findVideoWithQuality(VideoQuality.hd720);
videoFormats.forEach(it -> {
    System.out.println(it.videoQuality() + " : " + it.url());
});

// itags can be found here - https://gist.github.com/sidneys/7095afe4da4ae58694d128b1034e01e2
Format formatByItag = video.findFormatByItag(136); 
if (formatByItag != null) {
    System.out.println(formatByItag.url());
}

File outputDir = new File("my_videos");
Format format = videoFormats.get(0);

// sync downloading
File file = video.download(format, outputDir);

// async downloading with callback
Future<File> future = video.downloadAsync(videoFormats.get(0), outputDir, new OnYoutubeDownloadListener() {
    @Override
    public void onDownloading(int progress) {
        System.out.printf("Downloaded %d%%\n", progress);
    }
            
    @Override
    public void onFinished(File file) {
        System.out.println("Finished file: " + file);
    }

    @Override
    public void onError(Throwable throwable) {
        System.out.println("Error: " + throwable.getLocalizedMessage());
    }
});

// async downloading without callback
Future<File> future = video.downloadAsync(format, outputDir);
File file = future.get(5, TimeUnit.SECONDS);

// cancel downloading
future.cancel(true); // true is required to interrupt downloading thread

// live videos and streams
if (video.details().isLive()) {
    System.out.println("Live Stream HLS URL: " + video.details().liveUrl());
}

// naming
// by default file name will be same as video title on youtube, 
// but you can specify output file name
File myAwesomeFile = video.download(format, outputDir, "myAwesomeName");
System.out.println(file.getName()); // myAwesomeName.mp4
// if file with such name already exits sufix will be added myAwesomeFile(1).mp4
// you may disable this feature by passing overwrite flag
File myAwesomeFile = video.download(format, outputDir, "myAwesomeName", true);

// subtitles
// you can get subtitles from video captions if you have already parsed video meta
List<SubtitlesInfo> subtitles = video.subtitles(); // NOTE: includes auto-generated
// if you don't need video meta, but just subtitles use this instead
List<SubtitlesInfo> subtitles = downloader.getVideoSubtitles(videoId); // NOTE: does not include auto-generated

for (SubtitlesInfo info : subtitles) {
    Subtitles subtitles = info.getSubtitles()
             .formatTo(Extension.JSON3)
             .translateTo("uk");
    // sync download
    String subtitlesData = subtitles.download();
    // async download
    Future<String> subtitlesFuture = subtitles.downloadAsync(callback/*optional*/);
    // to download using external download manager
    String downloadUrl = subtitles.getDownloadUrl(); 
}

// playlists

// parsing data
String playlistId = "abc12345"; // for url https://www.youtube.com/playlist?list=abc12345
YoutubePlaylist playlist = downloader.getPlaylist(playlistId);

// playlist details
PlaylistDetails details = playlist.details();
System.out.println(details.title());
...
System.out.println(details.videoCount());

// get video details
PlaylistVideoDetails videoDetails = playlist.videos().get(0);
System.out.println(videoDetails.title());
...
System.out.println(videoDetails.index());

// get video
YoutubeVideo video = downloader.getVideo(videoDetails.videoId());
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
  <version>2.4.1</version>
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
  implementation 'com.github.sealedtx:java-youtube-downloader:2.4.1'
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
