package com.knoxhack.echo.creatorcore.ui;

import com.knoxhack.echo.creatorcore.EchoCreatorCore;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class CreatorDashboardScreen extends Screen {
    private static final int BG = 0xF003080C;
    private static final int PANEL = 0xDD071219;
    private static final int PANEL_ALT = 0xAA0B1B24;
    private static final int ROW = 0x66142A34;
    private static final int ROW_ACTIVE = 0x8838DFF4;
    private static final int TEXT = 0xFFEAF7F9;
    private static final int MUTED = 0xFF87AAB3;
    private static final int ACCENT = 0xFF38DFF4;
    private static final int WARN = 0xFFFFC857;
    private static final int GOOD = 0xFFA6E22E;

    private final CreatorDashboardModel model = new CreatorDashboardModel();
    private final List<Hitbox> hitboxes = new ArrayList<>();
    private Identifier activePanel = EchoCreatorCore.id("overview");
    private int scroll;
    private int navX;
    private int navY;
    private int navW;
    private int contentX;
    private int contentY;
    private int contentW;
    private int contentH;

    public CreatorDashboardScreen() {
        super(Component.translatable("screen.echocreatorcore.dashboard"));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        hitboxes.clear();
        layout();
        graphics.fill(0, 0, width, height, BG);
        drawHeader(graphics, font);
        drawNavigation(graphics, font, mouseX, mouseY);
        drawContent(graphics, font);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        for (Hitbox hitbox : List.copyOf(hitboxes)) {
            if (hitbox.inside(event.x(), event.y())) {
                activePanel = hitbox.id();
                scroll = 0;
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (inside(mouseX, mouseY, contentX, contentY, contentW, contentH)) {
            scroll = Math.max(0, scroll - (int) Math.round(scrollY * 18.0D));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            Minecraft.getInstance().setScreen(null);
            return true;
        }
        if (key == GLFW.GLFW_KEY_R) {
            model.refresh();
            return true;
        }
        if (key == GLFW.GLFW_KEY_UP) {
            scroll = Math.max(0, scroll - 18);
            return true;
        }
        if (key == GLFW.GLFW_KEY_DOWN) {
            scroll += 18;
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void layout() {
        int margin = Math.max(6, Math.min(14, Math.min(width, height) / 70));
        navW = width < 420 ? 96 : 126;
        navX = margin;
        navY = 50;
        contentX = navX + navW + margin;
        contentY = navY;
        contentW = Math.max(120, width - contentX - margin);
        contentH = Math.max(80, height - contentY - margin);
    }

    private void drawHeader(GuiGraphicsExtractor graphics, Font font) {
        graphics.fill(0, 0, width, 42, 0xF0061016);
        graphics.fill(0, 0, Math.max(80, width / 4), 2, ACCENT);
        graphics.text(font, "ECHO: CREATORCORE", 10, 10, TEXT, false);
        graphics.text(font, "CreatorCore by ECHO Labs", 10, 24, MUTED, false);
        int x = Math.max(160, width - 340);
        x = chip(graphics, font, x, 10, "Creator Mode: " + (model.doctorReport().scriptCoreAvailable() ? "Ready" : "Foundation"), ACCENT);
        x = chip(graphics, font, x + 5, 10, "Writes: " + (model.writeLocked() ? "Locked" : "Allowed"), model.writeLocked() ? WARN : GOOD);
        chip(graphics, font, x + 5, 10, "Diag: " + model.doctorReport().errors() + "/" + model.doctorReport().warnings(), WARN);
    }

    private int chip(GuiGraphicsExtractor graphics, Font font, int x, int y, String label, int color) {
        int w = Math.min(112, Math.max(54, font.width(label) + 12));
        if (x + w > width - 4) {
            return x;
        }
        graphics.fill(x, y, x + w, y + 14, 0xAA0B1B24);
        graphics.fill(x, y, x + 2, y + 14, color);
        graphics.text(font, trim(font, label, w - 8), x + 6, y + 3, TEXT, false);
        return x + w;
    }

    private void drawNavigation(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY) {
        graphics.fill(navX, navY, navX + navW, height - 8, PANEL);
        int y = navY + 8;
        for (CreatorPanel panel : com.knoxhack.echo.creatorcore.api.CreatorCoreApi.get().panels().panels()) {
            boolean active = panel.id().equals(activePanel);
            boolean hovered = inside(mouseX, mouseY, navX + 6, y, navW - 12, 18);
            graphics.fill(navX + 6, y, navX + navW - 6, y + 18, active ? ROW_ACTIVE : hovered ? 0x771B3944 : ROW);
            graphics.text(font, trim(font, panel.title(), navW - 20), navX + 12, y + 5, active ? 0xFF031014 : TEXT, false);
            hitboxes.add(new Hitbox(panel.id(), navX + 6, y, navW - 12, 18));
            y += 22;
        }
        graphics.text(font, "R refresh", navX + 10, height - 28, MUTED, false);
        graphics.text(font, "Esc close", navX + 10, height - 16, MUTED, false);
    }

    private void drawContent(GuiGraphicsExtractor graphics, Font font) {
        CreatorPanel panel = com.knoxhack.echo.creatorcore.api.CreatorCoreApi.get().panels().get(activePanel)
                .orElse(com.knoxhack.echo.creatorcore.api.CreatorCoreApi.get().panels().panels().get(0));
        graphics.fill(contentX, contentY, contentX + contentW, contentY + contentH, PANEL);
        graphics.fill(contentX, contentY, contentX + contentW, contentY + 24, PANEL_ALT);
        graphics.fill(contentX, contentY, contentX + 3, contentY + contentH, ACCENT);
        graphics.text(font, panel.title(), contentX + 12, contentY + 8, TEXT, false);
        if (!panel.summary().isBlank()) {
            graphics.text(font, trim(font, panel.summary(), contentW - 160), contentX + 110, contentY + 8, MUTED, false);
        }
        List<String> lines = panel.lines(model);
        int viewportTop = contentY + 32;
        int viewportBottom = contentY + contentH - 8;
        graphics.enableScissor(contentX + 6, viewportTop, contentX + contentW - 6, viewportBottom);
        int y = viewportTop - scroll;
        for (String line : lines) {
            if (y > viewportTop - 12 && y < viewportBottom) {
                int color = line.startsWith("ERROR") ? 0xFFFF6B6B
                        : line.startsWith("WARNING") ? WARN
                        : line.startsWith("INFO") ? MUTED
                        : line.startsWith("  ") ? MUTED : TEXT;
                graphics.text(font, trim(font, line, contentW - 24), contentX + 12, y, color, false);
            }
            y += 13;
        }
        graphics.disableScissor();
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    private static String trim(Font font, String text, int maxWidth) {
        if (text == null || maxWidth <= 0) {
            return "";
        }
        if (font.width(text) <= maxWidth) {
            return text;
        }
        String value = text;
        String ellipsis = "...";
        int allowed = Math.max(1, maxWidth - font.width(ellipsis));
        while (!value.isEmpty() && font.width(value) > allowed) {
            value = value.substring(0, value.length() - 1);
        }
        return value + ellipsis;
    }

    private record Hitbox(Identifier id, int x, int y, int w, int h) {
        boolean inside(double mouseX, double mouseY) {
            return CreatorDashboardScreen.inside(mouseX, mouseY, x, y, w, h);
        }
    }
}
