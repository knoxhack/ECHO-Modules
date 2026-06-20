package com.knoxhack.echoashfallprotocol.client.screen;

import com.knoxhack.echoashfallprotocol.Config;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeEnvironmentBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;

/**
 * Safe Ashfall terminal skin for vanilla menu and world-flow screens.
 */
public final class EchoVanillaScreenTheme {
    private EchoVanillaScreenTheme() {
    }

    public static boolean isEnabled() {
        try {
            return Config.ENABLE_ECHO_MAIN_MENU.get();
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    public static boolean shouldTheme(Screen screen) {
        return isEnabled() && surface(screen).owned();
    }

    public static boolean shouldThemeButton(Screen screen) {
        return shouldTheme(screen);
    }

    public static boolean ownsScreen(Screen screen) {
        return shouldTheme(screen);
    }

    public static boolean ownsLoadingOverlay() {
        if (EchoNativeRuntimeEnvironmentBridge.isNativeLoaderActive()) {
            return true;
        }
        try {
            return Config.ENABLE_ECHO_LOADING_SCREEN.get();
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    public static boolean renderButton(Button button, GuiGraphicsExtractor graphics) {
        Screen screen = Minecraft.getInstance().screen;
        if (!shouldThemeButton(screen)) {
            return false;
        }

        int x = button.getX();
        int y = button.getY();
        int width = button.getWidth();
        int height = button.getHeight();
        boolean selected = button.isHoveredOrFocused();
        boolean active = button.active;
        int border = active ? (selected ? EchoTerminalStyle.CYAN : EchoTerminalStyle.LINE) : EchoTerminalStyle.LINE_DIM;
        int fill = active ? (selected ? 0xC31A0A3B : 0xA507151D) : 0x78101820;
        int textColor = active ? (selected ? EchoTerminalStyle.TEXT : EchoTerminalStyle.CYAN) : EchoTerminalStyle.MUTED;

        graphics.fill(x, y, x + width, y + height, fill);
        graphics.outline(x, y, width, height, border);
        if (width > 12 && height > 6) {
            graphics.fill(x + 2, y + 2, x + 5, y + height - 2, active ? 0xAA38DFF4 : 0x5538DFF4);
            graphics.fill(x + width - 6, y + 2, x + width - 3, y + height - 2, selected ? 0x998B4DFF : 0x442E8E9D);
            if (selected && Config.TERMINAL_ANIMATION.get()) {
                int ticks = screen == null ? 0 : screenTicks(screen);
                int sweepWidth = 8 + ticks % Math.max(9, width - 18);
                graphics.fill(x + 8, y + height - 3, Math.min(x + width - 8, x + sweepWidth), y + height - 2, 0xCC66E8FF);
            }
        }

        Font font = Minecraft.getInstance().font;
        String label = EchoTerminalStyle.clipToWidth(font, button.getMessage().getString(), Math.max(1, width - 14));
        int labelX = x + Math.max(7, (width - font.width(label)) / 2);
        int labelY = y + Math.max(1, (height - 8) / 2);
        graphics.text(font, label, labelX, labelY, textColor, false);
        return true;
    }

    public static void renderBackground(Screen screen, GuiGraphicsExtractor graphics, float partialTick) {
        if (!shouldTheme(screen)) {
            return;
        }

        int ticks = screenTicks(screen);
        EchoAshfallScreenChrome.renderBackground(screen, graphics, surface(screen), ticks, partialTick);
    }

    public static void renderForeground(Screen screen, GuiGraphicsExtractor graphics, float partialTick) {
        if (!shouldTheme(screen)) {
            return;
        }

        EchoAshfallScreenChrome.renderForeground(screen, graphics, surface(screen), screenTicks(screen));
    }

    private static EchoAshfallScreenSurface surface(Screen screen) {
        return EchoAshfallScreenSurface.classify(screen);
    }

    private static int screenTicks(Screen screen) {
        return Math.max(0, System.identityHashCode(screen) + (int) (System.currentTimeMillis() / 50L));
    }
}
