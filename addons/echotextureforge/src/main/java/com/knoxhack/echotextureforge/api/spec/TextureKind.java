package com.knoxhack.echotextureforge.api.spec;

import java.util.Locale;

public enum TextureKind {
    ITEM("item"),
    BLOCK("block"),
    MACHINE("machine"),
    ARMOR("armor"),
    ENTITY("entity"),
    UI("ui"),
    PARTICLE("particle"),
    FLUID("fluid"),
    STRUCTURE_ICON("structure_icon");

    private final String id;

    TextureKind(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static TextureKind byId(String raw, TextureKind fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String normalized = raw.strip().toLowerCase(Locale.ROOT).replace('-', '_');
        for (TextureKind kind : values()) {
            if (kind.id.equals(normalized) || kind.name().equalsIgnoreCase(normalized)) {
                return kind;
            }
        }
        return fallback;
    }
}
