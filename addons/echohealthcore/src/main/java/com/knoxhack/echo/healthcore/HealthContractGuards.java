package com.knoxhack.echo.healthcore;

import java.util.Collection;
import java.util.List;
import java.util.Map;

final class HealthContractGuards {
    private HealthContractGuards() {
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

    static double nonNegative(double value, String label) {
        if (!Double.isFinite(value) || value < 0.0D) {
            throw new IllegalArgumentException(label + " must be a finite non-negative value");
        }
        return value;
    }

    static long nonNegative(long value, String label) {
        if (value < 0L) {
            throw new IllegalArgumentException(label + " must be non-negative");
        }
        return value;
    }

    static <T> List<T> immutableList(Collection<? extends T> values) {
        return values == null || values.isEmpty() ? List.of() : List.copyOf(values);
    }

    static <K, V> Map<K, V> immutableMap(Map<? extends K, ? extends V> values) {
        return values == null || values.isEmpty() ? Map.of() : Map.copyOf(values);
    }
}
