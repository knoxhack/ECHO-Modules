package com.knoxhack.echo.platformcore;

import java.util.Set;

final class EchoContractGuards {
    private EchoContractGuards() {
    }

    static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    static String optionalText(String value) {
        return value == null ? "" : value.trim();
    }

    static <T> Set<T> immutableSet(Set<T> values) {
        return values == null ? Set.of() : Set.copyOf(values);
    }
}
