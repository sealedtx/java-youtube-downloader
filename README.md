java-youtube-downloader
============

[![](https://jitpack.io/v/sealedtx/java-youtube-downloader.svg)](https://jitpack.io/#sealedtx/java-youtube-downloader)

Simple java parser for receiving youtube video meta info and download videos and audio of all available formats.

**Notice**: Youtube API does not support video download. In faсt it is prohibited - [Terms of Service - II. Prohibitions](https://developers.google.com/youtube/terms/api-services-terms-of-service). 

This project is used only for educational purposes.

Usage
-------

```java

// parsing data
String videoId = "YOUR_VIDEO_ID"; // https://www.youtube.com/watch?v=abc12345 <---- this is VIDEO_ID 
YoutubeVideo video = YoutubeDownloader.getVideo(videoId);

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
Optional<Format> formatByItag = video.findFormatByItag(136); 
if (formatByItag.isPresent()) {
    Format it = formatByItag.get();
    System.out.println(it.url());
}

// downloading
File outputDir = new File("my_videos");
video.download(videoFormats.get(0), outputDir);

// async downloading

video.downloadAsync(videoFormats.get(0), outputDir new YoutubeDownloader.DownloadCallback() {
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
  <version>1.0.2</version>
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
  implementation 'com.github.sealedtx:java-youtube-downloader:1.0.2'
}
```
