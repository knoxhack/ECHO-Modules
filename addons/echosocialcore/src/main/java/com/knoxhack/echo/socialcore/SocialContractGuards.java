package com.knoxhack.echo.socialcore;

import java.util.List;
import java.util.Map;
import java.util.Set;

final class SocialContractGuards {
    private static final int MIN_REPUTATION = -1_000;
    private static final int MAX_REPUTATION = 1_000;

    private SocialContractGuards() {
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

    static int boundedReputation(int value) {
        return Math.max(MIN_REPUTATION, Math.min(MAX_REPUTATION, value));
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
