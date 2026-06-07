package com.knoxhack.echo.guidecore;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class GuideContractGuards {
    private GuideContractGuards() {
    }

    static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.strip();
    }

    static String id(String value, String fieldName) {
        return requireText(value, fieldName).toLowerCase(Locale.ROOT);
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
