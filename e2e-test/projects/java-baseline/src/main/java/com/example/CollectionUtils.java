package com.example;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

public class CollectionUtils {

    public <T> List<T> filter(List<T> items, Predicate<T> predicate) {
        if (items == null) return List.of();
        List<T> result = new ArrayList<>();
        for (T item : items) {
            if (predicate.test(item)) {
                result.add(item);
            }
        }
        return result;
    }

    public <T extends Comparable<T>> T findMax(List<T> items) {
        if (items == null || items.isEmpty()) return null;
        T max = items.get(0);
        for (int i = 1; i < items.size(); i++) {
            if (items.get(i).compareTo(max) > 0) {
                max = items.get(i);
            }
        }
        return max;
    }

    public <T extends Comparable<T>> boolean isSorted(List<T> items) {
        if (items == null || items.size() <= 1) return true;
        for (int i = 1; i < items.size(); i++) {
            if (items.get(i).compareTo(items.get(i - 1)) < 0) {
                return false;
            }
        }
        return true;
    }

    public List<Integer> range(int start, int endExclusive) {
        List<Integer> result = new ArrayList<>();
        for (int i = start; i < endExclusive; i++) {
            result.add(i);
        }
        return result;
    }

    public <T> int indexOf(List<T> items, T target) {
        if (items == null) return -1;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).equals(target)) {
                return i;
            }
        }
        return -1;
    }
}
