package com.knoxhack.echoashfallprotocol.client.screen;

import java.util.function.IntSupplier;

import com.knoxhack.echoashfallprotocol.Config;
import com.knoxhack.echocore.api.EchoRuntimeModules;
import com.knoxhack.echocore.client.ui.EchoNativeHubScreen;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;

/**
 * ECHO-branded title screen rendered as an ECHO terminal shell.
 */
public class EchoMainMenuScreen extends Screen {
    private static final int BG = EchoTerminalStyle.BG;
    private static final int PANEL = EchoTerminalStyle.PANEL;
    private static final int PANEL_SOFT = EchoTerminalStyle.PANEL_SOFT;
    private static final int LINE = EchoTerminalStyle.LINE;
    private static final int LINE_DIM = EchoTerminalStyle.LINE_DIM;
    private static final int CYAN = EchoTerminalStyle.CYAN;
    private static final int CYAN_DIM = EchoTerminalStyle.CYAN_DIM;
    private static final int GREEN = EchoTerminalStyle.GREEN;
    private static final int AMBER = EchoTerminalStyle.AMBER;
    private static final int RED = EchoTerminalStyle.RED;
    private static final int TEXT = EchoTerminalStyle.TEXT;
    private static final int MUTED = EchoTerminalStyle.MUTED;

    private static final String[] ROTATING_ARCHIVES = {
            "Gridfall left the surface quiet enough for buried relays to answer again.",
            "Nexus telemetry no longer points down. It bends through orbit and waits.",
            "Recovered ECHO terminals agree on one command: rebuild below, then listen above.",
            "The orbital channel is not a rescue signal. It is a door with memory on the other side."
    };
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_TOP_OFFSET = 48;
    private static final int BUTTON_BOTTOM_PADDING = 12;
    private static final int BUTTON_COUNT = 5;
    private static final int COMPACT_BUTTON_GAP = 23;
    private static final int WIDE_BUTTON_GAP = 25;

    private int ticks;

    public EchoMainMenuScreen() {
        super(Component.literal("ECHO Terminal"));
    }

    @Override
    protected void init() {
        int menuWidth = commandWidth();
        int menuX = commandX(menuWidth);
        int menuY = commandY();
        int gap = commandButtonGap();
        int buttonY = menuY + BUTTON_TOP_OFFSET;

        this.addRenderableWidget(terminalButton("[ SINGLEPLAYER ]", button ->
                this.minecraft.setScreen(new SelectWorldScreen(this)), menuX + 18, buttonY, menuWidth - 36));
        this.addRenderableWidget(terminalButton("[ MULTIPLAYER ]", button ->
                this.minecraft.setScreen(new JoinMultiplayerScreen(this)), menuX + 18, buttonY + gap, menuWidth - 36));
        this.addRenderableWidget(terminalButton("[ MODS ]", button ->
                this.minecraft.setScreen(new EchoNativeHubScreen()), menuX + 18, buttonY + gap * 2, menuWidth - 36));
        this.addRenderableWidget(terminalButton("[ OPTIONS ]", button ->
                this.minecraft.setScreen(new OptionsScreen(this, this.minecraft.options, false)), menuX + 18, buttonY + gap * 3, menuWidth - 36));
        this.addRenderableWidget(terminalButton("[ QUIT ]", button ->
                this.minecraft.stop(), menuX + 18, buttonY + gap * 4, menuWidth - 36));
    }

    @Override
    public void tick() {
        super.tick();
        this.ticks++;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        renderTerminalBackground(graphics, partialTick);
        renderArchivePanel(graphics, partialTick);
        renderCommandPanel(graphics, partialTick);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        renderFooter(graphics, partialTick);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void renderTerminalBackground(GuiGraphicsExtractor graphics, float partialTick) {
        EchoTerminalBackgrounds.render(
                graphics, EchoTerminalBackgrounds.Plate.MAIN_MENU, this.width, this.height, this.ticks, partialTick);
    }

    private void renderArchivePanel(GuiGraphicsExtractor graphics, float partialTick) {
        boolean compact = compactLayout();
        int margin = margin();
        int commandWidth = commandWidth();
        int commandX = commandX(commandWidth);
        int left = margin;
        int top = compact ? 24 : 34;
        int right = compact ? this.width - margin : commandX - margin;
        int bottom = compact ? Math.min(this.height - margin, commandY() - 10) : this.height - 42;

        if (bottom - top < 88) {
            if (compact) {
                return;
            }
            bottom = Math.min(this.height - margin, top + 88);
        }

        graphics.fill(left, top, right, bottom, 0xBA071019);
        graphics.outline(left, top, right - left, bottom - top, LINE);
        graphics.fill(left + 1, top + 1, right - 1, top + 21, 0x8C19013A);
        graphics.fill(left + 12, top + 17, left + Math.min(180, right - left - 12), top + 18, pulseColor(0x5538DFF4, 0xBB66E8FF, 42));

        boolean cursor = ((this.ticks / 18) % 2) == 0;
        EchoTerminalStyle.pixelText(graphics, "ECHO", left + 14, top + 28, CYAN, 1.0F, compact ? 2 : 3);
        text(graphics, "TERMINAL // ASHFALL PROTOCOL" + (cursor ? "_" : ""), left + 14, top + 7, CYAN);
        text(graphics, "NEXUS RECOVERY INTERFACE", left + 14, top + (compact ? 51 : 61), TEXT);
        text(graphics, "MAIN MENU BOOT VECTOR: STABLE", left + 14, top + (compact ? 65 : 75), GREEN);

        int contentWidth = Math.max(120, right - left - 28);
        int y = top + (compact ? 86 : 104);
        drawSection(graphics, left + 14, y, "WORLD STATUS", contentWidth);
        y += 18;
        drawStatus(graphics, left + 18, y, "Gridfall", "surface network damaged; recovery routes online", AMBER, contentWidth);
        y += 15;
        drawStatus(graphics, left + 18, y, "Nexus signal", "post-choice telemetry present", CYAN, contentWidth);
        y += 15;
        drawStatus(graphics, left + 18, y, "Drone link", "field companion handshake waiting", GREEN, contentWidth);

        boolean echo7Loaded = EchoRuntimeModules.isLoaded("echoorbitalremnants");
        y += compact ? 20 : 30;
        drawSection(graphics, left + 14, y, "ORBITAL REMNANTS", contentWidth);
        y += 18;
        drawStatus(graphics, left + 18, y, "Orbital uplink", echo7Loaded ? "PRESENT" : "OPTIONAL MODULE ABSENT", echo7Loaded ? GREEN : MUTED, contentWidth);
        y += 15;
        drawStatus(graphics, left + 18, y, "Orbital uplink", echo7Loaded ? "addon chapter detected" : "standalone ECHO mode", echo7Loaded ? CYAN : MUTED, contentWidth);

        if (!compact && y + 60 < bottom) {
            y += 30;
            drawSection(graphics, left + 14, y, "ARCHIVE FEED", contentWidth);
            y += 18;
            String archive = ROTATING_ARCHIVES[(this.ticks / 120) % ROTATING_ARCHIVES.length];
            drawWrapped(graphics, archive, left + 18, y, contentWidth - 8, 3, TEXT);
        }
    }

    private void renderCommandPanel(GuiGraphicsExtractor graphics, float partialTick) {
        int width = commandWidth();
        int left = commandX(width);
        int top = commandY();
        int height = commandHeight();
        int bottom = Math.min(this.height - margin(), top + height);

        graphics.fill(left, top, left + width, bottom, 0xB00A1620);
        graphics.outline(left, top, width, bottom - top, LINE);
        graphics.fill(left + 1, top + 1, left + width - 1, top + 29, 0x8620024A);
        graphics.fill(left + 12, top + 30, left + width - 12, top + 31, pulseColor(0x5038DFF4, 0xB466E8FF, 48));

        text(graphics, "ECHO BUS", left + 18, top + 10, CYAN);
        text(graphics, "SELECT BOOT SIGNAL", left + 18, top + 24, MUTED);

        int meterTop = bottom - 32;
        if (meterTop > top + 160) {
            text(graphics, "ECHO BUS", left + 18, meterTop, CYAN_DIM);
            int meterLeft = left + 74;
            int meterRight = left + width - 18;
            graphics.outline(meterLeft, meterTop - 2, meterRight - meterLeft, 8, LINE_DIM);
            int pulseWidth = 18 + (this.ticks % Math.max(22, meterRight - meterLeft - 18));
            graphics.fill(meterLeft + 2, meterTop, Math.min(meterRight - 2, meterLeft + pulseWidth), meterTop + 4, 0xB766E8FF);
        }
    }

    private void renderFooter(GuiGraphicsExtractor graphics, float partialTick) {
        int y = this.height - 26;
        if (y < 8) {
            return;
        }
        String version = "Minecraft " + SharedConstants.getCurrentVersion().name();
        text(graphics, version, 22, y, MUTED);
        String right = "ECHO listens while the world reloads.";
        int rightWidth = this.font.width(right);
        text(graphics, right, Math.max(22, this.width - rightWidth - 22), y, CYAN_DIM);
    }

    private EchoTerminalButton terminalButton(String label, Button.OnPress action, int x, int y, int width) {
        return new EchoTerminalButton(x, y, width, 20, label, action, () -> this.ticks);
    }

    private void drawSection(GuiGraphicsExtractor graphics, int x, int y, String label, int width) {
        text(graphics, ":: " + label, x, y, CYAN);
        graphics.fill(x, y + 11, x + Math.max(40, Math.min(width, 230)), y + 12, LINE_DIM);
    }

    private void drawStatus(GuiGraphicsExtractor graphics, int x, int y, String label, String value, int color, int width) {
        int labelWidth = compactLayout() ? 76 : 88;
        text(graphics, label.toUpperCase() + ":", x, y, CYAN_DIM);
        String clipped = clipToWidth(value, Math.max(40, width - labelWidth - 6));
        text(graphics, clipped, x + labelWidth, y, color);
    }

    private int drawWrapped(GuiGraphicsExtractor graphics, String value, int x, int y, int width, int maxLines, int color) {
        String remaining = value.trim();
        int line = 0;
        while (!remaining.isEmpty() && line < maxLines) {
            String next = takeLine(remaining, width);
            text(graphics, next, x, y + line * 11, color);
            remaining = remaining.substring(next.length()).trim();
            line++;
        }
        return y + line * 11;
    }

    private String takeLine(String value, int width) {
        if (this.font.width(value) <= width) {
            return value;
        }
        String[] words = value.split(" ");
        String line = "";
        for (String word : words) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (this.font.width(candidate) > width) {
                return line.isEmpty() ? clipToWidth(word, width) : line;
            }
            line = candidate;
        }
        return line;
    }

    private String clipToWidth(String value, int width) {
        if (this.font.width(value) <= width) {
            return value;
        }
        String suffix = "...";
        int limit = Math.max(1, value.length() - 1);
        while (limit > 1 && this.font.width(value.substring(0, limit) + suffix) > width) {
            limit--;
        }
        return value.substring(0, limit) + suffix;
    }

    private void text(GuiGraphicsExtractor graphics, String value, int x, int y, int color) {
        graphics.text(this.font, value, x, y, color, false);
    }

    private boolean compactLayout() {
        return this.width < 640 || this.height < 330;
    }

    private int margin() {
        return clamp(this.width / 32, 12, 34);
    }

    private int commandWidth() {
        return clamp(this.width / 3, 190, 236);
    }

    private int commandHeight() {
        int desired = compactLayout() ? 190 : 220;
        int minimum = commandButtonAreaHeight();
        int available = Math.max(minimum, this.height - margin() * 2);
        return clamp(desired, minimum, available);
    }

    private int commandX(int commandWidth) {
        if (compactLayout()) {
            return Math.max(margin(), (this.width - commandWidth) / 2);
        }
        return Math.max(margin(), this.width - commandWidth - margin());
    }

    private int commandY() {
        int desired = compactLayout() ? this.height / 2 - 38 : this.height / 2 - 108;
        int minTop = margin();
        int maxTop = Math.max(minTop, this.height - commandHeight() - margin());
        return clamp(desired, minTop, maxTop);
    }

    private int commandButtonGap() {
        int defaultGap = compactLayout() ? COMPACT_BUTTON_GAP : WIDE_BUTTON_GAP;
        int available = Math.max(0, this.height - margin() * 2 - BUTTON_TOP_OFFSET - BUTTON_HEIGHT - BUTTON_BOTTOM_PADDING);
        return clamp(available / Math.max(1, BUTTON_COUNT - 1), 18, defaultGap);
    }

    private int commandButtonAreaHeight() {
        return BUTTON_TOP_OFFSET + commandButtonGap() * (BUTTON_COUNT - 1) + BUTTON_HEIGHT + BUTTON_BOTTOM_PADDING;
    }

    private int pulseColor(int low, int high, int period) {
        return EchoTerminalStyle.pulseColor(this.ticks, low, high, period);
    }

    private static int clamp(int value, int min, int max) {
        return EchoTerminalStyle.clamp(value, min, max);
    }

    private static int alphaRgb(int alpha, int rgb) {
        return EchoTerminalStyle.alphaRgb(alpha, rgb);
    }

    private static final class EchoTerminalButton extends Button {
        private final String label;
        private final IntSupplier ticks;

        private EchoTerminalButton(int x, int y, int width, int height, String label, OnPress onPress, IntSupplier ticks) {
            super(x, y, width, height, Component.literal(label), onPress, DEFAULT_NARRATION);
            this.label = label;
            this.ticks = ticks;
            this.setFGColor(CYAN);
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            boolean selected = this.isHoveredOrFocused();
            int x = this.getX();
            int y = this.getY();
            int width = this.getWidth();
            int height = this.getHeight();
            int tick = this.ticks.getAsInt();
            int fill = selected ? 0xC31A0A3B : 0xA507151D;
            int border = selected ? CYAN : LINE;
            int text = selected ? TEXT : CYAN;

            graphics.fill(x, y, x + width, y + height, fill);
            graphics.outline(x, y, width, height, border);
            graphics.fill(x + 2, y + 2, x + 5, y + height - 2, selected ? 0xFF66E8FF : 0x7738DFF4);
            graphics.fill(x + width - 6, y + 2, x + width - 3, y + height - 2, selected ? 0xBB8B4DFF : 0x5538DFF4);

            if (selected && Config.TERMINAL_ANIMATION.get()) {
                int sweep = 10 + (tick % Math.max(12, width - 24));
                graphics.fill(x + 8, y + height - 3, Math.min(x + width - 8, x + sweep), y + height - 2, 0xCC66E8FF);
            }

            Minecraft minecraft = Minecraft.getInstance();
            int labelWidth = minecraft.font.width(this.label);
            int textX = x + Math.max(10, (width - labelWidth) / 2);
            int textY = y + 6;
            graphics.text(minecraft.font, this.label, textX, textY, text, false);
        }
    }
}
