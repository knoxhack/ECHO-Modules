package com.knoxhack.echoashfallprotocol.client;

import com.knoxhack.echoashfallprotocol.Config;
import com.knoxhack.echoashfallprotocol.client.screen.EchoMainMenuScreen;
import com.knoxhack.echoashfallprotocol.client.screen.EchoNativeMainMenuScreen;
import com.knoxhack.echoashfallprotocol.client.screen.EchoVanillaScreenTheme;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import java.util.concurrent.atomic.AtomicBoolean;

public final class EchoMainMenuEvents {
    private static final AtomicBoolean NATIVE_MENU_PROJECTED = new AtomicBoolean(false);

    private EchoMainMenuEvents() {
    }

    public static void onScreenOpening(Object event) {
        Screen newScreen = screen(event, "getNewScreen");
        if (echoMenuEnabled() && newScreen instanceof TitleScreen) {
            try {
                if (nativeLoaderActive()) {
                    if (NATIVE_MENU_PROJECTED.compareAndSet(false, true)) {
                        setScreen(event, new EchoNativeMainMenuScreen());
                    }
                } else {
                    setScreen(event, new EchoMainMenuScreen());
                }
            } catch (RuntimeException ignored) {
                // Leave the vanilla title screen alone if the custom shell cannot be created.
            }
        }
    }

    public static void onScreenBackground(Object event) {
        try {
            EchoVanillaScreenTheme.renderBackground(screen(event, "getScreen"), graphics(event), partialTick(event));
        } catch (RuntimeException ignored) {
            // Preserve vanilla screens if the terminal skin cannot be drawn.
        }
    }

    public static void onScreenPostRender(Object event) {
        try {
            EchoVanillaScreenTheme.renderForeground(screen(event, "getScreen"), graphics(event), partialTick(event));
        } catch (RuntimeException ignored) {
            // Preserve vanilla screens if the terminal skin cannot be drawn.
        }
    }

    private static Screen screen(Object event, String methodName) {
        try {
            Object value = event == null ? null : event.getClass().getMethod(methodName).invoke(event);
            return value instanceof Screen screen ? screen : null;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return null;
        }
    }

    private static void setScreen(Object event, Screen screen) {
        if (event == null || screen == null) {
            return;
        }
        try {
            for (java.lang.reflect.Method method : event.getClass().getMethods()) {
                if ("setNewScreen".equals(method.getName()) && method.getParameterCount() == 1) {
                    method.invoke(event, screen);
                    return;
                }
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
    }

    private static GuiGraphicsExtractor graphics(Object event) {
        try {
            Object value = event == null ? null : event.getClass().getMethod("getGuiGraphics").invoke(event);
            return value instanceof GuiGraphicsExtractor graphics ? graphics : null;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return null;
        }
    }

    private static float partialTick(Object event) {
        try {
            Object value = event == null ? null : event.getClass().getMethod("getPartialTick").invoke(event);
            return value instanceof Number number ? number.floatValue() : 0.0F;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return 0.0F;
        }
    }

    private static boolean nativeLoaderActive() {
        return dev.echo.nativeplatform.contracts.EchoNativeClientRuntimeEnvironment.isNativeLoaderActive();
    }

    private static boolean echoMenuEnabled() {
        if (nativeLoaderActive()) {
            return true;
        }
        return Config.ENABLE_ECHO_MAIN_MENU.get();
    }
}
