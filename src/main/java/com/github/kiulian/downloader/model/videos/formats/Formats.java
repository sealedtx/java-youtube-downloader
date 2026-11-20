package com.github.kiulian.downloader.model.videos.formats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The formats of a video, with helpers to pick one.
 */
public final class Formats implements Iterable<Format> {

    private static final Comparator<Format> BY_VIDEO = Comparator.comparingInt(Format::height)
            .thenComparingInt(Format::fps)
            .thenComparingInt(Format::bitrate);

    private static final Comparator<Format> BY_AUDIO = Comparator.comparing(Format::isDefaultAudioTrack)
            .thenComparing(f -> !f.isDrc())
            .thenComparingInt(f -> f.averageBitrate() > 0 ? f.averageBitrate() : f.bitrate());

    private final List<Format> formats;

    public Formats(List<Format> formats) {
        this.formats = Collections.unmodifiableList(new ArrayList<>(formats));
    }

    public List<Format> list() {
        return formats;
    }

    public Stream<Format> stream() {
        return formats.stream();
    }

    @Override
    public Iterator<Format> iterator() {
        return formats.iterator();
    }

    public int size() {
        return formats.size();
    }

    public boolean isEmpty() {
        return formats.isEmpty();
    }

    public List<Format> filter(Predicate<Format> predicate) {
        return formats.stream().filter(predicate).collect(Collectors.toList());
    }

    /** The first format with this itag. */
    public Optional<Format> byItag(int itag) {
        return formats.stream().filter(f -> f.itag() == itag).findFirst();
    }

    /** Formats with video and no audio. */
    public List<Format> videoOnly() {
        return filter(f -> f.hasVideo() && !f.hasAudio());
    }

    /** Formats with audio and no video. */
    public List<Format> audioOnly() {
        return filter(f -> f.hasAudio() && !f.hasVideo());
    }

    /** Formats with video and audio in one file. */
    public List<Format> muxed() {
        return filter(f -> f.hasVideo() && f.hasAudio());
    }

    /** Highest resolution, then frame rate, then bitrate, among formats with video (with or without audio). */
    public Optional<Format> bestVideo() {
        return formats.stream().filter(Format::hasVideo).max(BY_VIDEO);
    }

    /** Best audio-only format: main audio track first, without volume compression, then highest bitrate. */
    public Optional<Format> bestAudio() {
        return audioOnly().stream().max(BY_AUDIO);
    }

    /** Best format with video and audio in one file. YouTube offers few of these, often only 360p. */
    public Optional<Format> bestMuxed() {
        return muxed().stream().max(BY_VIDEO);
    }

    @Override
    public String toString() {
        return formats.toString();
    }
}
