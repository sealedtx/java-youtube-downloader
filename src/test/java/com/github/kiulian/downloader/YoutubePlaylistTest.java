package com.github.kiulian.downloader;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;

import com.github.kiulian.downloader.model.playlist.PlaylistDetails;
import com.github.kiulian.downloader.model.playlist.PlaylistVideo;
import com.github.kiulian.downloader.model.playlist.YoutubePlaylist;

public abstract class YoutubePlaylistTest extends TestUtils {

    // Bruce Lee - 10 videos - stable
    protected static final String BRUCE_PLAYLIST_ID = "PLC3w7RzH8Yf9Zhgk43XX2w_HEDNptS-Ca";

    // Chuck Norris - 10 videos - stable
    protected static final String CHUCK_PLAYLIST_ID = "PLllnWjUgIDSOANnVoE8QTnKmTxYA0R4Ro";

    // Lord Of The Rings Complete - 210 videos - stable
    protected static final String LOTR_PLAYLIST_ID = "PL924DFB59EB36FA1A";

    // Electronic Music - > 2800 videos - unstable
    protected static final String ELECTRO_PLAYLIST_ID = "PLr0CT5anc-eu8rS9n93DcmIA9Ms0gfZEe";

    // Live playlist (from root live channel) - 100 videos - unstable
    protected static final String LIVE_PLAYLIST_ID = "PLU12uITxBEPH7J8GtRoVmrPLrucyX_hn5";

    protected YoutubeDownloader downloader;

    @BeforeEach
    void initDownloader() {
        this.downloader = new YoutubeDownloader();
    }

    protected YoutubePlaylist getPlaylist(String playlistId) throws YoutubeException {
        YoutubePlaylist playlist = downloader.getPlaylist(playlistId);
        PlaylistDetails details = playlist.details();
        assertNotNull(details, "playlist details should not be null: " + playlistId);
        assertEquals(playlistId, details.playlistId(), "playlistId should be " + playlistId);
        assertTrue(details.videoCount() > 0, "playlist should have at least 1 video");
        assertTrue(details.viewCount() > 0, "video should have at least 1 view");
        return playlist;
    }

    protected static PlaylistVideo getVideo(YoutubePlaylist playlist, int index) {
        PlaylistVideo video = playlist.findVideoByIndex(index);
        assertNotNull(video, "findVideoByIndex: " + index + " should return not null video");
        assertEquals(index, video.details().index(), "index should be " + index);
        return video;
    }
}
