package com.github.kiulian.downloader;

import com.github.kiulian.downloader.model.search.SearchChannel;
import com.github.kiulian.downloader.model.search.SearchPlaylist;
import com.github.kiulian.downloader.model.search.SearchQuery;
import com.github.kiulian.downloader.model.search.SearchResult;
import com.github.kiulian.downloader.model.search.SearchVideo;
import com.github.kiulian.downloader.model.search.field.DurationField;
import com.github.kiulian.downloader.model.search.field.SortField;
import com.github.kiulian.downloader.model.search.field.TypeField;
import com.github.kiulian.downloader.testing.Corpus;
import com.github.kiulian.downloader.testing.FakeYoutube;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Search on recorded YouTube answers. Re-record with {@code mvn test -Precord}.
 */
class SearchFixtureTest {

    private final FakeYoutube youtube = FakeYoutube.fromFixtures();
    private final YoutubeDownloader yt = YoutubeDownloader.builder().transport(youtube).build();

    @Test
    void mixedResults() {
        SearchResult result = yt.search(Corpus.SEARCH);

        assertTrue(result.items().size() >= 15, result.items().size() + " items");
        assertTrue(result.estimatedResults() > 1000);
        assertNull(result.correctedQuery());

        List<SearchVideo> regular = result.videos().stream().filter(v -> !v.isShort()).collect(Collectors.toList());
        assertTrue(regular.size() >= 10);
        SearchVideo video = regular.get(0);
        assertEquals(11, video.videoId().length());
        assertTrue(video.title() != null && video.author() != null && video.channelId().startsWith("UC"));
        assertTrue(video.lengthSeconds() > 0 && video.viewCount() > 0);
        assertFalse(video.thumbnails().isEmpty());

        assertTrue(result.videos().stream().anyMatch(SearchVideo::isShort), "shorts shelf");
        SearchChannel nasa = result.channels().get(0);
        assertEquals("@NASA", nasa.handle());
        assertTrue(nasa.subscriberCount() > 1_000_000);
    }

    @Test
    void nextPageHasOtherVideos() {
        SearchResult first = yt.search(Corpus.SEARCH);
        assertTrue(first.hasNextPage());
        SearchResult second = first.nextPage();

        Set<String> firstIds = ids(first);
        long repeated = ids(second).stream().filter(firstIds::contains).count();
        assertTrue(second.items().size() >= 15);
        assertTrue(repeated < second.items().size() / 2, repeated + " repeated");
    }

    @Test
    void channelFilter() {
        SearchResult result = yt.search(SearchQuery.of("nasa").filter(TypeField.CHANNEL));

        assertEquals(result.items().size(), result.channels().size(), "only channels");
        assertTrue(result.channels().size() >= 10);
        assertTrue(result.channels().stream().allMatch(c -> c.channelId().startsWith("UC")));
    }

    @Test
    void playlistFilter() {
        SearchResult result = yt.search(SearchQuery.of("nasa").filter(TypeField.PLAYLIST));

        assertEquals(result.items().size(), result.playlists().size(), "only playlists");
        SearchPlaylist playlist = result.playlists().get(0);
        assertTrue(playlist.videoCount() > 0);
        assertTrue(playlist.author() != null);
    }

    @Test
    void shortVideosByViewCount() {
        SearchResult result = yt.search(SearchQuery.of("nasa").filter(TypeField.VIDEO, DurationField.UNDER_4_MINUTES).sortBy(SortField.VIEW_COUNT));

        List<SearchVideo> videos = result.videos();
        assertTrue(videos.size() >= 10);
        assertTrue(videos.stream().allMatch(v -> v.lengthSeconds() > 0 && v.lengthSeconds() < 240), "all under 4 minutes");
        assertTrue(videos.get(0).viewCount() >= videos.get(videos.size() - 1).viewCount(), "sorted by views");
    }

    @Test
    void misspelledQueryIsCorrected() {
        SearchResult result = yt.search(Corpus.SEARCH_TYPO);

        assertEquals("rick astley never gonna give", result.correctedQuery());
        assertEquals(Corpus.MUSIC, result.videos().get(0).videoId());
    }

    private static Set<String> ids(SearchResult result) {
        return result.items().stream().map(i -> i.id()).collect(Collectors.toCollection(HashSet::new));
    }
}
