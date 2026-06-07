package com.knoxhack.echothemecore.content;

import com.knoxhack.echothemecore.EchoThemeCore;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;

public final class ThemeReloaders {
    private ThemeReloaders() {
    }

    public static List<NativeReloadListenerRegistration> reloadListeners() {
        return List.of(
            new NativeReloadListenerRegistration(EchoThemeCore.id("themes"), new ThemeJsonReloadListener()),
            new NativeReloadListenerRegistration(EchoThemeCore.id("render_presets"), new RenderPresetReloadListener()));
    }

    public record NativeReloadListenerRegistration(
        Identifier id,
        PreparableReloadListener listener) {
    }
}
