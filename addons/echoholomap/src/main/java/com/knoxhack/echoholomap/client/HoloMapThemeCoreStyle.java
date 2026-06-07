package com.knoxhack.echoholomap.client;

import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

final class HoloMapThemeCoreStyle {
    private HoloMapThemeCoreStyle() {
    }

    static int color(Player player, String method, int fallback) {
        if (player == null) {
            return fallback;
        }
        try {
            Class<?> api = Class.forName("com.knoxhack.echothemecore.api.EchoThemeApi");
            Object colors = api.getMethod("getColors", Player.class).invoke(null, player);
            return ((Integer) colors.getClass().getMethod(method).invoke(colors)).intValue();
        } catch (ReflectiveOperationException | LinkageError exception) {
            return fallback;
        }
    }

    static float intensity(Player player, String method, float fallback) {
        if (player == null) {
            return fallback;
        }
        try {
            Class<?> api = Class.forName("com.knoxhack.echothemecore.api.EchoThemeApi");
            Object render = api.getMethod("getRenderProfile", Player.class).invoke(null, player);
            return ((Float) render.getClass().getMethod(method).invoke(render)).floatValue();
        } catch (ReflectiveOperationException | LinkageError exception) {
            return fallback;
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    static Identifier moduleTexture(Player player, String textureKey, Identifier fallback) {
        if (player == null || textureKey == null || textureKey.isBlank()) {
            return fallback;
        }
        try {
            Class<?> api = Class.forName("com.knoxhack.echothemecore.api.EchoThemeApi");
            Class<?> keyClass = Class.forName("com.knoxhack.echothemecore.api.EchoThemeTextureKey");
            Object key = Enum.valueOf((Class) keyClass.asSubclass(Enum.class), textureKey);
            Object theme = api.getMethod("getTheme", Player.class).invoke(null, player);
            Object texture = theme.getClass().getMethod("moduleTexture", keyClass).invoke(theme, key);
            if (texture instanceof Optional<?> optional && optional.orElse(null) instanceof Identifier id) {
                return id;
            }
        } catch (ReflectiveOperationException | IllegalArgumentException | LinkageError exception) {
            return fallback;
        }
        return fallback;
    }
}
