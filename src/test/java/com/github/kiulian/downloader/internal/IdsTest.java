package com.github.kiulian.downloader.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IdsTest {

    private static final String ID = "jNQXAC9IVRw";

    @ParameterizedTest
    @ValueSource(strings = {
            "jNQXAC9IVRw",
            "  jNQXAC9IVRw  ",
            "https://www.youtube.com/watch?v=jNQXAC9IVRw",
            "https://www.youtube.com/watch?feature=share&v=jNQXAC9IVRw&t=10s",
            "https://www.youtube.com/watch?v=jNQXAC9IVRw#t=30",
            "http://youtube.com/watch?v=jNQXAC9IVRw",
            "youtube.com/watch?v=jNQXAC9IVRw",
            "www.youtube.com/watch?v=jNQXAC9IVRw",
            "https://m.youtube.com/watch?v=jNQXAC9IVRw",
            "https://music.youtube.com/watch?v=jNQXAC9IVRw&list=RDAMVMjNQXAC9IVRw",
            "https://youtu.be/jNQXAC9IVRw",
            "https://youtu.be/jNQXAC9IVRw?si=abc&t=3",
            "youtu.be/jNQXAC9IVRw",
            "https://www.youtube.com/shorts/jNQXAC9IVRw",
            "https://www.youtube.com/live/jNQXAC9IVRw?feature=share",
            "https://www.youtube.com/embed/jNQXAC9IVRw?start=5",
            "https://www.youtube-nocookie.com/embed/jNQXAC9IVRw",
            "HTTPS://WWW.YOUTUBE.COM/watch?v=jNQXAC9IVRw",
    })
    void findsTheVideoId(String input) {
        assertEquals(ID, Ids.videoId(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "abc",
            "jNQXAC9IVR",
            "jNQXAC9IVRww",
            "jNQXAC9IV!w",
            "https://www.youtube.com/watch?v=short",
            "https://www.youtube.com/watch?list=PL590L5WQmH8fJ54F369BLDSqIwcs-TCfs",
            "https://www.youtube.com/playlist?list=PL590L5WQmH8fJ54F369BLDSqIwcs-TCfs",
            "https://www.youtube.com/channel/UCSJ4gkVC6NrvII8umztf0Ow",
            "https://evilyoutube.com/watch?v=jNQXAC9IVRw",
            "https://youtube.com.evil.example/watch?v=jNQXAC9IVRw",
            "https://example.com/watch?v=jNQXAC9IVRw",
            "https://notyoutu.be/jNQXAC9IVRw",
            "ftp://www.youtube.com/watch?v=jNQXAC9IVRw",
            "javascript:alert(1)",
    })
    void rejectsEverythingElse(String input) {
        assertThrows(IllegalArgumentException.class, () -> Ids.videoId(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "PL2aBZuCeDwlRbJhDIkKXilZRz9ur4VVAA",
            "https://www.youtube.com/playlist?list=PL2aBZuCeDwlRbJhDIkKXilZRz9ur4VVAA",
            "https://www.youtube.com/watch?v=jNQXAC9IVRw&list=PL2aBZuCeDwlRbJhDIkKXilZRz9ur4VVAA&index=2",
            "music.youtube.com/playlist?list=PL2aBZuCeDwlRbJhDIkKXilZRz9ur4VVAA",
    })
    void findsThePlaylistId(String input) {
        assertEquals("PL2aBZuCeDwlRbJhDIkKXilZRz9ur4VVAA", Ids.playlistId(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "WL", "LL", "PLshort", "https://example.com/playlist?list=PL2aBZuCeDwlRbJhDIkKXilZRz9ur4VVAA",
            "https://www.youtube.com/watch?v=jNQXAC9IVRw"})
    void rejectsWhatIsNotAPlaylist(String input) {
        assertThrows(IllegalArgumentException.class, () -> Ids.playlistId(input));
    }

    @Test
    void findsChannels() {
        assertArrayEquals(new String[]{"UCLA_DiR1FfKNvjuUpBHmylQ", null}, Ids.channel("UCLA_DiR1FfKNvjuUpBHmylQ"));
        assertArrayEquals(new String[]{"UCLA_DiR1FfKNvjuUpBHmylQ", null}, Ids.channel("https://www.youtube.com/channel/UCLA_DiR1FfKNvjuUpBHmylQ/videos"));
        assertArrayEquals(new String[]{null, "https://www.youtube.com/@NASA"}, Ids.channel("@NASA"));
        assertArrayEquals(new String[]{null, "https://www.youtube.com/@NASA"}, Ids.channel("https://m.youtube.com/@NASA/shorts"));
        assertArrayEquals(new String[]{null, "https://www.youtube.com/c/NASA"}, Ids.channel("youtube.com/c/NASA"));
        assertArrayEquals(new String[]{null, "https://www.youtube.com/user/NASAtelevision"}, Ids.channel("https://www.youtube.com/user/NASAtelevision"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "NASA", "@a", "https://example.com/@NASA", "https://www.youtube.com/watch?v=jNQXAC9IVRw"})
    void rejectsWhatIsNotAChannel(String input) {
        assertThrows(IllegalArgumentException.class, () -> Ids.channel(input));
    }

    @Test
    void rejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> Ids.videoId(null));
    }
}
