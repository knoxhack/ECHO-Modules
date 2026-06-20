package com.knoxhack.echothemecore.client.vanilla;

import com.knoxhack.echothemecore.api.EchoTheme;
import com.knoxhack.echothemecore.api.EchoThemeColors;
import com.knoxhack.echothemecore.api.EchoThemeTextureKey;
import com.knoxhack.echothemecore.api.EchoThemeVanillaUiProfile;
import com.knoxhack.echothemecore.client.ClientThemeState;
import com.knoxhack.echothemecore.client.NativeLoaderTextIdentity;
import com.knoxhack.echothemecore.config.ThemeCoreConfig;
import com.knoxhack.echocore.client.ui.EchoCyberGlassUi;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractScrollArea;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;

public final class VanillaUiSkinLayer {
    private static final Identifier CYBERGLASS_TOOLTIP_STYLE = Identifier.fromNamespaceAndPath("echothemecore", "cyberglass");
    private static VanillaUiSurface lastSurface = VanillaUiSurface.UNKNOWN;
    private static String lastScreenClass = "";
    private static WeakReference<Screen> replacementBackgroundScreen = new WeakReference<>(null);
    private static WeakReference<Screen> replacementAccentScreen = new WeakReference<>(null);

    private VanillaUiSkinLayer() {
    }

    public static VanillaUiSurface currentSurface() {
        return lastSurface;
    }

    public static String currentScreenClass() {
        return lastScreenClass;
    }

    public static void onScreenBackground(Screen screen, GuiGraphicsExtractor graphics) {
        if (VanillaUiProductOwnership.productOwnsScreen(screen)) {
            return;
        }
        if (consumeReplacementBackground(screen)) {
            return;
        }
        lastSurface = VanillaUiScreenClassifier.classify(screen);
        lastScreenClass = screen == null ? "" : screen.getClass().getName();
        if (!VanillaUiScreenClassifier.enabled(screen, lastSurface)) {
            return;
        }
        EchoTheme theme = ClientThemeState.currentTheme();
        if (isContainerSurface(lastSurface) && screen instanceof AbstractContainerScreen<?> container) {
            renderContainerBackdrop(graphics, container, lastSurface, theme);
        } else {
            renderScreenBackdrop(graphics, screen, lastSurface, theme);
        }
    }

    public static void onScreenRender(Screen screen, GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (VanillaUiProductOwnership.productOwnsScreen(screen)) {
            return;
        }
        if (consumeReplacementAccents(screen)) {
            return;
        }
        lastSurface = VanillaUiScreenClassifier.classify(screen);
        lastScreenClass = screen == null ? "" : screen.getClass().getName();
        if (!VanillaUiScreenClassifier.enabled(screen, lastSurface)) {
            return;
        }
        EchoTheme theme = ClientThemeState.currentTheme();
        if (isContainerSurface(lastSurface) && screen instanceof AbstractContainerScreen<?> container) {
            renderContainerAccents(graphics, container, theme);
        } else {
            renderScreenAccents(graphics, screen, lastSurface, theme);
        }
        if (ThemeCoreConfig.bool(ThemeCoreConfig.BUTTON_RESKIN)) {
            renderWidgetAccents(graphics, screen, lastSurface, theme, mouseX, mouseY);
        }
        if (ThemeCoreConfig.showDebugScreenNames()) {
            drawDebugName(graphics, screen, lastSurface, theme);
        }
    }

    public static void onRenderGui(GuiGraphicsExtractor graphics) {
        if (!ThemeCoreConfig.vanillaUiEnabled() || Minecraft.getInstance().player == null) {
            return;
        }
        EchoTheme theme = ClientThemeState.currentTheme();
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        if (ThemeCoreConfig.bool(ThemeCoreConfig.THEME_HOTBAR)) {
            renderHotbarAccent(graphics, width, height, theme);
        }
        if (ThemeCoreConfig.bool(ThemeCoreConfig.THEME_BOSS_BAR)) {
            renderBossBarAccent(graphics, width, theme);
        }
        if (ThemeCoreConfig.bool(ThemeCoreConfig.THEME_CHAT)) {
            renderChatAccent(graphics, height, theme);
        }
    }

    public static Identifier tooltipTexture() {
        if (!ThemeCoreConfig.vanillaUiEnabled() || !ThemeCoreConfig.bool(ThemeCoreConfig.THEME_TOOLTIPS)) {
            return null;
        }
        return CYBERGLASS_TOOLTIP_STYLE;
    }

    public static boolean renderReplacementScreenBackground(Screen screen, GuiGraphicsExtractor graphics) {
        VanillaUiSurface surface = classifyReplacementScreen(screen, graphics);
        if (surface == null || isContainerSurface(surface) || !fullMenuReplacementEnabled(surface)) {
            return false;
        }
        renderScreenBackdrop(graphics, screen, surface, ClientThemeState.currentTheme());
        markReplacementBackground(screen);
        return true;
    }

    public static boolean renderReplacementScreenAccents(Screen screen, GuiGraphicsExtractor graphics,
            int mouseX, int mouseY) {
        VanillaUiSurface surface = classifyReplacementScreen(screen, graphics);
        if (surface == null || isContainerSurface(surface) || !fullMenuReplacementEnabled(surface)) {
            return false;
        }
        EchoTheme theme = ClientThemeState.currentTheme();
        renderScreenAccents(graphics, screen, surface, theme);
        if (ThemeCoreConfig.bool(ThemeCoreConfig.BUTTON_RESKIN)) {
            renderWidgetAccents(graphics, screen, surface, theme, mouseX, mouseY);
        }
        if (ThemeCoreConfig.showDebugScreenNames()) {
            drawDebugName(graphics, screen, surface, theme);
        }
        markReplacementAccents(screen);
        return true;
    }

    public static boolean renderReplacementContainerBackground(AbstractContainerScreen<?> screen,
            GuiGraphicsExtractor graphics) {
        VanillaUiSurface surface = classifyReplacementScreen(screen, graphics);
        if (surface == null || !isContainerSurface(surface) || !fullInventoryReplacementEnabled(surface)) {
            return false;
        }
        renderContainerBackdrop(graphics, screen, surface, ClientThemeState.currentTheme());
        markReplacementBackground(screen);
        return true;
    }

    public static boolean renderReplacementContainerAccents(AbstractContainerScreen<?> screen,
            GuiGraphicsExtractor graphics) {
        VanillaUiSurface surface = classifyReplacementScreen(screen, graphics);
        if (surface == null || !isContainerSurface(surface) || !fullInventoryReplacementEnabled(surface)) {
            return false;
        }
        renderContainerAccents(graphics, screen, ClientThemeState.currentTheme());
        markReplacementAccents(screen);
        return true;
    }

    public static boolean onToastAdd(Toast toast) {
        if (!ThemeCoreConfig.vanillaUiEnabled() || !ThemeCoreConfig.bool(ThemeCoreConfig.THEME_TOASTS)) {
            return false;
        }
        if (toast == null || toast instanceof CyberGlassToast || toast.getToken() != Toast.NO_TOKEN) {
            return false;
        }
        Minecraft.getInstance().getToastManager().addToast(new CyberGlassToast(toast));
        return true;
    }

    private static VanillaUiSurface classifyReplacementScreen(Screen screen, GuiGraphicsExtractor graphics) {
        if (screen == null || graphics == null || !ThemeCoreConfig.vanillaUiEnabled()) {
            return null;
        }
        if (VanillaUiProductOwnership.productOwnsScreen(screen)) {
            return null;
        }
        VanillaUiSurface surface = VanillaUiScreenClassifier.classify(screen);
        lastSurface = surface;
        lastScreenClass = screen.getClass().getName();
        return VanillaUiScreenClassifier.enabled(screen, surface) ? surface : null;
    }

    private static boolean fullMenuReplacementEnabled(VanillaUiSurface surface) {
        return ThemeCoreConfig.fullReplacementEnabled()
                && ThemeCoreConfig.menuReplacementEnabled()
                && surface != VanillaUiSurface.UNKNOWN;
    }

    private static boolean fullInventoryReplacementEnabled(VanillaUiSurface surface) {
        return ThemeCoreConfig.fullReplacementEnabled()
                && ThemeCoreConfig.inventoryReplacementEnabled()
                && isContainerSurface(surface);
    }

    private static void markReplacementBackground(Screen screen) {
        replacementBackgroundScreen = new WeakReference<>(screen);
    }

    private static void markReplacementAccents(Screen screen) {
        replacementAccentScreen = new WeakReference<>(screen);
    }

    private static boolean consumeReplacementBackground(Screen screen) {
        return consumeReplacementMarker(screen, true);
    }

    private static boolean consumeReplacementAccents(Screen screen) {
        return consumeReplacementMarker(screen, false);
    }

    private static boolean consumeReplacementMarker(Screen screen, boolean background) {
        if (screen == null || !ThemeCoreConfig.fullReplacementEnabled()) {
            return false;
        }
        WeakReference<Screen> reference = background ? replacementBackgroundScreen : replacementAccentScreen;
        Screen handled = reference.get();
        if (handled != screen) {
            return false;
        }
        if (background) {
            replacementBackgroundScreen = new WeakReference<>(null);
        } else {
            replacementAccentScreen = new WeakReference<>(null);
        }
        return true;
    }

    private static void renderScreenBackdrop(GuiGraphicsExtractor graphics, Screen screen, VanillaUiSurface surface, EchoTheme theme) {
        if (screen == null) {
            return;
        }
        int w = screen.width;
        int h = screen.height;
        if (w <= 0 || h <= 0) {
            return;
        }
        if (surface == VanillaUiSurface.ECHO_SCREEN) {
            EchoCyberGlassUi.screenBackdrop(graphics, w, h, EchoCyberGlassUi.Surface.ECHO_APP);
        } else if (ThemeCoreConfig.bool(ThemeCoreConfig.ENERGY_BACKGROUND)) {
            EchoCyberGlassUi.screenBackdrop(graphics, w, h, backdropSurface(surface));
        } else {
            graphics.fill(0, 0, w, h, VanillaUiColors.cappedAlpha(theme.vanillaUiProfile().backgroundTint(), 72));
        }
        if (surface == VanillaUiSurface.LOADER_MODS) {
            renderLoaderModsBackdrop(graphics, screen, theme);
        }
        if (surface == VanillaUiSurface.PAUSE_MENU) {
            renderPauseBackplate(graphics, screen, theme);
        }
        if (!ThemeCoreConfig.vanillaSafeMode() && surface != VanillaUiSurface.ECHO_SCREEN) {
            switch (surface) {
                case MAIN_MENU -> blitIfPresent(graphics, theme, EchoThemeTextureKey.VANILLA_BACKGROUND, 0, 0, w, h);
                case LOADING -> renderLoadingPanel(graphics, theme, w, h);
                case WORLD_SELECT -> renderWorldSelectArchiveBackdrop(graphics, screen, theme);
                case OPTIONS_MENU, MULTIPLAYER, RESOURCE_PACKS, SOCIAL, STATS ->
                    renderWidgetBackplate(graphics, screen, theme, EchoThemeTextureKey.VANILLA_PANEL, 12, 248, 84, true);
                default -> {
                }
            }
        }
    }

    private static void renderContainerBackdrop(GuiGraphicsExtractor graphics, AbstractContainerScreen<?> screen,
            VanillaUiSurface surface, EchoTheme theme) {
        int w = screen.width;
        int h = screen.height;
        if (w <= 0 || h <= 0) {
            return;
        }
        graphics.fill(0, 0, w, h, VanillaUiColors.cappedAlpha(theme.vanillaUiProfile().backgroundTint(), 42));
        if (ThemeCoreConfig.bool(ThemeCoreConfig.ENERGY_BACKGROUND)) {
            EchoCyberGlassUi.screenBackdrop(graphics, w, h, EchoCyberGlassUi.Surface.CONTAINER);
        }
        int x = screen.getLeftPos();
        int y = screen.getTopPos();
        int imageW = screen.getImageWidth();
        int imageH = screen.getImageHeight();
        if (ThemeCoreConfig.bool(ThemeCoreConfig.GLASS_INVENTORY_PANELS)) {
            renderContainerTerminalGlass(graphics, screen, theme, x, y, imageW, imageH);
        }
        Optional<Identifier> frame = textureFor(theme, containerFrameKey(surface));
        if (frame.isPresent() && !ThemeCoreConfig.vanillaSafeMode()) {
            drawTextureRails(graphics, frame.get(), x - 6, y - 6, imageW + 12, imageH + 12);
        }
    }

    private static void renderScreenAccents(GuiGraphicsExtractor graphics, Screen screen, VanillaUiSurface surface, EchoTheme theme) {
        if (screen == null) {
            return;
        }
        EchoThemeColors colors = theme.colors();
        EchoThemeVanillaUiProfile vanilla = theme.vanillaUiProfile();
        int w = screen.width;
        int h = screen.height;
        if (w <= 0 || h <= 0) {
            return;
        }
        int bg = VanillaUiColors.cappedAlpha(vanilla.backgroundTint(), surface == VanillaUiSurface.MAIN_MENU ? 58 : 34);
        int panel = VanillaUiColors.cappedAlpha(vanilla.panelTint(), ThemeCoreConfig.vanillaSafeMode() ? 58 : 82);
        int border = VanillaUiColors.cappedAlpha(colors.border(), ThemeCoreConfig.bool(ThemeCoreConfig.VANILLA_EDGE_GLOW) ? 132 : 86);

        if (ThemeCoreConfig.bool(ThemeCoreConfig.ENERGY_BACKGROUND) && surface != VanillaUiSurface.LOADING) {
            graphics.fill(0, 0, w, 18, bg);
            graphics.fill(0, h - 18, w, h, bg);
        }

        EchoCyberGlassUi.screenChrome(graphics, w, h, surface != VanillaUiSurface.MAIN_MENU);

        if (surface == VanillaUiSurface.ECHO_SCREEN) {
            if (ClientThemeState.transitioning()) {
                int alpha = Math.round(54.0F * (1.0F - ClientThemeState.transitionProgress()));
                graphics.fill(0, 0, w, h, EchoThemeColors.withAlpha(colors.glow(), alpha));
            }
            return;
        }

        if (surface == VanillaUiSurface.LOADER_MODS) {
            renderLoaderModsAccents(graphics, screen, theme);
        }

        if (ThemeCoreConfig.bool(ThemeCoreConfig.TRANSPARENT_PANELS) && surface != VanillaUiSurface.MAIN_MENU) {
            int inset = Math.max(12, Math.min(w, h) / 18);
            if (w > inset * 2 && h > inset * 2) {
                graphics.fill(inset, inset, w - inset, inset + 1, panel);
                graphics.fill(inset, h - inset - 1, w - inset, h - inset, panel);
            }
        }

        // Texture-backed panel decoration for supported surfaces
        if (!ThemeCoreConfig.vanillaSafeMode()) {
            switch (surface) {
                case MAIN_MENU -> blitIfPresent(graphics, theme, EchoThemeTextureKey.VANILLA_TITLE_BACKPLATE, w / 2 - 200, h / 2 - 80, 400, 160);
                case WORLD_SELECT -> renderWorldSelectArchiveAccents(graphics, screen, theme);
                case LOADING -> renderLoadingPanelAccents(graphics, theme, w, h);
                default -> {
                }
            }
        }

        if (ClientThemeState.transitioning()) {
            int alpha = Math.round(70.0F * (1.0F - ClientThemeState.transitionProgress()));
            graphics.fill(0, 0, w, h, EchoThemeColors.withAlpha(colors.glow(), alpha));
        }
        renderNativeLoaderBadge(graphics, surface, theme, w, h);
    }

    private static void renderContainerAccents(GuiGraphicsExtractor graphics, AbstractContainerScreen<?> screen, EchoTheme theme) {
        EchoThemeColors colors = theme.colors();
        VanillaUiProtectedBounds bounds = containerThemeBounds(screen);
        int x = bounds.x();
        int y = bounds.y();
        int w = bounds.width();
        int h = bounds.height();
        if (w <= 0 || h <= 0) {
            return;
        }
        boolean inventory = isPlayerInventoryScreen(screen);
        int borderAlpha = inventory ? inventoryChromeAlpha(theme, 176, 122, 118, 188) : 122;
        int softAlpha = inventory ? inventoryChromeAlpha(theme, 96, 62, 56, 108) : 62;
        int glowAlpha = inventory
                ? inventoryChromeAlpha(theme, 76, ThemeCoreConfig.vanillaSafeMode() ? 28 : 42, 34, 92)
                : ThemeCoreConfig.vanillaSafeMode() ? 28 : 42;
        int border = VanillaUiColors.cappedAlpha(colors.border(), borderAlpha);
        int soft = VanillaUiColors.cappedAlpha(colors.borderSoft(), softAlpha);
        int glow = VanillaUiColors.cappedAlpha(colors.glow(), glowAlpha);
        if (ThemeCoreConfig.bool(ThemeCoreConfig.GLASS_INVENTORY_PANELS)) {
            renderInventoryToneWash(graphics, theme, x, y, w, h, inventory);
            if (!inventory) {
                outlineIfPositive(graphics, x - 2, y - 2, w + 4, h + 4, soft);
            }
            outlineIfPositive(graphics, x, y, w, h, border);
            renderContainerSlotAccents(graphics, screen, x, y, w, h, containerSlotAccent(theme, inventory));
        }
        if (ThemeCoreConfig.bool(ThemeCoreConfig.VANILLA_EDGE_GLOW)) {
            graphics.fill(Math.max(0, x - 5), Math.max(0, y - 5), x - 3, y + h + 5, glow);
            graphics.fill(x + w + 3, Math.max(0, y - 5), Math.min(screen.width, x + w + 5), y + h + 5, glow);
        }
    }

    private static void renderInventoryToneWash(GuiGraphicsExtractor graphics, EchoTheme theme,
            int x, int y, int w, int h, boolean inventory) {
        if (!inventory || !ThemeCoreConfig.bool(ThemeCoreConfig.REDUCE_VANILLA_BROWN)
                || !theme.vanillaUiProfile().reduceVanillaBrown()) {
            return;
        }
        EchoThemeColors colors = theme.colors();
        int wash = VanillaUiColors.cappedAlpha(colors.background(), inventoryOverlayAlpha(theme, 34, 8, 18));
        int cool = VanillaUiColors.cappedAlpha(colors.glow(), inventoryOverlayAlpha(theme, 22, 5, 12));
        int rail = VanillaUiColors.cappedAlpha(colors.borderSoft(), inventoryChromeAlpha(theme, 34, 14, 12, 28));
        graphics.fill(x, y, x + w, y + h, wash);
        if (w > 12 && h > 10) {
            graphics.fill(x + 1, y + 1, x + w - 1, y + Math.min(y + h - 1, y + 17), cool);
            graphics.fill(x + 1, y + 1, x + 2, y + h - 1, rail);
            graphics.fill(Math.max(x + 2, x + w - 3), y + 2, x + w - 2, Math.min(y + h - 2, y + h / 2), rail);
        }
    }

    private static void renderContainerTerminalGlass(GuiGraphicsExtractor graphics, AbstractContainerScreen<?> screen,
            EchoTheme theme, int x, int y, int w, int h) {
        EchoThemeColors colors = theme.colors();
        boolean inventory = isPlayerInventoryScreen(screen);
        int fillAlpha = ThemeCoreConfig.vanillaSafeMode() ? 30 : inventory ? inventoryPanelAlpha(theme, 152, 76, 150) : 30;
        int topAlpha = ThemeCoreConfig.vanillaSafeMode() ? 10 : inventory ? inventoryOverlayAlpha(theme, 72, 20, 42) : 12;
        int railAlpha = ThemeCoreConfig.vanillaSafeMode() ? 36 : inventory ? inventoryChromeAlpha(theme, 168, 112, 112, 196) : 54;
        int fill = VanillaUiColors.cappedAlpha(inventory ? colors.background() : theme.vanillaUiProfile().panelTint(), fillAlpha);
        int topGlow = VanillaUiColors.cappedAlpha(colors.glow(), topAlpha);
        int rail = VanillaUiColors.cappedAlpha(colors.border(), railAlpha);

        graphics.fill(x, y, x + w, y + h, fill);
        if (w > 16 && h > 12) {
            graphics.fill(x + 1, y + 1, x + w - 1, y + Math.min(y + h - 1, y + 18), topGlow);
            int topRailEnd = Math.min(x + w - 2, x + Math.max(8, Math.min(72, w / 3)));
            int sideRailEnd = Math.min(y + h - 2, y + Math.max(8, Math.min(58, h / 3)));
            int bottomRailEnd = Math.min(x + w - 2, x + Math.max(8, Math.min(54, w / 4)));
            if (topRailEnd > x + 2) {
                graphics.fill(x + 2, y + 2, topRailEnd, y + 3, rail);
            }
            if (sideRailEnd > y + 2) {
                graphics.fill(x + w - 3, y + 2, x + w - 2, sideRailEnd, rail);
            }
            if (bottomRailEnd > x + 2) {
                graphics.fill(x + 2, y + h - 3, bottomRailEnd, y + h - 2, rail);
            }
        }
    }

    private static int containerSlotAccent(EchoTheme theme, boolean inventory) {
        EchoThemeColors colors = theme.colors();
        int slotAlpha = ThemeCoreConfig.vanillaSafeMode() ? 34 : inventory ? inventoryPanelAlpha(theme, 172, 90, 124) : 50;
        return VanillaUiColors.cappedAlpha(colors.borderSoft(), slotAlpha);
    }

    private static int inventoryPanelAlpha(EchoTheme theme, int multiplier, int min, int max) {
        return scaledAlpha(theme.vanillaUiProfile().panelOpacity(), multiplier, min, max);
    }

    private static int inventoryOverlayAlpha(EchoTheme theme, int multiplier, int min, int max) {
        return scaledAlpha(theme.vanillaUiProfile().overlayOpacity(), multiplier, min, max);
    }

    private static int inventoryChromeAlpha(EchoTheme theme, int multiplier, int fallback, int min, int max) {
        if (ThemeCoreConfig.vanillaSafeMode()) {
            return fallback;
        }
        return scaledAlpha(theme.vanillaUiProfile().edgeGlowStrength(), multiplier, min, max);
    }

    private static int scaledAlpha(float value, int multiplier, int min, int max) {
        int scaled = Math.round(Math.max(0.0F, value) * multiplier);
        return Math.max(min, Math.min(max, scaled));
    }

    private static EchoThemeTextureKey containerFrameKey(VanillaUiSurface surface) {
        VanillaUiSurface mode = surface == null ? VanillaUiSurface.CONTAINER : surface;
        return switch (mode) {
            case CREATIVE_INVENTORY -> EchoThemeTextureKey.VANILLA_CREATIVE_FRAME;
            case INVENTORY -> EchoThemeTextureKey.VANILLA_INVENTORY_FRAME;
            default -> EchoThemeTextureKey.VANILLA_CONTAINER_FRAME;
        };
    }

    private static VanillaUiProtectedBounds containerThemeBounds(AbstractContainerScreen<?> screen) {
        int x = screen.getLeftPos();
        int y = screen.getTopPos();
        int w = screen.getImageWidth();
        int h = screen.getImageHeight();
        if (isPlayerInventoryScreen(screen)) {
            w = Math.min(w, 176);
            h = Math.min(h, 166);
        }
        return new VanillaUiProtectedBounds(x, y, w, h);
    }

    private static void renderContainerSlotAccents(GuiGraphicsExtractor graphics, AbstractContainerScreen<?> screen,
            int left, int top, int width, int height, int color) {
        if (color == 0 || screen.getMenu() == null) {
            return;
        }
        for (Slot slot : screen.getMenu().slots) {
            if (slot == null || !slot.isActive()) {
                continue;
            }
            int x = left + slot.x - 1;
            int y = top + slot.y - 1;
            if (x < left || y < top || x + 18 > left + width || y + 18 > top + height) {
                continue;
            }
            outlineIfPositive(graphics, x, y, 18, 18, color);
        }
    }

    private static boolean isPlayerInventoryScreen(AbstractContainerScreen<?> screen) {
        return screen != null && screen.getClass().getName().endsWith(".InventoryScreen");
    }

    private static void renderWorldSelectArchiveBackdrop(GuiGraphicsExtractor graphics, Screen screen, EchoTheme theme) {
        if (graphics == null || screen == null || textureFor(theme, EchoThemeTextureKey.VANILLA_PANEL).isEmpty()) {
            return;
        }
        VanillaUiProtectedBounds bounds = worldSelectArchiveBounds(screen);
        if (bounds.width() <= 0 || bounds.height() <= 0) {
            return;
        }
        EchoThemeColors colors = theme.colors();
        int fill = VanillaUiColors.cappedAlpha(theme.vanillaUiProfile().panelTint(), 36);
        int topGlow = VanillaUiColors.cappedAlpha(colors.glow(), 16);
        int grid = VanillaUiColors.cappedAlpha(colors.borderSoft(), 18);
        graphics.fill(bounds.x(), bounds.y(), bounds.x() + bounds.width(), bounds.y() + bounds.height(), fill);
        if (bounds.width() > 16 && bounds.height() > 16) {
            graphics.fill(bounds.x() + 1, bounds.y() + 1, bounds.x() + bounds.width() - 1,
                bounds.y() + Math.min(bounds.height() - 1, 18), topGlow);
            EchoCyberGlassUi.quietGrid(graphics, bounds.x() + 2, bounds.y() + 2,
                bounds.width() - 4, bounds.height() - 4, Math.max(20, Math.min(34, bounds.width() / 12)), grid);
        }
    }

    private static void renderWorldSelectArchiveAccents(GuiGraphicsExtractor graphics, Screen screen, EchoTheme theme) {
        if (graphics == null || screen == null || textureFor(theme, EchoThemeTextureKey.VANILLA_PANEL).isEmpty()) {
            return;
        }
        VanillaUiProtectedBounds bounds = worldSelectArchiveBounds(screen);
        if (bounds.width() <= 0 || bounds.height() <= 0) {
            return;
        }
        EchoThemeColors colors = theme.colors();
        int border = VanillaUiColors.cappedAlpha(colors.border(), 76);
        int soft = VanillaUiColors.cappedAlpha(colors.borderSoft(), 52);
        int glow = VanillaUiColors.cappedAlpha(colors.glow(), 46);
        EchoCyberGlassUi.calmFrame(graphics, bounds.x(), bounds.y(), bounds.width(), bounds.height(), border);
        if (bounds.width() > 34 && bounds.height() > 12) {
            graphics.fill(bounds.x() + 2, bounds.y() + 2,
                bounds.x() + Math.max(28, Math.min(bounds.width() / 4, 86)), bounds.y() + 3, glow);
            graphics.fill(bounds.x() + 2, bounds.y() + bounds.height() - 3,
                bounds.x() + Math.max(24, Math.min(bounds.width() / 5, 62)), bounds.y() + bounds.height() - 2, soft);
            graphics.fill(bounds.x() + bounds.width() - 3, bounds.y() + 2,
                bounds.x() + bounds.width() - 2, bounds.y() + Math.max(18, Math.min(bounds.height() / 5, 54)), soft);
        }
    }

    private static VanillaUiProtectedBounds worldSelectArchiveBounds(Screen screen) {
        Optional<VanillaUiProtectedBounds> listBounds = largestScrollBounds(screen);
        VanillaUiProtectedBounds bounds = listBounds.orElseGet(() -> fallbackWorldSelectBounds(screen));
        int padding = listBounds.isPresent() ? 8 : 0;
        int x = Math.max(8, bounds.x() - padding);
        int y = Math.max(8, bounds.y() - padding);
        int right = Math.min(screen.width - 8, bounds.x() + bounds.width() + padding);
        int bottom = Math.min(screen.height - 32, bounds.y() + bounds.height() + padding);
        return new VanillaUiProtectedBounds(x, y, Math.max(0, right - x), Math.max(0, bottom - y));
    }

    private static Optional<VanillaUiProtectedBounds> largestScrollBounds(Screen screen) {
        VanillaUiProtectedBounds best = null;
        int bestArea = 0;
        for (GuiEventListener listener : screen.children()) {
            if (listener instanceof AbstractSelectionList<?> list && list.visible) {
                int area = list.getWidth() * list.getHeight();
                if (area > bestArea) {
                    best = new VanillaUiProtectedBounds(list.getX(), list.getY(), list.getWidth(), list.getHeight());
                    bestArea = area;
                }
            } else if (listener instanceof AbstractScrollArea scrollArea && scrollArea.visible) {
                int area = scrollArea.getWidth() * scrollArea.getHeight();
                if (area > bestArea) {
                    best = new VanillaUiProtectedBounds(scrollArea.getX(), scrollArea.getY(),
                        scrollArea.getWidth(), scrollArea.getHeight());
                    bestArea = area;
                }
            }
        }
        return Optional.ofNullable(best);
    }

    private static VanillaUiProtectedBounds fallbackWorldSelectBounds(Screen screen) {
        int width = Math.min(Math.max(220, screen.width / 4), Math.max(220, screen.width - 48));
        int height = Math.max(92, screen.height - 122);
        int x = Math.max(12, (screen.width - width) / 2);
        int y = Math.max(48, Math.min(screen.height - 56, 58));
        return new VanillaUiProtectedBounds(x, y, width, Math.min(height, Math.max(0, screen.height - y - 52)));
    }

    private static void renderHotbarAccent(GuiGraphicsExtractor graphics, int width, int height, EchoTheme theme) {
        if (width <= 0 || height <= 0) {
            return;
        }
        int selected = Minecraft.getInstance().player == null ? 0 : Minecraft.getInstance().player.getInventory().getSelectedSlot();
        int hotbarX = width / 2 - 91;
        int hotbarY = height - 22;
        int accent = VanillaUiColors.cappedAlpha(theme.vanillaUiProfile().hotbarAccent(), 165);
        int soft = VanillaUiColors.cappedAlpha(theme.colors().borderSoft(), 90);

        Optional<Identifier> hotbarTex = textureFor(theme, EchoThemeTextureKey.HUD_HOTBAR_FRAME)
            .or(() -> textureFor(theme, EchoThemeTextureKey.VANILLA_SELECTED_SLOT));
        if (hotbarTex.isPresent() && !ThemeCoreConfig.vanillaSafeMode()) {
            blitStretched(graphics, hotbarTex.get(), hotbarX, hotbarY + 18, 182, 4);
        } else {
            graphics.fill(hotbarX, hotbarY + 20, hotbarX + 182, hotbarY + 22, soft);
        }
        if (ThemeCoreConfig.itemIconChromeEnabled()) {
            Optional<Identifier> frame = textureFor(theme, EchoThemeTextureKey.ITEM_ICON_FRAME);
            for (int slot = 0; slot < 9; slot++) {
                int slotX = hotbarX + slot * 20 + 1;
                int slotY = hotbarY + 1;
                if (frame.isPresent() && !ThemeCoreConfig.vanillaSafeMode()) {
                    blitStretched(graphics, frame.get(), slotX, slotY, 18, 18);
                } else {
                    outlineIfPositive(graphics, slotX, slotY, 18, 18, soft);
                }
            }
        }
        Optional<Identifier> selectedTex = textureFor(theme, EchoThemeTextureKey.HUD_SELECTED_SLOT)
            .or(() -> textureFor(theme, EchoThemeTextureKey.ITEM_ICON_RARITY_RING));
        int selectedX = hotbarX + selected * 20;
        if (selectedTex.isPresent() && !ThemeCoreConfig.vanillaSafeMode()) {
            blitStretched(graphics, selectedTex.get(), selectedX, hotbarY, 22, 22);
        }
        outlineIfPositive(graphics, selectedX, hotbarY, 22, 22, accent);
    }

    private static void renderBossBarAccent(GuiGraphicsExtractor graphics, int width, EchoTheme theme) {
        if (width <= 0) {
            return;
        }
        int x = width / 2 - 92;
        int color = VanillaUiColors.cappedAlpha(theme.colors().glow(), 70);
        Optional<Identifier> bossBar = textureFor(theme, EchoThemeTextureKey.VANILLA_BOSS_BAR_ACCENT);
        if (bossBar.isPresent() && !ThemeCoreConfig.vanillaSafeMode()) {
            blitStretched(graphics, bossBar.get(), x, 10, 184, 8);
        } else {
            graphics.fill(x, 13, x + 184, 14, color);
        }
    }

    private static void renderChatAccent(GuiGraphicsExtractor graphics, int height, EchoTheme theme) {
        if (height <= 0) {
            return;
        }
        int color = VanillaUiColors.cappedAlpha(theme.vanillaUiProfile().chatAccent(), 60);
        graphics.fill(0, height - 58, 3, height - 18, color);
    }

    private static void drawDebugName(GuiGraphicsExtractor graphics, Screen screen, VanillaUiSurface surface, EchoTheme theme) {
        Font font = Minecraft.getInstance().font;
        int panel = VanillaUiColors.cappedAlpha(theme.colors().panel(), 180);
        int text = VanillaUiColors.readableText(theme.colors(), panel);
        String name = surface + "  " + (screen == null ? "<none>" : screen.getClass().getSimpleName());
        graphics.fill(4, 4, 8 + font.width(name) + 4, 18, panel);
        graphics.text(font, name, 8, 8, text, false);
    }

    private static void renderNativeLoaderBadge(GuiGraphicsExtractor graphics, VanillaUiSurface surface, EchoTheme theme,
            int screenWidth, int screenHeight) {
        if (!NativeLoaderTextIdentity.active() || !nativeLoaderBadgeSurface(surface) || screenWidth <= 0 || screenHeight <= 0) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        EchoThemeColors colors = theme.colors();
        String first = NativeLoaderTextIdentity.badgeLabel();
        String second = NativeLoaderTextIdentity.productLabel();
        int maxTextWidth = Math.max(96, Math.min(screenWidth - 24, 310));
        first = trimmed(font, first, maxTextWidth);
        second = trimmed(font, second, maxTextWidth);
        int width = Math.min(screenWidth - 12, Math.max(font.width(first), font.width(second)) + 18);
        int height = 30;
        int x = 6;
        int y = surface == VanillaUiSurface.LOADING ? 22 : 6;
        int panel = VanillaUiColors.cappedAlpha(colors.background(), 172);
        int border = VanillaUiColors.cappedAlpha(colors.border(), 164);
        int rail = VanillaUiColors.cappedAlpha(colors.selection(), 118);
        int text = VanillaUiColors.readableText(colors, panel);
        graphics.fill(x, y, x + width, y + height, panel);
        graphics.fill(x + 1, y + 1, x + width - 1, y + 4, rail);
        EchoCyberGlassUi.calmFrame(graphics, x, y, width, height, border);
        graphics.text(font, first, x + 8, y + 8, colors.primary(), false);
        graphics.text(font, second, x + 8, y + 19, text, false);
    }

    private static boolean nativeLoaderBadgeSurface(VanillaUiSurface surface) {
        return switch (surface) {
            case MAIN_MENU, PAUSE_MENU, LOADING, LOADER_MODS -> true;
            default -> false;
        };
    }

    private static boolean isContainerSurface(VanillaUiSurface surface) {
        return switch (surface) {
            case INVENTORY, CREATIVE_INVENTORY, CONTAINER, FURNACE, CRAFTING, ANVIL, ENCHANTING, GRINDSTONE, SMITHING -> true;
            default -> false;
        };
    }

    private static void blitIfPresent(GuiGraphicsExtractor graphics, EchoTheme theme, EchoThemeTextureKey key, int x, int y, int w, int h) {
        Optional<Identifier> tex = textureFor(theme, key);
        if (tex.isPresent()) {
            drawDecorPlate(graphics, theme, tex.get(), x, y, w, h);
        }
    }

    private static void renderWidgetBackplate(GuiGraphicsExtractor graphics, Screen screen, EchoTheme theme,
            EchoThemeTextureKey textureKey, int padding, int minWidth, int minHeight) {
        renderWidgetBackplate(graphics, screen, theme, textureKey, padding, minWidth, minHeight, false);
    }

    private static void renderWidgetBackplate(GuiGraphicsExtractor graphics, Screen screen, EchoTheme theme,
            EchoThemeTextureKey textureKey, int padding, int minWidth, int minHeight, boolean compact) {
        if (screen == null) {
            return;
        }
        Optional<Identifier> texture = textureFor(theme, textureKey);
        widgetClusterBounds(screen, padding, minWidth, minHeight)
            .ifPresent(bounds -> drawDecorPlate(graphics, theme, texture.orElse(null),
                bounds.x(), bounds.y(), bounds.width(), bounds.height(), compact));
    }

    private static void renderPauseBackplate(GuiGraphicsExtractor graphics, Screen screen, EchoTheme theme) {
        if (screen == null) {
            return;
        }
        Optional<Identifier> texture = textureFor(theme, EchoThemeTextureKey.VANILLA_PAUSE_PANEL);
        pauseButtonCluster(screen).ifPresent(cluster -> {
            VanillaUiProtectedBounds bounds = pausePanelBounds(screen.width, screen.height, cluster);
            drawPauseTitleChip(graphics, screen, theme, bounds, cluster);
            drawPauseMenuPlate(graphics, theme, texture.orElse(null),
                bounds.x(), bounds.y(), bounds.width(), bounds.height());
        });
    }

    private static Optional<VanillaUiProtectedBounds> widgetClusterBounds(Screen screen, int padding, int minWidth, int minHeight) {
        if (screen == null) {
            return Optional.empty();
        }
        List<VanillaUiProtectedBounds> widgets = new ArrayList<>();
        for (GuiEventListener listener : screen.children()) {
            if (listener instanceof AbstractWidget widget && widget.visible
                    && widget.getWidth() > 0 && widget.getHeight() > 0) {
                widgets.add(new VanillaUiProtectedBounds(widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight()));
            }
        }
        if (widgets.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(widgetClusterBounds(screen.width, screen.height, widgets, padding, minWidth, minHeight));
    }

    private static Optional<VanillaUiProtectedBounds> pausePanelBounds(Screen screen) {
        return pauseButtonCluster(screen)
            .map(cluster -> pausePanelBounds(screen.width, screen.height, cluster));
    }

    private static Optional<List<VanillaUiProtectedBounds>> pauseButtonCluster(Screen screen) {
        if (screen == null) {
            return Optional.empty();
        }
        List<VanillaUiProtectedBounds> buttons = new ArrayList<>();
        for (GuiEventListener listener : screen.children()) {
            if (listener instanceof Button button && button.visible
                    && button.getWidth() > 0 && button.getHeight() > 0
                    && isSanePauseControl(screen.width, screen.height, button.getWidth(), button.getHeight())) {
                buttons.add(new VanillaUiProtectedBounds(button.getX(), button.getY(), button.getWidth(), button.getHeight()));
            }
        }
        if (buttons.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(selectedPauseButtonCluster(screen.width, screen.height, buttons));
    }

    public static VanillaUiProtectedBounds widgetClusterBoundsForTests(int screenWidth, int screenHeight,
            List<VanillaUiProtectedBounds> widgets, int padding, int minWidth, int minHeight) {
        return widgetClusterBounds(screenWidth, screenHeight, widgets, padding, minWidth, minHeight);
    }

    public static VanillaUiProtectedBounds pausePanelBoundsForTests(int screenWidth, int screenHeight,
            List<VanillaUiProtectedBounds> buttons) {
        return pausePanelBounds(screenWidth, screenHeight, buttons);
    }

    public static VanillaUiProtectedBounds pauseTitleChipBoundsForTests(int screenWidth, int screenHeight,
            List<VanillaUiProtectedBounds> buttons, int titleWidth) {
        List<VanillaUiProtectedBounds> cluster = selectedPauseButtonCluster(screenWidth, screenHeight, buttons);
        return pauseTitleChipBounds(screenWidth, screenHeight, pausePanelBounds(screenWidth, screenHeight, cluster),
            boundsOf(cluster), titleWidth);
    }

    public static boolean shouldDecorateWidgetForTests(VanillaUiSurface surface, int screenWidth, int screenHeight,
            VanillaUiProtectedBounds widget, VanillaUiProtectedBounds pausePanel) {
        return shouldDecorateWidgetAccent(surface, screenWidth, screenHeight, widget, pausePanel, pausePanel);
    }

    public static boolean shouldDecorateWidgetForTests(VanillaUiSurface surface, int screenWidth, int screenHeight,
            VanillaUiProtectedBounds widget, VanillaUiProtectedBounds pausePanel, VanillaUiProtectedBounds pauseCluster) {
        return shouldDecorateWidgetAccent(surface, screenWidth, screenHeight, widget, pausePanel, pauseCluster);
    }

    private static VanillaUiProtectedBounds widgetClusterBounds(int screenWidth, int screenHeight,
            List<VanillaUiProtectedBounds> widgets, int padding, int minWidth, int minHeight) {
        int minX = screenWidth;
        int minY = screenHeight;
        int maxX = 0;
        int maxY = 0;
        for (VanillaUiProtectedBounds widget : widgets) {
            if (widget == null || widget.width() <= 0 || widget.height() <= 0) {
                continue;
            }
            minX = Math.min(minX, widget.x());
            minY = Math.min(minY, widget.y());
            maxX = Math.max(maxX, widget.x() + widget.width());
            maxY = Math.max(maxY, widget.y() + widget.height());
        }
        if (maxX <= minX || maxY <= minY) {
            int w = Math.min(Math.max(1, minWidth), Math.max(1, screenWidth - 8));
            int h = Math.min(Math.max(1, minHeight), Math.max(1, screenHeight - 8));
            return new VanillaUiProtectedBounds(Math.max(0, (screenWidth - w) / 2),
                Math.max(0, (screenHeight - h) / 2), w, h);
        }
        int centerX = (minX + maxX) / 2;
        int centerY = (minY + maxY) / 2;
        int maxWidth = Math.max(1, screenWidth - 8);
        int maxHeight = Math.max(1, screenHeight - 8);
        int width = Math.min(Math.max(maxX - minX + padding * 2, minWidth), maxWidth);
        int height = Math.min(Math.max(maxY - minY + padding * 2, minHeight), maxHeight);
        int x = clamp(centerX - width / 2, 4, Math.max(4, screenWidth - width - 4));
        int y = clamp(centerY - height / 2, 4, Math.max(4, screenHeight - height - 4));
        return new VanillaUiProtectedBounds(x, y, width, height);
    }

    private static VanillaUiProtectedBounds pausePanelBounds(int screenWidth, int screenHeight,
            List<VanillaUiProtectedBounds> buttons) {
        List<VanillaUiProtectedBounds> cluster = selectedPauseButtonCluster(screenWidth, screenHeight, buttons);
        if (cluster.isEmpty()) {
            return widgetClusterBounds(screenWidth, screenHeight, buttons, 10, 228, 70);
        }
        return pausePanelBoundsForCluster(screenWidth, screenHeight, cluster);
    }

    private static VanillaUiProtectedBounds pausePanelBoundsForCluster(int screenWidth, int screenHeight,
            List<VanillaUiProtectedBounds> cluster) {
        VanillaUiProtectedBounds buttonBounds = boundsOf(cluster);
        if (buttonBounds.width() <= 0 || buttonBounds.height() <= 0) {
            return widgetClusterBounds(screenWidth, screenHeight, cluster, 10, 228, 70);
        }
        int sidePad = 10;
        int topPad = Math.max(14, Math.min(18, screenHeight / 36));
        int bottomPad = 12;
        int minWidth = 228;
        int maxWidth = Math.max(1, screenWidth - 8);
        int maxHeight = Math.max(1, screenHeight - 8);
        int width = Math.min(Math.max(buttonBounds.width() + sidePad * 2, minWidth), maxWidth);
        int height = Math.min(buttonBounds.height() + topPad + bottomPad, maxHeight);
        int centerX = buttonBounds.x() + buttonBounds.width() / 2;
        int x = clamp(centerX - width / 2, 4, Math.max(4, screenWidth - width - 4));
        int y = clamp(buttonBounds.y() - topPad, 4, Math.max(4, screenHeight - height - 4));
        return new VanillaUiProtectedBounds(x, y, width, height);
    }

    private static List<VanillaUiProtectedBounds> selectedPauseButtonCluster(int screenWidth, int screenHeight,
            List<VanillaUiProtectedBounds> buttons) {
        List<VanillaUiProtectedBounds> sane = new ArrayList<>();
        for (VanillaUiProtectedBounds button : buttons) {
            if (button != null && isSanePauseControl(screenWidth, screenHeight, button.width(), button.height())) {
                sane.add(button);
            }
        }
        if (sane.isEmpty()) {
            return List.of();
        }
        sane.sort((left, right) -> {
            int y = Integer.compare(left.y(), right.y());
            return y != 0 ? y : Integer.compare(left.x(), right.x());
        });
        int gapThreshold = Math.max(10, Math.min(28, screenHeight / 28));
        List<List<VanillaUiProtectedBounds>> groups = new ArrayList<>();
        List<VanillaUiProtectedBounds> current = new ArrayList<>();
        int lastBottom = Integer.MIN_VALUE;
        for (VanillaUiProtectedBounds button : sane) {
            if (!current.isEmpty() && button.y() - lastBottom > gapThreshold) {
                groups.add(current);
                current = new ArrayList<>();
            }
            current.add(button);
            lastBottom = Math.max(lastBottom, button.y() + button.height());
        }
        if (!current.isEmpty()) {
            groups.add(current);
        }
        List<VanillaUiProtectedBounds> best = groups.get(0);
        int bestScore = Integer.MIN_VALUE;
        int screenCenterX = screenWidth / 2;
        int screenCenterY = screenHeight / 2;
        for (List<VanillaUiProtectedBounds> group : groups) {
            VanillaUiProtectedBounds bounds = boundsOf(group);
            int centerX = bounds.x() + bounds.width() / 2;
            int centerY = bounds.y() + bounds.height() / 2;
            int score = group.size() * 100_000
                - Math.abs(centerX - screenCenterX) * 80
                - Math.abs(centerY - screenCenterY) * 8
                - Math.max(0, bounds.height() - screenHeight / 3) * 200;
            if (score > bestScore) {
                bestScore = score;
                best = group;
            }
        }
        return best;
    }

    private static VanillaUiProtectedBounds boundsOf(List<VanillaUiProtectedBounds> widgets) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = 0;
        int maxY = 0;
        for (VanillaUiProtectedBounds widget : widgets) {
            if (widget == null || widget.width() <= 0 || widget.height() <= 0) {
                continue;
            }
            minX = Math.min(minX, widget.x());
            minY = Math.min(minY, widget.y());
            maxX = Math.max(maxX, widget.x() + widget.width());
            maxY = Math.max(maxY, widget.y() + widget.height());
        }
        if (maxX <= minX || maxY <= minY) {
            return new VanillaUiProtectedBounds(0, 0, 0, 0);
        }
        return new VanillaUiProtectedBounds(minX, minY, maxX - minX, maxY - minY);
    }

    private static void drawPauseTitleChip(GuiGraphicsExtractor graphics, Screen screen, EchoTheme theme,
            VanillaUiProtectedBounds panel, List<VanillaUiProtectedBounds> cluster) {
        if (graphics == null || screen == null || panel == null || cluster == null || cluster.isEmpty()) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        int titleWidth = font == null ? 64 : font.width(screen.getTitle());
        VanillaUiProtectedBounds clusterBounds = boundsOf(cluster);
        VanillaUiProtectedBounds chip = pauseTitleChipBounds(screen.width, screen.height, panel, clusterBounds, titleWidth);
        EchoThemeColors colors = theme.colors();
        int fill = VanillaUiColors.cappedAlpha(colors.background(), 96);
        int top = VanillaUiColors.cappedAlpha(colors.glow(), 24);
        int border = VanillaUiColors.cappedAlpha(colors.border(), 120);
        graphics.fill(chip.x(), chip.y(), chip.x() + chip.width(), chip.y() + chip.height(), fill);
        if (chip.width() > 8 && chip.height() > 6) {
            graphics.fill(chip.x() + 1, chip.y() + 1, chip.x() + chip.width() - 1,
                chip.y() + Math.min(chip.height() - 1, 5), top);
            graphics.fill(chip.x() + 6, chip.y() + 2,
                chip.x() + Math.min(chip.width() - 6, 54), chip.y() + 3,
                VanillaUiColors.cappedAlpha(colors.border(), 162));
        }
        EchoCyberGlassUi.calmFrame(graphics, chip.x(), chip.y(), chip.width(), chip.height(), border);
    }

    private static void drawPauseMenuPlate(GuiGraphicsExtractor graphics, EchoTheme theme, Identifier texture,
            int x, int y, int w, int h) {
        if (graphics == null || w <= 0 || h <= 0) {
            return;
        }
        EchoThemeColors colors = theme.colors();
        int fill = VanillaUiColors.cappedAlpha(colors.background(), 78);
        int topGlow = VanillaUiColors.cappedAlpha(colors.glow(), 18);
        int grid = VanillaUiColors.cappedAlpha(colors.borderSoft(), 16);
        int border = VanillaUiColors.cappedAlpha(colors.border(), 152);
        graphics.fill(x, y, x + w, y + h, fill);
        if (w > 8 && h > 8) {
            graphics.fill(x + 1, y + 1, x + w - 1, y + Math.min(y + h - 1, y + 16), topGlow);
            EchoCyberGlassUi.quietGrid(graphics, x + 2, y + 2, Math.max(0, w - 4), Math.max(0, h - 4),
                Math.max(18, Math.min(32, w / 10)), grid);
            drawPauseTextureAccents(graphics, texture, x, y, w, h);
            drawPauseMenuRails(graphics, colors, x, y, w, h);
        }
        EchoCyberGlassUi.calmFrame(graphics, x, y, w, h, border);
    }

    private static void drawPauseTextureAccents(GuiGraphicsExtractor graphics, Identifier texture, int x, int y, int w, int h) {
        if (graphics == null || texture == null || ThemeCoreConfig.vanillaSafeMode() || w <= 24 || h <= 12) {
            return;
        }
        int railH = Math.max(3, Math.min(10, h / 8));
        int railW = Math.max(24, Math.min(Math.max(24, w / 3), 128));
        int corner = Math.max(8, Math.min(18, Math.min(w, h) / 5));
        graphics.blit(texture, x + 4, y, x + Math.min(x + w - 4, x + 4 + railW), y + railH,
            0.08F, 0.40F, 0.04F, 0.12F);
        graphics.blit(texture, Math.max(x + 4, x + w - 4 - railW), y, x + w - 4, y + railH,
            0.60F, 0.92F, 0.04F, 0.12F);
        graphics.blit(texture, x, y, x + corner, y + corner,
            0.04F, 0.12F, 0.04F, 0.12F);
        graphics.blit(texture, x + w - corner, y, x + w, y + corner,
            0.88F, 0.96F, 0.04F, 0.12F);
    }

    private static void drawPauseMenuRails(GuiGraphicsExtractor graphics, EchoThemeColors colors, int x, int y, int w, int h) {
        int bright = VanillaUiColors.cappedAlpha(colors.border(), 176);
        int soft = VanillaUiColors.cappedAlpha(colors.borderSoft(), 78);
        int glow = VanillaUiColors.cappedAlpha(colors.glow(), 56);
        int topLong = Math.max(20, Math.min(w - 8, Math.max(32, w * 35 / 100)));
        int bottomLong = Math.max(18, Math.min(w - 8, Math.max(26, w * 28 / 100)));
        graphics.fill(x + 2, y + 2, x + 2 + topLong, y + 3, bright);
        graphics.fill(x + 2, y + h - 3, x + 2 + bottomLong, y + h - 2, soft);
        graphics.fill(x + 6, y + 5, x + Math.min(x + w - 6, x + 54), y + 6, glow);
        int sideEnd = Math.min(y + h - 4, y + Math.max(10, Math.min(42, h / 3)));
        if (sideEnd > y + 3) {
            graphics.fill(x + w - 4, y + 3, x + w - 3, sideEnd, soft);
        }
    }

    private static VanillaUiProtectedBounds pauseTitleChipBounds(int screenWidth, int screenHeight,
            VanillaUiProtectedBounds panel, VanillaUiProtectedBounds clusterBounds, int titleWidth) {
        int width = Math.max(92, Math.min(Math.max(92, panel.width() - 24), Math.max(1, titleWidth + 28)));
        int height = 18;
        int centerX = panel.x() + panel.width() / 2;
        int x = clamp(centerX - width / 2, 4, Math.max(4, screenWidth - width - 4));
        int titleDistance = Math.max(30, Math.min(190, screenHeight / 6));
        int y = clamp(clusterBounds.y() - titleDistance, 4, Math.max(4, panel.y() - height - 8));
        return new VanillaUiProtectedBounds(x, y, width, height);
    }

    private static boolean isSanePauseControl(int screenWidth, int screenHeight, int width, int height) {
        int maxWidth = Math.max(240, Math.min(screenWidth - 16, screenWidth / 2));
        int maxHeight = Math.max(24, Math.min(72, screenHeight / 8));
        return width > 0 && height > 0 && width <= maxWidth && height <= maxHeight;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void renderLoaderModsBackdrop(GuiGraphicsExtractor graphics, Screen screen, EchoTheme theme) {
        int w = screen.width;
        int h = screen.height;
        if (w <= 0 || h <= 0) {
            return;
        }
        EchoThemeColors colors = theme.colors();
        EchoThemeVanillaUiProfile vanilla = theme.vanillaUiProfile();
        int scrim = VanillaUiColors.cappedAlpha(vanilla.backgroundTint(), 118);
        int panel = VanillaUiColors.cappedAlpha(vanilla.panelTint(), 108);
        int soft = VanillaUiColors.cappedAlpha(colors.borderSoft(), 72);
        int glow = VanillaUiColors.cappedAlpha(colors.glow(), ThemeCoreConfig.bool(ThemeCoreConfig.VANILLA_EDGE_GLOW) ? 42 : 24);
        graphics.fill(0, 0, w, h, scrim);

        int margin = Math.max(8, Math.min(w, h) / 42);
        int top = Math.max(24, h / 24);
        int bottom = Math.max(top + 80, h - Math.max(28, h / 18));
        int railW = Math.min(Math.max(176, w / 4), Math.max(176, w - margin * 3 - 190));
        int detailX = margin * 2 + railW;
        int detailW = Math.max(96, w - detailX - margin);
        int headerH = Math.min(36, Math.max(20, h / 16));

        Identifier panelTexture = textureFor(theme, EchoThemeTextureKey.VANILLA_PANEL).orElse(null);
        drawDecorPlate(graphics, theme, panelTexture, margin, top + headerH, railW, Math.max(24, bottom - top - headerH));
        drawDecorPlate(graphics, theme, panelTexture, detailX, top, detailW, Math.max(24, bottom - top));
        graphics.fill(margin + 3, top + headerH + 3, margin + 6, bottom - 3, VanillaUiColors.cappedAlpha(colors.selection(), 82));
        graphics.fill(detailX + 3, top + 3, detailX + 6, bottom - 3, VanillaUiColors.cappedAlpha(colors.glow(), 70));
        graphics.fill(margin, top + headerH, margin + railW, top + headerH + 1, soft);
        graphics.fill(detailX, top, detailX + detailW, top + 1, soft);
        graphics.fill(margin, top + headerH - 4, margin + railW, top + headerH - 2, glow);
        graphics.fill(detailX, top + 2, detailX + detailW, top + 4, glow);
    }

    private static void renderLoaderModsAccents(GuiGraphicsExtractor graphics, Screen screen, EchoTheme theme) {
        int w = screen.width;
        int h = screen.height;
        if (w <= 0 || h <= 0) {
            return;
        }
        EchoThemeColors colors = theme.colors();
        int margin = Math.max(8, Math.min(w, h) / 42);
        int top = Math.max(24, h / 24);
        int bottom = Math.max(top + 80, h - Math.max(28, h / 18));
        int railW = Math.min(Math.max(176, w / 4), Math.max(176, w - margin * 3 - 190));
        int detailX = margin * 2 + railW;
        int detailW = Math.max(96, w - detailX - margin);
        int headerH = Math.min(36, Math.max(20, h / 16));
        int border = VanillaUiColors.cappedAlpha(colors.border(), 136);
        int selection = VanillaUiColors.cappedAlpha(colors.selection(), 112);
        int glow = VanillaUiColors.cappedAlpha(colors.glow(), 56);

        outlineIfPositive(graphics, margin - 1, top + headerH - 1, railW + 2, bottom - top - headerH + 2, border);
        outlineIfPositive(graphics, detailX - 1, top - 1, detailW + 2, bottom - top + 2, border);
        graphics.fill(Math.max(0, detailX - 5), top + 4, Math.max(0, detailX - 3), bottom - 4, glow);
        graphics.fill(Math.max(0, margin - 5), top + headerH + 4, Math.max(0, margin - 3), bottom - 4, selection);
    }

    private static void renderWidgetAccents(GuiGraphicsExtractor graphics, Screen screen, VanillaUiSurface surface,
            EchoTheme theme, int mouseX, int mouseY) {
        if (screen == null) {
            return;
        }
        int border = VanillaUiColors.cappedAlpha(theme.vanillaUiProfile().widgetAccent(), 86);
        int hover = VanillaUiColors.cappedAlpha(theme.colors().selection(), 118);
        List<VanillaUiProtectedBounds> pauseCluster = surface == VanillaUiSurface.PAUSE_MENU
            ? pauseButtonCluster(screen).orElse(List.of())
            : List.of();
        VanillaUiProtectedBounds pausePanel = pauseCluster.isEmpty()
            ? null
            : pausePanelBounds(screen.width, screen.height, pauseCluster);
        VanillaUiProtectedBounds pauseClusterBounds = pauseCluster.isEmpty() ? null : boundsOf(pauseCluster);
        for (GuiEventListener listener : screen.children()) {
            if (listener instanceof AbstractWidget widget) {
                if (listener instanceof Button button && shouldDrawThemedButtonSurface(surface)) {
                    renderThemedButtonChrome(graphics, button, theme, mouseX, mouseY);
                    continue;
                }
                VanillaUiProtectedBounds widgetBounds = new VanillaUiProtectedBounds(
                    widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight());
                if (!shouldDecorateWidgetAccent(surface, screen.width, screen.height, widgetBounds,
                        pausePanel, pauseClusterBounds)) {
                    continue;
                }
                int color = widget.isMouseOver(mouseX, mouseY) ? hover : border;
                outlineIfPositive(graphics, widget.getX() - 1, widget.getY() - 1, widget.getWidth() + 2, widget.getHeight() + 2, color);
            }
        }
    }

    private static boolean shouldDecorateWidgetAccent(VanillaUiSurface surface, int screenWidth, int screenHeight,
            VanillaUiProtectedBounds widget, VanillaUiProtectedBounds pausePanel, VanillaUiProtectedBounds pauseCluster) {
        if (widget == null || widget.width() <= 0 || widget.height() <= 0) {
            return false;
        }
        int maxWidth = Math.max(320, Math.min(screenWidth - 16, screenWidth / 2));
        int maxHeight = Math.max(32, Math.min(96, screenHeight / 6));
        if (widget.width() > maxWidth || widget.height() > maxHeight) {
            return false;
        }
        if (surface != VanillaUiSurface.PAUSE_MENU) {
            return true;
        }
        return pausePanel != null
            && pauseCluster != null
            && pausePanel.contains(widget.x(), widget.y())
            && pausePanel.contains(widget.x() + widget.width() - 1, widget.y() + widget.height() - 1)
            && boundsOverlap(widget, pauseCluster);
    }

    private static boolean boundsOverlap(VanillaUiProtectedBounds left, VanillaUiProtectedBounds right) {
        if (left == null || right == null || left.width() <= 0 || left.height() <= 0
                || right.width() <= 0 || right.height() <= 0) {
            return false;
        }
        return left.x() < right.x() + right.width()
            && left.x() + left.width() > right.x()
            && left.y() < right.y() + right.height()
            && left.y() + left.height() > right.y();
    }

    private static boolean shouldDrawThemedButtonSurface(VanillaUiSurface surface) {
        return switch (surface) {
            case MAIN_MENU, PAUSE_MENU, OPTIONS_MENU, WORLD_SELECT, MULTIPLAYER, RESOURCE_PACKS, SOCIAL, STATS, LOADER_MODS -> true;
            default -> false;
        };
    }

    public static VanillaUiProtectedBounds buttonChromeBoundsForTests(int x, int y, int width, int height) {
        return buttonChromeBounds(x, y, width, height);
    }

    public static VanillaUiProtectedBounds loadingPanelBoundsForTests(int screenWidth, int screenHeight) {
        return loadingPanelBounds(screenWidth, screenHeight);
    }

    private static VanillaUiProtectedBounds buttonChromeBounds(Button button) {
        return buttonChromeBounds(button.getX(), button.getY(), button.getWidth(), button.getHeight());
    }

    private static VanillaUiProtectedBounds buttonChromeBounds(int x, int y, int width, int height) {
        return new VanillaUiProtectedBounds(x, y, Math.max(0, width), Math.max(0, height));
    }

    private static void renderThemedButtonChrome(GuiGraphicsExtractor graphics, Button button, EchoTheme theme, int mouseX, int mouseY) {
        if (graphics == null || button == null || !button.visible || button.getWidth() <= 0 || button.getHeight() <= 0) {
            return;
        }
        EchoThemeColors colors = theme.colors();
        boolean hovered = button.isMouseOver(mouseX, mouseY) || button.isHoveredOrFocused();
        boolean active = button.active;
        VanillaUiProtectedBounds chrome = buttonChromeBounds(button);
        int x = chrome.x();
        int y = chrome.y();
        int w = chrome.width();
        int h = chrome.height();
        int accent = VanillaUiColors.cappedAlpha(colors.border(), active ? hovered ? 228 : 184 : 82);
        int secondary = VanillaUiColors.cappedAlpha(colors.selection(), active ? hovered ? 78 : 42 : 24);
        int base = VanillaUiColors.cappedAlpha(active ? colors.background() : colors.locked(),
            active ? hovered ? 202 : 184 : 178);
        int fill = VanillaUiColors.cappedAlpha(active ? colors.panelAlt() : colors.locked(),
            active ? hovered ? 178 : 154 : 150);
        int hoverWash = VanillaUiColors.cappedAlpha(colors.selection(), active && hovered ? 30 : 6);
        int rail = VanillaUiColors.cappedAlpha(colors.glow(), active ? hovered ? 172 : 84 : 34);
        int topGlow = VanillaUiColors.cappedAlpha(colors.glow(), active ? hovered ? 48 : 22 : 10);

        graphics.fill(x, y, x + w, y + h, base);
        if (w > 2 && h > 2) {
            graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
        }
        if (w > 8 && h > 5) {
            graphics.fill(x + 1, y + 1, x + w - 1, y + Math.min(y + h - 1, y + Math.max(3, h / 3)), topGlow);
            if (active) {
                graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, hoverWash);
            }
            graphics.fill(x + 2, y + h - 2, x + w - 2, y + h - 1, rail);
            graphics.fill(x + 2, y + 2, x + 4, y + h - 2, accent);
            graphics.fill(x + w - 3, y + 3, x + w - 2, y + h - 3, secondary);
            if (hovered && active) {
                int topStart = x + 7;
                int topEnd = Math.min(x + w - 7, x + 46);
                if (topEnd > topStart) {
                    graphics.fill(topStart, y + 2, topEnd, y + 3, VanillaUiColors.cappedAlpha(colors.text(), 96));
                }
                int bottomEnd = x + w - 7;
                int bottomStart = Math.max(x + 7, x + w - Math.min(Math.max(0, w - 7), 46));
                if (bottomEnd > bottomStart) {
                    graphics.fill(bottomStart, y + h - 3, bottomEnd, y + h - 2,
                        VanillaUiColors.cappedAlpha(colors.selection(), 82));
                }
            }
        }
        EchoCyberGlassUi.calmFrame(graphics, x, y, w, h, accent);
        renderThemedButtonLabel(graphics, button, colors, x, y, w, h, active, hovered);
    }

    private static void renderThemedButtonLabel(GuiGraphicsExtractor graphics, Button button, EchoThemeColors colors,
            int x, int y, int w, int h, boolean active, boolean hovered) {
        Font font = Minecraft.getInstance().font;
        if (font == null) {
            return;
        }
        Component label = fittedLabel(font, button.getMessage(), Math.max(1, w - 12));
        int labelWidth = font.width(label);
        int labelX = x + Math.max(4, (w - labelWidth) / 2);
        int labelY = y + Math.max(1, (h - 8) / 2);
        int text = active
            ? VanillaUiColors.cappedAlpha(hovered ? colors.text() : colors.primary(), 248)
            : VanillaUiColors.cappedAlpha(colors.mutedText(), 190);
        graphics.text(font, label, labelX, labelY, text, false);
    }

    private static Component fittedLabel(Font font, Component value, int width) {
        Component safe = value == null ? Component.empty() : value;
        if (font.width(safe) <= width) {
            return safe;
        }
        return Component.literal(trimmed(font, safe.getString(), width));
    }

    private static String trimmed(Font font, String value, int width) {
        String safe = value == null ? "" : value;
        if (width <= 0 || font.width(safe) <= width) {
            return width <= 0 ? "" : safe;
        }
        String suffix = "...";
        int suffixWidth = font.width(suffix);
        if (width <= suffixWidth) {
            return font.plainSubstrByWidth(safe, width);
        }
        return font.plainSubstrByWidth(safe, width - suffixWidth).stripTrailing() + suffix;
    }

    private static Optional<Identifier> textureFor(EchoTheme theme, EchoThemeTextureKey key) {
        Optional<Identifier> module = theme.moduleTexture(key);
        if (module.isPresent()) {
            return module;
        }
        Optional<Identifier> vanilla = theme.vanillaUiProfile().texture(key);
        if (vanilla.isPresent()) {
            return vanilla;
        }
        return theme.uiAssets().texture(key);
    }

    private static void renderLoadingPanel(GuiGraphicsExtractor graphics, EchoTheme theme, int screenWidth, int screenHeight) {
        VanillaUiProtectedBounds bounds = loadingPanelBounds(screenWidth, screenHeight);
        Optional<Identifier> texture = textureFor(theme, EchoThemeTextureKey.VANILLA_LOADING_PANEL)
            .or(() -> textureFor(theme, EchoThemeTextureKey.VANILLA_PAUSE_PANEL))
            .or(() -> textureFor(theme, EchoThemeTextureKey.VANILLA_PANEL));
        drawDecorPlate(graphics, theme, texture.orElse(null), bounds.x(), bounds.y(), bounds.width(), bounds.height(), true);
    }

    private static void renderLoadingPanelAccents(GuiGraphicsExtractor graphics, EchoTheme theme, int screenWidth, int screenHeight) {
        VanillaUiProtectedBounds bounds = loadingPanelBounds(screenWidth, screenHeight);
        EchoThemeColors colors = theme.colors();
        int x = bounds.x();
        int y = bounds.y();
        int w = bounds.width();
        int h = bounds.height();
        EchoCyberGlassUi.calmFrame(graphics, x, y, w, h, VanillaUiColors.cappedAlpha(colors.border(), 186));
        if (w > 48 && h > 32) {
            int glow = VanillaUiColors.cappedAlpha(colors.glow(), 76);
            int soft = VanillaUiColors.cappedAlpha(colors.borderSoft(), 74);
            graphics.fill(x + 16, y + 12, x + Math.min(x + w - 16, x + 190), y + 13, glow);
            graphics.fill(x + w - Math.min(w - 24, 220), y + h - 15, x + w - 18, y + h - 14, soft);
            graphics.fill(x + 12, y + h - 36, x + 13, y + h - 14, soft);
            graphics.fill(x + w - 14, y + 18, x + w - 13, y + Math.min(y + h - 18, y + 74), glow);
        }
    }

    private static VanillaUiProtectedBounds loadingPanelBounds(int screenWidth, int screenHeight) {
        int availableW = Math.max(1, screenWidth - 96);
        int availableH = Math.max(1, screenHeight - 96);
        int panelW = Math.min(availableW, 760);
        int panelH = Math.min(availableH, 300);
        panelW = Math.min(Math.max(360, panelW), Math.max(1, screenWidth - 32));
        panelH = Math.min(Math.max(180, panelH), Math.max(1, screenHeight - 32));
        int x = Math.max(16, (screenWidth - panelW) / 2);
        int y = Math.max(16, (screenHeight - panelH) / 2 - Math.max(0, Math.min(36, screenHeight / 24)));
        if (y + panelH > screenHeight - 32) {
            y = Math.max(16, screenHeight - 32 - panelH);
        }
        return new VanillaUiProtectedBounds(x, y, panelW, panelH);
    }

    private static EchoCyberGlassUi.Surface backdropSurface(VanillaUiSurface surface) {
        return switch (surface) {
            case INVENTORY, CREATIVE_INVENTORY, CONTAINER, FURNACE, CRAFTING, ANVIL, ENCHANTING, GRINDSTONE, SMITHING ->
                    EchoCyberGlassUi.Surface.CONTAINER;
            case ECHO_SCREEN, LOADER_MODS -> EchoCyberGlassUi.Surface.ECHO_APP;
            case PAUSE_MENU -> EchoCyberGlassUi.Surface.OVERLAY_DRAWER;
            default -> EchoCyberGlassUi.Surface.MENU;
        };
    }

    private static void blitStretched(GuiGraphicsExtractor graphics, Identifier texture, int x, int y, int w, int h) {
        if (graphics == null || texture == null || w <= 0 || h <= 0) {
            return;
        }
        if (!ThemeCoreConfig.vanillaSafeMode()) {
            graphics.blit(texture, x, y, x + w, y + h, 0.0F, 1.0F, 0.0F, 1.0F);
        }
        EchoCyberGlassUi.calmFrame(graphics, x, y, w, h,
            VanillaUiColors.cappedAlpha(ClientThemeState.currentTheme().colors().border(), 112));
    }

    private static void drawDecorPlate(GuiGraphicsExtractor graphics, EchoTheme theme, int x, int y, int w, int h) {
        drawDecorPlate(graphics, theme, null, x, y, w, h, false);
    }

    private static void drawDecorPlate(GuiGraphicsExtractor graphics, EchoTheme theme, Identifier texture, int x, int y, int w, int h) {
        drawDecorPlate(graphics, theme, texture, x, y, w, h, false);
    }

    private static void drawDecorPlate(GuiGraphicsExtractor graphics, EchoTheme theme, Identifier texture,
            int x, int y, int w, int h, boolean compact) {
        if (graphics == null || w <= 0 || h <= 0) {
            return;
        }
        EchoThemeColors colors = theme.colors();
        int panel = VanillaUiColors.cappedAlpha(compact ? colors.background() : theme.vanillaUiProfile().panelTint(),
            compact ? 88 : 74);
        int border = VanillaUiColors.cappedAlpha(colors.border(), compact ? 154 : 104);
        graphics.fill(x, y, x + w, y + h, panel);
        if (texture != null && !ThemeCoreConfig.vanillaSafeMode()) {
            if (compact) {
                drawTextureRails(graphics, texture, x, y, w, h, true);
            } else {
                graphics.blit(texture, x, y, x + w, y + h, 0.0F, 1.0F, 0.0F, 1.0F);
            }
        }
        if (w > 8 && h > 8) {
            graphics.fill(x + 1, y + 1, x + w - 1, y + Math.min(y + h - 1, y + 20),
                VanillaUiColors.cappedAlpha(colors.glow(), compact ? 22 : 22));
            EchoCyberGlassUi.quietGrid(graphics, x + 2, y + 2, Math.max(0, w - 4), Math.max(0, h - 4),
                Math.max(18, Math.min(32, w / 10)), VanillaUiColors.cappedAlpha(colors.borderSoft(), compact ? 18 : 16));
            if (compact) {
                drawCompactRails(graphics, colors, x, y, w, h);
            }
        }
        EchoCyberGlassUi.calmFrame(graphics, x, y, w, h, border);
    }

    private static void drawTextureRails(GuiGraphicsExtractor graphics, Identifier texture, int x, int y, int w, int h) {
        drawTextureRails(graphics, texture, x, y, w, h, false);
    }

    private static void drawTextureRails(GuiGraphicsExtractor graphics, Identifier texture, int x, int y, int w, int h,
            boolean compact) {
        if (graphics == null || texture == null || w <= 0 || h <= 0) {
            return;
        }
        int top = compact ? Math.max(3, Math.min(10, h / 8)) : Math.max(4, Math.min(18, h / 5));
        int bottom = compact ? Math.max(3, Math.min(8, h / 10)) : Math.max(4, Math.min(18, h / 5));
        int side = compact ? Math.max(3, Math.min(7, w / 24)) : Math.max(4, Math.min(14, w / 12));
        if (w > 8 && top > 0) {
            graphics.blit(texture, x, y, x + w, y + top, 0.08F, 0.92F, 0.04F, compact ? 0.12F : 0.18F);
        }
        if (w > 8 && bottom > 0) {
            graphics.blit(texture, x, y + h - bottom, x + w, y + h, 0.08F, 0.92F, compact ? 0.88F : 0.82F, 0.96F);
        }
        int middleTop = y + top;
        int middleBottom = y + h - bottom;
        if (middleBottom > middleTop && side > 0) {
            float v0 = compact ? 0.22F : 0.18F;
            float v1 = compact ? 0.78F : 0.82F;
            graphics.blit(texture, x, middleTop, x + side, middleBottom, 0.04F, compact ? 0.10F : 0.16F, v0, v1);
            graphics.blit(texture, x + w - side, middleTop, x + w, middleBottom, compact ? 0.90F : 0.84F, 0.96F, v0, v1);
        }
    }

    private static void drawCompactRails(GuiGraphicsExtractor graphics, EchoThemeColors colors, int x, int y, int w, int h) {
        int bright = VanillaUiColors.cappedAlpha(colors.border(), 184);
        int soft = VanillaUiColors.cappedAlpha(colors.borderSoft(), 86);
        int glow = VanillaUiColors.cappedAlpha(colors.glow(), 62);
        int topLong = Math.max(8, Math.min(w - 4, Math.max(30, w / 3)));
        int bottomLong = Math.max(8, Math.min(w - 4, Math.max(22, w / 4)));
        int sideEnd = Math.min(y + h - 4, y + Math.max(12, h / 3));
        graphics.fill(x + 2, y + 2, x + 2 + topLong, y + 3, bright);
        graphics.fill(x + 2, y + h - 3, x + 2 + bottomLong, y + h - 2, soft);
        if (sideEnd > y + 3) {
            graphics.fill(x + w - 4, y + 3, x + w - 3, sideEnd, soft);
        }
        graphics.fill(x + 6, y + 5, x + Math.min(x + w - 6, x + 58), y + 6, glow);
    }

    private static void outlineIfPositive(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int color) {
        if (graphics == null || w <= 0 || h <= 0) {
            return;
        }
        graphics.outline(x, y, w, h, color);
    }

    private record CyberGlassToast(Toast delegate) implements Toast {
        @Override
        public Visibility getWantedVisibility() {
            return delegate.getWantedVisibility();
        }

        @Override
        public void update(ToastManager manager, long fullyVisibleForMs) {
            delegate.update(manager, fullyVisibleForMs);
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, Font font, long fullyVisibleForMs) {
            if (width() <= 0 || height() <= 0) {
                delegate.extractRenderState(graphics, font, fullyVisibleForMs);
                return;
            }
            EchoCyberGlassUi.panel(graphics, 0, 0, width(), height());
            graphics.fill(0, 0, 3, height(), EchoCyberGlassUi.palette().accentSecondary());
            delegate.extractRenderState(graphics, font, fullyVisibleForMs);
        }

        @Override
        public net.minecraft.sounds.SoundEvent getSoundEvent() {
            return delegate.getSoundEvent();
        }

        @Override
        public Object getToken() {
            return delegate.getToken();
        }

        @Override
        public float xPos(int screenWidth, float visiblePortion) {
            return delegate.xPos(screenWidth, visiblePortion);
        }

        @Override
        public float yPos(int firstSlotIndex) {
            return delegate.yPos(firstSlotIndex);
        }

        @Override
        public int width() {
            return delegate.width();
        }

        @Override
        public int height() {
            return delegate.height();
        }

        @Override
        public int occcupiedSlotCount() {
            return delegate.occcupiedSlotCount();
        }

        @Override
        public void onFinishedRendering() {
            delegate.onFinishedRendering();
        }
    }
}
