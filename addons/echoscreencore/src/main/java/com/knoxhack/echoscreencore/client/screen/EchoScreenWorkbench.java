package com.knoxhack.echoscreencore.client.screen;

import com.knoxhack.echoscreencore.api.EchoDataContext;
import com.knoxhack.echoscreencore.client.api.EchoEmbeddedSurface;
import com.knoxhack.echoscreencore.client.engine.EchoScreenEngine;
import com.knoxhack.echoscreencore.client.reference.ScreenCoreReferenceData;
import com.knoxhack.echoscreencore.client.reference.ScreenCoreWorkbenchState;
import com.knoxhack.echoscreencore.client.reference.ScreenCoreWorkbenchState.PreviewMode;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class EchoScreenWorkbench extends Screen {
    private EchoEmbeddedSurface surface;
    private Identifier surfacePage;

    public EchoScreenWorkbench(Identifier pageId) {
        super(Component.literal("ScreenCore Workbench"));
        ScreenCoreWorkbenchState.selectPage(pageId == null ? ScreenCoreWorkbenchState.selectedPage() : pageId);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        Font font = Minecraft.getInstance().font;
        graphics.fill(0, 0, width, height, 0xF2050B10);
        graphics.text(font, Component.literal("ScreenCore Workbench"), 10, 8, 0xFF45FFB0, false);
        graphics.text(font, Component.literal("real renderer / real GUI scale / real diagnostics"), 148, 8, 0xFFB7D7E3, false);

        int top = 26;
        int bottom = height - 30;
        int leftW = Math.min(190, Math.max(124, width / 5));
        int rightW = Math.min(270, Math.max(178, width / 4));
        int previewX = leftW + 18;
        int previewY = top;
        int previewW = Math.max(120, width - leftW - rightW - 36);
        int previewH = Math.max(80, bottom - top);
        int rightX = previewX + previewW + 8;

        panel(graphics, 8, top, leftW, previewH);
        panel(graphics, previewX, previewY, previewW, previewH);
        panel(graphics, rightX, top, rightW, previewH);
        drawPageList(graphics, font, 14, top + 8, leftW - 12);
        drawPreview(graphics, previewX + 6, previewY + 18, previewW - 12, previewH - 24, mouseX, mouseY, partialTick);
        drawDiagnostics(graphics, font, rightX + 8, top + 8, rightW - 16);
        drawModeBar(graphics, font, 10, height - 24);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int top = 26;
        int leftW = Math.min(190, Math.max(124, width / 5));
        int rowY = top + 24;
        List<Map<String, Object>> pages = ScreenCoreReferenceData.referencePages();
        for (Map<String, Object> page : pages) {
            if (mouseX >= 14 && mouseX <= leftW - 2 && mouseY >= rowY && mouseY < rowY + 20) {
                selectPage(String.valueOf(page.get("id")));
                return true;
            }
            rowY += 22;
        }
        int modeX = 10;
        for (PreviewMode mode : PreviewMode.values()) {
            int w = modeWidth(mode);
            if (mouseX >= modeX && mouseX < modeX + w && mouseY >= height - 25 && mouseY < height - 8) {
                ScreenCoreWorkbenchState.setPreviewMode(mode);
                return true;
            }
            modeX += w + 6;
        }
        if (mouseX >= modeX && mouseX < modeX + 64 && mouseY >= height - 25 && mouseY < height - 8) {
            ScreenCoreWorkbenchState.toggleDebug();
            if (surface != null) {
                surface.setDebug(ScreenCoreWorkbenchState.debug());
            }
            return true;
        }
        return surface != null && surface.mouseClicked(mouseX, mouseY, event.button())
                || super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return surface != null && surface.mouseReleased(event.x(), event.y(), event.button())
                || super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        return surface != null && surface.mouseDragged(event.x(), event.y(), event.button(), dragX, dragY)
                || super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return surface != null && surface.mouseScrolled(mouseX, mouseY, scrollY)
                || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            Minecraft.getInstance().setScreen(null);
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_R && surface != null) {
            surface.reloadPage();
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_D) {
            ScreenCoreWorkbenchState.toggleDebug();
            if (surface != null) {
                surface.setDebug(ScreenCoreWorkbenchState.debug());
            }
            return true;
        }
        return surface != null && surface.keyPressed(event.key()) || super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void drawPageList(GuiGraphicsExtractor graphics, Font font, int x, int y, int width) {
        graphics.text(font, Component.literal("REFERENCE PAGES"), x, y, 0xFFEAFBFF, false);
        y += 16;
        for (Map<String, Object> page : ScreenCoreReferenceData.referencePages()) {
            String id = String.valueOf(page.get("id"));
            boolean selected = id.equals(ScreenCoreWorkbenchState.selectedPage().toString());
            int fill = selected ? 0x9922D7FF : 0x550A1A24;
            graphics.fill(x, y, x + width, y + 18, fill);
            graphics.text(font, Component.literal(trim(font, String.valueOf(page.get("title")), width - 8)), x + 4, y + 5,
                    selected ? 0xFF00141A : 0xFFB7D7E3, false);
            y += 22;
        }
    }

    private void drawPreview(GuiGraphicsExtractor graphics, int x, int y, int width, int height,
            int mouseX, int mouseY, float partialTick) {
        Identifier selected = ScreenCoreWorkbenchState.selectedPage();
        if (surface == null || !selected.equals(surfacePage)) {
            surfacePage = selected;
            surface = new EchoEmbeddedSurface(surfacePage, EchoDataContext.empty());
            surface.setDebug(ScreenCoreWorkbenchState.debug());
        }
        int viewportWidth = ScreenCoreWorkbenchState.viewportWidth(width);
        int viewportHeight = ScreenCoreWorkbenchState.viewportHeight(height);
        int renderWidth = ScreenCoreWorkbenchState.previewMode() == PreviewMode.FIT ? width : viewportWidth;
        int renderHeight = ScreenCoreWorkbenchState.previewMode() == PreviewMode.FIT ? height : viewportHeight;
        graphics.text(Minecraft.getInstance().font,
                Component.literal(selected + " @ " + renderWidth + "x" + renderHeight),
                x, y - 13, 0xFFB7D7E3, false);
        graphics.enableScissor(x, y, x + width, y + height);
        try {
            surface.render(graphics, x, y, renderWidth, renderHeight, mouseX, mouseY, partialTick);
        } finally {
            graphics.disableScissor();
        }
    }

    private void drawDiagnostics(GuiGraphicsExtractor graphics, Font font, int x, int y, int width) {
        Identifier selected = ScreenCoreWorkbenchState.selectedPage();
        int viewportW = ScreenCoreWorkbenchState.viewportWidth(width);
        int viewportH = ScreenCoreWorkbenchState.viewportHeight(180);
        List<String> lines = EchoScreenEngine.inspectPage(selected, viewportW, viewportH);
        graphics.text(font, Component.literal("INSPECTION"), x, y, 0xFFEAFBFF, false);
        y += 14;
        for (String line : lines.stream().limit(16).toList()) {
            graphics.text(font, Component.literal(trim(font, line, width)), x, y, line.contains("diagnostic") || line.contains("Fix:")
                    ? 0xFFFFD166 : 0xFFB7D7E3, false);
            y += 10;
        }
    }

    private void drawModeBar(GuiGraphicsExtractor graphics, Font font, int x, int y) {
        int cursor = x;
        for (PreviewMode mode : PreviewMode.values()) {
            int w = modeWidth(mode);
            boolean selected = mode == ScreenCoreWorkbenchState.previewMode();
            graphics.fill(cursor, y, cursor + w, y + 17, selected ? 0xFF2BEAFF : 0xAA10202A);
            graphics.text(font, Component.literal(modeLabel(mode)), cursor + 5, y + 5,
                    selected ? 0xFF00141A : 0xFFB7D7E3, false);
            cursor += w + 6;
        }
        graphics.fill(cursor, y, cursor + 64, y + 17, ScreenCoreWorkbenchState.debug() ? 0xFFFFD166 : 0xAA10202A);
        graphics.text(font, Component.literal("DEBUG"), cursor + 8, y + 5,
                ScreenCoreWorkbenchState.debug() ? 0xFF00141A : 0xFFB7D7E3, false);
    }

    private void selectPage(String raw) {
        try {
            ScreenCoreWorkbenchState.selectPage(Identifier.parse(raw));
            surface = null;
        } catch (RuntimeException ignored) {
        }
    }

    private static void panel(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, 0xAA071017);
        graphics.fill(x, y, x + width, y + 1, 0x992BEAFF);
        graphics.fill(x, y + height - 1, x + width, y + height, 0x662BEAFF);
        graphics.fill(x, y, x + 1, y + height, 0x662BEAFF);
        graphics.fill(x + width - 1, y, x + width, y + height, 0x662BEAFF);
    }

    private static int modeWidth(PreviewMode mode) {
        return switch (mode) {
            case FIT -> 42;
            case SMALL -> 62;
            case DEFAULT -> 70;
            case LARGE -> 76;
            case CURRENT -> 68;
        };
    }

    private static String modeLabel(PreviewMode mode) {
        return switch (mode) {
            case FIT -> "FIT";
            case SMALL -> "360x240";
            case DEFAULT -> "854x480";
            case LARGE -> "1280x720";
            case CURRENT -> "CURRENT";
        };
    }

    private static String trim(Font font, String value, int width) {
        if (font == null || value == null || width <= 0) {
            return "";
        }
        if (font.width(value) <= width) {
            return value;
        }
        return font.plainSubstrByWidth(value, Math.max(0, width - font.width("..."))) + "...";
    }
}
