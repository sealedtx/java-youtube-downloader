package com.github.kiulian.downloader;

import com.github.kiulian.downloader.YoutubeException.ChannelUnavailableException;
import com.github.kiulian.downloader.YoutubeException.PlaylistUnavailableException;
import com.github.kiulian.downloader.model.playlist.Playlist;
import com.github.kiulian.downloader.model.playlist.PlaylistVideo;
import com.github.kiulian.downloader.testing.Corpus;
import com.github.kiulian.downloader.testing.FakeYoutube;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Playlists and channel uploads on recorded YouTube answers. Re-record with {@code mvn test -Precord}.
 */
class PlaylistFixtureTest {

    private final FakeYoutube youtube = FakeYoutube.fromFixtures();
    private final YoutubeDownloader yt = YoutubeDownloader.builder().transport(youtube).build();

    @Test
    void playlistWithOwner() {
        Playlist playlist = yt.playlist("https://www.youtube.com/playlist?list=" + Corpus.ASK_NASA);

        assertEquals("#AskNASA", playlist.details().title());
        assertEquals("NASA", playlist.details().author());
        assertEquals(Corpus.NASA_CHANNEL_ID, playlist.details().channelId());
        assertEquals(22, playlist.details().videoCount());
        assertTrue(playlist.details().viewCount() > 0);
        assertTrue(playlist.warnings().isEmpty(), playlist.warnings().toString());

        List<PlaylistVideo> videos = playlist.allVideos();
        assertEquals(22, videos.size());
        PlaylistVideo first = videos.get(0);
        assertEquals(1, first.index());
        assertEquals(11, first.videoId().length());
        assertEquals("NASA", first.author());
        assertEquals(Corpus.NASA_CHANNEL_ID, first.channelId());
        assertTrue(first.lengthSeconds() > 0);
        assertFalse(first.thumbnails().isEmpty());
        assertEquals(22, videos.get(21).index());
    }

    @Test
    void iteratingAgainGivesTheSameVideos() {
        Playlist playlist = yt.playlist(Corpus.ASK_NASA);
        assertEquals(ids(playlist.allVideos()), ids(playlist.allVideos()));
    }

    @Test
    void albumWithoutOwner() {
        Playlist album = yt.playlist(Corpus.ALBUM);

        assertNull(album.details().author());
        List<PlaylistVideo> tracks = album.allVideos();
        assertEquals(13, tracks.size());
        assertTrue(tracks.stream().allMatch(t -> t.author() != null), "every track has an artist");
    }

    @Test
    void missingPlaylist() {
        PlaylistUnavailableException error = assertThrows(PlaylistUnavailableException.class, () -> yt.playlist(Corpus.MISSING_PLAYLIST));
        assertEquals("The playlist does not exist.", error.reason());
    }

    @Test
    void channelUploadsLoadPagesOnlyWhenNeeded() {
        Playlist uploads = yt.channelUploads(Corpus.NASA_HANDLE);
        assertEquals("UU" + Corpus.NASA_CHANNEL_ID.substring(2), uploads.details().playlistId());
        assertTrue(uploads.details().videoCount() > 1000);

        List<PlaylistVideo> first150 = uploads.stream().limit(150).collect(Collectors.toList());
        assertEquals(150, first150.size());
        assertEquals(150, new HashSet<>(ids(first150)).size(), "no duplicates across pages");
        for (int i = 0; i < first150.size(); i++) {
            assertEquals(i + 1, first150.get(i).index());
        }
        long continuations = youtube.requests().stream().filter(r -> r.contains("continuation-")).count();
        assertEquals(1, continuations, "150 videos need the first page and one more");
    }

    @Test
    void channelByUrlOrIdGivesTheSameUploads() {
        String expected = "UU" + Corpus.NASA_CHANNEL_ID.substring(2);
        assertEquals(expected, yt.channelUploads("https://www.youtube.com/c/NASA").details().playlistId());
        assertEquals(expected, yt.channelUploads(Corpus.NASA_CHANNEL_ID).details().playlistId());
        assertEquals(expected, yt.channelUploads("https://www.youtube.com/channel/" + Corpus.NASA_CHANNEL_ID + "/videos").details().playlistId());
    }

    @Test
    void unknownChannel() {
        assertThrows(ChannelUnavailableException.class, () -> yt.channelUploads("@noSuchChannelHere1"));
    }

    @Test
    void fallsBackToTheWebPageWhenTheApiFails() {
        FakeYoutube apiDown = new FakeYoutube((FakeYoutube.Router) request -> {
            if (request.url().contains("/youtubei/v1/browse")) {
                throw new IOException("connection reset");
            }
            return FakeYoutube.fixture(FakeYoutube.fixturePath(request));
        });
        Playlist playlist = YoutubeDownloader.builder().transport(apiDown).retries(0).build().playlist(Corpus.ASK_NASA);

        assertEquals("#AskNASA", playlist.details().title());
        assertEquals(22, playlist.stream().limit(22).count());
        assertTrue(playlist.warnings().get(0).startsWith("browse API failed"), playlist.warnings().toString());
    }

    private static List<String> ids(List<PlaylistVideo> videos) {
        return videos.stream().map(PlaylistVideo::videoId).collect(Collectors.toList());
    }
}
