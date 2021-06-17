package com.github.kiulian.downloader.model;

import java.util.LinkedList;
import java.util.List;

@FunctionalInterface
public interface Filter<T> {

    boolean test(T element);

    default List<T> select(List<T> elements) {
        List<T> filtered = new LinkedList<>();
        for (T element : elements) {
            if (test(element)) {
                filtered.add(element);
            }
        }
        return filtered;
    }
}
