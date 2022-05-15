package com.github.kiulian.downloader;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

import com.github.kiulian.downloader.downloader.request.RequestSearchContinuation;
import com.github.kiulian.downloader.downloader.request.RequestSearchResult;
import com.github.kiulian.downloader.downloader.response.Response;
import com.github.kiulian.downloader.model.search.*;
import com.github.kiulian.downloader.model.search.field.SortField;
import com.github.kiulian.downloader.model.search.field.TypeField;

@DisplayName("Tests extracting metadata from search results")
public class YoutubeSearchExtractor_Tests {

    private YoutubeDownloader downloader;

    @BeforeEach
    void initDownloader() {
        downloader = new YoutubeDownloader();
    }

    @Test
    @DisplayName("search 'nasa' should have many results")
    void searchNasaAndContinuation_Success() {
        assertDoesNotThrow(() -> {
            SearchResult result = search(new RequestSearchResult("nasa"));
            assertTrue(result.estimatedResults() > 20_000_000, "Estimated results should be over 20 M");
            assertTrue(!result.channels().isEmpty(), "Result should contain a channel");
            assertTrue(!result.shelves().isEmpty(), "Result should contain the channel latest shelf");
            assertTrue(!result.videos().isEmpty(), "Result should contain at least a video");
            
            SearchResult next = downloader.getNextPage(new RequestSearchContinuation(result)).data();
            assertTrue(next.estimatedResults() > 20_000_000, "Next page results should also be over 20 M");
        });
    }

    @Test
    @DisplayName("search 'sun' using each type filter and check that items are of the expected type")
    void searchSunAllTypes_Success() {
        assertDoesNotThrow(() -> {
            RequestSearchResult request = new RequestSearchResult("sun");
            SearchResult result;
            
            result = search(request.type(TypeField.VIDEO));
            for (SearchResultItem item : result.items()) {
                assertTrue(item.isVideo() || item.isShelf(), "Video result should only contain videos and shelves");
            }
            
            result = search(request.type(TypeField.MOVIE));
            boolean movieFound = false;
            for (SearchResultItem item : result.items()) {
                assertTrue(item.isVideo(), "Movie result should only contain videos");
                if (item.asVideo().isMovie()) {
                    movieFound = true;
                }
            }
            assertTrue(movieFound, "Movie result should contain at least one movie");
            
            result = search(request.type(TypeField.PLAYLIST));
            for (SearchResultItem item : result.items()) {
                assertTrue(item.isPlaylist(), "Playlist result should only contain playlists");
            }
            
            result = search(request.type(TypeField.CHANNEL));
            for (SearchResultItem item : result.items()) {
                assertTrue(item.isChannel(), "Channel result should only contain channels");
            }
        });
    }

    @Test
    @DisplayName("search 'strange' videos sorted by view count should be sorted by view count")
    void searchStrangeVideosByViewCount_Success() {
        assertDoesNotThrow(() -> {
            SearchResult result = search(new RequestSearchResult("strange")
                    .select(TypeField.VIDEO)
                    .sortBy(SortField.VIEW_COUNT));
            
            SearchResultVideoDetails lastVideo = null;
            for (SearchResultVideoDetails video : result.videos()) {
                if (lastVideo != null) {
                    // 5% margin
                    long viewCount = (video.viewCount() * 95L) / 100L;
                    assertTrue(viewCount < lastVideo.viewCount(), "View count -5% should be less than previous video's view count");
                }
                lastVideo = video;
            }
        });
    }

    private SearchResult search(RequestSearchResult request) {
        Response<SearchResult> response = downloader.search(request);
        if (!response.ok()) {
            response.error().printStackTrace();
        }
        assertTrue(response.ok());
        SearchResult result = response.data();
        assertFalse(result.estimatedResults() < 1, "search results should have a positive estimated count");
        assertNotNull(result.items(), "search result should contain items");
        assertFalse(result.items().isEmpty(), "search result should contain items");
        return result;
    }
}
