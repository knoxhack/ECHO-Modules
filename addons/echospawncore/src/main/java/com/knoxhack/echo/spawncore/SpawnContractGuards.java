package com.knoxhack.echo.spawncore;

import java.util.List;
import java.util.Map;
import java.util.Set;

final class SpawnContractGuards {
    private SpawnContractGuards() {
    }

    static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.strip();
    }

    static String optionalText(String value) {
        return value == null ? "" : value.strip();
    }

    static int nonNegative(int value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " must not be negative");
        }
        return value;
    }

    static double nonNegative(double value, String fieldName) {
        if (!Double.isFinite(value) || value < 0.0D) {
            throw new IllegalArgumentException(fieldName + " must be finite and non-negative");
        }
        return value;
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
