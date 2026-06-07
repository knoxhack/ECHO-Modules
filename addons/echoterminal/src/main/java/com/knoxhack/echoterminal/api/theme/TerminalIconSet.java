package com.knoxhack.echoterminal.api.theme;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.resources.Identifier;

public record TerminalIconSet(Map<TerminalIconKey, Identifier> icons, Identifier fallbackIcon) {
    public TerminalIconSet {
        icons = Map.copyOf(icons == null ? Map.of() : icons);
    }

    public static Builder builder() {
        return new Builder();
    }

    public Identifier resolve(TerminalIconKey key) {
        return resolve(key, fallbackIcon);
    }

    public Identifier resolve(TerminalIconKey key, Identifier fallback) {
        if (key == null) {
            return fallback == null ? fallbackIcon : fallback;
        }
        Identifier resolved = icons.get(key);
        if (resolved != null) {
            return resolved;
        }
        resolved = icons.get(TerminalIconKey.fallback(key.category()));
        if (resolved != null) {
            return resolved;
        }
        return fallback == null ? fallbackIcon : fallback;
    }

    public TerminalIconSet mergedWith(TerminalIconSet overrides) {
        if (overrides == null || overrides.icons().isEmpty()) {
            return this;
        }
        Map<TerminalIconKey, Identifier> merged = new LinkedHashMap<>(icons);
        merged.putAll(overrides.icons());
        return new TerminalIconSet(merged, overrides.fallbackIcon() == null ? fallbackIcon : overrides.fallbackIcon());
    }

    public static final class Builder {
        private final Map<TerminalIconKey, Identifier> icons = new LinkedHashMap<>();
        private Identifier fallbackIcon;

        public Builder icon(TerminalIconKey key, Identifier texture) {
            icons.put(Objects.requireNonNull(key, "Icon key is required."),
                    Objects.requireNonNull(texture, "Icon texture is required."));
            return this;
        }

        public Builder fallback(Identifier texture) {
            fallbackIcon = texture;
            return this;
        }

        public TerminalIconSet build() {
            return new TerminalIconSet(icons, fallbackIcon);
        }
    }
}
