package com.knoxhack.echo.validationcore;

import java.util.List;
import java.util.Map;
import java.util.Set;

final class ValidationContractGuards {
    private ValidationContractGuards() {
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

    static <T> List<T> immutableList(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    static <T> Set<T> immutableSet(Set<T> values) {
        return values == null ? Set.of() : Set.copyOf(values);
    }

    static Map<String, String> immutableStringMap(Map<String, String> values) {
        return values == null ? Map.of() : Map.copyOf(values);
    }
}
