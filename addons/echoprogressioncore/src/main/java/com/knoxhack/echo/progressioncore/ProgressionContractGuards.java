package com.knoxhack.echo.progressioncore;

import java.util.List;
import java.util.Map;
import java.util.Set;

final class ProgressionContractGuards {
    private ProgressionContractGuards() {
    }

    static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return value.trim();
    }

    static String optionalText(String value) {
        return value == null ? "" : value.trim();
    }

    static int nonNegative(int value, String label) {
        if (value < 0) {
            throw new IllegalArgumentException(label + " must be non-negative");
        }
        return value;
    }

    static int positiveOrOne(int value) {
        return Math.max(1, value);
    }

    static double boundedPercent(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    static <T> List<T> immutableList(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    static <T> Set<T> immutableSet(Set<T> values) {
        return values == null ? Set.of() : Set.copyOf(values);
    }

    static <K, V> Map<K, V> immutableMap(Map<K, V> values) {
        return values == null ? Map.of() : Map.copyOf(values);
    }
}
