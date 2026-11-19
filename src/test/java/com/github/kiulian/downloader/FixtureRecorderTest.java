package com.github.kiulian.downloader;

import com.github.kiulian.downloader.YoutubeException.PlaylistUnavailableException;
import com.github.kiulian.downloader.client.ClientProfile;
import com.github.kiulian.downloader.client.ClientProfiles;
import com.github.kiulian.downloader.http.HttpTransport;
import com.github.kiulian.downloader.http.HttpTransport.Request;
import com.github.kiulian.downloader.http.UrlConnectionTransport;
import com.github.kiulian.downloader.internal.http.Http;
import com.github.kiulian.downloader.internal.http.RetryPolicy;
import com.github.kiulian.downloader.internal.json.Json;
import com.github.kiulian.downloader.internal.youtube.Innertube;
import com.github.kiulian.downloader.model.search.SearchQuery;
import com.github.kiulian.downloader.model.search.field.DurationField;
import com.github.kiulian.downloader.model.search.field.SortField;
import com.github.kiulian.downloader.model.search.field.TypeField;
import com.github.kiulian.downloader.testing.Corpus;
import com.github.kiulian.downloader.testing.RecordingTransport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Re-records the offline fixtures from real YouTube: {@code mvn test -Precord}.
 * <p>
 * Review the git diff afterwards: it shows what YouTube changed. The caller's IP address is masked.
 */
@Tag("record")
class FixtureRecorderTest {

    private static final Path FIXTURES = Paths.get("src/test/resources/fixtures");

    private static RecordingTransport transport;

    @BeforeAll
    static void recordingTransport() throws IOException {
        transport = new RecordingTransport(UrlConnectionTransport.create(), FIXTURES, ownIp(UrlConnectionTransport.create()));
    }

    @Test
    void recordPlayerAnswersOfEveryClient() throws IOException {
        Innertube innertube = new Innertube(new Http(transport, RetryPolicy.retries(2)));
        for (String videoId : Corpus.VIDEOS) {
            for (ClientProfile client : ClientProfiles.defaults()) {
                innertube.player(client, videoId);
            }
        }
    }

    @Test
    void recordPlaylistsAndChannels() throws IOException {
        YoutubeDownloader yt = YoutubeDownloader.builder().transport(transport).build();
        yt.playlist(Corpus.ASK_NASA).allVideos();
        yt.playlist(Corpus.ALBUM).allVideos();
        try {
            yt.playlist(Corpus.MISSING_PLAYLIST);
        } catch (PlaylistUnavailableException expected) {
            // recorded as it is
        }
        yt.channelUploads(Corpus.NASA_HANDLE).stream().limit(150).count();
        yt.channelUploads("https://www.youtube.com/c/NASA");
        new Innertube(new Http(transport, RetryPolicy.retries(2))).initialData("/playlist?list=" + Corpus.ASK_NASA);
    }

    @Test
    void recordSearches() {
        YoutubeDownloader yt = YoutubeDownloader.builder().transport(transport).build();
        yt.search(Corpus.SEARCH).nextPage();
        yt.search(SearchQuery.of("nasa").filter(TypeField.CHANNEL));
        yt.search(SearchQuery.of("nasa").filter(TypeField.PLAYLIST));
        yt.search(SearchQuery.of("nasa").filter(TypeField.VIDEO, DurationField.UNDER_4_MINUTES).sortBy(SortField.VIEW_COUNT));
        yt.search(Corpus.SEARCH_TYPO);
    }

    // sw.js_data also carries the caller's IP address; it must never end up in the repository.
    private static String ownIp(HttpTransport real) throws IOException {
        try (HttpTransport.Response response = real.send(Request.get(Innertube.ORIGIN + "/sw.js_data").build())) {
            String text = new String(response.body().readAllBytes(), UTF_8);
            return Json.parsePrefix(text, text.indexOf('[')).at(0, 2, 0, 0, 3).asString();
        }
    }
}
