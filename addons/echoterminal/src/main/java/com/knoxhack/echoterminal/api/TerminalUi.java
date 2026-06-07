package com.knoxhack.echoterminal.api;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import com.knoxhack.echoterminal.api.theme.TerminalChapterStyle;
import com.knoxhack.echoterminal.api.theme.BuiltinTerminalThemes;
import com.knoxhack.echoterminal.api.theme.TerminalIconKey;
import com.knoxhack.echoterminal.api.theme.TerminalTheme;
import com.knoxhack.echoterminal.api.theme.TerminalThemeRegistry;
import com.knoxhack.echoterminal.api.theme.TerminalThemeTokens;
import com.knoxhack.echoterminal.client.screen.TerminalClientOptions;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class TerminalUi {
    public static int CYAN = 0xFF66E8FF;
    public static int CYAN_DIM = 0xFF2E8E9D;
    public static int TEXT = 0xFFE9FBFF;
    public static int MUTED = 0xFF8CA7B5;
    public static int GREEN = 0xFF92F7A6;
    public static int AMBER = 0xFFFFD166;
    public static int RED = 0xFFFF8FA3;
    public static int PANEL = 0x6610242F;
    public static int PANEL_DARK = 0xB6050D14;
    public static int ROW = 0xFF0D171F;
    public static int ROW_SELECTED = 0xFF123241;
    private static final float TERMINAL_PANEL_ASPECT = 2.0F;
    private static final float TERMINAL_BACKDROP_ASPECT = 16.0F / 9.0F;
    private static final Map<Identifier, Boolean> TEXTURE_AVAILABILITY = new ConcurrentHashMap<>();

    public enum ImageFit {
        STRETCH,
        COVER,
        CONTAIN,
        NINE_SLICE
    }

    private TerminalUi() {
    }

    public static void applyThemeGlobals(TerminalTheme theme) {
        TerminalThemeTokens tokens = (theme == null ? TerminalThemeRegistry.defaultTheme() : theme).tokens();
        CYAN = tokens.colors().accent();
        CYAN_DIM = tokens.colors().accentDim();
        TEXT = TerminalClientOptions.highContrastMode() ? 0xFFFFFFFF : tokens.colors().text();
        MUTED = TerminalClientOptions.highContrastMode() ? 0xFFC8DDE6 : tokens.colors().muted();
        GREEN = tokens.colors().success();
        AMBER = tokens.colors().warning();
        RED = tokens.colors().danger();
        PANEL = tokens.panels().baseFill();
        PANEL_DARK = tokens.panels().darkFill();
        ROW = tokens.colors().row();
        ROW_SELECTED = tokens.colors().rowSelected();
    }

    public static TerminalTheme theme(TerminalRenderContext context) {
        return context == null ? TerminalThemeRegistry.defaultTheme() : context.theme();
    }

    public static TerminalThemeTokens tokens(TerminalRenderContext context) {
        return theme(context).tokens();
    }

    public static TerminalChapterStyle chapterStyle(TerminalRenderContext context) {
        return theme(context).chapterStyle(context == null ? null : context.themeContext());
    }

    public static int chapterAccent(TerminalRenderContext context, int fallback) {
        TerminalChapterStyle style = chapterStyle(context);
        return style == null || style.accentColor() == 0 ? fallback : style.accentColor();
    }

    public static int chapterSecondary(TerminalRenderContext context, int fallback) {
        TerminalChapterStyle style = chapterStyle(context);
        return style == null || style.secondaryColor() == 0 ? fallback : style.secondaryColor();
    }

    public static Identifier chapterPanel(TerminalRenderContext context) {
        if (!visualAssets(context)) {
            return null;
        }
        TerminalChapterStyle style = chapterStyle(context);
        Identifier panel = style == null ? null : style.panel();
        return themedVisual(context, panel == null ? tokens(context).assets().panelPlate() : panel);
    }

    public static Identifier chapterBanner(TerminalRenderContext context) {
        if (!visualAssets(context)) {
            return null;
        }
        TerminalChapterStyle style = chapterStyle(context);
        Identifier banner = style == null ? null : style.banner();
        return themedVisual(context, banner == null ? tokens(context).assets().defaultBanner() : banner);
    }

    private static boolean visualAssets(TerminalRenderContext context) {
        return !TerminalClientOptions.reducedClutterMode()
                && (context == null || context.themeContext() == null || context.themeContext().visualAssets());
    }

    private static boolean cyberglass(TerminalRenderContext context) {
        return context != null
                && BuiltinTerminalThemes.CYBERGLASS.equals(theme(context).id())
                && !TerminalClientOptions.cyberglassUseClassicLayout();
    }

    private static int cyberRadiusSmall() {
        return TerminalClientOptions.cyberglassCompact() ? 6 : 8;
    }

    private static int cyberRadiusMedium() {
        return TerminalClientOptions.cyberglassCompact() ? 8 : 12;
    }

    private static int cyberRadiusLarge() {
        return TerminalClientOptions.cyberglassCinematic() ? 18 : 16;
    }

    private static int cyberRadiusShell() {
        return TerminalClientOptions.cyberglassCinematic() ? 22 : 20;
    }

    private static int scaledGlowAlpha(int alpha) {
        if (TerminalClientOptions.reduceGlow() || TerminalClientOptions.cyberglassReduceVisualNoise()) {
            alpha = Math.round(alpha * 0.45F);
        }
        return Math.max(0, Math.min(255, Math.round(alpha * TerminalClientOptions.cyberglassGlowStrength())));
    }

    private static void glassRect(GuiGraphicsExtractor graphics, int x, int y, int w, int h,
            int radius, int fill, int border, int highlight) {
        if (w <= 0 || h <= 0) {
            return;
        }
        int r = Math.max(0, Math.min(Math.min(radius, w / 2), h / 2));
        if (r <= 1) {
            graphics.fill(x, y, x + w, y + h, fill);
            if ((border >>> 24) != 0) {
                graphics.outline(x, y, w, h, border);
            }
            return;
        }
        graphics.fill(x + r, y, x + w - r, y + h, fill);
        graphics.fill(x, y + r, x + w, y + h - r, fill);
        graphics.fill(x + 2, y + 2, x + r + 1, y + r + 1, fill);
        graphics.fill(x + w - r - 1, y + 2, x + w - 2, y + r + 1, fill);
        graphics.fill(x + 2, y + h - r - 1, x + r + 1, y + h - 2, fill);
        graphics.fill(x + w - r - 1, y + h - r - 1, x + w - 2, y + h - 2, fill);
        if ((border >>> 24) != 0) {
            graphics.fill(x + r, y, x + w - r, y + 1, border);
            graphics.fill(x + r, y + h - 1, x + w - r, y + h, border);
            graphics.fill(x, y + r, x + 1, y + h - r, border);
            graphics.fill(x + w - 1, y + r, x + w, y + h - r, border);
            graphics.fill(x + 2, y + r - 1, x + r, y + r, border);
            graphics.fill(x + r - 1, y + 2, x + r, y + r, border);
            graphics.fill(x + w - r, y + 2, x + w - r + 1, y + r, border);
            graphics.fill(x + w - r, y + r - 1, x + w - 2, y + r, border);
            graphics.fill(x + 2, y + h - r, x + r, y + h - r + 1, border);
            graphics.fill(x + r - 1, y + h - r, x + r, y + h - 2, border);
            graphics.fill(x + w - r, y + h - r, x + w - 2, y + h - r + 1, border);
            graphics.fill(x + w - r, y + h - r, x + w - r + 1, y + h - 2, border);
        }
        if ((highlight >>> 24) != 0 && w > r * 2 + 10) {
            graphics.fill(x + r + 3, y + 1, x + w - r - 3, y + 2, highlight);
        }
    }

    private static void cyberGlow(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int color, boolean strong) {
        int alpha = scaledGlowAlpha(strong ? 56 : 28);
        if (alpha <= 0) {
            return;
        }
        int glow = withAlpha(color, alpha);
        graphics.fill(x + 10, y - 1, x + Math.max(x + 12, x + w - 10), y, glow);
        graphics.fill(x + 10, y + h, x + Math.max(x + 12, x + w - 10), y + h + 1, glow);
        graphics.fill(x - 1, y + 10, x, y + Math.max(y + 12, y + h - 10), glow);
        graphics.fill(x + w, y + 10, x + w + 1, y + Math.max(y + 12, y + h - 10), glow);
    }

    private static void cyberSurface(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int w, int h, int radius, int fill, int border, int accent, boolean raised) {
        if (raised) {
            cyberGlow(graphics, x, y, w, h, accent, true);
        }
        glassRect(graphics, x, y, w, h, radius, fill, border, withAlpha(0xFFFFFFFF, raised ? 0x18 : 0x0E));
        if (w > 36 && h > 20) {
            graphics.fill(x + radius, y + 2, x + Math.min(x + w - radius, x + w / 2), y + 3,
                    withAlpha(accent, raised ? 0x82 : 0x36));
            graphics.fill(x + radius, y + h - 2, x + Math.min(x + w - radius, x + w / 3), y + h - 1,
                    withAlpha(accent, raised ? 0x60 : 0x24));
        }
    }

    private static int cyberGlassFill(TerminalRenderContext context, boolean raised, boolean active) {
        TerminalThemeTokens tokens = tokens(context);
        if (active) {
            return withAlpha(tokens.colors().rowSelected(), 0xB8);
        }
        return raised ? tokens.panels().elevatedFill() : tokens.panels().baseFill();
    }

    private static void activeOutline(GuiGraphicsExtractor graphics, int x, int y, int w, int h,
            boolean active, int color) {
        if (active) {
            graphics.outline(x, y, w, h, color);
        }
    }

    private static void interactionOutline(GuiGraphicsExtractor graphics, int x, int y, int w, int h,
            boolean selected, boolean hovered, int selectedColor, int hoverColor) {
        if (selected) {
            graphics.outline(x, y, w, h, selectedColor);
        } else if (hovered) {
            graphics.outline(x, y, w, h, hoverColor);
        }
    }

    public static int text(TerminalRenderContext context) {
        return TerminalClientOptions.highContrastMode() ? 0xFFFFFFFF : tokens(context).colors().text();
    }

    public static int muted(TerminalRenderContext context) {
        return TerminalClientOptions.highContrastMode() ? 0xFFC8DDE6 : tokens(context).colors().muted();
    }

    public static int success(TerminalRenderContext context) {
        return TerminalClientOptions.highContrastMode() ? 0xFF7CFF93 : tokens(context).colors().success();
    }

    public static int warning(TerminalRenderContext context) {
        return TerminalClientOptions.highContrastMode() ? 0xFFFFE066 : tokens(context).colors().warning();
    }

    public static int danger(TerminalRenderContext context) {
        return TerminalClientOptions.highContrastMode() ? 0xFFFF6B6B : tokens(context).colors().danger();
    }

    public static int accent(TerminalRenderContext context) {
        return TerminalClientOptions.highContrastMode() ? 0xFF7DEBFF : tokens(context).colors().accent();
    }

    public static int accentDim(TerminalRenderContext context) {
        return tokens(context).colors().accentDim();
    }

    public static Identifier themedIcon(TerminalRenderContext context, TerminalIconKey key, Identifier fallback) {
        return theme(context).icon(key, context == null ? null : context.themeContext(), fallback);
    }

    public static Identifier themedVisual(TerminalRenderContext context, Identifier texture) {
        return theme(context).visual(texture);
    }

    public static Identifier themedGroupIcon(TerminalRenderContext context, String group) {
        return themedIcon(context, TerminalIconKey.group(group), TerminalVisualAssets.terminalGroupIcon(group));
    }

    public static Identifier themedPageIcon(TerminalRenderContext context, String title) {
        return themedIcon(context, TerminalIconKey.page(semanticName(title)), TerminalVisualAssets.terminalPageIcon(title));
    }

    public static Identifier themedActionIcon(TerminalRenderContext context, String action, Identifier fallback) {
        return themedIcon(context, TerminalIconKey.action(semanticName(action)), fallback);
    }

    public static Identifier themedStateIcon(TerminalRenderContext context, String state, Identifier fallback) {
        return themedIcon(context, TerminalIconKey.state(semanticName(state)), fallback);
    }

    public static Identifier themedMissionIcon(
            TerminalRenderContext context, Identifier missionId, String category) {
        Identifier fallback = TerminalVisualAssets.missionIconArt(missionId, category);
        if (fallback != null && fallback.getPath().startsWith("textures/gui/mission_icons/")) {
            return themedVisual(context, fallback);
        }
        Identifier chapter = missionId == null ? null : themedIcon(context,
                TerminalIconKey.chapter(missionId.getNamespace()), null);
        if (chapter != null && category == null) {
            return chapter;
        }
        return themedIcon(context, TerminalIconKey.missionCategory(missionCategoryKey(category)),
                themedVisual(context, fallback));
    }

    public static Identifier themedSemanticIcon(TerminalRenderContext context, String name, Identifier fallback) {
        String key = semanticName(name);
        Identifier icon = themedIcon(context, TerminalIconKey.state(key), null);
        if (icon != null) {
            return icon;
        }
        icon = themedIcon(context, TerminalIconKey.action(key), null);
        if (icon != null) {
            return icon;
        }
        icon = themedIcon(context, TerminalIconKey.page(key), null);
        if (icon != null) {
            return icon;
        }
        return themedIcon(context, TerminalIconKey.fallback("unknown"), fallback);
    }

    private static String missionCategoryKey(String category) {
        String key = semanticName(category);
        if (key.contains("hazard") || key.contains("weather") || key.contains("storm") || key.contains("biome")) {
            return "hazard";
        }
        if (key.contains("survival") || key.contains("water") || key.contains("radiation")) {
            return "survival";
        }
        if (key.contains("craft") || key.contains("machine") || key.contains("recipe")) {
            return "crafting";
        }
        if (key.contains("tech") || key.contains("research") || key.contains("power") || key.contains("grid")) {
            return "tech";
        }
        if (key.contains("explor") || key.contains("world") || key.contains("route") || key.contains("poi")) {
            return "exploration";
        }
        if (key.contains("combat") || key.contains("guardian") || key.contains("warden") || key.contains("boss")) {
            return "combat";
        }
        if (key.contains("story") || key.contains("nexus") || key.contains("archive")) {
            return "story";
        }
        return "side_ops";
    }

    public static void section(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            String text, int x, int y, int color) {
        graphics.text(font(context), trim(context, text, Math.max(40, context.contentX() + context.contentWidth() - x)),
                x, y, opaque(color), tokens(context).typography().shadowText());
    }

    public static int sectionHeader(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            String title, String detail, int x, int y, int width, int color) {
        if (detail != null && !detail.isBlank()) {
            int detailWidth = Math.max(48, Math.min(160, width / 3));
            int detailX = x + width - detailWidth;
            line(context, graphics, title, x, y, Math.max(24, detailX - x - 8), color);
            line(context, graphics, detail, detailX, y, detailWidth, muted(context));
        } else {
            line(context, graphics, title, x, y, width, color);
        }
        divider(graphics, x, y + 14, width, color);
        return y + 20;
    }

    public static void panel(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        graphics.fill(x, y, x + w, y + h, PANEL_DARK);
    }

    public static void panel(TerminalRenderContext context, GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        TerminalThemeTokens tokens = tokens(context);
        graphics.fill(x, y, x + w, y + h, tokens.panels().darkFill());
    }

    public static void densePanel(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + h, 0xBB071017);
        graphics.fill(x, y, x + Math.max(18, Math.min(w, w / 5)), y + 1, opaque(color));
    }

    public static void densePanel(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int w, int h, int color) {
        TerminalThemeTokens tokens = tokens(context);
        graphics.fill(x, y, x + w, y + h, tokens.panels().elevatedFill());
        graphics.fill(x, y, x + Math.max(18, Math.min(w, w / 5)), y + 1, opaque(color));
    }

    public static void flatHudPanel(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + h, 0xE8071017);
        graphics.fill(x + 1, y + 1, x + w - 1, y + Math.min(h - 1, 22), 0x1466E8FF);
        graphics.fill(x, y, x + Math.max(28, Math.min(w, w / 5)), y + 2, opaque(color));
        graphics.fill(x, y + h - 2, x + Math.max(24, Math.min(w, w / 7)), y + h, opaque(color));
        if (w > 56 && h > 26) {
            graphics.fill(x + 8, y + 8, x + w - 8, y + 9, 0x1C38DFF4);
        }
    }

    public static void flatHudPanel(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int w, int h, int color) {
        if (cyberglass(context)) {
            cyberSurface(context, graphics, x, y, w, h, cyberRadiusMedium(),
                    cyberGlassFill(context, false, false), tokens(context).borders().subtle(), color, false);
            return;
        }
        TerminalThemeTokens tokens = tokens(context);
        graphics.fill(x, y, x + w, y + h, tokens.panels().elevatedFill());
    }

    public static void cinematicPanel(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + h, 0xD8050C13);
        graphics.fill(x + 1, y + 1, x + w - 1, y + Math.min(h - 1, 26), 0x18163843);
        graphics.fill(x, y, x + Math.min(w, Math.max(42, w / 5)), y + 2, opaque(color));
        graphics.fill(x, y + h - 2, x + Math.min(w, Math.max(36, w / 6)), y + h, opaque(color));
        graphics.fill(x + w - Math.min(w, 38), y + h - 2, x + w, y + h, 0x4438DFF4);
        if (w > 48 && h > 44) {
            graphics.fill(x + 10, y + 10, x + w - 10, y + 11, 0x1766E8FF);
            graphics.fill(x + 10, y + h - 11, x + w - 10, y + h - 10, 0x182E8E9D);
        }
    }

    public static void cinematicHeroPanel(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            Identifier texture, int x, int y, int w, int h, String title, String detail, int color) {
        imagePanel(context, graphics, texture, x, y, w, h, color, 0.76F, false, ImageFit.COVER);
        cinematicPanel(context, graphics, x, y, w, h, color);
        line(context, graphics, title, x + 10, y + 10, Math.max(40, w - 20), color);
        if (detail != null && !detail.isBlank()) {
            wrap(context, graphics, detail, x + 10, y + 25, Math.max(40, w - 20), text(context));
        }
    }

    public static void imagePanel(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            Identifier texture, int x, int y, int w, int h, int color, float darken, boolean frame) {
        imagePanel(graphics, visualAssets(context) ? themedVisual(context, texture) : null,
                x, y, w, h, color, darken, frame);
    }

    public static void imagePanel(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            Identifier texture, int x, int y, int w, int h, int color, float darken, boolean frame, ImageFit fit) {
        imagePanel(graphics, visualAssets(context) ? themedVisual(context, texture) : null,
                x, y, w, h, color, darken, frame, fit);
    }

    public static void imagePanel(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            Identifier texture, int x, int y, int w, int h, int color, float darken, boolean frame,
            ImageFit fit, float sourceAspect) {
        imagePanel(graphics, visualAssets(context) ? themedVisual(context, texture) : null,
                x, y, w, h, color, darken, frame, fit, sourceAspect);
    }

    public static void imagePanel(GuiGraphicsExtractor graphics,
            Identifier texture, int x, int y, int w, int h, int color, float darken, boolean frame) {
        imagePanel(graphics, texture, x, y, w, h, color, darken, frame, ImageFit.COVER);
    }

    public static void imagePanel(GuiGraphicsExtractor graphics,
            Identifier texture, int x, int y, int w, int h, int color, float darken, boolean frame, ImageFit fit) {
        imagePanel(graphics, texture, x, y, w, h, color, darken, frame, fit, TERMINAL_PANEL_ASPECT);
    }

    public static void imagePanel(GuiGraphicsExtractor graphics,
            Identifier texture, int x, int y, int w, int h, int color, float darken, boolean frame,
            ImageFit fit, float sourceAspect) {
        if (!textureAvailable(texture)) {
            fallbackVisualPanel(graphics, x, y, w, h, color);
        } else {
            blitFitted(graphics, texture, x, y, w, h, fit, sourceAspect);
        }
        int alpha = Math.max(0, Math.min(230, Math.round(darken * 255.0F)));
        graphics.fill(x, y, x + w, y + h, (alpha << 24) | 0x071017);
        if (frame) {
            graphics.fill(x, y, x + Math.max(22, Math.min(w, w / 4)), y + 2, opaque(color));
            graphics.fill(x, y + h - 2, x + Math.max(22, Math.min(w, w / 5)), y + h, opaque(color));
        }
    }

    public static void cardPlate(GuiGraphicsExtractor graphics,
            Identifier texture, int x, int y, int w, int h, int color, float darken) {
        cardPlate(graphics, texture, x, y, w, h, color, darken, ImageFit.COVER);
    }

    public static void cardPlate(GuiGraphicsExtractor graphics,
            Identifier texture, int x, int y, int w, int h, int color, float darken, ImageFit fit) {
        graphics.fill(x, y, x + w, y + h, PANEL_DARK);
        if (w > 6 && h > 6) {
            graphics.fill(x + 1, y + 1, x + w - 1, y + Math.min(y + h - 1, y + 18), 0x1638DFF4);
        }
        graphics.fill(x, y, x + Math.max(34, Math.min(w, w / 5)), y + 2, opaque(color));
        graphics.fill(x, y + h - 2, x + Math.max(28, Math.min(w, w / 7)), y + h, opaque(color));
    }

    public static void cardPlate(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            Identifier texture, int x, int y, int w, int h, int color, float darken) {
        cardPlate(context, graphics, texture, x, y, w, h, color, darken, ImageFit.COVER);
    }

    public static void cardPlate(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            Identifier texture, int x, int y, int w, int h, int color, float darken, ImageFit fit) {
        if (cyberglass(context)) {
            cyberSurface(context, graphics, x, y, w, h, cyberRadiusMedium(),
                    tokens(context).panels().baseFill(), tokens(context).borders().subtle(), color, false);
            return;
        }
        TerminalThemeTokens tokens = tokens(context);
        graphics.fill(x, y, x + w, y + h, tokens.panels().darkFill());
    }

    public static void hdBackplatePanel(GuiGraphicsExtractor graphics,
            Identifier texture, int x, int y, int w, int h, int color, float darken, ImageFit fit) {
        graphics.fill(x, y, x + w, y + h, PANEL_DARK);
        if (w > 6 && h > 6) {
            graphics.fill(x + 1, y + 1, x + w - 1, y + Math.min(y + h - 1, y + 24), 0x1838DFF4);
        }
        graphics.fill(x, y, x + Math.max(42, Math.min(w, w / 6)), y + 2, opaque(color));
        graphics.fill(x, y + h - 2, x + Math.max(34, Math.min(w, w / 8)), y + h, opaque(color));
        if (w > 72 && h > 34) {
            graphics.fill(x + 10, y + 10, x + w - 10, y + 11, 0x1838DFF4);
        }
    }

    public static void texturedPanel(GuiGraphicsExtractor graphics,
            Identifier texture, int x, int y, int w, int h, int color, float darken) {
        cardPlate(graphics, texture, x, y, w, h, color, darken);
    }

    public static void texturedPanel(GuiGraphicsExtractor graphics,
            Identifier texture, int x, int y, int w, int h, int color, float darken, ImageFit fit) {
        cardPlate(graphics, texture, x, y, w, h, color, darken, fit);
    }

    public static int dataPanel(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            Identifier texture, int x, int y, int w, int h, String title, String detail, int color, float darken) {
        cardPlate(context, graphics, texture, x, y, w, h, color, darken);
        return sectionHeader(context, graphics, title, detail, x + 14, y + 14, Math.max(24, w - 28), color);
    }

    public static int dataPanel(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            Identifier texture, int x, int y, int w, int h, String title, String detail,
            int color, float darken, ImageFit fit) {
        cardPlate(context, graphics, texture, x, y, w, h, color, darken, fit);
        return sectionHeader(context, graphics, title, detail, x + 14, y + 14, Math.max(24, w - 28), color);
    }

    public static int flatDataPanel(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int w, int h, String title, String detail, int color) {
        flatHudPanel(context, graphics, x, y, w, h, color);
        return sectionHeader(context, graphics, title, detail, x + 14, y + 14, Math.max(24, w - 28), color);
    }

    public static void filterToolbarPanel(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int w, int h, int color) {
        if (cyberglass(context)) {
            cyberSurface(context, graphics, x, y, w, h, cyberRadiusMedium(),
                    withAlpha(tokens(context).colors().content(), 0x9A),
                    tokens(context).borders().subtle(), color, false);
            return;
        }
        TerminalThemeTokens tokens = tokens(context);
        cardPlate(context, graphics, TerminalVisualAssets.CARD_FILTER_TOOLBAR_PLATE, x, y, w, h, color,
                Math.min(0.82F, tokens.panels().imageDarken() + 0.08F), ImageFit.NINE_SLICE);
        graphics.fill(x + 1, y + 1, x + w - 1, y + Math.min(h - 1, 12), tokens.panels().headerFill());
        graphics.fill(x + 8, y + Math.max(4, h - 5), x + Math.max(x + 28, x + w / 4),
                y + Math.max(5, h - 3), opaque(color));
    }

    public static void actionBarPanel(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int w, int h, int color) {
        if (cyberglass(context)) {
            cyberSurface(context, graphics, x, y, w, h, cyberRadiusLarge(),
                    withAlpha(tokens(context).colors().content(), 0xC8),
                    tokens(context).borders().normal(), color, true);
            graphics.fill(x + 12, y + 1, x + Math.max(x + 28, x + w / 3), y + 2, withAlpha(color, 0x7E));
            return;
        }
        TerminalThemeTokens tokens = tokens(context);
        cardPlate(context, graphics, TerminalVisualAssets.CARD_ACTION_BAR_PLATE, x, y, w, h, color,
                Math.min(0.80F, tokens.panels().imageDarken() + 0.10F), ImageFit.NINE_SLICE);
        graphics.fill(x + 1, y + 1, x + w - 1, y + Math.min(h - 1, 16), withAlpha(color, 0x10));
        graphics.fill(x, y, x + 3, y + h, withAlpha(color, 0xC8));
        graphics.fill(x + Math.max(34, w / 4), y + h - 2, x + w - 8, y + h - 1,
                withAlpha(color, 0x66));
    }

    public static int iconTitleHeader(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            Identifier texture, TerminalIcon fallback, int x, int y, int w, int h,
            String title, String detail, String status, int color, int statusColor) {
        cardPlate(context, graphics, chapterPanel(context), x, y, w, h, color,
                tokens(context).panels().imageDarken());
        int iconSize = Math.min(56, Math.max(34, h - 36));
        hybridIconBadge(graphics, texture, fallback, x + 14, y + Math.max(12, (h - iconSize) / 2), iconSize,
                color, true);
        int textX = x + iconSize + 28;
        int pillW = status == null || status.isBlank() ? 0 : Math.max(70, Math.min(112, w / 5));
        line(context, graphics, title, textX, y + 18, Math.max(40, w - (textX - x) - pillW - 22), text(context));
        if (detail != null && !detail.isBlank()) {
            wrap(context, graphics, detail, textX, y + 36, Math.max(40, w - (textX - x) - 18), muted(context));
        }
        if (pillW > 0) {
            miniStatusPill(context, graphics, status, x + w - pillW - 14, y + 18, pillW,
                    statusColor, true);
        }
        return y + h;
    }

    public static int v2HeroHeader(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            Identifier artTexture, Identifier iconTexture, TerminalIcon fallback, int x, int y, int w, int h,
            String title, String detail, String body, String primaryStatus, String secondaryStatus,
            float progressValue, int color, int statusColor, boolean useArt) {
        int safeW = Math.max(80, w);
        int safeH = Math.max(92, h);
        if (cyberglass(context)) {
            cyberSurface(context, graphics, x, y, safeW, safeH, cyberRadiusLarge(),
                    tokens(context).colors().content(), tokens(context).borders().normal(), color, true);
            if (useArt && artTexture != null) {
                imagePanel(context, graphics, artTexture, x + 1, y + 1, safeW - 2, safeH - 2,
                        color, 0.42F, false, ImageFit.COVER);
                graphics.fill(x + 1, y + 1, x + safeW - 1, y + safeH - 1, 0x66010711);
                graphics.fill(x + 1, y + safeH / 2, x + safeW - 1, y + safeH - 1, 0xA4010711);
            } else {
                graphics.fill(x + 1, y + 1, x + safeW - 1, y + safeH - 1, withAlpha(color, 0x12));
            }
            int iconSize = Math.min(42, Math.max(30, safeH / 3));
            int iconX = x + 14;
            int chipY = y + 14;
            hybridIconBadge(context, graphics, iconTexture, fallback, iconX, chipY, iconSize, color, true);
            int textX = iconX + iconSize + 14;
            int statusW = Math.max(72, Math.min(116, safeW / 5));
            boolean showStatus = primaryStatus != null && !primaryStatus.isBlank() && safeW >= 260;
            int rightInset = showStatus ? statusW + 28 : 16;
            int titleW = Math.max(48, safeW - (textX - x) - rightInset);
            if (detail != null && !detail.isBlank()) {
                miniStatusPill(context, graphics, detail, textX, chipY + 2,
                        Math.min(Math.max(70, statusBadgeWidth(context, detail)), Math.max(70, titleW)),
                        color, false);
            }
            if (showStatus) {
                missionStatusPill(context, graphics, primaryStatus, x + safeW - statusW - 14, chipY + 1, statusW);
                if (secondaryStatus != null && !secondaryStatus.isBlank()) {
                    miniStatusPill(context, graphics, secondaryStatus, x + safeW - statusW - 14,
                            chipY + 19, statusW, statusColor, false);
                }
            }
            int titleY = y + Math.max(42, safeH - 58);
            line(context, graphics, title == null ? "" : title, textX, titleY, titleW, text(context));
            int bodyY = titleY + 17;
            wrap(context, graphics, body == null ? "" : body, textX, bodyY,
                    Math.max(48, safeW - (textX - x) - 18), statusColor == muted(context) ? muted(context) : text(context));
            progress(context, graphics, x + 14, y + safeH - 12, safeW - 28, 5, progressValue, color);
            return y + safeH;
        }
        if (useArt && artTexture != null) {
            imagePanel(context, graphics, artTexture, x, y, safeW, safeH, color, 0.42F, true, ImageFit.COVER);
            graphics.fill(x, y, x + safeW, y + safeH, 0x58071117);
        } else {
            flatHudPanel(context, graphics, x, y, safeW, safeH, color);
        }
        int iconSize = Math.min(52, Math.max(34, safeH - 48));
        int iconX = x + 12;
        int iconY = y + Math.max(12, (safeH - iconSize) / 2);
        hybridIconBadge(graphics, iconTexture, fallback, iconX, iconY, iconSize, color, true);

        boolean showStatus = primaryStatus != null && !primaryStatus.isBlank() && safeW >= 270;
        int pillW = showStatus ? Math.max(82, Math.min(126, safeW / 5)) : 0;
        int pillX = x + safeW - pillW - 14;
        int textX = iconX + iconSize + 16;
        int textRightInset = showStatus ? pillW + 28 : 16;
        int textW = Math.max(48, safeW - (textX - x) - textRightInset);
        line(context, graphics, title == null ? "" : title, textX, y + 18, textW, text(context));
        if (detail != null && !detail.isBlank()) {
            line(context, graphics, detail, textX, y + 34, textW, color);
        }
        if (showStatus) {
            missionStatusPill(context, graphics, primaryStatus, pillX, y + 16, pillW);
            if (secondaryStatus != null && !secondaryStatus.isBlank()) {
                miniStatusPill(context, graphics, secondaryStatus, pillX, y + 34, pillW, statusColor, false);
            }
        }

        String safeBody = body == null ? "" : body;
        int bodyY = y + 52;
        int bodyW = Math.max(48, safeW - (textX - x) - 16);
        int bodyColor = statusColor == muted(context) ? muted(context) : text(context);
        if (safeH - (bodyY - y) < 34) {
            line(context, graphics, safeBody, textX, bodyY, bodyW, bodyColor);
        } else {
            wrap(context, graphics, safeBody, textX, bodyY, bodyW, bodyColor);
        }
        progress(graphics, x + 12, y + safeH - 14, safeW - 24, 6, progressValue, color);
        return y + safeH;
    }

    public static void dataListRow(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int w, int h, String title, String detail, String status,
            boolean selected, boolean hovered, int color, int statusColor) {
        TerminalThemeTokens tokens = tokens(context);
        int bg = hovered ? tokens.panels().hoverFill() : tokens.colors().row();
        graphics.fill(x, y, x + w, y + h, bg);
        if (selected) {
            graphics.fill(x, y, x + w, y + h, withAlpha(color, 0x24));
        }
        activeOutline(graphics, x, y, w, h, selected || hovered,
                selected ? withAlpha(color, 0xC0) : tokens.borders().normal());
        graphics.fill(x, y, x + 3, y + h, selected ? withAlpha(color, 0xDD) : tokens.dividers().line());
        if (selected) {
            graphics.fill(x + Math.max(28, w / 3), y + h - 2, x + w - 8, y + h - 1, withAlpha(color, 0x88));
        }
        int pillW = status == null || status.isBlank() ? 0 : Math.max(68, Math.min(112, w / 3));
        line(context, graphics, title, x + 10, y + Math.max(5, h >= 30 ? 7 : (h - 8) / 2),
                Math.max(40, w - pillW - 24), selected ? text(context) : muted(context));
        if (detail != null && !detail.isBlank() && h >= 30) {
            line(context, graphics, detail, x + 10, y + 20, Math.max(40, w - 20), muted(context));
        }
        if (pillW > 0) {
            miniStatusPill(context, graphics, status, x + w - pillW - 10, y + Math.max(3, (h - 14) / 2),
                    pillW, statusColor, selected);
        }
    }

    public static void iconDataListRow(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            Identifier texture, TerminalIcon fallback, int x, int y, int w, int h,
            String title, String detail, String status, boolean selected, boolean hovered,
            int color, int statusColor, boolean iconActive) {
        TerminalThemeTokens tokens = tokens(context);
        int bg = hovered ? tokens.panels().hoverFill() : tokens.colors().row();
        graphics.fill(x, y, x + w, y + h, bg);
        if (selected) {
            graphics.fill(x, y, x + w, y + h, withAlpha(color, 0x24));
        }
        activeOutline(graphics, x, y, w, h, selected || hovered,
                selected ? withAlpha(color, 0xC0) : tokens.borders().normal());
        graphics.fill(x, y, x + 3, y + h, selected ? withAlpha(color, 0xDD) : tokens.dividers().line());
        if (selected) {
            graphics.fill(x + Math.max(34, w / 3), y + h - 2, x + w - 8, y + h - 1, withAlpha(color, 0x88));
        }
        int iconSize = Math.min(18, Math.max(12, h - 8));
        hybridIcon(graphics, texture, fallback, x + 8, y + Math.max(4, (h - iconSize) / 2),
                iconSize, statusColor, iconActive);
        int pillW = status == null || status.isBlank() ? 0 : Math.max(68, Math.min(112, w / 3));
        int textX = x + iconSize + 22;
        line(context, graphics, title, textX, y + Math.max(5, h >= 30 ? 7 : (h - 8) / 2),
                Math.max(40, w - (textX - x) - pillW - 16), selected ? text(context) : muted(context));
        if (detail != null && !detail.isBlank() && h >= 30) {
            line(context, graphics, detail, textX, y + 20, Math.max(40, w - (textX - x) - 10), muted(context));
        }
        if (pillW > 0) {
            miniStatusPill(context, graphics, status, x + w - pillW - 10, y + Math.max(3, (h - 14) / 2),
                    pillW, statusColor, selected);
        }
    }

    public static int imageHero(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            Identifier texture, int x, int y, int w, int h, int color) {
        imagePanel(context, graphics, texture, x, y, w, h, color, 0.62F, true);
        return y + h + 8;
    }

    private static void drawPanelTexture(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            Identifier texture, int x, int y, int w, int h, float darken) {
        return;
    }

    public static void questArtCard(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            Identifier texture, int x, int y, int w, int h, int color, boolean selected, boolean hovered) {
        if (cyberglass(context)) {
            cyberSurface(context, graphics, x, y, w, h, cyberRadiusLarge(),
                    tokens(context).colors().content(), selected ? withAlpha(color, 0xBB) : tokens(context).borders().normal(),
                    color, selected || hovered);
            imagePanel(context, graphics, texture, x + 1, y + 1, Math.max(1, w - 2), Math.max(1, h - 2),
                    color, selected ? 0.38F : 0.46F, false, ImageFit.COVER);
            graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0x2800D9FF);
            graphics.fill(x + 1, y + Math.max(y + 1, y + h - 40), x + w - 1, y + h - 1, 0x8C010711);
            if (hovered || selected) {
                cyberGlow(graphics, x, y, w, h, color, selected);
            }
            return;
        }
        imagePanel(context, graphics, texture, x, y, w, h, color, selected ? 0.68F : 0.78F, false);
        if (hovered) {
            graphics.fill(x, y, x + w, y + h, 0x22163843);
        }
        activeOutline(graphics, x, y, w, h, selected || hovered,
                selected ? opaque(color) : 0x4438DFF4);
        graphics.fill(x, y, x + 3, y + h, selected ? opaque(color) : 0x7738DFF4);
        graphics.fill(x, y + h - 2, x + w, y + h, selected ? opaque(color) : 0x5538DFF4);
    }

    private static void fallbackVisualPanel(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + h, 0xEE071017);
        graphics.fill(x + 1, y + 1, x + w - 1, y + Math.max(2, h / 3), 0x33163843);
        for (int i = 0; i < 4; i++) {
            int lineY = y + 8 + i * Math.max(8, h / 5);
            graphics.fill(x + 8, lineY, x + Math.max(18, w - 8 - i * 14), lineY + 1, 0x33244352);
        }
        graphics.fill(x, y, x + Math.max(22, w / 5), y + 2, opaque(color));
    }

    private static void blitFitted(GuiGraphicsExtractor graphics, Identifier texture,
            int x, int y, int w, int h, ImageFit fit, float sourceAspect) {
        if (w <= 0 || h <= 0) {
            return;
        }
        ImageFit mode = fit == null ? ImageFit.STRETCH : fit;
        if (mode == ImageFit.NINE_SLICE) {
            blitNineSlice(graphics, texture, x, y, w, h);
            return;
        }
        float safeSourceAspect = sourceAspect <= 0.0F ? TERMINAL_PANEL_ASPECT : sourceAspect;
        if (mode == ImageFit.CONTAIN) {
            float destAspect = w / (float) h;
            int drawW = w;
            int drawH = h;
            int drawX = x;
            int drawY = y;
            if (destAspect > safeSourceAspect) {
                drawW = Math.max(1, Math.round(h * safeSourceAspect));
                drawX = x + (w - drawW) / 2;
            } else if (destAspect < safeSourceAspect) {
                drawH = Math.max(1, Math.round(w / safeSourceAspect));
                drawY = y + (h - drawH) / 2;
            }
            graphics.fill(x, y, x + w, y + h, 0xFF071017);
            graphics.blit(texture, drawX, drawY, drawX + drawW, drawY + drawH,
                    0.0F, 1.0F, 0.0F, 1.0F);
            return;
        }

        float u0 = 0.0F;
        float u1 = 1.0F;
        float v0 = 0.0F;
        float v1 = 1.0F;
        if (mode == ImageFit.COVER) {
            float destAspect = w / (float) h;
            if (destAspect > safeSourceAspect) {
                float visibleV = Math.max(0.0F, Math.min(1.0F, safeSourceAspect / destAspect));
                v0 = (1.0F - visibleV) * 0.5F;
                v1 = 1.0F - v0;
            } else if (destAspect < safeSourceAspect) {
                float visibleU = Math.max(0.0F, Math.min(1.0F, destAspect / safeSourceAspect));
                u0 = (1.0F - visibleU) * 0.5F;
                u1 = 1.0F - u0;
            }
        }
        graphics.blit(texture, x, y, x + w, y + h, u0, u1, v0, v1);
    }

    private static void blitNineSlice(GuiGraphicsExtractor graphics, Identifier texture, int x, int y, int w, int h) {
        if (graphics == null || texture == null || w <= 0 || h <= 0) {
            return;
        }
        TextureDrawSpec spec = textureSpec(texture);
        int sourceWidth = Math.max(1, spec.sourceWidth());
        int sourceHeight = Math.max(1, spec.sourceHeight());
        int slice = Math.max(1, spec.slice());
        int left = Math.min(Math.min(slice, sourceWidth / 2), Math.max(1, w / 2));
        int right = Math.min(Math.min(slice, sourceWidth - left), Math.max(0, w - left));
        int top = Math.min(Math.min(slice, sourceHeight / 2), Math.max(1, h / 2));
        int bottom = Math.min(Math.min(slice, sourceHeight - top), Math.max(0, h - top));
        int[] dx = {x, x + left, x + Math.max(left, w - right), x + w};
        int[] dy = {y, y + top, y + Math.max(top, h - bottom), y + h};
        int[] sx = {0, slice, Math.max(slice, sourceWidth - slice), sourceWidth};
        int[] sy = {0, slice, Math.max(slice, sourceHeight - slice), sourceHeight};
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                if (dx[col + 1] <= dx[col] || dy[row + 1] <= dy[row]
                        || sx[col + 1] <= sx[col] || sy[row + 1] <= sy[row]) {
                    continue;
                }
                graphics.blit(texture, dx[col], dy[row], dx[col + 1], dy[row + 1],
                        sx[col] / (float) sourceWidth, sx[col + 1] / (float) sourceWidth,
                        sy[row] / (float) sourceHeight, sy[row + 1] / (float) sourceHeight);
            }
        }
    }

    private static TextureDrawSpec textureSpec(Identifier texture) {
        if (texture == null) {
            return TextureDrawSpec.DEFAULT;
        }
        String path = texture.getPath();
        if (path.endsWith("/terminal_frame_backdrop.png")) {
            return new TextureDrawSpec(1280, 720, 48);
        }
        if (path.endsWith("/action_bar_plate.png")) {
            return new TextureDrawSpec(512, 88, 18);
        }
        if (path.endsWith("/empty_state_plate.png")) {
            return new TextureDrawSpec(512, 96, 18);
        }
        if (path.endsWith("/filter_toolbar_plate.png")) {
            return new TextureDrawSpec(512, 48, 12);
        }
        if (path.contains("/terminal/cards/")) {
            return new TextureDrawSpec(1024, 512, 48);
        }
        if (path.contains("/mission_heroes/")) {
            return new TextureDrawSpec(448, 224, 24);
        }
        if (path.contains("/terminal/mission_")) {
            return new TextureDrawSpec(512, 256, 32);
        }
        if (path.contains("/terminal/")) {
            return new TextureDrawSpec(1024, 512, 48);
        }
        if (path.contains("/panels/")) {
            return new TextureDrawSpec(1024, 512, 48);
        }
        return TextureDrawSpec.DEFAULT;
    }

    private record TextureDrawSpec(int sourceWidth, int sourceHeight, int slice) {
        private static final TextureDrawSpec DEFAULT = new TextureDrawSpec(1024, 512, 48);
    }

    public static void divider(GuiGraphicsExtractor graphics, int x, int y, int w, int color) {
        graphics.fill(x, y, x + w, y + 1, 0x55244352);
        graphics.fill(x, y, x + Math.max(12, w / 5), y + 1, opaque(color));
    }

    public static void selectableRow(GuiGraphicsExtractor graphics, int x, int y, int w, int h,
            boolean selected, boolean hovered, int accentColor) {
        int bg = selected ? ROW_SELECTED : (hovered ? 0xFF102630 : ROW);
        graphics.fill(x, y, x + w, y + h, bg);
        if (selected) {
            graphics.fill(x, y + h - 2, x + w, y + h, opaque(accentColor));
        }
    }

    public static void selectableRow(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int w, int h, boolean selected, boolean hovered, int accentColor) {
        TerminalThemeTokens tokens = tokens(context);
        int bg = selected ? tokens.panels().selectedFill()
                : hovered ? tokens.panels().hoverFill() : tokens.colors().row();
        graphics.fill(x, y, x + w, y + h, bg);
        activeOutline(graphics, x, y, w, h, selected || hovered,
                selected ? opaque(accentColor) : tokens.borders().normal());
        if (selected) {
            graphics.fill(x, y + h - 2, x + w, y + h, opaque(accentColor));
        }
    }

    public static void roadmapMissionRow(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int w, int h, boolean selected, boolean hovered, boolean emphasized, int accentColor) {
        if (cyberglass(context)) {
            TerminalThemeTokens tokens = tokens(context);
            int fill = selected ? withAlpha(accentColor, 0x2C)
                    : hovered ? tokens.panels().hoverFill()
                    : emphasized ? withAlpha(accentColor, 0x16)
                    : tokens.colors().row();
            int border = selected ? withAlpha(accentColor, 0xB8)
                    : hovered ? tokens.borders().normal()
                    : tokens.borders().subtle();
            cyberSurface(context, graphics, x, y, w, h, cyberRadiusMedium(), fill, border, accentColor,
                    selected || hovered);
            graphics.fill(x + 1, y + cyberRadiusSmall(), x + 3, y + h - cyberRadiusSmall(),
                    selected || emphasized ? withAlpha(accentColor, 0xD8) : tokens.dividers().line());
            if (selected) {
                graphics.fill(x + 36, y + h - 3, x + Math.max(x + 40, x + w - 18), y + h - 2,
                        withAlpha(accentColor, 0xB0));
            }
            return;
        }
        TerminalThemeTokens tokens = tokens(context);
        graphics.fill(x, y, x + w, y + h, tokens.colors().row());
        if (selected || hovered || emphasized) {
            int overlayAlpha = selected ? 0x2E : hovered ? 0x22 : 0x16;
            graphics.fill(x, y, x + w, y + h, withAlpha(accentColor, overlayAlpha));
        }
        interactionOutline(graphics, x, y, w, h, selected, hovered,
                withAlpha(accentColor, 0xC8), tokens.borders().normal());
        int stripW = selected ? 3 : emphasized ? 2 : 1;
        graphics.fill(x, y, x + stripW, y + h, emphasized || selected
                ? opaque(accentColor)
                : tokens.dividers().line());
        if (selected) {
            graphics.fill(x + Math.max(24, w / 4), y + h - 2, x + w - 8, y + h,
                    withAlpha(accentColor, 0xD0));
        } else if (hovered) {
            graphics.fill(x + 8, y + h - 1, x + w - 8, y + h, withAlpha(accentColor, 0x99));
        }
    }

    public static void chip(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            String label, int x, int y, int width, int color) {
        graphics.fill(x, y, x + width, y + 13, 0xFF10232C);
        graphics.fill(x, y + 11, x + width, y + 13, opaque(color));
        graphics.centeredText(font(context), trim(context, label, width - 6), x + width / 2, y + 3, opaque(color));
    }

    public static void statusPill(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            String label, int x, int y, int width, int color, boolean selected) {
        if (isSemanticStatus(label)) {
            drawSemanticStatusPill(context, graphics, label, x, y, width, 14);
            return;
        }
        if (cyberglass(context)) {
            int bg = selected ? withAlpha(color, 0x32) : withAlpha(tokens(context).colors().content(), 0xA8);
            glassRect(graphics, x, y, width, 14, cyberRadiusSmall(), bg,
                    selected ? withAlpha(color, 0xA8) : tokens(context).borders().subtle(),
                    withAlpha(0xFFFFFFFF, 0x12));
            graphics.centeredText(font(context), trim(context, label, width - 8), x + width / 2, y + 4,
                    selected ? text(context) : opaque(color));
            return;
        }
        int bg = selected ? ROW_SELECTED : 0xFF10232C;
        graphics.fill(x, y, x + width, y + 14, bg);
        graphics.outline(x, y, width, 14, selected ? opaque(color) : 0x5538DFF4);
        graphics.fill(x, y + 12, x + width, y + 14, opaque(color));
        graphics.centeredText(font(context), trim(context, label, width - 6), x + width / 2, y + 4,
                selected ? text(context) : opaque(color));
    }

    public static void miniStatusPill(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            String label, int x, int y, int width, int color, boolean filled) {
        if (isSemanticStatus(label)) {
            drawSemanticStatusPill(context, graphics, label, x, y, width, 14);
            return;
        }
        if (cyberglass(context)) {
            int bg = filled ? withAlpha(color, 0x2D) : withAlpha(tokens(context).colors().content(), 0x8E);
            glassRect(graphics, x, y, width, 14, cyberRadiusSmall(), bg,
                    filled ? withAlpha(color, 0x9C) : tokens(context).borders().subtle(),
                    withAlpha(0xFFFFFFFF, 0x10));
            if (filled && width > 18) {
                graphics.fill(x + 5, y + 4, x + 8, y + 7, withAlpha(color, 0xDD));
            }
            graphics.centeredText(font(context), trim(context, label, width - 10), x + width / 2, y + 4,
                    filled ? text(context) : opaque(color));
            return;
        }
        TerminalThemeTokens tokens = tokens(context);
        int bg = filled ? withAlpha(color, 0x34) : tokens.colors().row();
        int text = filled ? tokens.colors().text() : opaque(color);
        graphics.fill(x, y, x + width, y + 14, bg);
        graphics.outline(x, y, width, 14, filled ? withAlpha(color, 0xBB) : tokens.borders().normal());
        graphics.fill(x, y + 12, x + width, y + 14, withAlpha(color, filled ? 0xD0 : 0xA0));
        graphics.centeredText(font(context), trim(context, label, width - 8), x + width / 2, y + 4, text);
    }

    public static int statusBadgeWidth(TerminalRenderContext context, String label) {
        String value = TerminalDesignTokens.normalizeStatus(label);
        return Math.max(46, font(context).width(value) + 18);
    }

    public static int statusBadgeRowsHeight(TerminalRenderContext context, Iterable<String> labels, int width) {
        int rows = 0;
        int cx = 0;
        for (String label : labels) {
            if (label == null || label.isBlank()) {
                continue;
            }
            int badgeW = Math.min(Math.max(34, width), statusBadgeWidth(context, label));
            if (cx > 0 && cx + badgeW > width) {
                rows++;
                cx = 0;
            }
            cx += badgeW + 4;
        }
        if (cx > 0) {
            rows++;
        }
        return rows * 16;
    }

    public static int statusBadgeRow(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            Iterable<String> labels, int x, int y, int width, int color) {
        int cx = x;
        int cy = y;
        boolean drewAny = false;
        for (String label : labels) {
            if (label == null || label.isBlank()) {
                continue;
            }
            int badgeW = Math.min(Math.max(34, width), statusBadgeWidth(context, label));
            if (cx > x && cx + badgeW > x + width) {
                cx = x;
                cy += 16;
            }
            miniStatusPill(context, graphics, label, cx, cy, badgeW, color, false);
            cx += badgeW + 4;
            drewAny = true;
        }
        return drewAny ? cy + 16 : y;
    }

    public static void missionStatusPill(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            String label, int x, int y, int width) {
        drawSemanticStatusPill(context, graphics, label, x, y, width, 14);
    }

    private static boolean isSemanticStatus(String label) {
        return TerminalDesignTokens.semanticStatus(label);
    }

    private static void drawSemanticStatusPill(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            String label, int x, int y, int width, int height) {
        String value = TerminalDesignTokens.normalizeStatus(label);
        TerminalThemeTokens tokens = tokens(context);
        int color = TerminalDesignTokens.statusColor(context, value, tokens.colors().accent());
        int bg;
        int border;
        int text;
        switch (value) {
            case "READY", "CLAIM", "OPEN", "ONLINE" -> {
                bg = withAlpha(success(context), TerminalClientOptions.highContrastMode() ? 0x46 : 0x2B);
                border = withAlpha(success(context), 0xBB);
                text = success(context);
            }
            case "ACTIVE" -> {
                bg = withAlpha(accent(context), TerminalClientOptions.highContrastMode() ? 0x44 : 0x2C);
                border = withAlpha(accent(context), 0xBB);
                text = TerminalClientOptions.highContrastMode() ? text(context) : accent(context);
            }
            case "WARNING" -> {
                bg = withAlpha(warning(context), TerminalClientOptions.highContrastMode() ? 0x46 : 0x2B);
                border = withAlpha(warning(context), 0xBB);
                text = warning(context);
            }
            case "DONE" -> {
                bg = withAlpha(success(context), 0x20);
                border = withAlpha(success(context), 0x70);
                text = TerminalClientOptions.highContrastMode() ? success(context) : muted(context);
            }
            case "MISSING", "OFFLINE", "UNAVAILABLE" -> {
                bg = withAlpha(danger(context), TerminalClientOptions.highContrastMode() ? 0x44 : 0x24);
                border = withAlpha(danger(context), 0xB8);
                text = danger(context);
            }
            case "LOCKED" -> {
                bg = cyberglass(context) ? withAlpha(warning(context), 0x18) : tokens.panels().disabledFill();
                border = cyberglass(context) ? withAlpha(warning(context), 0x76) : tokens.borders().disabled();
                text = cyberglass(context) ? warning(context) : muted(context);
            }
            case "OPTIONAL", "INFO", "IDLE" -> {
                bg = withAlpha(accent(context), 0x1C);
                border = tokens.borders().subtle();
                text = value.equals("IDLE") ? muted(context) : accent(context);
            }
            default -> {
                bg = tokens.colors().rowSelected();
                border = tokens.borders().selected();
                text = text(context);
            }
        }
        int safeHeight = Math.max(14, height);
        if (cyberglass(context)) {
            glassRect(graphics, x, y, width, safeHeight, cyberRadiusSmall(), bg, border, withAlpha(0xFFFFFFFF, 0x10));
            if (width > 24) {
                graphics.fill(x + 7, y + Math.max(4, safeHeight / 2 - 1),
                        x + 10, y + Math.max(7, safeHeight / 2 + 2), withAlpha(color, 0xE0));
            }
            graphics.centeredText(font(context), trim(context, value, Math.max(8, width - 16)), x + width / 2,
                    y + Math.max(2, (safeHeight - 8) / 2), text);
            return;
        }
        graphics.fill(x, y, x + width, y + safeHeight, bg);
        graphics.outline(x, y, width, safeHeight, border);
        graphics.fill(x, y, x + Math.min(3, Math.max(1, width)), y + safeHeight, withAlpha(color, 0xDD));
        graphics.centeredText(font(context), trim(context, value, Math.max(8, width - 14)), x + width / 2,
                y + Math.max(2, (safeHeight - 8) / 2), text);
    }

    public static void tabChip(GuiGraphicsExtractor graphics, Font font, int x, int y, int width, int height,
            String label, boolean selected, boolean hovered, int color) {
        int bg = selected ? ROW_SELECTED : hovered ? 0xFF102630 : ROW;
        int text = selected ? TEXT : MUTED;
        int accent = selected ? opaque(color) : CYAN_DIM;
        graphics.fill(x, y, x + width, y + height, bg);
        activeOutline(graphics, x, y, width, height, selected || hovered,
                selected ? opaque(color) : 0x4438DFF4);
        graphics.fill(x, y + height - 2, x + width, y + height, accent);
        graphics.centeredText(font, trim(font, label, width - 8), x + width / 2, y + Math.max(4, (height - 8) / 2), text);
    }

    public static void categoryChip(GuiGraphicsExtractor graphics, Font font, int x, int y, int width, int height,
            String label, boolean selected, boolean hovered, int color) {
        int bg = selected ? 0x8A07131B : hovered ? 0x66102630 : 0x33050D14;
        graphics.fill(x, y, x + width, y + height, bg);
        activeOutline(graphics, x, y, width, height, selected || hovered,
                selected ? 0x8866E8FF : 0x4438DFF4);
        graphics.fill(x, y, x + 3, y + height, selected ? opaque(color) : 0x552E8E9D);
        graphics.centeredText(font, trim(font, label, width - 12), x + width / 2,
                y + Math.max(4, (height - 8) / 2), selected ? TEXT : MUTED);
    }

    public static void categoryChip(TerminalRenderContext context, GuiGraphicsExtractor graphics, Font font,
            int x, int y, int width, int height, String label, boolean selected, boolean hovered, int color) {
        if (!cyberglass(context)) {
            categoryChip(graphics, font, x, y, width, height, label, selected, hovered, color);
            return;
        }
        int bg = selected ? withAlpha(color, 0x2F)
                : hovered ? tokens(context).panels().hoverFill()
                : withAlpha(tokens(context).colors().content(), 0x8A);
        cyberSurface(context, graphics, x, y, width, height, cyberRadiusMedium(), bg,
                selected ? withAlpha(color, 0xB8) : tokens(context).borders().subtle(), color, selected || hovered);
        graphics.centeredText(font, trim(font, label, width - 18), x + width / 2,
                y + Math.max(4, (height - 8) / 2), selected ? text(context) : muted(context));
    }

    public static void pageTab(GuiGraphicsExtractor graphics, Font font, int x, int y, int width, int height,
            String label, boolean selected, boolean hovered, int color) {
        int bg = selected ? 0xD00B2630 : hovered ? 0xAA102630 : 0x44050D14;
        graphics.fill(x, y, x + width, y + height, bg);
        activeOutline(graphics, x, y, width, height, selected || hovered,
                selected ? withAlpha(color, 0xCC) : 0x4438DFF4);
        if (selected) {
            graphics.fill(x, y, x + 3, y + height, opaque(color));
        }
        graphics.centeredText(font, trim(font, label, width - 10), x + width / 2,
                y + Math.max(4, (height - 8) / 2), selected ? TEXT : MUTED);
    }

    public static void pageTab(TerminalRenderContext context, GuiGraphicsExtractor graphics, Font font,
            int x, int y, int width, int height, String label, boolean selected, boolean hovered, int color) {
        if (!cyberglass(context)) {
            pageTab(graphics, font, x, y, width, height, label, selected, hovered, color);
            return;
        }
        int fill = selected ? withAlpha(color, 0x28)
                : hovered ? tokens(context).panels().hoverFill()
                : withAlpha(tokens(context).colors().content(), 0x78);
        int border = selected ? withAlpha(color, 0xBA)
                : hovered ? tokens(context).borders().normal()
                : withAlpha(tokens(context).borders().subtle(), 0x70);
        cyberSurface(context, graphics, x, y, width, height, cyberRadiusSmall(), fill, border, color,
                selected || hovered);
        if (selected && width > 26) {
            graphics.fill(x + 10, y + height - 3, x + width - 10, y + height - 2, withAlpha(color, 0xB8));
        }
        graphics.centeredText(font, trim(font, label, width - 16), x + width / 2,
                y + Math.max(4, (height - 8) / 2), selected ? text(context) : muted(context));
    }

    public static void sidebarGroupChip(GuiGraphicsExtractor graphics, Font font, int x, int y, int width, int height,
            String label, boolean selected, boolean hovered, int color) {
        int bg = selected ? 0xFF123241 : hovered ? 0xFF102630 : 0xAA0A151C;
        int text = selected ? TEXT : MUTED;
        graphics.fill(x, y, x + width, y + height, bg);
        activeOutline(graphics, x, y, width, height, selected || hovered,
                selected ? opaque(color) : 0x4438DFF4);
        graphics.fill(x, y, x + 3, y + height, selected ? opaque(color) : CYAN_DIM);
        graphics.text(font, trim(font, label, width - 18), x + 10, y + Math.max(4, (height - 8) / 2), text, false);
    }

    public static void sidebarTabChip(GuiGraphicsExtractor graphics, Font font, int x, int y, int width, int height,
            String label, String summary, boolean selected, boolean hovered, int color) {
        int bg = selected ? 0xFF123241 : hovered ? 0xFF102630 : ROW;
        graphics.fill(x, y, x + width, y + height, bg);
        activeOutline(graphics, x, y, width, height, selected || hovered,
                selected ? opaque(color) : 0x4438DFF4);
        graphics.fill(x, y + height - 2, x + width, y + height, selected ? opaque(color) : CYAN_DIM);
        graphics.text(font, trim(font, label, width - 12), x + 7, y + 5, selected ? TEXT : MUTED, false);
        if (height >= 28 && summary != null && !summary.isBlank()) {
            graphics.text(font, trim(font, summary, width - 12), x + 7, y + 17, MUTED, false);
        }
    }

    public static int pageHeader(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            String title, String detail, int x, int y, int width, int color) {
        densePanel(context, graphics, x, y, width, 30, color);
        line(context, graphics, title, x + 8, y + 6, Math.max(40, width / 2), color);
        if (detail != null && !detail.isBlank()) {
            String trimmed = trim(context, detail, Math.max(40, width / 2 - 10));
            graphics.text(font(context), trimmed, x + width - 8 - font(context).width(trimmed),
                    y + 6, muted(context), false);
        }
        return y + 38;
    }

    public static int shortcutCard(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int width, String title, String value, String detail, int color, boolean hovered) {
        int height = Math.max(42, 30 + wrappedHeight(context, detail, width - 16));
        graphics.fill(x, y, x + width, y + height, hovered ? 0xFF102630 : PANEL_DARK);
        activeOutline(graphics, x, y, width, height, hovered, opaque(color));
        graphics.fill(x, y, x + 3, y + height, opaque(color));
        line(context, graphics, title, x + 8, y + 6, width - 16, MUTED);
        line(context, graphics, value, x + 8, y + 18, width - 16, color);
        wrap(context, graphics, detail, x + 8, y + 31, width - 16, TEXT);
        return height;
    }

    public static int missionLaneHeader(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int width, String title, String count, int color) {
        TerminalThemeTokens tokens = tokens(context);
        graphics.fill(x, y, x + width, y + 17, tokens.colors().row());
        graphics.fill(x, y, x + 3, y + 17, opaque(color));
        line(context, graphics, title, x + 8, y + 4, width - 80, color);
        if (count != null && !count.isBlank()) {
            line(context, graphics, count, x + width - 70, y + 4, 64, tokens.colors().muted());
        }
        return y + 20;
    }

    public static int stickyActionBar(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int width, int height, String title, String detail, int color) {
        actionBarPanel(context, graphics, x, y, width, height, color);
        line(context, graphics, title, x + 8, y + 7, width - 16, color);
        wrap(context, graphics, detail, x + 8, y + 20, width - 16, TEXT);
        return y + height + 6;
    }

    public static void filterChip(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int width, String label, boolean selected, boolean enabled, int color, boolean hovered) {
        segmentedChip(context, graphics, x, y, width, 15, label, selected, enabled, color, hovered);
    }

    public static void segmentedChip(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int width, int height, String label, boolean selected, boolean enabled,
            int color, boolean hovered) {
        TerminalThemeTokens tokens = tokens(context);
        int chipH = Math.max(14, height);
        int bg = !enabled
                ? tokens.panels().disabledFill()
                : selected ? tokens.colors().rowSelected()
                : hovered ? tokens.panels().hoverFill()
                : tokens.colors().row();
        int border = !enabled
                ? tokens.borders().disabled()
                : selected ? tokens.borders().selected()
                : hovered ? tokens.borders().normal()
                : tokens.borders().subtle();
        int text = selected ? tokens.colors().text() : enabled ? tokens.colors().muted() : tokens.colors().accentDim();
        graphics.fill(x, y, x + width, y + chipH, bg);
        if (selected || hovered || !enabled) {
            graphics.outline(x, y, width, chipH, border);
        }
        if (selected) {
            graphics.fill(x, y, x + 3, y + chipH, opaque(color));
        }
        graphics.centeredText(font(context), trim(context, label, width - 8), x + width / 2,
                y + Math.max(3, (chipH - 8) / 2), opaque(text));
    }

    public static int statusCard(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int width, String label, String value, String detail, int color) {
        int detailHeight = wrappedHeight(context, detail, width - 14);
        int height = Math.max(54, 42 + detailHeight);
        panel(context, graphics, x, y, width, height);
        line(context, graphics, label, x + 7, y + 7, width - 14, muted(context));
        line(context, graphics, value, x + 7, y + 22, width - 14, color);
        wrap(context, graphics, detail, x + 7, y + 37, width - 14, text(context));
        return height;
    }

    public static int dataCard(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int width, String title, String value, String detail, int color) {
        return statusCard(context, graphics, x, y, width, title, value, detail, color);
    }

    public static int denseDataCard(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int width, String title, String value, String detail, int color) {
        int detailHeight = wrappedHeight(context, detail, width - 12);
        int height = Math.max(44, 32 + detailHeight);
        densePanel(context, graphics, x, y, width, height, color);
        line(context, graphics, title, x + 6, y + 6, width - 12, muted(context));
        line(context, graphics, value, x + 6, y + 18, width - 12, color);
        wrap(context, graphics, detail, x + 6, y + 31, width - 12, text(context));
        return height;
    }

    public static int commandStrip(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int width, String title, String detail, int color) {
        int detailHeight = wrappedHeight(context, detail, width - 14);
        int height = Math.max(34, 24 + detailHeight);
        panel(context, graphics, x, y, width, height);
        line(context, graphics, title, x + 7, y + 7, width - 14, color);
        wrap(context, graphics, detail, x + 7, y + 20, width - 14, text(context));
        return y + height + 5;
    }

    public static int compactCommandStrip(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int width, String title, String detail, int color) {
        int detailHeight = wrappedHeight(context, detail, width - 12);
        int height = Math.max(28, 18 + detailHeight);
        densePanel(context, graphics, x, y, width, height, color);
        line(context, graphics, title, x + 6, y + 5, width - 12, color);
        wrap(context, graphics, detail, x + 6, y + 17, width - 12, text(context));
        return y + height + 4;
    }

    public static int emptyState(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int width, String title, String detail, int color) {
        int height = Math.max(42, 28 + wrappedHeight(context, detail, width - 16));
        cardPlate(context, graphics, TerminalVisualAssets.CARD_EMPTY_STATE_PLATE, x, y, width, height, color,
                Math.min(0.84F, tokens(context).panels().imageDarken() + 0.08F), ImageFit.NINE_SLICE);
        line(context, graphics, title, x + 8, y + 8, width - 16, color);
        wrap(context, graphics, detail, x + 8, y + 22, width - 16, muted(context));
        return y + height + 5;
    }

    public static int callout(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int width, String title, String detail, int color) {
        int bodyHeight = wrappedHeight(context, detail, width - 20);
        int height = Math.max(38, 25 + bodyHeight);
        if (cyberglass(context)) {
            cyberSurface(context, graphics, x, y, width, height, cyberRadiusMedium(),
                    withAlpha(tokens(context).colors().content(), 0x94),
                    tokens(context).borders().subtle(), color, false);
            iconBadge(context, graphics, TerminalIcon.TARGET, x + 10, y + Math.max(8, (height - 22) / 2),
                    22, color, true);
            line(context, graphics, title, x + 42, y + 8, width - 54, color);
            wrap(context, graphics, detail, x + 42, y + 21, width - 54, text(context));
            return y + height + 6;
        }
        graphics.fill(x, y, x + width, y + height, tokens(context).colors().row());
        graphics.fill(x, y, x + 3, y + height, opaque(color));
        line(context, graphics, title, x + 9, y + 7, width - 18, color);
        wrap(context, graphics, detail, x + 9, y + 20, width - 18, text(context));
        return y + height + 5;
    }

    public static int metricRow(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int width, String label, String value, int color) {
        int valueWidth = Math.min(112, Math.max(54, width / 3));
        line(context, graphics, label, x, y, Math.max(24, width - valueWidth - 8), MUTED);
        line(context, graphics, value, x + width - valueWidth, y, valueWidth, color);
        return y + 13;
    }

    public static int denseMetricRow(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int width, String label, String value, int color) {
        int valueWidth = Math.min(104, Math.max(50, width / 3));
        line(context, graphics, label, x, y, Math.max(24, width - valueWidth - 8), MUTED);
        line(context, graphics, value, x + width - valueWidth, y, valueWidth, color);
        return y + 11;
    }

    public static int keyValue(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int width, String label, String value, int color) {
        int split = Math.min(150, Math.max(86, width / 3));
        line(context, graphics, label, x, y, split - 6, MUTED);
        line(context, graphics, value, x + split, y, Math.max(24, width - split), color);
        return y + 14;
    }

    public static int checklistRow(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int width, String label, boolean ok, String detail) {
        int color = ok ? GREEN : AMBER;
        int split = Math.min(160, Math.max(96, width / 3));
        line(context, graphics, (ok ? "[x] " : "[ ] ") + label, x, y, split - 6, color);
        return wrap(context, graphics, detail, x + split, y, Math.max(24, width - split), color) + 3;
    }

    public static int disabledActionRow(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int width, String reason, int color) {
        int bodyHeight = wrappedHeight(context, reason, width - 14);
        int height = Math.max(23, bodyHeight + 14);
        panel(graphics, x, y, width, height);
        wrap(context, graphics, reason, x + 7, y + 7, width - 14, color);
        return y + height + 4;
    }

    public static int disabledReasonRow(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int width, String reason, int color) {
        return disabledActionRow(context, graphics, x, y, width, reason, color);
    }

    public static int line(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            String text, int x, int y, int maxWidth, int color) {
        graphics.text(font(context), trim(context, text, Math.max(20, maxWidth)), x, y, opaque(color),
                tokens(context).typography().shadowText());
        return y;
    }

    public static int wrap(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            String text, int x, int y, int maxWidth, int color) {
        int cy = y;
        String value = text == null ? "" : text;
        for (String paragraph : value.split("\\R", -1)) {
            if (paragraph.isEmpty()) {
                cy += 11;
                continue;
            }
            for (var line : font(context).split(Component.literal(paragraph), Math.max(24, maxWidth))) {
                graphics.text(font(context), line, x, cy, opaque(color), tokens(context).typography().shadowText());
                cy += 11;
            }
        }
        return cy;
    }

    public static void button(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int w, String label, int color, boolean enabled, boolean hovered) {
        TerminalThemeTokens tokens = tokens(context);
        int buttonH = TerminalClientOptions.largeTextMode()
                ? TerminalDesignTokens.LARGE_BUTTON_HEIGHT
                : TerminalDesignTokens.BUTTON_HEIGHT;
        if (cyberglass(context)) {
            int bg = !enabled ? tokens.panels().disabledFill()
                    : hovered ? withAlpha(color, 0x24)
                    : withAlpha(tokens.colors().content(), 0x92);
            glassRect(graphics, x, y, w, buttonH, cyberRadiusSmall(), bg,
                    !enabled ? tokens.borders().disabled()
                            : hovered ? withAlpha(color, 0xAA) : tokens.borders().subtle(),
                    withAlpha(0xFFFFFFFF, 0x12));
            graphics.centeredText(font(context), trim(context, label, w - 10), x + w / 2, y + 5,
                    enabled ? tokens.colors().text() : tokens.colors().muted());
            return;
        }
        int bg = enabled ? (hovered ? tokens.panels().hoverFill() : tokens.colors().row())
                : tokens.panels().disabledFill();
        graphics.fill(x, y, x + w, y + buttonH, bg);
        if (enabled) {
            graphics.fill(x, y, x + w, y + buttonH, withAlpha(color, hovered ? 0x20 : 0x10));
        }
        if (hovered || !enabled) {
            graphics.outline(x, y, w, buttonH, enabled ? tokens.borders().normal() : tokens.borders().disabled());
        }
        graphics.fill(x, y, x + 3, y + buttonH, enabled ? withAlpha(color, 0xCC) : tokens.dividers().line());
        graphics.centeredText(font(context), trim(context, label, w - 8), x + w / 2, y + 5,
                enabled ? tokens.colors().text() : tokens.colors().muted());
    }

    public static void compactButton(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int w, String label, int color, boolean enabled, boolean hovered) {
        compactButton(context, graphics, x, y, w, 16, label, color, enabled, hovered);
    }

    public static void compactButton(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int w, int h, String label, int color, boolean enabled, boolean hovered) {
        TerminalThemeTokens tokens = tokens(context);
        int buttonH = Math.max(16, h);
        if (cyberglass(context)) {
            int bg = !enabled ? tokens.panels().disabledFill()
                    : hovered ? withAlpha(color, 0x24)
                    : withAlpha(tokens.colors().content(), 0x88);
            glassRect(graphics, x, y, w, buttonH, cyberRadiusSmall(), bg,
                    !enabled ? tokens.borders().disabled()
                            : hovered ? withAlpha(color, 0xA0) : tokens.borders().subtle(),
                    withAlpha(0xFFFFFFFF, 0x10));
            graphics.centeredText(font(context), trim(context, label, w - 8), x + w / 2,
                    y + Math.max(4, (buttonH - 8) / 2),
                    enabled ? tokens.colors().text() : tokens.colors().muted());
            return;
        }
        int bg = enabled ? (hovered ? tokens.panels().hoverFill() : tokens.colors().row())
                : tokens.panels().disabledFill();
        graphics.fill(x, y, x + w, y + buttonH, bg);
        if (enabled && hovered) {
            graphics.fill(x, y, x + w, y + buttonH, withAlpha(color, 0x18));
        }
        if (hovered || !enabled) {
            graphics.outline(x, y, w, buttonH, enabled ? tokens.borders().normal() : tokens.borders().disabled());
        }
        graphics.fill(x, y, x + 3, y + buttonH, enabled ? withAlpha(color, 0xC0) : tokens.dividers().line());
        graphics.centeredText(font(context), trim(context, label, w - 8), x + w / 2,
                y + Math.max(4, (buttonH - 8) / 2),
                enabled ? tokens.colors().text() : tokens.colors().muted());
    }

    public static int responsiveControlWidth(int rowWidth, boolean textEntry) {
        int preferred = textEntry ? 168 : 152;
        return Math.max(68, Math.min(preferred, Math.max(68, rowWidth - 16)));
    }

    public static boolean shouldStackControls(int rowWidth, boolean textEntry) {
        int controls = responsiveControlWidth(rowWidth, textEntry);
        int minimumCopy = textEntry ? 190 : 178;
        return rowWidth < minimumCopy + controls + 30;
    }

    public static int responsiveControlRowHeight(int rowWidth, boolean textEntry, boolean hasBadges) {
        int copyH = hasBadges ? 48 : 34;
        int controlsH = textEntry ? 44 : 20;
        if (shouldStackControls(rowWidth, textEntry)) {
            return copyH + controlsH + 12;
        }
        return Math.max(textEntry ? 62 : 48, copyH + 12);
    }

    public static void dataSurfaceRow(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int w, int h, int color, boolean hovered, boolean selected, boolean enabled) {
        TerminalThemeTokens tokens = tokens(context);
        int bg = !enabled
                ? tokens.panels().disabledFill()
                : selected ? tokens.colors().rowSelected()
                : hovered ? tokens.panels().hoverFill()
                : tokens.colors().row();
        graphics.fill(x, y, x + w, y + h, bg);
        if (!enabled || selected || hovered) {
            graphics.outline(x, y, w, h, !enabled ? tokens.borders().disabled() : tokens.borders().normal());
        }
        graphics.fill(x, y, x + 3, y + h, enabled ? opaque(color) : tokens.dividers().line());
        if (hovered && enabled) {
            graphics.fill(x + 6, y + 3, x + w - 6, y + 4, tokens.borders().glow());
        }
    }

    public static void inlineValueField(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int w, int h, String value, int color, boolean active, boolean enabled) {
        TerminalThemeTokens tokens = tokens(context);
        int fieldH = Math.max(18, h);
        int bg = active ? withAlpha(color, 0x32) : enabled ? tokens.panels().selectedFill() : tokens.panels().disabledFill();
        graphics.fill(x, y, x + w, y + fieldH, bg);
        if (active || !enabled) {
            graphics.outline(x, y, w, fieldH, active ? opaque(color) : tokens.borders().disabled());
        }
        graphics.text(font(context), trim(context, value, w - 8), x + 4,
                y + Math.max(5, (fieldH - 8) / 2),
                enabled ? tokens.colors().text() : tokens.colors().muted(), false);
    }

    public static void primaryCommandButton(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int w, int h, String label, int color, boolean hovered) {
        primaryCommandButton(context, graphics, x, y, w, h, label, null, color, hovered);
    }

    public static void primaryCommandButton(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int w, int h, String label, Identifier texture, int color, boolean hovered) {
        TerminalThemeTokens tokens = tokens(context);
        if (cyberglass(context)) {
            cyberSurface(context, graphics, x, y, w, h, cyberRadiusSmall(),
                    hovered ? withAlpha(color, 0x34) : withAlpha(color, 0x24),
                    hovered ? withAlpha(color, 0xC0) : withAlpha(color, 0x88), color, hovered);
            drawCommandLabel(context, graphics, x, y, w, h, label, texture, color, tokens.colors().text(), true);
            return;
        }
        int bg = hovered ? tokens.panels().hoverFill() : tokens.colors().row();
        graphics.fill(x, y, x + w, y + h, bg);
        graphics.fill(x, y, x + w, y + h, withAlpha(color, hovered ? 0x28 : 0x1C));
        if (hovered) {
            graphics.outline(x, y, w, h, withAlpha(color, 0xCC));
        }
        graphics.fill(x, y, x + 4, y + h, withAlpha(color, 0xD8));
        drawCommandLabel(context, graphics, x, y, w, h, label, texture, color, tokens.colors().text(), true);
    }

    public static void secondaryCommandButton(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int w, int h, String label, int color, boolean hovered) {
        secondaryCommandButton(context, graphics, x, y, w, h, label, null, color, hovered);
    }

    public static void secondaryCommandButton(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int w, int h, String label, Identifier texture, int color, boolean hovered) {
        TerminalThemeTokens tokens = tokens(context);
        if (cyberglass(context)) {
            glassRect(graphics, x, y, w, h, cyberRadiusSmall(),
                    hovered ? withAlpha(color, 0x22) : withAlpha(tokens.colors().content(), 0x8C),
                    hovered ? withAlpha(color, 0x94) : tokens.borders().subtle(),
                    withAlpha(0xFFFFFFFF, 0x12));
            drawCommandLabel(context, graphics, x, y, w, h, label, texture,
                    hovered ? color : tokens.colors().accentDim(),
                    hovered ? tokens.colors().text() : tokens.colors().muted(), false);
            return;
        }
        int bg = hovered ? withAlpha(tokens.panels().hoverFill(), 0xD0) : tokens.colors().row();
        graphics.fill(x, y, x + w, y + h, bg);
        activeOutline(graphics, x, y, w, h, hovered, tokens.borders().normal());
        graphics.fill(x + 4, y + 5, x + 6, y + h - 5,
                hovered ? withAlpha(color, 0xCC) : tokens.dividers().line());
        drawCommandLabel(context, graphics, x, y, w, h, label, texture,
                hovered ? color : tokens.colors().accentDim(),
                hovered ? tokens.colors().text() : tokens.colors().muted(), false);
    }

    public static void disabledCommandButton(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int w, int h, String label) {
        disabledCommandButton(context, graphics, x, y, w, h, label, null);
    }

    public static void disabledCommandButton(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int w, int h, String label, Identifier texture) {
        TerminalThemeTokens tokens = tokens(context);
        if (cyberglass(context)) {
            glassRect(graphics, x, y, w, h, cyberRadiusSmall(), tokens.panels().disabledFill(),
                    tokens.borders().disabled(), withAlpha(0xFFFFFFFF, 0x08));
            drawCommandLabel(context, graphics, x, y, w, h, label, texture,
                    tokens.colors().accentDim(), tokens.colors().muted(), false);
            return;
        }
        graphics.fill(x, y, x + w, y + h, tokens.panels().disabledFill());
        graphics.fill(x + 4, y + 4, x + 7, y + h - 4, tokens.dividers().line());
        drawCommandLabel(context, graphics, x, y, w, h, label, texture,
                tokens.colors().accentDim(), tokens.colors().muted(), false);
    }

    public static void iconBadge(GuiGraphicsExtractor graphics, TerminalIcon icon,
            int x, int y, int size, int color, boolean active) {
        graphics.fill(x, y, x + size, y + size, 0xAA071017);
        graphics.outline(x, y, size, size, active ? opaque(color) : 0x5538DFF4);
        graphics.fill(x, y + size - 2, x + size, y + size, active ? opaque(color) : CYAN_DIM);
        icon.draw(graphics, x + 8, y + 8, Math.max(18, size - 16), color, active);
    }

    public static void iconBadge(TerminalRenderContext context, GuiGraphicsExtractor graphics, TerminalIcon icon,
            int x, int y, int size, int color, boolean active) {
        TerminalThemeTokens tokens = tokens(context);
        if (cyberglass(context)) {
            glassRect(graphics, x, y, size, size, Math.min(cyberRadiusSmall(), Math.max(3, size / 4)),
                    active ? withAlpha(color, 0x24) : withAlpha(tokens.colors().content(), 0x86),
                    active ? withAlpha(color, 0x9C) : tokens.borders().subtle(),
                    withAlpha(0xFFFFFFFF, 0x10));
            icon.draw(graphics, x + Math.max(4, size / 5), y + Math.max(4, size / 5),
                    Math.max(10, size - Math.max(8, size * 2 / 5)), color, active);
            return;
        }
        graphics.fill(x, y, x + size, y + size, active ? tokens.colors().rowSelected() : tokens.colors().row());
        graphics.outline(x, y, size, size, active ? opaque(color) : tokens.borders().normal());
        graphics.fill(x, y + size - 2, x + size, y + size, active ? opaque(color) : tokens.colors().accentDim());
        icon.draw(graphics, x + 8, y + 8, Math.max(18, size - 16), color, active);
    }

    public static void iconTextureBadge(GuiGraphicsExtractor graphics, Identifier texture,
            int x, int y, int size, int color, boolean active) {
        boolean hasTexture = textureAvailable(texture) && size > 12;
        graphics.fill(x, y, x + size, y + size, active ? 0xCC071017 : 0x99071117);
        if (hasTexture) {
            graphics.blit(texture, x + 1, y + 1, x + size - 1, y + size - 1,
                    0.0F, 1.0F, 0.0F, 1.0F);
            graphics.outline(x, y, size, size, active ? opaque(color) : 0x5538DFF4);
        } else {
            graphics.outline(x, y, size, size, active ? opaque(color) : 0x5538DFF4);
            graphics.fill(x + 2, y + 2, x + size - 2, y + 4, active ? 0x5538DFF4 : 0x332E8E9D);
            graphics.fill(x + 2, y + size - 4, x + size - 2, y + size - 2,
                    active ? opaque(color) : 0x552E8E9D);
        }
    }

    public static void iconTextureBadge(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            Identifier texture, int x, int y, int size, int color, boolean active) {
        TerminalThemeTokens tokens = tokens(context);
        texture = themedVisual(context, texture);
        boolean hasTexture = textureAvailable(texture) && size > 12;
        if (cyberglass(context)) {
            glassRect(graphics, x, y, size, size, Math.min(cyberRadiusSmall(), Math.max(3, size / 4)),
                    active ? withAlpha(color, 0x24) : withAlpha(tokens.colors().content(), 0x86),
                    active ? withAlpha(color, 0x9C) : tokens.borders().subtle(),
                    withAlpha(0xFFFFFFFF, 0x10));
            if (hasTexture) {
                graphics.blit(texture, x + 2, y + 2, x + size - 2, y + size - 2,
                        0.0F, 1.0F, 0.0F, 1.0F);
            } else {
                graphics.fill(x + 4, y + 4, x + size - 4, y + 6, active ? withAlpha(color, 0x90) : tokens.dividers().line());
                graphics.fill(x + 5, y + size - 6, x + size - 5, y + size - 4,
                        active ? withAlpha(color, 0x70) : tokens.dividers().line());
            }
            return;
        }
        graphics.fill(x, y, x + size, y + size, active ? tokens.colors().rowSelected() : tokens.colors().row());
        if (hasTexture) {
            graphics.blit(texture, x + 1, y + 1, x + size - 1, y + size - 1,
                    0.0F, 1.0F, 0.0F, 1.0F);
            graphics.outline(x, y, size, size, active ? opaque(color) : tokens.borders().normal());
        } else {
            graphics.outline(x, y, size, size, active ? opaque(color) : tokens.borders().normal());
            graphics.fill(x + 2, y + 2, x + size - 2, y + 4,
                    active ? tokens.borders().normal() : tokens.dividers().line());
            graphics.fill(x + 2, y + size - 4, x + size - 2, y + size - 2,
                    active ? opaque(color) : tokens.dividers().line());
        }
    }

    public static void cinematicPanel(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int w, int h, int color) {
        TerminalThemeTokens tokens = tokens(context);
        if (cyberglass(context)) {
            cyberSurface(context, graphics, x, y, w, h, cyberRadiusLarge(), tokens.colors().content(),
                    tokens.borders().subtle(), color, false);
            return;
        }
        graphics.fill(x, y, x + w, y + h, tokens.colors().content());
        if (!TerminalClientOptions.reducedClutterMode()) {
            drawPanelTexture(context, graphics, chapterPanel(context), x, y, w, h,
                    Math.min(0.88F, tokens.panels().imageDarken() + 0.12F));
        }
        graphics.fill(x, y, x + 2, y + h, withAlpha(color, 0x50));
    }

    public static void hybridIconBadge(GuiGraphicsExtractor graphics, Identifier texture, TerminalIcon fallback,
            int x, int y, int size, int color, boolean active) {
        if (textureAvailable(texture)) {
            iconTextureBadge(graphics, texture, x, y, size, color, active);
        } else {
            iconBadge(graphics, fallback == null ? TerminalIcon.DEFAULT : fallback, x, y, size, color, active);
        }
    }

    public static void hybridIconBadge(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            Identifier texture, TerminalIcon fallback, int x, int y, int size, int color, boolean active) {
        texture = themedVisual(context, texture);
        if (textureAvailable(texture)) {
            iconTextureBadge(context, graphics, texture, x, y, size, color, active);
        } else {
            iconBadge(context, graphics, fallback == null ? TerminalIcon.DEFAULT : fallback, x, y, size, color, active);
        }
    }

    public static void hybridIcon(GuiGraphicsExtractor graphics, Identifier texture, TerminalIcon fallback,
            int x, int y, int size, int color, boolean active) {
        drawHybridIcon(graphics, texture, fallback, x, y, size, color, active);
    }

    public static int statusLineRow(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int width, TerminalIcon icon, String label, String value, int color) {
        return statusLineRow(context, graphics, x, y, width, icon, null, label, value, color);
    }

    public static int statusLineRow(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int width, TerminalIcon icon, Identifier texture, String label, String value, int color) {
        drawHybridIcon(graphics, themedVisual(context, texture), icon, x, y - 2, 14, color, false);
        line(context, graphics, label, x + 20, y, Math.max(24, width - 92), muted(context));
        line(context, graphics, value, x + Math.max(80, width - 84), y, Math.min(84, width / 3), color);
        return y + 18;
    }

    public static void progress(GuiGraphicsExtractor graphics, int x, int y, int w, int h, float progress, int color) {
        int fill = Math.max(0, Math.min(w - 2, Math.round((w - 2) * Math.max(0.0F, Math.min(1.0F, progress)))));
        graphics.fill(x, y, x + w, y + h, 0xFF263842);
        graphics.fill(x + 1, y + 1, x + 1 + fill, y + h - 1, opaque(color));
    }

    public static void progress(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int w, int h, float progress, int color) {
        int fill = Math.max(0, Math.min(w - 2, Math.round((w - 2) * Math.max(0.0F, Math.min(1.0F, progress)))));
        if (cyberglass(context)) {
            int trackH = Math.max(4, h);
            glassRect(graphics, x, y, w, trackH, Math.max(2, trackH / 2),
                    withAlpha(tokens(context).colors().rowSelected(), 0x78),
                    withAlpha(tokens(context).borders().subtle(), 0x60), 0x00000000);
            if (fill > 0) {
                glassRect(graphics, x + 1, y + 1, Math.max(2, fill), Math.max(2, trackH - 2),
                        Math.max(1, (trackH - 2) / 2), withAlpha(color, 0xD8),
                        0x00000000, withAlpha(0xFFFFFFFF, 0x18));
            }
            return;
        }
        graphics.fill(x, y, x + w, y + h, tokens(context).colors().rowSelected());
        graphics.fill(x + 1, y + 1, x + 1 + fill, y + h - 1, opaque(color));
    }

    public static void meter(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int w, String label, int value, int color) {
        int clamped = Math.max(0, Math.min(100, value));
        line(context, graphics, label + " " + clamped + "%", x, y, Math.max(20, w), text(context));
        progress(context, graphics, x + 118, y + 2, Math.max(50, w - 118), 8, clamped / 100.0F, color);
    }

    public static void compactMeter(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int w, String label, int value, int color) {
        int clamped = Math.max(0, Math.min(100, value));
        line(context, graphics, label + " " + clamped + "%", x, y, Math.max(20, w), text(context));
        progress(context, graphics, x, y + 12, Math.max(36, w), 6, clamped / 100.0F, color);
    }

    public static void itemSlot(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            ItemStack stack, int x, int y, int color, boolean hovered) {
        if (cyberglass(context)) {
            glassRect(graphics, x, y, 20, 20, cyberRadiusSmall(),
                    hovered ? withAlpha(color, 0x28) : withAlpha(tokens(context).colors().content(), 0x8C),
                    hovered ? withAlpha(color, 0xA8) : tokens(context).borders().subtle(),
                    withAlpha(0xFFFFFFFF, 0x12));
            if (stack != null && !stack.isEmpty()) {
                graphics.item(stack, x + 2, y + 2);
                graphics.itemDecorations(font(context), stack, x + 2, y + 2);
                if (hovered) {
                    graphics.setTooltipForNextFrame(font(context), stack, x + 10, y + 10);
                }
            } else {
                graphics.fill(x + 7, y + 9, x + 13, y + 11, accentDim(context));
            }
            return;
        }
        graphics.fill(x, y, x + 20, y + 20, tokens(context).colors().row());
        graphics.outline(x, y, 20, 20, tokens(context).borders().normal());
        if (stack != null && !stack.isEmpty()) {
            graphics.item(stack, x + 2, y + 2);
            graphics.itemDecorations(font(context), stack, x + 2, y + 2);
            if (hovered) {
                graphics.outline(x, y, 20, 20, opaque(color));
                graphics.setTooltipForNextFrame(font(context), stack, x + 10, y + 10);
            }
        } else {
            graphics.fill(x + 5, y + 9, x + 15, y + 11, accentDim(context));
        }
    }

    public static int itemRow(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            ItemStack stack, int x, int y, int width, String label, String detail,
            int color, int mouseX, int mouseY) {
        boolean hovered = inside(mouseX, mouseY, x, y, 20, 20);
        itemSlot(context, graphics, stack, x, y, color, hovered);
        line(context, graphics, label, x + 26, y + 1, width - 26, color);
        int detailHeight = wrappedHeight(context, detail, width - 26);
        wrap(context, graphics, detail, x + 26, y + 12, width - 26, MUTED);
        return y + Math.max(24, 13 + detailHeight) + 3;
    }

    public static int itemGrid(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            java.util.List<ItemStack> stacks, int x, int y, int width, int color, int mouseX, int mouseY) {
        if (stacks == null || stacks.isEmpty()) {
            line(context, graphics, "No item rewards recorded.", x, y, width, MUTED);
            return y + 14;
        }
        int slotStep = 24;
        int columns = Math.max(1, Math.min(8, Math.max(1, width) / slotStep));
        int cy = y;
        int index = 0;
        for (ItemStack stack : stacks) {
            int sx = x + (index % columns) * slotStep;
            int sy = cy + (index / columns) * slotStep;
            itemSlot(context, graphics, stack, sx, sy, color, inside(mouseX, mouseY, sx, sy, 20, 20));
            index++;
        }
        int rows = (int) Math.ceil(stacks.size() / (double) columns);
        return y + rows * slotStep + 2;
    }

    public static int itemGridHeight(int stackCount, int width) {
        if (stackCount <= 0) {
            return 14;
        }
        int columns = Math.max(1, Math.min(8, Math.max(1, width) / 24));
        return ((stackCount + columns - 1) / columns) * 24 + 2;
    }

    public static void appShellBackdrop(GuiGraphicsExtractor graphics,
            Identifier texture, int x, int y, int w, int h, int color, boolean visuals, boolean reducedMotion) {
        int screenW = graphics.guiWidth();
        int screenH = graphics.guiHeight();
        graphics.fill(0, 0, screenW, screenH, 0xFF02070C);
        if (visuals) {
            if (textureAvailable(texture)) {
                blitFitted(graphics, texture, 0, 0, screenW, screenH, ImageFit.COVER, TERMINAL_BACKDROP_ASPECT);
            }
            graphics.fill(0, 0, screenW, screenH, reducedMotion ? 0xDD02070C : 0xC902070C);
            graphics.fill(0, 0, screenW, Math.max(80, screenH / 5), 0x55100528);
            graphics.fill(0, screenH - Math.max(70, screenH / 6), screenW, screenH, 0x6602070C);
        }
        graphics.fill(x, y, x + w, y + h, 0x9002070C);
        graphics.outline(x, y, w, h, 0x8A38DFF4);
        graphics.fill(x + 1, y + 1, x + w - 1, y + 18, 0x2A163843);
        graphics.fill(x + 12, y + 10, x + Math.min(x + 278, x + w / 3), y + 11, opaque(color));
        graphics.fill(x + 12, y + 10, x + 14, y + 68, opaque(color));
        graphics.fill(x + w - Math.min(278, w / 3), y + 10, x + w - 12, y + 11, 0x5538DFF4);
        graphics.fill(x + w - 2, y + 46, x + w, y + h - 34, 0x442E8E9D);
        graphics.fill(x + 12, y + h - 18, x + Math.min(x + 250, x + w / 3), y + h - 17, 0x552E8E9D);
        graphics.fill(x + w - 18, y + 18, x + w, y + 34, 0x6602070C);
        graphics.fill(x + w - 34, y + 18, x + w - 18, y + 20, 0x6638DFF4);
    }

    public static void appShellBackdrop(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int w, int h, int color) {
        TerminalThemeTokens tokens = tokens(context);
        Identifier texture = themedVisual(context, tokens.assets().shellBackdrop());
        boolean visuals = visualAssets(context);
        boolean reducedMotion = context != null && context.themeContext().reducedMotion();
        int screenW = graphics.guiWidth();
        int screenH = graphics.guiHeight();
        graphics.fill(0, 0, screenW, screenH, tokens.colors().background());
        if (cyberglass(context)) {
            if (visuals && textureAvailable(texture)) {
                blitFitted(graphics, texture, 0, 0, screenW, screenH, ImageFit.COVER, TERMINAL_BACKDROP_ASPECT);
            }
            graphics.fill(0, 0, screenW, screenH,
                    TerminalClientOptions.cyberglassBackgroundEffects() ? 0xD4010711 : 0xEA01050B);
            if (TerminalClientOptions.cyberglassBackgroundEffects()
                    && !TerminalClientOptions.cyberglassReduceVisualNoise()) {
                drawTerminalGrid(graphics, screenW, screenH, reducedMotion ? 42 : 34, 0x1025B7D1);
                graphics.fill(0, 0, screenW, Math.max(58, screenH / 8), 0x2200D9FF);
                graphics.fill(0, screenH - Math.max(66, screenH / 7), screenW, screenH, 0x4D010711);
            }
            cyberGlow(graphics, x, y, w, h, color, true);
            glassRect(graphics, x, y, w, h, cyberRadiusShell(), tokens.colors().shell(),
                    tokens.borders().normal(), withAlpha(0xFFFFFFFF, 0x12));
            graphics.fill(x + 20, y + 2, x + Math.max(x + 38, x + w / 3), y + 3, withAlpha(color, 0xA8));
            graphics.fill(x + Math.max(20, w * 42 / 100), y + 1,
                    x + Math.min(w - 30, w * 64 / 100), y + 2, withAlpha(tokens.colors().info(), 0x70));
            graphics.fill(x + w - Math.max(38, w / 5), y + 2, x + w - 18, y + 3,
                    withAlpha(tokens.colors().accent(), 0x34));
            return;
        }
        if (visuals) {
            if (textureAvailable(texture)) {
                blitFitted(graphics, texture, 0, 0, screenW, screenH, ImageFit.COVER, TERMINAL_BACKDROP_ASPECT);
            }
            graphics.fill(0, 0, screenW, screenH,
                    reducedMotion ? (tokens.effects().overlayColor() | 0x0E000000) : tokens.effects().overlayColor());
            if (!TerminalClientOptions.reducedClutterMode()) {
                graphics.fill(0, 0, screenW, Math.max(80, screenH / 5), tokens.panels().headerFill());
                graphics.fill(0, screenH - Math.max(70, screenH / 6), screenW, screenH, tokens.colors().shell());
            }
        }
        if (tokens.effects().grid() && !TerminalClientOptions.reduceGridNoise()) {
            drawTerminalGrid(graphics, screenW, screenH, reducedMotion ? 34 : 28, tokens.dividers().gridLine());
        }
        if (tokens.effects().scanlines() && !TerminalClientOptions.reduceGridNoise()) {
            drawTerminalScanlines(graphics, screenW, screenH, 0x09000000);
        }
        graphics.fill(x, y, x + w, y + h, tokens.colors().shell());
        graphics.outline(x, y, w, h, tokens.borders().normal());
    }

    private static void drawTerminalGrid(GuiGraphicsExtractor graphics, int width, int height, int step) {
        drawTerminalGrid(graphics, width, height, step, 0x1D2E8E9D);
    }

    private static void drawTerminalGrid(GuiGraphicsExtractor graphics, int width, int height, int step, int grid) {
        for (int gx = 0; gx < width; gx += Math.max(12, step)) {
            graphics.fill(gx, 0, gx + 1, height, grid);
        }
        for (int gy = 0; gy < height; gy += Math.max(12, step)) {
            graphics.fill(0, gy, width, gy + 1, grid);
        }
    }

    private static void drawTerminalScanlines(GuiGraphicsExtractor graphics, int width, int height, int color) {
        for (int gy = 2; gy < height; gy += 4) {
            graphics.fill(0, gy, width, gy + 1, color);
        }
    }

    public static void topMetaBar(GuiGraphicsExtractor graphics, Font font,
            int x, int y, int w, String title, String subtitle, String meta, int color) {
        topMetaBar(graphics, font, x, y, w, 52, title, subtitle, meta, color);
    }

    public static void topMetaBar(GuiGraphicsExtractor graphics, Font font,
            int x, int y, int w, int h, String title, String subtitle, String meta, int color) {
        int barH = Math.max(42, h);
        graphics.fill(x, y, x + w, y + barH, 0xDE020A10);
        graphics.fill(x + 12, y + barH - 8, x + w - 12, y + barH - 7, 0x3E38DFF4);
        graphics.fill(x + 1, y + 1, x + w - 1, y + Math.min(14, barH - 12), 0x1C163843);
        int iconSize = Math.min(34, Math.max(28, barH - 14));
        int iconY = y + Math.max(5, (barH - iconSize) / 2);
        hybridIconBadge(graphics, TerminalVisualAssets.ICON_BRAND_ECHO, TerminalIcon.CORE, x + 18, iconY, iconSize, color, true);
        String online = meta == null || meta.isBlank() ? "LINK: STANDBY  |  USER: OPERATOR  |  ONLINE" : meta;
        boolean offline = online.toUpperCase().contains("OFFLINE");
        String rightSource = w < 640 ? (offline ? "OFFLINE" : "ONLINE") : online;
        int rightMax = Math.max(64, Math.min(w - 100, w < 640 ? 86 : w * 34 / 100));
        String right = trim(font, rightSource, rightMax);
        int rightColor = right.toUpperCase().contains("OFFLINE") ? RED : MUTED;
        int rightX = x + w - 26 - font.width(right);
        int textX = x + 18 + iconSize + 12;
        int leftMax = Math.max(72, rightX - textX - 12);
        int titleY = y + Math.max(7, (barH - 28) / 2);
        int subtitleY = Math.min(y + barH - 15, titleY + 15);
        int rightY = y + Math.max(16, (barH - 8) / 2);
        graphics.text(font, trim(font, title, leftMax), textX, titleY, opaque(color), false);
        graphics.text(font, trim(font, subtitle, leftMax), textX, subtitleY, MUTED, false);
        graphics.text(font, right, rightX, rightY, rightColor, false);
        int dotColor = right.toUpperCase().contains("OFFLINE") ? RED : GREEN;
        int dotY = rightY + 1;
        graphics.fill(x + w - 18, dotY, x + w - 12, dotY + 6, opaque(dotColor));
    }

    public static void topMetaBar(TerminalRenderContext context, GuiGraphicsExtractor graphics, Font font,
            int x, int y, int w, String title, String subtitle, String meta, int color) {
        topMetaBar(context, graphics, font, x, y, w, 52, title, subtitle, meta, color);
    }

    public static void topMetaBar(TerminalRenderContext context, GuiGraphicsExtractor graphics, Font font,
            int x, int y, int w, int h, String title, String subtitle, String meta, int color) {
        TerminalThemeTokens tokens = tokens(context);
        int barH = Math.max(42, h);
        if (cyberglass(context)) {
            int pad = 12;
            int iconSize = Math.min(38, Math.max(30, barH - 14));
            int cardH = Math.max(34, barH - 12);
            int cardY = y + Math.max(6, (barH - cardH) / 2);
            int cardW = Math.max(170, Math.min(w / 2, 360));
            cyberSurface(context, graphics, x + pad, cardY, cardW, cardH, cyberRadiusMedium(),
                    withAlpha(tokens.colors().content(), 0x8A), tokens.borders().subtle(), color, false);
            hybridIconBadge(graphics,
                    themedIcon(context, TerminalIconKey.theme("brand"), TerminalVisualAssets.ICON_BRAND_ECHO),
                    TerminalIcon.CORE, x + pad + 10, cardY + Math.max(3, (cardH - iconSize) / 2),
                    iconSize, color, true);
            int textX = x + pad + 20 + iconSize;
            graphics.text(font, trim(font, title, Math.max(70, cardW - (textX - x) - 22)),
                    textX, cardY + Math.max(5, (cardH - 22) / 2), text(context), false);
            graphics.text(font, trim(font, subtitle, Math.max(70, cardW - (textX - x) - 22)),
                    textX, cardY + Math.max(18, (cardH - 22) / 2 + 14), accent(context), false);
            String online = meta == null || meta.isBlank() ? "LINK ONLINE" : meta;
            boolean offline = online.toUpperCase(Locale.ROOT).contains("OFFLINE");
            String right = trim(font, online, Math.max(56, Math.min(w * 36 / 100, w - cardW - 48)));
            int statusW = Math.max(74, font.width(right) + 32);
            int statusX = x + w - pad - statusW;
            int statusColor = offline ? tokens.colors().danger() : tokens.colors().success();
            cyberSurface(context, graphics, statusX, cardY + 3, statusW, Math.max(22, cardH - 6),
                    cyberRadiusSmall(), withAlpha(tokens.colors().content(), 0x72),
                    tokens.borders().subtle(), statusColor, false);
            graphics.text(font, right, statusX + 12, cardY + Math.max(10, (cardH - 8) / 2),
                    offline ? tokens.colors().danger() : tokens.colors().accent(), false);
            graphics.fill(statusX + statusW - 13, cardY + Math.max(11, (cardH - 5) / 2),
                    statusX + statusW - 8, cardY + Math.max(16, (cardH - 5) / 2 + 5), statusColor);
            return;
        }
        graphics.fill(x, y, x + w, y + barH, tokens.colors().shell());
        graphics.fill(x + 12, y + barH - 5, x + w - 12, y + barH - 4,
                withAlpha(color, 0x66));
        int iconSize = Math.min(34, Math.max(28, barH - 14));
        int iconY = y + Math.max(5, (barH - iconSize) / 2);
        hybridIconBadge(graphics,
                themedIcon(context, TerminalIconKey.theme("brand"), TerminalVisualAssets.ICON_BRAND_ECHO),
                TerminalIcon.CORE, x + 18, iconY, iconSize, color, true);
        String online = meta == null || meta.isBlank() ? "LINK: STANDBY  |  USER: OPERATOR  |  ONLINE" : meta;
        boolean offline = online.toUpperCase().contains("OFFLINE");
        String rightSource = w < 430 ? (offline ? "OFF" : "ON") : w < 640 ? (offline ? "OFFLINE" : "ONLINE") : online;
        int rightMax = Math.max(44, Math.min(w - 96, w < 430 ? 48 : w < 640 ? 86 : w * 34 / 100));
        String right = trim(font, rightSource, rightMax);
        int rightColor = right.toUpperCase().contains("OFFLINE") ? tokens.colors().danger() : tokens.colors().muted();
        int rightX = x + w - 26 - font.width(right);
        int textX = x + 18 + iconSize + 12;
        int leftMax = Math.max(72, rightX - textX - 12);
        int titleY = y + Math.max(7, (barH - 28) / 2);
        int subtitleY = Math.min(y + barH - 15, titleY + 15);
        int rightY = y + Math.max(16, (barH - 8) / 2);
        graphics.text(font, trim(font, title, leftMax), textX, titleY, text(context), false);
        if (leftMax >= 92 && barH >= 44) {
            graphics.text(font, trim(font, subtitle, leftMax), textX, subtitleY, accent(context), false);
        }
        graphics.text(font, right, rightX, rightY, rightColor, false);
        int dotColor = right.toUpperCase().contains("OFFLINE") ? tokens.colors().danger() : tokens.colors().success();
        int dotY = rightY + 1;
        graphics.fill(x + w - 18, dotY, x + w - 12, dotY + 6, opaque(dotColor));
    }

    public static void bottomShortcutBar(GuiGraphicsExtractor graphics, Font font,
            int x, int y, int w, String left, String right, int color) {
        bottomShortcutBar(graphics, font, x, y, w, 30, left, right, color);
    }

    public static void bottomShortcutBar(GuiGraphicsExtractor graphics, Font font,
            int x, int y, int w, int h, String left, String right, int color) {
        int barH = Math.max(24, h);
        int keyH = 16;
        int keyY = y + Math.max(4, (barH - keyH) / 2);
        int textY = keyY + 5;
        graphics.fill(x, y, x + w, y + barH, 0xD8020A10);
        graphics.fill(x + 12, y, x + w - 12, y + 1, 0x2838DFF4);
        String r = trimBreadcrumb(font, right == null ? "" : right, Math.max(110, Math.min(420, w * 38 / 100)));
        int rightX = x + w - 16 - font.width(r);
        int leftLimit = Math.max(x + 80, rightX - 18);
        int cx = x + 14;
        String[] tokens = left == null ? new String[0] : left.split("\\s{2,}");
        for (String token : tokens) {
            if (token.isBlank()) {
                continue;
            }
            int space = token.indexOf(' ');
            String key = space <= 0 ? token : token.substring(0, space);
            String label = space <= 0 ? "" : token.substring(space + 1);
            int keyW = Math.max(22, font.width(key) + 8);
            if (cx + keyW + font.width(label) + 18 > leftLimit) {
                break;
            }
            graphics.fill(cx, keyY, cx + keyW, keyY + keyH, 0x88071117);
            graphics.fill(cx, keyY + keyH - 2, cx + keyW, keyY + keyH, 0x5538DFF4);
            graphics.centeredText(font, trim(font, key, keyW - 4), cx + keyW / 2, textY, opaque(color));
            cx += keyW + 7;
            if (!label.isBlank()) {
                String shortLabel = trim(font, label, 82);
                graphics.text(font, shortLabel, cx, textY, MUTED, false);
                cx += font.width(shortLabel) + 18;
            }
        }
        graphics.text(font, r, rightX, textY, opaque(color), false);
    }

    public static void bottomShortcutBar(TerminalRenderContext context, GuiGraphicsExtractor graphics, Font font,
            int x, int y, int w, String left, String right, int color) {
        bottomShortcutBar(context, graphics, font, x, y, w, 30, left, right, color);
    }

    public static void bottomShortcutBar(TerminalRenderContext context, GuiGraphicsExtractor graphics, Font font,
            int x, int y, int w, int h, String left, String right, int color) {
        TerminalThemeTokens tokens = tokens(context);
        int barH = Math.max(24, h);
        int keyH = 16;
        int keyY = y + Math.max(4, (barH - keyH) / 2);
        int textY = keyY + 5;
        if (cyberglass(context)) {
            int dockX = x + 12;
            int dockW = Math.max(80, w - 24);
            int dockY = y + Math.max(3, (barH - Math.max(22, barH - 6)) / 2);
            int dockH = Math.max(22, barH - 6);
            cyberSurface(context, graphics, dockX, dockY, dockW, dockH, cyberRadiusMedium(),
                    withAlpha(tokens.colors().content(), 0x8F), tokens.borders().subtle(), color, false);
            String r = trimBreadcrumb(font, right == null ? "" : right,
                    Math.max(64, Math.min(w - 28, w < 520 ? w / 2 : w * 32 / 100)));
            int rightX = Math.max(dockX + 10, dockX + dockW - 12 - font.width(r));
            int leftLimit = rightX - 18;
            int cx = dockX + 12;
            String[] parts = left == null ? new String[0] : left.split("\\s{2,}");
            if (leftLimit >= dockX + 80) {
                for (String token : parts) {
                    if (token.isBlank()) {
                        continue;
                    }
                    int space = token.indexOf(' ');
                    String key = space <= 0 ? token : token.substring(0, space);
                    String label = space <= 0 ? "" : token.substring(space + 1);
                    int keyW = Math.max(26, font.width(key) + 12);
                    if (cx + keyW + font.width(label) + 18 > leftLimit) {
                        break;
                    }
                    glassRect(graphics, cx, keyY, keyW, keyH, cyberRadiusSmall(),
                            withAlpha(color, 0x18), withAlpha(color, 0x62), withAlpha(0xFFFFFFFF, 0x10));
                    graphics.centeredText(font, trim(font, key, keyW - 6), cx + keyW / 2, textY, opaque(color));
                    cx += keyW + 7;
                    if (!label.isBlank()) {
                        String shortLabel = trim(font, label, Math.max(48, Math.min(82, leftLimit - cx - 8)));
                        graphics.text(font, shortLabel, cx, textY, tokens.colors().muted(), false);
                        cx += font.width(shortLabel) + 18;
                    }
                }
            }
            graphics.text(font, r, rightX, textY, opaque(color), false);
            return;
        }
        graphics.fill(x, y, x + w, y + barH, tokens.colors().shell());
        graphics.fill(x + 12, y, x + w - 12, y + 1, tokens.dividers().line());
        int rightMax = Math.max(64, Math.min(w - 28, w < 520 ? w / 2 : w * 38 / 100));
        String r = trimBreadcrumb(font, right == null ? "" : right, rightMax);
        int rightX = Math.max(x + 14, x + w - 16 - font.width(r));
        int leftLimit = rightX - 18;
        int cx = x + 14;
        String[] parts = left == null ? new String[0] : left.split("\\s{2,}");
        if (leftLimit >= x + 80) {
            for (String token : parts) {
                if (token.isBlank()) {
                    continue;
                }
                int space = token.indexOf(' ');
                String key = space <= 0 ? token : token.substring(0, space);
                String label = space <= 0 ? "" : token.substring(space + 1);
                int keyW = Math.max(22, font.width(key) + 8);
                if (cx + keyW + font.width(label) + 18 > leftLimit) {
                    break;
                }
                graphics.fill(cx, keyY, cx + keyW, keyY + keyH, tokens.colors().row());
                graphics.fill(cx, keyY + keyH - 2, cx + keyW, keyY + keyH, withAlpha(color, 0x88));
                graphics.centeredText(font, trim(font, key, keyW - 4), cx + keyW / 2, textY, opaque(color));
                cx += keyW + 7;
                if (!label.isBlank()) {
                    String shortLabel = trim(font, label, Math.max(48, Math.min(82, leftLimit - cx - 8)));
                    graphics.text(font, shortLabel, cx, textY, tokens.colors().muted(), false);
                    cx += font.width(shortLabel) + 18;
                }
            }
        }
        graphics.text(font, r, rightX, textY, opaque(color), false);
    }

    public static void iconRailButton(GuiGraphicsExtractor graphics, Font font,
            int x, int y, int w, int h, TerminalIcon icon, String label, boolean selected, boolean hovered, int color) {
        iconRailButton(graphics, font, x, y, w, h, icon, null, label, selected, hovered, color);
    }

    public static void iconRailButton(GuiGraphicsExtractor graphics, Font font,
            int x, int y, int w, int h, TerminalIcon icon, Identifier texture, String label,
            boolean selected, boolean hovered, int color) {
        int bg = selected ? 0xD80B3440 : hovered ? 0x880D2530 : 0x52071117;
        graphics.fill(x, y, x + w, y + h, bg);
        activeOutline(graphics, x, y, w, h, selected || hovered,
                selected ? opaque(color) : 0x4438DFF4);
        if (selected) {
            graphics.fill(x, y, x + 3, y + h, opaque(color));
        }
        drawHybridIcon(graphics, texture, icon, x + 9, y + 8, 24, color, selected);
        graphics.text(font, trim(font, label, w - 46), x + 42, y + Math.max(8, (h - 8) / 2),
                selected ? TEXT : MUTED, false);
    }

    public static void iconRailButton(TerminalRenderContext context, GuiGraphicsExtractor graphics, Font font,
            int x, int y, int w, int h, TerminalIcon icon, Identifier texture, String label,
            boolean selected, boolean hovered, int color) {
        TerminalThemeTokens tokens = tokens(context);
        int bg = selected ? tokens.panels().selectedFill() : hovered ? tokens.panels().hoverFill() : tokens.colors().row();
        graphics.fill(x, y, x + w, y + h, bg);
        activeOutline(graphics, x, y, w, h, selected || hovered,
                selected ? opaque(color) : tokens.borders().normal());
        if (selected) {
            graphics.fill(x, y, x + 3, y + h, opaque(color));
        }
        drawHybridIcon(graphics, themedVisual(context, texture), icon, x + 9, y + 8, 24,
                selected ? color : tokens.colors().accentDim(), selected);
        graphics.text(font, trim(font, label, w - 46), x + 42, y + Math.max(8, (h - 8) / 2),
                selected ? tokens.colors().text() : tokens.colors().muted(), false);
    }

    public static void pageRailButton(GuiGraphicsExtractor graphics, Font font,
            int x, int y, int w, int h, TerminalIcon icon, String label, String summary,
            boolean selected, boolean hovered, int color) {
        int bg = selected ? 0xEE0B3440 : hovered ? 0xAA0D2530 : 0x77071117;
        graphics.fill(x, y, x + w, y + h, bg);
        activeOutline(graphics, x, y, w, h, selected || hovered,
                selected ? opaque(color) : 0x4438DFF4);
        if (selected) {
            graphics.fill(x, y + h - 2, x + w, y + h, opaque(color));
        }
        icon.draw(graphics, x + 8, y + 7, 20, color, selected);
        graphics.text(font, trim(font, label, w - 42), x + 34, y + 7, selected ? TEXT : MUTED, false);
        if (summary != null && !summary.isBlank() && h >= 38) {
            graphics.text(font, trim(font, summary, w - 42), x + 34, y + 20, MUTED, false);
        }
    }

    public static void commandStackPanel(GuiGraphicsExtractor graphics, Font font,
            int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + h, 0xCF050B10);
        graphics.fill(x, y, x + w, y + 34, 0x5E071923);
        graphics.fill(x, y, x + 3, y + h, opaque(color));
        graphics.fill(x, y, x + Math.max(48, w * 2 / 3), y + 2, opaque(color));
        graphics.fill(x + w - 2, y + 34, x + w, y + h - 16, 0x552E8E9D);
        graphics.fill(x + 8, y + 30, x + w - 8, y + 31, 0x332E8E9D);
        String title = w < 190 ? "ECHO" : "ECHO NAV";
        String subtitle = w < 190 ? "VIEWS" : "SELECT VIEW";
        graphics.text(font, trim(font, title, w - 28), x + 12, y + 8, CYAN, false);
        graphics.text(font, trim(font, subtitle, w - 28), x + 12, y + 21, MUTED, false);
    }

    public static void commandStackPanel(TerminalRenderContext context, GuiGraphicsExtractor graphics, Font font,
            int x, int y, int w, int h, int color) {
        TerminalThemeTokens tokens = tokens(context);
        if (cyberglass(context)) {
            cyberSurface(context, graphics, x, y, w, h, cyberRadiusLarge(), tokens.colors().content(),
                    tokens.borders().subtle(), color, false);
            String title = w < 190 ? "NAV" : "+ NAVIGATION";
            graphics.text(font, trim(font, title, w - 34), x + 14, y + 16, accent(context), false);
            graphics.fill(x + 14, y + 31, x + Math.max(x + 34, x + Math.min(w - 18, 116)), y + 32,
                    withAlpha(color, 0x70));
            return;
        }
        graphics.fill(x, y, x + w, y + h, tokens.colors().content());
        graphics.fill(x, y, x + 2, y + h, withAlpha(color, 0x55));
        String title = w < 190 ? "NAV" : "SECTIONS";
        graphics.text(font, trim(font, title, w - 24), x + 12, y + 10, accent(context), false);
    }

    public static void commandStackGroupLabel(GuiGraphicsExtractor graphics, Font font,
            int x, int y, int w, String label, boolean selected, int color) {
        int accent = selected ? opaque(color) : CYAN_DIM;
        graphics.text(font, trim(font, label.toUpperCase(), w - 18), x + 8, y + 3,
                selected ? accent : MUTED, false);
        graphics.fill(x + 8, y + 13, x + Math.max(x + 38, x + Math.min(w - 10, 116)), y + 14,
                selected ? accent : 0x442E8E9D);
    }

    public static void commandStackGroupButton(GuiGraphicsExtractor graphics, Font font,
            int x, int y, int w, int h, TerminalIcon icon, String label, boolean selected, boolean hovered, int color) {
        commandStackGroupButton(graphics, font, x, y, w, h, icon, null, label, selected, hovered, color);
    }

    public static void commandStackGroupButton(GuiGraphicsExtractor graphics, Font font,
            int x, int y, int w, int h, TerminalIcon icon, Identifier texture, String label,
            boolean selected, boolean hovered, int color) {
        int bg = selected ? 0xC40C3340 : hovered ? 0x76102630 : 0x46071117;
        int border = selected ? 0xCC66E8FF : hovered ? 0x4A38DFF4 : 0x1E38DFF4;
        graphics.fill(x, y, x + w, y + h, bg);
        if (selected || hovered) {
            graphics.outline(x, y, w, h, border);
        }
        graphics.fill(x, y, x + 3, y + h, selected ? opaque(color) : CYAN_DIM);
        if (selected) {
            graphics.fill(x, y + h - 2, x + w, y + h, opaque(color));
            graphics.fill(x + Math.max(24, w / 3), y + h - 4, x + w - 8, y + h - 2, 0x5CE9FBFF);
            graphics.fill(x, y + 1, x + w, y + Math.min(h - 1, Math.max(3, h / 2)), 0x1066E8FF);
        }
        int iconSize = Math.min(22, Math.max(16, h - 8));
        drawHybridIcon(graphics, texture, icon, x + 7, y + Math.max(3, (h - iconSize) / 2), iconSize,
                selected ? color : CYAN_DIM, selected);
        graphics.text(font, trim(font, label.toUpperCase(), w - 42), x + 34,
                y + Math.max(5, (h - 8) / 2), selected ? TEXT : MUTED, false);
    }

    public static void commandStackGroupButton(TerminalRenderContext context, GuiGraphicsExtractor graphics, Font font,
            int x, int y, int w, int h, TerminalIcon icon, Identifier texture, String label,
            boolean selected, boolean hovered, int color) {
        TerminalThemeTokens tokens = tokens(context);
        if (cyberglass(context)) {
            int bg = selected ? withAlpha(color, 0x27)
                    : hovered ? tokens.panels().hoverFill()
                    : withAlpha(tokens.colors().content(), 0x38);
            int border = selected ? withAlpha(color, 0xB8)
                    : hovered ? tokens.borders().normal()
                    : 0x00000000;
            cyberSurface(context, graphics, x, y, w, h, cyberRadiusMedium(), bg, border, color, selected || hovered);
            if (selected) {
                graphics.fill(x + 1, y + cyberRadiusSmall(), x + 4, y + h - cyberRadiusSmall(),
                        withAlpha(color, 0xD8));
            }
            int iconSize = Math.min(24, Math.max(18, h - 10));
            int iconX = x + 9;
            drawHybridIcon(graphics, themedVisual(context, texture), icon,
                    iconX, y + Math.max(4, (h - iconSize) / 2), iconSize,
                    selected ? color : accentDim(context), selected);
            graphics.text(font, trim(font, label, w - 46), x + 38,
                    y + Math.max(6, (h - 8) / 2), selected ? tokens.colors().text() : tokens.colors().muted(), false);
            if (selected && w > 50) {
                graphics.fill(x + w - 18, y + h / 2 - 2, x + w - 13, y + h / 2 + 3, withAlpha(color, 0xC8));
            }
            return;
        }
        int bg = selected ? tokens.panels().selectedFill()
                : hovered ? tokens.panels().hoverFill() : tokens.colors().row();
        graphics.fill(x, y, x + w, y + h, bg);
        if (selected) {
            graphics.fill(x, y, x + w, y + h, withAlpha(color, 0x18));
        }
        interactionOutline(graphics, x, y, w, h, selected, hovered,
                tokens.borders().selected(), tokens.borders().normal());
        graphics.fill(x, y, x + 4, y + h, selected ? opaque(color) : tokens.dividers().line());
        int iconSize = Math.min(22, Math.max(16, h - 8));
        drawHybridIcon(graphics, themedVisual(context, texture), icon,
                x + 7, y + Math.max(3, (h - iconSize) / 2), iconSize,
                selected ? color : muted(context), selected);
        graphics.text(font, trim(font, label, w - 42), x + 34,
                y + Math.max(5, (h - 8) / 2), selected ? tokens.colors().text() : tokens.colors().muted(), false);
    }

    public static void commandPageButton(GuiGraphicsExtractor graphics, Font font,
            int x, int y, int w, int h, TerminalIcon icon, String label, String summary,
            boolean selected, boolean hovered, int color) {
        commandPageButton(graphics, font, x, y, w, h, icon, null, label, summary, selected, hovered, color);
    }

    public static void commandPageButton(GuiGraphicsExtractor graphics, Font font,
            int x, int y, int w, int h, TerminalIcon icon, Identifier texture, String label, String summary,
            boolean selected, boolean hovered, int color) {
        int bg = selected ? 0xBE0B3440 : hovered ? 0x760D2530 : 0x3E071117;
        int border = selected ? 0xCC66E8FF : hovered ? 0x4A38DFF4 : 0x1E38DFF4;
        graphics.fill(x, y, x + w, y + h, bg);
        if (selected || hovered) {
            graphics.outline(x, y, w, h, border);
        }
        if (selected) {
            graphics.fill(x, y, x + 3, y + h, opaque(color));
            graphics.fill(x, y + h - 2, x + w, y + h, opaque(color));
            graphics.fill(x + Math.max(26, w / 3), y + h - 4, x + w - 10, y + h - 2, 0x5CE9FBFF);
            graphics.fill(x, y + 1, x + w, y + Math.min(h - 1, Math.max(3, h / 2)), 0x1066E8FF);
        } else if (hovered) {
            graphics.fill(x, y + h - 1, x + w, y + h, 0x5538DFF4);
        }
        drawHybridIcon(graphics, texture, icon, x + 7, y + Math.max(4, (h - 20) / 2),
                Math.min(24, Math.max(16, h - 10)), selected ? color : CYAN_DIM, selected);
        graphics.text(font, trim(font, label, w - 42), x + 34, y + Math.max(5, h >= 30 ? 6 : (h - 8) / 2),
                selected ? TEXT : MUTED, false);
        if (h >= 32 && summary != null && !summary.isBlank()) {
            graphics.text(font, trim(font, summary, w - 42), x + 34, y + 19,
                    selected ? MUTED : 0xFF6F8793, false);
        }
    }

    public static void commandPageButton(TerminalRenderContext context, GuiGraphicsExtractor graphics, Font font,
            int x, int y, int w, int h, TerminalIcon icon, Identifier texture, String label, String summary,
            boolean selected, boolean hovered, int color) {
        TerminalThemeTokens tokens = tokens(context);
        if (cyberglass(context)) {
            int bg = selected ? withAlpha(color, 0x24)
                    : hovered ? tokens.panels().hoverFill()
                    : withAlpha(tokens.colors().content(), 0x30);
            int border = selected ? withAlpha(color, 0xA8)
                    : hovered ? tokens.borders().normal()
                    : 0x00000000;
            cyberSurface(context, graphics, x, y, w, h, cyberRadiusMedium(), bg, border, color, selected || hovered);
            if (selected) {
                graphics.fill(x + 1, y + cyberRadiusSmall(), x + 4, y + h - cyberRadiusSmall(),
                        withAlpha(color, 0xC8));
            }
            int iconSize = Math.min(24, Math.max(16, h - 10));
            drawHybridIcon(graphics, themedVisual(context, texture), icon, x + 9, y + Math.max(4, (h - iconSize) / 2),
                    iconSize, selected ? color : accentDim(context), selected);
            graphics.text(font, trim(font, label, w - 44), x + 38, y + Math.max(5, h >= 32 ? 7 : (h - 8) / 2),
                    selected ? tokens.colors().text() : tokens.colors().muted(), false);
            if (h >= 34 && summary != null && !summary.isBlank()) {
                graphics.text(font, trim(font, summary, w - 44), x + 38, y + 21,
                        selected ? tokens.colors().muted() : tokens.output().mutedColor(), false);
            }
            return;
        }
        int bg = selected ? tokens.panels().selectedFill()
                : hovered ? tokens.panels().hoverFill() : tokens.colors().row();
        graphics.fill(x, y, x + w, y + h, bg);
        if (selected) {
            graphics.fill(x, y, x + w, y + h, withAlpha(color, 0x18));
        }
        interactionOutline(graphics, x, y, w, h, selected, hovered,
                tokens.borders().selected(), tokens.borders().normal());
        graphics.fill(x, y, x + 4, y + h, selected ? opaque(color) : tokens.dividers().line());
        drawHybridIcon(graphics, themedVisual(context, texture), icon, x + 7, y + Math.max(4, (h - 20) / 2),
                Math.min(24, Math.max(16, h - 10)), selected ? color : muted(context), selected);
        graphics.text(font, trim(font, label, w - 42), x + 34, y + Math.max(5, h >= 30 ? 6 : (h - 8) / 2),
                selected ? tokens.colors().text() : tokens.colors().muted(), false);
        if (h >= 32 && summary != null && !summary.isBlank()) {
            graphics.text(font, trim(font, summary, w - 42), x + 34, y + 19,
                    selected ? tokens.colors().muted() : tokens.output().mutedColor(), false);
        }
    }

    public static void diagnosticRail(GuiGraphicsExtractor graphics, Font font,
            int x, int y, int w, int h, boolean online, int color) {
        graphics.fill(x, y, x + w, y + h, 0x66071117);
        graphics.text(font, trim(font, "ECHO LINK", w - 20), x + 10, y + 7, CYAN_DIM, false);
        int stateColor = online ? GREEN : RED;
        graphics.text(font, online ? "ONLINE" : "OFFLINE", x + 10, y + 20, stateColor, false);
        progress(graphics, x + 10, y + h - 12, Math.max(28, w - 20), 5, online ? 0.82F : 0.12F, color);
    }

    public static void diagnosticRail(TerminalRenderContext context, GuiGraphicsExtractor graphics, Font font,
            int x, int y, int w, int h, boolean online, int color) {
        TerminalThemeTokens tokens = tokens(context);
        if (cyberglass(context)) {
            cyberSurface(context, graphics, x, y, w, h, cyberRadiusMedium(),
                    withAlpha(tokens.colors().content(), 0x90), tokens.borders().subtle(), color, false);
            graphics.text(font, trim(font, "ECHO LINK", w - 20), x + 10, y + 9, accent(context), false);
            int stateColor = online ? tokens.colors().success() : tokens.colors().danger();
            graphics.text(font, online ? "ONLINE" : "OFFLINE", x + 10, y + 25, stateColor, false);
            int bars = Math.max(4, Math.min(9, w / 16));
            int barX = x + 10;
            int barY = y + Math.max(38, h - 28);
            int filled = online ? Math.max(2, bars - 1) : 1;
            for (int i = 0; i < bars; i++) {
                int bh = 7 + i % 3;
                int bx = barX + i * 9;
                int c = i < filled ? withAlpha(color, 0xB8) : withAlpha(tokens.colors().rowSelected(), 0x75);
                glassRect(graphics, bx, barY + 10 - bh, bx + 6 - bx, bh, 2, c, 0x00000000, 0x00000000);
            }
            if (h >= 58 && w > 110) {
                graphics.text(font, "Signal", x + 10, y + h - 16, tokens.colors().muted(), false);
                String pct = online ? "94%" : "12%";
                graphics.text(font, pct, x + w - 10 - font.width(pct), y + h - 16, text(context), false);
            }
            return;
        }
        graphics.fill(x, y, x + w, y + h, tokens.colors().row());
        graphics.text(font, trim(font, "ECHO LINK", w - 20), x + 10, y + 7, tokens.colors().accentDim(), false);
        int stateColor = online ? tokens.colors().success() : tokens.colors().danger();
        graphics.text(font, online ? "ONLINE" : "OFFLINE", x + 10, y + 20, stateColor, false);
        progress(context, graphics, x + 10, y + h - 12, Math.max(28, w - 20), 5,
                online ? 0.82F : 0.12F, color);
    }

    public static void contentFrame(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int w, int h, int color, boolean hovered) {
        TerminalThemeTokens tokens = tokens(context);
        if (cyberglass(context)) {
            cyberSurface(context, graphics, x, y, w, h, cyberRadiusLarge(),
                    tokens.colors().content(), hovered ? tokens.borders().normal() : tokens.borders().subtle(),
                    color, hovered);
            return;
        }
        graphics.fill(x, y, x + w, y + h, tokens.colors().content());
        if (!TerminalClientOptions.reducedClutterMode()) {
            drawPanelTexture(context, graphics, chapterPanel(context), x, y, w, h,
                    Math.min(0.90F, tokens.panels().imageDarken() + 0.14F));
        }
        activeOutline(graphics, x, y, w, h, hovered, tokens.borders().normal());
        if (!TerminalClientOptions.reducedClutterMode()) {
            graphics.fill(x, y, x + 3, y + h, withAlpha(color, 0x70));
        }
    }

    public static void collapseToggle(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int w, int h, boolean collapsed, boolean hovered, int color) {
        TerminalThemeTokens tokens = tokens(context);
        if (cyberglass(context)) {
            int bg = hovered ? withAlpha(color, 0x22) : withAlpha(tokens.colors().content(), 0x92);
            glassRect(graphics, x, y, w, h, cyberRadiusSmall(), bg,
                    hovered ? withAlpha(color, 0x98) : tokens.borders().subtle(),
                    withAlpha(0xFFFFFFFF, 0x12));
            drawChevron(graphics, x + w / 2, y + h / 2, collapsed, hovered ? color : tokens.colors().muted());
            return;
        }
        int bg = hovered ? tokens.panels().hoverFill() : tokens.colors().row();
        graphics.fill(x, y, x + w, y + h, bg);
        activeOutline(graphics, x, y, w, h, hovered, tokens.borders().selected());
        graphics.fill(x, y + h - 2, x + w, y + h, hovered ? opaque(color) : tokens.dividers().line());
        int iconColor = hovered ? opaque(color) : tokens.colors().muted();
        drawChevron(graphics, x + w / 2, y + h / 2, collapsed, iconColor);
    }

    public static void navigationSpine(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int h, int color) {
        TerminalThemeTokens tokens = tokens(context);
        if (cyberglass(context)) {
            graphics.fill(x, y + 4, x + 1, y + h - 4, withAlpha(tokens.dividers().line(), 0x70));
            graphics.fill(x + 1, y + 4, x + 2, y + h - 4, withAlpha(color, 0x60));
            return;
        }
        graphics.fill(x, y, x + 1, y + h, tokens.dividers().line());
        graphics.fill(x + 1, y, x + 2, y + h, withAlpha(color, 0x66));
    }

    public static void collapsedRailStatus(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int w, float progressValue, int color) {
        TerminalThemeTokens tokens = tokens(context);
        int fill = Math.max(4, Math.min(w, Math.round(w * Math.max(0.0F, Math.min(1.0F, progressValue)))));
        if (cyberglass(context)) {
            glassRect(graphics, x, y, w, 6, 3, withAlpha(tokens.colors().rowSelected(), 0x8A),
                    tokens.borders().subtle(), 0x00000000);
            glassRect(graphics, x, y, fill, 6, 3, withAlpha(color, 0xBC), 0x00000000, 0x00000000);
            return;
        }
        graphics.fill(x, y, x + w, y + 4, tokens.colors().rowSelected());
        graphics.fill(x, y, x + fill, y + 4, opaque(color));
        graphics.fill(x, y + 5, x + w, y + 6, tokens.dividers().line());
    }

    public static void scrollbar(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int h, int scroll, int maxScroll, int color, boolean hovered) {
        if (maxScroll <= 0 || h <= 16) {
            return;
        }
        TerminalThemeTokens tokens = tokens(context);
        int trackW = hovered ? 5 : 4;
        int thumbH = Math.max(18, h * h / (h + maxScroll));
        int thumbY = y + Math.round((h - thumbH) * (scroll / (float) maxScroll));
        if (cyberglass(context)) {
            int w = hovered ? 6 : 5;
            glassRect(graphics, x, y, w, h, 3, withAlpha(tokens.colors().rowSelected(), 0x70),
                    0x00000000, 0x00000000);
            glassRect(graphics, x, thumbY, w, thumbH, 3, withAlpha(color, hovered ? 0xDA : 0xB0),
                    hovered ? withAlpha(color, 0xC8) : 0x00000000, 0x00000000);
            return;
        }
        graphics.fill(x, y, x + trackW, y + h, tokens.colors().rowSelected());
        graphics.fill(x, thumbY, x + trackW, thumbY + thumbH, opaque(color));
        if (hovered) {
            graphics.fill(x + 1, thumbY + 1, x + trackW - 1, thumbY + Math.max(2, thumbH - 1),
                    tokens.borders().strong());
        }
    }

    public static void cinematicContentFrame(GuiGraphicsExtractor graphics,
            int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + h, 0x5E050B10);
        graphics.fill(x, y, x + Math.max(44, Math.min(w, w / 7)), y + 2, opaque(color));
        graphics.fill(x, y + h - 2, x + Math.max(36, Math.min(w, w / 8)), y + h, opaque(color));
        graphics.fill(x + w - 2, y, x + w, y + Math.min(h, 46), 0x4A38DFF4);
        graphics.fill(x + w - Math.min(w, 60), y + h - 2, x + w, y + h, 0x3438DFF4);
    }

    public static int dashboardCard(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int w, int h, String title, int color) {
        graphics.fill(x, y, x + w, y + h, tokens(context).panels().elevatedFill());
        drawPanelTexture(context, graphics, chapterPanel(context), x, y, w, h, tokens(context).panels().imageDarken());
        graphics.fill(x, y, x + Math.max(28, Math.min(w, w / 5)), y + 2, opaque(color));
        line(context, graphics, title, x + 8, y + 7, w - 16, color);
        return y + 22;
    }

    public static int heroCard(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            Identifier texture, int x, int y, int w, int h, String title, String detail, int color) {
        imagePanel(context, graphics, texture, x, y, w, h, color, 0.56F, true);
        line(context, graphics, title, x + 10, y + 10, w - 20, text(context));
        wrap(context, graphics, detail, x + 10, y + h - 34, w - 20, text(context));
        return y + h + 10;
    }

    public static int metricTile(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int w, String label, String value, int color) {
        graphics.fill(x, y, x + w, y + 34, tokens(context).colors().row());
        line(context, graphics, label, x + 7, y + 6, w - 14, muted(context));
        line(context, graphics, value, x + 7, y + 19, w - 14, color);
        return y + 38;
    }

    public static int objectiveRow(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int w, String label, String detail, boolean complete, int color) {
        graphics.fill(x, y, x + w, y + 28, tokens(context).colors().row());
        activeOutline(graphics, x, y, w, 28, complete, withAlpha(success(context), 0x88));
        graphics.fill(x + 8, y + 8, x + 14, y + 14, complete ? success(context) : tokens(context).colors().rowSelected());
        line(context, graphics, label, x + 22, y + 5, w - 30, complete ? success(context) : text(context));
        line(context, graphics, detail, x + 22, y + 16, w - 30, muted(context));
        return y + 32;
    }

    public static void rewardTile(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            ItemStack stack, int x, int y, int w, String label, int color, int mouseX, int mouseY) {
        graphics.fill(x, y, x + w, y + 42, tokens(context).colors().row());
        itemSlot(context, graphics, stack, x + 6, y + 6, color, inside(mouseX, mouseY, x + 6, y + 6, 20, 20));
        line(context, graphics, label, x + 32, y + 11, w - 38, text(context));
    }

    public static void phaseAccordionRow(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int w, int h, String phaseNumber, String phaseTitle, String progressCount,
            boolean expanded, boolean hovered, int color) {
        TerminalThemeTokens tokens = tokens(context);
        if (cyberglass(context)) {
            int bg = hovered ? tokens.panels().hoverFill() : withAlpha(tokens.colors().content(), 0x7D);
            cyberSurface(context, graphics, x, y, w, h, cyberRadiusMedium(), bg,
                    hovered ? tokens.borders().normal() : tokens.borders().subtle(), color, hovered);
            int numberW = Math.max(22, font(context).width(phaseNumber) + 10);
            graphics.centeredText(font(context), phaseNumber, x + numberW / 2 + 4,
                    y + Math.max(5, (h - 8) / 2), color);
            int titleX = x + numberW + 12;
            boolean showChevron = w >= 120;
            int chevronX = x + w - 18;
            int countW = Math.min(Math.max(36, w / 5), font(context).width(progressCount) + 18);
            int countX = showChevron ? chevronX - countW - 8 : x + w - countW - 10;
            int titleW = Math.max(24, countX - titleX - 10);
            line(context, graphics, phaseTitle.toUpperCase(Locale.ROOT), titleX,
                    y + Math.max(5, (h - 8) / 2), titleW, text(context));
            glassRect(graphics, countX, y + Math.max(4, (h - 14) / 2), countW, 14,
                    cyberRadiusSmall(), withAlpha(color, 0x16), withAlpha(color, 0x58), 0x00000000);
            graphics.centeredText(font(context), trim(context, progressCount, countW - 8),
                    countX + countW / 2, y + Math.max(6, (h - 8) / 2), muted(context));
            if (showChevron) {
                drawChevron(graphics, chevronX, y + h / 2, !expanded, hovered ? color : tokens.colors().muted());
            }
            return;
        }
        int bg = hovered ? tokens.panels().hoverFill() : tokens.colors().row();
        graphics.fill(x, y, x + w, y + h, bg);
        activeOutline(graphics, x, y, w, h, hovered, tokens.borders().normal());
        graphics.fill(x, y, x + 3, y + h, opaque(color));
        int numberW = font(context).width(phaseNumber);
        line(context, graphics, phaseNumber, x + 10, y + Math.max(5, (h - 8) / 2), numberW + 8, color);
        int titleX = x + numberW + 18;
        boolean showChevron = w >= 120;
        int chevronX = x + w - 16;
        int countW = Math.min(Math.max(26, w / 5), font(context).width(progressCount) + 8);
        int countX = showChevron ? chevronX - countW - 8 : x + w - countW - 8;
        int titleW = Math.max(24, countX - titleX - 8);
        line(context, graphics, phaseTitle.toUpperCase(Locale.ROOT), titleX, y + Math.max(5, (h - 8) / 2), titleW, text(context));
        if (countX > titleX + 32) {
            line(context, graphics, progressCount, countX, y + Math.max(5, (h - 8) / 2), countW, muted(context));
        }
        if (!showChevron) {
            return;
        }
        int chevronY = y + h / 2;
        int chevronColor = hovered ? opaque(color) : tokens.colors().muted();
        if (expanded) {
            graphics.fill(chevronX - 3, chevronY - 1, chevronX - 1, chevronY + 1, chevronColor);
            graphics.fill(chevronX - 1, chevronY + 1, chevronX + 1, chevronY + 3, chevronColor);
            graphics.fill(chevronX + 1, chevronY + 3, chevronX + 3, chevronY + 5, chevronColor);
            graphics.fill(chevronX - 1, chevronY - 3, chevronX + 1, chevronY - 1, chevronColor);
            graphics.fill(chevronX - 3, chevronY - 5, chevronX - 1, chevronY - 3, chevronColor);
        } else {
            graphics.fill(chevronX + 1, chevronY - 5, chevronX + 3, chevronY - 3, chevronColor);
            graphics.fill(chevronX - 1, chevronY - 3, chevronX + 1, chevronY - 1, chevronColor);
            graphics.fill(chevronX - 3, chevronY - 1, chevronX - 1, chevronY + 1, chevronColor);
            graphics.fill(chevronX - 1, chevronY + 1, chevronX + 1, chevronY + 3, chevronColor);
            graphics.fill(chevronX + 1, chevronY + 3, chevronX + 3, chevronY + 5, chevronColor);
        }
    }

    public static void subduedMissionRow(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int w, int h, TerminalIcon icon, String title, String statusLabel,
            float progressValue, int color, boolean selected, boolean hovered) {
        TerminalThemeTokens tokens = tokens(context);
        if (cyberglass(context)) {
            int bg = selected ? withAlpha(color, 0x28)
                    : hovered ? tokens.panels().hoverFill()
                    : withAlpha(tokens.colors().content(), 0x66);
            cyberSurface(context, graphics, x, y, w, h, cyberRadiusMedium(), bg,
                    selected ? withAlpha(color, 0xA8) : hovered ? tokens.borders().normal() : tokens.borders().subtle(),
                    color, selected || hovered);
            if (selected) {
                graphics.fill(x + 1, y + cyberRadiusSmall(), x + 4, y + h - cyberRadiusSmall(),
                        withAlpha(color, 0xC8));
            }
            int iconSize = Math.min(26, Math.max(18, h - 14));
            int iconY = y + Math.max(5, (h - iconSize) / 2);
            iconBadge(context, graphics, icon, x + 9, iconY, iconSize, color, selected || hovered);
            int chipW = Math.max(54, Math.min(88, statusBadgeWidth(context, statusLabel)));
            int chipX = x + w - chipW - 10;
            int textX = x + iconSize + 24;
            int textW = Math.max(24, chipX - textX - 10);
            line(context, graphics, title, textX, y + Math.max(6, h >= 42 ? 9 : (h - 8) / 2),
                    textW, selected ? text(context) : muted(context));
            miniStatusPill(context, graphics, statusLabel, chipX, y + Math.max(6, (h - 14) / 2),
                    chipW, color, selected);
            if (textW > 42) {
                progress(context, graphics, textX, y + h - 10, textW, 4, progressValue, color);
            }
            return;
        }
        int bg = tokens.colors().row();
        graphics.fill(x, y, x + w, y + h, bg);
        if (selected || hovered) {
            graphics.fill(x, y, x + w, y + h, withAlpha(color, selected ? 0x26 : 0x18));
        }
        if (selected) {
            graphics.outline(x, y, w, h, withAlpha(color, 0xC8));
            graphics.fill(x, y, x + 3, y + h, opaque(color));
        } else if (hovered) {
            graphics.outline(x, y, w, h, tokens.borders().normal());
        }
        int iconSize = Math.min(18, h - 8);
        int iconY = y + Math.max(3, (h - iconSize) / 2);
        iconBadge(context, graphics, icon, x + 6, iconY, iconSize, color, selected);
        int chipW = Math.max(50, Math.min(80, statusBadgeWidth(context, statusLabel)));
        int chipX = x + w - chipW - 6;
        int textX = x + iconSize + 14;
        int textW = Math.max(24, chipX - textX - 8);
        int titleColor = selected ? text(context) : muted(context);
        line(context, graphics, title, textX, y + Math.max(4, (h - 8) / 2), textW, titleColor);
        miniStatusPill(context, graphics, statusLabel, chipX, y + Math.max(3, (h - 14) / 2), chipW, color, false);
        int barW = textW;
        if (barW > 36) {
            progress(context, graphics, textX, y + h - 8, barW, 3, progressValue, color);
        }
    }

    public static void missionCard(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            Identifier texture, int x, int y, int w, int h, String title, String detail,
            String status, int color, boolean selected, boolean hovered) {
        if (texture != null) {
            imagePanel(context, graphics, texture, x, y, w, h, color, selected ? 0.64F : 0.78F, false);
        } else {
            graphics.fill(x, y, x + w, y + h, selected ? tokens(context).panels().selectedFill()
                    : hovered ? tokens(context).panels().hoverFill() : tokens(context).colors().row());
        }
        interactionOutline(graphics, x, y, w, h, selected, hovered,
                opaque(color), tokens(context).borders().normal());
        graphics.fill(x, y, x + 3, y + h, selected ? opaque(color) : accentDim(context));
        line(context, graphics, title, x + 9, y + 7, w - 92, selected ? text(context) : muted(context));
        line(context, graphics, detail, x + 9, y + 20, w - 92, muted(context));
        miniStatusPill(context, graphics, status, x + w - 78, y + 8, 68, color, selected);
    }

    public static void actionButton(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int w, String label, int color, boolean enabled, boolean hovered) {
        button(context, graphics, x, y, w, label, color, enabled, hovered);
    }

    public static void dangerButton(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int w, String label, boolean enabled, boolean hovered) {
        button(context, graphics, x, y, w, label, RED, enabled, hovered);
    }

    public static void sortDropdownLikeChip(GuiGraphicsExtractor graphics, Font font,
            int x, int y, int w, String label, int color) {
        graphics.fill(x, y, x + w, y + 16, 0xAA071017);
        graphics.fill(x, y + 14, x + w, y + 16, 0x5538DFF4);
        graphics.text(font, trim(font, label, w - 16), x + 7, y + 5, MUTED, false);
        graphics.fill(x + w - 10, y + 7, x + w - 4, y + 8, opaque(color));
    }

    public static void scrollbar(GuiGraphicsExtractor graphics, int x, int y, int h, int scroll, int maxScroll, int color) {
        if (maxScroll <= 0 || h <= 16) {
            return;
        }
        int trackW = 4;
        graphics.fill(x, y, x + trackW, y + h, 0x44244352);
        int thumbH = Math.max(18, h * h / (h + maxScroll));
        int thumbY = y + Math.round((h - thumbH) * (scroll / (float) maxScroll));
        graphics.fill(x, thumbY, x + trackW, thumbY + thumbH, opaque(color));
    }

    public static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    public static String trim(TerminalRenderContext context, String text, int maxWidth) {
        return trim(font(context), text, maxWidth);
    }

    public static String trim(Font font, String text, int maxWidth) {
        return TerminalRenderCache.current().trim(font, text, maxWidth);
    }

    private static String trimBreadcrumb(Font font, String text, int maxWidth) {
        String value = text == null ? "" : text.strip();
        if (font.width(value) <= maxWidth) {
            return value;
        }
        int divider = value.indexOf('|');
        String prefix = divider >= 0 ? value.substring(0, divider + 1).strip() + " " : "";
        String path = divider >= 0 ? value.substring(divider + 1).strip() : value;
        String[] parts = path.split("\\s*/\\s*");
        if (parts.length > 1) {
            for (int keep = Math.min(3, parts.length); keep >= 1; keep--) {
                StringBuilder candidate = new StringBuilder(prefix).append("... / ");
                for (int i = parts.length - keep; i < parts.length; i++) {
                    if (i > parts.length - keep) {
                        candidate.append(" / ");
                    }
                    candidate.append(parts[i]);
                }
                String compact = candidate.toString();
                if (font.width(compact) <= maxWidth) {
                    return compact;
                }
            }
        }
        return trim(font, value, maxWidth);
    }

    private static String semanticName(String value) {
        String cleaned = value == null ? "" : value.strip().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        return cleaned.isBlank() ? "unknown" : cleaned;
    }

    public static int wrappedHeight(TerminalRenderContext context, String text, int maxWidth) {
        return TerminalRenderCache.current().wrappedHeight(font(context), text, maxWidth);
    }

    public static int cardHeight(TerminalRenderContext context, String detail, int width) {
        return Math.max(54, 42 + wrappedHeight(context, detail, width - 14));
    }

    public static int listHeight(int rows, int rowHeight, int headingHeight) {
        return headingHeight + Math.max(0, rows) * rowHeight;
    }

    public static int clampScroll(int value, int contentHeight, int viewportHeight) {
        return Math.max(0, Math.min(value, Math.max(0, contentHeight - viewportHeight)));
    }

    public static int opaque(int color) {
        return (color >>> 24) == 0 ? 0xFF000000 | color : color;
    }

    private static int withAlpha(int color, int alpha) {
        return ((alpha & 0xFF) << 24) | (color & 0x00FFFFFF);
    }

    private static Font font(TerminalRenderContext context) {
        if (context != null && context.minecraft() != null) {
            return context.minecraft().font;
        }
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft == null ? null : minecraft.font;
    }

    private static void drawHybridIcon(GuiGraphicsExtractor graphics, Identifier texture, TerminalIcon fallback,
            int x, int y, int size, int color, boolean active) {
        if (textureAvailable(texture) && size > 8) {
            graphics.blit(texture, x, y, x + size, y + size, 0.0F, 1.0F, 0.0F, 1.0F);
        } else {
            (fallback == null ? TerminalIcon.DEFAULT : fallback).draw(graphics, x, y, size, color, active);
        }
    }

    private static void drawCommandLabel(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int x, int y, int w, int h, String label, Identifier texture, int iconColor, int textColor, boolean active) {
        texture = themedVisual(context, texture);
        boolean drawIcon = textureAvailable(texture) && w >= 70 && h >= 16;
        int iconSize = Math.min(16, Math.max(12, h - 8));
        int textMax = drawIcon ? Math.max(28, w - 42) : w - 18;
        String trimmed = trim(context, label, textMax);
        int textY = y + Math.max(5, (h - 8) / 2);
        if (drawIcon) {
            drawHybridIcon(graphics, texture, TerminalIcon.DEFAULT, x + 10, y + Math.max(3, (h - iconSize) / 2),
                    iconSize, iconColor, active);
            int centered = x + w / 2 - font(context).width(trimmed) / 2 + 8;
            int textX = Math.max(x + 32, Math.min(centered, x + w - 8 - font(context).width(trimmed)));
            graphics.text(font(context), trimmed, textX, textY, textColor, false);
        } else {
            graphics.centeredText(font(context), trimmed, x + w / 2, textY, textColor);
        }
    }

    private static void drawChevron(GuiGraphicsExtractor graphics, int cx, int cy, boolean pointsRight, int color) {
        if (pointsRight) {
            graphics.fill(cx - 3, cy - 5, cx - 1, cy - 3, color);
            graphics.fill(cx - 1, cy - 3, cx + 1, cy - 1, color);
            graphics.fill(cx + 1, cy - 1, cx + 3, cy + 1, color);
            graphics.fill(cx - 1, cy + 1, cx + 1, cy + 3, color);
            graphics.fill(cx - 3, cy + 3, cx - 1, cy + 5, color);
        } else {
            graphics.fill(cx + 1, cy - 5, cx + 3, cy - 3, color);
            graphics.fill(cx - 1, cy - 3, cx + 1, cy - 1, color);
            graphics.fill(cx - 3, cy - 1, cx - 1, cy + 1, color);
            graphics.fill(cx - 1, cy + 1, cx + 1, cy + 3, color);
            graphics.fill(cx + 1, cy + 3, cx + 3, cy + 5, color);
        }
    }

    private static boolean textureAvailable(Identifier texture) {
        if (!isValidTextureResource(texture)) {
            return false;
        }
        Boolean cached = TEXTURE_AVAILABILITY.get(texture);
        if (cached != null) {
            return cached;
        }
        try {
            boolean available = Minecraft.getInstance().getResourceManager().getResource(texture).isPresent();
            TEXTURE_AVAILABILITY.put(texture, available);
            return available;
        } catch (RuntimeException | LinkageError ignored) {
            TEXTURE_AVAILABILITY.put(texture, false);
            return false;
        }
    }

    public static void clearTextureAvailabilityCache() {
        TEXTURE_AVAILABILITY.clear();
    }

    private static boolean isValidTextureResource(Identifier texture) {
        if (texture == null) {
            return false;
        }
        String namespace = texture.getNamespace();
        String path = texture.getPath();
        return namespace != null
                && !namespace.isBlank()
                && path != null
                && !path.isBlank()
                && path.startsWith("textures/")
                && path.endsWith(".png")
                && !path.contains("..");
    }
}
