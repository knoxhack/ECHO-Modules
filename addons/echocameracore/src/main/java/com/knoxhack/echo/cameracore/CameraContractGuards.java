package com.knoxhack.echo.cameracore;

import java.util.List;
import java.util.Locale;
import java.util.Map;

final class CameraContractGuards {
    private CameraContractGuards() {
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

    static String normalizedId(String value, String label) {
        return requireText(value, label).toLowerCase(Locale.ROOT).replace('\\', '/');
    }

    static double nonNegative(double value, String label) {
        if (value < 0.0D) {
            throw new IllegalArgumentException(label + " must be non-negative");
        }
        return value;
    }

    static double clamped01(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    static <T> List<T> immutableList(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    static <K, V> Map<K, V> immutableMap(Map<K, V> values) {
        return values == null ? Map.of() : Map.copyOf(values);
    }
}
