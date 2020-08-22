package com.github.kiulian.downloader;

import static com.github.kiulian.downloader.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.github.kiulian.downloader.model.playlist.PlaylistDetails;
import com.github.kiulian.downloader.model.playlist.PlaylistVideo;
import com.github.kiulian.downloader.model.playlist.YoutubePlaylist;

@DisplayName("Tests extracting metadata from youtube playlists")
public class YoutubePlaylistExtractor_Tests {

    @Test
    @DisplayName("getLongPlaylist should be successful")
    void getLongPlaylist_Success() {
        YoutubeDownloader downloader = new YoutubeDownloader();

        assertDoesNotThrow(() -> {
            YoutubePlaylist playlist = downloader.getPlaylist(LOTR_PLAYLIST_ID);
            testPlaylist(playlist, LOTR_PLAYLIST_ID,
                    "The Lord of the Rings Complete Recordings",
                    "Darkrunn",
                    210);    // stable
            testVideo(getVideo(playlist, "tK_bCeRcGxo", 194), "Howard Shore-The Ruins of Dale", "willburowgh", true);
            testVideo(getVideo(playlist, 207), "Dragon-sickness", "Howard Shore - Topic", true);
        });
    }
    
    @Test
    @DisplayName("getVeryLongPlaylist should be successful")
    void getVeryLongPlaylist_Success() {
        YoutubeDownloader downloader = new YoutubeDownloader();

        assertDoesNotThrow(() -> {
            YoutubePlaylist playlist = downloader.getPlaylist(ELECTRO_PLAYLIST_ID);
            testPlaylist(playlist, ELECTRO_PLAYLIST_ID,
                    "Electronic Music Playlist :D",
                    "NuttyMrBubbles",
                    -1); // 2827 videos, unstable
        });
    }
    
    private static void testPlaylist(YoutubePlaylist playlist, String playlistId, String title, String author, int size) {
        PlaylistDetails details;
        List<PlaylistVideo> videos;
        int videoCount;
        
        details = playlist.details();
        assertNotNull(details, "playlist details should not be null: " + playlistId);
        assertEquals(playlistId, details.playlistId(), "playlistId should be " + playlistId);
        assertEquals(title, details.title(), "title should be " + title);
        assertEquals(author, details.author(), "author should be " + author);
        assertTrue(details.videoCount() > 0, "video count should not be 0");
        if (size < 0) {
            videoCount = details.videoCount();
        } else {
            assertEquals(size, details.videoCount(), "playlist size should be " + size);
            videoCount = size;
        }
        
        videos = playlist.videos();
        assertNotNull(videos, "playlist videos should not be null: " + playlistId);
        assertEquals(videoCount, videos.size(), "size should be " + videoCount);
        
        for (PlaylistVideo video : videos) {
            if (!video.playable()) {
                assertNull(video.author(), "Not playable video should not have an author");
                assertTrue(video.title().equals("[Private video]") || video.title().equals("[Deleted video]"),
                        "Not playable video has a wrong title: " + video.title());
            }
        }
    }
    
    private static void testVideo(PlaylistVideo video, String title, String author, boolean playable) {
        assertEquals(title, video.title(), "title should be " + title);
        assertEquals(author, video.author(), "author should be " + author);
        if (playable) {
            assertTrue(video.playable(), "video should be playable");
        } else {
            assertFalse(video.playable(), "video should not be playable");
        }
    }
    
    private static PlaylistVideo getVideo(YoutubePlaylist playlist, String videoId, int index) {
        PlaylistVideo video = playlist.findVideoById(videoId);
        assertNotNull(video, "findVideoById: " + videoId + " should return not null video");
        assertEquals(videoId, video.videoId(), "video id should be " + videoId);
        if (index > 0) {
            assertEquals(index, video.index(), "video index should be " + index);
        }
        return video;
    }
    
    private static PlaylistVideo getVideo(YoutubePlaylist playlist, int index) {
        PlaylistVideo video = playlist.findVideoByIndex(index);
        assertNotNull(video, "findVideoByIndex: " + index + " should return not null video");
        assertEquals(index, video.index(), "index should be " + index);
        return video;
    }
}
