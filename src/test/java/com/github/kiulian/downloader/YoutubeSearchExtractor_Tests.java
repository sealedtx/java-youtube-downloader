package com.github.kiulian.downloader;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

import com.github.kiulian.downloader.downloader.request.*;
import com.github.kiulian.downloader.downloader.response.Response;
import com.github.kiulian.downloader.model.search.*;
import com.github.kiulian.downloader.model.search.field.SortField;
import com.github.kiulian.downloader.model.search.field.TypeField;
import com.github.kiulian.downloader.model.search.query.*;

@DisplayName("Tests extracting metadata from search results")
public class YoutubeSearchExtractor_Tests {

    private YoutubeDownloader downloader;

    @BeforeEach
    void initDownloader() {
        downloader = new YoutubeDownloader();
    }

    @Test
    @DisplayName("search 'nasa' should have many results")
    void searchContinuation_Success() {
        assertDoesNotThrow(() -> {
            SearchResult result = search(new RequestSearchResult("nasa"));
            assertTrue(result.estimatedResults() > 20_000_000, "Estimated results should be over 20 M");
            assertFalse(result.channels().isEmpty(), "Result should contain a channel");
            assertFalse(result.shelves().isEmpty(), "Result should contain the channel latest shelf");
            assertFalse(result.videos().isEmpty(), "Result should contain at least one video");
            
            // first continuation
            SearchResult next = downloader.searchContinuation(new RequestSearchContinuation(result)).data();
            assertTrue(next.estimatedResults() > 20_000_000, "Next page results should also be over 20 M");
            
            // second continuation, asserts not empty
            downloader.searchContinuation(new RequestSearchContinuation(next)).data();
        });
    }

    @Test
    @DisplayName("search 'star wars' using each type filter and check that items are of the expected type")
    void searchAllTypes_Success() {
        assertDoesNotThrow(() -> {
            RequestSearchResult request = new RequestSearchResult("star wars");
            SearchResult result;
            
            result = search(request.type(TypeField.VIDEO));
            for (SearchResultItem item : result.items()) {
                assertTrue(item.type() == SearchResultItemType.VIDEO || item.type() == SearchResultItemType.SHELF,
                        "Video result should only contain videos, shelf and query suggestion");
            }
            
            result = search(request.type(TypeField.MOVIE));
            int movieCount = 0;
            for (SearchResultItem item : result.items()) {
                assertTrue(item.type() == SearchResultItemType.VIDEO, "Movie result should only contain videos");
                if (item.asVideo().isMovie()) {
                    movieCount++;
                }
            }
            assertTrue(movieCount > 0, "Movie result should contain at least one movie");
            
            result = search(request.type(TypeField.PLAYLIST));
            for (SearchResultItem item : result.items()) {
                assertTrue(item.type() == SearchResultItemType.PLAYLIST, "Playlist result should only contain playlists");
            }
            
            result = search(request.type(TypeField.CHANNEL));
            for (SearchResultItem item : result.items()) {
                assertTrue(item.type() == SearchResultItemType.CHANNEL, "Channel result should only contain channels");
            }
        });
    }

    @Test
    @DisplayName("search 'strange' videos sorted by view count should be sorted by view count")
    void searchVideosByViewCount_Success() {
        assertDoesNotThrow(() -> {
            SearchResult result = search(new RequestSearchResult("strange")
                    .filter(TypeField.VIDEO)
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

    @Test
    @DisplayName("search 'where is your love' videos")
    void searchVideosWithHugeResult_Success () {
        assertDoesNotThrow(() -> {
            SearchResult result = search(new RequestSearchResult("where is your love")
                    .filter(TypeField.VIDEO)
                    .sortBy(SortField.VIEW_COUNT));

            SearchResult next = downloader.searchContinuation(new RequestSearchContinuation(result)).data();
            assertTrue(next.estimatedResults() > 20_000_000, "Next page results should also be over 20 M");

        });
    }

    @Test
    @DisplayName("search 'lord of the rngs' and check that the result contains an auto correction or a suggestion")
    void searchAutoCorrectionOrSuggestion_Success() {
        final String expectedCorrection = "lord of the rings";
        assertDoesNotThrow(() -> {
            RequestSearchResult request = new RequestSearchResult("lord of the rngs");
            SearchResult result = search(request);
            QuerySuggestion suggestion = result.suggestion();
            if (suggestion == null) {
                assertTrue(result.isAutoCorrected(), "Result should be auto corrected or contain a suggestion");
                assertEquals(expectedCorrection, result.autoCorrectedQuery(), "Auto corrected query");
                
                // force initial query
                result = search(request.forceExactQuery(true));
                assertFalse(result.isAutoCorrected(), "Forced result should not be auto corrected");
                assertNotNull(result.suggestion(), "Forced result should contain a suggestion");
                assertEquals(expectedCorrection, result.suggestion().query(), "Forced result query suggestion");
            } else {
                assertEquals(expectedCorrection, suggestion.query(), "Query suggestion");
            }
        });
    }

    @Test
    @DisplayName("search 'michael jackson' and follow first refinement")
    void searchRefinement_Success() {
        assertDoesNotThrow(() -> {
            SearchResult result;
            result = search(new RequestSearchResult("michael jackson"));
            String initialTitle = result.items().get(0).title();
            QueryRefinementList refinements = result.refinements();
            assertNotNull(refinements, "Result should contain refinements");
            if (refinements != null) {
                assertFalse(refinements.isEmpty(), "Result refinements should not be empty");

                // refinement
                result = search(refinements.get(0));
                String refinedTitle = result.items().get(0).title();
                assertNotEquals(initialTitle, refinedTitle, "Refined title should be different");
            } else {
                System.out.println("No refinement found");
            }
        });
    }

    private SearchResult search(RequestSearchResult request) {
        return check(downloader.search(request));
    }

    private SearchResult search(Searchable searchable) {
        return check(downloader.search(new RequestSearchable(searchable)));
    }

    private static SearchResult check(Response<SearchResult> response) {
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
