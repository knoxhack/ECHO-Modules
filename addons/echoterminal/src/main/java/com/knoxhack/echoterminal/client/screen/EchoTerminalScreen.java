package com.knoxhack.echoterminal.client.screen;

import com.knoxhack.echoterminal.BuiltinTerminalCommonIntegration;
import com.knoxhack.echoterminal.EchoTerminal;
import com.knoxhack.echoterminal.EchoTerminalClient;
import com.knoxhack.echoterminal.api.ClientTerminalTab;
import com.knoxhack.echoterminal.api.TerminalDesignTokens;
import com.knoxhack.echoterminal.api.TerminalRenderCache;
import com.knoxhack.echoterminal.api.TerminalRenderContext;
import com.knoxhack.echoterminal.api.TerminalThemedSounds;
import com.knoxhack.echoterminal.api.TerminalIcon;
import com.knoxhack.echoterminal.api.TerminalLayoutProfile;
import com.knoxhack.echoterminal.api.TerminalNavigationSection;
import com.knoxhack.echoterminal.api.TerminalTab;
import com.knoxhack.echoterminal.api.TerminalTabChrome;
import com.knoxhack.echoterminal.api.TerminalTabRegistry;
import com.knoxhack.echoterminal.api.TerminalUi;
import com.knoxhack.echoterminal.api.theme.TerminalIconKey;
import com.knoxhack.echoterminal.api.theme.TerminalThemeContext;
import com.knoxhack.echoterminal.client.BuiltinTerminalTabs;
import com.knoxhack.echoterminal.menu.EchoTerminalMenu;
import dev.echo.nativeplatform.contracts.EchoNativeLoadStatus;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;

@Deprecated
public class EchoTerminalScreen extends AbstractContainerScreen<EchoTerminalMenu> {
    private static final Identifier OVERVIEW_TAB =
            Identifier.fromNamespaceAndPath("echoterminal", "overview");
    private static final String CHAPTER_PROGRESS_LABEL = "Chapter Progress";
    private static final AtomicBoolean BOOTSTRAPPED = new AtomicBoolean(false);
    private static Identifier rememberedTabId;
    private static volatile String bootstrapWarning = "";

    private enum ScrollbarDragTarget {
        NONE,
        COMMAND_STACK,
        CONTENT
    }

    private record PageTabChoice(int index, String label, String summary, int accentColor) {
    }

    private final TerminalScreenTheme theme;
    private final Map<Identifier, Integer> tabScroll = new HashMap<>();

    private List<TerminalTab> cachedTabs = List.of();
    private TerminalNavigationModel navigationModel = TerminalNavigationModel.of(List.of());
    private int activeTab;
    private boolean initialTabSelected;
    private int ticks;
    private TerminalLayoutProfile layoutProfile = TerminalLayoutProfile.MEDIUM_CAROUSEL;
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int contentX;
    private int contentY;
    private int contentW;
    private int contentH;
    private int navX;
    private int navY;
    private int navW;
    private int navH;
    private int groupRailX;
    private int groupRailY;
    private int groupRailW;
    private int groupRailH;
    private int pageRailX;
    private int pageRailY;
    private int pageRailW;
    private int pageRailH;
    private boolean commandStackNavigation;
    private boolean sidebarNavigation;
    private boolean commandStackCollapsed;
    private boolean chapterProgressExpanded;
    private int commandStackScroll;
    private String commandStackScrollGroup = "";
    private ScrollbarDragTarget scrollbarDragTarget = ScrollbarDragTarget.NONE;
    private int scrollbarDragOffset;
    private int collapseToggleX;
    private int collapseToggleY;
    private int collapseToggleW;
    private int collapseToggleH;

    public record LayoutMetrics(
            int panelX,
            int panelY,
            int panelW,
            int panelH,
            int contentX,
            int contentY,
            int contentW,
            int contentH,
            int renderContentX,
            int renderContentY,
            int renderContentW,
            int renderContentH,
            int groupRailX,
            int groupRailY,
            int groupRailW,
            int groupRailH,
            int collapseToggleX,
            int collapseToggleY,
            int collapseToggleW,
            int collapseToggleH,
            int shellHeaderH,
            int shellFooterH,
            TerminalLayoutProfile layoutProfile) {
    }

    public EchoTerminalScreen(EchoTerminalMenu menu, Inventory playerInventory, Component title) {
        this(menu, playerInventory, title, TerminalScreenTheme.modular());
    }

    public EchoTerminalScreen(EchoTerminalMenu menu, Inventory playerInventory, Component title, TerminalScreenTheme theme) {
        super(menu, playerInventory, title);
        ensureTerminalTabsReady();
        this.theme = theme == null ? TerminalScreenTheme.modular() : theme;
    }

    private static void ensureTerminalTabsReady() {
        if (BOOTSTRAPPED.get() && !TerminalTabRegistry.tabs().isEmpty()) {
            return;
        }
        synchronized (EchoTerminalScreen.class) {
            if (BOOTSTRAPPED.get() && !TerminalTabRegistry.tabs().isEmpty()) {
                return;
            }
            BOOTSTRAPPED.set(true);
            try {
                TerminalClientOptions.load();
                BuiltinTerminalCommonIntegration.register();
                BuiltinTerminalTabs.register();
                TerminalTabRegistry.ensureSorted();
                bootstrapWarning = "";
            } catch (RuntimeException | LinkageError exception) {
                bootstrapWarning = "Terminal tab bootstrap failed: " + exception.getClass().getSimpleName();
                BOOTSTRAPPED.set(false);
                EchoTerminal.LOGGER.warn("ECHO Terminal screen could not bootstrap built-in tabs.", exception);
            }
        }
    }

    public static LayoutMetrics layoutMetricsForTests(
            int screenWidth,
            int screenHeight,
            TerminalScreenTheme theme,
            TerminalClientOptions.InterfaceDensity density,
            TerminalClientOptions.TerminalZoom zoom,
            boolean commandStackCollapsed) {
        return layoutMetricsForTests(screenWidth, screenHeight, theme, density, zoom,
                commandStackCollapsed, TerminalClientOptions.NavigationStyle.APP_HUB);
    }

    public static LayoutMetrics layoutMetricsForTests(
            int screenWidth,
            int screenHeight,
            TerminalScreenTheme theme,
            TerminalClientOptions.InterfaceDensity density,
            TerminalClientOptions.TerminalZoom zoom,
            boolean commandStackCollapsed,
        TerminalClientOptions.NavigationStyle navigationStyle) {
        TerminalScreenTheme resolvedTheme = theme == null ? TerminalScreenTheme.modular() : theme;
        return computeLayoutMetrics(screenWidth, screenHeight, resolvedTheme.panelMaxWidth(),
                resolvedTheme.panelMaxHeight(), density, zoom, commandStackCollapsed, navigationStyle);
    }

    public static List<String> progressNavigationRowsForTests(
            List<TerminalTab> tabs, int activeTab, boolean chapterProgressExpanded) {
        TerminalNavigationModel model = TerminalNavigationModel.of(tabs);
        return navigationRowsForTests(
                model, TerminalNavigationSection.CHAPTERS.key(), activeTab, chapterProgressExpanded);
    }

    private static List<String> navigationRowsForTests(
            TerminalNavigationModel model, String group, int activeTab, boolean chapterProgressExpanded) {
        List<String> rows = new ArrayList<>();
        for (TerminalNavigationModel.IndexedTab entry : model.directTabsInGroup(group)) {
            rows.add("PAGE:" + entry.tab().chrome().shortTitle());
        }
        String activeChapter = model.activeChapterId(activeTab);
        for (TerminalNavigationModel.ChapterGroup chapter : model.chaptersInGroup(group)) {
            if (chapter.id().equals(activeChapter) && chapter.tabs().size() > 1) {
                for (TerminalNavigationModel.IndexedTab entry : chapter.tabs()) {
                    rows.add("CHILD:" + entry.tab().chrome().shortTitle());
                }
            } else {
                rows.add("CHAPTER:" + chapter.title());
            }
        }
        return List.copyOf(rows);
    }

    private static boolean chapterProgressOpenForTests(
            TerminalNavigationModel model, String group, int activeTab, boolean chapterProgressExpanded) {
        return chapterProgressExpanded || model.activeChapterInGroup(group, activeTab);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        ticks++;
        TerminalUi.applyThemeGlobals(TerminalClientOptions.currentTheme());
        TerminalRenderCache.beginFrame();
        layout();
        List<TerminalTab> tabs = tabs();
        normalizeActiveTab(tabs);
        clampCommandStackScroll();

        drawChrome(graphics, tabs, mouseX, mouseY);
        drawBody(graphics, tabs, mouseX, mouseY, partialTick);
        drawFooter(graphics, tabs);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        TerminalRenderCache.beginFrame();
        layout();
        List<TerminalTab> tabs = tabs();
        normalizeActiveTab(tabs);
        clampCommandStackScroll();
        TerminalTab tab = activeTab < tabs.size() ? tabs.get(activeTab) : null;
        ClientTerminalTab clientTab = clientTab(tab);
        if (clientTab != null && clientTab.keyPressed(contextFor(tab, scrollFor(tab)), event)) {
            return true;
        }

        int key = event.key();
        boolean openTerminalKey = EchoTerminalClient.OPEN_TERMINAL_KEY.matches(event);
        if (openTerminalKey && EchoTerminalClient.nativeLoaderClientActiveForScreens() && ticks < 4) {
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE || openTerminalKey) {
            if (EchoTerminalClient.nativeLoaderClientActiveForScreens()) {
                EchoNativeLoadStatus lifecycleStatus = EchoTerminalClient.publishNativeScreenLifecycle(
                            "close",
                            "terminal.screen.close",
                            getClass().getName(),
                            Map.of(
                                    "transitionSource", "terminal_key",
                                    "closeKey", key
                            ));
                if (lifecycleStatus != EchoNativeLoadStatus.MUTATED) {
                    return false;
                }
            }
            Minecraft.getInstance().setScreen(null);
            return true;
        }
        if (tab != null && key == GLFW.GLFW_KEY_PAGE_UP) {
            setScroll(tab, scrollFor(tab) - Math.max(36, contentBodyH() - 34));
            return true;
        }
        if (tab != null && key == GLFW.GLFW_KEY_PAGE_DOWN) {
            setScroll(tab, scrollFor(tab) + Math.max(36, contentBodyH() - 34));
            return true;
        }
        if (key == GLFW.GLFW_KEY_UP && !tabs.isEmpty()) {
            return selectGroupOffset(tabs, -1);
        }
        if (key == GLFW.GLFW_KEY_DOWN && !tabs.isEmpty()) {
            return selectGroupOffset(tabs, 1);
        }
        if (key == GLFW.GLFW_KEY_LEFT && !tabs.isEmpty()) {
            return selectActiveGroupPageOffset(tabs, -1);
        }
        if ((key == GLFW.GLFW_KEY_RIGHT || key == GLFW.GLFW_KEY_TAB) && !tabs.isEmpty()) {
            return selectActiveGroupPageOffset(tabs, 1);
        }
        if (key == GLFW.GLFW_KEY_HOME && !tabs.isEmpty()) {
            selectTab(firstVisibleTabIndex(tabs), tabs);
            return true;
        }
        if (key == GLFW.GLFW_KEY_END && !tabs.isEmpty()) {
            selectTab(lastVisibleTabIndex(tabs), tabs);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        TerminalRenderCache.beginFrame();
        layout();
        List<TerminalTab> tabs = tabs();
        normalizeActiveTab(tabs);
        clampCommandStackScroll();
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && beginScrollbarDrag(tabs, event.x(), event.y())) {
            return true;
        }
        if (handleNavigationClick(tabs, event.x(), event.y())) {
            return true;
        }
        if (handleContentPageTabClick(tabs, event.x(), event.y())) {
            return true;
        }

        TerminalTab tab = activeTab < tabs.size() ? tabs.get(activeTab) : null;
        ClientTerminalTab clientTab = clientTab(tab);
        if (clientTab != null && TerminalUi.inside(event.x(), event.y(), contentX, contentBodyY(), contentW, contentBodyH())
                && clientTab.mouseClicked(contextFor(tab, scrollFor(tab)), event.x(), event.y(), event.button())) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        TerminalRenderCache.beginFrame();
        layout();
        List<TerminalTab> tabs = tabs();
        normalizeActiveTab(tabs);
        clampCommandStackScroll();
        if (scrollbarDragTarget != ScrollbarDragTarget.NONE) {
            updateScrollbarDrag(tabs, event.x(), event.y());
            return true;
        }
        TerminalTab tab = activeTab < tabs.size() ? tabs.get(activeTab) : null;
        ClientTerminalTab clientTab = clientTab(tab);
        if (clientTab != null && clientTab.mouseDragged(contextFor(tab, scrollFor(tab)),
                event.x(), event.y(), event.button(), dragX, dragY)) {
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        TerminalRenderCache.beginFrame();
        layout();
        List<TerminalTab> tabs = tabs();
        normalizeActiveTab(tabs);
        clampCommandStackScroll();
        if (scrollbarDragTarget != ScrollbarDragTarget.NONE) {
            scrollbarDragTarget = ScrollbarDragTarget.NONE;
            return true;
        }
        TerminalTab tab = activeTab < tabs.size() ? tabs.get(activeTab) : null;
        ClientTerminalTab clientTab = clientTab(tab);
        if (clientTab != null && clientTab.mouseReleased(contextFor(tab, scrollFor(tab)),
                event.x(), event.y(), event.button())) {
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public boolean handleMouseScroll(double mouseX, double mouseY, double deltaY) {
        TerminalRenderCache.beginFrame();
        layout();
        List<TerminalTab> tabs = tabs();
        normalizeActiveTab(tabs);
        if (handleCommandStackMouseScroll(tabs, mouseX, mouseY, deltaY)) {
            return true;
        }
        clampCommandStackScroll();
        TerminalTab tab = activeTab < tabs.size() ? tabs.get(activeTab) : null;
        if (tab == null || !TerminalUi.inside(mouseX, mouseY, contentX, contentBodyY(), contentW, contentBodyH())) {
            return false;
        }
        int current = scrollFor(tab);
        TerminalRenderContext context = contextFor(tab, current);
        ClientTerminalTab clientTab = clientTab(tab);
        if (clientTab != null && clientTab.mouseScrolled(context, mouseX, mouseY, deltaY)) {
            clampScroll(tab);
            return true;
        }
        int next = current - (int) Math.round(deltaY * 18.0D);
        setScroll(tab, next);
        return true;
    }

    public boolean handleCharTyped(CharacterEvent event) {
        TerminalRenderCache.beginFrame();
        layout();
        List<TerminalTab> tabs = tabs();
        normalizeActiveTab(tabs);
        clampCommandStackScroll();
        TerminalTab tab = activeTab < tabs.size() ? tabs.get(activeTab) : null;
        ClientTerminalTab clientTab = clientTab(tab);
        return clientTab != null && clientTab.charTyped(contextFor(tab, scrollFor(tab)), event);
    }

    private List<TerminalTab> tabs() {
        if (TerminalTabRegistry.tabs().isEmpty()) {
            ensureTerminalTabsReady();
        }
        List<TerminalTab> current = TerminalTabRegistry.tabs();
        if (current != cachedTabs) {
            cachedTabs = current;
            navigationModel = TerminalNavigationModel.of(current);
        }
        return navigationModel.tabs();
    }

    private void drawChrome(GuiGraphicsExtractor graphics, List<TerminalTab> tabs, int mouseX, int mouseY) {
        TerminalTab tab = activeTab < tabs.size() ? tabs.get(activeTab) : null;
        TerminalTabChrome chrome = tab == null ? null : tab.chrome();
        String status = theme.statusProvider().statusLine(Minecraft.getInstance());
        String meta = Minecraft.getInstance().player == null ? "LINK OFFLINE" : "LINK ONLINE";
        if (chrome != null && !chrome.summary().isBlank()) {
            meta += "  |  " + chrome.summary();
        }
        TerminalRenderContext chromeContext = contextFor(tab, tab == null ? 0 : scrollFor(tab));
        TerminalUi.appShellBackdrop(chromeContext, graphics, panelX, panelY, panelW, panelH, chromeColor(tab));
        TerminalUi.topMetaBar(chromeContext, graphics, font, panelX, panelY, panelW,
                shellHeaderHeight(), theme.title(), status, meta, chromeColor(tab));

        drawSidebarNavigation(graphics, tabs, mouseX, mouseY);
    }

    private void drawSidebarNavigation(GuiGraphicsExtractor graphics, List<TerminalTab> tabs, int mouseX, int mouseY) {
        drawCommandStackNavigation(graphics, tabs, mouseX, mouseY);
    }

    private void drawCommandStackNavigation(GuiGraphicsExtractor graphics, List<TerminalTab> tabs, int mouseX, int mouseY) {
        String activeGroup = navigationModel.activeGroup(activeTab);
        int accent = navigationModel.groupAccent(activeGroup, theme.accentColor());
        if (commandStackEffectivelyCollapsed()) {
            drawCollapsedCommandStack(graphics, tabs, mouseX, mouseY, activeGroup, accent);
            return;
        }
        clampCommandStackScroll();
        TerminalRenderContext renderContext = tabs.isEmpty() ? contextFor(null, 0) : contextFor(tabs.get(activeTab), 0);
        TerminalUi.commandStackPanel(renderContext, graphics, font, groupRailX, groupRailY, groupRailW, groupRailH, accent);
        drawCollapseToggle(renderContext, graphics, mouseX, mouseY, accent);
        boolean compact = groupRailW < 190;
        int cy = commandStackContentY() - commandStackScroll;
        int viewportTop = commandStackViewportTop();
        int viewportH = commandStackViewportHeight();
        boolean railHovered = commandStackViewportContains(mouseX, mouseY);
        int groupInset = railInset(8);
        int groupTrim = railTrim(16);
        graphics.enableScissor(groupRailX, viewportTop, groupRailX + groupRailW,
                commandStackViewportBottom());
        for (String group : navigationModel.groups()) {
            boolean active = group.equals(activeGroup);
            int groupColor = navigationModel.groupAccent(group, theme.accentColor());
            int groupH = commandGroupHeight(compact);
            boolean groupHover = railHovered
                    && TerminalUi.inside(mouseX, mouseY, groupRailX + groupInset, cy, groupRailW - groupTrim, groupH);
            TerminalUi.commandStackGroupButton(renderContext, graphics, font, groupRailX + groupInset, cy,
                    groupRailW - groupTrim, groupH,
                    TerminalIcon.fromGroup(group), TerminalUi.themedGroupIcon(renderContext, group),
                    navigationModel.groupLabel(group), active, groupHover, groupColor);
            cy += groupH + zoomed(7, 5);
        }
        graphics.disableScissor();
        TerminalUi.scrollbar(renderContext, graphics, groupRailX + groupRailW - zoomed(7, 5), viewportTop, viewportH,
                commandStackScroll, maxCommandStackScroll(), accent, railHovered);
        int diagnosticH = diagnosticRailHeight();
        TerminalUi.diagnosticRail(renderContext, graphics, font, groupRailX + railInset(10),
                groupRailY + groupRailH - diagnosticH - zoomed(11, 8), groupRailW - railTrim(20), diagnosticH,
                Minecraft.getInstance().player != null, accent);
    }

    private int drawExpandedGroupPages(GuiGraphicsExtractor graphics, String group, int mouseX, int mouseY,
            int cy, int rowH, int gap, boolean compact, TerminalRenderContext renderContext) {
        for (TerminalNavigationModel.IndexedTab entry : navigationModel.directTabsInGroup(group)) {
            cy = drawNavigationPage(graphics, entry, mouseX, mouseY, cy, rowH, gap, compact, 14, 22);
        }
        if (!navigationModel.hasChaptersInGroup(group)) {
            return cy;
        }
        cy = drawChapterProgressHeader(graphics, group, mouseX, mouseY, cy, rowH, gap, compact, renderContext);
        if (!chapterProgressOpen(group)) {
            return cy;
        }
        String activeChapter = navigationModel.activeChapterId(activeTab);
        for (TerminalNavigationModel.ChapterGroup chapter : navigationModel.chaptersInGroup(group)) {
            boolean selectedChapter = chapter.id().equals(activeChapter);
            int chapterInset = railInset(14);
            int chapterTrim = railTrim(22);
            boolean chapterHover = commandStackViewportContains(mouseX, mouseY)
                    && TerminalUi.inside(mouseX, mouseY, groupRailX + chapterInset, cy,
                            groupRailW - chapterTrim, rowH);
            String label = compact ? chapter.iconLabel() : chapter.title();
            String summary = chapterRailSummary(chapter, compact);
            TerminalIcon chapterIcon = TerminalIcon.fromTitle(chapter.title());
            if (chapterIcon == TerminalIcon.DEFAULT) {
                chapterIcon = TerminalIcon.ADDONS;
            }
            TerminalRenderContext chapterContext = renderContext.withChapterTheme(chapter.id(), chapter.title(), chapter.id());
            TerminalUi.commandPageButton(chapterContext, graphics, font, groupRailX + chapterInset, cy,
                    groupRailW - chapterTrim, rowH,
                    chapterIcon, TerminalUi.themedIcon(chapterContext, com.knoxhack.echoterminal.api.theme.TerminalIconKey.chapter(chapter.id()),
                            TerminalUi.themedPageIcon(renderContext, chapter.title())), label, summary,
                    selectedChapter, chapterHover, chapter.accent());
            cy += rowH + gap;
            if (selectedChapter) {
                int childRailX = groupRailX + railInset(18);
                int childRailH = chapter.tabs().size() * (rowH + gap) - gap;
                if (childRailH > 0) {
                    TerminalUi.navigationSpine(chapterContext, graphics, childRailX, cy, childRailH, chapter.accent());
                }
                for (TerminalNavigationModel.IndexedTab entry : chapter.tabs()) {
                    cy = drawNavigationPage(graphics, entry, mouseX, mouseY, cy, rowH, gap, compact, 24, 34);
                }
            }
        }
        return cy;
    }

    private int drawChapterProgressHeader(GuiGraphicsExtractor graphics, String group, int mouseX, int mouseY,
            int cy, int rowH, int gap, boolean compact, TerminalRenderContext renderContext) {
        int chapterInset = railInset(14);
        int chapterTrim = railTrim(22);
        boolean open = chapterProgressOpen(group);
        boolean active = navigationModel.activeChapterInGroup(group, activeTab);
        boolean hover = commandStackViewportContains(mouseX, mouseY)
                && TerminalUi.inside(mouseX, mouseY, groupRailX + chapterInset, cy, groupRailW - chapterTrim, rowH);
        int count = navigationModel.chaptersInGroup(group).size();
        String label = compact ? "CP" : CHAPTER_PROGRESS_LABEL;
        String summary = compact ? "" : open ? "Reference routes" : count + " read-only routes";
        TerminalRenderContext chapterContext = renderContext.withChapterTheme(
                "chapter_progress", CHAPTER_PROGRESS_LABEL, "echoterminal");
        TerminalUi.commandPageButton(chapterContext, graphics, font, groupRailX + chapterInset, cy,
                groupRailW - chapterTrim, rowH,
                TerminalIcon.ADDONS,
                TerminalUi.themedIcon(chapterContext, TerminalIconKey.chapter("chapter_progress"),
                        TerminalUi.themedGroupIcon(renderContext, group)),
                label, summary, active, hover, navigationModel.groupAccent(group, theme.accentColor()));
        TerminalUi.collapseToggle(chapterContext, graphics,
                groupRailX + groupRailW - railTrim(38), cy + Math.max(3, (rowH - 12) / 2),
                14, 12, !open, hover, navigationModel.groupAccent(group, theme.accentColor()));
        return cy + rowH + gap;
    }

    private static String chapterRailSummary(TerminalNavigationModel.ChapterGroup chapter, boolean compact) {
        if (compact || chapter == null) {
            return "";
        }
        String title = chapter.rawTitle() == null ? "" : chapter.rawTitle();
        if (title.startsWith("Chapter ")) {
            return "Story route";
        }
        if (title.startsWith("Optional:")) {
            return "Side route";
        }
        int count = chapter.tabs().size();
        return count == 1 ? "Linked page" : count + " linked pages";
    }

    private boolean chapterProgressOpen(String group) {
        return chapterProgressExpanded || navigationModel.activeChapterInGroup(group, activeTab);
    }

    private int visibleCommandRowCount(String group) {
        int rows = navigationModel.directTabsInGroup(group).size();
        if (!navigationModel.hasChaptersInGroup(group)) {
            return Math.max(1, rows);
        }
        rows += 1;
        if (chapterProgressOpen(group)) {
            rows += navigationModel.chaptersInGroup(group).size();
            String activeChapter = navigationModel.activeChapterId(activeTab);
            for (TerminalNavigationModel.ChapterGroup chapter : navigationModel.chaptersInGroup(group)) {
                if (chapter.id().equals(activeChapter)) {
                    rows += chapter.tabs().size();
                    break;
                }
            }
        }
        return Math.max(1, rows);
    }

    private int drawNavigationPage(GuiGraphicsExtractor graphics, TerminalNavigationModel.IndexedTab entry,
            int mouseX, int mouseY, int cy, int rowH, int gap, boolean compact, int inset, int widthTrim) {
        TerminalTab tab = entry.tab();
        boolean selected = entry.index() == activeTab;
        int scaledInset = railInset(inset);
        int scaledTrim = railTrim(widthTrim);
        boolean hover = commandStackViewportContains(mouseX, mouseY)
                && TerminalUi.inside(mouseX, mouseY, groupRailX + scaledInset, cy, groupRailW - scaledTrim, rowH);
        TerminalRenderContext renderContext = contextFor(tab, scrollFor(tab));
        TerminalUi.commandPageButton(renderContext, graphics, font, groupRailX + scaledInset, cy,
                groupRailW - scaledTrim, rowH,
                TerminalIcon.fromTitle(tab.chrome().shortTitle()),
                TerminalUi.themedPageIcon(renderContext, tab.chrome().shortTitle()), tab.chrome().shortTitle(),
                compact ? "" : tab.chrome().summary(), selected, hover, tab.descriptor().accentColor());
        return cy + rowH + gap;
    }

    private void drawCollapsedCommandStack(GuiGraphicsExtractor graphics, List<TerminalTab> tabs,
            int mouseX, int mouseY, String activeGroup, int accent) {
        TerminalRenderContext renderContext = tabs.isEmpty() ? contextFor(null, 0) : contextFor(tabs.get(activeTab), 0);
        TerminalUi.cinematicPanel(renderContext, graphics, groupRailX, groupRailY, groupRailW, groupRailH, accent);
        if (canToggleCommandStack()) {
            drawCollapseToggle(renderContext, graphics, mouseX, mouseY, accent);
        }
        int buttonW = Math.max(zoomed(30, 24), groupRailW - railTrim(16));
        int cy = groupRailY + zoomed(34, 28);
        int rowH = collapsedGroupRowHeight();
        for (String group : navigationModel.groups()) {
            boolean active = group.equals(activeGroup);
            int groupColor = navigationModel.groupAccent(group, theme.accentColor());
            int groupInset = railInset(8);
            boolean hover = TerminalUi.inside(mouseX, mouseY, groupRailX + groupInset, cy, buttonW, rowH);
            TerminalUi.iconRailButton(renderContext, graphics, font, groupRailX + groupInset, cy, buttonW, rowH,
                    TerminalIcon.fromGroup(group), TerminalUi.themedGroupIcon(renderContext, group), "",
                    active, hover, groupColor);
            cy += rowH + zoomed(6, 4);
        }
        if (!tabs.isEmpty()) {
            TerminalTab tab = tabs.get(activeTab);
            TerminalRenderContext tabContext = contextFor(tab, scrollFor(tab));
            int pageY = Math.min(groupRailY + groupRailH - zoomed(82, 68), cy + zoomed(8, 6));
            int badgeSize = zoomed(28, 22);
            TerminalUi.hybridIconBadge(tabContext, graphics, TerminalUi.themedPageIcon(tabContext, tab.chrome().shortTitle()),
                    TerminalIcon.fromTitle(tab.chrome().shortTitle()),
                    groupRailX + Math.max(railInset(8), (groupRailW - badgeSize) / 2), pageY, badgeSize,
                    tab.descriptor().accentColor(), true);
            TerminalUi.collapsedRailStatus(tabContext, graphics, groupRailX + railInset(10),
                    groupRailY + groupRailH - zoomed(26, 22), groupRailW - railTrim(20), 0.82F, accent);
        }
    }

    private void drawCollapseToggle(TerminalRenderContext context, GuiGraphicsExtractor graphics,
            int mouseX, int mouseY, int accent) {
        boolean hovered = TerminalUi.inside(mouseX, mouseY, collapseToggleX, collapseToggleY, collapseToggleW, collapseToggleH);
        TerminalUi.collapseToggle(context, graphics, collapseToggleX, collapseToggleY, collapseToggleW,
                collapseToggleH, commandStackEffectivelyCollapsed(), hovered, accent);
    }

    private void drawCompactTopNavigation(GuiGraphicsExtractor graphics, List<TerminalTab> tabs, int mouseX, int mouseY) {
        if (tabs.isEmpty()) {
            return;
        }
        String activeGroup = navigationModel.activeGroup(activeTab);
        int groupW = layoutProfile == TerminalLayoutProfile.COMPACT_STACK ? 100 : 142;
        int accent = navigationModel.groupAccent(activeGroup, theme.accentColor());
        boolean groupHover = TerminalUi.inside(mouseX, mouseY, navX, navY, groupW, navH);
        TerminalRenderContext navContext = tabs.isEmpty() ? contextFor(null, 0) : contextFor(tabs.get(activeTab), 0);
        TerminalUi.categoryChip(navContext, graphics, font, navX, navY, groupW, navH,
                navigationModel.groupLabel(activeGroup), true, groupHover, accent);
        List<TerminalNavigationModel.IndexedTab> groupTabs = navigationModel.visibleTabsInGroup(activeGroup, activeTab);
        int x = navX + groupW + 4;
        int available = Math.max(48, navW - groupW - 4);
        int chipW = groupTabs.isEmpty() ? 0 : Math.max(72, Math.min(160,
                (available - Math.max(0, groupTabs.size() - 1) * 4) / groupTabs.size()));
        for (TerminalNavigationModel.IndexedTab entry : groupTabs) {
            TerminalTab tab = entry.tab();
            boolean active = entry.index() == activeTab;
            boolean hover = TerminalUi.inside(mouseX, mouseY, x, navY, chipW, navH);
            TerminalRenderContext tabContext = contextFor(tab, scrollFor(tab));
            TerminalUi.pageTab(tabContext, graphics, font, x, navY, chipW, navH,
                    tab.chrome().shortTitle(), active, hover, tab.descriptor().accentColor());
            x += chipW + 4;
        }
    }

    private void drawBody(GuiGraphicsExtractor graphics, List<TerminalTab> tabs, int mouseX, int mouseY, float partialTick) {
        int bodyAccent = tabs.isEmpty() ? theme.accentColor() : chromeColor(tabs.get(activeTab));
        boolean contentHovered = TerminalUi.inside(mouseX, mouseY, contentX, contentY, contentW, contentH);
        TerminalRenderContext bodyContext = tabs.isEmpty() ? contextFor(null, 0) : contextFor(tabs.get(activeTab), 0);
        TerminalUi.contentFrame(bodyContext, graphics, contentX, contentY, contentW, contentH, bodyAccent, contentHovered);
        drawContentPageTabs(graphics, tabs, mouseX, mouseY);
        int bodyY = contentBodyY();
        int bodyH = contentBodyH();
        if (tabs.isEmpty()) {
            graphics.text(font, Component.literal("No terminal views are online."), contentX + 10, bodyY + 10, theme.mutedColor(), false);
            return;
        }
        if (Minecraft.getInstance().player == null) {
            graphics.text(font, Component.literal("LINK OFFLINE"), contentX + 10, bodyY + 10, TerminalUi.RED, true);
            graphics.text(font, Component.literal("Operator link unavailable. Reopen after joining a world."),
                    contentX + 10, bodyY + 26, theme.mutedColor(), false);
            return;
        }

        TerminalTab tab = tabs.get(activeTab);
        clampScroll(tab);
        int scroll = scrollFor(tab);
        TerminalRenderContext context = contextFor(tab, scroll);
        graphics.enableScissor(contentX, bodyY, contentX + contentW, bodyY + bodyH);
        ClientTerminalTab clientTab = clientTab(tab);
        if (clientTab != null) {
            clientTab.render(context, graphics, mouseX, mouseY, partialTick);
        }
        graphics.disableScissor();
        TerminalUi.scrollbar(context, graphics, contentX + contentW - zoomed(7, 5),
                bodyY + zoomed(8, 6), bodyH - zoomed(16, 12),
                scroll, maxScroll(tab), tab.descriptor().accentColor(), contentHovered);
    }

    private void drawContentPageTabs(GuiGraphicsExtractor graphics, List<TerminalTab> tabs, int mouseX, int mouseY) {
        List<PageTabChoice> choices = activeGroupPageTabs(tabs);
        if (choices.size() <= 1) {
            return;
        }
        int tabH = pageTabHeight();
        int y = contentY + zoomed(TerminalDesignTokens.GAP_SMALL, 6);
        int x = contentX + zoomed(10, 8);
        int available = Math.max(80, contentW - zoomed(24, 18));
        int gap = zoomed(6, 4);
        int tabW = Math.max(48, (available - gap * Math.max(0, choices.size() - 1)) / choices.size());
        for (PageTabChoice choice : choices) {
            boolean selected = choice.index() == activeTab;
            boolean hovered = TerminalUi.inside(mouseX, mouseY, x, y, tabW, tabH);
            TerminalTab choiceTab = choice.index() >= 0 && choice.index() < tabs.size() ? tabs.get(choice.index()) : null;
            TerminalRenderContext tabContext = choiceTab == null ? contextFor(null, 0) : contextFor(choiceTab, scrollFor(choiceTab));
            TerminalUi.pageTab(tabContext, graphics, font, x, y, tabW, tabH,
                    choice.label(), selected, hovered, choice.accentColor());
            x += tabW + gap;
        }
    }

    private void drawFooter(GuiGraphicsExtractor graphics, List<TerminalTab> tabs) {
        String footer = panelW < 560
                ? "ESC Close   Arrows Nav   Wheel Scroll"
                : useCompactTopNavigation()
                        ? "ESC/Open Key Close   Group Chip Switches Section   Tab/Arrows Navigate"
                : layoutProfile == TerminalLayoutProfile.COMPACT_STACK
                        ? "ESC/Open Key Close   Arrows Navigate   Enter Command   Wheel Scroll"
                        : "ESC/Open Key Close   Arrows Navigate   Enter Command   Page/Wheel Scroll";
        String label = "";
        if (!tabs.isEmpty()) {
            TerminalTab active = tabs.get(activeTab);
            label = navigationModel.activePathLabel(activeTab);
        }
        int color = tabs.isEmpty() ? theme.accentColor() : chromeColor(tabs.get(activeTab));
        TerminalRenderContext renderContext = tabs.isEmpty() ? contextFor(null, 0) : contextFor(tabs.get(activeTab), 0);
        int footerH = shellFooterHeight();
        TerminalUi.bottomShortcutBar(renderContext, graphics, font, panelX, panelY + panelH - footerH, panelW,
                footerH, footer, label.isBlank() ? "Esc Back" : label, color);
    }

    private boolean handleNavigationClick(List<TerminalTab> tabs, double mouseX, double mouseY) {
        if (tabs.isEmpty()) {
            return false;
        }
        if (useCompactTopNavigation()) {
            return handleCompactNavigationClick(tabs, mouseX, mouseY);
        }
        return handleSidebarNavigationClick(tabs, mouseX, mouseY);
    }

    private boolean handleSidebarNavigationClick(List<TerminalTab> tabs, double mouseX, double mouseY) {
        return handleCommandStackNavigationClick(tabs, mouseX, mouseY);
    }

    private boolean handleCommandStackNavigationClick(List<TerminalTab> tabs, double mouseX, double mouseY) {
        if (canToggleCommandStack()
                && TerminalUi.inside(mouseX, mouseY, collapseToggleX, collapseToggleY, collapseToggleW, collapseToggleH)) {
            commandStackCollapsed = !commandStackCollapsed;
            commandStackScroll = 0;
            playUiSound(0.85F);
            return true;
        }
        if (commandStackEffectivelyCollapsed()) {
            return handleCollapsedCommandStackClick(tabs, mouseX, mouseY);
        }
        clampCommandStackScroll();
        if (!commandStackViewportContains(mouseX, mouseY)) {
            return false;
        }
        String activeGroup = navigationModel.activeGroup(activeTab);
        boolean compact = groupRailW < 190;
        int cy = commandStackContentY() - commandStackScroll;
        int groupInset = railInset(8);
        int groupTrim = railTrim(16);
        for (String group : navigationModel.groups()) {
            int groupH = commandGroupHeight(compact);
            if (TerminalUi.inside(mouseX, mouseY, groupRailX + groupInset, cy, groupRailW - groupTrim, groupH)) {
                selectTab(navigationModel.firstTabInGroup(group), tabs);
                return true;
            }
            cy += groupH + zoomed(7, 5);
        }
        return false;
    }

    private boolean handleContentPageTabClick(List<TerminalTab> tabs, double mouseX, double mouseY) {
        List<PageTabChoice> choices = activeGroupPageTabs(tabs);
        if (choices.size() <= 1) {
            return false;
        }
        int tabH = pageTabHeight();
        int y = contentY + zoomed(TerminalDesignTokens.GAP_SMALL, 6);
        if (!TerminalUi.inside(mouseX, mouseY, contentX, y, contentW, tabH)) {
            return false;
        }
        int x = contentX + zoomed(10, 8);
        int available = Math.max(80, contentW - zoomed(24, 18));
        int gap = zoomed(6, 4);
        int tabW = Math.max(48, (available - gap * Math.max(0, choices.size() - 1)) / choices.size());
        for (PageTabChoice choice : choices) {
            if (TerminalUi.inside(mouseX, mouseY, x, y, tabW, tabH)) {
                selectTab(choice.index(), tabs);
                return true;
            }
            x += tabW + gap;
        }
        return false;
    }

    private int handleExpandedGroupClick(List<TerminalTab> tabs, String group, double mouseX, double mouseY,
            int cy, int rowH, int gap) {
        for (TerminalNavigationModel.IndexedTab entry : navigationModel.directTabsInGroup(group)) {
            if (TerminalUi.inside(mouseX, mouseY, groupRailX + railInset(14), cy,
                    groupRailW - railTrim(22), rowH)) {
                selectTab(entry.index(), tabs);
                return -1;
            }
            cy += rowH + gap;
        }
        if (!navigationModel.hasChaptersInGroup(group)) {
            return cy;
        }
        if (TerminalUi.inside(mouseX, mouseY, groupRailX + railInset(14), cy,
                groupRailW - railTrim(22), rowH)) {
            chapterProgressExpanded = !chapterProgressExpanded;
            playUiSound(chapterProgressExpanded ? 1.05F : 0.85F);
            return -1;
        }
        cy += rowH + gap;
        if (!chapterProgressOpen(group)) {
            return cy;
        }
        String activeChapter = navigationModel.activeChapterId(activeTab);
        for (TerminalNavigationModel.ChapterGroup chapter : navigationModel.chaptersInGroup(group)) {
            if (TerminalUi.inside(mouseX, mouseY, groupRailX + railInset(14), cy,
                    groupRailW - railTrim(22), rowH)) {
                selectTab(navigationModel.firstTabInChapter(chapter), tabs);
                return -1;
            }
            cy += rowH + gap;
            if (chapter.id().equals(activeChapter)) {
                for (TerminalNavigationModel.IndexedTab entry : chapter.tabs()) {
                    if (TerminalUi.inside(mouseX, mouseY, groupRailX + railInset(22), cy,
                            groupRailW - railTrim(30), rowH)) {
                        selectTab(entry.index(), tabs);
                        return -1;
                    }
                    cy += rowH + gap;
                }
            }
        }
        return cy;
    }

    private boolean handleCollapsedCommandStackClick(List<TerminalTab> tabs, double mouseX, double mouseY) {
        int buttonW = Math.max(zoomed(30, 24), groupRailW - railTrim(16));
        int cy = groupRailY + zoomed(34, 28);
        int rowH = collapsedGroupRowHeight();
        for (String group : navigationModel.groups()) {
            if (TerminalUi.inside(mouseX, mouseY, groupRailX + railInset(8), cy, buttonW, rowH)) {
                selectTab(navigationModel.firstTabInGroup(group), tabs);
                return true;
            }
            cy += rowH + zoomed(6, 4);
        }
        return false;
    }

    private boolean handleCompactNavigationClick(List<TerminalTab> tabs, double mouseX, double mouseY) {
        String activeGroup = navigationModel.activeGroup(activeTab);
        int groupW = layoutProfile == TerminalLayoutProfile.COMPACT_STACK ? 100 : 142;
        if (TerminalUi.inside(mouseX, mouseY, navX, navY, groupW, navH)) {
            return selectGroupOffset(tabs, 1);
        }
        List<TerminalNavigationModel.IndexedTab> groupTabs = navigationModel.visibleTabsInGroup(activeGroup, activeTab);
        int x = navX + groupW + 8;
        int available = Math.max(48, navW - groupW - 12);
        int chipW = groupTabs.isEmpty() ? 0 : Math.max(72, Math.min(160,
                (available - Math.max(0, groupTabs.size() - 1) * 4) / groupTabs.size()));
        for (TerminalNavigationModel.IndexedTab entry : groupTabs) {
            if (TerminalUi.inside(mouseX, mouseY, x, navY, chipW, navH)) {
                selectTab(entry.index(), tabs);
                return true;
            }
            x += chipW + 4;
        }
        return false;
    }

    private int commandRowHeight() {
        if (commandStackEffectivelyCollapsed()) {
            return zoomed(Math.max(24, 28 - densityStep() * 2), 20);
        }
        String activeGroup = navigationModel.activeGroup(activeTab);
        int tabCount = Math.max(1, visibleCommandRowCount(activeGroup));
        int groupCount = Math.max(1, navigationModel.groups().size());
        boolean compact = groupRailW < 190;
        int reserved = zoomed(compact ? 88 : 106, compact ? 72 : 86)
                + groupCount * (commandGroupHeight(compact) + zoomed(5, 3));
        int min = zoomed(Math.max(compact ? 20 : 23, (compact ? 22 : 26) - densityStep()),
                compact ? 18 : 20);
        int max = zoomed(Math.max(compact ? 24 : 28, (compact ? 28 : 34) - densityStep() * 3),
                compact ? 20 : 23);
        int available = Math.max(tabCount * min, groupRailH - reserved);
        return Math.max(min, Math.min(max, (available / tabCount) - zoomed(2, 1)));
    }

    private int commandRowGap() {
        return commandRowHeight() <= zoomed(25, 21) ? zoomed(2, 2) : zoomed(3, 2);
    }

    private boolean handleCommandStackMouseScroll(List<TerminalTab> tabs, double mouseX, double mouseY, double deltaY) {
        if (tabs.isEmpty() || useCompactTopNavigation() || commandStackEffectivelyCollapsed()
                || !commandStackViewportContains(mouseX, mouseY)) {
            return false;
        }
        int maxScroll = maxCommandStackScroll();
        if (maxScroll <= 0) {
            return false;
        }
        setCommandStackScroll(commandStackScroll - (int) Math.round(deltaY * commandStackScrollStep()));
        return true;
    }

    private boolean beginScrollbarDrag(List<TerminalTab> tabs, double mouseX, double mouseY) {
        TerminalScrollbar.Metrics commandStack = commandStackScrollbar();
        if (commandStack.insideTrack(mouseX, mouseY)) {
            scrollbarDragTarget = ScrollbarDragTarget.COMMAND_STACK;
            scrollbarDragOffset = commandStack.dragOffset(mouseY);
            setCommandStackScroll(commandStack.scrollForMouse(mouseY, scrollbarDragOffset));
            return true;
        }
        TerminalTab tab = activeTab < tabs.size() ? tabs.get(activeTab) : null;
        TerminalScrollbar.Metrics content = contentScrollbar(tab);
        if (content.insideTrack(mouseX, mouseY)) {
            scrollbarDragTarget = ScrollbarDragTarget.CONTENT;
            scrollbarDragOffset = content.dragOffset(mouseY);
            setScroll(tab, content.scrollForMouse(mouseY, scrollbarDragOffset));
            return true;
        }
        return false;
    }

    private void updateScrollbarDrag(List<TerminalTab> tabs, double mouseX, double mouseY) {
        if (scrollbarDragTarget == ScrollbarDragTarget.COMMAND_STACK) {
            TerminalScrollbar.Metrics commandStack = commandStackScrollbar();
            setCommandStackScroll(commandStack.scrollForMouse(mouseY, scrollbarDragOffset));
        } else if (scrollbarDragTarget == ScrollbarDragTarget.CONTENT) {
            TerminalTab tab = activeTab < tabs.size() ? tabs.get(activeTab) : null;
            if (tab != null) {
                TerminalScrollbar.Metrics content = contentScrollbar(tab);
                setScroll(tab, content.scrollForMouse(mouseY, scrollbarDragOffset));
            }
        }
    }

    private void syncCommandStackScrollGroup() {
        String activeGroup = navigationModel.activeGroup(activeTab);
        if (!activeGroup.equals(commandStackScrollGroup)) {
            commandStackScrollGroup = activeGroup;
            commandStackScroll = 0;
        }
    }

    private void setCommandStackScroll(int value) {
        syncCommandStackScrollGroup();
        commandStackScroll = Math.max(0, Math.min(value, maxCommandStackScroll()));
    }

    private void clampCommandStackScroll() {
        syncCommandStackScrollGroup();
        if (useCompactTopNavigation() || commandStackEffectivelyCollapsed()) {
            commandStackScroll = 0;
            return;
        }
        commandStackScroll = Math.max(0, Math.min(commandStackScroll, maxCommandStackScroll()));
    }

    private int maxCommandStackScroll() {
        if (useCompactTopNavigation() || commandStackEffectivelyCollapsed()) {
            return 0;
        }
        return Math.max(0, commandStackContentHeight() - commandStackViewportHeight());
    }

    private int commandStackContentHeight() {
        boolean compact = groupRailW < 190;
        int cy = commandStackContentY();
        for (String group : navigationModel.groups()) {
            cy += commandGroupHeight(compact) + zoomed(7, 5);
        }
        return Math.max(0, cy - commandStackViewportTop());
    }

    private int advanceExpandedGroup(String group, int cy, int rowH, int gap) {
        cy += navigationModel.directTabsInGroup(group).size() * (rowH + gap);
        if (!navigationModel.hasChaptersInGroup(group)) {
            return cy;
        }
        cy += rowH + gap;
        if (!chapterProgressOpen(group)) {
            return cy;
        }
        String activeChapter = navigationModel.activeChapterId(activeTab);
        for (TerminalNavigationModel.ChapterGroup chapter : navigationModel.chaptersInGroup(group)) {
            cy += rowH + gap;
            if (chapter.id().equals(activeChapter)) {
                cy += chapter.tabs().size() * (rowH + gap);
            }
        }
        return cy;
    }

    private int commandStackContentY() {
        return groupRailY + (groupRailW < 190
                ? zoomed(Math.max(30, 32 - densityStep()), 26)
                : zoomed(Math.max(34, 40 - densityStep() * 3), 30));
    }

    private int commandStackViewportTop() {
        return groupRailY + zoomed(Math.max(26, 28 - densityStep()), 22);
    }

    private int commandStackViewportBottom() {
        return groupRailY + groupRailH - commandStackFooterReserve();
    }

    private int commandStackViewportHeight() {
        return Math.max(0, commandStackViewportBottom() - commandStackViewportTop());
    }

    private boolean commandStackViewportContains(double mouseX, double mouseY) {
        return !useCompactTopNavigation()
                && !commandStackEffectivelyCollapsed()
                && TerminalUi.inside(mouseX, mouseY, groupRailX, commandStackViewportTop(),
                        groupRailW, commandStackViewportHeight());
    }

    private TerminalScrollbar.Metrics commandStackScrollbar() {
        return TerminalScrollbar.vertical(
                groupRailX + groupRailW - zoomed(7, 5),
                commandStackViewportTop(),
                zoomed(7, 5),
                commandStackViewportHeight(),
                commandStackScroll,
                maxCommandStackScroll());
    }

    private TerminalScrollbar.Metrics contentScrollbar(TerminalTab tab) {
        if (tab == null) {
            return TerminalScrollbar.vertical(0, 0, 0, 0, 0, 0);
        }
        int bodyY = contentBodyY();
        int bodyH = contentBodyH();
        return TerminalScrollbar.vertical(
                contentX + contentW - zoomed(7, 5),
                bodyY + zoomed(8, 6),
                zoomed(7, 5),
                bodyH - zoomed(16, 12),
                scrollFor(tab),
                maxScroll(tab));
    }

    private boolean useCompactTopNavigation() {
        return navigationStyle() == TerminalClientOptions.NavigationStyle.COMPACT_TOP;
    }

    private boolean commandStackEffectivelyCollapsed() {
        return navigationStyle() == TerminalClientOptions.NavigationStyle.SIDEBAR_HUB || commandStackCollapsed;
    }

    private boolean canToggleCommandStack() {
        return navigationStyle() == TerminalClientOptions.NavigationStyle.APP_HUB;
    }

    private TerminalClientOptions.NavigationStyle navigationStyle() {
        return TerminalClientOptions.navigationStyle();
    }

    private TerminalClientOptions.InterfaceDensity interfaceDensity() {
        return TerminalClientOptions.interfaceDensity();
    }

    private int densityStep() {
        return interfaceDensity().compactness();
    }

    private TerminalClientOptions.TerminalZoom terminalZoom() {
        return TerminalClientOptions.terminalZoom();
    }

    private double terminalZoomScale() {
        return terminalZoom().scale();
    }

    private int zoomed(int value) {
        return (int) Math.round(value * terminalZoomScale());
    }

    private int zoomed(int value, int minimum) {
        return Math.max(minimum, zoomed(value));
    }

    private double zoomed(double value, double minimum) {
        return Math.max(minimum, value * terminalZoomScale());
    }

    private static int contentZoomed(int value, int minimum, TerminalClientOptions.TerminalZoom zoom) {
        double scale = zoom == null ? 1.0D : zoom.scale();
        return Math.max(minimum, (int) Math.round(value * scale));
    }

    private static int shellSized(int value, int minimum) {
        return Math.max(minimum, value);
    }

    private int railInset(int value) {
        return zoomed(value, Math.max(5, (int) Math.floor(value * 0.72D)));
    }

    private int railTrim(int value) {
        return zoomed(value, Math.max(10, (int) Math.floor(value * 0.72D)));
    }

    private int shellHeaderHeight() {
        return shellHeaderHeight(densityStep());
    }

    private int shellFooterHeight() {
        return shellFooterHeight(densityStep());
    }

    private static int shellHeaderHeight(int density) {
        if (TerminalClientOptions.cyberglassActive()) {
            int base = TerminalClientOptions.cyberglassCinematic() ? 64 : TerminalClientOptions.cyberglassCompact() ? 48 : 56;
            return shellSized(Math.max(44, base - density * 2), 40);
        }
        return shellSized(Math.max(42, 52 - density * 4), 36);
    }

    private static int shellFooterHeight(int density) {
        if (TerminalClientOptions.cyberglassActive()) {
            int base = TerminalClientOptions.cyberglassCinematic() ? 42 : TerminalClientOptions.cyberglassCompact() ? 30 : 36;
            return shellSized(Math.max(26, base - density), 24);
        }
        return shellSized(Math.max(24, 30 - density * 2), 22);
    }

    private int commandGroupHeight(boolean compact) {
        if (TerminalClientOptions.cyberglassActive()) {
            int base = TerminalClientOptions.cyberglassCinematic() ? 40 : TerminalClientOptions.cyberglassCompact() ? 30 : 34;
            return zoomed(compact ? Math.max(28, base - 3) : base, compact ? 24 : 28);
        }
        return zoomed(Math.max(compact ? 22 : 24, (compact ? 24 : 28) - densityStep() * 2),
                compact ? 19 : 20);
    }

    private int collapsedGroupRowHeight() {
        int base = groupRailW <= 52 ? 32 : 34;
        if (TerminalClientOptions.cyberglassActive()) {
            base = TerminalClientOptions.cyberglassCinematic() ? base + 6 : base + 2;
        }
        return zoomed(Math.max(28, base - densityStep() * 2), 24);
    }

    private int diagnosticRailHeight() {
        if (TerminalClientOptions.cyberglassActive()) {
            int base = TerminalClientOptions.cyberglassCinematic() ? 92 : TerminalClientOptions.cyberglassCompact() ? 58 : 76;
            return zoomed(base, 50);
        }
        return zoomed(Math.max(30, 36 - densityStep() * 2), 26);
    }

    private int commandStackFooterReserve() {
        return diagnosticRailHeight() + zoomed(16, 12);
    }

    private double commandStackScrollStep() {
        return zoomed(Math.max(14.0D, 18.0D - densityStep() * 2.0D), 12.0D);
    }

    private int pageTabHeight() {
        if (TerminalClientOptions.cyberglassActive()) {
            int base = TerminalClientOptions.cyberglassCinematic() ? 32 : TerminalClientOptions.cyberglassCompact() ? 22 : 28;
            return zoomed(base, 20);
        }
        return TerminalClientOptions.largeTextMode()
                ? zoomed(26, 24)
                : zoomed(22, 20);
    }

    private int contentTabAreaHeight() {
        return activeGroupPageTabs(navigationModel.tabs()).size() <= 1
                ? 0
                : pageTabHeight() + zoomed(TerminalDesignTokens.GAP_SECTION, 12);
    }

    private int contentBodyY() {
        return contentY + contentTabAreaHeight();
    }

    private int contentBodyH() {
        return Math.max(80, contentH - contentTabAreaHeight());
    }

    private TerminalRenderContext contextFor(TerminalTab tab, int scroll) {
        Minecraft minecraft = Minecraft.getInstance();
        Identifier tabId = tab == null ? null : tab.descriptor().id();
        String group = tab == null ? navigationModel.activeGroup(activeTab) : tab.chrome().group();
        String chapterId = navigationModel.activeChapterId(activeTab);
        String chapterTitle = navigationModel.activePathLabel(activeTab);
        String namespace = tabId == null || "echoterminal".equals(tabId.getNamespace()) ? chapterId : tabId.getNamespace();
        TerminalThemeContext themeContext = new TerminalThemeContext(
                tabId,
                group,
                chapterId,
                chapterTitle,
                namespace,
                ticks,
                TerminalClientOptions.useVisualAssets(),
                TerminalClientOptions.reduceMotion());
        int contentPadX = TerminalClientOptions.cyberglassActive()
                ? zoomed(TerminalClientOptions.cyberglassCinematic() ? 16 : 12, 10)
                : zoomed(10, 8);
        int contentPadY = TerminalClientOptions.cyberglassActive()
                ? zoomed(TerminalClientOptions.cyberglassCinematic() ? 16 : 12, 10)
                : zoomed(10, 8);
        int contentTrimW = TerminalClientOptions.cyberglassActive()
                ? zoomed(TerminalClientOptions.cyberglassCinematic() ? 34 : 28, 20)
                : zoomed(22, 16);
        int contentTrimH = TerminalClientOptions.cyberglassActive()
                ? zoomed(TerminalClientOptions.cyberglassCinematic() ? 32 : 26, 20)
                : zoomed(20, 16);
        int bodyY = contentBodyY();
        int bodyH = contentBodyH();
        return new TerminalRenderContext(
                minecraft,
                minecraft.player,
                width,
                height,
                contentX + contentPadX,
                bodyY + contentPadY - scroll,
                Math.max(80, contentW - contentTrimW),
                Math.max(80, bodyH - contentTrimH),
                scroll,
                this::selectTabById,
                this::hasTab,
                TerminalClientOptions.currentTheme(),
                themeContext);
    }

    private boolean hasTab(Identifier tabId) {
        if (tabId == null) {
            return false;
        }
        return tabs().stream().anyMatch(tab -> tab.descriptor().id().equals(tabId));
    }

    private void selectTabById(Identifier tabId) {
        List<TerminalTab> tabs = tabs();
        for (int i = 0; i < tabs.size(); i++) {
            if (tabs.get(i).descriptor().id().equals(tabId)) {
                selectTab(i, tabs);
                return;
            }
        }
    }

    private void selectTab(int index, List<TerminalTab> tabs) {
        if (tabs.isEmpty()) {
            activeTab = 0;
            return;
        }
        int previousTab = activeTab;
        activeTab = Math.max(0, Math.min(index, tabs.size() - 1));
        TerminalTab tab = tabs.get(activeTab);
        rememberedTabId = tab.descriptor().id();
        ClientTerminalTab clientTab = clientTab(tab);
        if (clientTab != null) {
            clientTab.onSelected(contextFor(tab, scrollFor(tab)));
        }
        clampScroll(tab);
        clampCommandStackScroll();
        if (initialTabSelected && previousTab != activeTab) {
            playUiSound(1.15F);
        }
    }

    private void playUiSound(float pitch) {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(TerminalThemedSounds.click(), pitch, 0.35F));
    }

    private boolean selectGroupOffset(List<TerminalTab> tabs, int offset) {
        List<String> groups = navigationModel.groups();
        if (groups.isEmpty()) {
            return false;
        }
        int index = Math.max(0, groups.indexOf(navigationModel.activeGroup(activeTab)));
        String group = groups.get(Math.floorMod(index + offset, groups.size()));
        selectTab(navigationModel.firstTabInGroup(group), tabs);
        return true;
    }

    private boolean selectVisibleTabOffset(List<TerminalTab> tabs, int offset) {
        List<Integer> visible = visibleTabIndexes(tabs);
        if (visible.isEmpty()) {
            return false;
        }
        int current = visible.indexOf(activeTab);
        if (current < 0) {
            current = 0;
        }
        selectTab(visible.get(Math.floorMod(current + offset, visible.size())), tabs);
        return true;
    }

    private boolean selectActiveGroupPageOffset(List<TerminalTab> tabs, int offset) {
        List<PageTabChoice> choices = activeGroupPageTabs(tabs);
        if (choices.isEmpty()) {
            return false;
        }
        int current = 0;
        for (int i = 0; i < choices.size(); i++) {
            if (choices.get(i).index() == activeTab) {
                current = i;
                break;
            }
        }
        selectTab(choices.get(Math.floorMod(current + offset, choices.size())).index(), tabs);
        return true;
    }

    private List<PageTabChoice> activeGroupPageTabs(List<TerminalTab> tabs) {
        if (tabs.isEmpty()) {
            return List.of();
        }
        String activeGroup = navigationModel.activeGroup(activeTab);
        String activeChapter = navigationModel.activeChapterId(activeTab);
        List<PageTabChoice> choices = new ArrayList<>();
        for (TerminalNavigationModel.IndexedTab entry : navigationModel.directTabsInGroup(activeGroup)) {
            addPageTabChoice(choices, entry.index(), entry.tab().chrome().shortTitle(),
                    entry.tab().chrome().summary(), entry.tab().descriptor().accentColor());
        }
        for (TerminalNavigationModel.ChapterGroup chapter : navigationModel.chaptersInGroup(activeGroup)) {
            if (chapter.tabs().isEmpty()) {
                continue;
            }
            if (chapter.id().equals(activeChapter) && chapter.tabs().size() > 1) {
                for (TerminalNavigationModel.IndexedTab entry : chapter.tabs()) {
                    addPageTabChoice(choices, entry.index(), entry.tab().chrome().shortTitle(),
                            entry.tab().chrome().summary(), entry.tab().descriptor().accentColor());
                }
            } else {
                TerminalNavigationModel.IndexedTab entry = chapter.tabs().get(0);
                addPageTabChoice(choices, entry.index(), chapter.title(),
                        chapter.rawTitle(), chapter.accent());
            }
        }
        return List.copyOf(choices);
    }

    private static void addPageTabChoice(List<PageTabChoice> choices, int index, String label, String summary, int accent) {
        for (PageTabChoice choice : choices) {
            if (choice.index() == index) {
                return;
            }
        }
        choices.add(new PageTabChoice(index, label == null || label.isBlank() ? "Page" : label, summary, accent));
    }

    private int firstVisibleTabIndex(List<TerminalTab> tabs) {
        List<Integer> visible = visibleTabIndexes(tabs);
        return visible.isEmpty() ? 0 : visible.get(0);
    }

    private int lastVisibleTabIndex(List<TerminalTab> tabs) {
        List<Integer> visible = visibleTabIndexes(tabs);
        return visible.isEmpty() ? Math.max(0, tabs.size() - 1) : visible.get(visible.size() - 1);
    }

    private List<Integer> visibleTabIndexes(List<TerminalTab> tabs) {
        if (tabs.isEmpty()) {
            return List.of();
        }
        boolean chapterTabsVisible = chapterProgressExpanded || navigationModel.tabHasChapter(activeTab);
        List<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < tabs.size(); i++) {
            if (!navigationModel.tabHasChapter(i) || chapterTabsVisible) {
                indexes.add(i);
            }
        }
        return List.copyOf(indexes);
    }

    private void normalizeActiveTab(List<TerminalTab> tabs) {
        if (tabs.isEmpty()) {
            activeTab = 0;
            return;
        }
        if (!initialTabSelected) {
            initialTabSelected = true;
            Identifier target = rememberedTabId == null ? OVERVIEW_TAB : rememberedTabId;
            int index = findTab(tabs, target);
            if (index < 0 && target != OVERVIEW_TAB) {
                index = findTab(tabs, OVERVIEW_TAB);
            }
            selectTab(index < 0 ? 0 : index, tabs);
            return;
        }
        if (activeTab >= tabs.size()) {
            activeTab = tabs.size() - 1;
        }
        clampCommandStackScroll();
    }

    private int findTab(List<TerminalTab> tabs, Identifier id) {
        for (int i = 0; i < tabs.size(); i++) {
            if (tabs.get(i).descriptor().id().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    private int chromeColor(TerminalTab tab) {
        return tab == null ? theme.accentColor() : tab.descriptor().accentColor();
    }

    private int scrollFor(TerminalTab tab) {
        return tabScroll.getOrDefault(tab.descriptor().id(), 0);
    }

    private void setScroll(TerminalTab tab, int value) {
        int next = Math.max(0, Math.min(value, maxScroll(tab)));
        tabScroll.put(tab.descriptor().id(), next);
    }

    private void clampScroll(TerminalTab tab) {
        setScroll(tab, scrollFor(tab));
    }

    private int maxScroll(TerminalTab tab) {
        ClientTerminalTab clientTab = clientTab(tab);
        int contentHeight = Math.max(0, clientTab == null ? contentBodyH() : clientTab.contentHeight(contextFor(tab, 0)));
        int contentTrimH = zoomed(20, 16);
        int viewportH = Math.max(80, contentBodyH() - contentTrimH);
        return Math.max(0, contentHeight - viewportH);
    }

    private void layout() {
        LayoutMetrics metrics = computeLayoutMetrics(width, height, theme.panelMaxWidth(), theme.panelMaxHeight(),
                interfaceDensity(), terminalZoom(), commandStackEffectivelyCollapsed(), navigationStyle());
        panelX = metrics.panelX();
        panelY = metrics.panelY();
        panelW = metrics.panelW();
        panelH = metrics.panelH();
        layoutProfile = metrics.layoutProfile();
        commandStackNavigation = !useCompactTopNavigation();
        sidebarNavigation = commandStackNavigation;
        groupRailX = metrics.groupRailX();
        groupRailY = metrics.groupRailY();
        groupRailW = metrics.groupRailW();
        groupRailH = metrics.groupRailH();
        pageRailX = groupRailX;
        pageRailY = groupRailY;
        pageRailW = groupRailW;
        pageRailH = groupRailH;
        navX = pageRailX;
        navY = pageRailY;
        navW = pageRailW;
        navH = pageRailH;
        collapseToggleX = metrics.collapseToggleX();
        collapseToggleY = metrics.collapseToggleY();
        collapseToggleW = metrics.collapseToggleW();
        collapseToggleH = metrics.collapseToggleH();
        contentX = metrics.contentX();
        contentY = metrics.contentY();
        contentW = metrics.contentW();
        contentH = metrics.contentH();
        clampActiveContentScroll();
        clampCommandStackScroll();
    }

    private static LayoutMetrics computeLayoutMetrics(
            int screenWidth,
            int screenHeight,
            int panelMaxWidth,
            int panelMaxHeight,
            TerminalClientOptions.InterfaceDensity densityOption,
            TerminalClientOptions.TerminalZoom zoom,
            boolean commandStackCollapsed,
            TerminalClientOptions.NavigationStyle navigationStyle) {
        TerminalClientOptions.InterfaceDensity resolvedDensity = densityOption == null
                ? TerminalClientOptions.InterfaceDensity.BALANCED
                : densityOption;
        TerminalClientOptions.TerminalZoom resolvedZoom = zoom == null
                ? TerminalClientOptions.TerminalZoom.ZOOM_100
                : zoom;
        TerminalClientOptions.NavigationStyle resolvedNavigation = navigationStyle == null
                ? TerminalClientOptions.NavigationStyle.APP_HUB
                : navigationStyle;
        boolean compactTopNavigation = resolvedNavigation == TerminalClientOptions.NavigationStyle.COMPACT_TOP;
        boolean collapsedNavigation = resolvedNavigation == TerminalClientOptions.NavigationStyle.SIDEBAR_HUB
                || commandStackCollapsed;
        boolean cyberglass = TerminalClientOptions.cyberglassActive();
        int density = resolvedDensity.compactness();
        int minDimension = Math.min(screenWidth, screenHeight);
        int baseMargin = Math.max(8, Math.min(16, minDimension / 68));
        int margin = Math.min(Math.max(baseMargin + density * 8 + (cyberglass ? 2 : 0), baseMargin),
                Math.max(10, minDimension / 7));
        int minPanelW = shellSized(340, 300);
        int minPanelH = shellSized(280, 240);
        int usableW = Math.max(minPanelW, screenWidth - margin * 2);
        int usableH = Math.max(minPanelH, screenHeight - margin * 2);
        int maxPanelW = Math.max(minPanelW, shellSized(Math.max(360, panelMaxWidth - density * 36), minPanelW));
        int maxPanelH = Math.max(minPanelH, shellSized(Math.max(270, panelMaxHeight - density * 24), minPanelH));
        int panelW = Math.min(maxPanelW, usableW);
        int panelH = Math.min(maxPanelH, usableH);
        TerminalLayoutProfile layoutProfile = panelW < 660
                ? TerminalLayoutProfile.COMPACT_STACK
                : panelW < 980 ? TerminalLayoutProfile.MEDIUM_CAROUSEL : TerminalLayoutProfile.APP_HUB;
        int panelX = (screenWidth - panelW) / 2;
        int panelY = (screenHeight - panelH) / 2;
        int shellHeaderH = shellHeaderHeight(density);
        int shellFooterH = shellFooterHeight(density);
        int footerTop = panelY + panelH - shellFooterH - shellSized(4, 3);
        int horizontalPad = shellSized(panelW < 560 ? 12 : 18 + density + (cyberglass ? 4 : 0), 8);
        int groupRailX = panelX + horizontalPad;
        int groupRailY = panelY + shellHeaderH + shellSized(cyberglass ? 10 : 6, 4);
        int groupRailW;
        if (collapsedNavigation) {
            groupRailW = panelW >= 760 ? shellSized(58, 46) : shellSized(50, 42);
        } else if (panelW >= 980) {
            groupRailW = Math.max(shellSized(cyberglass ? 188 : 164, 148),
                    Math.min(shellSized(cyberglass ? 220 : 184, 164), panelW / 6 - density * 4));
        } else if (panelW >= 760) {
            groupRailW = Math.max(shellSized(cyberglass ? 172 : 150, 136),
                    Math.min(shellSized(cyberglass ? 198 : 170, 150), panelW / 5 - density * 3));
        } else {
            groupRailW = Math.max(shellSized(96, 84), Math.min(shellSized(128, 108), panelW / 5));
        }
        int groupRailH = Math.max(shellSized(210, 168), footerTop - groupRailY - shellSized(8, 6));
        int collapseToggleH = contentZoomed(18, 14, resolvedZoom);
        int collapseToggleW = collapsedNavigation
                ? Math.max(contentZoomed(30, 24, resolvedZoom), groupRailW - contentZoomed(18, 14, resolvedZoom))
                : contentZoomed(26, 22, resolvedZoom);
        int collapseToggleX = collapsedNavigation
                ? groupRailX + contentZoomed(9, 7, resolvedZoom)
                : groupRailX + groupRailW - collapseToggleW - contentZoomed(8, 6, resolvedZoom);
        int collapseToggleY = groupRailY + contentZoomed(8, 6, resolvedZoom);
        int gap = collapsedNavigation
                ? shellSized(Math.max(8, 10 - density), 6)
                : panelW >= 760 ? shellSized(Math.max(cyberglass ? 16 : 10, (cyberglass ? 18 : 14) - density * 2), 8)
                        : shellSized(cyberglass ? 10 : 8, 6);
        int contentX = groupRailX + groupRailW + gap;
        int contentY = groupRailY;
        int contentW = Math.max(shellSized(panelW < 520 ? 180 : 260, panelW < 520 ? 150 : 210),
                panelX + panelW - contentX - horizontalPad);
        int contentH = Math.max(shellSized(168, 136), footerTop - contentY - (cyberglass ? 12 : 8));
        if (compactTopNavigation) {
            int navH = shellSized(Math.max(cyberglass ? 28 : 22, (cyberglass ? 32 : 28) - density * 2), 20);
            groupRailW = Math.max(shellSized(220, 180), panelW - horizontalPad * 2);
            groupRailH = navH;
            collapseToggleW = 0;
            collapseToggleH = 0;
            collapseToggleX = groupRailX;
            collapseToggleY = groupRailY;
            contentX = groupRailX;
            contentY = groupRailY + navH + shellSized(Math.max(cyberglass ? 12 : 8, 10 - density), 6);
            contentW = Math.max(shellSized(panelW < 520 ? 220 : 300, panelW < 520 ? 180 : 240),
                    panelX + panelW - contentX - horizontalPad);
            contentH = Math.max(shellSized(168, 136), footerTop - contentY - (cyberglass ? 12 : 8));
        }
        int contentPadX = contentZoomed(cyberglass ? 12 : 10, cyberglass ? 10 : 8, resolvedZoom);
        int contentPadY = contentZoomed(cyberglass ? 12 : 10, cyberglass ? 10 : 8, resolvedZoom);
        int contentTrimW = contentZoomed(cyberglass ? 28 : 22, cyberglass ? 20 : 16, resolvedZoom);
        int contentTrimH = contentZoomed(cyberglass ? 26 : 20, cyberglass ? 20 : 16, resolvedZoom);
        return new LayoutMetrics(
                panelX,
                panelY,
                panelW,
                panelH,
                contentX,
                contentY,
                contentW,
                contentH,
                contentX + contentPadX,
                contentY + contentPadY,
                Math.max(80, contentW - contentTrimW),
                Math.max(80, contentH - contentTrimH),
                groupRailX,
                groupRailY,
                groupRailW,
                groupRailH,
                collapseToggleX,
                collapseToggleY,
                collapseToggleW,
                collapseToggleH,
                shellHeaderH,
                shellFooterH,
                layoutProfile);
    }

    private void clampActiveContentScroll() {
        List<TerminalTab> tabs = navigationModel.tabs();
        if (activeTab >= 0 && activeTab < tabs.size()) {
            clampScroll(tabs.get(activeTab));
        }
    }

    private static ClientTerminalTab clientTab(TerminalTab tab) {
        return tab instanceof ClientTerminalTab clientTab ? clientTab : null;
    }

    private String trim(String text, int maxWidth) {
        return TerminalUi.trim(font, text, maxWidth);
    }

    private static int pulseColor(int tick, int low, int high, int period) {
        float phase = (float) ((Math.sin((tick % period) / (double) period * Math.PI * 2.0D) + 1.0D) * 0.5D);
        int la = (low >>> 24) & 0xFF;
        int ha = (high >>> 24) & 0xFF;
        int alpha = Math.round(la + (ha - la) * phase);
        return (alpha << 24) | (high & 0x00FFFFFF);
    }

}
