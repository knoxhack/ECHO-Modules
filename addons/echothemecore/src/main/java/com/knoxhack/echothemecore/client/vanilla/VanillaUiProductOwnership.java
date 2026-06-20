package com.knoxhack.echothemecore.client.vanilla;

import java.lang.reflect.Method;
import net.minecraft.client.gui.screens.Screen;

/**
 * Optional product-specific vanilla UI ownership. ThemeCore stays generic, but
 * lets an installed product shell take precedence without a compile dependency.
 */
public final class VanillaUiProductOwnership {
    private static final String ASHFALL_THEME_CLASS =
            "com.knoxhack.echoashfallprotocol.client.screen.EchoVanillaScreenTheme";

    private VanillaUiProductOwnership() {
    }

    public static boolean productOwnsScreen(Screen screen) {
        if (screen == null) {
            return false;
        }
        try {
            Class<?> type = Class.forName(ASHFALL_THEME_CLASS);
            Method method = type.getMethod("ownsScreen", Screen.class);
            Object result = method.invoke(null, screen);
            return Boolean.TRUE.equals(result);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    public static boolean productOwnsLoadingOverlay() {
        try {
            Class<?> type = Class.forName(ASHFALL_THEME_CLASS);
            Method method = type.getMethod("ownsLoadingOverlay");
            Object result = method.invoke(null);
            return Boolean.TRUE.equals(result);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return false;
        }
    }
}
