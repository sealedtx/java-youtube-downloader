package com.github.kiulian.downloader.internal.json;

/**
 * Thrown when text is not valid JSON or a value cannot be written as JSON.
 */
public final class JsonException extends RuntimeException {

    JsonException(String message) {
        super(message);
    }
}
