package com.knoxhack.echothemecore.content;

import com.knoxhack.echothemecore.api.EchoThemeRenderPreset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;

public final class RenderPresetRegistry {
    private static final Map<Identifier, EchoThemeRenderPreset> PRESETS = new LinkedHashMap<>();

    private RenderPresetRegistry() {
    }

    public static synchronized void replaceLoaded(Map<Identifier, EchoThemeRenderPreset> loaded) {
        PRESETS.clear();
        if (loaded != null) {
            loaded.values().stream()
                .filter(preset -> preset != null && preset.id() != null)
                .forEach(preset -> PRESETS.put(preset.id(), preset));
        }
    }

    public static synchronized Optional<EchoThemeRenderPreset> find(Identifier id) {
        return Optional.ofNullable(PRESETS.get(id));
    }

    public static synchronized List<EchoThemeRenderPreset> listPresets() {
        return Collections.unmodifiableList(new ArrayList<>(PRESETS.values()));
    }

    public static synchronized List<EchoThemeRenderPreset> forTheme(Identifier themeId) {
        if (themeId == null) {
            return List.of();
        }
        return PRESETS.values().stream()
            .filter(preset -> themeId.equals(preset.theme()))
            .toList();
    }
}
