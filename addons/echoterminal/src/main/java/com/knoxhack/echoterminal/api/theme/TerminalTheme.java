package com.knoxhack.echoterminal.api.theme;

import com.knoxhack.echoterminal.api.TerminalApiIds;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import net.minecraft.resources.Identifier;

public record TerminalTheme(
        Identifier id,
        String displayName,
        TerminalThemeTokens tokens,
        TerminalIconSet icons,
        Map<Identifier, Identifier> visualOverrides,
        Map<String, TerminalChapterStyle> chapterStyles,
        TerminalChapterStyle fallbackChapterStyle) {
    public TerminalTheme {
        TerminalApiIds.requireLowercase(id, "Terminal theme");
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("Terminal theme display name is required.");
        }
        tokens = Objects.requireNonNull(tokens, "Terminal theme tokens are required.");
        icons = icons == null ? TerminalIconSet.builder().build() : icons;
        visualOverrides = Map.copyOf(visualOverrides == null ? Map.of() : visualOverrides);
        chapterStyles = Map.copyOf(chapterStyles == null ? Map.of() : chapterStyles);
        fallbackChapterStyle = fallbackChapterStyle == null
                ? TerminalChapterStyle.builder("", displayName).colors(tokens.colors().accent(), tokens.colors().muted()).build()
                : fallbackChapterStyle;
    }

    public static Builder builder(Identifier id, String displayName) {
        return new Builder(id, displayName);
    }

    public TerminalChapterStyle chapterStyle(TerminalThemeContext context) {
        if (context != null) {
            TerminalChapterStyle byNamespace = chapterStyles.get(clean(context.namespace()));
            if (byNamespace != null) {
                return byNamespace;
            }
            TerminalChapterStyle byChapter = chapterStyles.get(clean(context.chapterId()));
            if (byChapter != null) {
                return byChapter;
            }
            if (context.activeTabId() != null) {
                TerminalChapterStyle byTabNamespace = chapterStyles.get(clean(context.activeTabId().getNamespace()));
                if (byTabNamespace != null) {
                    return byTabNamespace;
                }
            }
        }
        return fallbackChapterStyle;
    }

    public Identifier icon(TerminalIconKey key, TerminalThemeContext context, Identifier fallback) {
        TerminalChapterStyle style = chapterStyle(context);
        Identifier resolved = style.icons().resolve(key, null);
        return resolved == null ? icons.resolve(key, fallback) : resolved;
    }

    public Identifier visual(Identifier texture) {
        if (texture == null) {
            return null;
        }
        Identifier resolved = visualOverrides.get(texture);
        if (resolved != null) {
            return resolved;
        }
        String path = texture.getPath();
        if (texture.getNamespace().equals(id.getNamespace())
                && path.startsWith("textures/gui/")
                && !path.startsWith("textures/gui/themes/")) {
            return Identifier.fromNamespaceAndPath(texture.getNamespace(),
                    "textures/gui/themes/" + id.getPath() + "/" + path.substring("textures/gui/".length()));
        }
        return texture;
    }

    private static String clean(String value) {
        return value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
    }

    public static final class Builder {
        private final Identifier id;
        private final String displayName;
        private TerminalThemeTokens tokens;
        private TerminalIconSet icons = TerminalIconSet.builder().build();
        private final Map<Identifier, Identifier> visualOverrides = new LinkedHashMap<>();
        private final Map<String, TerminalChapterStyle> chapterStyles = new LinkedHashMap<>();
        private TerminalChapterStyle fallbackChapterStyle;

        private Builder(Identifier id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }

        public Builder tokens(TerminalThemeTokens tokens) {
            this.tokens = tokens;
            return this;
        }

        public Builder icons(TerminalIconSet icons) {
            this.icons = icons == null ? TerminalIconSet.builder().build() : icons;
            return this;
        }

        public Builder visualOverride(Identifier source, Identifier replacement) {
            visualOverrides.put(
                    Objects.requireNonNull(source, "Source texture is required."),
                    Objects.requireNonNull(replacement, "Replacement texture is required."));
            return this;
        }

        public Builder visualOverrides(Map<Identifier, Identifier> overrides) {
            if (overrides != null) {
                overrides.forEach(this::visualOverride);
            }
            return this;
        }

        public Builder chapterStyle(TerminalChapterStyle style) {
            if (style != null && !style.key().isBlank()) {
                chapterStyles.put(style.key(), style);
            }
            return this;
        }

        public Builder fallbackChapterStyle(TerminalChapterStyle style) {
            fallbackChapterStyle = style;
            return this;
        }

        public TerminalTheme build() {
            return new TerminalTheme(id, displayName, tokens, icons, visualOverrides, chapterStyles, fallbackChapterStyle);
        }
    }
}
