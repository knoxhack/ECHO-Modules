package com.knoxhack.echoterminal.client.screencore;

import com.knoxhack.echoscreencore.api.EchoDataContext;
import com.knoxhack.echoscreencore.api.EchoScreens;
import com.knoxhack.echocore.api.EchoRuntimeModules;
import com.knoxhack.echoterminal.EchoTerminal;
import com.knoxhack.echoterminal.EchoTerminalClient;
import com.knoxhack.echoterminal.api.TerminalNavigationProfiles;
import com.knoxhack.echoterminal.api.TerminalScreenCorePageMetadata;
import com.knoxhack.echoterminal.api.TerminalTab;
import com.knoxhack.echoterminal.api.TerminalTabRegistry;
import com.knoxhack.echoterminal.client.screen.EchoTerminalScreen;
import com.knoxhack.echoterminal.client.screen.EchoTerminalScreenProvider;
import com.knoxhack.echoterminal.client.screen.EchoTerminalScreens;
import com.knoxhack.echoterminal.client.screen.TerminalClientOptions;
import com.knoxhack.echoterminal.menu.EchoTerminalMenu;
import com.knoxhack.echoterminal.mission.MainSurvivalQuestProvider;
import com.knoxhack.echoterminal.mission.VanillaJourneyProvider;
import dev.echo.nativeplatform.contracts.EchoNativeLoadStatus;
import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public final class TerminalScreenCoreBridge {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);
    private static final Identifier DEFAULT_TAB = id("overview");
    private static final Identifier FALLBACK_PAGE = page("terminal_fallback");
    private static final Map<Identifier, Identifier> BUILTIN_TAB_PAGES = builtinTabPages();
    private static WeakReference<TerminalScreenCoreScreen> activeScreen = new WeakReference<>(null);
    private static final AtomicBoolean WARNED_SCREENCORE_FALLBACK = new AtomicBoolean(false);
    private static final AtomicBoolean LOGGED_SCREENCORE_OPEN = new AtomicBoolean(false);
    private static final AtomicBoolean LOGGED_LEGACY_OPEN = new AtomicBoolean(false);

    private TerminalScreenCoreBridge() {
    }

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }
        TerminalScreenCoreDataProviders.register();
        TerminalScreenCoreActions.register();
        EchoScreens.registerInvalidationHandler(TerminalScreenCoreBridge::invalidateOpenScreen);
        EchoTerminalScreens.registerPrimary(new EchoTerminalScreenProvider() {
            @Override
            public AbstractContainerScreen<EchoTerminalMenu> create(
                    EchoTerminalMenu menu, Inventory playerInventory, Component title) {
                if (!safeShouldUseScreenCoreTerminal()) {
                    if (LOGGED_LEGACY_OPEN.compareAndSet(false, true)) {
                        EchoTerminal.LOGGER.info(
                                "Opening ECHO Terminal legacy renderer. ScreenCore present={}, useScreenCore={}, matchExistingLayout={}, experimentalTabs={}, cyberglassTheme={}.",
                                screenCorePresent(),
                                TerminalClientOptions.useScreenCore(),
                                TerminalClientOptions.screenCoreMatchExistingLayout(),
                                TerminalClientOptions.screenCoreExperimentalTabs(),
                                TerminalClientOptions.useCyberglassScreenCoreTheme());
                    }
                    return null;
                }
                try {
                    if (LOGGED_SCREENCORE_OPEN.compareAndSet(false, true)) {
                        EchoTerminal.LOGGER.info(
                                "Opening ECHO Terminal ScreenCore shell. activeTab={}, matchExistingLayout={}, experimentalTabs={}.",
                                DEFAULT_TAB,
                                TerminalClientOptions.screenCoreMatchExistingLayout(),
                                TerminalClientOptions.screenCoreExperimentalTabs());
                    }
                    return new TerminalScreenCoreScreen(menu, playerInventory, title, DEFAULT_TAB);
                } catch (RuntimeException | LinkageError exception) {
                    if (WARNED_SCREENCORE_FALLBACK.compareAndSet(false, true)) {
                        EchoTerminal.LOGGER.warn("ECHO Terminal ScreenCore cyberglass shell failed; opening legacy renderer.",
                                exception);
                    }
                    return new EchoTerminalScreen(menu, playerInventory, title);
                }
            }

            @Override
            public boolean isTerminalScreen(Screen screen) {
                return screen instanceof TerminalScreenCoreScreen;
            }
        });
        EchoTerminal.LOGGER.info("ECHO Terminal ScreenCore bridge registered; fallback renderer remains available.");
    }

    public static boolean screenCorePresent() {
        try {
            return EchoRuntimeModules.isLoaded("echoscreencore");
        } catch (RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    public static boolean shouldUseScreenCoreTerminal() {
        if (!screenCorePresent() || !TerminalClientOptions.useScreenCore()) {
            return false;
        }
        if (TerminalClientOptions.cyberglassActive()
                && TerminalClientOptions.useCyberglassScreenCoreTheme()) {
            return true;
        }
        return TerminalClientOptions.screenCoreMatchExistingLayout()
                && TerminalClientOptions.screenCoreExperimentalTabs();
    }

    private static boolean safeShouldUseScreenCoreTerminal() {
        try {
            return shouldUseScreenCoreTerminal();
        } catch (RuntimeException | LinkageError exception) {
            if (WARNED_SCREENCORE_FALLBACK.compareAndSet(false, true)) {
                EchoTerminal.LOGGER.warn("ECHO Terminal ScreenCore routing check failed; opening legacy renderer.",
                        exception);
            }
            return false;
        }
    }

    public static EchoDataContext screenContext(Identifier activeTabId) {
        Identifier tabId = normalizeTab(activeTabId);
        Identifier pageId = pageForTab(tabId);
        return EchoDataContext.empty()
                .missingPlaceholder("")
                .put("terminal.activeTabId", tabId.toString())
                .put("terminal.activePageId", pageId.toString())
                .put("terminal.screenCore.present", screenCorePresent())
                .put("terminal.screenCore.useScreenCore", TerminalClientOptions.useScreenCore())
                .put("terminal.screenCore.matchExistingLayout", TerminalClientOptions.screenCoreMatchExistingLayout())
                .put("terminal.screenCore.experimentalTabs", TerminalClientOptions.screenCoreExperimentalTabs())
                .put("terminal.screenCore.debug", TerminalClientOptions.screenCoreDebug())
                .put("terminal.screenCore.fallbackAvailable", true);
    }

    public static Identifier pageForTab(Identifier tabId) {
        if (tabId == null) {
            return page("terminal_overview");
        }
        Optional<Identifier> externalPage = tab(tabId)
                .filter(TerminalScreenCorePageMetadata.class::isInstance)
                .map(TerminalScreenCorePageMetadata.class::cast)
                .map(TerminalScreenCorePageMetadata::screenCorePageId)
                .filter(Objects::nonNull);
        if (externalPage.isPresent()) {
            return externalPage.get();
        }
        return BUILTIN_TAB_PAGES.getOrDefault(tabId, FALLBACK_PAGE);
    }

    public static Identifier normalizeTab(Identifier tabId) {
        if (tabId != null && TerminalTabRegistry.tabs().stream()
                .anyMatch(tab -> tab != null && tab.descriptor().id().equals(tabId))) {
            return tabId;
        }
        return DEFAULT_TAB;
    }

    public static Optional<TerminalTab> tab(Identifier tabId) {
        if (tabId == null) {
            return Optional.empty();
        }
        return TerminalTabRegistry.tabs().stream()
                .filter(tab -> tab != null && tab.descriptor().id().equals(tabId))
                .findFirst();
    }

    public static List<TerminalTab> tabs() {
        return TerminalTabRegistry.tabs();
    }

    public static String migrationState(Identifier tabId) {
        if (tabId == null) {
            return "fallback";
        }
        if (BUILTIN_TAB_PAGES.containsKey(tabId)) {
            return TerminalClientOptions.screenCoreExperimentalTabs() ? "screencore" : "fallback-default";
        }
        if (tab(tabId).filter(TerminalScreenCorePageMetadata.class::isInstance).isPresent()) {
            return TerminalClientOptions.screenCoreExperimentalTabs()
                    ? "external-screencore"
                    : "external-fallback-default";
        }
        return "external-fallback";
    }

    public static boolean openLegacyRenderer() {
        TerminalScreenCoreScreen screen = activeScreen.get();
        return screen != null && screen.openLegacyRenderer();
    }

    public static boolean openTab(Identifier tabId) {
        TerminalTabRegistry.ensureSorted();
        Identifier normalizedTab = normalizeTab(tabId);
        if (tabId != null && !normalizedTab.equals(tabId)) {
            return false;
        }
        if (!safeShouldUseScreenCoreTerminal()) {
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return false;
        }
        if (minecraft.screen != null && !EchoTerminalScreens.isManagedTerminalScreen(minecraft.screen)) {
            return false;
        }
        TerminalScreenCoreScreen current = activeScreen.get();
        if (current != null && minecraft.screen == current) {
            EchoNativeLoadStatus lifecycleStatus = EchoTerminalClient.publishNativeScreenLifecycle(
                    "open",
                    "terminal.screencore.open_tab",
                    current.getClass().getName(),
                    Map.of(
                            "targetScreenClass", TerminalScreenCoreScreen.class.getName(),
                            "transitionSource", "terminal_screencore_bridge_open_tab",
                            "nextPage", pageForTab(normalizedTab).toString(),
                            "nextTab", normalizedTab.toString(),
                            "replacingActiveScreen", true
                    ));
            if (EchoTerminalClient.nativeLoaderClientActiveForScreens()
                    && lifecycleStatus != EchoNativeLoadStatus.MUTATED) {
                return false;
            }
            minecraft.setScreen(new TerminalScreenCoreScreen(
                    current.terminalMenu(), current.playerInventory(), current.screenTitle(), normalizedTab));
            return true;
        }
        EchoNativeLoadStatus lifecycleStatus = EchoTerminalClient.publishNativeScreenLifecycle(
                "open",
                "terminal.screencore.open_tab",
                TerminalScreenCoreScreen.class.getName(),
                Map.of(
                        "targetScreenClass", TerminalScreenCoreScreen.class.getName(),
                        "transitionSource", "terminal_screencore_bridge_open_tab",
                        "nextPage", pageForTab(normalizedTab).toString(),
                        "nextTab", normalizedTab.toString(),
                        "replacingActiveScreen", false
                ));
        if (EchoTerminalClient.nativeLoaderClientActiveForScreens()
                && lifecycleStatus != EchoNativeLoadStatus.MUTATED) {
            return false;
        }
        minecraft.setScreen(new TerminalScreenCoreScreen(
                new EchoTerminalMenu(0, minecraft.player.getInventory()),
                minecraft.player.getInventory(),
                Component.translatable("container.echoterminal.echo_terminal"),
                normalizedTab));
        return true;
    }

    public static boolean open() {
        return openTab(DEFAULT_TAB);
    }

    public static String navigationGroup(Identifier tabId) {
        return tab(tabId)
                .map(TerminalNavigationProfiles::profileFor)
                .map(profile -> profile.section().key())
                .orElse("terminal");
    }

    public static void markActive(TerminalScreenCoreScreen screen) {
        activeScreen = new WeakReference<>(screen);
    }

    public static void clearActive(TerminalScreenCoreScreen screen) {
        TerminalScreenCoreScreen current = activeScreen.get();
        if (current == screen) {
            activeScreen = new WeakReference<>(null);
        }
    }

    private static void invalidateOpenScreen(Identifier pageId) {
        TerminalScreenCoreScreen screen = activeScreen.get();
        if (screen != null && (pageId == null || pageId.equals(screen.pageId()))) {
            screen.markDataDirty();
        }
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoTerminal.MODID, path);
    }

    private static Identifier page(String path) {
        return Identifier.fromNamespaceAndPath(EchoTerminal.MODID, path);
    }

    private static Map<Identifier, Identifier> builtinTabPages() {
        Map<Identifier, Identifier> pages = new LinkedHashMap<>();
        pages.put(id("overview"), page("terminal_overview"));
        pages.put(id("mission_graph"), page("terminal_mission_graph"));
        pages.put(MainSurvivalQuestProvider.TAB_ID, page("terminal_mission_browser"));
        pages.put(VanillaJourneyProvider.TAB_ID, page("terminal_mission_browser"));
        pages.put(id("addons"), page("terminal_addons"));
        pages.put(id("recipe_index"), page("terminal_recipe_index"));
        pages.put(id("route_records"), page("terminal_route_records"));
        pages.put(id("discovery_grid"), page("terminal_discovery_grid"));
        pages.put(id("faction_atlas"), page("terminal_faction_atlas"));
        pages.put(id("archives"), page("terminal_archives"));
        pages.put(id("vitals"), page("terminal_vitals"));
        pages.put(id("reward_inbox"), page("terminal_reward_inbox"));
        pages.put(id("data_core"), page("terminal_data_core"));
        pages.put(id("settings"), page("terminal_settings"));
        pages.put(Identifier.fromNamespaceAndPath("echoscriptcore", "terminal_browser"), page("terminal_scriptcore_browser"));
        return Map.copyOf(pages);
    }
}
