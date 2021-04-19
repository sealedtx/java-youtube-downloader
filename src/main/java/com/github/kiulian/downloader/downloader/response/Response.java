package com.github.kiulian.downloader.downloader.response;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public interface Response<T> {

    /**
     * Waits if necessary for the request to complete, and then returns its result
     * if the request completed successfully.
     * <p>
     * {@code data} if the response contains data;
     * {@code null} otherwise
     */
    T data();

    /**
     * Waits if necessary for at most the given timeout for the request to complete,
     * and then returns its result if the request completed successfully.
     * <p>
     *
     * @param timeout the maximum time to wait
     * @param unit    the time unit of the timeout argument
     * @return {@code data} if the response contains data;
     * {@code null} otherwise
     */
    T data(long timeout, TimeUnit unit) throws TimeoutException;

    /**
     * Waits if request is still processing and then returns the error if the request
     * completed exceptionally, or returns immediately the error if the request
     * has been already completed exceptionally.
     *
     * <p>
     * {@code error} if the request completed exceptionally;
     * {@code null} if the request is completed normally or canceled
     */
    Throwable error();

    /**
     * Returns the status of for this response.
     *
     * @return {@code DownloadStatus.downloading} if the request is still downloading;
     * {@code DownloadStatus.error} if the request completed exceptionally;
     * {@code DownloadStatus.canceled} if the request canceled;
     * {@code DownloadStatus.completed} otherwise
     */
    ResponseStatus status();

    /**
     * Returns the {@code true} if the request completed successfully.
     *
     * @return {@code true} if the response status equals {@code DownloadStatus.completed};
     * {@code false} otherwise
     */
    boolean ok();

    /**
     * Attempts to cancel execution of request {@code data} if it is still downloading.
     * This attempt will fail if the task has already completed or has already been cancelled.
     *
     * @return {@code false} if the task could not be cancelled, because
     * has already completed normally or has already finished with an error;
     * {@code true} otherwise
     */
    boolean cancel();
}
