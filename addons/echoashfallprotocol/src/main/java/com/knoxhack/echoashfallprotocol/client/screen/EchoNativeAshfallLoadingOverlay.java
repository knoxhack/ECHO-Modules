package com.knoxhack.echoashfallprotocol.client.screen;

import com.knoxhack.echoashfallprotocol.nativebridge.AshfallNativeClientRouteRegistrar;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class EchoNativeAshfallLoadingOverlay {
    private static final int BG = 0xFF03070C;
    private static final int PANEL = 0xDF07131D;
    private static final int LINE = 0xDD38DFF4;
    private static final int LINE_DIM = 0x6638DFF4;
    private static final int TEXT = 0xFFE8F8FF;
    private static final int MUTED = 0xFF8CA2AE;
    private static final int CYAN = 0xFF66E8FF;
    private static final int GREEN = 0xFF7CFFB2;
    private static final int AMBER = 0xFFFFC857;

    private EchoNativeAshfallLoadingOverlay() {
    }

    public static Map<String, Object> render(GuiGraphicsExtractor graphics, float partialTick, int ticks) {
        return render(graphics, partialTick, ticks, -1.0F, "");
    }

    public static Map<String, Object> render(
            GuiGraphicsExtractor graphics,
            float partialTick,
            int ticks,
            float liveProgress,
            String phase
    ) {
        Map<String, Object> state = new LinkedHashMap<>();
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        int margin = clamp(width / 24, 16, 42);
        int panelWidth = Math.max(240, Math.min(width - margin * 2, 660));
        int panelHeight = height < 260 ? 126 : 160;
        int left = Math.max(margin, (width - panelWidth) / 2);
        int top = clamp(height / 2 - panelHeight / 2, 24, Math.max(24, height - panelHeight - 24));
        int right = left + panelWidth;
        int bottom = top + panelHeight;
        boolean liveProgressAvailable = liveProgress >= 0.0F;
        float progress = liveProgressAvailable
                ? clamp(liveProgress, 0.0F, 1.0F)
                : Math.min(1.0F, (ticks + partialTick) / 86.0F);
        String phaseLabel = cleanPhase(phase, progress);

        graphics.fill(0, 0, width, height, BG);
        for (int y = ticks % 18; y < height; y += 18) {
            graphics.fill(0, y, width, y + 1, 0x1328D7F4);
        }
        graphics.fill(left, top, right, bottom, PANEL);
        graphics.outline(left, top, panelWidth, panelHeight, LINE);
        graphics.fill(left + 1, top + 1, right - 1, top + 26, 0x99200642);
        graphics.fill(left + 14, top + 28, right - 14, top + 29, pulse(ticks));

        Font font = Minecraft.getInstance().font;
        String percent = Math.round(progress * 100.0F) + "%";
        text(graphics, font, "ASHFALL // NATIVE LOADER HANDOFF", left + 16, top + 9, CYAN);
        text(graphics, font, percent, right - 16 - font.width(percent), top + 9, GREEN);
        text(graphics, font, "mounting product profile and native UI surfaces", left + 16, top + 44, TEXT);
        text(graphics, font, phaseLabel, left + 16, top + 60, progress >= 0.98F ? GREEN : AMBER);

        int barX = left + 16;
        int barY = bottom - 36;
        int barW = panelWidth - 32;
        graphics.outline(barX, barY, barW, 10, LINE_DIM);
        int fill = Math.max(1, Math.round((barW - 4) * progress));
        graphics.fill(barX + 2, barY + 2, barX + 2 + fill, barY + 8, CYAN);
        if (panelHeight > 136) {
            text(graphics, font, "terminal / index / lens / holomap / hud", left + 16, bottom - 58, MUTED);
        }

        state.put("rendered", true);
        state.put("progress", progress);
        state.put("progressSource", liveProgressAvailable ? "minecraft_reload_instance" : "native_loader_tick_estimate");
        state.put("phase", phaseLabel);
        state.put("ticks", ticks);
        state.put("surface", "LOADING");
        state.put("surfaceType", "loading_screen");
        state.put("surfaceId", "echoashfallprotocol:echo_native_loading");
        state.put("productProfile", "echoashfallprotocol:ashfall_native_product");
        state.put("nativeLoadingSurface", true);
        Map<String, Object> snapshot = Map.copyOf(state);
        AshfallNativeClientRouteRegistrar.publishLifecycleEvent(
                "loading_screen",
                "render",
                "ashfall.loading_screen",
                snapshot);
        return snapshot;
    }

    private static String status(float progress) {
        if (progress < 0.24F) {
            return "resolving native module jars";
        }
        if (progress < 0.52F) {
            return "installing product screens";
        }
        if (progress < 0.78F) {
            return "binding HUD and hotkey routes";
        }
        if (progress < 0.98F) {
            return "opening Native client window";
        }
        return "handoff ready";
    }

    private static String cleanPhase(String phase, float progress) {
        String text = phase == null ? "" : phase.trim();
        return text.isBlank() ? status(progress) : text;
    }

    private static void text(GuiGraphicsExtractor graphics, Font font, String value, int x, int y, int color) {
        graphics.text(font, value, x, y, color, false);
    }

    private static int pulse(int ticks) {
        int alpha = 72 + Math.round(82.0F * ((ticks % 48) / 47.0F));
        return (alpha << 24) | 0x38DFF4;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
