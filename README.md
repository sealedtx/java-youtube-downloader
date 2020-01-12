java-youtube-downloader
============

[![](https://jitpack.io/v/sealedtx/java-youtube-downloader.svg)](https://jitpack.io/#sealedtx/java-youtube-downloader)

Simple java parser for receiving youtube video meta info and download videos and audio of all available formats.

**Since 2.0.0** supports downloading videos with signature.

**Notice**: Youtube API does not support video download. In faсt it is prohibited - [Terms of Service - II. Prohibitions](https://developers.google.com/youtube/terms/api-services-terms-of-service). 

This project is used only for educational purposes.

Usage
-------

```java
// init downloader
YoutubeDownloader downloader = new YoutubeDownloader();
YoutubeDownloader downloader = new YoutubeDownloader(new Parser()); // you can easly implement or extend existing parsing logic 

// parsing data
String videoId = "YOUR_VIDEO_ID"; // https://www.youtube.com/watch?v=abc12345 <---- this is VIDEO_ID
YoutubeVideo video = downloader.getVideo(videoId);

// video details
VideoDetails details = video.details();
System.out.println(details.title());
...
System.out.println(details.viewCount());
details.thumbnails().forEach(image -> System.out.println("Thumbnail: " + image));

// filtering formats
List<VideoFormat> videoFormats = video.findVideoWithQuality(VideoQuality.hd720);
videoFormats.forEach(it -> {
    System.out.println(it.videoQuality() + " : " + it.url());
});

// itags can be found here - https://gist.github.com/sidneys/7095afe4da4ae58694d128b1034e01e2
Format formatByItag = video.findFormatByItag(136); 
if (formatByItag != null) {
    Format it = formatByItag.get();
    System.out.println(it.url());
}

// downloading
File outputDir = new File("my_videos");
video.download(videoFormats.get(0), outputDir);

// async downloading

video.downloadAsync(videoFormats.get(0), outputDir new OnYoutubeDownloadListener() {
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
  <version>2.0.1</version>
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
  
dependencies {
  implementation 'com.github.sealedtx:java-youtube-downloader:2.0.1'
}
```
