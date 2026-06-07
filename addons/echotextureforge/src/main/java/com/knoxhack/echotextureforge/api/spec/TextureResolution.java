package com.knoxhack.echotextureforge.api.spec;

public record TextureResolution(int width, int height) {
    public static final TextureResolution DEFAULT_32 = new TextureResolution(32, 32);

    public TextureResolution {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Texture resolution must be positive.");
        }
    }

    public String id() {
        return width + "x" + height;
    }

    public static TextureResolution parse(String raw, TextureResolution fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String[] parts = raw.strip().toLowerCase(java.util.Locale.ROOT).split("x", 2);
        if (parts.length != 2) {
            return fallback;
        }
        try {
            int width = Integer.parseInt(parts[0].trim());
            int height = Integer.parseInt(parts[1].trim());
            return new TextureResolution(width, height);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}
