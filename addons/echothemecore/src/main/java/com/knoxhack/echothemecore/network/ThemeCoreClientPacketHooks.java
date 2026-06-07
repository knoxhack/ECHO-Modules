package com.knoxhack.echothemecore.network;

import com.knoxhack.echothemecore.EchoThemeCore;
import java.lang.reflect.Method;
import net.minecraft.resources.Identifier;

final class ThemeCoreClientPacketHooks {
    private ThemeCoreClientPacketHooks() {
    }

    static void applyTheme(Identifier themeId) {
        try {
            Class<?> cache = Class.forName("com.knoxhack.echothemecore.client.ClientThemeCache");
            Method method = cache.getMethod("applyServerTheme", Identifier.class);
            method.invoke(null, themeId);
        } catch (ReflectiveOperationException exception) {
            EchoThemeCore.LOGGER.debug("ThemeCore client theme cache is unavailable for packet sync.", exception);
        }
    }
}
