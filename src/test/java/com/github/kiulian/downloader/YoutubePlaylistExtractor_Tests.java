package com.github.kiulian.downloader;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.github.kiulian.downloader.model.Itag;
import com.github.kiulian.downloader.model.formats.Format;
import com.github.kiulian.downloader.model.playlist.PlaylistDetails;
import com.github.kiulian.downloader.model.playlist.PlaylistVideo;
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
            for (PlaylistVideo video : playlist.videos()) {
            	if (video.details().isLive()) {
            		liveCount ++;
            	}
                assertTrue(video.details().isPlayable(), "live playlist video should be playable");
            }
            int minLiveCount = 90;
            assertTrue(liveCount > minLiveCount, "live playlist should contain at least " + minLiveCount + " live videos");
        });
    }

    @Test
    @DisplayName("getPlaylist should throws exception for unavailable video")
    void getPlaylist_Unavailable_ThrowsException() {
        assertThrows(YoutubeException.BadPageException.class, () -> {
            getPlaylist("12345678901");
        });
    }

    @Test
    @DisplayName("available formats should be successful")
    void playlistAvailableItags_Success() {
        assertDoesNotThrow(() -> {
            YoutubePlaylist playlist = getPlaylist(BRUCE_PLAYLIST_ID);
            Set<Itag> availableItags = getAvailableItags(playlist);
            // Results can vary, no count test can be done
            // assertFalse(availableItags.isEmpty(), "no avilable itags for Bruce");
            System.out.println("Available itags for Bruce: " + availableItags.size());
            
            playlist = getPlaylist(CHUCK_PLAYLIST_ID);
            availableItags = getAvailableItags(playlist);
            // Results can vary, no count test can be done
            // assertFalse(availableItags.isEmpty(), "no avilable itags for Bruce");
            System.out.println("Available itags for Chuck: " + availableItags.size());
        });
    }

    private static void testPlaylist(YoutubePlaylist playlist, String playlistId, String title, String author, int size) {
        PlaylistDetails details = playlist.details();
        assertEquals(title, details.title(), "title should be " + title);
        assertEquals(author, details.author(), "author should be " + author);
        
        List<PlaylistVideo> videos = playlist.videos();
        int videoCount;
        if (size < 0) {
            videoCount = details.videoCount();
        } else {
            assertEquals(size, details.videoCount(), "playlist size should be " + size);
            videoCount = size;
        }
        
        assertNotNull(videos, "playlist videos should not be null: " + playlistId);
        assertEquals(videoCount, videos.size(), "size should be " + videoCount);
        
        for (PlaylistVideo video : videos) {
            if (video.details().lengthSeconds() > 0 && !video.details().isPlayable()) {
                assertNull(video.details().author(), "Not playable video should not have an author");
                assertTrue(video.details().title().equals("[Private video]") || video.details().title().equals("[Deleted video]"),
                        "Not playable video has a wrong title: " + video.details().title());
            }
        }
    }

    private static void testVideo(PlaylistVideo video, String title, String author, boolean isPlayable) {
        PlaylistVideoDetails details = video.details();
        assertEquals(title, details.title(), "title should be " + title);
        assertEquals(author, details.author(), "author should be " + author);
        if (isPlayable) {
            assertTrue(details.isPlayable(), "video should be playable");
        } else {
            assertFalse(details.isPlayable(), "video should not be playable");
        }
    }

    private static PlaylistVideo getVideo(YoutubePlaylist playlist, String videoId, int index) {
        PlaylistVideo video = playlist.findVideoById(videoId);
        assertNotNull(video, "findVideoById: " + videoId + " should return not null video");
        PlaylistVideoDetails details = video.details();
        assertEquals(videoId, details.videoId(), "video id should be " + videoId);
        if (index > 0) {
            assertEquals(index, details.index(), "video index should be " + index);
        }
        return video;
    }

    // Gets itags available for all videos
    private static Set<Itag> getAvailableItags(YoutubePlaylist playlist) throws YoutubeException {
        Set<Itag> authorized = new HashSet<>();
        boolean first = true;
        
        for (PlaylistVideo video : playlist.videos()) {
            if (first) {
                for (Format format : video.formats()) {
                    authorized.add(format.itag());
                }
                first = false;
            } else {
                authorized.removeIf(itag -> {
                    for (Format format : video.formats()) {
                        if (format.itag() == itag) {
                            return false;
                        }
                    };
                    return true;
                });
                if (authorized.isEmpty()) {
                    break;
                }
            }
        }
        return authorized;
    }
}
