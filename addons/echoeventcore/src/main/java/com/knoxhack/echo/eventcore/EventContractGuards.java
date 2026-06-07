package com.knoxhack.echo.eventcore;

import java.util.List;
import java.util.Locale;
import java.util.Map;

final class EventContractGuards {
    private EventContractGuards() {
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

    static long nonNegative(long value, String fieldName) {
        if (value < 0L) {
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

    static double ratio(double value, String fieldName) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(fieldName + " must be finite");
        }
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    static <T> List<T> immutableList(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    static <K, V> Map<K, V> immutableMap(Map<K, V> values) {
        return values == null ? Map.of() : Map.copyOf(values);
    }
}
