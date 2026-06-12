package com.knoxhack.echoashfallprotocol.client.screen;

import com.echoplatform.echocore.api.EchoRuntimeModules;
import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * Client-only first-join briefing opened by the existing welcome packet.
 */
public class WelcomeScreen extends Screen {
    private static final int MAX_DASH_W = 800;
    private static final int MAX_DASH_H = 440;
    private static final int HEADER_H = 62;
    private static final int BUTTON_AREA_H = 40;
    private static final int FOOTER_H = 18;
    private static final int CONTENT_PAD = 14;
    private static final int BACKGROUND_SOURCE_W = 1920;
    private static final int BACKGROUND_SOURCE_H = 1080;
    private static final int ICON_SOURCE_SIZE = 128;

    private static final int COL_BG = 0xF2080C14;
    private static final int COL_PANEL = 0xE60D141C;
    private static final int COL_PANEL_SOFT = 0xAA121820;
    private static final int COL_CYAN = 0xFF4DBAF4;
    private static final int COL_CYAN_DIM = 0x664DBAF4;
    private static final int COL_TEXT = 0xFFE0E8F0;
    private static final int COL_DIM = 0xFF8B9BB0;
    private static final int COL_GREEN = 0xFF42D67E;
    private static final int COL_YELLOW = 0xFFFFD54F;
    private static final int COL_RED = 0xFFFF5252;
    private static final int COL_PURPLE = 0xFFC8A4FF;

    private static final Identifier WELCOME_BACKGROUND = Identifier.fromNamespaceAndPath(
            EchoAshfallProtocol.MODID, "textures/gui/welcome/welcome_background.png");
    private static final Identifier ICON_MASK = Identifier.fromNamespaceAndPath(
            EchoAshfallProtocol.MODID, "textures/gui/welcome/icons/mask.png");
    private static final Identifier ICON_WATER = Identifier.fromNamespaceAndPath(
            EchoAshfallProtocol.MODID, "textures/gui/welcome/icons/water.png");
    private static final Identifier ICON_SHELTER = Identifier.fromNamespaceAndPath(
            EchoAshfallProtocol.MODID, "textures/gui/welcome/icons/shelter.png");

    private static final String[] STATUS_KEYS = {
            "screen.EchoAshfallProtocol.welcome.status.pod",
            "screen.EchoAshfallProtocol.welcome.status.atmosphere",
            "screen.EchoAshfallProtocol.welcome.status.kit",
            "screen.EchoAshfallProtocol.welcome.status.objective"
    };

    private static final int[] STATUS_COLORS = {
            COL_GREEN,
            COL_RED,
            COL_YELLOW,
            COL_CYAN
    };

    private static final String[] HAZARD_KEYS = {
            "screen.EchoAshfallProtocol.welcome.hazard.air",
            "screen.EchoAshfallProtocol.welcome.hazard.radiation",
            "screen.EchoAshfallProtocol.welcome.hazard.signal",
            "screen.EchoAshfallProtocol.welcome.hazard.echo"
    };

    private static final int[] HAZARD_COLORS = {
            COL_RED,
            COL_YELLOW,
            0xFFFF9B2F,
            COL_GREEN
    };

    private static final BriefCard[] CARDS = {
            new BriefCard("mask", COL_YELLOW),
            new BriefCard("water", COL_CYAN),
            new BriefCard("shelter", COL_GREEN)
    };

    private static final String[] FIRST_TEN_KEYS = {
            "screen.EchoAshfallProtocol.welcome.first10.step.scavenge",
            "screen.EchoAshfallProtocol.welcome.first10.step.knife",
            "screen.EchoAshfallProtocol.welcome.first10.step.water",
            "screen.EchoAshfallProtocol.welcome.first10.step.shelter"
    };

    private static final int[] FIRST_TEN_COLORS = {
            COL_YELLOW,
            COL_RED,
            COL_CYAN,
            COL_GREEN
    };

    private static final String[] THREAT_KEYS = {
            "screen.EchoAshfallProtocol.welcome.threat.air",
            "screen.EchoAshfallProtocol.welcome.threat.water",
            "screen.EchoAshfallProtocol.welcome.threat.radiation",
            "screen.EchoAshfallProtocol.welcome.threat.mutations"
    };

    private static final int[] THREAT_COLORS = {
            COL_RED,
            COL_CYAN,
            COL_YELLOW,
            COL_PURPLE
    };

    private static final String[] PILLAR_KEYS = {
            "screen.EchoAshfallProtocol.welcome.modpack.pillar.survival",
            "screen.EchoAshfallProtocol.welcome.modpack.pillar.echo",
            "screen.EchoAshfallProtocol.welcome.modpack.pillar.scrap",
            "screen.EchoAshfallProtocol.welcome.modpack.pillar.factions",
            "screen.EchoAshfallProtocol.welcome.modpack.pillar.nexus"
    };

    private static boolean pendingOpen;

    private long tick;

    private enum LayoutMode {
        WIDE,
        COMPACT,
        MICRO
    }

    private record DashboardLayout(
            int x, int y, int w, int h,
            int contentX, int contentY, int contentW, int contentH,
            int buttonX, int buttonY, int buttonW,
            int footerY,
            LayoutMode mode
    ) {}

    private WelcomeScreen() {
        super(Component.translatable("screen.EchoAshfallProtocol.welcome.title"));
    }

    public static void requestOpen() {
        pendingOpen = true;
    }

    public static void openNow() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        pendingOpen = false;
        minecraft.setScreen(new WelcomeScreen());
    }

    public static void openPendingIfReady() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!pendingOpen || minecraft.player == null || minecraft.level == null || minecraft.screen != null) {
            return;
        }

        openNow();
    }

    @Override
    protected void init() {
    }

    @Override
    public void tick() {
        tick++;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        drawBackdrop(graphics, tick + partialTick);
        drawDashboard(graphics, tick + partialTick, mouseX, mouseY);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == GLFW.GLFW_KEY_ESCAPE
                || key == GLFW.GLFW_KEY_SPACE
                || key == GLFW.GLFW_KEY_ENTER
                || key == GLFW.GLFW_KEY_KP_ENTER) {
            dismiss();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            DashboardLayout layout = computeLayout();
            int mx = (int) event.x();
            int my = (int) event.y();
            if (mx >= layout.buttonX()
                    && mx <= layout.buttonX() + layout.buttonW()
                    && my >= layout.buttonY()
                    && my <= layout.buttonY() + 24) {
                dismiss();
                return true;
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void onClose() {
        pendingOpen = false;
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return Minecraft.getInstance().isSingleplayer();
    }

    private void dismiss() {
        pendingOpen = false;
        Minecraft.getInstance().setScreen(null);
    }

    private void drawBackdrop(GuiGraphicsExtractor g, float frame) {
        if (!drawWelcomeBackground(g)) {
            g.fill(0, 0, width, height, 0xDD000000);
            g.fillGradient(0, 0, width, height, COL_BG, 0xF00F1722);
        }

        g.fill(0, 0, width, height, 0x88000008);
        g.fillGradient(0, 0, width, Math.max(1, height / 3), 0xAA020713, 0x22020713);
        g.fillGradient(0, Math.max(0, height - height / 3), width, height, 0x22000000, 0xB8000006);

        for (int x = (int) -(frame % 28); x < width; x += 28) {
            g.fill(x, 0, x + 1, height, 0x1038A8FF);
        }
        for (int y = (int) (frame % 24); y < height; y += 24) {
            g.fill(0, y, width, y + 1, 0x1238A8FF);
        }
        for (int y = 0; y < height; y += 4) {
            g.fill(0, y, width, y + 1, 0x15000000);
        }
        for (int x = 0; x < width; x += 96) {
            g.fill(x + 6, 8, x + 36, 9, 0x354DBAF4);
            g.fill(x + 44, height - 10, x + 78, height - 9, 0x244DBAF4);
        }
    }

    private boolean drawWelcomeBackground(GuiGraphicsExtractor g) {
        try {
            float screenAspect = width / (float) Math.max(1, height);
            float sourceAspect = BACKGROUND_SOURCE_W / (float) BACKGROUND_SOURCE_H;
            int srcW = BACKGROUND_SOURCE_W;
            int srcH = BACKGROUND_SOURCE_H;
            float u = 0.0F;
            float v = 0.0F;

            if (screenAspect > sourceAspect) {
                srcH = Math.max(1, Math.round(BACKGROUND_SOURCE_W / screenAspect));
                v = (BACKGROUND_SOURCE_H - srcH) / 2.0F;
            } else if (screenAspect < sourceAspect) {
                srcW = Math.max(1, Math.round(BACKGROUND_SOURCE_H * screenAspect));
                u = (BACKGROUND_SOURCE_W - srcW) / 2.0F;
            }

            g.blit(RenderPipelines.GUI_TEXTURED, WELCOME_BACKGROUND, 0, 0, u, v, width, height,
                    srcW, srcH, BACKGROUND_SOURCE_W, BACKGROUND_SOURCE_H, 0xFFFFFFFF);
            return true;
        } catch (RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    private void drawDashboard(GuiGraphicsExtractor g, float frame, int mouseX, int mouseY) {
        DashboardLayout layout = computeLayout();
        int x = layout.x();
        int y = layout.y();
        int dashW = layout.w();
        int dashH = layout.h();
        int pulse = 150 + (int) (60 * Math.sin(frame / 22.0));
        int border = (pulse << 24) | (COL_CYAN & 0x00FFFFFF);

        drawTechFrame(g, x, y, dashW, dashH, border);

        drawHeader(g, x, y, dashW);

        if (layout.mode() == LayoutMode.MICRO) {
            drawMicroLayout(g, layout.contentX(), layout.contentY(), layout.contentW(), layout.contentH(), frame);
        } else if (layout.mode() == LayoutMode.COMPACT) {
            drawCompactLayout(g, layout.contentX(), layout.contentY(), layout.contentW(), layout.contentH(), frame);
        } else {
            drawWideLayout(g, layout.contentX(), layout.contentY(), layout.contentW(), layout.contentH(), frame);
        }

        drawBeginButton(g, layout, frame, mouseX, mouseY);
        drawFooter(g, x, layout.footerY(), dashW);
    }

    private DashboardLayout computeLayout() {
        int margin = height < 260 ? 6 : Math.max(10, Math.min(24, width / 24));
        int availableW = Math.max(120, width - margin * 2);
        int availableH = Math.max(120, height - margin * 2);
        int dashW = Math.min(MAX_DASH_W, availableW);
        int dashH = Math.min(MAX_DASH_H, availableH);
        int x = Math.max(0, (width - dashW) / 2);
        int y = Math.max(0, (height - dashH) / 2);

        int contentX = x + CONTENT_PAD;
        int contentY = y + HEADER_H + 8;
        int contentW = Math.max(80, dashW - CONTENT_PAD * 2);
        int contentBottom = y + dashH - BUTTON_AREA_H - FOOTER_H - 8;
        int contentH = Math.max(0, contentBottom - contentY);

        int buttonW = Math.min(300, Math.max(156, dashW - 40));
        int buttonX = x + (dashW - buttonW) / 2;
        int buttonY = y + dashH - FOOTER_H - BUTTON_AREA_H + 8;
        int footerY = y + dashH - FOOTER_H + 2;

        LayoutMode mode;
        if (contentW < 360 || contentH < 210) {
            mode = LayoutMode.MICRO;
        } else if (contentW < 610 || contentH < 270) {
            mode = LayoutMode.COMPACT;
        } else {
            mode = LayoutMode.WIDE;
        }

        return new DashboardLayout(
                x, y, dashW, dashH,
                contentX, contentY, contentW, contentH,
                buttonX, buttonY, buttonW,
                footerY,
                mode);
    }

    private void drawTechFrame(GuiGraphicsExtractor g, int x, int y, int w, int h, int border) {
        g.fill(x, y, x + w, y + h, 0xCC05080F);
        g.outline(x, y, w, h, border);
        g.outline(x + 4, y + 4, w - 8, h - 8, 0x552980FF);

        int corner = 30;
        int cut = 9;
        g.fill(x + 6, y + 6, x + corner, y + 8, COL_CYAN);
        g.fill(x + 6, y + 6, x + 8, y + corner, COL_CYAN);
        g.fill(x + w - corner, y + 6, x + w - 6, y + 8, COL_CYAN);
        g.fill(x + w - 8, y + 6, x + w - 6, y + corner, COL_CYAN);
        g.fill(x + 6, y + h - 8, x + corner, y + h - 6, COL_CYAN);
        g.fill(x + 6, y + h - corner, x + 8, y + h - 6, COL_CYAN);
        g.fill(x + w - corner, y + h - 8, x + w - 6, y + h - 6, COL_CYAN);
        g.fill(x + w - 8, y + h - corner, x + w - 6, y + h - 6, COL_CYAN);

        g.fill(x + cut, y + 20, x + cut + 1, y + 36, COL_CYAN_DIM);
        g.fill(x + w - cut - 1, y + 20, x + w - cut, y + 36, COL_CYAN_DIM);
        g.fill(x + w / 2 - 3, y + 9, x + w / 2 + 3, y + 11, COL_CYAN);
    }

    private void drawHeader(GuiGraphicsExtractor g, int x, int y, int w) {
        g.fill(x + 8, y + 8, x + w - 8, y + HEADER_H - 7, 0x77101520);
        g.fill(x + 70, y + HEADER_H - 11, x + w - 70, y + HEADER_H - 10, COL_CYAN_DIM);
        g.fill(x + w / 2 - 2, y + HEADER_H - 13, x + w / 2 + 2, y + HEADER_H - 9, COL_CYAN_DIM);
        g.centeredText(font, fit(Component.translatable("screen.EchoAshfallProtocol.welcome.title").getString(), w - 24), x + w / 2, y + 17, COL_CYAN);
        g.centeredText(font, fit(Component.translatable("screen.EchoAshfallProtocol.welcome.subtitle").getString(), w - 24), x + w / 2, y + 34, COL_DIM);
    }

    private void drawWideLayout(GuiGraphicsExtractor g, int x, int y, int w, int h, float frame) {
        int gap = 8;
        int badgeH = 24;
        int statusH = 24;
        int cardH = clamp(h * 38 / 100, 98, 122);
        int briefH = Math.max(34, h - badgeH - statusH - cardH - gap * 3);

        drawBadgeRow(g, x, y, w, badgeH);
        drawStatusStrip(g, x, y + badgeH + gap, w, statusH, frame);
        drawSurvivalCards(g, x, y + badgeH + statusH + gap * 2, w, cardH, false);
        drawCenterBriefing(g, x, y + badgeH + statusH + cardH + gap * 3, w, briefH);
    }

    private void drawBadgeRow(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        int gap = 12;
        int badgeW = (w - gap * 3) / 4;
        String[] keys = {
                "screen.EchoAshfallProtocol.welcome.badge.first_light",
                "screen.EchoAshfallProtocol.welcome.badge.kit",
                terminalInstalled()
                        ? "screen.EchoAshfallProtocol.welcome.badge.terminal"
                        : "screen.EchoAshfallProtocol.welcome.badge.guidance",
                "screen.EchoAshfallProtocol.welcome.badge.reopen"
        };
        int[] colors = { COL_CYAN, COL_GREEN, COL_CYAN, COL_PURPLE };

        for (int i = 0; i < keys.length; i++) {
            int bx = x + i * (badgeW + gap);
            int bw = i == keys.length - 1 ? x + w - bx : badgeW;
            drawWideBadge(g, bx, y, bw, h, Component.translatable(keys[i]).getString(), colors[i]);
        }
    }

    private void drawWideBadge(GuiGraphicsExtractor g, int x, int y, int w, int h, String text, int color) {
        g.fill(x, y + 2, x + w, y + h - 2, 0x66121820);
        g.outline(x, y + 2, w, h - 4, (0xAA << 24) | (color & 0x00FFFFFF));
        g.fill(x + 5, y, x + w - 5, y + 1, (0x88 << 24) | (color & 0x00FFFFFF));
        g.fill(x + 5, y + h - 1, x + w - 5, y + h, (0x88 << 24) | (color & 0x00FFFFFF));
        g.centeredText(font, fit(text, w - 14), x + w / 2, y + 8, color);
    }

    private void drawStatusStrip(GuiGraphicsExtractor g, int x, int y, int w, int h, float frame) {
        int gap = 1;
        int cellW = (w - gap * (HAZARD_KEYS.length - 1)) / HAZARD_KEYS.length;
        for (int i = 0; i < HAZARD_KEYS.length; i++) {
            int sx = x + i * (cellW + gap);
            int sw = i == HAZARD_KEYS.length - 1 ? x + w - sx : cellW;
            int color = HAZARD_COLORS[i];
            String text = Component.translatable(HAZARD_KEYS[i]).getString();
            int active = i == Math.min(HAZARD_KEYS.length - 1, Math.max(0, (int) frame / 22)) ? 0x22 : 0x00;

            g.fill(sx, y, sx + sw, y + h, (0x66 + active) << 24 | 0x00121820);
            g.outline(sx, y, sw, h, 0x442980FF);
            g.fill(sx, y, sx + 2, y + h, (0xAA << 24) | (color & 0x00FFFFFF));
            g.text(font, fit(text, sw - 12), sx + 7, y + 8, color, false);
        }
    }

    private void drawSurvivalCards(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean compact) {
        int gap = 10;
        int cardW = (w - gap * 2) / 3;
        for (int i = 0; i < CARDS.length; i++) {
            BriefCard card = CARDS[i];
            int cx = x + i * (cardW + gap);
            int cw = i == CARDS.length - 1 ? x + w - cx : cardW;
            drawCinematicCard(g, card, cx, y, cw, h, compact);
        }
    }

    private void drawCinematicCard(GuiGraphicsExtractor g, BriefCard card, int x, int y, int w, int h, boolean compact) {
        g.fill(x, y, x + w, y + h, COL_PANEL);
        g.outline(x, y, w, h, (0xBB << 24) | (card.color & 0x00FFFFFF));
        g.fill(x, y, x + w, y + 2, (0xCC << 24) | (card.color & 0x00FFFFFF));
        g.fill(x + 6, y + h - 7, x + 18, y + h - 5, (0x55 << 24) | (card.color & 0x00FFFFFF));
        g.fill(x + w - 23, y + 6, x + w - 8, y + 7, (0x55 << 24) | (card.color & 0x00FFFFFF));

        g.enableScissor(x + 1, y + 1, x + w - 1, y + h - 1);
        int iconW = compact ? 0 : Math.min(70, Math.min(w / 3, h - 28));
        if (iconW > 0 && h >= 76) {
            drawCardIcon(g, card.key, x + 10, y + 25, iconW, h - 38, card.color);
        }

        int textX = iconW > 0 ? x + iconW + 18 : x + 10;
        int textW = x + w - 10 - textX;
        g.text(font, fit(Component.translatable("screen.EchoAshfallProtocol.welcome.card." + card.key + ".title").getString(), textW),
                textX, y + 13, card.color, false);
        g.fill(textX, y + 25, textX + Math.min(textW, 92), y + 26, (0x77 << 24) | (card.color & 0x00FFFFFF));

        if (!compact || h >= 54) {
            drawCardBullet(g, Component.translatable("screen.EchoAshfallProtocol.welcome.card." + card.key + ".line1").getString(),
                    textX, y + 35, textW, y + h - 8, card.color);
            drawCardBullet(g, Component.translatable("screen.EchoAshfallProtocol.welcome.card." + card.key + ".line2").getString(),
                    textX, y + 54, textW, y + h - 8, card.color);
        }
        g.disableScissor();
    }

    private void drawCardBullet(GuiGraphicsExtractor g, String text, int x, int y, int w, int maxY, int accent) {
        if (y + 9 > maxY) {
            return;
        }

        g.text(font, ">", x, y, accent, false);
        drawWrapped(g, Component.literal(text), x + 12, y, w - 12, COL_TEXT, maxY);
    }

    private void drawCardIcon(GuiGraphicsExtractor g, String key, int x, int y, int w, int h, int color) {
        int size = Math.max(1, Math.min(Math.min(w, h), 70));
        int ix = x + (w - size) / 2;
        int iy = y + (h - size) / 2;
        if (drawGeneratedCardIcon(g, key, ix, iy, size)) {
            return;
        }

        int cx = x + w / 2;
        int cy = y + h / 2;
        int soft = (0x33 << 24) | (color & 0x00FFFFFF);
        int bright = (0xCC << 24) | (color & 0x00FFFFFF);

        if ("mask".equals(key)) {
            g.outline(cx - 18, cy - 14, 36, 18, bright);
            g.fill(cx - 14, cy - 10, cx - 3, cy - 2, soft);
            g.fill(cx + 3, cy - 10, cx + 14, cy - 2, soft);
            g.fill(cx - 5, cy + 5, cx + 5, cy + 17, soft);
            g.outline(cx - 8, cy + 2, 16, 18, bright);
            g.fill(cx - 23, cy + 6, cx - 14, cy + 15, soft);
            g.fill(cx + 14, cy + 6, cx + 23, cy + 15, soft);
            return;
        }

        if ("water".equals(key)) {
            g.fill(cx - 5, cy - 21, cx + 5, cy - 11, soft);
            g.fill(cx - 10, cy - 11, cx + 10, cy + 5, soft);
            g.fill(cx - 15, cy + 5, cx + 15, cy + 18, soft);
            g.outline(cx - 16, cy + 4, 32, 16, bright);
            g.fill(cx + 4, cy + 9, cx + 10, cy + 13, bright);
            return;
        }

        g.fill(cx - 20, cy - 1, cx, cy - 17, soft);
        g.fill(cx, cy - 17, cx + 20, cy - 1, soft);
        g.outline(cx - 17, cy - 1, 34, 29, bright);
        g.outline(cx - 6, cy + 10, 12, 18, bright);
        g.fill(cx - 22, cy, cx + 22, cy + 3, bright);
    }

    private boolean drawGeneratedCardIcon(GuiGraphicsExtractor g, String key, int x, int y, int size) {
        Identifier texture = iconTexture(key);
        if (texture == null) {
            return false;
        }

        try {
            g.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0.0F, 0.0F, size, size,
                    ICON_SOURCE_SIZE, ICON_SOURCE_SIZE, ICON_SOURCE_SIZE, ICON_SOURCE_SIZE, 0xFFFFFFFF);
            return true;
        } catch (RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    private Identifier iconTexture(String key) {
        return switch (key) {
            case "mask" -> ICON_MASK;
            case "water" -> ICON_WATER;
            case "shelter" -> ICON_SHELTER;
            default -> null;
        };
    }

    private void drawCenterBriefing(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.enableScissor(x, y, x + w, y + h);
        int cy = y + 3;
        String firstTen = Component.translatable("screen.EchoAshfallProtocol.welcome.first10.line").getString();
        g.centeredText(font, fit(firstTen, w - 20), x + w / 2, cy, COL_TEXT);
        cy += 14;
        if (cy + 9 <= y + h) {
            g.centeredText(font, fit(Component.translatable("screen.EchoAshfallProtocol.welcome.first10.actions").getString(), w - 20),
                    x + w / 2, cy, COL_CYAN);
        }
        cy += 14;
        if (cy + 9 <= y + h) {
            g.centeredText(font, fit(Component.translatable("screen.EchoAshfallProtocol.welcome.modpack.micro").getString(), w - 20),
                    x + w / 2, cy, COL_DIM);
        }
        cy += 14;
        if (cy + 9 <= y + h) {
            g.centeredText(font, fit(Component.translatable("screen.EchoAshfallProtocol.welcome.modpack.loop").getString(), w - 20),
                    x + w / 2, cy, COL_PURPLE);
        }
        g.disableScissor();
    }

    private void drawBeginButton(GuiGraphicsExtractor g, DashboardLayout layout, float frame, int mouseX, int mouseY) {
        int x = layout.buttonX();
        int y = layout.buttonY();
        int w = layout.buttonW();
        int h = 24;
        boolean hovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        int pulse = 170 + (int) (55 * Math.sin(frame / 16.0));
        if (hovered) {
            pulse = 245;
        }
        int glow = (pulse << 24) | (COL_CYAN & 0x00FFFFFF);

        g.fill(x - 10, y - 5, x + w + 10, y + h + 5, hovered ? 0x55205258 : 0x33002535);
        g.outline(x - 6, y - 3, w + 12, h + 6, glow);
        g.outline(x - 3, y, w + 6, h, hovered ? 0xFFFFFFFF : glow);
        g.outline(x, y, w, h, COL_CYAN);
        g.fill(x + 3, y + 3, x + w - 3, y + h - 3, hovered ? 0xCC0A2430 : 0xAA06131B);
        g.fill(x + 6, y + 2, x + w - 6, y + 3, 0xCC4DBAF4);
        g.fill(x + 6, y + h - 3, x + w - 6, y + h - 2, 0xCC4DBAF4);
        g.fill(x + 11, y + 7, x + 19, y + 8, COL_CYAN);
        g.fill(x + 11, y + h - 8, x + 19, y + h - 7, COL_CYAN);
        g.fill(x + w - 19, y + 7, x + w - 11, y + 8, COL_CYAN);
        g.fill(x + w - 19, y + h - 8, x + w - 11, y + h - 7, COL_CYAN);
        g.text(font, "<", x + 28, y + 8, hovered ? COL_TEXT : COL_CYAN, false);
        g.text(font, ">", x + w - 33, y + 8, hovered ? COL_TEXT : COL_CYAN, false);
        g.centeredText(font, fit(Component.translatable("screen.EchoAshfallProtocol.welcome.begin").getString(), w - 70),
                x + w / 2, y + 8, hovered ? COL_TEXT : COL_CYAN);
    }

    private void drawCompactLayout(GuiGraphicsExtractor g, int x, int y, int w, int h, float frame) {
        if (h < 210) {
            drawMicroLayout(g, x, y, w, h, frame);
            return;
        }

        int gap = 6;
        int badgeH = h >= 250 ? 22 : 0;
        int statusH = 22;
        int cardH = clamp(h * 32 / 100, 54, 78);
        int cy = y;

        if (badgeH > 0) {
            drawBadgeRow(g, x, cy, w, badgeH);
            cy += badgeH + gap;
        }

        drawStatusStrip(g, x, cy, w, statusH, frame);
        cy += statusH + gap;
        drawSurvivalCards(g, x, cy, w, cardH, true);
        cy += cardH + gap;
        drawCenterBriefing(g, x, cy, w, Math.max(0, y + h - cy));
    }

    private void drawMicroLayout(GuiGraphicsExtractor g, int x, int y, int w, int h, float frame) {
        if (h <= 0) {
            return;
        }

        drawMicroBriefingCard(g, x, y, w, h, frame);
    }

    private void drawMicroBriefingCard(GuiGraphicsExtractor g, int x, int y, int w, int h, float frame) {
        g.fill(x, y, x + w, y + h, COL_PANEL);
        g.outline(x, y, w, h, 0x662980FF);
        g.fill(x, y, x + w, y + 2, COL_CYAN);
        g.fill(x, y + h - 2, x + w, y + h, 0x6638A8FF);

        g.enableScissor(x + 1, y + 1, x + w - 1, y + h - 1);

        int badgeY = y + 8;
        int badgeX = x + 10;
        boolean showBadges = h >= 116;
        if (showBadges) {
            badgeX = drawBadge(g, badgeX, badgeY, Component.translatable("screen.EchoAshfallProtocol.welcome.badge.first_light").getString(), COL_CYAN) + 5;
            if (Minecraft.getInstance().isSingleplayer()) {
                badgeX = drawBadge(g, badgeX, badgeY, Component.translatable("screen.EchoAshfallProtocol.welcome.badge.paused").getString(), COL_GREEN) + 5;
            }
            drawBadge(g, badgeX, badgeY, Component.translatable("screen.EchoAshfallProtocol.welcome.badge.reopen").getString(), COL_PURPLE);
        }

        String activeStatus = currentStatusLine(frame);
        int statusY = showBadges ? y + 28 : y + 10;
        g.text(font, fit(activeStatus, w - 20), x + 10, statusY, COL_DIM, false);

        int firstTenY = statusY + 18;
        if (firstTenY + 22 <= y + h - 52) {
            g.text(font, fit(Component.translatable("screen.EchoAshfallProtocol.welcome.first10.title").getString(), w - 20),
                    x + 10, firstTenY, COL_YELLOW, false);
            drawFirstTenChips(g, x + 10, firstTenY + 13, w - 20, true);
        }

        int threatY = firstTenY + 42;
        if (threatY + 20 <= y + h - 31) {
            g.text(font, fit(Component.translatable("screen.EchoAshfallProtocol.welcome.threat.title").getString(), w - 20),
                    x + 10, threatY, COL_RED, false);
            drawThreatBadges(g, x + 10, threatY + 13, w - 20);
        }

        int modY = y + h - 19;
        if (modY > y + 58) {
            g.fill(x + 10, modY - 5, x + w - 10, modY - 4, 0x332980FF);
            drawModpackLoop(g, x + 10, modY, w - 20);
        }

        g.disableScissor();
    }

    private void drawPriorityChips(GuiGraphicsExtractor g, int x, int y, int w) {
        int gap = 6;
        int chipW = Math.max(40, (w - gap * 2) / 3);
        for (int i = 0; i < CARDS.length; i++) {
            BriefCard card = CARDS[i];
            int cx = x + i * (chipW + gap);
            int cw = i == CARDS.length - 1 ? x + w - cx : chipW;
            g.fill(cx, y, cx + cw, y + 22, 0x66121820);
            g.outline(cx, y, cw, 22, (0x88 << 24) | (card.color & 0x00FFFFFF));
            g.fill(cx, y, cx + 3, y + 22, card.color);
            g.centeredText(font,
                    fit(Component.translatable("screen.EchoAshfallProtocol.welcome.card." + card.key + ".title").getString(), cw - 8),
                    cx + cw / 2,
                    y + 7,
                    card.color);
        }
    }

    private void drawFirstTenChips(GuiGraphicsExtractor g, int x, int y, int w, boolean numbered) {
        int gap = 5;
        int chipW = Math.max(34, (w - gap * 3) / FIRST_TEN_KEYS.length);
        for (int i = 0; i < FIRST_TEN_KEYS.length; i++) {
            int cx = x + i * (chipW + gap);
            int cw = i == FIRST_TEN_KEYS.length - 1 ? x + w - cx : chipW;
            int color = FIRST_TEN_COLORS[i];
            String label = Component.translatable(FIRST_TEN_KEYS[i]).getString();
            if (numbered) {
                label = (i + 1) + " " + label;
            }

            g.fill(cx, y, cx + cw, y + 20, 0x66121820);
            g.outline(cx, y, cw, 20, (0x88 << 24) | (color & 0x00FFFFFF));
            g.fill(cx, y, cx + 3, y + 20, color);
            g.centeredText(font, fit(label, cw - 8), cx + cw / 2, y + 6, color);
        }
    }

    private void drawThreatBadges(GuiGraphicsExtractor g, int x, int y, int w) {
        int gap = 5;
        int badgeW = Math.max(34, (w - gap * 3) / THREAT_KEYS.length);
        for (int i = 0; i < THREAT_KEYS.length; i++) {
            int bx = x + i * (badgeW + gap);
            int bw = i == THREAT_KEYS.length - 1 ? x + w - bx : badgeW;
            int color = THREAT_COLORS[i];
            String label = Component.translatable(THREAT_KEYS[i]).getString();
            g.fill(bx, y, bx + bw, y + 18, 0x44121820);
            g.outline(bx, y, bw, 18, (0x66 << 24) | (color & 0x00FFFFFF));
            g.text(font, fit(label, bw - 8), bx + 4, y + 5, color, false);
        }
    }

    private void drawModpackLoop(GuiGraphicsExtractor g, int x, int y, int w) {
        g.text(font, fit(Component.translatable("screen.EchoAshfallProtocol.welcome.modpack.loop").getString(), w),
                x, y, COL_PURPLE, false);
    }

    private int drawBadge(GuiGraphicsExtractor g, int x, int y, String text, int color) {
        int w = font.width(text) + 10;
        g.fill(x, y, x + w, y + 13, 0x66121820);
        g.outline(x, y, w, 13, (0x88 << 24) | (color & 0x00FFFFFF));
        g.text(font, text, x + 5, y + 3, color, false);
        return x + w;
    }

    private String currentStatusLine(float frame) {
        int visibleIndex = Math.min(STATUS_KEYS.length - 1, Math.max(0, (int) frame / 22));
        return Component.translatable(STATUS_KEYS[visibleIndex]).getString();
    }

    private void drawTelemetry(GuiGraphicsExtractor g, int x, int y, int w, int h, float frame) {
        drawPanel(g, x, y, w, h, Component.translatable("screen.EchoAshfallProtocol.welcome.boot"), COL_CYAN);
        if (h < 30) {
            return;
        }

        g.enableScissor(x + 1, y + 1, x + w - 1, y + h - 1);
        int cy = y + 24;
        int rowH = h < 110 ? 12 : 16;
        for (int i = 0; i < STATUS_KEYS.length; i++) {
            if (cy + 9 > y + h - 6) {
                g.disableScissor();
                return;
            }
            int color = STATUS_COLORS[i];
            int localTick = Math.max(0, (int) frame - i * 22);
            String text = Component.translatable(STATUS_KEYS[i]).getString();
            int chars = Math.min(text.length(), localTick / 2);
            String shown = fit(text.substring(0, chars), w - 42);
            g.fill(x + 8, cy + 2, x + 12, cy + 6, color);
            g.text(font, shown, x + 18, cy, chars >= text.length() ? color : COL_DIM, false);
            cy += rowH;
        }
        g.disableScissor();
    }

    private void drawSurvivalBrief(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        drawPanel(g, x, y, w, h, Component.translatable("screen.EchoAshfallProtocol.welcome.first10.title"), COL_GREEN);
        if (h < 42) {
            return;
        }

        g.enableScissor(x + 1, y + 1, x + w - 1, y + h - 1);
        int cy = y + 24;
        cy = drawWrapped(g, Component.translatable("screen.EchoAshfallProtocol.welcome.brief.body"), x + 10, cy, w - 20, COL_TEXT, y + h - 44);
        if (cy + 24 <= y + h - 28) {
            drawFirstTenChips(g, x + 10, cy + 5, w - 20, true);
        }
        int calloutY = y + h - 24;
        g.fill(x + 8, calloutY, x + w - 8, calloutY + 16, 0x3338A8FF);
        g.text(font, fit(Component.translatable(welcomeCalloutKey()).getString(), w - 22),
                x + 12, calloutY + 4, COL_CYAN, false);
        g.disableScissor();
    }

    private void drawCards(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean compact) {
        int gap = 7;
        int cardW = compact ? (w - gap * 2) / 3 : (w - gap * 2) / 3;
        for (int i = 0; i < CARDS.length; i++) {
            BriefCard card = CARDS[i];
            int cx = x + i * (cardW + gap);
            drawCard(g, card, cx, y, i == CARDS.length - 1 ? x + w - cx : cardW, h, compact);
        }
    }

    private void drawCard(GuiGraphicsExtractor g, BriefCard card, int x, int y, int w, int h, boolean compact) {
        g.fill(x, y, x + w, y + h, COL_PANEL_SOFT);
        g.outline(x, y, w, h, (0x88 << 24) | (card.color & 0x00FFFFFF));
        g.fill(x, y, x + 3, y + h, card.color);
        g.enableScissor(x + 1, y + 1, x + w - 1, y + h - 1);
        g.text(font, fit(Component.translatable("screen.EchoAshfallProtocol.welcome.card." + card.key + ".title").getString(), w - 14),
                x + 9, y + 8, card.color, false);
        if (!compact || h >= 54) {
            drawWrapped(g,
                    Component.translatable("screen.EchoAshfallProtocol.welcome.card." + card.key + ".body"),
                    x + 9, y + 22, w - 14, COL_TEXT, y + h - 5);
        }
        g.disableScissor();
    }

    private void drawModpackBrief(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean condensed) {
        drawPanel(g, x, y, w, h, Component.translatable("screen.EchoAshfallProtocol.welcome.modpack.title"), COL_PURPLE);
        if (h < 30) {
            return;
        }

        g.enableScissor(x + 1, y + 1, x + w - 1, y + h - 1);
        int cy = y + 23;
        int maxY = y + h - 6;
        g.text(font, fit(Component.translatable("screen.EchoAshfallProtocol.welcome.modpack.name").getString(), w - 20),
                x + 10, cy, COL_TEXT, false);
        cy += 12;
        if (cy + 9 > maxY) {
            g.disableScissor();
            return;
        }
        g.text(font, fit(Component.translatable("screen.EchoAshfallProtocol.welcome.modpack.version").getString(), w - 20),
                x + 10, cy, COL_DIM, false);
        cy += 15;

        if (condensed) {
            if (cy + 9 <= maxY) {
                drawModpackLoop(g, x + 10, cy, w - 20);
                cy += 12;
            }
            if (cy + 9 <= maxY) {
                g.text(font, fit(Component.translatable("screen.EchoAshfallProtocol.welcome.modpack.micro").getString(), w - 20),
                        x + 10, cy, COL_TEXT, false);
            }
            g.disableScissor();
            return;
        }

        for (String key : PILLAR_KEYS) {
            if (cy + 9 > maxY) {
                g.disableScissor();
                return;
            }
            g.text(font, fit(Component.translatable(key).getString(), w - 20), x + 10, cy, COL_TEXT, false);
            cy += 11;
        }
        g.disableScissor();
    }

    private void drawFooter(GuiGraphicsExtractor g, int x, int y, int w) {
        g.fill(x + 4, y, x + w - 4, y + 1, COL_CYAN_DIM);
        int center = x + w / 2;
        String dismiss = Component.translatable("screen.EchoAshfallProtocol.welcome.skip.dismiss").getString();
        String reopen = Component.translatable("screen.EchoAshfallProtocol.welcome.skip.reopen").getString();
        int dismissW = Math.min(font.width(dismiss), w / 2 - 36);
        int reopenW = Math.min(font.width(reopen), w / 2 - 36);

        g.outline(center - dismissW - 41, y + 4, 24, 10, 0x884DBAF4);
        g.text(font, "ESC", center - dismissW - 38, y + 6, COL_CYAN, false);
        g.text(font, fit(dismiss, dismissW), center - dismissW - 12, y + 6, COL_DIM, false);

        g.outline(center + 17, y + 4, 10, 10, 0x88C8A4FF);
        g.text(font, "N", center + 20, y + 6, COL_PURPLE, false);
        g.text(font, fit(reopen, reopenW), center + 33, y + 6, COL_DIM, false);
    }

    private void drawPanel(GuiGraphicsExtractor g, int x, int y, int w, int h, Component title, int accent) {
        g.fill(x, y, x + w, y + h, COL_PANEL);
        g.outline(x, y, w, h, 0x442980FF);
        g.fill(x, y, x + w, y + 2, (0xAA << 24) | (accent & 0x00FFFFFF));
        g.text(font, fit(title.getString(), w - 18), x + 8, y + 8, accent, false);
    }

    private int drawWrapped(GuiGraphicsExtractor g, Component text, int x, int y, int maxW, int color, int maxY) {
        int cy = y;
        for (var line : font.split(text, maxW)) {
            if (cy + 9 > maxY) {
                return cy;
            }
            g.text(font, line, x, cy, color, false);
            cy += 11;
        }
        return cy;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private String fit(String text, int maxW) {
        if (font.width(text) <= maxW) {
            return text;
        }
        if (maxW <= font.width("...")) {
            return "";
        }
        return font.plainSubstrByWidth(text, maxW - font.width("...")) + "...";
    }

    private static boolean terminalInstalled() {
        return EchoRuntimeModules.isLoaded("echoterminal");
    }

    private static String welcomeCalloutKey() {
        return terminalInstalled()
                ? "screen.EchoAshfallProtocol.welcome.callout.terminal"
                : "screen.EchoAshfallProtocol.welcome.callout.guide";
    }

    private record BriefCard(String key, int color) {}
}
