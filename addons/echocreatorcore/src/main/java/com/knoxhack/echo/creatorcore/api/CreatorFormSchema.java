package com.knoxhack.echo.creatorcore.api;

import java.util.List;

public record CreatorFormSchema(
        String type,
        String title,
        String description,
        List<CreatorFormField> fields,
        boolean readOnly) {
    public CreatorFormSchema {
        type = safe(type, "generic");
        title = safe(title, type);
        description = description == null ? "" : description;
        fields = List.copyOf(fields == null ? List.of() : fields);
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}

