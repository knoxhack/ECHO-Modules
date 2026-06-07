package com.knoxhack.echoterminal.client.discovery;

import com.knoxhack.echocore.api.network.EchoDiscoveryToast;
import com.knoxhack.echoterminal.api.TerminalUi;
import com.knoxhack.echoterminal.api.TerminalVisualAssets;
import com.knoxhack.echoterminal.client.hud.TerminalHudNotice;
import com.knoxhack.echoterminal.client.hud.TerminalHudNoticeSurface;
import com.knoxhack.echoterminal.client.screen.TerminalClientOptions;
import java.util.ArrayDeque;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public final class DiscoveryToastHud {
    private static final ArrayDeque<EchoDiscoveryToast> QUEUE = new ArrayDeque<>();
    private static final int DURATION_FRAMES = 150;
    private static final int MAX_QUEUE = 4;
    private static EchoDiscoveryToast active;
    private static int frames;

    private DiscoveryToastHud() {
    }

    public static void push(EchoDiscoveryToast packet) {
        if (packet == null) {
            return;
        }
        while (QUEUE.size() >= MAX_QUEUE) {
            QUEUE.removeFirst();
        }
        QUEUE.addLast(packet);
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            QUEUE.clear();
            active = null;
            frames = 0;
            return;
        }
        if (minecraft.options.hideGui || minecraft.screen != null) {
            return;
        }
        if (active == null) {
            active = QUEUE.pollFirst();
            frames = 0;
        } else if (++frames >= DURATION_FRAMES) {
            active = QUEUE.pollFirst();
            frames = 0;
        }
    }

    public static void render(GuiGraphicsExtractor graphics, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || minecraft.screen != null || active == null) {
            return;
        }
        if (!TerminalHudNoticeSurface.shouldRenderInternalCards()) {
            return;
        }
        renderToast(graphics, active, frames + partialTick);
    }

    public static Optional<TerminalHudNotice> activeNoticeForHud() {
        return active == null ? Optional.empty() : Optional.of(toHudNotice(active));
    }

    public static TerminalHudNotice noticeForHudForTests(EchoDiscoveryToast packet) {
        return toHudNotice(packet);
    }

    private static void renderToast(GuiGraphicsExtractor graphics, EchoDiscoveryToast packet, float age) {
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        int screenW = minecraft.getWindow().getGuiScaledWidth();
        int w = Math.max(210, Math.min(306, screenW - 16));
        int h = 72;
        float enter = Math.min(1.0F, age / 12.0F);
        float exit = age > DURATION_FRAMES - 18 ? Math.min(1.0F, (age - (DURATION_FRAMES - 18)) / 18.0F) : 0.0F;
        int slide = Math.round((1.0F - enter + exit) * 34.0F);
        int x = screenW - w - 8 + slide;
        int y = 84;
        int accent = TerminalUi.opaque(packet.accentColor());

        TerminalUi.flatHudPanel(graphics, x, y, w, h, accent);
        Identifier hero = Identifier.tryParse(packet.heroArt());
        if (hero != null) {
            hero = TerminalClientOptions.currentTheme().visual(hero);
            TerminalUi.imagePanel(graphics, hero, x + 5, y + 5, 56, h - 10, accent, 0.68F, true);
        }
        Identifier icon = Identifier.tryParse(packet.iconArt());
        icon = TerminalClientOptions.currentTheme().visual(icon == null ? TerminalVisualAssets.ICON_STATE_OPEN : icon);
        TerminalUi.iconTextureBadge(graphics, icon,
                x + 16, y + 19, 28, accent, true);

        int pulse = age % 26 < 13 ? 0xFFFFFFFF : 0xFF92F7A6;
        graphics.fill(x + 48, y + 45, x + 55, y + 48, pulse);
        graphics.fill(x + 54, y + 39, x + 58, y + 48, pulse);

        int textX = x + 68;
        graphics.text(font, "DISCOVERED", textX, y + 8, accent, false);
        graphics.text(font, font.plainSubstrByWidth(packet.category(), Math.max(40, w - 152)),
                x + w - Math.min(82, w / 3), y + 8, 0xFF8CA7B5, false);
        graphics.text(font, font.plainSubstrByWidth(packet.title(), Math.max(40, w - 84)),
                textX, y + 27, 0xFFE9FBFF, false);
        graphics.text(font, font.plainSubstrByWidth(packet.subtitle(), Math.max(40, w - 84)),
                textX, y + 40, 0xFFB9CAD8, false);
        graphics.text(font, "Terminal Intel // Discovery Grid",
                textX, y + 55, 0xFF8CA7B5, false);
    }

    private static TerminalHudNotice toHudNotice(EchoDiscoveryToast packet) {
        return new TerminalHudNotice(
                "DISCOVERY",
                packet.category(),
                packet.title(),
                packet.subtitle(),
                "Discovery Grid",
                packet.accentColor(),
                0.0F,
                1);
    }
}
