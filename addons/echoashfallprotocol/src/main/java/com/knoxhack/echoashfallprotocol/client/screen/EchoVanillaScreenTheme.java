package com.knoxhack.echoashfallprotocol.client.screen;

import com.knoxhack.echoashfallprotocol.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;

/**
 * Safe ECHO terminal skin for vanilla pre-game screens.
 */
public final class EchoVanillaScreenTheme {
    private static final String WORLD_SELECTION_PACKAGE = "net.minecraft.client.gui.screens.worldselection.";
    private static final String MULTIPLAYER_PACKAGE = "net.minecraft.client.gui.screens.multiplayer.";
    private static final String DIALOG_PACKAGE = "net.minecraft.client.gui.screens.dialog.";
    private static final String OPTIONS_PACKAGE = "net.minecraft.client.gui.screens.options.";

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
        return isEnabled() && isTerminalPreGameScreen(screen);
    }

    public static boolean shouldThemeButton(Screen screen) {
        return shouldTheme(screen);
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

        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        int ticks = screenTicks(screen);
        EchoTerminalBackgrounds.render(graphics, EchoTerminalBackgrounds.forScreen(screen), width, height, ticks, partialTick);

        String label = screenLabel(screen);
        int margin = EchoTerminalStyle.clamp(width / 32, 12, 34);
        int stripHeight = 31;
        graphics.fill(margin, 14, width - margin, 14 + stripHeight, 0xB20A1620);
        graphics.outline(margin, 14, width - margin * 2, stripHeight, EchoTerminalStyle.LINE);
        graphics.fill(margin + 1, 15, width - margin - 1, 28, 0x8120024A);
        graphics.fill(margin + 12, 44, Math.min(width - margin - 12, margin + 220), 45,
                EchoTerminalStyle.pulseColor(ticks, 0x5038DFF4, 0xB466E8FF, 48));

        Font font = Minecraft.getInstance().font;
        EchoTerminalStyle.text(graphics, font, "ECHO TERMINAL // " + label, margin + 14, 19, EchoTerminalStyle.CYAN, 1.0F);
        String title = screenTitle(screen);
        EchoTerminalStyle.text(graphics, font, title, margin + 14, 33, EchoTerminalStyle.TEXT, 1.0F);

        String status = statusLine(screen);
        int statusWidth = font.width(status);
        EchoTerminalStyle.text(graphics, font, status, Math.max(margin + 14, width - margin - statusWidth - 14), 25,
                statusColor(screen), 1.0F);

        if (height > 260 && width > 520) {
            renderDiagnostics(graphics, font, screen, margin, height, ticks);
        }
    }

    public static void renderForeground(Screen screen, GuiGraphicsExtractor graphics, float partialTick) {
        if (!shouldTheme(screen)) {
            return;
        }

        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        if (height < 180) {
            return;
        }

        Font font = Minecraft.getInstance().font;
        int margin = EchoTerminalStyle.clamp(width / 32, 12, 34);
        String footer = footerLine(screen);
        EchoTerminalStyle.text(graphics, font, footer, margin, height - 18, EchoTerminalStyle.CYAN_DIM, 1.0F);

        String right = "VANILLA CONTROL PATH PRESERVED";
        int rightWidth = font.width(right);
        if (rightWidth + margin * 2 + font.width(footer) + 16 < width) {
            EchoTerminalStyle.text(graphics, font, right, width - margin - rightWidth, height - 18, EchoTerminalStyle.MUTED, 1.0F);
        }
    }

    private static void renderDiagnostics(GuiGraphicsExtractor graphics, Font font, Screen screen, int margin, int height, int ticks) {
        int panelWidth = 206;
        int top = Math.max(58, height / 2 - 72);
        int bottom = Math.min(height - 38, top + 132);
        if (bottom - top < 86) {
            return;
        }

        graphics.fill(margin, top, margin + panelWidth, bottom, 0x9A061018);
        graphics.outline(margin, top, panelWidth, bottom - top, EchoTerminalStyle.LINE_DIM);
        EchoTerminalStyle.text(graphics, font, ":: SESSION DIAGNOSTICS", margin + 10, top + 9, EchoTerminalStyle.CYAN_DIM, 1.0F);
        EchoTerminalStyle.text(graphics, font, "ROUTE: " + routeLine(screen), margin + 10, top + 28, EchoTerminalStyle.TEXT, 1.0F);
        EchoTerminalStyle.text(graphics, font, "SHELL: TERMINAL", margin + 10, top + 43, EchoTerminalStyle.GREEN, 1.0F);
        EchoTerminalStyle.text(graphics, font, "INPUT: VANILLA", margin + 10, top + 58, EchoTerminalStyle.GREEN, 1.0F);

        int meterLeft = margin + 10;
        int meterTop = bottom - 22;
        int meterWidth = panelWidth - 20;
        graphics.outline(meterLeft, meterTop, meterWidth, 8, EchoTerminalStyle.LINE_DIM);
        int fill = 12 + ticks % Math.max(13, meterWidth - 16);
        graphics.fill(meterLeft + 2, meterTop + 2, Math.min(meterLeft + meterWidth - 2, meterLeft + fill), meterTop + 6, 0xB766E8FF);
    }

    private static boolean isTerminalPreGameScreen(Screen screen) {
        if (screen == null || screen instanceof EchoMainMenuScreen) {
            return false;
        }

        String name = screen.getClass().getName();
        if (name.startsWith(WORLD_SELECTION_PACKAGE) || name.startsWith(MULTIPLAYER_PACKAGE) || name.startsWith(DIALOG_PACKAGE)) {
            return true;
        }

        if (isNoWorldOptionsScreen(name)) {
            return true;
        }

        return name.equals("net.minecraft.client.gui.screens.CreateFlatWorldScreen")
                || name.endsWith(".CreateWorldScreen")
                || name.equals("net.minecraft.client.gui.screens.CreateBuffetWorldScreen")
                || name.equals("net.minecraft.client.gui.screens.PresetFlatWorldScreen")
                || name.equals("net.minecraft.client.gui.screens.DirectJoinServerScreen")
                || name.equals("net.minecraft.client.gui.screens.ManageServerScreen")
                || name.equals("net.minecraft.client.gui.screens.ConnectScreen")
                || name.equals("net.minecraft.client.gui.screens.LevelLoadingScreen")
                || name.equals("net.minecraft.client.gui.screens.ProgressScreen")
                || name.endsWith(".FileFixerProgressScreen")
                || name.endsWith(".OptimizeWorldScreen")
                || name.equals("net.minecraft.client.gui.screens.BackupConfirmScreen")
                || name.equals("net.minecraft.client.gui.screens.ConfirmScreen")
                || name.equals("net.minecraft.client.gui.screens.AlertScreen")
                || name.equals("net.minecraft.client.gui.screens.DisconnectedScreen")
                || name.endsWith(".WorldCreationGameRulesScreen")
                || name.endsWith(".ExperimentsScreen");
    }

    private static boolean isNoWorldOptionsScreen(String name) {
        if (Minecraft.getInstance().level != null) {
            return false;
        }

        return name.startsWith(OPTIONS_PACKAGE)
                || name.equals("net.minecraft.client.gui.screens.LanguageSelectScreen")
                || name.equals("net.minecraft.client.gui.screens.packs.PackSelectionScreen");
    }

    private static String screenLabel(Screen screen) {
        String name = screen.getClass().getName();
        if (name.equals("net.minecraft.client.gui.screens.LevelLoadingScreen")) {
            return "LOADING TERRAIN";
        }
        if (name.equals("net.minecraft.client.gui.screens.ProgressScreen")
                || name.endsWith(".FileFixerProgressScreen")
                || name.endsWith(".OptimizeWorldScreen")) {
            return "WORLD MAINTENANCE";
        }
        if (name.startsWith(MULTIPLAYER_PACKAGE)
                || name.equals("net.minecraft.client.gui.screens.DirectJoinServerScreen")
                || name.equals("net.minecraft.client.gui.screens.ManageServerScreen")
                || name.equals("net.minecraft.client.gui.screens.ConnectScreen")
                || name.equals("net.minecraft.client.gui.screens.DisconnectedScreen")) {
            return "MULTIPLAYER UPLINK";
        }
        if (isNoWorldOptionsScreen(name)) {
            return "SYSTEM OPTIONS";
        }
        if (name.endsWith(".CreateWorldScreen")
                || name.equals("net.minecraft.client.gui.screens.CreateFlatWorldScreen")
                || name.equals("net.minecraft.client.gui.screens.CreateBuffetWorldScreen")
                || name.equals("net.minecraft.client.gui.screens.PresetFlatWorldScreen")
                || name.endsWith(".WorldCreationGameRulesScreen")
                || name.endsWith(".ExperimentsScreen")) {
            return "CREATE SIMULATION";
        }
        if (name.startsWith(DIALOG_PACKAGE)
                || name.equals("net.minecraft.client.gui.screens.ConfirmScreen")
                || name.equals("net.minecraft.client.gui.screens.AlertScreen")
                || name.equals("net.minecraft.client.gui.screens.BackupConfirmScreen")) {
            return "ECHO CONFIRMATION";
        }
        return "WORLD ARCHIVE";
    }

    private static String screenTitle(Screen screen) {
        String value = screen.getTitle().getString();
        if (value == null || value.isBlank()) {
            value = screenLabel(screen);
        }
        return EchoTerminalStyle.clipToWidth(Minecraft.getInstance().font, value.toUpperCase(), Math.max(80, Minecraft.getInstance().getWindow().getGuiScaledWidth() / 2));
    }

    private static String statusLine(Screen screen) {
        String label = screenLabel(screen);
        if ("LOADING TERRAIN".equals(label) || "WORLD MAINTENANCE".equals(label)) {
            return "BOOT VECTOR: SYNCING";
        }
        if ("MULTIPLAYER UPLINK".equals(label)) {
            return "LINK: STANDBY";
        }
        if ("SYSTEM OPTIONS".equals(label)) {
            return "CONFIG: READY";
        }
        return "STATUS: STABLE";
    }

    private static int statusColor(Screen screen) {
        String label = screenLabel(screen);
        if ("LOADING TERRAIN".equals(label) || "WORLD MAINTENANCE".equals(label)) {
            return EchoTerminalStyle.AMBER;
        }
        if ("MULTIPLAYER UPLINK".equals(label)) {
            return EchoTerminalStyle.CYAN;
        }
        if ("SYSTEM OPTIONS".equals(label)) {
            return EchoTerminalStyle.CYAN;
        }
        return EchoTerminalStyle.GREEN;
    }

    private static String routeLine(Screen screen) {
        String label = screenLabel(screen);
        if ("MULTIPLAYER UPLINK".equals(label)) {
            return "REMOTE";
        }
        if ("CREATE SIMULATION".equals(label)) {
            return "NEW WORLD";
        }
        if ("SYSTEM OPTIONS".equals(label)) {
            return "CONFIG";
        }
        if ("LOADING TERRAIN".equals(label)) {
            return "CHUNK LOAD";
        }
        return "LOCAL";
    }

    private static String footerLine(Screen screen) {
        String label = screenLabel(screen);
        if ("MULTIPLAYER UPLINK".equals(label)) {
            return "ECHO LINK SHELL ACTIVE.";
        }
        if ("CREATE SIMULATION".equals(label)) {
            return "SIMULATION PARAMETERS REMAIN VANILLA-SAFE.";
        }
        if ("SYSTEM OPTIONS".equals(label)) {
            return "CONFIGURATION SHELL ACTIVE.";
        }
        if ("LOADING TERRAIN".equals(label) || "WORLD MAINTENANCE".equals(label)) {
            return "ECHO LISTENS WHILE THE WORLD LOADS.";
        }
        return "ARCHIVE SHELL ACTIVE.";
    }

    private static int screenTicks(Screen screen) {
        return Math.max(0, System.identityHashCode(screen) + (int) (System.currentTimeMillis() / 50L));
    }
}
