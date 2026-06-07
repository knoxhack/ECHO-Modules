package com.knoxhack.echoterminal.client.screen;

import com.knoxhack.echoterminal.BuiltinTerminalCommonIntegration;
import com.knoxhack.echoterminal.EchoTerminal;
import com.knoxhack.echoterminal.api.ClientTerminalTab;
import com.knoxhack.echoterminal.api.TerminalRenderCache;
import com.knoxhack.echoterminal.api.TerminalRenderContext;
import com.knoxhack.echoterminal.api.TerminalTab;
import com.knoxhack.echoterminal.api.TerminalTabRegistry;
import com.knoxhack.echoterminal.api.theme.TerminalThemeContext;
import com.knoxhack.echoterminal.client.BuiltinTerminalTabs;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class EchoNativeTerminalScreen extends Screen {
    private static final int BG = 0xF002070C;
    private static final int PANEL = 0xDD06131A;
    private static final int ROW = 0x66112632;
    private static final int CYAN = 0xFF38DFF4;
    private static final int TEXT = 0xFFEAFBFF;
    private static final int MUTED = 0xFF7BA6B2;
    private static final int ERROR = 0xFFFF6F6F;
    private static final AtomicBoolean BOOTSTRAPPED = new AtomicBoolean(false);
    private static volatile String bootstrapWarning = "";
    private int selected;

    public EchoNativeTerminalScreen() {
        super(Component.translatable("screen.echoterminal.native_terminal"));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        TerminalRenderCache.beginFrame();
        ensureTerminalTabsReady();
        Font font = Minecraft.getInstance().font;
        List<TerminalTab> tabs = TerminalTabRegistry.tabs();
        if (selected >= tabs.size()) {
            selected = Math.max(0, tabs.size() - 1);
        }

        graphics.fill(0, 0, width, height, BG);
        int panelW = Math.min(width - 32, 860);
        int panelH = Math.min(height - 32, 520);
        int panelX = (width - panelW) / 2;
        int panelY = (height - panelH) / 2;
        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, PANEL);
        graphics.fill(panelX, panelY, panelX + panelW, panelY + 2, CYAN);
        graphics.text(font, "ECHO TERMINAL // NATIVE", panelX + 14, panelY + 12, CYAN, false);
        graphics.text(font, "M or Esc closes / Up Down selects", panelX + panelW - 190, panelY + 12, MUTED, false);

        int railX = panelX + 12;
        int railY = panelY + 42;
        int railW = Math.min(260, panelW / 3);
        int contentX = railX + railW + 12;
        int contentW = panelX + panelW - contentX - 12;
        graphics.fill(railX, railY, railX + railW, panelY + panelH - 12, 0xAA051018);
        graphics.fill(contentX, railY, contentX + contentW, panelY + panelH - 12, 0x8808121A);

        if (tabs.isEmpty()) {
            graphics.text(font, "No terminal tabs registered yet.", railX + 10, railY + 12, MUTED, false);
            if (!bootstrapWarning.isBlank()) {
                graphics.text(font, bootstrapWarning, railX + 10, railY + 28, ERROR, false);
            }
            return;
        }

        int y = railY + 8;
        for (int index = 0; index < tabs.size() && y < panelY + panelH - 28; index++) {
            TerminalTab tab = tabs.get(index);
            boolean active = index == selected;
            graphics.fill(railX + 6, y, railX + railW - 6, y + 18, active ? 0xAA123646 : ROW);
            graphics.text(font, trim(font, tab.descriptor().title(), railW - 24), railX + 12, y + 5,
                    active ? CYAN : TEXT, false);
            y += 21;
        }

        TerminalTab activeTab = tabs.get(selected);
        int cy = railY + 14;
        graphics.text(font, activeTab.descriptor().title(), contentX + 14, cy, CYAN, false);
        cy += 20;
        if (!bootstrapWarning.isBlank()) {
            graphics.text(font, bootstrapWarning, contentX + 14, cy, ERROR, false);
            cy += 18;
        }
        ClientTerminalTab clientTab = clientTab(activeTab);
        if (clientTab == null) {
            graphics.text(font, activeTab.descriptor().id().toString(), contentX + 14, cy, TEXT, false);
            cy += 26;
            graphics.text(font, "This terminal tab does not expose a client renderer yet.", contentX + 14, cy,
                    MUTED, false);
            return;
        }
        try {
            clientTab.render(contextFor(activeTab, contentX + 12, cy, contentW - 24,
                    panelY + panelH - cy - 24), graphics, mouseX, mouseY, partialTick);
        } catch (RuntimeException | LinkageError exception) {
            drawNativeSafeTabFallback(graphics, font, activeTab, contentX + 14, cy, contentW - 28);
            EchoTerminal.LOGGER.warn("ECHO Terminal native fallback tab render failed for {}.",
                    activeTab.descriptor().id(), exception);
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        TerminalRenderCache.beginFrame();
        ensureTerminalTabsReady();
        List<TerminalTab> tabs = TerminalTabRegistry.tabs();
        TerminalTab tab = selected >= 0 && selected < tabs.size() ? tabs.get(selected) : null;
        ClientTerminalTab clientTab = clientTab(tab);
        if (clientTab != null && clientTab.keyPressed(contextFor(tab), event)) {
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_ESCAPE || event.key() == GLFW.GLFW_KEY_M) {
            onClose();
            return true;
        }
        int size = tabs.size();
        if (size > 0 && event.key() == GLFW.GLFW_KEY_DOWN) {
            select(Math.min(size - 1, selected + 1), tabs);
            return true;
        }
        if (size > 0 && event.key() == GLFW.GLFW_KEY_UP) {
            select(Math.max(0, selected - 1), tabs);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        List<TerminalTab> tabs = TerminalTabRegistry.tabs();
        TerminalTab tab = selected >= 0 && selected < tabs.size() ? tabs.get(selected) : null;
        ClientTerminalTab clientTab = clientTab(tab);
        return clientTab != null && clientTab.charTyped(contextFor(tab), event);
    }

    private static void ensureTerminalTabsReady() {
        if (!BOOTSTRAPPED.compareAndSet(false, true)) {
            return;
        }
        try {
            TerminalClientOptions.load();
            BuiltinTerminalCommonIntegration.register();
            BuiltinTerminalTabs.register();
            TerminalTabRegistry.ensureSorted();
            bootstrapWarning = "";
        } catch (RuntimeException | LinkageError exception) {
            bootstrapWarning = "Terminal tab bootstrap failed: " + exception.getClass().getSimpleName();
            BOOTSTRAPPED.set(false);
            EchoTerminal.LOGGER.warn("ECHO Terminal native fallback could not bootstrap built-in tabs.", exception);
        }
    }

    private TerminalRenderContext contextFor(TerminalTab tab) {
        return contextFor(tab, 0, 0, Math.max(80, width), Math.max(80, height));
    }

    private TerminalRenderContext contextFor(TerminalTab tab, int contentX, int contentY, int contentW, int contentH) {
        Minecraft minecraft = Minecraft.getInstance();
        Identifier tabId = tab == null ? null : tab.descriptor().id();
        String group = tab == null ? "SYSTEMS" : tab.chrome().group();
        TerminalThemeContext themeContext = new TerminalThemeContext(
                tabId,
                group,
                group.toLowerCase(),
                group,
                tabId == null ? EchoTerminal.MODID : tabId.getNamespace(),
                0,
                TerminalClientOptions.useVisualAssets(),
                TerminalClientOptions.reduceMotion());
        return new TerminalRenderContext(
                minecraft,
                minecraft.player,
                width,
                height,
                contentX,
                contentY,
                Math.max(80, contentW),
                Math.max(80, contentH),
                0,
                this::selectTabById,
                this::hasTab,
                TerminalClientOptions.currentTheme(),
                themeContext);
    }

    private boolean hasTab(Identifier tabId) {
        return tabId != null && TerminalTabRegistry.tabs().stream()
                .anyMatch(tab -> tab.descriptor().id().equals(tabId));
    }

    private void selectTabById(Identifier tabId) {
        List<TerminalTab> tabs = TerminalTabRegistry.tabs();
        for (int index = 0; index < tabs.size(); index++) {
            if (tabs.get(index).descriptor().id().equals(tabId)) {
                select(index, tabs);
                return;
            }
        }
    }

    private void select(int index, List<TerminalTab> tabs) {
        if (tabs.isEmpty()) {
            selected = 0;
            return;
        }
        selected = Math.max(0, Math.min(index, tabs.size() - 1));
        ClientTerminalTab clientTab = clientTab(tabs.get(selected));
        if (clientTab != null) {
            clientTab.onSelected(contextFor(tabs.get(selected)));
        }
    }

    private static ClientTerminalTab clientTab(TerminalTab tab) {
        return tab instanceof ClientTerminalTab clientTab ? clientTab : null;
    }

    private static void drawNativeSafeTabFallback(
            GuiGraphicsExtractor graphics,
            Font font,
            TerminalTab tab,
            int x,
            int y,
            int width
    ) {
        String summary = tab.chrome().summary();
        graphics.text(font, "Native-safe view", x, y, CYAN, false);
        int cy = y + 18;
        if (!summary.isBlank()) {
            graphics.text(font, trim(font, summary, width), x, cy, TEXT, false);
            cy += 18;
        }
        graphics.text(font, "Route: " + tab.descriptor().id(), x, cy, MUTED, false);
        cy += 26;
        String id = tab.descriptor().id().toString();
        if ("echoterminal:overview".equals(id)) {
            graphics.text(font, "Command Deck is online under the Native Loader.", x, cy, TEXT, false);
            cy += 16;
            graphics.text(font, "Use Up/Down to inspect Terminal sections while the remaining data APIs are ported.",
                    x, cy, MUTED, false);
        } else if ("echoterminal:addons".equals(id)) {
            graphics.text(font, "Installed module routing is active.", x, cy, TEXT, false);
            cy += 16;
            graphics.text(font, "Full module detail will light up after native attachment/save-data parity lands.",
                    x, cy, MUTED, false);
        } else if ("echoterminal:recipe_index".equals(id)) {
            graphics.text(font, "Recipe Index is available as its own native Index surface.", x, cy, TEXT, false);
            cy += 16;
            graphics.text(font, "Press G/R/U outside this terminal for the current native Index route.",
                    x, cy, MUTED, false);
        } else {
            graphics.text(font, "This tab still depends on a backend-only data path.", x, cy, TEXT, false);
            cy += 16;
            graphics.text(font, "The native terminal shell is keeping the session open while that tab is ported.",
                    x, cy, MUTED, false);
        }
    }

    private static String trim(Font font, String value, int maxWidth) {
        if (font.width(value) <= maxWidth) {
            return value;
        }
        String ellipsis = "...";
        int limit = Math.max(0, maxWidth - font.width(ellipsis));
        String result = value;
        while (!result.isEmpty() && font.width(result) > limit) {
            result = result.substring(0, result.length() - 1);
        }
        return result + ellipsis;
    }
}
