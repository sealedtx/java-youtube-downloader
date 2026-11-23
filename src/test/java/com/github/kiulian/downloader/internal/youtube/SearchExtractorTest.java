package com.github.kiulian.downloader.internal.youtube;

import com.github.kiulian.downloader.internal.http.Http;
import com.github.kiulian.downloader.internal.http.RetryPolicy;
import com.github.kiulian.downloader.internal.json.Json;
import com.github.kiulian.downloader.internal.json.JsonNode;
import com.github.kiulian.downloader.model.search.SearchItem;
import com.github.kiulian.downloader.model.search.SearchPlaylist;
import com.github.kiulian.downloader.model.search.SearchQuery;
import com.github.kiulian.downloader.model.search.SearchResult;
import com.github.kiulian.downloader.model.search.SearchVideo;
import com.github.kiulian.downloader.model.search.field.SortField;
import com.github.kiulian.downloader.model.search.field.TypeField;
import com.github.kiulian.downloader.testing.FakeYoutube;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Item shapes, ads and paging on small hand-written search answers.
 */
class SearchExtractorTest {

    private static final String VIDEO = "{\"videoRenderer\":{\"videoId\":\"jNQXAC9IVRw\",\"title\":{\"runs\":[{\"text\":\"Me at the zoo\"}]},"
            + "\"lengthText\":{\"simpleText\":\"0:19\"},\"viewCountText\":{\"simpleText\":\"1,234 views\"}}}";

    @Test
    void sendsQueryAndEncodedFilters() {
        List<JsonNode> bodies = new CopyOnWriteArrayList<>();
        search(request -> {
            bodies.add(Json.parse(new String(request.body(), UTF_8)));
            return "{}";
        }, SearchQuery.of("nasa").filter(TypeField.VIDEO).sortBy(SortField.UPLOAD_DATE));

        assertEquals("nasa", bodies.get(0).get("query").asString());
        assertEquals("CAISAhAB", bodies.get(0).get("params").asString());
    }

    @Test
    void leavesOutAdsAndUnknownWidgets() {
        String page = "{\"contents\":[{\"adSlotRenderer\":{\"inner\":" + VIDEO.replace("jNQXAC9IVRw", "AAAAAAAAAAA") + "}},"
                + "{\"brandNewWidgetRenderer\":{\"x\":1}}," + VIDEO + "]}";

        SearchResult result = search(request -> page, SearchQuery.of("zoo"));

        assertEquals(1, result.items().size());
        SearchVideo video = result.videos().get(0);
        assertEquals("jNQXAC9IVRw", video.videoId());
        assertEquals(19, video.lengthSeconds());
        assertEquals(1234, video.viewCount());
    }

    @Test
    void readsOldPlaylistAndShortShapes() {
        String page = "{\"contents\":[{\"playlistRenderer\":{\"playlistId\":\"PL1234567890ab\",\"title\":{\"simpleText\":\"List\"},\"videoCount\":\"7\"}},"
                + "{\"reelItemRenderer\":{\"videoId\":\"GZAm_gtnOts\",\"headline\":{\"simpleText\":\"A short\"},\"viewCountText\":{\"simpleText\":\"2.8M views\"}}}]}";

        List<SearchItem> items = search(request -> page, SearchQuery.of("x")).items();

        assertEquals(7, ((SearchPlaylist) items.get(0)).videoCount());
        SearchVideo shortVideo = (SearchVideo) items.get(1);
        assertTrue(shortVideo.isShort());
        assertEquals("A short", shortVideo.title());
        assertEquals(2_800_000, shortVideo.viewCount());
    }

    @Test
    void pagesUntilThereIsNoToken() {
        String first = "{\"contents\":[" + VIDEO + ",{\"continuationItemRenderer\":{\"continuationEndpoint\":{\"continuationCommand\":{\"token\":\"t1\"}}}}]}";
        String second = "{\"onResponseReceivedCommands\":[{\"appendContinuationItemsAction\":{\"continuationItems\":["
                + VIDEO.replace("jNQXAC9IVRw", "eRsGyueVLvQ") + "]}}]}";

        SearchResult page1 = search(request -> new String(request.body(), UTF_8).contains("\"continuation\"") ? second : first, SearchQuery.of("x"));
        SearchResult page2 = page1.nextPage();

        assertEquals("eRsGyueVLvQ", page2.videos().get(0).videoId());
        assertFalse(page2.hasNextPage());
        assertThrows(NoSuchElementException.class, page2::nextPage);
        assertEquals(page2.items().size(), page1.nextPage().items().size(), "a page can be loaded again");
    }

    @Test
    void stopsWhenAPagePointsToItself() {
        String looping = "{\"onResponseReceivedCommands\":[{\"appendContinuationItemsAction\":{\"continuationItems\":[" + VIDEO
                + ",{\"continuationItemRenderer\":{\"continuationEndpoint\":{\"continuationCommand\":{\"token\":\"t1\"}}}}]}}]}";
        String first = "{\"contents\":[{\"continuationItemRenderer\":{\"continuationEndpoint\":{\"continuationCommand\":{\"token\":\"t1\"}}}}]}";

        SearchResult page = search(request -> new String(request.body(), UTF_8).contains("\"continuation\"") ? looping : first, SearchQuery.of("x"));

        assertFalse(page.nextPage().hasNextPage());
    }

    @Test
    void parsesCounts() {
        assertEquals(2_917_237, Renderers.parseCount("2,917,237 views"));
        assertEquals(2_800_000, Renderers.parseCount("2.8M views"));
        assertEquals(15_100, Renderers.parseCount("15.1K subscribers"));
        assertEquals(1_200_000_000, Renderers.parseCount("1.2B views"));
        assertEquals(0, Renderers.parseCount("No views"));
    }

    private static SearchResult search(FakeYoutube.Router router, SearchQuery query) {
        return new SearchExtractor(new Innertube(new Http(new FakeYoutube(router), RetryPolicy.NONE))).search(query);
    }
}
