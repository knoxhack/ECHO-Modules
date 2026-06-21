package com.knoxhack.echoterminal.client.screencore;

import com.knoxhack.echoscreencore.api.EchoDataContext;
import com.knoxhack.echoscreencore.api.action.EchoActionContext;
import com.knoxhack.echoscreencore.api.theme.EchoAccessibilitySettings;
import com.knoxhack.echoscreencore.client.api.EchoFitScreenSurface;
import com.knoxhack.echoterminal.EchoTerminalClient;
import com.knoxhack.echoterminal.client.screen.EchoTerminalScreen;
import com.knoxhack.echoterminal.client.screen.TerminalClientOptions;
import com.knoxhack.echoterminal.menu.EchoTerminalMenu;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public final class TerminalScreenCoreScreen extends AbstractContainerScreen<EchoTerminalMenu> {
    private final EchoTerminalMenu terminalMenu;
    private final Inventory playerInventory;
    private final Component screenTitle;
    private Identifier activeTabId;
    private Identifier pageId;
    private EchoFitScreenSurface surface;

    public TerminalScreenCoreScreen(EchoTerminalMenu menu, Inventory playerInventory, Component title, Identifier activeTabId) {
        super(menu, playerInventory, title);
        this.terminalMenu = menu;
        this.playerInventory = playerInventory;
        this.screenTitle = title;
        this.activeTabId = TerminalScreenCoreBridge.normalizeTab(activeTabId);
        rebuildEngine();
        TerminalScreenCoreBridge.markActive(this);
    }

    Identifier pageId() {
        return pageId;
    }

    EchoTerminalMenu terminalMenu() {
        return terminalMenu;
    }

    Inventory playerInventory() {
        return playerInventory;
    }

    Component screenTitle() {
        return screenTitle;
    }

    void markDataDirty() {
        if (surface != null) {
            surface.markDataDirty();
        }
    }

    boolean openLegacyRenderer() {
        if (EchoTerminalClient.nativeLoaderClientActiveForScreens()) {
            EchoTerminalClient.publishNativeScreenLifecycle(
                    "open",
                    "terminal.screencore.open_legacy_renderer",
                    getClass().getName(),
                    Map.of(
                            "targetScreenClass", EchoTerminalScreen.class.getName(),
                            "transitionSource", "terminal_screencore_legacy_renderer",
                            "screenBridge", "classic_terminal"
                    ));
        }
        Minecraft.getInstance().setScreen(new EchoTerminalScreen(terminalMenu, playerInventory, screenTitle));
        return true;
    }

    @Override
    public void removed() {
        TerminalScreenCoreBridge.clearActive(this);
        super.removed();
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (surface == null) {
            rebuildEngine();
        }
        surface.render(graphics, 0, 0, width, height, mouseX, mouseY, partialTick);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (EchoTerminalClient.nativeLoaderClientActiveForScreens()
                && EchoTerminalClient.dispatchNativeScreenCoreMouse(
                        this, "click", event.x(), event.y(), event.button(), 0.0D, 0.0D)) {
            return true;
        }
        return handleNativeRouteMouse("click", event.x(), event.y(), event.button(), 0.0D, 0.0D)
                || super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (EchoTerminalClient.nativeLoaderClientActiveForScreens()
                && EchoTerminalClient.dispatchNativeScreenCoreMouse(
                        this, "release", event.x(), event.y(), event.button(), 0.0D, 0.0D)) {
            return true;
        }
        return handleNativeRouteMouse("release", event.x(), event.y(), event.button(), 0.0D, 0.0D)
                || super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (EchoTerminalClient.nativeLoaderClientActiveForScreens()
                && EchoTerminalClient.dispatchNativeScreenCoreMouse(
                        this, "drag", event.x(), event.y(), event.button(), dragX, dragY)) {
            return true;
        }
        return handleNativeRouteMouse("drag", event.x(), event.y(), event.button(), dragX, dragY)
                || super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (EchoTerminalClient.nativeLoaderClientActiveForScreens()
                && EchoTerminalClient.dispatchNativeScreenCoreScroll(this, mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }
        return handleNativeRouteScroll(mouseX, mouseY, scrollX, scrollY)
                || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        boolean openTerminalKey = EchoTerminalClient.OPEN_TERMINAL_KEY.matches(event);
        if (EchoTerminalClient.nativeLoaderClientActiveForScreens()
                && EchoTerminalClient.dispatchNativeScreenCoreKey(this, event.key(), openTerminalKey)) {
            return true;
        }
        return handleNativeRouteKey(event.key(), openTerminalKey) || super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        boolean allowed = event != null && event.isAllowedChatCharacter();
        String character = event == null ? "" : event.codepointAsString();
        if (EchoTerminalClient.nativeLoaderClientActiveForScreens()
                && EchoTerminalClient.dispatchNativeScreenCoreChar(this, character, allowed)) {
            return true;
        }
        return handleNativeRouteChar(character, allowed) || super.charTyped(event);
    }

    public boolean handleNativeRouteMouse(
            String phase,
            double mouseX,
            double mouseY,
            int button,
            double dragX,
            double dragY
    ) {
        if (surface == null) {
            return false;
        }
        return switch (phase) {
            case "release" -> surface.mouseReleased(mouseX, mouseY, button);
            case "drag" -> surface.mouseDragged(mouseX, mouseY, button, dragX, dragY);
            default -> surface.mouseClicked(mouseX, mouseY, button);
        };
    }

    public boolean handleNativeRouteScroll(double mouseX, double mouseY, double scrollX, double scrollY) {
        return surface != null && surface.mouseScrolled(mouseX, mouseY, scrollY);
    }

    public boolean handleNativeRouteKey(int key, boolean openTerminalKey) {
        if (openTerminalKey) {
            if (EchoTerminalClient.nativeLoaderClientActiveForScreens()) {
                EchoTerminalClient.publishNativeScreenLifecycle(
                        "close",
                        "terminal.screencore.close",
                        getClass().getName(),
                        Map.of(
                                "transitionSource", "terminal_screencore_key",
                                "closeKey", key
                        ));
            }
            Minecraft.getInstance().setScreen(null);
            return true;
        }
        return surface != null && surface.keyPressed(key);
    }

    public boolean handleNativeRouteChar(String character, boolean allowedChatCharacter) {
        return allowedChatCharacter && surface != null && surface.charTyped(character);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void rebuildEngine() {
        activeTabId = TerminalScreenCoreBridge.normalizeTab(activeTabId);
        pageId = TerminalScreenCoreBridge.pageForTab(activeTabId);
        surface = new EchoFitScreenSurface(pageId, TerminalScreenCoreBridge.screenContext(activeTabId),
                accessibility(), new Controls());
        surface.setDebug(TerminalClientOptions.screenCoreDebug());
    }

    private EchoAccessibilitySettings accessibility() {
        return new EchoAccessibilitySettings(
                TerminalClientOptions.largeTextMode(),
                TerminalClientOptions.highContrastMode(),
                TerminalClientOptions.reducedClutterMode(),
                TerminalClientOptions.reduceGlow(),
                TerminalClientOptions.interfaceDensity() == TerminalClientOptions.InterfaceDensity.COMPACT,
                TerminalClientOptions.interfaceDensity() == TerminalClientOptions.InterfaceDensity.COMFORTABLE,
                TerminalClientOptions.hideDebugInfo(),
                TerminalClientOptions.simplifiedTerminalMode());
    }

    private final class Controls implements EchoActionContext.ScreenControls {
        @Override
        public boolean close() {
            if (EchoTerminalClient.nativeLoaderClientActiveForScreens()) {
                EchoTerminalClient.publishNativeScreenLifecycle(
                        "close",
                        "terminal.screencore.close",
                        TerminalScreenCoreScreen.this.getClass().getName(),
                        Map.of("transitionSource", "terminal_screencore_controls_close"));
            }
            Minecraft.getInstance().setScreen(null);
            return true;
        }

        @Override
        public boolean back() {
            if (EchoTerminalClient.nativeLoaderClientActiveForScreens()) {
                EchoTerminalClient.publishNativeScreenLifecycle(
                        "close",
                        "terminal.screencore.back",
                        TerminalScreenCoreScreen.this.getClass().getName(),
                        Map.of("transitionSource", "terminal_screencore_controls_back"));
            }
            Minecraft.getInstance().setScreen(null);
            return true;
        }

        @Override
        public boolean open(Identifier nextPage, EchoDataContext context) {
            Identifier nextTab = activeTabFromContext(context, nextPage);
            if (EchoTerminalClient.nativeLoaderClientActiveForScreens()) {
                EchoTerminalClient.publishNativeScreenLifecycle(
                        "open",
                        "terminal.screencore.open_page",
                        TerminalScreenCoreScreen.this.getClass().getName(),
                        Map.of(
                                "targetScreenClass", TerminalScreenCoreScreen.class.getName(),
                                "transitionSource", "terminal_screencore_controls_open",
                                "nextPage", nextPage == null ? "" : nextPage.toString(),
                                "nextTab", nextTab == null ? "" : nextTab.toString()
                        ));
            }
            activeTabId = TerminalScreenCoreBridge.normalizeTab(nextTab);
            rebuildEngine();
            return true;
        }

        @Override
        public boolean toggleDebug() {
            TerminalClientOptions.setScreenCoreDebug(!TerminalClientOptions.screenCoreDebug());
            rebuildEngine();
            return true;
        }

        private Identifier activeTabFromContext(EchoDataContext context, Identifier nextPage) {
            if (context != null) {
                String raw = context.resolveToString("terminal.activeTabId");
                Identifier parsed = Identifier.tryParse(raw);
                if (parsed != null) {
                    return parsed;
                }
            }
            if (nextPage != null) {
                for (TerminalTabPageCandidate candidate : TerminalScreenCoreDataProviders.pageCandidates()) {
                    if (nextPage.equals(candidate.pageId())) {
                        return candidate.tabId();
                    }
                }
            }
            return activeTabId;
        }
    }
}
