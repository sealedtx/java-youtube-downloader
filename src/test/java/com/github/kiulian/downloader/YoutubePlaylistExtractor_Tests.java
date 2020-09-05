package com.github.kiulian.downloader;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.github.kiulian.downloader.model.playlist.PlaylistDetails;
import com.github.kiulian.downloader.model.playlist.PlaylistVideoDetails;
import com.github.kiulian.downloader.model.playlist.YoutubePlaylist;

@DisplayName("Tests extracting metadata from youtube playlists")
public class YoutubePlaylistExtractor_Tests extends YoutubePlaylistTest {

    @Test
    @DisplayName("getShortPlaylist should be successful")
    void getShortPlaylist_Success() {
        assertDoesNotThrow(() -> {
            YoutubePlaylist playlist = getPlaylist(BRUCE_PLAYLIST_ID);
            testPlaylist(playlist, BRUCE_PLAYLIST_ID, "Bruce lee", "Tom De Brito", 10);
            testVideo(getVideo(playlist, "xML-j6NsGwM", 1),
                    "legend of bruce lee - Enemy.mp4", "andreaboni1", true);
            testVideo(getVideo(playlist, "DE3er3wDAik", 10),
                    "legend of bruce lee - From Heaven.mp4", "andreaboni1", true);
        });
    }

    @Test
    @DisplayName("getLongPlaylist should be successful")
    void getLongPlaylist_Success() {
        assertDoesNotThrow(() -> {
            YoutubePlaylist playlist = getPlaylist(LOTR_PLAYLIST_ID);
            testPlaylist(playlist, LOTR_PLAYLIST_ID,
                    "The Lord of the Rings Complete Recordings",
                    "Darkrunn",
                    210);    // stable
            testVideo(getVideo(playlist, "tK_bCeRcGxo", 194),
                    "Howard Shore-The Ruins of Dale", "willburowgh", true);
            testVideo(getVideo(playlist, 207),
                    "Dragon-sickness", "Howard Shore - Topic", true);
            
            String author = "willburowgh";
            List<PlaylistVideoDetails> videos = playlist.findVideos(video -> video.author().equals(author));
            assertFalse(videos.isEmpty(), "filtered videos shoud not be empty");
            videos.forEach(video -> {
                assertEquals(author, video.author(), "author should be " + author);
            });
        });
    }

    @Test
    @DisplayName("getVeryLongPlaylist should be successful")
    void getVeryLongPlaylist_Success() {
        assertDoesNotThrow(() -> {
            YoutubePlaylist playlist = getPlaylist(ELECTRO_PLAYLIST_ID);
            testPlaylist(playlist, ELECTRO_PLAYLIST_ID,
                    "Electronic Music Playlist :D",
                    "NuttyMrBubbles",
                    -1); // > 2800 videos, unstable
        });
    }

    @Test
    @DisplayName("getLivePlaylist should be successful")
    void getLivePlaylist() {
        assertDoesNotThrow(() -> {
            YoutubePlaylist playlist = getPlaylist(LIVE_PLAYLIST_ID);
            testPlaylist(playlist, LIVE_PLAYLIST_ID, "Live", "Live", 100);
            int liveCount = 0;
            for (PlaylistVideoDetails video : playlist.videos()) {
                if (video.isLive()) {
                    liveCount ++;
                }
                assertTrue(video.isPlayable(), "live playlist video should be playable");
            }
            int minLiveCount = 90;
            assertTrue(liveCount > minLiveCount, "live playlist should contain at least " + minLiveCount + " live videos");
        });
    }

    @Test
    @DisplayName("getPlaylist should throw exception for unavailable playlist")
    void getPlaylist_Unavailable_ThrowsException() {
        assertThrows(YoutubeException.BadPageException.class, () -> {
            getPlaylist("12345678901");
        });
    }

    private static void testPlaylist(YoutubePlaylist playlist, String playlistId, String title, String author, int size) {
        PlaylistDetails details = playlist.details();
        assertEquals(title, details.title(), "title should be " + title);
        assertEquals(author, details.author(), "author should be " + author);
        
        List<PlaylistVideoDetails> videos = playlist.videos();
        int videoCount;
        if (size < 0) {
            videoCount = details.videoCount();
        } else {
            assertEquals(size, details.videoCount(), "playlist size should be " + size);
            videoCount = size;
        }
        
        assertNotNull(videos, "playlist videos should not be null: " + playlistId);
        assertEquals(videoCount, videos.size(), "size should be " + videoCount);
        
        for (PlaylistVideoDetails video : videos) {
            if (video.lengthSeconds() > 0 && !video.isPlayable()) {
                assertNull(video.author(), "Not playable video should not have an author");
                assertTrue(video.title().equals("[Private video]") || video.title().equals("[Deleted video]"),
                        "Not playable video has a wrong title: " + video.title());
            }
        }
    }

    private static void testVideo(PlaylistVideoDetails video, String title, String author, boolean isPlayable) {
        assertEquals(title, video.title(), "title should be " + title);
        assertEquals(author, video.author(), "author should be " + author);
        if (isPlayable) {
            assertTrue(video.isPlayable(), "video should be playable");
        } else {
            assertFalse(video.isPlayable(), "video should not be playable");
        }
    }

    private static PlaylistVideoDetails getVideo(YoutubePlaylist playlist, String videoId, int index) {
        PlaylistVideoDetails video = playlist.findVideoById(videoId);
        assertNotNull(video, "findVideoById: " + videoId + " should return not null video");
        assertEquals(videoId, video.videoId(), "video id should be " + videoId);
        if (index > 0) {
            assertEquals(index, video.index(), "video index should be " + index);
        }
        return video;
    }
}
