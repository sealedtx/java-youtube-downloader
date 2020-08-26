package com.github.kiulian.downloader;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.github.kiulian.downloader.model.Extension;
import com.github.kiulian.downloader.model.formats.Format;
import com.github.kiulian.downloader.model.playlist.PlaylistVideo;
import com.github.kiulian.downloader.model.playlist.YoutubePlaylist;

@DisplayName("Tests downloading youtube playlists")
class YoutubePlaylistDownloader_Tests extends YoutubePlaylistTest {

    private static final File outDir = new File("playlists");

    @AfterEach
    void cleanOutDir() {
        if (outDir.isDirectory()) {
            clean(outDir);
        }
    }

    @Test
    @DisplayName("download video sync should be successful")
    void downloadVideo_Sync_Success() {
        assertDoesNotThrow(() -> {
            YoutubePlaylist playlist = getPlaylist(BRUCE_PLAYLIST_ID);
            int itag = 18;
            List<PlaylistVideo> videos = playlist.findVideosWithItagFormat(itag);
            assertFalse(videos.isEmpty(), "playlist should contains at least one video with itag " + itag);
            
            PlaylistVideo video = videos.get(0);
            Format format = video.findFormatByItag(itag);
            assertNotNull(format, "findFormatByItag should return not null format");
            assertTrue(isReachable(format.url()), "url should be reachable");
            assertDoesNotThrow(() -> {
                // Classic download from video
                File file = video.download(format, outDir);
                assertTrue(outDir.exists(), "output directory should be created");
                assertTrue(file.exists(), "file should be downloaded");

                Extension extension = format.extension();
                assertTrue(file.getName().endsWith(extension.value()), "file name should ends with: " + extension.value());
                assertTrue(file.length() > 0, "file should be not empty");
            });
            
            assertDoesNotThrow(() -> {
                // Download with numbering
                File file = playlist.download(video, format, outDir);
                assertTrue(outDir.exists(), "output directory should be created");
                assertTrue(file.exists(), "file should be downloaded");

                Extension extension = format.extension();
                assertTrue(file.getName().endsWith(extension.value()), "file name should ends with: " + extension.value());
                assertTrue(file.length() > 0, "file should be not empty");
            });

        });
    }

    @Test
    @DisplayName("download playlist sync should be successful")
    void downloadPlaylist_Sync_Success() {
        assertDoesNotThrow(() -> {
            YoutubePlaylist playlist = getPlaylist(CHUCK_PLAYLIST_ID);
            File playlistDir = playlist.playlistDir(outDir);
            int itag = 18;
            int count = playlist.downloadByItag(outDir, itag).size();
            assertTrue(playlistDir.exists(), "playlist output directory should be created");
            // Since results can vary, no reliable count test can be done
            // assertEquals(playlist.details().videoCount(), count);
            System.out.println("Playlist download: " + count + "/" + playlist.details().videoCount());
        });
    }
}
