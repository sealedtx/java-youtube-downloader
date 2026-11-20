package com.github.kiulian.downloader.js;

import com.github.kiulian.downloader.YoutubeDownloader;
import com.github.kiulian.downloader.client.ClientProfiles;
import com.github.kiulian.downloader.internal.json.Json;
import com.github.kiulian.downloader.internal.json.JsonNode;
import com.github.kiulian.downloader.model.videos.VideoInfo;
import com.github.kiulian.downloader.testing.Corpus;
import com.github.kiulian.downloader.testing.FakeYoutube;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * EJS in Node.js on the recorded player script; skipped when no Node.js 22+ is installed.
 * Expected answers come from the fixture recording ({@code mvn test -Precord}).
 */
class EjsSolverTest {

    private static EjsSolver solver;
    private static JsonNode expected;
    private static String playerCode;

    @BeforeAll
    static void needsNode() throws IOException {
        solver = EjsSolver.detect();
        assumeTrue(solver != null, "Node.js 22+ is not installed");
        try (InputStream in = EjsSolverTest.class.getResourceAsStream("/fixtures/js/solutions.json")) {
            expected = Json.parse(new String(in.readAllBytes(), UTF_8));
        }
        String id = expected.get("playerId").asString();
        playerCode = FakeYoutube.fixture("page/s_player_" + id + "_player_ias_vflset_en_US_base_js.html.gz");
    }

    @Test
    void solvesLikeAtRecordingTime() throws IOException {
        Set<String> n = new HashSet<>(expected.get("n").keys());
        Set<String> signatures = new HashSet<>(expected.get("sig").keys());

        ChallengeSolutions first = solver.solve(expected.get("playerId").asString(), playerCode, n, signatures);
        ChallengeSolutions second = solver.solve(expected.get("playerId").asString(), playerCode, n, signatures); // preprocessed player

        for (String challenge : n) {
            assertEquals(expected.at("n", challenge).asString(), first.n(challenge), "n " + challenge);
            assertEquals(first.n(challenge), second.n(challenge));
        }
        for (String challenge : signatures) {
            assertEquals(expected.at("sig", challenge).asString(), first.signature(challenge), "sig " + challenge);
            assertEquals(first.signature(challenge), second.signature(challenge));
        }
    }

    @Test
    void reportsAPlayerItCannotRead() {
        IOException error = assertThrows(IOException.class, () -> EjsSolver.node().solve("broken00", "var x = 1;",
                Collections.singleton("abc"), Collections.emptySet()));
        assertFalse(error.getMessage().isEmpty());
    }

    @Test
    void aRuntimeThatDoesNotReadStdinTimesOut() {
        EjsSolver stuck = EjsSolver.command(java.util.Arrays.asList("sh", "-c", "sleep 30")).withTimeout(java.time.Duration.ofSeconds(1));
        long start = System.currentTimeMillis();

        IOException error = assertThrows(IOException.class, () -> stuck.solve("p0000000", "", Collections.singleton("abc"), Collections.emptySet()));
        assertTrue(error.getMessage().contains("timed out"), error.getMessage());
        assertTrue(System.currentTimeMillis() - start < 10_000);
    }

    @Test
    void lotsOfStderrDoesNotBlockTheRuntime() {
        EjsSolver noisy = EjsSolver.command(java.util.Arrays.asList("sh", "-c", "cat > /dev/null; head -c 300000 /dev/zero | tr '\\0' x >&2; echo '{\"type\":\"error\",\"error\":\"nope\"}'"))
                .withTimeout(java.time.Duration.ofSeconds(20));
        long start = System.currentTimeMillis();

        IOException error = assertThrows(IOException.class, () -> noisy.solve("p0000000", "", Collections.singleton("abc"), Collections.emptySet()));
        assertTrue(error.getMessage().contains("nope"), error.getMessage());
        assertTrue(System.currentTimeMillis() - start < 10_000, "blocked on a full stderr pipe");
    }

    @Test
    void madeForKidsVideoThroughTheWholePipeline() {
        YoutubeDownloader yt = YoutubeDownloader.builder().transport(FakeYoutube.fromFixtures()).challengeSolver(solver)
                .clients(Collections.singletonList(ClientProfiles.TV_SAMSUNG)).build();

        VideoInfo video = yt.videoInfo(Corpus.MADE_FOR_KIDS);

        assertEquals("tv_samsung", video.client());
        assertTrue(video.formats().size() >= 10, video.formats().size() + " formats");
        assertTrue(video.formats().stream().allMatch(f -> f.url().contains("&sig=") && f.availableAtMillis() > 0));
    }
}
