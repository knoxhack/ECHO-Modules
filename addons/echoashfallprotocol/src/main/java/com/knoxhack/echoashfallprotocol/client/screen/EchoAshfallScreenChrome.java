package com.knoxhack.echoashfallprotocol.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;

/**
 * Shared Ashfall shell drawing for vanilla menu surfaces.
 */
final class EchoAshfallScreenChrome {
    private EchoAshfallScreenChrome() {
    }

    static void renderBackground(
            Screen screen,
            GuiGraphicsExtractor graphics,
            EchoAshfallScreenSurface surface,
            int ticks,
            float partialTick
    ) {
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        EchoTerminalBackgrounds.render(graphics, surface.plate(), width, height, ticks, partialTick);

        int margin = margin(width);
        renderHeader(screen, graphics, surface, margin, ticks);
        renderPrimaryPlate(screen, graphics, surface, margin, ticks);
        renderStatusRail(graphics, surface, margin, ticks);
    }

    static void renderForeground(
            Screen screen,
            GuiGraphicsExtractor graphics,
            EchoAshfallScreenSurface surface,
            int ticks
    ) {
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        if (height < 160) {
            return;
        }

        Font font = Minecraft.getInstance().font;
        int margin = margin(width);
        String left = surface.footer();
        EchoTerminalStyle.text(graphics, font, EchoTerminalStyle.clipToWidth(font, left, Math.max(80, width / 2)),
                margin, height - 18, EchoTerminalStyle.CYAN_DIM, 1.0F);

        String right = EchoAshfallScreenSurface.isInWorld()
                ? "IN-WORLD ROUTE // INPUT PRESERVED"
                : "ASHFALL ROUTE // INPUT PRESERVED";
        int rightWidth = font.width(right);
        if (rightWidth < width - margin * 2 - 16) {
            EchoTerminalStyle.text(graphics, font, right, width - margin - rightWidth, height - 18,
                    EchoTerminalStyle.MUTED, 1.0F);
        }

        if (width > 580 && height > 260) {
            int pulseWidth = 46 + ticks % 96;
            graphics.fill(width - margin - 150, height - 26, width - margin - 150 + pulseWidth, height - 25,
                    EchoTerminalStyle.pulseColor(ticks, 0x5038DFF4, 0xA866E8FF, 42));
        }
    }

    private static void renderHeader(
            Screen screen,
            GuiGraphicsExtractor graphics,
            EchoAshfallScreenSurface surface,
            int margin,
            int ticks
    ) {
        int width = graphics.guiWidth();
        Font font = Minecraft.getInstance().font;
        int headerTop = 14;
        int headerHeight = graphics.guiHeight() < 260 ? 34 : 42;
        int left = margin;
        int right = width - margin;

        graphics.fill(left, headerTop, right, headerTop + headerHeight, 0xC1061119);
        graphics.outline(left, headerTop, right - left, headerHeight, EchoTerminalStyle.LINE);
        graphics.fill(left + 1, headerTop + 1, right - 1, headerTop + 16, 0x7620024A);
        graphics.fill(left + 14, headerTop + headerHeight - 3,
                Math.min(right - 14, left + 180 + ticks % Math.max(48, right - left - 220)),
                headerTop + headerHeight - 2,
                EchoTerminalStyle.pulseColor(ticks, 0x5538DFF4, 0xBB66E8FF, 48));

        EchoTerminalStyle.text(graphics, font, "ECHO TERMINAL // " + surface.label(), left + 14, headerTop + 7,
                EchoTerminalStyle.CYAN, 1.0F);
        String title = screenTitle(screen, surface, font, Math.max(96, (right - left) / 2));
        EchoTerminalStyle.text(graphics, font, title, left + 14, headerTop + 23, EchoTerminalStyle.TEXT, 1.0F);

        String status = surface.status();
        int statusWidth = font.width(status);
        int statusX = Math.max(left + 14, right - statusWidth - 14);
        EchoTerminalStyle.text(graphics, font, status, statusX, headerTop + 15, surface.statusColor(), 1.0F);
    }

    private static void renderPrimaryPlate(
            Screen screen,
            GuiGraphicsExtractor graphics,
            EchoAshfallScreenSurface surface,
            int margin,
            int ticks
    ) {
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        Bounds bounds = contentBounds(screen, graphics, surface, margin);
        int panel = surface.compactCenterPanel() ? 0xC0061119 : 0xA8061119;

        graphics.fill(bounds.left, bounds.top, bounds.right, bounds.bottom, panel);
        graphics.outline(bounds.left, bounds.top, bounds.width(), bounds.height(), EchoTerminalStyle.LINE_DIM);
        graphics.fill(bounds.left + 1, bounds.top + 1, bounds.right - 1,
                Math.min(bounds.bottom - 1, bounds.top + 22), 0x6620024A);
        graphics.fill(bounds.left + 12, bounds.top + 19,
                Math.min(bounds.right - 12, bounds.left + Math.max(70, bounds.width() / 3)),
                bounds.top + 20, EchoTerminalStyle.pulseColor(ticks, 0x4038DFF4, 0xA038DFF4, 40));

        if (surface == EchoAshfallScreenSurface.WORLD_ARCHIVE || surface == EchoAshfallScreenSurface.CREATE_WORLD) {
            renderArchiveGrid(graphics, bounds, ticks);
        } else if (surface == EchoAshfallScreenSurface.PAUSE) {
            renderPauseBackplate(graphics, bounds, ticks);
        } else if (surface.loadingLike()) {
            renderLoadingBackplate(graphics, bounds, ticks);
        }
    }

    private static void renderStatusRail(
            GuiGraphicsExtractor graphics,
            EchoAshfallScreenSurface surface,
            int margin,
            int ticks
    ) {
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        if (width < 560 || height < 270) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        int railWidth = Math.min(214, Math.max(170, width / 5));
        int left = margin;
        int top = Math.max(66, height / 2 - 82);
        int bottom = Math.min(height - 42, top + 148);
        if (bottom - top < 100) {
            return;
        }

        graphics.fill(left, top, left + railWidth, bottom, 0x91060E14);
        graphics.outline(left, top, railWidth, bottom - top, EchoTerminalStyle.LINE_DIM);
        EchoTerminalStyle.text(graphics, font, ":: ROUTE STATUS", left + 10, top + 10, EchoTerminalStyle.CYAN_DIM, 1.0F);
        EchoTerminalStyle.text(graphics, font, "SURFACE: " + surface.route(), left + 10, top + 30, EchoTerminalStyle.TEXT, 1.0F);
        EchoTerminalStyle.text(graphics, font, "SHELL: ASHFALL", left + 10, top + 46, EchoTerminalStyle.GREEN, 1.0F);
        EchoTerminalStyle.text(graphics, font, "INPUT: VANILLA", left + 10, top + 62, EchoTerminalStyle.GREEN, 1.0F);
        EchoTerminalStyle.text(graphics, font,
                EchoAshfallScreenSurface.isInWorld() ? "SESSION: ACTIVE" : "SESSION: TITLE",
                left + 10, top + 78, EchoTerminalStyle.AMBER, 1.0F);

        int meterLeft = left + 10;
        int meterTop = bottom - 22;
        int meterWidth = railWidth - 20;
        graphics.outline(meterLeft, meterTop, meterWidth, 8, EchoTerminalStyle.LINE_DIM);
        int fill = 12 + ticks % Math.max(13, meterWidth - 16);
        graphics.fill(meterLeft + 2, meterTop + 2, Math.min(meterLeft + meterWidth - 2, meterLeft + fill),
                meterTop + 6, 0xB766E8FF);
    }

    private static void renderArchiveGrid(GuiGraphicsExtractor graphics, Bounds bounds, int ticks) {
        int rowColor = 0x120F65A0;
        for (int y = bounds.top + 34; y < bounds.bottom - 16; y += 34) {
            graphics.fill(bounds.left + 10, y, bounds.right - 10, y + 1, rowColor);
        }
        for (int x = bounds.left + 18; x < bounds.right - 18; x += 58) {
            graphics.fill(x, bounds.top + 26, x + 1, bounds.bottom - 12, 0x0F38DFF4);
        }
        int sweep = bounds.left + 24 + ticks % Math.max(32, bounds.width() - 72);
        graphics.fill(sweep, bounds.top + 24, Math.min(bounds.right - 24, sweep + 2), bounds.bottom - 14, 0x4038DFF4);
    }

    private static void renderPauseBackplate(GuiGraphicsExtractor graphics, Bounds bounds, int ticks) {
        int mid = bounds.left + bounds.width() / 2;
        graphics.fill(mid - 2, bounds.top + 28, mid + 2, bounds.bottom - 18, 0x482E8E9D);
        graphics.fill(bounds.left + 22, bounds.bottom - 32,
                Math.min(bounds.right - 22, bounds.left + 82 + ticks % Math.max(30, bounds.width() - 112)),
                bounds.bottom - 30, 0x7466E8FF);
    }

    private static void renderLoadingBackplate(GuiGraphicsExtractor graphics, Bounds bounds, int ticks) {
        int cx = bounds.left + bounds.width() / 2;
        int cy = bounds.top + bounds.height() / 2 + 12;
        int size = Math.min(48, Math.max(26, bounds.height() / 4));
        graphics.outline(cx - size / 2, cy - size / 2, size, size, EchoTerminalStyle.LINE_DIM);
        graphics.outline(cx - size / 2 + 5, cy - size / 2 + 5, Math.max(1, size - 10), Math.max(1, size - 10),
                EchoTerminalStyle.pulseColor(ticks, 0x6638DFF4, 0xCC76F7A2, 32));
    }

    private static Bounds contentBounds(
            Screen screen,
            GuiGraphicsExtractor graphics,
            EchoAshfallScreenSurface surface,
            int margin
    ) {
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        int topLimit = height < 260 ? 54 : 66;
        int bottomLimit = Math.max(topLimit + 80, height - 38);
        Bounds widgets = widgetBounds(screen);

        if (surface.compactCenterPanel()) {
            int panelWidth = Math.min(width - margin * 2, surface.loadingLike() ? 560 : 390);
            int panelHeight = Math.min(Math.max(130, height / 3), height - topLimit - 44);
            if (widgets.valid()) {
                panelWidth = Math.min(width - margin * 2, Math.max(panelWidth, widgets.width() + 56));
                panelHeight = Math.min(height - topLimit - 34, Math.max(panelHeight, widgets.height() + 56));
            }
            int left = Math.max(margin, (width - panelWidth) / 2);
            int top = EchoTerminalStyle.clamp(height / 2 - panelHeight / 2, topLimit, Math.max(topLimit, bottomLimit - panelHeight));
            return new Bounds(left, top, left + panelWidth, top + panelHeight);
        }

        if (widgets.valid()) {
            int left = EchoTerminalStyle.clamp(widgets.left - 44, margin, Math.max(margin, width - 260));
            int right = EchoTerminalStyle.clamp(widgets.right + 44, left + 220, width - margin);
            int top = EchoTerminalStyle.clamp(widgets.top - 34, topLimit, Math.max(topLimit, bottomLimit - 100));
            int bottom = EchoTerminalStyle.clamp(widgets.bottom + 34, top + 120, bottomLimit);
            if (right - left < Math.min(520, width - margin * 2)) {
                int desired = Math.min(width - margin * 2, Math.max(520, right - left));
                left = Math.max(margin, (width - desired) / 2);
                right = Math.min(width - margin, left + desired);
            }
            return new Bounds(left, top, right, bottom);
        }

        int left = width > 700 ? margin + Math.min(236, width / 5) : margin;
        return new Bounds(left, topLimit, width - margin, bottomLimit);
    }

    private static Bounds widgetBounds(Screen screen) {
        if (screen == null) {
            return Bounds.invalid();
        }
        Bounds bounds = Bounds.invalid();
        try {
            for (GuiEventListener listener : screen.children()) {
                if (listener instanceof AbstractWidget widget && widget.visible) {
                    int x = widget.getX();
                    int y = widget.getY();
                    int right = x + widget.getWidth();
                    int bottom = y + widget.getHeight();
                    if (widget.getWidth() > 0 && widget.getHeight() > 0) {
                        bounds = bounds.include(x, y, right, bottom);
                    }
                }
            }
        } catch (RuntimeException | LinkageError ignored) {
            return Bounds.invalid();
        }
        return bounds;
    }

    private static String screenTitle(Screen screen, EchoAshfallScreenSurface surface, Font font, int width) {
        String value = screen == null ? "" : screen.getTitle().getString();
        if (value == null || value.isBlank()) {
            value = surface.label();
        }
        return EchoTerminalStyle.clipToWidth(font, value.toUpperCase(), width);
    }

    private static int margin(int width) {
        return EchoTerminalStyle.clamp(width / 32, 12, 34);
    }

    private record Bounds(int left, int top, int right, int bottom) {
        static Bounds invalid() {
            return new Bounds(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
        }

        boolean valid() {
            return this.left < this.right && this.top < this.bottom;
        }

        int width() {
            return Math.max(0, this.right - this.left);
        }

        int height() {
            return Math.max(0, this.bottom - this.top);
        }

        Bounds include(int left, int top, int right, int bottom) {
            if (!valid()) {
                return new Bounds(left, top, right, bottom);
            }
            return new Bounds(
                    Math.min(this.left, left),
                    Math.min(this.top, top),
                    Math.max(this.right, right),
                    Math.max(this.bottom, bottom));
        }
    }
}
