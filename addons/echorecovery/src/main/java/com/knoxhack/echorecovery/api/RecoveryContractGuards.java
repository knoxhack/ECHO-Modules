package com.knoxhack.echorecovery.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;

final class RecoveryContractGuards {
    private RecoveryContractGuards() {
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

    static double boundedConfidence(double value) {
        if (!Double.isFinite(value)) {
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(1.0D, value));
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
