package com.github.kiulian.downloader.internal.youtube;

import com.github.kiulian.downloader.internal.http.Http;
import com.github.kiulian.downloader.internal.http.RetryPolicy;
import com.github.kiulian.downloader.internal.json.Json;
import com.github.kiulian.downloader.model.playlist.Playlist;
import com.github.kiulian.downloader.model.playlist.PlaylistVideo;
import com.github.kiulian.downloader.testing.FakeYoutube;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Item shapes and paging on small hand-written browse answers.
 */
class PlaylistExtractorTest {

    @Test
    void readsTheOldPlaylistVideoRenderer() {
        String page = "{\"contents\":{\"list\":[{\"playlistVideoRenderer\":{\"videoId\":\"jNQXAC9IVRw\","
                + "\"title\":{\"runs\":[{\"text\":\"Me at \"},{\"text\":\"the zoo\"}]},\"lengthSeconds\":\"19\","
                + "\"shortBylineText\":{\"runs\":[{\"text\":\"jawed\",\"navigationEndpoint\":{\"browseEndpoint\":{\"browseId\":\"UC4QobU6STFB0P71PMvOGN5A\"}}}]}}}]}}";

        List<PlaylistVideo> videos = extractor((request) -> page).playlist("PL1234567890ab").allVideos();

        assertEquals(1, videos.size());
        assertEquals("Me at the zoo", videos.get(0).title());
        assertEquals("jawed", videos.get(0).author());
        assertEquals("UC4QobU6STFB0P71PMvOGN5A", videos.get(0).channelId());
        assertEquals(19, videos.get(0).lengthSeconds());
    }

    @Test
    void skipsItemsThatAreNotVideos() {
        String page = "{\"contents\":[" + lockup("LOCKUP_CONTENT_TYPE_PLAYLIST", "PL1234567890ab") + ","
                + lockup("LOCKUP_CONTENT_TYPE_VIDEO", "not-an-id") + "," + lockup("LOCKUP_CONTENT_TYPE_VIDEO", "jNQXAC9IVRw") + "]}";

        List<PlaylistVideo> videos = extractor(request -> page).playlist("PL1234567890ab").allVideos();

        assertEquals(1, videos.size());
        assertEquals(1, videos.get(0).index());
    }

    @Test
    void stopsWhenYoutubeRepeatsAContinuationToken() {
        AtomicInteger continuations = new AtomicInteger();
        String page = "{\"contents\":[" + lockup("LOCKUP_CONTENT_TYPE_VIDEO", "jNQXAC9IVRw")
                + ",{\"continuationItemViewModel\":{\"continuationCommand\":{\"token\":\"same\"}}}]}";
        String next = "{\"onResponseReceivedActions\":[{\"appendContinuationItemsAction\":{\"continuationItems\":["
                + lockup("LOCKUP_CONTENT_TYPE_VIDEO", "DFdOcVpRhWI")
                + ",{\"continuationItemViewModel\":{\"continuationCommand\":{\"token\":\"same\"}}}]}}]}";

        Playlist playlist = extractor(request -> {
            boolean isContinuation = Json.parse(new String(request.body(), java.nio.charset.StandardCharsets.UTF_8)).has("continuation");
            if (isContinuation) {
                continuations.incrementAndGet();
            }
            return isContinuation ? next : page;
        }).playlist("PL1234567890ab");

        assertEquals(2, playlist.allVideos().size());
        assertEquals(1, continuations.get());
    }

    @Test
    void emptyPlaylistIsAWarningNotAnError() {
        Playlist playlist = extractor(request -> "{\"contents\":{}}").playlist("PL1234567890ab");

        assertTrue(playlist.allVideos().isEmpty());
        assertEquals(1, playlist.warnings().size());
    }

    @Test
    void parsesDurations() {
        assertEquals(338, PlaylistExtractor.parseDuration("5:38"));
        assertEquals(3723, PlaylistExtractor.parseDuration("1:02:03"));
        assertEquals(0, PlaylistExtractor.parseDuration("LIVE"));
        assertEquals(0, PlaylistExtractor.parseDuration(null));
    }

    private static PlaylistExtractor extractor(FakeYoutube.Router router) {
        return new PlaylistExtractor(new Innertube(new Http(new FakeYoutube(router), RetryPolicy.NONE)));
    }

    private static String lockup(String type, String id) {
        return "{\"lockupViewModel\":{\"contentType\":\"" + type + "\",\"contentId\":\"" + id + "\","
                + "\"metadata\":{\"lockupMetadataViewModel\":{\"title\":{\"content\":\"Video " + id + "\"}}}}}";
    }
}
