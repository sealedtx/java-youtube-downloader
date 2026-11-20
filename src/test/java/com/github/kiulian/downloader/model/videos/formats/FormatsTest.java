package com.github.kiulian.downloader.model.videos.formats;

import com.github.kiulian.downloader.model.videos.quality.AudioQuality;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormatsTest {

    private final Format muxed360 = video(18, "video/mp4; codecs=\"avc1.42001E, mp4a.40.2\"", 360, 30).audioQuality(AudioQuality.low).build();
    private final Format video720 = video(136, "video/mp4; codecs=\"avc1.64001F\"", 720, 30).build();
    private final Format video720p60 = video(298, "video/mp4; codecs=\"avc1.4d4020\"", 720, 60).build();
    private final Format audioDrc = audio(140, 130_000).drc(true).build();
    private final Format audioPlain = audio(140, 128_000).build();
    private final Format audioDubbed = audio(251, 160_000).audioTrack("de.4", "German", false).build();

    private final Formats formats = new Formats(Arrays.asList(muxed360, video720, video720p60, audioDrc, audioPlain, audioDubbed));

    @Test
    void splitsByContent() {
        assertEquals(Collections.singletonList(muxed360), formats.muxed());
        assertEquals(Arrays.asList(video720, video720p60), formats.videoOnly());
        assertEquals(Arrays.asList(audioDrc, audioPlain, audioDubbed), formats.audioOnly());
    }

    @Test
    void picksTheBest() {
        assertEquals(video720p60, formats.bestVideo().orElse(null));
        assertEquals(muxed360, formats.bestMuxed().orElse(null));
        assertEquals(audioPlain, formats.bestAudio().orElse(null), "main track and no volume compression beat bitrate");
    }

    @Test
    void findsByItag() {
        assertEquals(audioDrc, formats.byItag(140).orElse(null));
        assertFalse(formats.byItag(999).isPresent());
    }

    @Test
    void emptyFormatsHaveNoBest() {
        Formats none = new Formats(Collections.emptyList());
        assertFalse(none.bestVideo().isPresent());
        assertFalse(none.bestAudio().isPresent());
    }

    @Test
    void readsCodecsAndExtension() {
        assertEquals("avc1.42001E, mp4a.40.2", muxed360.codecs());
        assertEquals(Extension.MP4, muxed360.extension());
        assertEquals(Extension.M4A, audioPlain.extension());
        assertEquals(Extension.WEBA, Extension.fromMimeType("audio/webm; codecs=\"opus\""));
        assertEquals(Extension.UNKNOWN, Extension.fromMimeType(null));
    }

    @Test
    void knowsItags() {
        assertEquals(Itag.i18, muxed360.knownItag());
        assertTrue(Itag.i18.isVideo() && Itag.i18.isAudio());
        assertNull(Itag.of(123456));
    }

    private static Format.Builder video(int itag, String mime, int height, int fps) {
        return Format.builder(itag, "https://rr1.googlevideo.com/videoplayback?itag=" + itag).mimeType(mime).height(height).fps(fps);
    }

    private static Format.Builder audio(int itag, int bitrate) {
        return Format.builder(itag, "https://rr1.googlevideo.com/videoplayback?itag=" + itag)
                .mimeType("audio/mp4; codecs=\"mp4a.40.2\"").averageBitrate(bitrate).audioQuality(AudioQuality.medium);
    }
}
