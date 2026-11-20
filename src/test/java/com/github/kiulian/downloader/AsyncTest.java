package com.github.kiulian.downloader;

import com.github.kiulian.downloader.model.videos.VideoInfo;
import com.github.kiulian.downloader.testing.Corpus;
import com.github.kiulian.downloader.testing.FakeYoutube;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsyncTest {

    @Test
    void aRejectingExecutorCompletesTheFutureExceptionally() {
        Executor rejecting = task -> {
            throw new RejectedExecutionException("full");
        };
        YoutubeDownloader yt = YoutubeDownloader.builder().transport(FakeYoutube.fromFixtures()).executor(rejecting).build();

        CompletableFuture<VideoInfo> future = yt.videoInfoAsync(Corpus.ZOO);

        ExecutionException error = assertThrows(ExecutionException.class, future::get);
        assertTrue(error.getCause() instanceof RejectedExecutionException);
    }

    @Test
    void aClosedDownloaderRefusesAsyncCallsButNotSyncOnes() {
        YoutubeDownloader yt = YoutubeDownloader.builder().transport(FakeYoutube.fromFixtures()).build();
        yt.close();

        ExecutionException error = assertThrows(ExecutionException.class, () -> yt.videoInfoAsync(Corpus.ZOO).get());
        assertTrue(error.getCause() instanceof IllegalStateException);
        assertEquals("Me at the zoo", yt.videoInfo(Corpus.ZOO).details().title());
    }

    @Test
    void workCancelledBeforeItStartsNeverRuns() {
        List<Runnable> queued = new ArrayList<>();
        FakeYoutube youtube = FakeYoutube.fromFixtures();
        YoutubeDownloader yt = YoutubeDownloader.builder().transport(youtube).executor(queued::add).build();

        CompletableFuture<VideoInfo> future = yt.videoInfoAsync(Corpus.ZOO);
        assertTrue(future.cancel(true));
        queued.forEach(Runnable::run);

        assertTrue(youtube.playerCalls().isEmpty());
    }
}
