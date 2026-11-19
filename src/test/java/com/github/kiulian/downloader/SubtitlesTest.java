package com.github.kiulian.downloader;

import com.github.kiulian.downloader.YoutubeException.HttpException;
import com.github.kiulian.downloader.model.subtitles.SubtitleFormat;
import com.github.kiulian.downloader.model.subtitles.SubtitleTrack;
import com.github.kiulian.downloader.testing.Corpus;
import com.github.kiulian.downloader.testing.FakeYoutube;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubtitlesTest {

    private final List<String> timedtextUrls = new CopyOnWriteArrayList<>();

    private final FakeYoutube youtube = new FakeYoutube((FakeYoutube.Router) request -> {
        if (request.url().contains("/api/timedtext")) {
            timedtextUrls.add(request.url());
            return request.url().contains("tlang=") ? FakeYoutube.status(429) : "WEBVTT\n\n00:00:00.000 --> 00:00:01.000\nAll right";
        }
        return FakeYoutube.fixture(FakeYoutube.fixturePath(request));
    });
    private final YoutubeDownloader yt = YoutubeDownloader.builder().transport(youtube).retries(0).build();

    @Test
    void downloadsATrackInTheChosenFormat() {
        SubtitleTrack track = yt.videoInfo(Corpus.ZOO).subtitles().get(0);

        String text = yt.subtitles(track, SubtitleFormat.VTT);

        assertTrue(text.startsWith("WEBVTT"));
        assertEquals(1, timedtextUrls.size());
        assertTrue(timedtextUrls.get(0).endsWith("&fmt=vtt"), timedtextUrls.get(0));
    }

    @Test
    void explainsWhenYoutubeRefusesATranslation() {
        SubtitleTrack track = yt.videoInfo(Corpus.ZOO).subtitles().get(0);
        assertTrue(track.isTranslatable());

        HttpException error = assertThrows(HttpException.class, () -> yt.subtitles(track, SubtitleFormat.SRT, "uk"));
        assertEquals(429, error.status());
        assertTrue(error.getMessage().contains("refuses machine translations"), error.getMessage());
        assertTrue(timedtextUrls.get(0).endsWith("&fmt=srt&tlang=uk"), timedtextUrls.get(0));
    }

    @Test
    void buildsUrlsWithoutDuplicateParameters() {
        SubtitleTrack track = new SubtitleTrack("en", "English", false, true,
                "https://www.youtube.com/api/timedtext?v=abc&lang=en&fmt=srv3&tlang=de&expire=1");

        assertEquals("https://www.youtube.com/api/timedtext?v=abc&lang=en&expire=1&fmt=json3", track.url(SubtitleFormat.JSON3));
        assertEquals("https://www.youtube.com/api/timedtext?v=abc&lang=en&expire=1&fmt=vtt&tlang=pt-BR", track.url(SubtitleFormat.VTT, "pt-BR"));
    }

    @Test
    void rejectsBadTranslationsAndForeignHosts() {
        SubtitleTrack notTranslatable = new SubtitleTrack("en", "English", false, false, "https://www.youtube.com/api/timedtext?v=abc");
        assertThrows(IllegalArgumentException.class, () -> notTranslatable.url(SubtitleFormat.VTT, "uk"));

        SubtitleTrack track = new SubtitleTrack("en", "English", false, true, "https://www.youtube.com/api/timedtext?v=abc");
        assertThrows(IllegalArgumentException.class, () -> track.url(SubtitleFormat.VTT, "uk&evil=1"));

        SubtitleTrack foreign = new SubtitleTrack("en", "English", false, true, "https://evil.example/api/timedtext?v=abc");
        assertThrows(IllegalArgumentException.class, () -> yt.subtitles(foreign, SubtitleFormat.VTT));
        assertFalse(timedtextUrls.stream().anyMatch(u -> u.contains("evil")));
    }
}
