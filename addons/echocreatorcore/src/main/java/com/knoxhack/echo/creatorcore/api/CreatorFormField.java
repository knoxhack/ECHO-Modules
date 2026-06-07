package com.knoxhack.echo.creatorcore.api;

import java.util.List;

public record CreatorFormField(
        String name,
        String label,
        CreatorFormFieldKind kind,
        boolean required,
        List<String> options,
        String placeholder,
        boolean readOnly) {
    public CreatorFormField {
        name = safe(name, "field");
        label = safe(label, name);
        kind = kind == null ? CreatorFormFieldKind.TEXT : kind;
        options = List.copyOf(options == null ? List.of() : options);
        placeholder = placeholder == null ? "" : placeholder;
    }

    public static CreatorFormField text(String name, String label, boolean required, String placeholder) {
        return new CreatorFormField(name, label, CreatorFormFieldKind.TEXT, required, List.of(), placeholder, false);
    }

    public static CreatorFormField textArea(String name, String label, boolean required, String placeholder) {
        return new CreatorFormField(name, label, CreatorFormFieldKind.TEXT_AREA, required, List.of(), placeholder, false);
    }

    public static CreatorFormField select(String name, String label, boolean required, List<String> options) {
        return new CreatorFormField(name, label, CreatorFormFieldKind.SELECT, required, options, "", false);
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}

