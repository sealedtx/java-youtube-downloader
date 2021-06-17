package com.github.kiulian.downloader.downloader.response;

import java.util.concurrent.*;

public class ResponseImpl<T> implements Response<T> {

    private Future<T> data;
    private Throwable error;

    private ResponseImpl(Future<T> data, Throwable error) {
        this.data = data;
        this.error = error;
    }

    public static <T> ResponseImpl<T> from(T data) {
        Future<T> future = new Future<T>() {

            @Override
            public T get(long timeout, TimeUnit unit) {
                return get();
            }

            @Override
            public T get() {
                return data;
            }

            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                return false;
            }

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public boolean isDone() {
                return true;
            }
        };

        return fromFuture(future);
    }

    public static <T> ResponseImpl<T> fromFuture(Future<T> data) {
        return new ResponseImpl<>(data, null);
    }

    public static <T> ResponseImpl<T> error(Throwable throwable) {
        return new ResponseImpl<>(null, throwable);
    }

    /**
     * {@inheritDoc}
     * NOTE: This implementation will block the thread if request is async
     */
    @Override
    public T data() {
        if (data != null) {
            try {
                return data.get();
            } catch (InterruptedException | ExecutionException e) {
                error = e;
            }
        }
        return null;
    }


    /**
     * {@inheritDoc}
     * NOTE: This implementation will block the thread if request is async
     */
    @Override
    public T data(long timeout, TimeUnit unit) throws TimeoutException {
        if (data != null) {
            try {
                return data.get(timeout, unit);
            } catch (InterruptedException | ExecutionException e) {
                error = e;
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * NOTE: This implementation will block the thread if request is async
     */
    @Override
    public Throwable error() {
        if (data != null) {
            try {
                data.get();
            } catch (InterruptedException | ExecutionException e) {
                error = e;
                return e;
            }
        }
        return error;
    }

    @Override
    public ResponseStatus status() {
        if (error != null) {
            return ResponseStatus.error;
        }
        if (data != null) {
            if (data.isCancelled()) {
                return ResponseStatus.canceled;
            }

            try {
                ((Future<?>) data).get(1, TimeUnit.MILLISECONDS);
            } catch (CancellationException e) {
                return ResponseStatus.canceled;
            } catch (TimeoutException e) {
                return ResponseStatus.downloading;
            } catch (ExecutionException | InterruptedException e) {
                error = e;
                return ResponseStatus.error;
            }
            return ResponseStatus.completed;
        }
        return ResponseStatus.error;
    }

    /**
     * {@inheritDoc}
     * NOTE: This implementation will block the thread if request is async
     */
    @Override
    public boolean ok() {
        if (error != null) {
            return false;
        }

        try {
            ((Future<?>) data).get();
            return true;
        } catch (CancellationException ignored) {
        } catch (Exception e) {
            error = e;
        }

        return false;
    }

    @Override
    public boolean cancel() {
        if (error != null)
            return false;
        return data.cancel(true);
    }
}
