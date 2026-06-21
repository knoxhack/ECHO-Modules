package com.knoxhack.echoterminal.test;

import com.knoxhack.echorendercore.client.RenderCoreScreenChromeStyle;
import com.knoxhack.echorendercore.client.RenderCoreScreenFrameOptions;
import com.knoxhack.echoterminal.EchoTerminal;
import com.knoxhack.echoterminal.BuiltinTerminalCommonIntegration;
import com.knoxhack.echoterminal.api.ClientTerminalTab;
import com.knoxhack.echoterminal.api.TerminalActionRegistry;
import com.knoxhack.echoterminal.api.TerminalAddonGuide;
import com.knoxhack.echoterminal.api.TerminalAddonInfo;
import com.knoxhack.echoterminal.api.TerminalAddonInfoProvider;
import com.knoxhack.echoterminal.api.TerminalAddonInfoRegistry;
import com.knoxhack.echoterminal.api.TerminalAddonLink;
import com.knoxhack.echoterminal.api.TerminalAddonMetric;
import com.knoxhack.echoterminal.api.TerminalAddonSection;
import com.knoxhack.echoterminal.api.TerminalIcon;
import com.knoxhack.echoterminal.api.TerminalLayoutProfile;
import com.knoxhack.echoterminal.api.TerminalPageLayout;
import com.knoxhack.echoterminal.api.TerminalNavigationProfile;
import com.knoxhack.echoterminal.api.TerminalNavigationProfiles;
import com.knoxhack.echoterminal.api.TerminalNavigationSection;
import com.knoxhack.echoterminal.api.TerminalRenderContext;
import com.knoxhack.echoterminal.api.TerminalTab;
import com.knoxhack.echoterminal.api.TerminalTabChrome;
import com.knoxhack.echoterminal.api.TerminalTabDescriptor;
import com.knoxhack.echoterminal.api.TerminalTabRegistry;
import com.knoxhack.echoterminal.api.TerminalUi;
import com.knoxhack.echoterminal.api.TerminalVisualAssets;
import com.knoxhack.echoterminal.api.theme.BuiltinTerminalThemes;
import com.knoxhack.echoterminal.api.theme.TerminalChapterStyle;
import com.knoxhack.echoterminal.api.theme.TerminalIconKey;
import com.knoxhack.echoterminal.api.theme.TerminalIconSet;
import com.knoxhack.echoterminal.api.theme.TerminalTheme;
import com.knoxhack.echoterminal.api.theme.TerminalThemeContext;
import com.knoxhack.echoterminal.api.theme.TerminalThemeRegistry;
import com.knoxhack.echoterminal.api.mission.TerminalMissionAction;
import com.knoxhack.echoterminal.api.mission.TerminalMissionActions;
import com.knoxhack.echoterminal.api.mission.TerminalMissionChapter;
import com.knoxhack.echoterminal.api.mission.TerminalMissionDefinition;
import com.knoxhack.echoterminal.api.mission.TerminalMissionIntelKind;
import com.knoxhack.echoterminal.api.mission.TerminalMissionIntelUnlock;
import com.knoxhack.echoterminal.api.mission.TerminalMissionPresentation;
import com.knoxhack.echoterminal.api.mission.TerminalMissionProvider;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRegistry;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRequirement;
import com.knoxhack.echoterminal.api.mission.TerminalMissionReward;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRole;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRoutePlacement;
import com.knoxhack.echoterminal.api.mission.TerminalMissionSnapshot;
import com.knoxhack.echoterminal.api.mission.TerminalMissionStatus;
import com.knoxhack.echoterminal.api.recipe.TerminalRecipeCategory;
import com.knoxhack.echoterminal.api.recipe.TerminalRecipeEntry;
import com.knoxhack.echoterminal.api.recipe.TerminalRecipeNote;
import com.knoxhack.echoterminal.api.recipe.TerminalRecipeProvider;
import com.knoxhack.echoterminal.api.recipe.TerminalRecipeRegistry;
import com.knoxhack.echoterminal.api.recipe.TerminalRecipeSnapshot;
import com.knoxhack.echoterminal.api.recipe.TerminalRecipeSlot;
import com.knoxhack.echoterminal.block.entity.EchoTerminalBlockEntity;
import com.knoxhack.echoterminal.client.BuiltinTerminalTabs;
import com.knoxhack.echoterminal.client.discovery.DiscoveryGridTab;
import com.knoxhack.echoterminal.client.discovery.DiscoveryToastHud;
import com.knoxhack.echoterminal.client.hud.TerminalHudNotice;
import com.knoxhack.echoterminal.client.hud.TerminalHudNoticeSurface;
import com.knoxhack.echoterminal.client.mission.TerminalMissionBrowser;
import com.knoxhack.echoterminal.client.mission.TerminalMissionHudController;
import com.knoxhack.echoterminal.client.mission.TerminalMissionNotice;
import com.knoxhack.echoterminal.client.mission.TerminalMissionNoticeType;
import com.knoxhack.echoterminal.client.recipe.TerminalRecipeIndexTab;
import com.knoxhack.echoterminal.client.screen.EchoTerminalScreen;
import com.knoxhack.echoterminal.client.screen.EchoTerminalScreens;
import com.knoxhack.echoterminal.client.screen.TerminalClientOptions;
import com.knoxhack.echoterminal.client.screen.TerminalScrollbar;
import com.knoxhack.echoterminal.client.screen.TerminalScreenTheme;
import com.knoxhack.echoterminal.integration.TerminalRenderCoreClientIntegration;
import com.knoxhack.echoterminal.integration.TerminalRuntimeSpineBridge;
import com.knoxhack.echoterminal.discovery.TerminalDiscoveryProvider;
import com.knoxhack.echoterminal.menu.EchoTerminalMenu;
import com.knoxhack.echoterminal.mission.MainSurvivalQuestProvider;
import com.knoxhack.echoterminal.mission.VanillaJourneyData;
import com.knoxhack.echoterminal.mission.VanillaJourneyProvider;
import com.knoxhack.echoterminal.network.TerminalActionPacket;
import com.knoxhack.echoterminal.player.TerminalPlayerData;
import com.knoxhack.echoterminal.registry.ModBlocks;
import com.knoxhack.echoterminal.service.EchoTerminalCoreServices;
import com.echoplatform.echocore.api.EchoAddonChapter;
import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.EchoDiscoveryCategory;
import com.echoplatform.echocore.api.EchoDiscoveryEntry;
import com.echoplatform.echocore.api.EchoDiscoveryProvider;
import com.echoplatform.echocore.api.EchoDiscoveryState;
import com.echoplatform.echocore.api.EchoResolvedDiscoveryEntry;
import com.echoplatform.echocore.api.EchoRuntimeSpineBus;
import com.echoplatform.echocore.api.EchoRuntimeSpineEvent;
import com.echoplatform.echocore.api.EchoRouteRecord;
import com.echoplatform.echocore.api.EchoServiceRegistry;
import com.echoplatform.echocore.api.config.EchoConfigApplyResult;
import com.echoplatform.echocore.api.config.EchoConfigCategory;
import com.echoplatform.echocore.api.config.EchoConfigEntry;
import com.echoplatform.echocore.api.config.EchoConfigEntrySnapshot;
import com.echoplatform.echocore.api.config.EchoConfigModule;
import com.echoplatform.echocore.api.config.EchoConfigModuleSnapshot;
import com.echoplatform.echocore.api.config.EchoConfigProvider;
import com.echoplatform.echocore.api.config.EchoConfigRegistry;
import com.echoplatform.echocore.api.config.EchoConfigSide;
import com.echoplatform.echocore.api.config.EchoConfigValueKind;
import com.echoplatform.echocore.api.network.EchoDiscoveryToast;
import com.echoplatform.echocore.api.network.EchoPacketDirection;
import com.echoplatform.echocore.api.network.EchoPacketKind;
import com.knoxhack.echocore.discovery.EchoDiscoveryData;
import com.knoxhack.echonetcore.client.EchoNetClientActions;
import com.knoxhack.echonetcore.config.EchoNetCoreConfig;
import com.knoxhack.echonetcore.network.EchoNetDebug;
import com.knoxhack.echoterminal.network.TerminalConfigActionPacket;
import com.knoxhack.echoterminal.network.TerminalConfigClientState;
import com.knoxhack.echoterminal.network.TerminalConfigSyncPacket;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.lwjgl.glfw.GLFW;

public final class ModGameTests {
    private static final long TERMINAL_TEXTURE_BUDGET_BYTES = 25L * 1024L * 1024L;

    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, EchoTerminal.MODID);

    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_API_IDS =
            TEST_FUNCTIONS.register("terminal_api_ids", () -> ModGameTests::terminalApiIds);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_TAB_REGISTRY =
            TEST_FUNCTIONS.register("terminal_tab_registry", () -> ModGameTests::terminalTabRegistry);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_MISSION_REGISTRY =
            TEST_FUNCTIONS.register("terminal_mission_registry", () -> ModGameTests::terminalMissionRegistry);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_ADDON_INFO_REGISTRY =
            TEST_FUNCTIONS.register("terminal_addon_info_registry", () -> ModGameTests::terminalAddonInfoRegistry);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_ADDON_GUIDE_ORDERING =
            TEST_FUNCTIONS.register("terminal_addon_guide_ordering", () -> ModGameTests::terminalAddonGuideOrdering);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_NAVIGATION_PROFILES =
            TEST_FUNCTIONS.register("terminal_navigation_profiles", () -> ModGameTests::terminalNavigationProfiles);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_DISCOVERY_GRID_FILTERS =
            TEST_FUNCTIONS.register("terminal_discovery_grid_filters", () -> ModGameTests::terminalDiscoveryGridFilters);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_DISCOVERY_GRID_ROUTE_STATE =
            TEST_FUNCTIONS.register("terminal_discovery_grid_route_state", () -> ModGameTests::terminalDiscoveryGridRouteState);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_DISCOVERY_GRID_BATCH_RESOLUTION =
            TEST_FUNCTIONS.register("terminal_discovery_grid_batch_resolution", () -> ModGameTests::terminalDiscoveryGridBatchResolution);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_RENDER_CONTEXT_NAVIGATION =
            TEST_FUNCTIONS.register("terminal_render_context_navigation", () -> ModGameTests::terminalRenderContextNavigation);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_THEME_REGISTRY =
            TEST_FUNCTIONS.register("terminal_theme_registry", () -> ModGameTests::terminalThemeRegistry);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_THEME_ICON_FALLBACK =
            TEST_FUNCTIONS.register("terminal_theme_icon_fallback", () -> ModGameTests::terminalThemeIconFallback);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_THEME_CHAPTER_STYLE =
            TEST_FUNCTIONS.register("terminal_theme_chapter_style", () -> ModGameTests::terminalThemeChapterStyle);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_THEME_RESOURCES =
            TEST_FUNCTIONS.register("terminal_theme_resources", () -> ModGameTests::terminalThemeResources);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_THEME_SELECTION =
            TEST_FUNCTIONS.register("terminal_theme_selection", () -> ModGameTests::terminalThemeSelection);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_CLIENT_OPTIONS_CONFIG =
            TEST_FUNCTIONS.register("terminal_client_options_config", () -> ModGameTests::terminalClientOptionsConfig);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_ZOOM_OPTIONS =
            TEST_FUNCTIONS.register("terminal_zoom_options", () -> ModGameTests::terminalZoomOptions);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_SCROLLBAR_METRICS =
            TEST_FUNCTIONS.register("terminal_scrollbar_metrics", () -> ModGameTests::terminalScrollbarMetrics);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_SCREEN_PROVIDER_PRECEDENCE =
            TEST_FUNCTIONS.register("terminal_screen_provider_precedence", () -> ModGameTests::terminalScreenProviderPrecedence);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_VISUAL_POLISH_LAYOUT =
            TEST_FUNCTIONS.register("terminal_visual_polish_layout", () -> ModGameTests::terminalVisualPolishLayout);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_SCREENCORE_TEXT_LAYOUT =
            TEST_FUNCTIONS.register("terminal_screencore_text_layout", () -> ModGameTests::terminalScreenCoreTextLayout);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_RESOURCE_NAME_CONTRACTS =
            TEST_FUNCTIONS.register("terminal_resource_name_contracts", () -> ModGameTests::terminalResourceNameContracts);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_COMMAND_DECK_PRIORITY =
            TEST_FUNCTIONS.register("terminal_command_deck_priority", () -> ModGameTests::terminalCommandDeckPriority);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_MISSION_ACTION_ROUTING =
            TEST_FUNCTIONS.register("terminal_mission_action_routing", () -> ModGameTests::terminalMissionActionRouting);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_LORE_TAXONOMY =
            TEST_FUNCTIONS.register("terminal_lore_taxonomy", () -> ModGameTests::terminalLoreTaxonomy);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_EMPTY_PROVIDER_CONTRACTS =
            TEST_FUNCTIONS.register("terminal_empty_provider_contracts", () -> ModGameTests::terminalEmptyProviderContracts);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_MAIN_SURVIVAL_ROUTE =
            TEST_FUNCTIONS.register("terminal_main_survival_route", () -> ModGameTests::terminalMainSurvivalRoute);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_MAIN_SURVIVAL_ROUTE_GATE =
            TEST_FUNCTIONS.register("terminal_main_survival_route_gate", () -> ModGameTests::terminalMainSurvivalRouteGate);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_MAIN_SURVIVAL_ROUTE_CACHE =
            TEST_FUNCTIONS.register("terminal_main_survival_route_cache", () -> ModGameTests::terminalMainSurvivalRouteCache);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_MAIN_SURVIVAL_ROUTE_BOUNDS =
            TEST_FUNCTIONS.register("terminal_main_survival_route_bounds", () -> ModGameTests::terminalMainSurvivalRouteBounds);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_REWARD_CACHE =
            TEST_FUNCTIONS.register("terminal_reward_cache", () -> ModGameTests::terminalRewardCache);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_REWARD_TRANSACTIONAL =
            TEST_FUNCTIONS.register("terminal_reward_transactional", () -> ModGameTests::terminalRewardTransactional);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_REWARD_CLAIM_FLOW =
            TEST_FUNCTIONS.register("terminal_reward_claim_flow", () -> ModGameTests::terminalRewardClaimFlow);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_RUNTIME_SPINE_ACTIONS =
            TEST_FUNCTIONS.register("terminal_runtime_spine_actions", () -> ModGameTests::terminalRuntimeSpineActions);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_REWARD_EXPLICIT_OWNER =
            TEST_FUNCTIONS.register("terminal_reward_explicit_owner", () -> ModGameTests::terminalRewardExplicitOwner);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_MENU_VALIDITY =
            TEST_FUNCTIONS.register("terminal_menu_validity", () -> ModGameTests::terminalMenuValidity);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_BASELINE_CACHE_CONTRACT =
            TEST_FUNCTIONS.register("terminal_baseline_cache_contract", () -> ModGameTests::terminalBaselineCacheContract);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_BASELINE_AUTO_REFRESH =
            TEST_FUNCTIONS.register("terminal_baseline_auto_refresh", () -> ModGameTests::terminalBaselineAutoRefresh);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_BASELINE_DATA_DEFINITIONS =
            TEST_FUNCTIONS.register("terminal_baseline_data_definitions", () -> ModGameTests::terminalBaselineDataDefinitions);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_MISSION_BROWSER_CACHE =
            TEST_FUNCTIONS.register("terminal_mission_browser_cache", () -> ModGameTests::terminalMissionBrowserCache);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_MISSION_BROWSER_PHASE_GATING =
            TEST_FUNCTIONS.register("terminal_mission_browser_phase_gating", () -> ModGameTests::terminalMissionBrowserPhaseGating);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_MISSION_HUD_NOTIFICATIONS =
            TEST_FUNCTIONS.register("terminal_mission_hud_notifications", () -> ModGameTests::terminalMissionHudNotifications);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_HUD_NOTICE_SURFACE =
            TEST_FUNCTIONS.register("terminal_hud_notice_surface", () -> ModGameTests::terminalHudNoticeSurface);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_RECIPE_REGISTRY =
            TEST_FUNCTIONS.register("terminal_recipe_registry", () -> ModGameTests::terminalRecipeRegistry);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_RECIPE_LOOKUPS =
            TEST_FUNCTIONS.register("terminal_recipe_lookups", () -> ModGameTests::terminalRecipeLookups);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_SCREENCORE_ACTION_CATALOG =
            TEST_FUNCTIONS.register("terminal_screencore_action_catalog", () -> ModGameTests::terminalScreenCoreActionCatalog);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_SCREENCORE_PARITY_STATE =
            TEST_FUNCTIONS.register("terminal_screencore_parity_state", () -> ModGameTests::terminalScreenCoreParityState);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_SCREENCORE_RECIPE_INDEX_CACHE =
            TEST_FUNCTIONS.register("terminal_screencore_recipe_index_cache", () -> ModGameTests::terminalScreenCoreRecipeIndexCache);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_SCREENCORE_MISSION_BROWSER_CACHE =
            TEST_FUNCTIONS.register("terminal_screencore_mission_browser_cache", () -> ModGameTests::terminalScreenCoreMissionBrowserCache);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_SCREENCORE_CLICK_ACTION_DISPATCH =
            TEST_FUNCTIONS.register("terminal_screencore_click_action_dispatch", () -> ModGameTests::terminalScreenCoreClickActionDispatch);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_SCREENCORE_OVERVIEW_ROUTE_CACHE =
            TEST_FUNCTIONS.register("terminal_screencore_overview_route_cache", () -> ModGameTests::terminalScreenCoreOverviewRouteCache);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_SCREENCORE_NATIVE_MISSION_FALLBACK =
            TEST_FUNCTIONS.register("terminal_screencore_native_mission_fallback", () -> ModGameTests::terminalScreenCoreNativeMissionFallback);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_SCREENCORE_NATIVE_LINKAGE_PROVIDER_FALLBACK =
            TEST_FUNCTIONS.register("terminal_screencore_native_linkage_provider_fallback",
                    () -> ModGameTests::terminalScreenCoreNativeLinkageProviderFallback);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TERMINAL_CONFIG_WORKFLOW =
            TEST_FUNCTIONS.register("terminal_config_workflow", () -> ModGameTests::terminalConfigWorkflow);

    private ModGameTests() {
    }

    public static void register(IEventBus eventBus) {
        TEST_FUNCTIONS.register(eventBus);
    }

    public static void registerTests(RegisterGameTestsEvent event) {
        if (!shouldRegisterTests()) {
            return;
        }
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(id("terminal_release"));
        register(event, environment, "terminal_api_ids", TERMINAL_API_IDS.getId());
        register(event, environment, "terminal_tab_registry", TERMINAL_TAB_REGISTRY.getId());
        register(event, environment, "terminal_mission_registry", TERMINAL_MISSION_REGISTRY.getId());
        register(event, environment, "terminal_addon_info_registry", TERMINAL_ADDON_INFO_REGISTRY.getId());
        register(event, environment, "terminal_addon_guide_ordering", TERMINAL_ADDON_GUIDE_ORDERING.getId());
        register(event, environment, "terminal_navigation_profiles", TERMINAL_NAVIGATION_PROFILES.getId());
        register(event, environment, "terminal_discovery_grid_filters", TERMINAL_DISCOVERY_GRID_FILTERS.getId());
        register(event, environment, "terminal_discovery_grid_route_state", TERMINAL_DISCOVERY_GRID_ROUTE_STATE.getId());
        register(event, environment, "terminal_discovery_grid_batch_resolution", TERMINAL_DISCOVERY_GRID_BATCH_RESOLUTION.getId());
        register(event, environment, "terminal_render_context_navigation", TERMINAL_RENDER_CONTEXT_NAVIGATION.getId());
        register(event, environment, "terminal_theme_registry", TERMINAL_THEME_REGISTRY.getId());
        register(event, environment, "terminal_theme_icon_fallback", TERMINAL_THEME_ICON_FALLBACK.getId());
        register(event, environment, "terminal_theme_chapter_style", TERMINAL_THEME_CHAPTER_STYLE.getId());
        register(event, environment, "terminal_theme_resources", TERMINAL_THEME_RESOURCES.getId());
        register(event, environment, "terminal_theme_selection", TERMINAL_THEME_SELECTION.getId());
        register(event, environment, "terminal_client_options_config", TERMINAL_CLIENT_OPTIONS_CONFIG.getId());
        register(event, environment, "terminal_zoom_options", TERMINAL_ZOOM_OPTIONS.getId());
        register(event, environment, "terminal_scrollbar_metrics", TERMINAL_SCROLLBAR_METRICS.getId());
        register(event, environment, "terminal_screen_provider_precedence", TERMINAL_SCREEN_PROVIDER_PRECEDENCE.getId());
        register(event, environment, "terminal_visual_polish_layout", TERMINAL_VISUAL_POLISH_LAYOUT.getId());
        register(event, environment, "terminal_screencore_text_layout", TERMINAL_SCREENCORE_TEXT_LAYOUT.getId());
        register(event, environment, "terminal_resource_name_contracts", TERMINAL_RESOURCE_NAME_CONTRACTS.getId());
        register(event, environment, "terminal_command_deck_priority", TERMINAL_COMMAND_DECK_PRIORITY.getId());
        register(event, environment, "terminal_mission_action_routing", TERMINAL_MISSION_ACTION_ROUTING.getId());
        register(event, environment, "terminal_lore_taxonomy", TERMINAL_LORE_TAXONOMY.getId());
        register(event, environment, "terminal_empty_provider_contracts", TERMINAL_EMPTY_PROVIDER_CONTRACTS.getId());
        register(event, environment, "terminal_main_survival_route", TERMINAL_MAIN_SURVIVAL_ROUTE.getId());
        register(event, environment, "terminal_main_survival_route_gate", TERMINAL_MAIN_SURVIVAL_ROUTE_GATE.getId());
        register(event, environment, "terminal_main_survival_route_cache", TERMINAL_MAIN_SURVIVAL_ROUTE_CACHE.getId());
        register(event, environment, "terminal_main_survival_route_bounds", TERMINAL_MAIN_SURVIVAL_ROUTE_BOUNDS.getId());
        register(event, environment, "terminal_reward_cache", TERMINAL_REWARD_CACHE.getId());
        register(event, environment, "terminal_reward_transactional", TERMINAL_REWARD_TRANSACTIONAL.getId());
        register(event, environment, "terminal_reward_claim_flow", TERMINAL_REWARD_CLAIM_FLOW.getId());
        register(event, environment, "terminal_runtime_spine_actions", TERMINAL_RUNTIME_SPINE_ACTIONS.getId());
        register(event, environment, "terminal_reward_explicit_owner", TERMINAL_REWARD_EXPLICIT_OWNER.getId());
        register(event, environment, "terminal_menu_validity", TERMINAL_MENU_VALIDITY.getId());
        register(event, environment, "terminal_baseline_cache_contract", TERMINAL_BASELINE_CACHE_CONTRACT.getId());
        register(event, environment, "terminal_baseline_auto_refresh", TERMINAL_BASELINE_AUTO_REFRESH.getId());
        register(event, environment, "terminal_baseline_data_definitions", TERMINAL_BASELINE_DATA_DEFINITIONS.getId());
        register(event, environment, "terminal_mission_browser_cache", TERMINAL_MISSION_BROWSER_CACHE.getId());
        register(event, environment, "terminal_mission_browser_phase_gating", TERMINAL_MISSION_BROWSER_PHASE_GATING.getId());
        register(event, environment, "terminal_mission_hud_notifications", TERMINAL_MISSION_HUD_NOTIFICATIONS.getId());
        register(event, environment, "terminal_hud_notice_surface", TERMINAL_HUD_NOTICE_SURFACE.getId());
        register(event, environment, "terminal_recipe_registry", TERMINAL_RECIPE_REGISTRY.getId());
        register(event, environment, "terminal_recipe_lookups", TERMINAL_RECIPE_LOOKUPS.getId());
        register(event, environment, "terminal_screencore_action_catalog", TERMINAL_SCREENCORE_ACTION_CATALOG.getId());
        register(event, environment, "terminal_screencore_parity_state", TERMINAL_SCREENCORE_PARITY_STATE.getId());
        register(event, environment, "terminal_screencore_mission_browser_cache", TERMINAL_SCREENCORE_MISSION_BROWSER_CACHE.getId());
        register(event, environment, "terminal_screencore_overview_route_cache", TERMINAL_SCREENCORE_OVERVIEW_ROUTE_CACHE.getId());
        register(event, environment, "terminal_screencore_native_mission_fallback", TERMINAL_SCREENCORE_NATIVE_MISSION_FALLBACK.getId());
        register(event, environment, "terminal_screencore_click_action_dispatch", TERMINAL_SCREENCORE_CLICK_ACTION_DISPATCH.getId());
        register(event, environment, "terminal_config_workflow", TERMINAL_CONFIG_WORKFLOW.getId());
    }

    private static void terminalApiIds(GameTestHelper helper) {
        helper.assertTrue(TerminalActionPacket.ID.equals(id("terminal_action")),
                "Terminal action packet id must be echoterminal:terminal_action");
        TerminalActionRegistry.withClearedForTests(() -> {
            AtomicBoolean handled = new AtomicBoolean(false);
            TerminalActionRegistry.register(id("test_tab"), id("test_action"), (player, payload) -> handled.set(true));
            helper.assertTrue(TerminalActionRegistry.handle(null, id("test_tab"), id("test_action"), ""),
                    "Registered terminal action should be routed");
            helper.assertTrue(handled.get(), "Registered terminal action handler should run");
            TerminalActionRegistry.register(id("test_tab"), id("failing_action"), (player, payload) -> {
                throw new IllegalStateException("test terminal action failure");
            });
            helper.assertFalse(TerminalActionRegistry.handle(null, id("test_tab"), id("failing_action"), ""),
                    "Failing terminal action handlers should be logged and ignored");
            AtomicBoolean denied = new AtomicBoolean(false);
            TerminalActionRegistry.register(id("test_tab"), id("denied_action"),
                    (player, payload) -> denied.set(true), context -> false);
            helper.assertTrue(TerminalActionRegistry.handle(null, id("test_tab"), id("denied_action"), ""),
                    "Known terminal actions rejected by validators should be consumed without reporting unknown");
            helper.assertFalse(denied.get(), "Rejected terminal action handlers should not run");
        });
        helper.succeed();
    }

    private static void terminalConfigWorkflow(GameTestHelper helper) {
        EchoConfigRegistry.withClearedForTests(() -> {
            AtomicInteger serverCount = new AtomicInteger(2);
            AtomicInteger clientCount = new AtomicInteger(3);
            EchoConfigRegistry.register(EchoConfigProvider.of(EchoTerminal.MODID, () -> new EchoConfigModule(
                    EchoTerminal.MODID,
                    "ECHO Terminal",
                    List.of(
                            new EchoConfigCategory("server", "Server", List.of(
                                    EchoConfigEntry.intEntry("server_count", "Server Count", "",
                                            EchoConfigSide.COMMON, 2, 0, 10, serverCount::get, serverCount::set,
                                            null, true, false, false))),
                            new EchoConfigCategory("client", "Client", List.of(
                                    EchoConfigEntry.intEntry("client_count", "Client Count", "",
                                            EchoConfigSide.CLIENT, 3, 0, 10, clientCount::get, clientCount::set,
                                            null, true, false, false)))))));

            TerminalConfigActionPacket packet = new TerminalConfigActionPacket(
                    TerminalConfigActionPacket.Action.SET,
                    EchoConfigSide.COMMON,
                    "ECHOterminal",
                    "Server_Count",
                    "4");
            helper.assertTrue(packet.moduleId().equals(EchoTerminal.MODID)
                            && packet.entryId().equals("server_count")
                            && packet.action() == TerminalConfigActionPacket.Action.SET,
                    "Config action packets should normalize ids and preserve action intent");

            List<EchoConfigModuleSnapshot> commonSnapshot = EchoConfigRegistry.snapshots(EchoConfigSide.COMMON);
            TerminalConfigClientState.apply(new TerminalConfigSyncPacket(commonSnapshot, "Snapshot ready."));
            helper.assertTrue(TerminalConfigClientState.commonModule("ECHOterminal").isPresent(),
                    "Client config state should apply common snapshots from the server");
            helper.assertTrue(TerminalConfigClientState.status().equals("Snapshot ready."),
                    "Client config state should expose visible server status");

            EchoConfigEntrySnapshot frozen = TerminalConfigClientState.commonModule(EchoTerminal.MODID).orElseThrow()
                    .categories().get(0).entries().get(0);
            helper.assertTrue(frozen.value().equals("2"), "Server snapshot should contain the synced value");
            serverCount.set(8);
            EchoConfigEntrySnapshot stillFrozen = TerminalConfigClientState.commonModule(EchoTerminal.MODID).orElseThrow()
                    .categories().get(0).entries().get(0);
            helper.assertTrue(stillFrozen.value().equals("2"),
                    "Terminal common config should render from server snapshots, not local common values");

            EchoConfigApplyResult applied = EchoConfigRegistry.apply(
                    EchoConfigSide.COMMON, EchoTerminal.MODID, "server_count", "6");
            helper.assertTrue(applied.success() && serverCount.get() == 6,
                    "Server config edits should validate and update common entries");
            helper.assertFalse(EchoConfigRegistry.apply(
                    EchoConfigSide.COMMON, EchoTerminal.MODID, "client_count", "5").success(),
                    "Server config actions should reject client-local entries");
            helper.assertFalse(EchoConfigRegistry.apply(
                    EchoConfigSide.COMMON, EchoTerminal.MODID, "missing", "5").success(),
                    "Unknown config entries should be rejected");
            helper.assertTrue(EchoConfigRegistry.apply(
                    EchoConfigSide.CLIENT, EchoTerminal.MODID, "client_count", "5").success()
                            && clientCount.get() == 5,
                    "Client-local config edits should apply through the client registry side");

            EchoAddonChapter aliasChapter = new EchoAddonChapter() {
                @Override
                public String id() {
                    return "terminal_alias";
                }

                @Override
                public String modId() {
                    return EchoTerminal.MODID;
                }

                @Override
                public String displayName() {
                    return "Terminal Alias";
                }

                @Override
                public String summary() {
                    return "Alias chapter for config matching.";
                }
            };
            List<String> matched = BuiltinTerminalTabs.addonConfigAwareGuideOrderForTests(List.of(aliasChapter));
            helper.assertTrue(matched.stream().filter(entry -> entry.endsWith("|" + EchoTerminal.MODID)).count() == 1,
                    "Addon config matching should use modId/chapterId keys and avoid duplicates");
            List<String> synthetic = BuiltinTerminalTabs.addonConfigAwareGuideOrderForTests(List.of());
            helper.assertTrue(synthetic.stream().anyMatch(entry -> entry.endsWith("|" + EchoTerminal.MODID)),
                    "Loaded config-capable modules without chapters should be added to the addon guide");
        });
        TerminalConfigClientState.apply(null);
        helper.succeed();
    }

    private static void terminalTabRegistry(GameTestHelper helper) {
        TerminalTabRegistry.withClearedForTests(() -> {
            TerminalTabRegistry.register(new DummyTab(id("zeta"), "ZETA", 20));
            TerminalTabRegistry.register(new DummyTab(id("alpha"), "ALPHA", 10));
            TerminalTabRegistry.register(new DummyTab(id("beta"), "BETA", 10));

            helper.assertTrue(TerminalTabRegistry.tabs().size() == 3,
                    "Dynamic terminal registry should expose registered tabs");
            helper.assertTrue(TerminalTabRegistry.tabs().get(0).descriptor().id().equals(id("alpha")),
                    "Tabs with lower order should sort first by id");
            helper.assertTrue(TerminalTabRegistry.tabs().get(1).descriptor().id().equals(id("beta")),
                    "Tabs with equal order should sort by id");
            helper.assertTrue(TerminalTabRegistry.tabs().get(2).descriptor().id().equals(id("zeta")),
                    "Tabs with higher order should sort last");
        });
        helper.succeed();
    }

    private static void terminalMissionRegistry(GameTestHelper helper) {
        TerminalMissionRegistry.withClearedForTests(() -> {
            TerminalMissionRegistry.register(new DummyMissionProvider(id("zeta_chapter"), 20, new AtomicBoolean(false)));
            TerminalMissionRegistry.register(new DummyMissionProvider(id("alpha_chapter"), 10, new AtomicBoolean(false)));

            helper.assertTrue(TerminalMissionRegistry.providers().size() == 2,
                    "Mission provider registry should expose registered providers");
            helper.assertTrue(TerminalMissionRegistry.providers().get(0).chapter().id().equals(id("alpha_chapter")),
                    "Mission providers should sort by order and id");
            TerminalMissionRegistry.register(new ThrowingChapterProvider());
            helper.assertTrue(TerminalMissionRegistry.providers().size() == 2,
                    "Mission providers with failing chapter metadata should be ignored");

            boolean duplicateRejected = false;
            try {
                TerminalMissionRegistry.register(new DummyMissionProvider(id("alpha_chapter"), 99, new AtomicBoolean(false)));
            } catch (IllegalArgumentException expected) {
                duplicateRejected = true;
            }
            helper.assertTrue(duplicateRejected, "Duplicate mission provider ids must fail fast");

            boolean duplicateSkipped = TerminalMissionRegistry.registerIfAbsent(
                    new DummyMissionProvider(id("alpha_chapter"), 99, new AtomicBoolean(false)));
            helper.assertFalse(duplicateSkipped,
                    "Idempotent mission provider registration should skip an existing provider id.");
            helper.assertTrue(TerminalMissionRegistry.providers().size() == 2,
                    "Idempotent duplicate registration should not add a second provider.");

            boolean uppercaseRejected = false;
            try {
                String badNamespace = "Echo" + "Terminal";
                new TerminalMissionChapter(Identifier.fromNamespaceAndPath(badNamespace, "bad"), "Bad", "", 0, 0xFFFFFFFF, true);
            } catch (RuntimeException expected) {
                uppercaseRejected = true;
            }
            helper.assertTrue(uppercaseRejected, "Mission chapter ids must reject uppercase namespaces");
        });
        helper.succeed();
    }

    private static void terminalRecipeRegistry(GameTestHelper helper) {
        TerminalRecipeRegistry.withClearedForTests(() -> {
            TerminalRecipeRegistry.register(new DummyRecipeProvider(id("alpha_provider"), 10));
            TerminalRecipeRegistry.register(new DummyRecipeProvider(id("zeta_provider"), 20));
            TerminalRecipeRegistry.register(new DuplicateRecipeProvider());
            TerminalRecipeRegistry.register(new ThrowingRecipeProvider());
            TerminalRecipeRegistry.register(new LinkageErrorRecipeProvider());
            helper.assertTrue(TerminalRecipeRegistry.providers().size() == 5,
                    "Recipe provider registry should expose registered providers that pass id validation");
            helper.assertTrue(TerminalRecipeRegistry.providers().get(0).id().equals(id("alpha_provider")),
                    "Recipe providers should sort by id");
            TerminalRecipeSnapshot snapshot = TerminalRecipeRegistry.snapshot(null);
            helper.assertTrue(snapshot.providerCount() == 5,
                    "Recipe snapshots should preserve provider count when one provider hits a linkage failure");
            helper.assertTrue(snapshot.categories().get(0).id().equals(id("alpha_category")),
                    "Recipe categories should sort by order then id");
            helper.assertTrue(snapshot.categories().stream().filter(category -> category.id().equals(id("alpha_category"))).count() == 1,
                    "Duplicate recipe categories should be de-duped in snapshots");
            helper.assertTrue(snapshot.recipes().stream().filter(recipe -> recipe.id().equals(id("alpha_provider/recipe"))).count() == 1,
                    "Duplicate recipe ids should be de-duped in snapshots");
            helper.assertTrue(snapshot.recipesFor(Items.APPLE).size() == 2,
                    "Snapshot output index should include deterministic provider recipes");
            helper.assertTrue(snapshot.usesFor(Items.STICK).size() == 2,
                    "Snapshot use index should include deterministic provider recipes");
            helper.assertTrue(BuiltinTerminalTabs.recipeIndexFailureDiagnosticIdsForTests()
                            .contains(id("diagnostic/recipe_index_failed").toString()),
                    "Command Deck diagnostics should convert recipe snapshot linkage failures into blocker rows");

            boolean duplicateRejected = false;
            try {
                TerminalRecipeRegistry.register(new DummyRecipeProvider(id("alpha_provider"), 99));
            } catch (IllegalArgumentException expected) {
                duplicateRejected = true;
            }
            helper.assertTrue(duplicateRejected, "Duplicate recipe provider ids must fail fast");
        });
        helper.succeed();
    }

    private static void terminalRecipeLookups(GameTestHelper helper) {
        TerminalRecipeEntry lockedRecipe = new TerminalRecipeEntry(
                id("recipe/test_locked"),
                id("recipe_category"),
                "Locked Apple",
                new ItemStack(Items.CRAFTING_TABLE),
                List.of(
                        TerminalRecipeSlot.input(new ItemStack(Items.STICK)),
                        TerminalRecipeSlot.catalyst(new ItemStack(Items.REDSTONE)),
                        TerminalRecipeSlot.info(new ItemStack(Items.BOOK), "Info"),
                        TerminalRecipeSlot.output(new ItemStack(Items.APPLE))),
                List.of(TerminalRecipeNote.warning("Requires TEST schematic unlock.")),
                40,
                true);
        TerminalRecipeEntry sourceRecipe = new TerminalRecipeEntry(
                id("recipe/source_cache"),
                id("recipe/sources"),
                "Cache Source",
                new ItemStack(Items.CHEST),
                List.of(TerminalRecipeSlot.output(Items.APPLE)),
                List.of(TerminalRecipeNote.info("Source type: Cache")),
                0,
                false);
        TerminalRecipeEntry rewardRecipe = new TerminalRecipeEntry(
                id("source/mission_reward/clean_water"),
                id("recipe/sources"),
                "Mission Reward: Clean Water",
                new ItemStack(Items.WRITABLE_BOOK),
                List.of(TerminalRecipeSlot.output(Items.APPLE)),
                List.of(TerminalRecipeNote.info("Source type: Mission Reward")),
                0,
                false);
        List<TerminalRecipeEntry> recipeFilterFixtures = List.of(lockedRecipe, sourceRecipe, rewardRecipe);
        helper.assertTrue(lockedRecipe.outputs(Items.APPLE), "Recipe lookup should match outputs");
        helper.assertTrue(lockedRecipe.uses(Items.STICK), "Recipe lookup should match inputs");
        helper.assertTrue(lockedRecipe.uses(Items.REDSTONE), "Recipe lookup should match catalysts");
        helper.assertTrue(lockedRecipe.uses(Items.CRAFTING_TABLE), "Recipe lookup should match machine slots");
        helper.assertTrue(lockedRecipe.mentions(Items.BOOK), "Recipe lookup should match info slots");
        helper.assertTrue(lockedRecipe.locked() && lockedRecipe.notes().stream().anyMatch(TerminalRecipeNote::warning),
                "Locked recipes should keep warning notes visible");
        helper.assertTrue(TerminalRecipeIndexTab.matchingRecipesForTests(
                        List.of(lockedRecipe), new ItemStack(Items.APPLE), false).size() == 1,
                "Recipe index should expose recipes for selected outputs");
        helper.assertTrue(TerminalRecipeIndexTab.matchingRecipesForTests(
                        List.of(lockedRecipe), new ItemStack(Items.STICK), true).size() == 1,
                "Recipe index should expose uses for selected inputs");
        helper.assertTrue(TerminalRecipeIndexTab.sourceFilteredRecipesForTests(recipeFilterFixtures, "processes")
                        .equals(List.of(lockedRecipe)),
                "Recipe index should default to process cards and hide acquisition sources.");
        helper.assertTrue(TerminalRecipeIndexTab.sourceFilteredRecipesForTests(recipeFilterFixtures, "sources")
                        .equals(List.of(sourceRecipe)),
                "Recipe index source filter should keep non-reward acquisition cards discoverable.");
        helper.assertTrue(TerminalRecipeIndexTab.sourceFilteredRecipesForTests(recipeFilterFixtures, "rewards")
                        .equals(List.of(rewardRecipe)),
                "Recipe index reward filter should isolate mission rewards.");
        helper.assertTrue(TerminalRecipeIndexTab.sourceFilteredRecipesForTests(recipeFilterFixtures, "all").size() == 3,
                "Recipe index all filter should include processes, sources, and rewards.");
        helper.assertTrue(TerminalRecipeIndexTab.echoItemsForTests().stream()
                        .allMatch(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().startsWith("echo")),
                "Recipe index item grid should only expose ECHO namespaces");
        helper.succeed();
    }

    private static void terminalScreenCoreActionCatalog(GameTestHelper helper) {
        if (!screenCoreLoaded()) {
            helper.succeed();
            return;
        }
        Set<String> catalog;
        Map<?, ?> registeredActions;
        try {
            screenCoreActionsClass().getMethod("register").invoke(null);
            catalog = screenCoreActionCatalog();
            registeredActions = screenCoreRegisteredActions();
        } catch (ReflectiveOperationException | LinkageError exception) {
            throw new AssertionError("Failed to inspect Terminal ScreenCore action catalog", exception);
        }
        helper.assertFalse(catalog.contains("terminal.expand_mission_group"),
                "Removed no-op mission group actions must not remain in the ScreenCore catalog");
        helper.assertFalse(catalog.contains("terminal.collapse_mission_group"),
                "Removed no-op mission group actions must not remain in the ScreenCore catalog");
        helper.assertFalse(catalog.contains("terminal.jump_to_tracked"),
                "Removed no-op tracked-mission actions must not remain in the ScreenCore catalog");

        Pattern literalActionTag = Pattern.compile("<[^>]+\\b(?:action|on-change)=\"([^\"]+)\"[^>]*>");
        Path root = euiSourceRoot();
        helper.assertTrue(Files.exists(root), "Built-in Terminal EUI source root should be available to game tests");
        try (var files = Files.walk(root)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".eui.xml")).toList()) {
                Matcher matcher = literalActionTag.matcher(Files.readString(file));
                while (matcher.find()) {
                    String tag = matcher.group(0);
                    String action = matcher.group(1).strip();
                    if (action.startsWith("{") || action.startsWith("screencore.")) {
                        continue;
                    }
                    if (action.startsWith("scriptcore.")) {
                        helper.assertTrue(tag.contains("disabled=") && tag.contains("disabled-reason="),
                                "External ScriptCore action " + action + " in " + root.relativize(file)
                                        + " must be disabled-gated with an explanation");
                        continue;
                    }
                    if (action.startsWith("terminal.")) {
                        helper.assertTrue(catalog.contains(action),
                                "Built-in EUI action " + action + " in " + root.relativize(file)
                                        + " must be in the ScreenCore action catalog");
                        helper.assertTrue(registeredActions.containsKey(action),
                                "Built-in EUI action " + action + " in " + root.relativize(file)
                                        + " must be registered by TerminalScreenCoreActions");
                        if (tag.contains("disabled=")) {
                            helper.assertTrue(tag.contains("disabled-reason="),
                                    "Disabled ScreenCore command " + action + " in " + root.relativize(file)
                                            + " must expose a disabled reason");
                        }
                        if (terminalActionRequiresValue(action)) {
                            helper.assertTrue(tag.contains("action-value=") && !tag.contains("action-value=\"\""),
                                    "ScreenCore command " + action + " in " + root.relativize(file)
                                            + " must carry a nonblank action value binding");
                        }
                    }
                }
            }
        } catch (IOException exception) {
            throw new AssertionError("Failed to scan Terminal EUI actions", exception);
        }
        helper.succeed();
    }

    private static boolean terminalActionRequiresValue(String action) {
        return action.equals("terminal.open_tab")
                || action.equals("terminal.open_related_tab")
                || action.equals("terminal.open_mission")
                || action.equals("terminal.select_mission")
                || action.equals("terminal.track_mission")
                || action.equals("terminal.untrack_mission")
                || action.equals("terminal.activate_selected_mission")
                || action.equals("terminal.claim_reward")
                || action.equals("terminal.perform_mission_action")
                || action.equals("terminal.rewardInbox.claim_reward")
                || action.equals("terminal.rewardInbox.defer")
                || action.equals("terminal.rewardInbox.mark_viewed")
                || action.equals("terminal.select_mission_provider")
                || action.equals("terminal.open_provider_route")
                || action.equals("terminal.open_provider_diagnostics")
                || action.equals("terminal.recipeIndex.select_item")
                || action.equals("terminal.recipeIndex.select_recipe")
                || action.equals("terminal.recipeIndex.set_mode")
                || action.equals("terminal.recipeIndex.set_source_filter")
                || action.equals("terminal.recipeIndex.toggle_category")
                || action.equals("terminal.select_route_record")
                || action.equals("terminal.filter_route_records")
                || action.equals("terminal.discoveryGrid.filter_category")
                || action.equals("terminal.discoveryGrid.filter_state")
                || action.equals("terminal.discoveryGrid.select_card")
                || action.equals("terminal.discoveryGrid.mark_checked")
                || action.equals("terminal.factions.filter_namespace")
                || action.equals("terminal.factions.select_faction")
                || action.equals("terminal.factions.open_contract")
                || action.equals("terminal.factions.open_route")
                || action.equals("terminal.archives.filter_state")
                || action.equals("terminal.archives.select_record")
                || action.equals("terminal.archives.mark_read")
                || action.equals("terminal.open_addon_link")
                || action.equals("terminal.select_addon")
                || action.equals("terminal.open_addon_route")
                || action.equals("terminal.open_addon_archives")
                || action.equals("terminal.open_addon_diagnostics")
                || action.equals("terminal.open_addon_config")
                || action.equals("terminal.settings.set_density")
                || action.equals("terminal.settings.set_zoom")
                || action.equals("terminal.settings.toggle_visual_treatment")
                || action.equals("terminal.settings.toggle_readability_option")
                || action.equals("terminal.settings.set_navigation_style");
    }

    private static void terminalScreenCoreParityState(GameTestHelper helper) {
        if (!screenCoreLoaded()) {
            helper.succeed();
            return;
        }
        try {
            screenCoreActionsClass().getMethod("register").invoke(null);
            screenCoreDataProvidersClass().getMethod("resetStateForTests").invoke(null);
            MainSurvivalQuestProvider.INSTANCE.clearCacheForTests();
            Identifier providerId = id("screen_parity_provider");
            Identifier missionId = Identifier.fromNamespaceAndPath("echoashfallprotocol", "secure_crash_outpost");
            Identifier turnInMissionId = id("screen_parity_turn_in");
            Identifier claimMissionId = id("screen_parity_claim");
            Identifier sideMissionId = id("screen_parity_side_op");
            Identifier completedMissionId = id("screen_parity_completed");
            Identifier lockedMissionId = id("screen_parity_locked");
            ConfigurableMissionProvider provider = new ConfigurableMissionProvider(
                    providerId,
                    "ScreenCore Parity",
                    5,
                    List.of(new ConfiguredMission(
                            missionId,
                            "Anchor Pod Outpost",
                            "Podfall",
                            "test",
                            "low",
                            TerminalMissionRole.MAIN,
                            TerminalMissionStatus.UNLOCKED,
                            List.of(
                                    TerminalMissionAction.enabled("primary", "Primary"),
                                    TerminalMissionAction.enabled("secondary", "Secondary"),
                                    TerminalMissionAction.disabled("locked", "Locked", "Unavailable")),
                            List.of(
                                    TerminalMissionRequirement.item(new ItemStack(Items.CAMPFIRE), 0, 1, false),
                                    TerminalMissionRequirement.block("Place Crafting Table", "Crafting station placed.",
                                            new ItemStack(Items.CRAFTING_TABLE), 1, 1, true),
                                    TerminalMissionRequirement.equipment("Equip Shield", "Carry a shield before pushing out.",
                                            new ItemStack(Items.SHIELD), true),
                                    TerminalMissionRequirement.entity("Clear Hostiles", "Hostiles neutralized.", 0, 2, false),
                                    TerminalMissionRequirement.location("Find Crash Site", "Reach the podfall marker.", true),
                                    TerminalMissionRequirement.custom("Read Field Brief", "Review the briefing packet.",
                                            ItemStack.EMPTY, 0, 1, false)),
                            List.of(TerminalMissionReward.of(new ItemStack(Items.TORCH, 12)),
                                    TerminalMissionReward.text("Route Cache",
                                            "Storage and first-night support cache."))),
                            new ConfiguredMission(
                                    sideMissionId,
                                    "Parity Side Operation",
                                    "Parity",
                                    "optional",
                                    "low",
                                    TerminalMissionRole.OPTIONAL,
                                    TerminalMissionStatus.CLAIMABLE,
                                    List.of(TerminalMissionAction.enabled("claim_reward", "CLAIM CACHE")),
                                    Optional.of(missionId),
                                    List.of(),
                                    List.of(),
                                    List.of(TerminalMissionReward.of(new ItemStack(Items.BREAD, 4)))),
                            new ConfiguredMission(
                                    completedMissionId,
                                    "Completed Route Mission",
                                    "Parity",
                                    "test",
                                    "low",
                                    TerminalMissionRole.MAIN,
                                    TerminalMissionStatus.COMPLETED,
                                    List.of()),
                            new ConfiguredMission(
                                    lockedMissionId,
                                    "Locked Route Mission",
                                    "Parity",
                                    "test",
                                    "low",
                                    TerminalMissionRole.MAIN,
                                    TerminalMissionStatus.LOCKED,
                                    List.of())));
            ConfigurableMissionProvider actionPriorityProvider = new ConfigurableMissionProvider(
                    id("screen_action_priority_provider"),
                    "ScreenCore Action Priority",
                    6,
                    List.of(new ConfiguredMission(
                            turnInMissionId,
                            "ScreenCore Turn-In Priority",
                            "Parity",
                            "test",
                            "low",
                            TerminalMissionRole.MAIN,
                            TerminalMissionStatus.UNLOCKED,
                            List.of(
                                    TerminalMissionAction.enabled("start", "Track"),
                                    TerminalMissionAction.enabled("complete", "Turn In"))),
                            new ConfiguredMission(
                                    claimMissionId,
                                    "ScreenCore Claim Priority",
                                    "Parity",
                                    "test",
                                    "low",
                                    TerminalMissionRole.MAIN,
                                    TerminalMissionStatus.CLAIMABLE,
                                    List.of(
                                            TerminalMissionAction.enabled("start", "Track"),
                                            TerminalMissionAction.enabled("claim_reward", "Claim")))));

            TerminalNavigationProfiles.withClearedForTests(() -> TerminalTabRegistry.withClearedForTests(() -> TerminalMissionRegistry.withClearedForTests(() ->
                    TerminalRecipeRegistry.withClearedForTests(() -> {
                        try {
                            TerminalTabRegistry.register(new DummyTab(id("overview"), "OVERVIEW", 0));
                            TerminalTabRegistry.register(new DummyTab(MainSurvivalQuestProvider.TAB_ID, "SURVIVAL ROUTE", 10));
                            TerminalTabRegistry.register(new DummyTab(VanillaJourneyProvider.TAB_ID, "BASELINE", 20));
                            TerminalTabRegistry.register(new DummyTab(id("addons"), "MODS", 30));
                            TerminalTabRegistry.register(new DummyTab(id("archives"), "ARCHIVES", 40));
                            TerminalTabRegistry.register(new DummyTab(id("data_core"), "DATA CORE", 50));
                            TerminalTabRegistry.register(new DummyTab(id("settings"), "SETTINGS", 60));
                            TerminalTabRegistry.register(new DummyTab(id("vitals"), "VITALS", 70));
                            TerminalTabRegistry.register(new DummyTab(id("discovery_grid"), "DISCOVERY", 80));
                            TerminalTabRegistry.register(new DummyTab(id("mission_graph"), "MISSION GRAPH", 90));
                            TerminalNavigationProfiles.register(id("overview"), TerminalNavigationProfile.system(0));
                            TerminalNavigationProfiles.register(MainSurvivalQuestProvider.TAB_ID,
                                    TerminalNavigationProfile.progress(0));
                            TerminalNavigationProfiles.register(VanillaJourneyProvider.TAB_ID,
                                    TerminalNavigationProfile.progress(50));
                            TerminalNavigationProfiles.register(id("addons"), TerminalNavigationProfile.progress(150));
                            TerminalNavigationProfiles.register(id("archives"), TerminalNavigationProfile.intel(950));
                            TerminalNavigationProfiles.register(id("data_core"), TerminalNavigationProfile.system(145));
                            TerminalNavigationProfiles.register(id("settings"), TerminalNavigationProfile.system(175));
                            TerminalMissionRegistry.register(provider);
                            TerminalRecipeRegistry.register(new DummyRecipeProvider(id("alpha_provider"), 10));

                            Object survivalContext = newScreenCoreDataContext();
                            survivalContext = screenCoreMissingPlaceholder(survivalContext, "");
                            survivalContext = putScreenCoreData(
                                    survivalContext,
                                    "terminal.activeTabId",
                                    MainSurvivalQuestProvider.TAB_ID.toString());

                            helper.assertTrue("secondary".equals(resolveMissionActionIdForTests(
                                            provider, missionId, "secondary")),
                                    "ScreenCore mission actions should honor the explicit EUI action id");
                            helper.assertTrue("".equals(resolveMissionActionIdForTests(
                                            provider, missionId, "missing")),
                                    "ScreenCore mission actions should reject ids not present in the current snapshot");
                            helper.assertTrue("primary".equals(resolveMissionActionIdForTests(
                                            provider, missionId, "")),
                                    "ScreenCore mission actions should fall back to the first enabled action only when no id is requested");
                            helper.assertTrue("complete".equals(resolveMissionActionIdForTests(
                                            actionPriorityProvider, turnInMissionId, "")),
                                    "ScreenCore mission actions should prefer turn-in over tracking when both are available");
                            helper.assertTrue("start".equals(resolveMissionActionIdForTests(
                                            actionPriorityProvider, turnInMissionId, "start")),
                                    "ScreenCore mission actions should still honor an explicit track/start action id");
                            helper.assertTrue("claim_reward".equals(resolveMissionActionIdForTests(
                                            actionPriorityProvider, claimMissionId, "")),
                                    "ScreenCore mission actions should prefer reward claims over setup actions");
                            helper.assertTrue("complete".equals(preferredScreenCoreMissionActionIdForTests(List.of(
                                            TerminalMissionAction.enabled("start", "Track"),
                                            TerminalMissionAction.enabled("complete", "Turn In")))),
                                    "ScreenCore mission rows should expose Turn In as the primary command before Track");
                            helper.assertTrue("claim_reward".equals(preferredScreenCoreMissionActionIdForTests(List.of(
                                            TerminalMissionAction.enabled("start", "Track"),
                                            TerminalMissionAction.enabled("claim_reward", "Claim Reward")))),
                                    "ScreenCore mission rows should expose Claim as the primary command before Track");
                            helper.assertTrue("turn_in".equals(preferredScreenCoreMissionActionIdForTests(List.of(
                                            TerminalMissionAction.disabled("start", "Track", "Unavailable"),
                                            TerminalMissionAction.disabled("turn_in", "Turn In", "Missing item")))),
                                    "ScreenCore mission rows should preserve completion priority for disabled fallback copy");

                            helper.assertTrue(runScreenCoreAction(screenCoreActionId("SELECT_MISSION_PROVIDER"),
                                            providerId.toString(), Map.of(), survivalContext, null),
                                    "Provider rows should update client-selected provider state");
                            helper.assertTrue(providerId.toString().equals(resolveScreenCoreData(
                                            survivalContext, "missionGraph.selectedProviderId")),
                                    "Selected provider id should be visible through ScreenCore data");

                            Identifier[] openedPage = new Identifier[1];
                            Object controls = screenCoreControls(openedPage);
                            helper.assertTrue(runScreenCoreAction(screenCoreActionId("OPEN_PROVIDER_ROUTE"),
                                            providerId.toString(), Map.of(), survivalContext, controls),
                                    "Provider route action should open the aggregate Survival Route");
                            helper.assertTrue(id("terminal_mission_browser").equals(openedPage[0]),
                                    "Provider route action should open the mission browser ScreenCore page");
                            helper.assertTrue(providerId.toString().equals(resolveScreenCoreData(
                                            survivalContext, "missionBrowser.providerFilter")),
                                    "Provider route action should apply the selected provider filter");
                            helper.assertTrue(MainSurvivalQuestProvider.TAB_ID.equals(
                                            providerRouteTargetForTests(providerId.toString())),
                                    "Non-vanilla providers should target Survival Route");
                            helper.assertTrue(VanillaJourneyProvider.TAB_ID.equals(
                                            providerRouteTargetForTests(VanillaJourneyProvider.CHAPTER_ID.toString())),
                                    "Vanilla provider should target Baseline");
                            helper.assertTrue(MainSurvivalQuestProvider.TAB_ID.equals(
                                            missionActionDispatchTabForTests(id("overview"), provider)),
                                    "Mission actions from a stale overview tab should dispatch through Survival Route");
                            helper.assertTrue(MainSurvivalQuestProvider.TAB_ID.equals(
                                            missionActionDispatchTabForTests(MainSurvivalQuestProvider.TAB_ID, provider)),
                                    "Mission actions from Survival Route should dispatch through the registered route tab");
                            helper.assertTrue(VanillaJourneyProvider.TAB_ID.equals(
                                            missionActionDispatchTabForTests(VanillaJourneyProvider.TAB_ID,
                                                    VanillaJourneyProvider.INSTANCE)),
                                    "Vanilla Journey actions should dispatch through the registered vanilla tab");
                            Identifier[] diagnosticsOpenedPage = new Identifier[1];
                            helper.assertTrue(runScreenCoreAction(screenCoreActionId("OPEN_PROVIDER_DIAGNOSTICS"),
                                            providerId.toString(), Map.of(), survivalContext,
                                            screenCoreControls(diagnosticsOpenedPage)),
                                    "Provider diagnostics action should open the Data Core with the provider filter applied");
                            helper.assertTrue(id("terminal_data_core").equals(diagnosticsOpenedPage[0]),
                                    "Provider diagnostics action should open the Data Core ScreenCore page");
                            helper.assertTrue(providerId.toString().equals(resolveScreenCoreData(
                                            survivalContext, "dataCore.filter")),
                                    "Provider diagnostics should expose the provider filter through Data Core state");

                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> navItems = (List<Map<String, Object>>)
                                    resolveScreenCoreData(survivalContext, "navigation.items");
                            helper.assertTrue(navItems.stream()
                                            .noneMatch(row -> id("diagnostics").toString().equals(String.valueOf(row.get("id")))),
                                    "ScreenCore navigation should not expose a standalone diagnostics tab");
                            helper.assertTrue(navItems.size() <= TerminalNavigationSection.storyFirstOrder().size(),
                                    "ScreenCore top navigation should expose section tabs only");
                            helper.assertTrue(navItems.stream()
                                            .anyMatch(row -> TerminalNavigationSection.CHAPTERS.key()
                                                    .equals(String.valueOf(row.get("id")))
                                                    && Boolean.TRUE.equals(row.get("active"))),
                                    "ScreenCore top navigation should mark the active Progress section");
                            helper.assertTrue(navItems.stream()
                                            .allMatch(row -> row.containsKey("defaultTabId")
                                                    && row.containsKey("countLabel")),
                                    "ScreenCore section tabs should expose default targets and compact counts");
                            helper.assertTrue(navItems.stream()
                                            .anyMatch(row -> TerminalNavigationSection.CHAPTERS.key()
                                                    .equals(String.valueOf(row.get("id")))
                                                    && MainSurvivalQuestProvider.TAB_ID.toString()
                                                            .equals(String.valueOf(row.get("defaultTabId")))),
                                    "The Progress section should default to Survival Route");
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> activeTabs = (List<Map<String, Object>>)
                                    resolveScreenCoreData(survivalContext, "navigation.activeTabs");
                            helper.assertTrue(activeTabs.stream()
                                            .allMatch(row -> TerminalNavigationSection.CHAPTERS.key()
                                                    .equals(String.valueOf(row.get("group")))),
                                    "ScreenCore left rail should be filtered to the active section");
                            helper.assertFalse(activeTabs.stream()
                                            .anyMatch(row -> "Terminal".equals(String.valueOf(row.get("compactShortTitle")))),
                                    "ScreenCore left rail should not fall back to duplicate Terminal labels");
                            helper.assertTrue(!activeTabs.isEmpty()
                                            && MainSurvivalQuestProvider.TAB_ID.toString()
                                                    .equals(String.valueOf(activeTabs.get(0).get("id"))),
                                    "Survival Route should be the first visible Progress tab");
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> priorityCards = (List<Map<String, Object>>)
                                    resolveScreenCoreData(survivalContext, "overview.priorityCards");
                            helper.assertTrue(priorityCards.stream()
                                            .noneMatch(row -> id("diagnostics").toString().equals(String.valueOf(row.get("tabId")))),
                                    "Command Deck priority cards should not link to removed diagnostics");
                            helper.assertTrue(MainSurvivalQuestProvider.TAB_ID.toString().equals(resolveScreenCoreData(
                                            survivalContext, "overview.bestNextAction.tabId")),
                                    "Command Deck best next action should fall back to Survival Route");
                            helper.assertTrue("Open Survival Route".equals(resolveScreenCoreData(
                                            survivalContext, "overview.bestNextAction.actionLabel")),
                                    "Command Deck hero CTA should use the same Survival Route handoff language as WHAT NOW");
                            helper.assertTrue(missionId.toString().equals(resolveScreenCoreData(
                                            survivalContext, "overview.bestNextAction.missionId")),
                                    "Fresh Command Deck should lead with the first Ashfall main-spine route mission");
                            helper.assertTrue("Anchor Pod Outpost".equals(resolveScreenCoreData(
                                            survivalContext, "overview.bestNextAction.title")),
                                    "Fresh Command Deck should use Ashfall starter mission copy");
                            String heroSummary = String.valueOf(resolveScreenCoreData(
                                    survivalContext, "overview.bestNextAction.summary"));
                            helper.assertFalse(heroSummary.contains("RelicTech"),
                                    "Ashfall starter hero summary should not present the route as RelicTech content");
                            String routeLine = String.valueOf(resolveScreenCoreData(
                                    survivalContext, "overview.routeStatus.routeLine"));
                            helper.assertTrue(routeLine.contains("Route 01")
                                            && routeLine.contains("Podfall")
                                            && !routeLine.contains("RelicTech"),
                                    "Early route status line should point to Podfall instead of RelicTech");
                            var routeLineMethod = screenCoreDataProvidersClass()
                                    .getDeclaredMethod("overviewMissionRouteLine", Map.class);
                            routeLineMethod.setAccessible(true);
                            String sanitizedStarterLine = String.valueOf(routeLineMethod.invoke(null, Map.of(
                                    "id", missionId.toString(),
                                    "routeLine", "Route 71 > Ashfall C45 > RelicTech Outpost")));
                            helper.assertTrue("Route 01 > Ashfall C45 > Podfall".equals(sanitizedStarterLine),
                                    "Ashfall starter route line should ignore stale RelicTech breadcrumb data");
                            String shellBreadcrumb = String.valueOf(resolveScreenCoreData(
                                    survivalContext, "shell.status.routeBreadcrumb"));
                            helper.assertTrue(shellBreadcrumb.contains("Route 01")
                                            && shellBreadcrumb.contains("Podfall")
                                            && !shellBreadcrumb.contains("RelicTech"),
                                    "Shell breadcrumb should derive from the starter route mission");
                            helper.assertTrue("READY".equals(resolveScreenCoreData(
                                            survivalContext, "overview.bestNextAction.statusCompactLabel")),
                                    "Command Deck hero should expose the compact selected mission route state");
                            helper.assertTrue(String.valueOf(resolveScreenCoreData(
                                            survivalContext, "overview.bestNextAction.routeProgressLabel")).startsWith("Route "),
                                    "Command Deck hero should expose a compact route progress chip label");
                            helper.assertTrue("RWD 2".equals(resolveScreenCoreData(
                                            survivalContext, "overview.bestNextAction.rewardCompactLabel")),
                                    "Command Deck hero should expose compact reward data for the selected route mission");
                            helper.assertTrue("REWARD".equals(resolveScreenCoreData(
                                            survivalContext, "overview.bestNextAction.rewardStateLabel")),
                                    "Command Deck hero should show listed-but-unclaimed reward state before completion");
                            helper.assertTrue(resolveScreenCoreData(survivalContext, "overview.blockerCards") instanceof List<?>,
                                    "Command Deck blocker cards should be exposed as a ScreenCore binding");
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> commandRows = (List<Map<String, Object>>)
                                    resolveScreenCoreData(survivalContext, "overview.commandRows");
                            helper.assertTrue(commandRows.stream()
                                            .allMatch(row -> row.containsKey("tabId") && row.containsKey("badge")),
                                    "Command Deck priority rows should expose stable action targets and status labels");
                            helper.assertTrue(!commandRows.isEmpty()
                                            && "Open Survival Route".equals(String.valueOf(commandRows.get(0).get("title")))
                                            && MainSurvivalQuestProvider.TAB_ID.toString()
                                                    .equals(String.valueOf(commandRows.get(0).get("tabId"))),
                                    "Command Deck WHAT NOW should lead with the same Survival Route handoff as the hero CTA");
                            helper.assertTrue(missionId.toString().equals(String.valueOf(commandRows.get(0).get("actionValue"))),
                                    "Command Deck WHAT NOW route handoff should carry the selected mission id");
                            helper.assertFalse(String.valueOf(commandRows.get(0).get("summary")).contains("RelicTech"),
                                    "Command Deck WHAT NOW starter row should not present the route as RelicTech content");
                            Identifier[] routeCommandOpenedPage = new Identifier[1];
                            helper.assertTrue(runScreenCoreAction(screenCoreActionId("OPEN_MISSION"),
                                            String.valueOf(commandRows.get(0).get("actionValue")), Map.of(),
                                            survivalContext, screenCoreControls(routeCommandOpenedPage)),
                                    "Command Deck WHAT NOW route handoff should open Survival Route through the mission path");
                            helper.assertTrue(id("terminal_mission_browser").equals(routeCommandOpenedPage[0])
                                            && missionId.toString().equals(resolveScreenCoreData(
                                                    survivalContext, "missionBrowser.selectedMissionId")),
                                    "Command Deck WHAT NOW should open Survival Route with the active mission selected");
                            helper.assertTrue(runScreenCoreAction(screenCoreActionId("SELECT_MISSION"),
                                            completedMissionId.toString(), Map.of(), survivalContext, null),
                                    "A stale ScreenCore selected mission should be selectable before returning home");
                            helper.assertTrue(missionId.toString().equals(resolveScreenCoreData(
                                            survivalContext, "overview.bestNextAction.missionId")),
                                    "Command Deck hero should ignore stale mission selection and keep the Ashfall spine first");
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> recentIntel = (List<Map<String, Object>>)
                                    resolveScreenCoreData(survivalContext, "overview.recentIntel");
                            helper.assertTrue(recentIntel.stream()
                                            .allMatch(row -> row.containsKey("compactStatusLabel")
                                                    && String.valueOf(row.get("compactStatusLabel")).length() <= 6),
                                    "Command Deck recent intel rows should use compact status labels");
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> sideOps = (List<Map<String, Object>>)
                                    resolveScreenCoreData(survivalContext, "overview.sideOps");
                            helper.assertTrue(sideOps.stream()
                                            .anyMatch(row -> sideMissionId.toString().equals(String.valueOf(row.get("missionId")))),
                                    "Command Deck side ops should come from route-linked TerminalMissionProvider records");
                            helper.assertTrue(sideOps.stream()
                                            .filter(row -> sideMissionId.toString().equals(String.valueOf(row.get("missionId"))))
                                            .allMatch(row -> missionId.toString().equals(String.valueOf(row.get("routeAnchor")))),
                                    "Command Deck side ops should retain their owning route anchor");
                            helper.assertTrue(sideOps.stream()
                                            .filter(row -> sideMissionId.toString().equals(String.valueOf(row.get("missionId"))))
                                            .allMatch(row -> "CLAIM".equals(String.valueOf(row.get("compactStatusLabel")))),
                                    "Command Deck side ops should mirror Survival Route compact labels");
                            helper.assertTrue(sideOps.stream()
                                            .filter(row -> sideMissionId.toString().equals(String.valueOf(row.get("missionId"))))
                                            .allMatch(row -> "RWD 1".equals(String.valueOf(row.get("reward")))),
                                    "Command Deck side ops should expose real reward counts instead of synthetic payout numbers");
                            Identifier[] missionOpenedPage = new Identifier[1];
                            helper.assertTrue(runScreenCoreAction(screenCoreActionId("OPEN_MISSION"),
                                            sideMissionId.toString(), Map.of(), survivalContext,
                                            screenCoreControls(missionOpenedPage)),
                                    "Mission links should open the Survival Route instead of dead-ending");
                            helper.assertTrue(id("terminal_mission_browser").equals(missionOpenedPage[0]),
                                    "Mission links should navigate to the mission browser page");
                            helper.assertTrue(sideMissionId.toString().equals(resolveScreenCoreData(
                                            survivalContext, "missionBrowser.selectedMissionId")),
                                    "Mission links should select the requested route-linked side operation");
                            helper.assertTrue("CLAIM".equals(resolveScreenCoreData(
                                            survivalContext, "missionBrowser.selectedMission.rewardStateLabel")),
                                    "Claimable side operation briefing should expose a claim-ready reward state");
                            helper.assertTrue("RWD 1".equals(resolveScreenCoreData(
                                            survivalContext, "missionBrowser.selectedMission.rewardCompactLabel")),
                                    "Claimable side operation briefing should expose compact reward count data");
                            helper.assertTrue("Claim".equals(resolveScreenCoreData(
                                            survivalContext, "missionBrowser.selectedMission.primaryCommandLabel")),
                                    "Claimable side operation should surface a real claim command");
                            helper.assertTrue("claim_reward".equals(resolveScreenCoreData(
                                            survivalContext, "missionBrowser.selectedMission.claimActionId")),
                                    "Claimable side operation should expose an explicit Claim Rewards action id");
                            helper.assertFalse(Boolean.TRUE.equals(resolveScreenCoreData(
                                            survivalContext, "missionBrowser.selectedMission.claimCommandDisabled")),
                                    "Claimable side operation should enable the explicit Claim Rewards button");
                            helper.assertTrue(Boolean.TRUE.equals(resolveScreenCoreData(
                                            survivalContext, "missionBrowser.selectedMission.completeCommandDisabled")),
                                    "Claimable side operation without a turn-in action should keep Complete disabled");
                            helper.assertTrue(!String.valueOf(resolveScreenCoreData(
                                            survivalContext, "missionBrowser.selectedMission.completeCommandDisabledReason")).isBlank(),
                                    "Disabled Complete commands should explain why they are unavailable");
                            helper.assertTrue(runScreenCoreAction(screenCoreActionId("PERFORM_MISSION_ACTION"),
                                            sideMissionId.toString(),
                                            Map.of("action_id", String.valueOf(resolveScreenCoreData(
                                                    survivalContext, "missionBrowser.selectedMission.claimActionId"))),
                                            survivalContext, null),
                                    "Explicit Claim Rewards button should route through the mission action channel");
                            helper.assertTrue(runScreenCoreAction(screenCoreActionId("ACTIVATE_SELECTED_MISSION"),
                                            sideMissionId.toString(), Map.of(), survivalContext, null),
                                    "Claimable contextual command should route through the mission action channel");

                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> visibleMissions = (List<Map<String, Object>>)
                                    resolveScreenCoreData(survivalContext, "missionBrowser.visibleMissions");
                            boolean contiguousDisplayLabels = true;
                            for (int index = 0; index < visibleMissions.size(); index++) {
                                contiguousDisplayLabels &= String.format("%02d", index + 1)
                                        .equals(String.valueOf(visibleMissions.get(index).get("displayOrderLabel")));
                            }
                            helper.assertTrue(contiguousDisplayLabels,
                                    "Survival Route should expose contiguous player-facing mission numbers within the selected phase");
                            helper.assertTrue(!visibleMissions.isEmpty()
                                            && missionId.toString().equals(String.valueOf(visibleMissions.get(0).get("id")))
                                            && "01".equals(String.valueOf(visibleMissions.get(0).get("displayOrderLabel"))),
                                    "Survival Route should keep the Ashfall starter mission first with display label 01");
                            helper.assertTrue(visibleMissions.stream()
                                            .allMatch(row -> "Podfall".equals(String.valueOf(row.get("phase")))),
                                    "Side operation selection should keep the center mission list scoped to its route anchor phase");
                            helper.assertTrue(visibleMissions.stream()
                                            .anyMatch(row -> providerId.toString().equals(String.valueOf(row.get("sourceChapterId")))),
                                    "Provider-filtered Survival Route data should retain the selected provider source id");
                            helper.assertTrue(visibleMissions.stream()
                                            .filter(row -> missionId.toString().equals(String.valueOf(row.get("id"))))
                                            .anyMatch(row -> "READY".equals(String.valueOf(row.get("statusCompactLabel")))),
                                    "Unlocked ScreenCore route rows should expose compact READY labels");
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> roadmapRows = (List<Map<String, Object>>)
                                    resolveScreenCoreData(survivalContext, "missionBrowser.roadmapRows");
                            helper.assertTrue(roadmapRows.stream()
                                            .filter(row -> "mission-phase-row".equals(String.valueOf(row.get("class"))))
                                            .map(row -> String.valueOf(row.get("title")))
                                            .distinct()
                                            .count() >= 2,
                                    "Survival Route phase lane should keep all filtered phases available while missions are phase-scoped");
                            helper.assertTrue(roadmapRows.stream()
                                            .filter(row -> "mission-phase-row".equals(String.valueOf(row.get("class"))))
                                            .anyMatch(row -> missionId.toString()
                                                    .equals(String.valueOf(row.get("targetMissionId")))),
                                    "Survival Route phase rows should target the first actionable real mission");
                            helper.assertTrue(roadmapRows.stream()
                                            .allMatch(row -> "mission-phase-row".equals(String.valueOf(row.get("class")))
                                                    && !String.valueOf(row.get("targetMissionId")).isBlank()),
                                    "Survival Route overview should be a compact clickable phase lane, not a second mission list");
                            helper.assertFalse(roadmapRows.stream()
                                            .filter(row -> "mission-record-row".equals(String.valueOf(row.get("class"))))
                                            .anyMatch(row -> "->".equals(String.valueOf(row.get("indexLabel")))),
                                    "Survival Route mission rows should not render placeholder arrow numbering");
                            helper.assertTrue(resolveScreenCoreData(
                                            survivalContext, "missionBrowser.selectedPhase.targetMissionId") instanceof String,
                                    "Survival Route selected phase context should remain available to ScreenCore bindings");
                            helper.assertTrue(runScreenCoreAction(screenCoreActionId("SELECT_MISSION"),
                                            missionId.toString(), Map.of(), survivalContext, null),
                                    "Route anchor mission should be selectable before inspecting side cards");
                            helper.assertTrue(!String.valueOf(resolveScreenCoreData(
                                            survivalContext, "missionBrowser.selectedMission.briefingBody")).isBlank(),
                                    "Selected Survival Route missions should expose player-facing briefing copy");
                            helper.assertTrue(!String.valueOf(resolveScreenCoreData(
                                            survivalContext, "missionBrowser.selectedMission.guidanceBody")).isBlank(),
                                    "Selected Survival Route missions should expose next-step guidance copy");
                            helper.assertTrue(String.valueOf(resolveScreenCoreData(
                                            survivalContext, "missionBrowser.selectedMission.contextBody")).contains("% complete"),
                                    "Selected Survival Route missions should expose route context and progress copy");
                            helper.assertTrue(Boolean.TRUE.equals(resolveScreenCoreData(
                                            survivalContext, "missionBrowser.selectedMission.completeCommandDisabled")),
                                    "Unlocked missions without a turn-in action should show Complete as disabled");
                            helper.assertTrue(!String.valueOf(resolveScreenCoreData(
                                            survivalContext, "missionBrowser.selectedMission.completeCommandDisabledReason")).isBlank(),
                                    "Disabled Complete buttons should expose readable reasons");
                            helper.assertTrue(Boolean.TRUE.equals(resolveScreenCoreData(
                                            survivalContext, "missionBrowser.selectedMission.claimCommandDisabled")),
                                    "Unlocked missions should not claim rewards before a claim action is available");
                            helper.assertTrue(!String.valueOf(resolveScreenCoreData(
                                            survivalContext, "missionBrowser.selectedMission.claimCommandDisabledReason")).isBlank(),
                                    "Disabled Claim Rewards buttons should expose readable reasons");
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> selectedRewardRows = (List<Map<String, Object>>)
                                    resolveScreenCoreData(survivalContext, "missionBrowser.selectedMission.rewardRows");
                            helper.assertTrue(selectedRewardRows.size() == 2
                                            && selectedRewardRows.stream()
                                            .anyMatch(row -> "x12".equals(String.valueOf(row.get("countLabel"))))
                                            && selectedRewardRows.stream()
                                            .anyMatch(row -> "Route Cache".equals(String.valueOf(row.get("title")))),
                                    "Survival Route briefing should expose item and text reward rows for the selected mission");
                            helper.assertTrue(selectedRewardRows.stream()
                                            .allMatch(row -> !String.valueOf(row.get("iconItemId")).isBlank()
                                                    && !"minecraft:air".equals(String.valueOf(row.get("iconItemId")))
                                                    && number(row.get("iconCount")) > 0),
                                    "Survival Route reward rows should expose real item icon ids and positive icon counts");
                            helper.assertTrue(selectedRewardRows.stream()
                                            .filter(row -> "x12".equals(String.valueOf(row.get("countLabel"))))
                                            .anyMatch(row -> "minecraft:torch".equals(String.valueOf(row.get("iconItemId")))
                                                    && number(row.get("iconCount")) == 12),
                                    "Item reward rows should render the actual reward stack icon and count");
                            helper.assertTrue(selectedRewardRows.stream()
                                            .filter(row -> "Route Cache".equals(String.valueOf(row.get("title"))))
                                            .anyMatch(row -> "minecraft:chest".equals(String.valueOf(row.get("iconItemId")))
                                                    && number(row.get("iconCount")) == 1),
                                    "Text reward rows should render the fallback reward cache icon");
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> selectedRequirements = (List<Map<String, Object>>)
                                    resolveScreenCoreData(survivalContext, "missionBrowser.selectedMission.requirements");
                            helper.assertTrue(selectedRequirements.size() == 6
                                            && selectedRequirements.stream()
                                            .allMatch(row -> !String.valueOf(row.get("iconItemId")).isBlank()
                                                    && !"minecraft:air".equals(String.valueOf(row.get("iconItemId")))
                                                    && number(row.get("iconCount")) > 0),
                                    "Survival Route requirement rows should expose real item icon ids and positive icon counts");
                            helper.assertTrue(selectedRequirements.stream()
                                            .filter(row -> "item".equals(String.valueOf(row.get("kind"))))
                                            .anyMatch(row -> "minecraft:campfire".equals(String.valueOf(row.get("iconItemId")))),
                                    "Item requirements should preserve their authored item icon");
                            helper.assertTrue(selectedRequirements.stream()
                                            .filter(row -> "block".equals(String.valueOf(row.get("kind"))))
                                            .anyMatch(row -> "minecraft:crafting_table".equals(String.valueOf(row.get("iconItemId")))),
                                    "Block requirements should preserve their authored block item icon");
                            helper.assertTrue(selectedRequirements.stream()
                                            .filter(row -> "equipment".equals(String.valueOf(row.get("kind"))))
                                            .anyMatch(row -> "minecraft:shield".equals(String.valueOf(row.get("iconItemId")))),
                                    "Equipment requirements should preserve their authored equipment icon");
                            helper.assertTrue(selectedRequirements.stream()
                                            .filter(row -> "entity_kill".equals(String.valueOf(row.get("kind"))))
                                            .anyMatch(row -> "minecraft:iron_sword".equals(String.valueOf(row.get("iconItemId")))),
                                    "Entity requirements should use the combat fallback icon");
                            helper.assertTrue(selectedRequirements.stream()
                                            .filter(row -> "location".equals(String.valueOf(row.get("kind"))))
                                            .anyMatch(row -> "minecraft:compass".equals(String.valueOf(row.get("iconItemId")))),
                                    "Location requirements should use the compass fallback icon");
                            helper.assertTrue(selectedRequirements.stream()
                                            .filter(row -> "custom".equals(String.valueOf(row.get("kind"))))
                                            .anyMatch(row -> "minecraft:paper".equals(String.valueOf(row.get("iconItemId")))),
                                    "Custom requirements should use the paper fallback icon");
                            helper.assertTrue("2 rewards".equals(resolveScreenCoreData(
                                            survivalContext, "missionBrowser.selectedMission.rewardCountLabel")),
                                    "Survival Route briefing should expose a readable reward count label");
                            helper.assertTrue(runScreenCoreAction(screenCoreActionId("TRACK_MISSION"),
                                            missionId.toString(), Map.of(), survivalContext, null),
                                    "Track button should remain wired through the mission tracking action");
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> selectedSideCards = (List<Map<String, Object>>)
                                    resolveScreenCoreData(survivalContext, "missionBrowser.selectedMission.sideCards");
                            helper.assertTrue(selectedSideCards.stream()
                                            .filter(row -> sideMissionId.toString().equals(String.valueOf(row.get("id"))))
                                            .anyMatch(row -> "CLAIM".equals(String.valueOf(row.get("statusCompactLabel")))),
                                    "Claimable ScreenCore side rows should expose compact CLAIM labels");
                            Map<String, Object> laterPhaseRow = roadmapRows.stream()
                                    .filter(row -> "mission-phase-row".equals(String.valueOf(row.get("class"))))
                                    .filter(row -> !"Podfall".equals(String.valueOf(row.get("title"))))
                                    .findFirst()
                                    .orElse(Map.of());
                            String laterPhaseTitle = String.valueOf(laterPhaseRow.get("title"));
                            helper.assertTrue(completedMissionId.toString().equals(String.valueOf(laterPhaseRow.get("targetMissionId")))
                                            && runScreenCoreAction(screenCoreActionId("SELECT_MISSION"),
                                            String.valueOf(laterPhaseRow.get("targetMissionId")), Map.of(), survivalContext, null),
                                    "Selecting a later phase lane should target that phase's first available mission");
                            visibleMissions = (List<Map<String, Object>>)
                                    resolveScreenCoreData(survivalContext, "missionBrowser.visibleMissions");
                            helper.assertTrue(visibleMissions.size() == 2
                                            && visibleMissions.stream()
                                            .allMatch(row -> laterPhaseTitle.equals(String.valueOf(row.get("phase")))),
                                    "Selected phase should limit the center mission list to that phase");
                            for (int index = 0; index < visibleMissions.size(); index++) {
                                helper.assertTrue(String.format("%02d", index + 1)
                                                .equals(String.valueOf(visibleMissions.get(index).get("displayOrderLabel"))),
                                        "Selected phase mission numbers should restart at 01");
                            }
                            helper.assertTrue(visibleMissions.stream()
                                            .filter(row -> completedMissionId.toString().equals(String.valueOf(row.get("id"))))
                                            .anyMatch(row -> "DONE".equals(String.valueOf(row.get("statusCompactLabel")))),
                                    "Completed ScreenCore route rows should expose compact DONE labels");
                            helper.assertTrue(visibleMissions.stream()
                                            .filter(row -> lockedMissionId.toString().equals(String.valueOf(row.get("id"))))
                                            .anyMatch(row -> "LOCKED".equals(String.valueOf(row.get("statusCompactLabel")))),
                                    "Locked ScreenCore route rows should expose compact LOCKED labels");
                            helper.assertTrue(runScreenCoreAction(screenCoreActionId("SELECT_MISSION"),
                                            completedMissionId.toString(), Map.of(), survivalContext, null),
                                    "Completed mission should be selectable through ScreenCore");
                            helper.assertTrue(!"No command".equals(resolveScreenCoreData(
                                            survivalContext, "missionBrowser.selectedMission.primaryCommandLabel")),
                                    "Completed ScreenCore missions should not surface a No command button");
                            helper.assertTrue("next".equals(resolveScreenCoreData(
                                            survivalContext, "missionBrowser.selectedMission.primaryCommandMode")),
                                    "Completed missions without provider actions should focus the next actionable route command");
                            helper.assertFalse(Boolean.TRUE.equals(resolveScreenCoreData(
                                            survivalContext, "missionBrowser.selectedMission.primaryActionDisabled")),
                                    "ScreenCore route primary commands should stay visibly actionable");
                            helper.assertTrue(runScreenCoreAction(screenCoreActionId("ACTIVATE_SELECTED_MISSION"),
                                            completedMissionId.toString(), Map.of(), survivalContext, null),
                                    "Completed contextual command should route to the next actionable mission");
                            helper.assertTrue(missionId.toString().equals(resolveScreenCoreData(
                                            survivalContext, "missionBrowser.selectedMissionId")),
                                    "Completed contextual command should select the next actionable mission");
                            helper.assertTrue(runScreenCoreAction(screenCoreActionId("SELECT_MISSION"),
                                            lockedMissionId.toString(), Map.of(), survivalContext, null),
                                    "Locked mission should be selectable through ScreenCore");
                            helper.assertTrue("Unlock".equals(resolveScreenCoreData(
                                            survivalContext, "missionBrowser.selectedMission.primaryCommandLabel")),
                                    "Locked ScreenCore missions should expose an unlock/requirements command");
                            helper.assertTrue("requirements".equals(resolveScreenCoreData(
                                            survivalContext, "missionBrowser.selectedMission.primaryCommandMode")),
                                    "Locked contextual command should focus requirements instead of disabling the button");
                            helper.assertTrue("Locked".equals(resolveScreenCoreData(
                                            survivalContext, "missionBrowser.selectedMission.guidanceTitle")),
                                    "Locked ScreenCore missions should label the mission info guidance as locked");
                            helper.assertTrue(!String.valueOf(resolveScreenCoreData(
                                            survivalContext, "missionBrowser.selectedMission.guidanceBody")).isBlank(),
                                    "Locked ScreenCore missions should explain what is blocking the objective");
                            helper.assertTrue(Boolean.TRUE.equals(resolveScreenCoreData(
                                            survivalContext, "missionBrowser.selectedMission.completeCommandDisabled"))
                                            && !String.valueOf(resolveScreenCoreData(
                                            survivalContext, "missionBrowser.selectedMission.completeCommandDisabledReason")).isBlank(),
                                    "Locked missions should show Complete disabled with a blocker reason");
                            helper.assertTrue(Boolean.TRUE.equals(resolveScreenCoreData(
                                            survivalContext, "missionBrowser.selectedMission.claimCommandDisabled"))
                                            && !String.valueOf(resolveScreenCoreData(
                                            survivalContext, "missionBrowser.selectedMission.claimCommandDisabledReason")).isBlank(),
                                    "Locked missions should show Claim Rewards disabled with a blocker reason");
                            helper.assertTrue(runScreenCoreAction(screenCoreActionId("ACTIVATE_SELECTED_MISSION"),
                                            lockedMissionId.toString(), Map.of(), survivalContext, null),
                                    "Locked contextual command should keep the briefing reachable");
                            helper.assertTrue(lockedMissionId.toString().equals(resolveScreenCoreData(
                                            survivalContext, "missionBrowser.selectedMissionId")),
                                    "Locked contextual command should keep the selected mission visible");
                            Identifier[] addonRouteOpenedPage = new Identifier[1];
                            helper.assertTrue(runScreenCoreAction(screenCoreActionId("OPEN_ADDON_ROUTE"),
                                            providerId.toString(), Map.of(), survivalContext,
                                            screenCoreControls(addonRouteOpenedPage)),
                                    "Addon route action should open the best matching route surface");
                            helper.assertTrue(id("terminal_mission_browser").equals(addonRouteOpenedPage[0]),
                                    "Provider-backed addon route action should land on Survival Route");
                            helper.assertTrue(providerId.toString().equals(resolveScreenCoreData(
                                            survivalContext, "missionBrowser.providerFilter")),
                                    "Provider-backed addon route action should keep the selected provider filter");
                            Identifier[] addonArchivesOpenedPage = new Identifier[1];
                            helper.assertTrue(runScreenCoreAction(screenCoreActionId("OPEN_ADDON_ARCHIVES"),
                                            providerId.toString(), Map.of(), survivalContext,
                                            screenCoreControls(addonArchivesOpenedPage)),
                                    "Addon archive action should open the shared archive hub");
                            helper.assertTrue(id("terminal_archives").equals(addonArchivesOpenedPage[0]),
                                    "Addon archive action should navigate to the Field Archive page");
                            helper.assertTrue(providerId.toString().equals(resolveScreenCoreData(
                                            survivalContext, "archives.group")),
                                    "Addon archive action should apply the addon archive group filter");
                            Identifier[] addonDiagnosticsOpenedPage = new Identifier[1];
                            helper.assertTrue(runScreenCoreAction(screenCoreActionId("OPEN_ADDON_DIAGNOSTICS"),
                                            providerId.toString(), Map.of(), survivalContext,
                                            screenCoreControls(addonDiagnosticsOpenedPage)),
                                    "Addon diagnostics action should open Data Core");
                            helper.assertTrue(id("terminal_data_core").equals(addonDiagnosticsOpenedPage[0]),
                                    "Addon diagnostics action should navigate to the Data Core page");
                            helper.assertTrue(providerId.toString().equals(resolveScreenCoreData(
                                            survivalContext, "dataCore.filter")),
                                    "Addon diagnostics action should apply the addon diagnostics filter");
                            Identifier[] addonConfigOpenedPage = new Identifier[1];
                            helper.assertTrue(runScreenCoreAction(screenCoreActionId("OPEN_ADDON_CONFIG"),
                                            providerId.toString(), Map.of(), survivalContext,
                                            screenCoreControls(addonConfigOpenedPage)),
                                    "Addon config action should open the settings surface");
                            helper.assertTrue(id("terminal_settings").equals(addonConfigOpenedPage[0]),
                                    "Addon config action should navigate to Terminal settings");

                            Identifier[] vitalsOpenedPage = new Identifier[1];
                            helper.assertTrue(runScreenCoreAction(screenCoreActionId("OPEN_HAZARD_MAP"),
                                            "", Map.of(), survivalContext, screenCoreControls(vitalsOpenedPage)),
                                    "Hazard map shortcut should use a concrete vitals route");
                            helper.assertTrue(id("terminal_vitals").equals(vitalsOpenedPage[0]),
                                    "Hazard map shortcut should navigate to the Vitals page");
                            Identifier[] discoveryOpenedPage = new Identifier[1];
                            helper.assertTrue(runScreenCoreAction(screenCoreActionId("PING_SCAN"),
                                            "", Map.of(), survivalContext, screenCoreControls(discoveryOpenedPage)),
                                    "Ping Scan should open discovery with a concrete scanned-state filter");
                            helper.assertTrue(id("terminal_discovery_grid").equals(discoveryOpenedPage[0]),
                                    "Ping Scan should navigate to Discovery Grid");
                            helper.assertTrue("all".equals(resolveScreenCoreData(
                                            survivalContext, "discoveryGrid.category")),
                                    "Ping Scan should reset the discovery category filter");
                            helper.assertTrue("discovered".equals(resolveScreenCoreData(
                                            survivalContext, "discoveryGrid.state")),
                                    "Ping Scan should show discovered scan results");
                            Identifier[] probeOpenedPage = new Identifier[1];
                            helper.assertTrue(runScreenCoreAction(screenCoreActionId("DEPLOY_PROBE"),
                                            "", Map.of(), survivalContext, screenCoreControls(probeOpenedPage)),
                                    "Deploy Probe should open the mission graph and reset provider scoping");
                            helper.assertTrue(id("terminal_mission_graph").equals(probeOpenedPage[0]),
                                    "Deploy Probe should navigate to the Mission Graph page");
                            helper.assertTrue("all".equals(resolveScreenCoreData(
                                            survivalContext, "missionBrowser.providerFilter")),
                                    "Deploy Probe should clear addon-specific mission filters");
                            helper.assertTrue(runScreenCoreAction(screenCoreActionId("REWARD_DEFER"),
                                            "terminal-cache", Map.of(), survivalContext, null),
                                    "Reward Defer should be a real ScreenCore action");
                            helper.assertTrue(Boolean.TRUE.equals(resolveScreenCoreData(
                                            survivalContext, "rewardInbox.deferred")),
                                    "Reward Defer should update visible reward inbox state");
                            Identifier[] viewedOpenedPage = new Identifier[1];
                            helper.assertTrue(runScreenCoreAction(screenCoreActionId("REWARD_MARK_VIEWED"),
                                            "terminal-cache", Map.of(), survivalContext,
                                            screenCoreControls(viewedOpenedPage)),
                                    "Mark Viewed should be a real ScreenCore action");
                            helper.assertTrue(id("terminal_overview").equals(viewedOpenedPage[0]),
                                    "Mark Viewed should return to the Command Deck");
                            helper.assertTrue(Boolean.TRUE.equals(resolveScreenCoreData(
                                            survivalContext, "rewardInbox.viewed")),
                                    "Mark Viewed should update visible reward inbox state");
                            helper.assertTrue(Boolean.FALSE.equals(resolveScreenCoreData(
                                            survivalContext, "rewardInbox.deferred")),
                                    "Mark Viewed should clear deferred reward state");

                            helper.assertTrue(runScreenCoreAction(screenCoreActionId("RECIPE_SELECT_ITEM"),
                                            "minecraft:stick", Map.of(), survivalContext, null),
                                    "Recipe item-cell action should filter by item id instead of parsing it as a recipe id");
                            helper.assertTrue("minecraft:stick".equals(resolveScreenCoreData(
                                            survivalContext, "recipeIndex.query")),
                                    "Recipe item-cell action should update recipe search text");
                            helper.assertTrue("uses".equals(resolveScreenCoreData(
                                            survivalContext, "recipeIndex.mode")),
                                    "Recipe item-cell action should switch recipe mode to uses by default");
                            helper.assertTrue(((Number) resolveScreenCoreData(
                                            survivalContext, "recipeIndex.visibleCount")).intValue() > 0,
                                    "Recipe item id filtering should keep matching recipes visible");

                            helper.assertFalse(runScreenCoreAction(screenCoreActionId("RECIPE_SET_MODE"),
                                            "invalid", Map.of(), survivalContext, null),
                                    "Recipe mode actions should reject unknown modes");
                            helper.assertTrue("uses".equals(resolveScreenCoreData(
                                            survivalContext, "recipeIndex.mode")),
                                    "Rejected recipe modes should not mutate visible ScreenCore state");
                            helper.assertTrue(runScreenCoreAction(screenCoreActionId("RECIPE_SET_MODE"),
                                            "sources", Map.of(), survivalContext, null),
                                    "Known recipe mode actions should update visible state");
                            helper.assertTrue("sources".equals(resolveScreenCoreData(
                                            survivalContext, "recipeIndex.mode")),
                                    "Known recipe mode should be visible through ScreenCore data");
                            Identifier recipeId = id("alpha_provider/recipe");
                            helper.assertTrue(runScreenCoreAction(screenCoreActionId("RECIPE_SELECT_RECIPE"),
                                            recipeId.toString(), Map.of(), survivalContext, null),
                                    "Recipe selection should continue to use recipe ids");
                            helper.assertTrue(recipeId.toString().equals(resolveScreenCoreData(
                                            survivalContext, "recipeIndex.selectedRecipeId")),
                                    "Selected recipe id should be visible through ScreenCore data");
                            helper.assertTrue(runScreenCoreAction(screenCoreActionId("RECIPE_TOGGLE_CATEGORY"),
                                            id("alpha_category").toString(), Map.of(), survivalContext, null),
                                    "Recipe category row should apply a concrete category filter");
                            helper.assertTrue(id("alpha_category").toString().equals(resolveScreenCoreData(
                                            survivalContext, "recipeIndex.category")),
                                    "Recipe category filter should be visible through ScreenCore data");
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> recipeCategories = (List<Map<String, Object>>)
                                    resolveScreenCoreData(survivalContext, "recipeIndex.categories");
                            helper.assertTrue(recipeCategories.stream()
                                            .allMatch(row -> !String.valueOf(row.get("compactTitle")).isBlank()
                                                    && String.valueOf(row.get("compactTitle")).length() <= 14),
                                    "Recipe category rows should expose compact labels for the horizontal ScreenCore tab strip");
                            helper.assertTrue(((Number) resolveScreenCoreData(
                                            survivalContext, "recipeIndex.visibleCount")).intValue() > 0,
                                    "Recipe category filter should keep provider-backed rows visible");
                            helper.assertTrue("".equals(resolveScreenCoreData(
                                            survivalContext, "recipeIndex.selectedRecipeId")),
                                    "Changing recipe category should clear stale recipe selection");
                            helper.assertTrue(runScreenCoreAction(screenCoreActionId("RECIPE_SELECT_ITEM"),
                                            "Stick", Map.of("mode", "uses"), survivalContext, null),
                                    "Recipe item-cell action should also accept item labels as search text");
                            helper.assertTrue("Stick".equals(resolveScreenCoreData(
                                            survivalContext, "recipeIndex.query")),
                                    "Item label filtering should update visible search text");
                        } catch (ReflectiveOperationException | LinkageError exception) {
                            throw new AssertionError("Failed to exercise Terminal ScreenCore parity state", exception);
                        }
                    }))));
        } catch (ReflectiveOperationException | LinkageError exception) {
            throw new AssertionError("Failed to prepare Terminal ScreenCore parity state", exception);
        }
        helper.succeed();
    }

    private static void terminalScreenCoreRecipeIndexCache(GameTestHelper helper) {
        if (!screenCoreLoaded()) {
            helper.succeed();
            return;
        }
        try {
            screenCoreActionsClass().getMethod("register").invoke(null);
            screenCoreDataProvidersClass().getMethod("resetStateForTests").invoke(null);
            AtomicInteger categoryCalls = new AtomicInteger();
            AtomicInteger recipeCalls = new AtomicInteger();
            TerminalRecipeRegistry.withClearedForTests(() -> {
                try {
                    TerminalRecipeRegistry.register(new LargeRecipeProvider(
                            id("large_provider"), 160, categoryCalls, recipeCalls));
                    Object context = screenCoreMissingPlaceholder(newScreenCoreDataContext(), "");
                    long initialBuilds = recipeUiBuildCountForTests();
                    helper.assertTrue(((Number) resolveScreenCoreData(context, "recipeIndex.visibleCount")).intValue() == 100,
                            "ScreenCore recipe index should cap visible rows at 100");
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> recipes = (List<Map<String, Object>>) resolveScreenCoreData(context, "recipeIndex.recipes");
                    helper.assertTrue(recipes.size() == 100,
                            "ScreenCore recipe rows should expose only the capped visible page");
                    helper.assertTrue(recipeUiBuildCountForTests() == initialBuilds + 1,
                            "First recipe binding pass should build one cached ScreenCore view");
                    helper.assertTrue(categoryCalls.get() == 1 && recipeCalls.get() == 1,
                            "First recipe binding pass should build one provider snapshot");

                    resolveScreenCoreData(context, "recipeIndex.visibleCount");
                    resolveScreenCoreData(context, "recipeIndex.recipes");
                    resolveScreenCoreData(context, "recipeIndex.selectedRecipe.title");
                    helper.assertTrue(recipeUiBuildCountForTests() == initialBuilds + 1,
                            "Repeated recipe bindings should reuse the cached ScreenCore view");
                    helper.assertTrue(categoryCalls.get() == 1 && recipeCalls.get() == 1,
                            "Repeated recipe bindings should reuse the cached provider snapshot");

                    Identifier selectedRecipe = id("large_provider/recipe_42");
                    helper.assertTrue(runScreenCoreAction(screenCoreActionId("RECIPE_SELECT_RECIPE"),
                                    selectedRecipe.toString(), Map.of(), context, null),
                            "Visible recipe rows should remain selectable by id");
                    helper.assertTrue("Bulk Recipe 42".equals(resolveScreenCoreData(context, "recipeIndex.selectedRecipe.title")),
                            "Selected recipe detail should resolve from the cached visible rows");
                    helper.assertTrue(recipeUiBuildCountForTests() == initialBuilds + 1,
                            "Selecting a visible recipe should not rebuild the cached visible rows");

                    helper.assertTrue(runScreenCoreAction(screenCoreActionId("RECIPE_SEARCH_CHANGED"),
                                    "needle", Map.of(), context, null),
                            "Recipe search action should update ScreenCore recipe filter state");
                    helper.assertTrue(((Number) resolveScreenCoreData(context, "recipeIndex.visibleCount")).intValue() == 1,
                            "Search should narrow the cached recipe rows to matching entries");
                    helper.assertTrue(recipeUiBuildCountForTests() == initialBuilds + 2,
                            "Changing recipe search should rebuild only the ScreenCore visible view");
                    helper.assertTrue(categoryCalls.get() == 1 && recipeCalls.get() == 1,
                            "Changing recipe search should not rebuild the provider snapshot");

                    TerminalRecipeRegistry.register(new DummyRecipeProvider(id("revision_provider"), 30));
                    helper.assertTrue(((Number) resolveScreenCoreData(context, "recipeIndex.visibleCount")).intValue() == 1,
                            "Registry revision changes should preserve stable search results");
                    helper.assertTrue(recipeUiBuildCountForTests() == initialBuilds + 3,
                            "Registry revision changes should rebuild the ScreenCore recipe view");
                    helper.assertTrue(categoryCalls.get() == 2 && recipeCalls.get() == 2,
                            "Registry revision changes should rebuild the provider snapshot");
                } catch (ReflectiveOperationException exception) {
                    throw new AssertionError("Failed to exercise Terminal ScreenCore recipe index cache", exception);
                }
            });
        } catch (ReflectiveOperationException | LinkageError exception) {
            throw new AssertionError("Failed to prepare Terminal ScreenCore recipe index cache test", exception);
        }
        helper.succeed();
    }

    private static void terminalScreenCoreMissionBrowserCache(GameTestHelper helper) {
        if (!screenCoreLoaded()) {
            helper.succeed();
            return;
        }
        try {
            screenCoreActionsClass().getMethod("register").invoke(null);
            screenCoreDataProvidersClass().getMethod("resetStateForTests").invoke(null);
            Identifier providerId = id("screen_mission_browser_cache_provider");
            Identifier missionId = id("screen_mission_browser_cache_main");
            Identifier sideId = id("screen_mission_browser_cache_side");
            CountingRouteMissionProvider provider = new CountingRouteMissionProvider(
                    providerId,
                    "Mission Browser Cache",
                    List.of(new ConfiguredMission(
                            missionId,
                            "Cache Main",
                            "Cache",
                            "Route",
                            "Main",
                            TerminalMissionRole.MAIN,
                            TerminalMissionStatus.UNLOCKED,
                            List.of(TerminalMissionAction.enabled("scan_cache", "SCAN CACHE"))),
                            new ConfiguredMission(
                                    sideId,
                                    "Cache Side",
                                    "Cache",
                                    "Optional",
                                    "Side",
                                    TerminalMissionRole.OPTIONAL,
                                    TerminalMissionStatus.CLAIMABLE,
                                    List.of(TerminalMissionAction.enabled("claim_cache", "CLAIM CACHE")),
                                    Optional.of(missionId),
                                    List.of())));

            TerminalMissionRegistry.withClearedForTests(() -> {
                MainSurvivalQuestProvider.INSTANCE.clearCacheForTests();
                TerminalMissionRegistry.register(provider);
                try {
                    Object context = putScreenCoreData(newScreenCoreDataContext(),
                            "terminal.activeTabId", MainSurvivalQuestProvider.TAB_ID.toString());
                    long initialBuilds = missionBrowserUiBuildCountForTests();

                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> visibleMissions = (List<Map<String, Object>>)
                            resolveScreenCoreData(context, "missionBrowser.visibleMissions");
                    helper.assertTrue(visibleMissions.stream()
                                    .anyMatch(row -> missionId.toString().equals(String.valueOf(row.get("id")))),
                            "ScreenCore mission browser cache fixture should expose the main route mission");
                    helper.assertTrue(missionBrowserUiBuildCountForTests() == initialBuilds + 1,
                            "First mission browser binding should build one cached ScreenCore view");

                    resolveScreenCoreData(context, "missionBrowser.roadmapRows");
                    resolveScreenCoreData(context, "missionBrowser.selectedMission.title");
                    resolveScreenCoreData(context, "missionBrowser.selectedMission.sideCards");
                    resolveScreenCoreData(context, "missionBrowser.selectedPhase.title");
                    resolveScreenCoreData(context, "missionBrowser.currentProvider.title");
                    resolveScreenCoreData(context, "missionBrowser.activeCompactLabel");
                    resolveScreenCoreData(context, "missionBrowser.readyLabel");
                    resolveScreenCoreData(context, "missionBrowser.routeProgressLabel");
                    helper.assertTrue(missionBrowserUiBuildCountForTests() == initialBuilds + 1,
                            "Repeated mission browser bindings should reuse the cached ScreenCore view");

                    helper.assertTrue(runScreenCoreAction(screenCoreActionId("SELECT_MISSION"),
                                    missionId.toString(), Map.of(), context, null),
                            "Mission browser cache test should select a visible mission");
                    helper.assertTrue("Cache Main".equals(resolveScreenCoreData(
                                    context, "missionBrowser.selectedMission.title")),
                            "Selected mission should resolve after mission selection invalidates the cache");
                    helper.assertTrue(missionBrowserUiBuildCountForTests() == initialBuilds + 2,
                            "Selecting a mission should invalidate and rebuild the mission browser cache once");

                    helper.assertTrue(runScreenCoreAction(screenCoreActionId("SELECT_MISSION_PROVIDER"),
                                    providerId.toString(), Map.of(), context, null),
                            "Mission browser cache test should select a provider filter source");
                    resolveScreenCoreData(context, "missionBrowser.activeCompactLabel");
                    helper.assertTrue(missionBrowserUiBuildCountForTests() == initialBuilds + 3,
                            "Selecting a mission provider should invalidate and rebuild the cache once");

                    helper.assertTrue(runScreenCoreAction(screenCoreActionId("PERFORM_MISSION_ACTION"),
                                    missionId.toString(), Map.of("action_id", "scan_cache"), context, null),
                            "Mission browser cache test should dispatch a mission action");
                    resolveScreenCoreData(context, "missionBrowser.selectedMission.primaryActionId");
                    helper.assertTrue(missionBrowserUiBuildCountForTests() == initialBuilds + 4,
                            "Mission action dispatch should invalidate and rebuild the cache once");
                } catch (ReflectiveOperationException | LinkageError exception) {
                    throw new AssertionError("Failed to exercise ScreenCore mission browser cache", exception);
                }
            });
        } catch (ReflectiveOperationException | LinkageError exception) {
            throw new AssertionError("Failed to prepare ScreenCore mission browser cache test", exception);
        } finally {
            try {
                screenCoreDataProvidersClass().getMethod("resetStateForTests").invoke(null);
            } catch (ReflectiveOperationException | LinkageError ignored) {
            }
            MainSurvivalQuestProvider.INSTANCE.clearCacheForTests();
        }
        helper.succeed();
    }

    private static void terminalScreenCoreOverviewRouteCache(GameTestHelper helper) {
        if (!screenCoreLoaded()) {
            helper.succeed();
            return;
        }
        try {
            screenCoreDataProvidersClass().getMethod("resetStateForTests").invoke(null);
            Identifier providerId = id("screen_overview_cache_provider");
            Identifier mainId = id("screen_overview_cache_main");
            Identifier sideId = id("screen_overview_cache_side");
            CountingRouteMissionProvider provider = new CountingRouteMissionProvider(
                    providerId,
                    "Overview Cache",
                    List.of(new ConfiguredMission(
                            mainId,
                            "Cache Main",
                            "Cache",
                            "Route",
                            "Main",
                            TerminalMissionRole.MAIN,
                            TerminalMissionStatus.UNLOCKED,
                            List.of()),
                            new ConfiguredMission(
                                    sideId,
                                    "Cache Side",
                                    "Cache",
                                    "Optional",
                                    "Side",
                                    TerminalMissionRole.OPTIONAL,
                                    TerminalMissionStatus.CLAIMABLE,
                                    List.of(TerminalMissionAction.enabled("open_cache", "OPEN")),
                                    Optional.of(mainId),
                                    List.of())));

            TerminalMissionRegistry.withClearedForTests(() -> {
                MainSurvivalQuestProvider.INSTANCE.clearCacheForTests();
                TerminalMissionRegistry.register(provider);
                try {
                    Object context = putScreenCoreData(newScreenCoreDataContext(),
                            "terminal.activeTabId", MainSurvivalQuestProvider.TAB_ID.toString());
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> sideOps = (List<Map<String, Object>>)
                            resolveScreenCoreData(context, "overview.sideOps");
                    helper.assertTrue(sideOps.stream()
                                    .anyMatch(row -> sideId.toString().equals(String.valueOf(row.get("missionId")))),
                            "Command Deck side ops should resolve route-linked records from the aggregate route");
                    int callsAfterFirstRouteBinding = provider.missionCalls().get();
                    helper.assertTrue(callsAfterFirstRouteBinding == 1,
                            "Command Deck route build should read the owning provider mission list once");

                    resolveScreenCoreData(context, "overview.bestNextAction.title");
                    resolveScreenCoreData(context, "overview.routeStatus.progressPercent");
                    resolveScreenCoreData(context, "overview.sideOps");
                    helper.assertTrue(provider.missionCalls().get() == callsAfterFirstRouteBinding,
                            "Command Deck overview bindings should reuse the cached route snapshot");
                } catch (ReflectiveOperationException | LinkageError exception) {
                    throw new AssertionError("Failed to exercise ScreenCore overview route cache", exception);
                }
            });
        } catch (ReflectiveOperationException | LinkageError exception) {
            throw new AssertionError("Failed to prepare ScreenCore overview route cache test", exception);
        } finally {
            try {
                screenCoreDataProvidersClass().getMethod("resetStateForTests").invoke(null);
            } catch (ReflectiveOperationException | LinkageError ignored) {
            }
            MainSurvivalQuestProvider.INSTANCE.clearCacheForTests();
        }
        helper.succeed();
    }

    private static void terminalScreenCoreNativeMissionFallback(GameTestHelper helper) {
        if (!screenCoreLoaded()) {
            helper.succeed();
            return;
        }
        try {
            screenCoreDataProvidersClass().getMethod("resetStateForTests").invoke(null);
            Identifier mainMissionId = Identifier.fromNamespaceAndPath("echoashfallprotocol", "secure_crash_outpost");
            Identifier sideMissionId = Identifier.fromNamespaceAndPath("echoashfallprotocol", "native_route_side_cache");
            ConfigurableMissionProvider directProvider = new ConfigurableMissionProvider(
                    MainSurvivalQuestProvider.CHAPTER_ID,
                    "Native MissionCore Direct",
                    5,
                    List.of(new ConfiguredMission(
                            mainMissionId,
                            "Native Starter Route",
                            "Podfall",
                            "Survival",
                            "Low",
                            TerminalMissionRole.MAIN,
                            TerminalMissionStatus.UNLOCKED,
                            List.of(TerminalMissionAction.enabled("start", "Track")),
                            List.of(TerminalMissionReward.of(new ItemStack(Items.TORCH, 8)))),
                            new ConfiguredMission(
                                    sideMissionId,
                                    "Native Cache Check",
                                    "Podfall",
                                    "Optional",
                                    "Low",
                                    TerminalMissionRole.OPTIONAL,
                                    TerminalMissionStatus.CLAIMABLE,
                                    List.of(TerminalMissionAction.enabled("claim_reward", "Claim")),
                                    Optional.of(mainMissionId),
                                    List.of())));

            TerminalMissionRegistry.withClearedForTests(() -> {
                MainSurvivalQuestProvider.INSTANCE.clearCacheForTests();
                TerminalMissionRegistry.register(directProvider);
                try {
                    Object context = putScreenCoreData(newScreenCoreDataContext(),
                            "terminal.activeTabId", MainSurvivalQuestProvider.TAB_ID.toString());

                    helper.assertTrue("Anchor Pod Outpost".equals(resolveScreenCoreData(
                                    context, "overview.bestNextAction.title")),
                            "Command Deck should use direct MissionCore rows when the aggregate Survival Route is empty");
                    helper.assertTrue(mainMissionId.toString().equals(resolveScreenCoreData(
                                    context, "overview.bestNextAction.missionId")),
                            "Command Deck fallback should preserve the loaded MissionCore mission id");

                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> visibleMissions = (List<Map<String, Object>>)
                            resolveScreenCoreData(context, "missionBrowser.visibleMissions");
                    helper.assertTrue(visibleMissions.stream()
                                    .anyMatch(row -> mainMissionId.toString().equals(String.valueOf(row.get("id")))),
                            "Survival Route browser should show direct MissionCore missions when aggregate rows are empty");

                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> sideOps = (List<Map<String, Object>>)
                            resolveScreenCoreData(context, "overview.sideOps");
                    helper.assertTrue(sideOps.stream()
                                    .anyMatch(row -> sideMissionId.toString().equals(String.valueOf(row.get("missionId")))),
                            "Command Deck side ops should retain route-linked direct MissionCore missions");
                } catch (ReflectiveOperationException | LinkageError exception) {
                    throw new AssertionError("Failed to exercise ScreenCore Native mission fallback", exception);
                }
            });

            Identifier lateProgressId = Identifier.fromNamespaceAndPath("echoashfallprotocol", "native_late_progress");
            NativeLateProgressMissionProvider lateProgressProvider = new NativeLateProgressMissionProvider(
                    Identifier.fromNamespaceAndPath("echoashfallprotocol", "ashfall_protocol"),
                    "Native Late Progress",
                    7,
                    List.of(new ConfiguredMission(
                            lateProgressId,
                            "Native Definition Route",
                            "Podfall",
                            "Survival",
                            "Low",
                            TerminalMissionRole.MAIN,
                            TerminalMissionStatus.UNLOCKED,
                            List.of(TerminalMissionAction.enabled("start", "Track")))));
            TerminalMissionRegistry.withClearedForTests(() -> {
                MainSurvivalQuestProvider.INSTANCE.clearCacheForTests();
                TerminalMissionRegistry.register(lateProgressProvider);
                List<TerminalMissionDefinition> routeRows =
                        MainSurvivalQuestProvider.INSTANCE.missions(helper.makeMockPlayer(GameType.SURVIVAL));
                helper.assertTrue(routeRows.stream()
                                .anyMatch(row -> lateProgressId.equals(row.id())),
                        "Survival Route should retry definition-only mission rows when live player progress is late.");
            });

            Identifier staleProviderId = Identifier.fromNamespaceAndPath("echoashfallprotocol", "stale_native_provider");
            Identifier staleMissionId = Identifier.fromNamespaceAndPath("echoashfallprotocol", "stale_native_mission");
            Identifier loadedProviderId = Identifier.fromNamespaceAndPath("echomissioncore", "missions");
            Identifier loadedMissionId = Identifier.fromNamespaceAndPath("echoashfallprotocol", "native_loaded_after_filter");
            ConfigurableMissionProvider staleProvider = new ConfigurableMissionProvider(
                    staleProviderId,
                    "Stale Native Provider",
                    9,
                    List.of(new ConfiguredMission(
                            staleMissionId,
                            "Stale Native Route",
                            "Podfall",
                            "Survival",
                            "Low",
                            TerminalMissionRole.MAIN,
                            TerminalMissionStatus.UNLOCKED,
                            List.of(TerminalMissionAction.enabled("start", "Track")))));
            ConfigurableMissionProvider loadedProvider = new ConfigurableMissionProvider(
                    loadedProviderId,
                    "MissionCore",
                    10,
                    List.of(new ConfiguredMission(
                            loadedMissionId,
                            "Loaded Native Route",
                            "Podfall",
                            "Survival",
                            "Low",
                            TerminalMissionRole.MAIN,
                            TerminalMissionStatus.UNLOCKED,
                            List.of(TerminalMissionAction.enabled("start", "Track")))));
            TerminalMissionRegistry.withClearedForTests(() -> {
                MainSurvivalQuestProvider.INSTANCE.clearCacheForTests();
                TerminalMissionRegistry.register(staleProvider);
                try {
                    screenCoreActionsClass().getMethod("register").invoke(null);
                    screenCoreDataProvidersClass().getMethod("resetStateForTests").invoke(null);
                    Object context = putScreenCoreData(newScreenCoreDataContext(),
                            "terminal.activeTabId", MainSurvivalQuestProvider.TAB_ID.toString());
                    Identifier[] openedPage = new Identifier[1];
                    helper.assertTrue(runScreenCoreAction(screenCoreActionId("OPEN_PROVIDER_ROUTE"),
                                    staleProviderId.toString(), Map.of(), context, screenCoreControls(openedPage)),
                            "Native provider route action should be able to set a provider-scoped mission filter.");
                    helper.assertTrue(staleProviderId.toString().equals(resolveScreenCoreData(
                                    context, "missionBrowser.providerFilter")),
                            "Native provider route action should expose the provider-scoped filter before content changes.");

                    TerminalMissionRegistry.clearForTests();
                    MainSurvivalQuestProvider.INSTANCE.clearCacheForTests();
                    TerminalMissionRegistry.register(loadedProvider);
                    screenCoreDataProvidersClass().getMethod("invalidateMissionData").invoke(null);
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> visibleMissions = (List<Map<String, Object>>)
                            resolveScreenCoreData(context, "missionBrowser.visibleMissions");
                    helper.assertTrue(visibleMissions.stream()
                                    .anyMatch(row -> loadedMissionId.toString().equals(String.valueOf(row.get("id")))),
                            "ScreenCore should recover from a stale provider filter and show newly loaded MissionCore rows.");
                    helper.assertTrue("all".equals(resolveScreenCoreData(context, "missionBrowser.providerFilter")),
                            "ScreenCore should reset stale Native provider filters once MissionCore owns the mission feed.");
                } catch (ReflectiveOperationException | LinkageError exception) {
                    throw new AssertionError("Failed to exercise stale Native provider filter recovery", exception);
                }
            });

            Identifier ashfallAliasProviderId =
                    Identifier.fromNamespaceAndPath("echoashfallprotocol", "ashfall_protocol");
            Identifier ashfallMissionCoreId =
                    Identifier.fromNamespaceAndPath("echoashfallprotocol", "ashfall_loaded_via_missioncore");
            ConfigurableMissionProvider ashfallAliasProvider = new ConfigurableMissionProvider(
                    ashfallAliasProviderId,
                    "Ashfall Protocol",
                    11,
                    List.of());
            ConfigurableMissionProvider ashfallMissionCoreProvider = new ConfigurableMissionProvider(
                    loadedProviderId,
                    "MissionCore",
                    12,
                    List.of(new ConfiguredMission(
                            ashfallMissionCoreId,
                            "Ashfall MissionCore Route",
                            "Podfall",
                            "Survival",
                            "Low",
                            TerminalMissionRole.MAIN,
                            TerminalMissionStatus.UNLOCKED,
                            List.of(TerminalMissionAction.enabled("start", "Track")))));
            TerminalMissionRegistry.withClearedForTests(() -> {
                MainSurvivalQuestProvider.INSTANCE.clearCacheForTests();
                TerminalMissionRegistry.register(ashfallAliasProvider);
                TerminalMissionRegistry.register(ashfallMissionCoreProvider);
                try {
                    screenCoreActionsClass().getMethod("register").invoke(null);
                    screenCoreDataProvidersClass().getMethod("resetStateForTests").invoke(null);
                    Object context = putScreenCoreData(newScreenCoreDataContext(),
                            "terminal.activeTabId", MainSurvivalQuestProvider.TAB_ID.toString());
                    Identifier[] openedPage = new Identifier[1];
                    helper.assertTrue(runScreenCoreAction(screenCoreActionId("OPEN_PROVIDER_ROUTE"),
                                    ashfallAliasProviderId.toString(), Map.of(), context, screenCoreControls(openedPage)),
                            "Native Ashfall route action should allow addon-scoped mission filters.");
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> visibleMissions = (List<Map<String, Object>>)
                            resolveScreenCoreData(context, "missionBrowser.visibleMissions");
                    helper.assertTrue(visibleMissions.stream()
                                    .anyMatch(row -> ashfallMissionCoreId.toString().equals(String.valueOf(row.get("id")))),
                            "ScreenCore should show MissionCore rows whose mission namespace matches the Ashfall route filter.");
                    helper.assertTrue(ashfallAliasProviderId.toString().equals(resolveScreenCoreData(
                                    context, "missionBrowser.providerFilter")),
                            "ScreenCore should keep valid Ashfall namespace filters instead of treating them as stale.");
                } catch (ReflectiveOperationException | LinkageError exception) {
                    throw new AssertionError("Failed to exercise Ashfall MissionCore namespace filter recovery", exception);
                }
            });
        } catch (ReflectiveOperationException | LinkageError exception) {
            throw new AssertionError("Failed to prepare ScreenCore Native mission fallback test", exception);
        } finally {
            try {
                screenCoreDataProvidersClass().getMethod("resetStateForTests").invoke(null);
            } catch (ReflectiveOperationException | LinkageError ignored) {
            }
            MainSurvivalQuestProvider.INSTANCE.clearCacheForTests();
        }
        helper.succeed();
    }

    private static void terminalScreenCoreNativeLinkageProviderFallback(GameTestHelper helper) {
        if (!screenCoreLoaded()) {
            helper.succeed();
            return;
        }
        try {
            EchoCoreServices.clearPlatformServicesForTests();
            screenCoreDataProvidersClass().getMethod("resetStateForTests").invoke(null);
            EchoCoreServices.registerHazardTelemetryService(ignored -> {
                throw nativeAttachmentLinkageError();
            });
            EchoCoreServices.registerDiagnosticService(ignored -> {
                throw nativeAttachmentLinkageError();
            });
            EchoCoreServices.registerRouteRecordService(ignored -> {
                throw nativeAttachmentLinkageError();
            });

            Object context = putScreenCoreData(newScreenCoreDataContext(),
                    "terminal.activeTabId", MainSurvivalQuestProvider.TAB_ID.toString());
            helper.assertTrue("Field systems nominal.".equals(resolveScreenCoreData(context, "shell.status.primary")),
                    "ScreenCore shell should fall back to nominal telemetry when an optional Native provider links late.");
            helper.assertTrue("0 checks".equals(resolveScreenCoreData(context, "shell.status.diagnosticsLabel")),
                    "ScreenCore shell should hide diagnostics from a provider with missing Native backend classes.");
            helper.assertTrue("0 routes".equals(resolveScreenCoreData(context, "shell.status.routesLabel")),
                    "ScreenCore shell should hide route records from a provider with missing Native backend classes.");
            Object routeRecords = resolveScreenCoreData(context, "routeRecords.visible");
            helper.assertTrue(routeRecords instanceof List<?> && ((List<?>) routeRecords).isEmpty(),
                    "Route record data provider should degrade to an empty list after a Native linkage failure.");
        } catch (ReflectiveOperationException | LinkageError exception) {
            throw new AssertionError("Failed to exercise Native linkage provider fallback", exception);
        } finally {
            EchoCoreServices.clearPlatformServicesForTests();
            try {
                screenCoreDataProvidersClass().getMethod("resetStateForTests").invoke(null);
            } catch (ReflectiveOperationException | LinkageError ignored) {
            }
        }
        helper.succeed();
    }

    private static NoClassDefFoundError nativeAttachmentLinkageError() {
        return new NoClassDefFoundError("net/neoforged/neoforge/attachment/IAttachmentSerializer");
    }

    private static void terminalAddonInfoRegistry(GameTestHelper helper) {
        TerminalAddonInfoRegistry.withClearedForTests(() -> {
            TerminalAddonInfo alpha = new TerminalAddonInfo(
                    "  Alpha summary  ",
                    Arrays.asList(
                            new TerminalAddonMetric("  State  ", "  ONLINE  ", "  Live details  ", TerminalUi.GREEN),
                            null),
                    Arrays.asList(new TerminalAddonSection("  Notes  ", Arrays.asList("  first  ", "", null, "second")), null),
                    Arrays.asList(new TerminalAddonLink(id("alpha_tab"), "  Open Alpha  ", "  Linked page  ", TerminalUi.CYAN), null));
            TerminalAddonInfoRegistry.register(new DummyAddonInfoProvider("zeta_chapter", TerminalAddonInfo.empty()));
            TerminalAddonInfoRegistry.register(new DummyAddonInfoProvider("alpha_chapter", alpha));

            helper.assertTrue(TerminalAddonInfoRegistry.providers().size() == 2,
                    "Addon info registry should expose registered providers");
            helper.assertTrue("alpha_chapter".equals(TerminalAddonInfoRegistry.providers().get(0).chapterId()),
                    "Addon info providers should sort by chapter id");

            TerminalAddonInfo resolved = TerminalAddonInfoRegistry.info(" alpha_chapter ", null);
            helper.assertTrue("Alpha summary".equals(resolved.summary()),
                    "Addon info should normalize summary text");
            helper.assertTrue(resolved.metrics().size() == 1
                            && "State".equals(resolved.metrics().get(0).label())
                            && "ONLINE".equals(resolved.metrics().get(0).value()),
                    "Addon info metrics should be immutable and null-safe");
            helper.assertTrue(resolved.sections().size() == 1
                            && resolved.sections().get(0).lines().equals(List.of("first", "second")),
                    "Addon info sections should drop blank lines");
            helper.assertTrue(resolved.links().size() == 1 && resolved.links().get(0).targetTabId().equals(id("alpha_tab")),
                    "Addon info links should preserve lowercase terminal tab targets");
            helper.assertTrue(resolved.guide() == TerminalAddonGuide.empty(),
                    "Legacy addon info constructors should use an empty guide snapshot");

            TerminalAddonInfo guided = new TerminalAddonInfo(
                    "Guided",
                    null,
                    null,
                    null,
                    new TerminalAddonGuide("  Chapter 9  ", 90, "  Test stage  ", "  Test hint  ",
                            Arrays.asList("  step one  ", "", null), true));
            helper.assertTrue("Chapter 9".equals(guided.guide().label())
                            && guided.guide().mainline()
                            && guided.guide().starterSteps().equals(List.of("step one")),
                    "Addon guides should normalize text and starter steps");
            TerminalAddonInfo nullGuide = new TerminalAddonInfo("Null guide", null, null, null, null);
            helper.assertTrue(nullGuide.guide() == TerminalAddonGuide.empty(),
                    "Null addon guides should fall back to the empty guide snapshot");

            TerminalAddonInfoRegistry.register(new NullAddonInfoProvider("null_output"));
            helper.assertTrue(TerminalAddonInfoRegistry.info("null_output", null) == TerminalAddonInfo.empty(),
                    "Null provider output should fall back to the empty snapshot");

            TerminalAddonInfoRegistry.register(new ThrowingAddonInfoProvider("throwing_output"));
            helper.assertTrue(TerminalAddonInfoRegistry.info("throwing_output", null) == TerminalAddonInfo.empty(),
                    "Failing provider output should fall back to the empty snapshot");

            TerminalAddonInfoRegistry.register(new ThrowingAddonChapterIdProvider());
            helper.assertTrue(TerminalAddonInfoRegistry.providers().size() == 4,
                    "Providers with failing chapter ids should be ignored");

            boolean duplicateRejected = false;
            try {
                TerminalAddonInfoRegistry.register(new DummyAddonInfoProvider("alpha_chapter", TerminalAddonInfo.empty()));
            } catch (IllegalArgumentException expected) {
                duplicateRejected = true;
            }
            helper.assertTrue(duplicateRejected, "Duplicate addon info provider ids must fail fast");

            boolean uppercaseRejected = false;
            try {
                TerminalAddonInfoRegistry.register(new DummyAddonInfoProvider("Bad_Chapter", TerminalAddonInfo.empty()));
            } catch (IllegalArgumentException expected) {
                uppercaseRejected = true;
            }
            helper.assertTrue(uppercaseRejected, "Addon info provider chapter ids must reject uppercase");
        });
        helper.succeed();
    }

    private static void terminalAddonGuideOrdering(GameTestHelper helper) {
        TerminalAddonInfoRegistry.withClearedForTests(() -> {
            List<String> order = BuiltinTerminalTabs.addonGuideOrderForTests(List.of(
                    new DummyAddonChapter("unknown_alpha", "ECHO: Unknown Alpha"),
                    new DummyAddonChapter("blackbox_protocol", "ECHO: Blackbox Protocol"),
                    new DummyAddonChapter("industrial_nexus", "ECHO: Industrial Nexus"),
                    new DummyAddonChapter("ashfall_protocol", "ECHO: Ashfall Protocol"),
                    new DummyAddonChapter("stationfall", "ECHO: Stationfall"),
                    new DummyAddonChapter("orbital_remnants", "ECHO: Orbital Remnants"),
                    new DummyAddonChapter("nexus_protocol", "ECHO: Nexus Protocol")));
            helper.assertTrue(order.equals(List.of(
                            "Chapter 1|ashfall_protocol",
                            "Chapter 2|orbital_remnants",
                            "Chapter 3|stationfall",
                            "Chapter 4|nexus_protocol",
                            "Chapter 5|blackbox_protocol",
                            "Optional|industrial_nexus",
                            "Optional|unknown_alpha")),
                    "Chapter guide should sort story chapters before optional and unknown addons");
            TerminalAddonGuide industrial = BuiltinTerminalTabs.addonGuideForTests("industrial_nexus");
            helper.assertTrue("Optional".equals(industrial.label()) && !industrial.mainline(),
                    "Industrial Nexus should remain an optional chapter guide entry");
        });
        helper.succeed();
    }

    private static void terminalNavigationProfiles(GameTestHelper helper) {
        TerminalNavigationProfiles.withClearedForTests(() -> {
            helper.assertTrue(TerminalNavigationSection.storyFirstOrder().equals(List.of(
                            TerminalNavigationSection.COMMAND,
                            TerminalNavigationSection.CHAPTERS,
                            TerminalNavigationSection.INTEL,
                            TerminalNavigationSection.INDEX,
                            TerminalNavigationSection.HOLOMAP,
                            TerminalNavigationSection.SYSTEM)),
                    "Terminal navigation should render the six readability-first top-level sections");

            TerminalNavigationProfile command = TerminalNavigationProfiles.profileFor(
                    new DummyTab(id("overview"), "OVERVIEW", 0));
            helper.assertTrue(command.section() == TerminalNavigationSection.COMMAND,
                    "Legacy protocol tabs should fall back into the Command section");

            helper.assertTrue(TerminalNavigationProfile.terminal(10).section() == TerminalNavigationSection.COMMAND,
                    "Legacy Terminal profiles should canonicalize to Command");
            helper.assertTrue(TerminalNavigationProfile.core(10).section() == TerminalNavigationSection.INTEL,
                    "Legacy Core profiles should canonicalize to Intel");
            helper.assertTrue(TerminalNavigationProfile.index(10).section() == TerminalNavigationSection.INDEX,
                    "Index profiles should render under the top-level Index section");
            helper.assertTrue(TerminalNavigationProfile.holomap(10).section() == TerminalNavigationSection.HOLOMAP,
                    "HoloMap profiles should render under the top-level HoloMap section");
            helper.assertTrue(TerminalNavigationSection.fromKey("TERMINAL") == TerminalNavigationSection.COMMAND,
                    "Legacy Terminal section keys should resolve to Command");
            helper.assertTrue(TerminalNavigationSection.fromKey("CORE") == TerminalNavigationSection.INTEL,
                    "Legacy Core section keys should resolve to Intel");

            TerminalNavigationProfile endgame = TerminalNavigationProfiles.profileFor(new DummyChromeTab(
                    new TerminalTabDescriptor(id("legacy_endgame"), "ENDGAME", 220, 0xFFC77DFF),
                    TerminalTabChrome.of("Legacy Endgame", TerminalTabChrome.GROUP_ENDGAME, "EG",
                            "Legacy finale", 220)));
            helper.assertTrue(endgame.section() == TerminalNavigationSection.INTEL,
                    "Legacy endgame tabs should not create a standalone Endgame section");

            TerminalNavigationProfile nexus = TerminalNavigationProfiles.profileFor(new DummyChromeTab(
                    new TerminalTabDescriptor(id("legacy_nexus"), "NEXUS", 230, 0xFFC77DFF),
                    TerminalTabChrome.of("Legacy Nexus", TerminalTabChrome.GROUP_NEXUS, "NX",
                            "Legacy finale", 230)));
            helper.assertTrue(nexus.section() == TerminalNavigationSection.INTEL,
                    "Nexus tabs require an explicit addon profile before they appear as beta chapter navigation");

            TerminalNavigationProfiles.register(MainSurvivalQuestProvider.TAB_ID,
                    TerminalNavigationProfile.progress(0));
            TerminalNavigationProfiles.register(VanillaJourneyProvider.TAB_ID,
                    TerminalNavigationProfile.progress(50));
            TerminalNavigationProfile survivalRoute =
                    TerminalNavigationProfiles.profile(MainSurvivalQuestProvider.TAB_ID).orElse(null);
            helper.assertTrue(survivalRoute != null
                            && survivalRoute.section() == TerminalNavigationSection.CHAPTERS,
                    "Survival Route should be the main Progress section destination");

            Identifier addons = id("addons");
            TerminalNavigationProfiles.register(addons, TerminalNavigationProfile.progress(150));
            TerminalNavigationProfile chapterStatus = TerminalNavigationProfiles.profile(addons).orElse(null);
            helper.assertTrue(chapterStatus != null
                            && chapterStatus.section() == TerminalNavigationSection.CHAPTERS,
                    "Mods should live in the Progress section while preserving the addons tab id");

            Map<Identifier, TerminalNavigationProfile> builtinProfiles =
                    BuiltinTerminalTabs.builtinNavigationProfilesForTests();
            helper.assertTrue(builtinProfiles.get(id("overview")).section() == TerminalNavigationSection.COMMAND,
                    "Command Deck should live in Command");
            helper.assertFalse(builtinProfiles.containsKey(id("diagnostics")),
                    "Standalone diagnostics should be folded into Command Deck instead of registered as a Command page");
            helper.assertTrue(builtinProfiles.get(MainSurvivalQuestProvider.TAB_ID)
                            .section() == TerminalNavigationSection.CHAPTERS,
                    "Survival Route should live in Progress");
            helper.assertTrue(builtinProfiles.get(VanillaJourneyProvider.TAB_ID)
                            .section() == TerminalNavigationSection.CHAPTERS,
                    "Baseline should expose the standalone vanilla route in Progress");
            helper.assertTrue(builtinProfiles.get(id("mission_graph")).section() == TerminalNavigationSection.HOLOMAP,
                    "Mission Graph should live in HoloMap as a route diagnostic");
            helper.assertTrue(builtinProfiles.get(MainSurvivalQuestProvider.TAB_ID).order()
                            < builtinProfiles.get(VanillaJourneyProvider.TAB_ID).order()
                            && builtinProfiles.get(VanillaJourneyProvider.TAB_ID).order()
                            < builtinProfiles.get(id("addons")).order(),
                    "Baseline should sit between the aggregate Survival Route and Mods");
            helper.assertTrue(builtinProfiles.get(id("addons")).section() == TerminalNavigationSection.CHAPTERS,
                    "Mods should live in Progress");
            helper.assertTrue(builtinProfiles.get(id("route_records")).section() == TerminalNavigationSection.HOLOMAP,
                    "Route Records should live in HoloMap");
            helper.assertTrue(builtinProfiles.get(DiscoveryGridTab.TAB_ID).section() == TerminalNavigationSection.HOLOMAP,
                    "Discovery Grid should live in HoloMap");
            helper.assertTrue(builtinProfiles.get(id("route_records")).order()
                            < builtinProfiles.get(DiscoveryGridTab.TAB_ID).order()
                            && builtinProfiles.get(DiscoveryGridTab.TAB_ID).order()
                            < builtinProfiles.get(id("faction_atlas")).order(),
                    "Discovery Grid should sit between Route Records and Faction Atlas");
            helper.assertTrue(builtinProfiles.get(id("faction_atlas")).section() == TerminalNavigationSection.INTEL,
                    "Faction Atlas should live in Intel");
            helper.assertTrue(builtinProfiles.get(TerminalRecipeIndexTab.TAB_ID).section() == TerminalNavigationSection.INDEX,
                    "Recipe Index should live in the top-level Index section");
            helper.assertTrue(builtinProfiles.get(id("archives")).section() == TerminalNavigationSection.INTEL,
                    "Field Archive should live in Intel");
            helper.assertTrue(builtinProfiles.get(id("vitals")).section() == TerminalNavigationSection.SYSTEM,
                    "Vitals should live in System");
            helper.assertTrue(builtinProfiles.get(id("reward_inbox")).section() == TerminalNavigationSection.SYSTEM,
                    "Reward Inbox should live in System");
            helper.assertTrue(builtinProfiles.get(id("settings")).section() == TerminalNavigationSection.SYSTEM,
                    "Interface Settings should live in System");

            Identifier stationfall = id("stationfall");
            TerminalNavigationProfiles.register(stationfall,
                    TerminalNavigationProfile.chapter("stationfall", "Chapter 3: Stationfall", "C3", 330));
            TerminalNavigationProfile stationProfile = TerminalNavigationProfiles.profile(stationfall).orElse(null);
            helper.assertTrue(stationProfile != null, "Registered navigation profiles should be discoverable");
            helper.assertTrue(stationProfile.section() == TerminalNavigationSection.CHAPTERS,
                    "Addon profiles should live in the Progress mod section");
            helper.assertTrue("stationfall".equals(stationProfile.chapterId()),
                    "Addon profiles should keep their chapter workspace id");
            helper.assertTrue("Chapter 3: Stationfall".equals(stationProfile.chapterTitle()),
                    "Addon profiles should expose numbered chapter titles");

            TerminalTab survivalTab = new DummyChromeTab(
                    new TerminalTabDescriptor(MainSurvivalQuestProvider.TAB_ID, "SURVIVAL ROUTE", 0, 0xFF92F7A6),
                    TerminalTabChrome.of("Survival Route", TerminalTabChrome.GROUP_FIELD, "SR",
                            "Main survival quest line", 0));
            TerminalTab baselineTab = new DummyChromeTab(
                    new TerminalTabDescriptor(VanillaJourneyProvider.TAB_ID, "BASELINE", 50, 0xFF92F7A6),
                    TerminalTabChrome.of("Baseline", TerminalTabChrome.GROUP_FIELD, "BL",
                            "Minecraft advancement route", 50));
            TerminalTab addonsTab = new DummyChromeTab(
                    new TerminalTabDescriptor(addons, "MODS", 150, 0xFFFFD166),
                    TerminalTabChrome.of("Mods", TerminalTabChrome.GROUP_CORE, "MD",
                            "Installed chapter review", 150));
            TerminalTab stationTab = new DummyChromeTab(
                    new TerminalTabDescriptor(stationfall, "STATIONFALL", 330, 0xFFFF536A),
                    TerminalTabChrome.of("Stationfall", TerminalTabChrome.GROUP_ORBITAL, "SF",
                            "Station route records", 330));
            List<TerminalTab> progressTabs = List.of(survivalTab, baselineTab, addonsTab, stationTab);
            helper.assertTrue(EchoTerminalScreen.progressNavigationRowsForTests(progressTabs, 0, false)
                            .equals(List.of(
                                    "PAGE:Survival Route",
                                    "PAGE:Baseline",
                                    "PAGE:Mods",
                                    "CHAPTER:Stationfall")),
                    "Progress content tabs should keep direct pages first and expose chapter entries in the main area");
            helper.assertTrue(EchoTerminalScreen.progressNavigationRowsForTests(progressTabs, 0, true)
                            .contains("CHAPTER:Stationfall"),
                    "Progress content tabs should expose chapter reference entries without nested sidebar rows");
            helper.assertTrue(EchoTerminalScreen.progressNavigationRowsForTests(progressTabs, 3, false)
                            .equals(List.of(
                                    "PAGE:Survival Route",
                                    "PAGE:Baseline",
                                    "PAGE:Mods",
                                    "CHAPTER:Stationfall")),
                    "Active chapter tabs should stay in the main content tabs instead of expanding the sidebar");
        });
        helper.succeed();
    }

    private static void terminalDiscoveryGridFilters(GameTestHelper helper) {
        EchoCoreServices.clearPlatformServicesForTests();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        EchoDiscoveryEntry locked = discoveryEntry("locked_structure", EchoDiscoveryCategory.STRUCTURE,
                "Locked Structure", 10);
        EchoDiscoveryEntry discovered = discoveryEntry("discovered_biome", EchoDiscoveryCategory.BIOME,
                "Discovered Biome", 20);
        EchoDiscoveryEntry checked = discoveryEntry("checked_guardian", EchoDiscoveryCategory.GUARDIAN,
                "Checked Guardian", 30);

        EchoCoreServices.registerDiscoveryProvider(new com.echoplatform.echocore.api.EchoDiscoveryProvider() {
            @Override
            public List<EchoDiscoveryEntry> entries(Player player) {
                return List.of(locked, discovered, checked);
            }

            @Override
            public EchoDiscoveryState state(Player player, EchoDiscoveryEntry entry) {
                return checked.id().equals(entry.id()) ? EchoDiscoveryState.CHECKED : EchoDiscoveryState.LOCKED;
            }
        });

        helper.assertTrue(recordDiscoveredForTest(player, discovered.id()),
                "Stored discovery should record the discovered card once");
        helper.assertTrue(DiscoveryGridTab.visibleEntriesForTests(player, null, null).size() == 3,
                "Discovery Grid should include all registered entries with no filters");
        helper.assertTrue(DiscoveryGridTab.visibleEntriesForTests(player, EchoDiscoveryCategory.STRUCTURE, null)
                        .equals(List.of(locked)),
                "Discovery Grid category filter should isolate structures");
        helper.assertTrue(DiscoveryGridTab.visibleEntriesForTests(player, null, EchoDiscoveryState.LOCKED)
                        .equals(List.of(locked)),
                "Discovery Grid locked filter should keep provider-locked hint cards");
        helper.assertTrue(DiscoveryGridTab.visibleEntriesForTests(player, null, EchoDiscoveryState.DISCOVERED)
                        .equals(List.of(discovered)),
                "Discovery Grid discovered filter should include stored discoveries");
        helper.assertTrue(DiscoveryGridTab.visibleEntriesForTests(player, null, EchoDiscoveryState.CHECKED)
                        .equals(List.of(checked)),
                "Discovery Grid checked filter should include live completed entries");
        EchoCoreServices.clearPlatformServicesForTests();
        helper.succeed();
    }

    private static void terminalDiscoveryGridRouteState(GameTestHelper helper) {
        EchoCoreServices.clearPlatformServicesForTests();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        EchoRouteRecord readyRecord = new EchoRouteRecord(
                id("route/ready_route"),
                "terminal_tests",
                "Ready Route",
                "Route",
                "Field",
                "READY",
                "Ready route summary",
                false);
        EchoRouteRecord completeRecord = new EchoRouteRecord(
                id("route/complete_route"),
                "terminal_tests",
                "Complete Route",
                "Route",
                "Field",
                "COMPLETE",
                "Complete route summary",
                true);
        Identifier readyDiscoveryId = EchoCoreServices.routeDiscoveryId(readyRecord.id());
        Identifier completeDiscoveryId = EchoCoreServices.routeDiscoveryId(completeRecord.id());

        EchoCoreServices.registerRouteRecordService(ignored -> List.of(readyRecord, completeRecord));
        EchoCoreServices.registerDiscoveryProvider(new TerminalDiscoveryProvider());

        helper.assertTrue(DiscoveryGridTab.visibleEntriesForTests(player, null, EchoDiscoveryState.LOCKED).stream()
                        .anyMatch(entry -> entry.id().equals(readyDiscoveryId)),
                "READY route records should stay locked until a persisted discovery exists");
        helper.assertTrue(DiscoveryGridTab.visibleEntriesForTests(player, null, EchoDiscoveryState.DISCOVERED).stream()
                        .noneMatch(entry -> entry.id().equals(readyDiscoveryId)),
                "READY route records should not reveal from route status alone");
        helper.assertTrue(DiscoveryGridTab.visibleEntriesForTests(player, null, EchoDiscoveryState.CHECKED).stream()
                        .anyMatch(entry -> entry.id().equals(completeDiscoveryId)),
                "Complete route records should resolve as checked from live progression");
        EchoDiscoveryEntry lockedEntry = EchoCoreServices.discoveryEntries(player).stream()
                .filter(entry -> entry.id().equals(readyDiscoveryId))
                .findFirst()
                .orElseThrow();
        helper.assertTrue(!lockedEntry.lockedHintTitle().equals(lockedEntry.revealedTitle())
                        && !lockedEntry.hintText().isBlank(),
                "Locked route cards should keep hint-only metadata");

        helper.assertTrue(recordDiscoveredForTest(player, readyDiscoveryId),
                "Test discovery seed should persist the READY route id once");
        helper.assertTrue(recordDiscoveredForTest(player, completeDiscoveryId),
                "Test discovery seed should persist the COMPLETE route id once");
        helper.assertTrue(EchoCoreServices.hasDiscoveredFeature(player, readyDiscoveryId),
                "Visible route discovery should persist the READY route id");
        helper.assertTrue(DiscoveryGridTab.visibleEntriesForTests(player, null, EchoDiscoveryState.DISCOVERED).stream()
                        .anyMatch(entry -> entry.id().equals(readyDiscoveryId)),
                "Persisted route discovery should reveal non-complete route cards");
        helper.assertFalse(recordDiscoveredForTest(player, readyDiscoveryId),
                "Duplicate route discovery should remain silent");

        EchoCoreServices.clearPlatformServicesForTests();
        helper.succeed();
    }

    private static void terminalDiscoveryGridBatchResolution(GameTestHelper helper) {
        EchoCoreServices.clearPlatformServicesForTests();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        EchoDiscoveryEntry locked = discoveryEntry("batch_locked_structure", EchoDiscoveryCategory.STRUCTURE,
                "Batch Locked Structure", 10);
        EchoDiscoveryEntry discovered = discoveryEntry("batch_discovered_biome", EchoDiscoveryCategory.BIOME,
                "Batch Discovered Biome", 20);
        EchoDiscoveryEntry checked = discoveryEntry("batch_checked_guardian", EchoDiscoveryCategory.GUARDIAN,
                "Batch Checked Guardian", 30);
        AtomicInteger entriesCalls = new AtomicInteger();

        EchoCoreServices.registerDiscoveryProvider(new EchoDiscoveryProvider() {
            @Override
            public List<EchoDiscoveryEntry> entries(Player player) {
                entriesCalls.incrementAndGet();
                return List.of(locked, discovered, checked);
            }

            @Override
            public EchoDiscoveryState state(Player player, EchoDiscoveryEntry entry) {
                return checked.id().equals(entry.id()) ? EchoDiscoveryState.CHECKED : EchoDiscoveryState.LOCKED;
            }
        });

        helper.assertTrue(recordDiscoveredForTest(player, discovered.id()),
                "Stored discovery should seed the batch resolver");
        List<EchoResolvedDiscoveryEntry> resolved = EchoCoreServices.resolvedDiscoveryEntries(player);
        helper.assertTrue(entriesCalls.get() == 1,
                "Resolved discovery batch should call provider entries once per snapshot");
        helper.assertTrue(stateOf(resolved, locked) == EchoDiscoveryState.LOCKED,
                "Resolved discovery batch should preserve locked provider entries");
        helper.assertTrue(stateOf(resolved, discovered) == EchoDiscoveryState.DISCOVERED,
                "Resolved discovery batch should overlay stored discoveries");
        helper.assertTrue(stateOf(resolved, checked) == EchoDiscoveryState.CHECKED,
                "Resolved discovery batch should preserve checked live state");

        entriesCalls.set(0);
        helper.assertTrue(DiscoveryGridTab.visibleEntriesForTests(player, null, EchoDiscoveryState.CHECKED)
                        .equals(List.of(checked)),
                "Discovery Grid test resolver should use batch state filtering");
        helper.assertTrue(entriesCalls.get() == 1,
                "Discovery Grid filtering should reuse one resolved batch instead of resolving per card");
        EchoCoreServices.clearPlatformServicesForTests();
        helper.succeed();
    }

    private static void terminalRenderContextNavigation(GameTestHelper helper) {
        Identifier target = id("target_tab");
        List<Identifier> visited = new ArrayList<>();
        TerminalRenderContext inert = new TerminalRenderContext(null, null,
                0, 0, 0, 0, 0, 0, 0, null, null);
        helper.assertFalse(inert.canNavigateToTab(target),
                "Terminal render contexts without navigation callbacks should reject tab navigation safely");
        inert.navigateToTab(target);

        TerminalRenderContext navigable = new TerminalRenderContext(null, null,
                0, 0, 0, 0, 0, 0, 0, visited::add, target::equals);
        helper.assertTrue(navigable.canNavigateToTab(target),
                "Terminal render contexts should expose available local tab destinations");
        navigable.navigateToTab(target);
        helper.assertTrue(visited.size() == 1 && visited.get(0).equals(target),
                "Terminal render contexts should call the local tab navigation callback");
        helper.succeed();
    }

    private static void terminalThemeRegistry(GameTestHelper helper) {
        TerminalThemeRegistry.setDefaultTheme(BuiltinTerminalThemes.ECHO_CONSOLE);
        helper.assertTrue(TerminalThemeRegistry.byId(null).id().equals(BuiltinTerminalThemes.ECHO_CONSOLE),
                "Theme registry should fall back to the default ECHO console theme");
        helper.assertTrue(TerminalThemeRegistry.byId(BuiltinTerminalThemes.NEXUS_MODPACK).displayName()
                        .equals("Nexus Modpack"),
                "Built-in Nexus Modpack theme should be registered");
        helper.assertFalse(TerminalThemeRegistry.setDefaultTheme(id("missing_theme")),
                "Theme registry should reject unregistered default theme ids");
        helper.assertTrue(TerminalThemeRegistry.setDefaultTheme(BuiltinTerminalThemes.NEXUS_MODPACK),
                "Theme registry should accept a registered default theme id");
        helper.assertTrue(TerminalThemeRegistry.defaultThemeId().equals(BuiltinTerminalThemes.NEXUS_MODPACK),
                "Theme registry should expose the active default theme id");
        helper.assertTrue(TerminalThemeRegistry.byId(null).id().equals(BuiltinTerminalThemes.NEXUS_MODPACK),
                "Theme registry should resolve null theme ids to the active default theme");
        helper.assertTrue(TerminalThemeRegistry.setDefaultTheme(BuiltinTerminalThemes.ECHO_CONSOLE),
                "Theme registry should allow restoring the built-in ECHO console default");
        boolean duplicateRejected = false;
        try {
            TerminalThemeRegistry.register(BuiltinTerminalThemes.echoConsole());
        } catch (IllegalArgumentException expected) {
            duplicateRejected = true;
        }
        helper.assertTrue(duplicateRejected, "Duplicate terminal theme ids should be rejected");
        helper.succeed();
    }

    private static void terminalThemeIconFallback(GameTestHelper helper) {
        Identifier fallback = id("fallback_icon");
        TerminalIconSet icons = TerminalIconSet.builder()
                .fallback(fallback)
                .icon(TerminalIconKey.action("claim"), id("claim_icon"))
                .build();
        helper.assertTrue(icons.resolve(TerminalIconKey.action("claim")).equals(id("claim_icon")),
                "Icon sets should resolve registered semantic icons");
        helper.assertTrue(icons.resolve(TerminalIconKey.action("missing")).equals(fallback),
                "Icon sets should fall back when a semantic icon is missing");
        boolean nullIconRejected = false;
        try {
            TerminalIconSet.builder().icon(TerminalIconKey.action("bad"), null);
        } catch (NullPointerException expected) {
            nullIconRejected = true;
        }
        helper.assertTrue(nullIconRejected, "Icon sets should reject null icon textures");
        TerminalTheme nexus = TerminalThemeRegistry.byId(BuiltinTerminalThemes.NEXUS_MODPACK);
        helper.assertTrue(nexus.icon(TerminalIconKey.action("claim"), TerminalThemeContext.empty(), null) != null,
                "Built-in themes should expose semantic action icons");
        helper.succeed();
    }

    private static void terminalThemeChapterStyle(GameTestHelper helper) {
        TerminalTheme nexus = TerminalThemeRegistry.byId(BuiltinTerminalThemes.NEXUS_MODPACK);
        TerminalThemeContext industrial = new TerminalThemeContext(
                id("industrial_tab"), "chapters", "echoindustrialnexus", "Industrial Nexus",
                "echoindustrialnexus", 0, true, false);
        TerminalChapterStyle style = nexus.chapterStyle(industrial);
        helper.assertTrue("echoindustrialnexus".equals(style.key()),
                "Theme chapter styles should resolve by active namespace");
        helper.assertTrue(style.banner() != null && style.icons().resolve(TerminalIconKey.chapter("echoindustrialnexus")) != null,
                "Chapter styles should provide banner and chapter icon assets");
        TerminalThemeContext unknown = new TerminalThemeContext(id("unknown_tab"), "", "", "", "unknownaddon", 0, true, false);
        helper.assertTrue(nexus.chapterStyle(unknown).equals(nexus.fallbackChapterStyle()),
                "Unknown namespaces should use the theme fallback chapter style");
        helper.succeed();
    }

    private static void terminalThemeResources(GameTestHelper helper) {
        TerminalTheme echo = TerminalThemeRegistry.byId(BuiltinTerminalThemes.ECHO_CONSOLE);
        TerminalTheme nexus = TerminalThemeRegistry.byId(BuiltinTerminalThemes.NEXUS_MODPACK);
        TerminalThemeContext context = new TerminalThemeContext(id("industrial_tab"), "chapters",
                "echoindustrialnexus", "Industrial Nexus", "echoindustrialnexus", 0, true, false);
        List<TerminalIconKey> keys = List.of(
                TerminalIconKey.theme("brand"),
                TerminalIconKey.theme("settings"),
                TerminalIconKey.theme("cycle"),
                TerminalIconKey.action("claim"),
                TerminalIconKey.action("theme_cycle"),
                TerminalIconKey.state("claimable"),
                TerminalIconKey.state("blocker"),
                TerminalIconKey.state("empty"),
                TerminalIconKey.reward("inbox"),
                TerminalIconKey.page("command_deck"),
                TerminalIconKey.page("reward_inbox"),
                TerminalIconKey.chapter("echoindustrialnexus"),
                TerminalIconKey.fallback("unknown"));
        Identifier mission = Identifier.fromNamespaceAndPath("echoashfallprotocol", "acquire_mutagen");
        List<Identifier> visuals = List.of(
                TerminalVisualAssets.TERMINAL_FRAME_BACKDROP,
                TerminalVisualAssets.MISSIONS_VISUAL_HERO,
                TerminalVisualAssets.CARD_PANEL_DETAIL_STANDARD,
                TerminalVisualAssets.CARD_METRIC_TILE_PLATE,
                TerminalVisualAssets.CARD_FILTER_TOOLBAR_PLATE,
                TerminalVisualAssets.CARD_EMPTY_STATE_PLATE,
                TerminalVisualAssets.CARD_ACTION_BAR_PLATE,
                TerminalVisualAssets.ICON_ACTION_CLAIM,
                TerminalVisualAssets.MISSION_ICON_SURVIVAL,
                TerminalVisualAssets.missionIconArt(mission, "story"),
                TerminalVisualAssets.missionHeroArt(mission, "story"));
        for (TerminalTheme theme : List.of(echo, nexus)) {
            for (TerminalIconKey key : keys) {
                Identifier texture = theme.icon(key, context, null);
                helper.assertTrue(texture != null && classpathResourceExists(texture),
                        theme.displayName() + " semantic icon should point at a packaged PNG: " + key + " -> " + texture);
                helper.assertTrue(pngHasTransparentCorners(texture),
                        theme.displayName() + " semantic icon should preserve transparent corners: " + texture);
            }
            for (Identifier visual : visuals) {
                Identifier themed = theme.visual(visual);
                helper.assertTrue(themed != null && classpathResourceExists(themed),
                        theme.displayName() + " visual override should point at a packaged PNG: " + visual + " -> " + themed);
            }
            helper.assertTrue(classpathResourceExists(theme.visual(theme.tokens().assets().shellBackdrop())),
                    theme.displayName() + " shell backdrop should be packaged");
            helper.assertTrue(classpathResourceExists(theme.chapterStyle(context).banner()),
                    theme.displayName() + " chapter banner should be packaged");
            helper.assertTrue(classpathResourceExists(theme.chapterStyle(context).panel()),
                    theme.displayName() + " chapter panel should be packaged");
        }
        helper.assertFalse(classpathResourceExists(Identifier.fromNamespaceAndPath(EchoTerminal.MODID,
                        "textures/gui/themes/nexus_modpack/backgrounds/asset_sheet_source.png")),
                "Generated source sheet should not ship as a runtime theme asset");
        helper.assertFalse(classpathResourceExists(Identifier.fromNamespaceAndPath(EchoTerminal.MODID,
                        "textures/gui/themes/echo_console/backgrounds/asset_sheet_source.png")),
                "Generated source sheet should not ship as a runtime theme asset");
        assertRuntimeTextureBudget(helper);
        assertRuntimeTextureDimensionCaps(helper);
        helper.succeed();
    }

    private static void terminalThemeSelection(GameTestHelper helper) {
        helper.assertTrue(TerminalThemeRegistry.setDefaultTheme(BuiltinTerminalThemes.NEXUS_MODPACK),
                "Theme registry should allow tests to switch the active default theme");
        TerminalClientOptions.resetThemeForTests(null);
        helper.assertTrue(TerminalClientOptions.selectedThemeId().equals(BuiltinTerminalThemes.NEXUS_MODPACK),
                "Missing client theme selections should resolve dynamically to the active registry default");
        TerminalClientOptions.resetThemeForTests(BuiltinTerminalThemes.ECHO_CONSOLE);
        helper.assertTrue(TerminalClientOptions.selectedThemeId().equals(BuiltinTerminalThemes.ECHO_CONSOLE),
                "Valid saved client theme selections should be preserved");
        TerminalClientOptions.resetThemeForTests(BuiltinTerminalThemes.NEXUS_MODPACK);
        helper.assertTrue(TerminalClientOptions.selectedThemeId().equals(BuiltinTerminalThemes.NEXUS_MODPACK),
                "Client theme selection should accept registered theme ids");
        TerminalClientOptions.resetThemeForTests(id("missing_theme"));
        helper.assertTrue(TerminalClientOptions.selectedThemeId().equals(TerminalThemeRegistry.defaultThemeId()),
                "Client theme selection should fall back when the stored theme id is missing");
        helper.assertTrue(TerminalThemeRegistry.setDefaultTheme(BuiltinTerminalThemes.ECHO_CONSOLE),
                "Theme registry should allow tests to restore the built-in default theme");
        helper.succeed();
    }

    private static void terminalClientOptionsConfig(GameTestHelper helper) {
        Path root;
        try {
            root = Files.createTempDirectory("echoterminal-client-options");
        } catch (IOException exception) {
            throw new AssertionError("Failed to create temporary terminal options directory", exception);
        }
        try {
            Path missing = root.resolve("missing.properties");
            TerminalClientOptions.reloadFromPathForTests(missing);
            helper.assertTrue(Files.isRegularFile(missing),
                    "Missing terminal client options file should be regenerated");
            Properties regenerated = loadProperties(missing);
            helper.assertTrue("true".equals(regenerated.getProperty("screenCoreExperimentalTabs")),
                    "Regenerated terminal client options should enable ScreenCore shell tabs by default");
            helper.assertTrue("true".equals(regenerated.getProperty("useScreenCore")),
                    "Regenerated terminal client options should keep ScreenCore enabled by default");
            helper.assertTrue("true".equals(regenerated.getProperty("useCyberglassScreenCoreTheme")),
                    "Regenerated terminal client options should keep the cyberglass ScreenCore theme enabled");

            Path explicitFalse = root.resolve("explicit-false.properties");
            Files.writeString(explicitFalse, "screenCoreExperimentalTabs=false\n");
            TerminalClientOptions.reloadFromPathForTests(explicitFalse);
            helper.assertFalse(TerminalClientOptions.screenCoreExperimentalTabs(),
                    "Explicit saved ScreenCore shell opt-out should be respected");
            Properties falseBackfilled = loadProperties(explicitFalse);
            helper.assertTrue("false".equals(falseBackfilled.getProperty("screenCoreExperimentalTabs")),
                    "Backfill should preserve explicit ScreenCore shell false values");
            helper.assertTrue(falseBackfilled.containsKey("cyberglassDensity"),
                    "Older terminal client options files should be backfilled with newer keys");

            Path partial = root.resolve("partial.properties");
            Files.writeString(partial, "useScreenCore=false\n");
            TerminalClientOptions.reloadFromPathForTests(partial);
            Properties partialBackfilled = loadProperties(partial);
            helper.assertTrue("false".equals(partialBackfilled.getProperty("useScreenCore")),
                    "Backfill should preserve explicit non-default settings");
            helper.assertTrue("true".equals(partialBackfilled.getProperty("screenCoreExperimentalTabs")),
                    "Missing ScreenCore shell key should be backfilled with the current default");

            Path corrupt = root.resolve("corrupt.properties");
            String corruptContent = "theme=bad\\u00ZZ\nscreenCoreExperimentalTabs=false\n";
            Files.writeString(corrupt, corruptContent);
            TerminalClientOptions.reloadFromPathForTests(corrupt);
            helper.assertTrue(corruptContent.equals(Files.readString(corrupt)),
                    "Malformed terminal client options files should not be overwritten");
            helper.assertTrue(TerminalClientOptions.screenCoreExperimentalTabs(),
                    "Malformed terminal client options should fall back to in-memory defaults");
        } catch (IOException exception) {
            throw new AssertionError("Failed to exercise terminal client options config regeneration", exception);
        } finally {
            deleteRecursively(root);
        }
        helper.succeed();
    }

    private static void terminalZoomOptions(GameTestHelper helper) {
        List<String> labels = Arrays.stream(TerminalClientOptions.TerminalZoom.values())
                .map(TerminalClientOptions.TerminalZoom::label)
                .toList();
        helper.assertTrue(labels.equals(List.of("50%", "75%", "85%", "90%", "100%", "110%", "125%", "150%")),
                "Terminal zoom options should preserve legacy presets and add 50%, 75%, and 150%");
        helper.assertTrue(TerminalClientOptions.TerminalZoom.ZOOM_50.scale() == 0.5D,
                "50% terminal zoom should scale to 0.5");
        helper.assertTrue(TerminalClientOptions.TerminalZoom.ZOOM_150.scale() == 1.5D,
                "150% terminal zoom should scale to 1.5");
        TerminalScreenTheme theme = TerminalScreenTheme.modular();
        EchoTerminalScreen.LayoutMetrics zoom50 = EchoTerminalScreen.layoutMetricsForTests(
                2048, 1152, theme,
                TerminalClientOptions.InterfaceDensity.BALANCED,
                TerminalClientOptions.TerminalZoom.ZOOM_50,
                false);
        EchoTerminalScreen.LayoutMetrics zoom100 = EchoTerminalScreen.layoutMetricsForTests(
                2048, 1152, theme,
                TerminalClientOptions.InterfaceDensity.BALANCED,
                TerminalClientOptions.TerminalZoom.ZOOM_100,
                false);
        EchoTerminalScreen.LayoutMetrics zoom150 = EchoTerminalScreen.layoutMetricsForTests(
                2048, 1152, theme,
                TerminalClientOptions.InterfaceDensity.BALANCED,
                TerminalClientOptions.TerminalZoom.ZOOM_150,
                false);
        helper.assertTrue(zoom50.panelX() == zoom100.panelX() && zoom100.panelX() == zoom150.panelX()
                        && zoom50.panelY() == zoom100.panelY() && zoom100.panelY() == zoom150.panelY()
                        && zoom50.panelW() == zoom100.panelW() && zoom100.panelW() == zoom150.panelW()
                        && zoom50.panelH() == zoom100.panelH() && zoom100.panelH() == zoom150.panelH(),
                "Terminal zoom should not resize or move the outer shell");
        helper.assertTrue(zoom50.contentX() == zoom100.contentX() && zoom100.contentX() == zoom150.contentX()
                        && zoom50.contentY() == zoom100.contentY() && zoom100.contentY() == zoom150.contentY()
                        && zoom50.contentW() == zoom100.contentW() && zoom100.contentW() == zoom150.contentW()
                        && zoom50.contentH() == zoom100.contentH() && zoom100.contentH() == zoom150.contentH(),
                "Terminal zoom should keep the outer content frame fixed");
        helper.assertTrue(zoom50.renderContentX() < zoom100.renderContentX()
                        && zoom100.renderContentX() < zoom150.renderContentX()
                        && zoom50.renderContentW() > zoom100.renderContentW()
                        && zoom100.renderContentW() > zoom150.renderContentW(),
                "Terminal zoom should affect the padded tab-rendered content viewport");
        for (EchoTerminalScreen.LayoutMetrics metrics : List.of(zoom50, zoom100, zoom150)) {
            helper.assertTrue(metrics.panelW() > 0 && metrics.panelH() > 0,
                    "Terminal shell dimensions should stay positive");
            helper.assertTrue(metrics.contentW() > 0 && metrics.contentH() > 0,
                    "Terminal content frame dimensions should stay positive");
            helper.assertTrue(metrics.renderContentW() > 0 && metrics.renderContentH() > 0,
                    "Terminal rendered content dimensions should stay positive");
        }
        helper.succeed();
    }

    private static void terminalScrollbarMetrics(GameTestHelper helper) {
        TerminalScrollbar.Metrics disabled = TerminalScrollbar.vertical(10, 20, 7, 100, 0, 0);
        helper.assertFalse(disabled.enabled(), "Scrollbars without overflow should not capture mouse input");
        helper.assertFalse(disabled.insideTrack(12, 24), "Disabled scrollbars should ignore track clicks");

        TerminalScrollbar.Metrics metrics = TerminalScrollbar.vertical(10, 20, 7, 100, 40, 200);
        helper.assertTrue(metrics.enabled(), "Overflowing scrollbars should expose drag metrics");
        helper.assertTrue(metrics.insideTrack(12, 40), "Scrollbar track should be mouse-hit-testable");
        helper.assertTrue(metrics.insideThumb(12, metrics.thumbY() + 2),
                "Scrollbar thumb should be mouse-hit-testable");
        int preservedOffset = metrics.dragOffset(metrics.thumbY() + 4);
        int preservedScroll = metrics.scrollForMouse(metrics.thumbY() + 4, preservedOffset);
        helper.assertTrue(Math.abs(preservedScroll - 40) <= 1,
                "Dragging from inside the thumb should preserve the grabbed offset");
        int trackJump = metrics.scrollForTrackClick(metrics.trackY() + metrics.trackH() / 2);
        helper.assertTrue(trackJump > 40 && trackJump < metrics.maxScroll(),
                "Clicking the track should jump the thumb toward the mouse");
        int bottom = metrics.scrollForMouse(metrics.trackY() + metrics.trackH() + 40, metrics.thumbH() / 2);
        helper.assertTrue(bottom == metrics.maxScroll(), "Dragging below the track should clamp at max scroll");
        helper.succeed();
    }

    private static void terminalScreenProviderPrecedence(GameTestHelper helper) {
        helper.assertTrue(EchoTerminalScreens.providerSlotForTests(true, true, true, true)
                        == EchoTerminalScreens.ProviderSlot.PRIMARY,
                "ScreenCore primary provider should win when it supplies a terminal screen");
        helper.assertTrue(EchoTerminalScreens.providerSlotForTests(true, false, true, true)
                        == EchoTerminalScreens.ProviderSlot.FALLBACK,
                "Ashfall fallback provider should open when the primary provider declines");
        helper.assertTrue(EchoTerminalScreens.providerSlotForTests(false, false, true, true)
                        == EchoTerminalScreens.ProviderSlot.FALLBACK,
                "Ashfall fallback provider should open when no primary provider is registered");
        helper.assertTrue(EchoTerminalScreens.providerSlotForTests(true, false, true, false)
                        == EchoTerminalScreens.ProviderSlot.DEFAULT,
                "Default terminal renderer should only open when no provider supplies a screen");
        helper.succeed();
    }

    private static void terminalVisualPolishLayout(GameTestHelper helper) {
        helper.assertTrue(Arrays.asList(TerminalLayoutProfile.values()).equals(List.of(
                        TerminalLayoutProfile.COMPACT_STACK,
                        TerminalLayoutProfile.MEDIUM_CAROUSEL,
                        TerminalLayoutProfile.APP_HUB)),
                "Terminal layout profiles should preserve compact, medium, and app hub breakpoints");
        helper.assertTrue(Arrays.asList(TerminalPageLayout.values()).containsAll(List.of(
                        TerminalPageLayout.DASHBOARD_GRID,
                        TerminalPageLayout.LIST_DETAIL,
                        TerminalPageLayout.HERO_DASHBOARD,
                        TerminalPageLayout.COMMAND_PANEL,
                        TerminalPageLayout.COMPACT_STACK)),
                "Terminal page layouts should expose all visual polish templates");
        TerminalScreenTheme theme = TerminalScreenTheme.modular();
        EchoTerminalScreen.LayoutMetrics appHub = EchoTerminalScreen.layoutMetricsForTests(
                1280, 720, theme, TerminalClientOptions.InterfaceDensity.BALANCED,
                TerminalClientOptions.TerminalZoom.ZOOM_100, false,
                TerminalClientOptions.NavigationStyle.APP_HUB);
        EchoTerminalScreen.LayoutMetrics sidebarHub = EchoTerminalScreen.layoutMetricsForTests(
                1280, 720, theme, TerminalClientOptions.InterfaceDensity.BALANCED,
                TerminalClientOptions.TerminalZoom.ZOOM_100, false,
                TerminalClientOptions.NavigationStyle.SIDEBAR_HUB);
        EchoTerminalScreen.LayoutMetrics compactTop = EchoTerminalScreen.layoutMetricsForTests(
                1280, 720, theme, TerminalClientOptions.InterfaceDensity.BALANCED,
                TerminalClientOptions.TerminalZoom.ZOOM_100, false,
                TerminalClientOptions.NavigationStyle.COMPACT_TOP);
        helper.assertTrue(sidebarHub.groupRailW() < appHub.groupRailW()
                        && sidebarHub.contentW() > appHub.contentW(),
                "Sidebar navigation style should collapse the rail and return width to terminal content");
        helper.assertTrue(compactTop.contentX() == compactTop.groupRailX()
                        && compactTop.contentW() > appHub.contentW()
                        && compactTop.collapseToggleW() == 0,
                "Compact top navigation style should move navigation above a wider content frame");
        for (EchoTerminalScreen.LayoutMetrics metrics : List.of(appHub, sidebarHub, compactTop)) {
            helper.assertTrue(metrics.panelW() > 0 && metrics.panelH() > 0
                            && metrics.contentW() > 0 && metrics.contentH() > 0
                            && metrics.renderContentW() > 0 && metrics.renderContentH() > 0,
                    "Terminal navigation polish should keep all layout dimensions positive");
        }
        helper.assertTrue(BuiltinTerminalTabs.addonConfigControlsStackForTests(260, EchoConfigValueKind.INTEGER),
                "Narrow config rows should stack numeric controls below copy");
        helper.assertFalse(BuiltinTerminalTabs.addonConfigControlsStackForTests(560, EchoConfigValueKind.ENUM),
                "Wide enum config rows should keep controls right-aligned");
        int narrowText = BuiltinTerminalTabs.addonConfigRowHeightForTests(260, EchoConfigValueKind.STRING, true);
        int wideToggle = BuiltinTerminalTabs.addonConfigRowHeightForTests(560, EchoConfigValueKind.BOOLEAN, false);
        helper.assertTrue(narrowText > wideToggle,
                "Stacked text config rows with badges should reserve more height than simple wide toggles");
        for (int width : List.of(160, 260, 420, 720)) {
            helper.assertTrue(TerminalUi.responsiveControlWidth(width, true) > 0,
                    "Responsive text controls should always reserve positive width");
            helper.assertTrue(TerminalUi.responsiveControlRowHeight(width, true, true) > 0,
                    "Responsive text rows should always reserve positive height");
            helper.assertTrue(TerminalUi.responsiveControlRowHeight(width, false, false) > 0,
                    "Responsive toggle rows should always reserve positive height");
        }
        Path euiRoot = euiSourceRoot();
        try {
            String shell = Files.readString(euiRoot.resolve("components").resolve("terminal_shell.eui.xml"));
            helper.assertFalse(shell.contains("terminal.navigation.activeTabs"),
                    "ScreenCore shell should not render page-level tab cards in the header");
            helper.assertTrue(shell.contains("terminal.navigation.sections")
                            && shell.contains("terminal-page-state")
                            && shell.contains("terminal-main-column")
                            && shell.contains("columns=\"182px 1fr\""),
                    "ScreenCore shell should keep one section rail, one page state chip, and one fixed body column");
            String css = Files.readString(euiRoot.resolve("styles").resolve("terminal_cyberglass_v2.eui.css"));
            String routePolishMarker = "Ashfall Survival Route final all-edition polish v2";
            int routePolishIndex = css.lastIndexOf(routePolishMarker);
            String finalRoutePolish = routePolishIndex < 0 ? "" : css.substring(routePolishIndex);
            helper.assertTrue(css.contains("Final compact ScreenCore terminal pass"),
                    "ScreenCore shell should include the compact GUI-scaled polish override");
            helper.assertTrue(css.contains("Terminal audit polish pass"),
                    "ScreenCore terminal CSS should include the consolidated audit polish override");
            helper.assertTrue(css.contains("Terminal completion polish pass")
                            && css.contains(".terminal-recipe-category-copy")
                            && css.contains(".terminal-danger-panel .terminal-hazard-list")
                            && css.contains("button[disabled]"),
                    "Terminal Cyberglass CSS should finish with the shared completion polish pass for rows, hazards, recipe chips, and disabled controls");
            helper.assertTrue(routePolishIndex >= 0,
                    "Survival Route CSS should finish with the all-edition Ashfall route polish override");
            for (String unsupportedRouteCopyProperty : List.of(
                    "title-line-height:",
                    "detail-line-height:",
                    "text-gap:",
                    "content-height:",
                    "title-max-lines:",
                    "detail-max-lines:",
                    "title-wrap:",
                    "detail-wrap:",
                    "padding-bottom:",
                    "margin-bottom:")) {
                helper.assertFalse(finalRoutePolish.contains(unsupportedRouteCopyProperty),
                        "Survival Route final polish should avoid unsupported ScreenCore copy-style property "
                                + unsupportedRouteCopyProperty);
            }
            helper.assertFalse(finalRoutePolish.contains("terminal-mission-detail-scroll")
                            || finalRoutePolish.contains("terminal-route-sideops-scroll"),
                    "Survival Route final polish should not style removed nested right-panel scroll classes");
            helper.assertFalse(css.contains("Final mission-hub polish"),
                    "ScreenCore CSS should not keep a later mission-hub override that re-expands compact layouts");
            helper.assertTrue(css.contains("theme-texture(screencore.surface.raised)")
                            && css.contains("theme-texture(screencore.button)")
                            && css.contains("theme-texture(screencore.status_chip)")
                            && css.contains("theme-texture(screencore.progress_bar)")
                            && css.contains("theme-texture(screencore.edge_rails)")
                            && css.contains("theme-texture(screencore.panel_sheen)")
                            && css.contains("theme-texture(screencore.micro_ticks)"),
                    "Terminal Cyberglass CSS should inherit shared ThemeCore ScreenCore kit textures");
            helper.assertTrue(css.contains("texture-fit: nine-slice")
                            && css.contains("texture-region: 0.078 0.398 0.922 0.716")
                            && css.contains("texture-region: 0.073 0.388 0.907 0.718"),
                    "ScreenCore button chrome should crop the ThemeCore atlas plate instead of stretching the full atlas");
            for (String legacyTexture : List.of(
                    "hud_grid_alpha.png",
                    "edge_rails_alpha.png",
                    "corner_cuts_alpha.png",
                    "panel_sheen_alpha.png",
                    "micro_ticks_alpha.png",
                    "status_chip_glint_alpha.png")) {
                helper.assertFalse(css.contains(legacyTexture),
                        "Terminal Cyberglass CSS should use ThemeCore kit tokens instead of duplicate chrome texture "
                                + legacyTexture);
            }
            helper.assertTrue(css.contains(".terminal-app-grid") && css.contains("columns: 182px 1fr"),
                    "ScreenCore shell should keep the persistent left rail narrow enough for the real GUI scale");
            helper.assertTrue(css.contains(".terminal-page-head") && css.contains("height: 40px"),
                    "ScreenCore shell page header should have an explicit compact fixed height");
            helper.assertTrue(css.contains(".terminal-main-column") && css.contains("height: 420px"),
                    "ScreenCore body wrapper should fit the visible terminal viewport without a phantom scrollbar");
            helper.assertTrue(css.contains(".terminal-main-scroll") && css.contains("height: 420px"),
                    "ScreenCore main viewport should use the available GUI-scaled workspace height");
            helper.assertTrue(css.contains(".terminal-section-row") && css.contains("height: 36px"),
                    "ScreenCore section rail rows should be compact fixed-height controls");
            helper.assertTrue(css.contains(".terminal-section-scroll") && css.contains("height: 252px"),
                    "ScreenCore section rail should expose all six primary sections without giant cards");
            helper.assertTrue(css.contains(".terminal-recipe-layout") && css.contains("height: 342px"),
                    "ScreenCore recipe layout should use more of the viewport without overflowing into the footer");
            helper.assertTrue(css.contains("flexible mission page pass")
                            && css.contains(".terminal-route-briefing-scroll")
                            && css.contains(".terminal-mission-detail-content"),
                    "Survival Route briefing should use one full-column scroll owner for dynamic mission detail");
            helper.assertTrue(css.contains(".terminal-mission-action-strip") && css.contains("height: 42px"),
                    "Survival Route action footer should have a fixed non-clipping ScreenCore row");
            helper.assertTrue(css.contains(".terminal-route-action-button")
                            && css.contains("columns: 1fr 1fr 1fr 1fr 1fr"),
                    "Survival Route action footer should use class-based equal grid button widths");
            helper.assertTrue(routePolishIndex > css.lastIndexOf("Canonical Survival Route split-panel")
                            && routePolishIndex > css.lastIndexOf("flexible mission page pass"),
                    "Survival Route all-edition polish should be the final route sizing cascade");
            helper.assertTrue(css.contains("Canonical Survival Route split-panel")
                            && css.contains(".terminal-route-layout")
                            && css.contains("height: 376px")
                            && css.contains(".terminal-route-briefing-panel")
                            && css.contains(".terminal-route-briefing-card")
                            && css.contains(".terminal-route-sideops-card"),
                    "Survival Route should use one bounded three-column layout with a single scrollable briefing stack");
            helper.assertTrue(css.contains("padding: 0px 0px 56px 0px")
                            && css.contains("padding: 0px 0px 64px 0px")
                            && css.contains(".terminal-route-claim-button"),
                    "Survival Route final polish should reserve bottom clearance and compact the claim action");
            helper.assertFalse(css.contains("height: 452px")
                            || css.contains("height: 404px")
                            || css.contains("420px workspace - 34px summary")
                            || css.contains("Final Survival Route layout clamp")
                            || css.contains("Survival Route briefing/action recovery"),
                    "Survival Route CSS should not keep stale oversized layout clamps that push buttons below the footer");
            helper.assertTrue(css.contains(".terminal-route-row-title")
                            && css.contains(".terminal-route-row-subtitle")
                            && css.contains(".terminal-route-row-progress")
                            && css.contains(".terminal-route-item-icon")
                            && css.contains(".terminal-route-action-button[disabled]"),
                    "Survival Route CSS should directly style route text, item icons, progress, and disabled action states");
            helper.assertTrue(css.contains(".terminal-command-row title")
                            && css.contains(".terminal-action-row title")
                            && css.contains(".terminal-dossier-action-row title")
                            && css.contains(".terminal-map-provider-row title")
                            && css.contains(".terminal-what-now-row title")
                            && css.contains(".terminal-reward-row title")
                            && css.contains(".terminal-recipe-row title")
                            && css.contains(".terminal-record-row title")
                            && css.contains(".terminal-action-row text")
                            && css.contains(".terminal-dossier-action-row text")
                            && css.contains(".terminal-map-provider-row text")
                            && css.contains(".terminal-what-now-row text")
                            && css.contains(".terminal-reward-row text")
                            && css.contains(".terminal-recipe-row text")
                            && css.contains(".terminal-record-row text")
                            && css.contains(".terminal-row-copy"),
                    "Terminal Cyberglass CSS should directly style shared title/text rows used by overview, dossier, map, reward, recipe, and route pages");
            helper.assertTrue(css.contains("list-row") && css.contains("layout: row"),
                    "ScreenCore terminal list rows should default to horizontal row layout so chips/icons do not collapse title copy");
            helper.assertFalse(css.toLowerCase(Locale.ROOT).contains("magenta")
                            || css.toLowerCase(Locale.ROOT).contains("pink")
                            || css.toLowerCase(Locale.ROOT).contains("purple"),
                    "Survival Route CSS should not keep legacy pink or purple route-row styling");
            helper.assertTrue(css.contains(".terminal-mission-reward-panel")
                            && css.contains(".terminal-mission-reward-row"),
                    "Survival Route briefing should style reward rows as compact checklist data");
            helper.assertTrue(css.contains(".terminal-reward-summary-scroll")
                            && css.contains(".terminal-reward-content-scroll")
                            && css.contains(".terminal-reward-action-button")
                            && css.contains(".terminal-search-clear-button")
                            && css.contains(".terminal-visible-count-chip")
                            && css.contains(".terminal-dossier-state-chip"),
                    "Terminal audit CSS should size reward, recipe, and dossier controls through shared classes");
            helper.assertTrue(css.contains(".terminal-command-overview-grid")
                            && css.contains(".terminal-command-primary-grid")
                            && css.contains(".terminal-command-secondary-grid")
                            && css.contains(".terminal-route-status-panel"),
                    "Command Deck CSS should size the stretched route status overview layout through shared classes");
            helper.assertFalse(css.contains("width: 72px") || css.contains("min-width: 72px"),
                    "Survival Route action footer should not depend on fixed 72px buttons");
            String manifest = Files.readString(euiRoot.resolve("eui_manifest.json"));
            helper.assertTrue(manifest.contains("\"echothemecore:cyberglass_kit\""),
                    "Terminal EUI manifest should declare the shared ThemeCore Cyberglass kit");

            Map<String, List<String>> requiredPageClasses = new LinkedHashMap<>();
            requiredPageClasses.put("terminal_overview.eui.xml",
                    List.of("terminal-command-overview-grid", "terminal-command-left-stack",
                            "terminal-command-primary-grid", "terminal-command-secondary-grid",
                            "terminal-route-status-panel", "ROUTE STATUS", "RECENT INTEL FEED",
                            "AVAILABLE SIDE OPS", "terminal-route-status-scroll",
                            "scroll-state=\"true\"", "state-key=\"terminal.overview.routeStatus\"",
                            "terminal-route-status-content", "terminal-route-status-hazard-list",
                            "terminal-home-route-chip-row", "echoterminal:main_survival_route",
                            "Open Survival Route", "card.actionValue", "terminal.open_mission",
                            "rewardCompactLabel", "terminal-what-now-row", "terminal-action-row",
                            "terminal-command-badge-chip", "terminal-command-state-chip",
                            "<copy-block", "title=\"{card.title}\"", "subtitle=\"{card.summary}\"",
                            "title=\"{signal.title}\"", "subtitle=\"{signal.summary"));
            requiredPageClasses.put("terminal_mission_browser.eui.xml",
                    List.of("terminal-route-layout", "terminal-route-summary-row", "terminal-route-chip-row",
                            "terminal-route-legend", "terminal-phase-lane", "terminal-selected-phase-card",
                            "terminal-phase-row", "columns=\"1fr 1fr 1fr 1fr 1fr\"",
                            "stack-below=\"300\"",
                            "terminal-route-briefing-scroll",
                            "terminal-mission-detail-content", "terminal-mission-check-panel",
                            "terminal-mission-reward-panel", "rewardRows", "rewardCompactLabel",
                            "terminal-route-briefing-card", "terminal-route-sideops-card",
                            "terminal-mission-action-strip", "terminal-route-action-button",
                            "terminal-route-phase-row", "terminal-route-mission-row",
                            "terminal-route-copy-block", "terminal-route-row-copy",
                            "terminal-route-row-progress", "terminal-route-index-chip",
                            "terminal-route-state-chip", "terminal-route-detail-card",
                            "MISSION INFO", "terminal-route-info-card",
                            "briefingTitle", "briefingBody", "guidanceBody",
                            "terminal-route-detail-row", "terminal-route-item-icon",
                            "<item-icon", "requirement.iconItemId", "reward.iconItemId",
                            "Complete", "Claim", "completeActionId", "claimActionId",
                            "completeCommandDisabled", "claimCommandDisabled",
                            "primaryCommandDisabled", "disabled-reason", "terminal.perform_mission_action",
                            "terminal.activate_selected_mission", "statusCompactLabel", "primaryCommandLabel"));
            requiredPageClasses.put("terminal_mission_graph.eui.xml",
                    List.of("terminal-map-layout", "terminal-map-provider-row",
                            "terminal-provider-count-chip", "terminal-row-copy",
                            "<copy-block", "title=\"{provider.title}\"", "subtitle=\"{provider.statusLine}\"",
                            "terminal.missionGraph.selectedProvider.summary"));
            requiredPageClasses.put("terminal_recipe_index.eui.xml",
                    List.of("terminal-category-list", "terminal-recipe-layout", "terminal-recipe-list",
                            "terminal-search-clear-button", "terminal-visible-count-chip",
                            "terminal-recipe-category-chip", "terminal-recipe-category-copy",
                            "category.compactTitle", "terminal-row-copy", "Provider Diagnostics"));
            requiredPageClasses.put("terminal_reward_inbox.eui.xml",
                    List.of("terminal-reward-layout", "terminal-reward-summary-scroll",
                            "terminal-reward-content-scroll", "terminal-reward-action-button",
                            "terminal-reward-badge-chip", "terminal-reward-state-chip",
                            "terminal-reward-count-chip", "disabled-reason"));
            requiredPageClasses.put("terminal_addons.eui.xml",
                    List.of("terminal-addon-layout", "terminal-module-icon-chip", "terminal-module-state-chip"));
            requiredPageClasses.put("terminal_archives.eui.xml",
                    List.of("terminal-archive-layout", "terminal-archive-state-chip", "terminal-row-copy"));
            requiredPageClasses.put("terminal_data_core.eui.xml",
                    List.of("terminal-system-overview-grid", "terminal-system-detail-grid",
                            "terminal-system-badge-chip", "terminal-system-state-chip",
                            "terminal-system-warning-chip", "terminal-row-copy"));
            requiredPageClasses.put("terminal_fallback.eui.xml",
                    List.of("terminal-dossier-layout", "terminal-dossier-action-row",
                            "terminal-dossier-state-chip", "terminal-dossier-badge-chip",
                            "Module Diagnostics", "<copy-block", "title=\"{action.title}\"",
                            "subtitle=\"{action.summary}\""));
            requiredPageClasses.put("terminal_scriptcore_browser.eui.xml",
                    List.of("scriptcore-state-chip", "scriptcore.execute", "scriptcore.preview",
                            "disabled-reason"));
            Path pageRoot = euiRoot.resolve("pages");
            try (var pages = Files.list(pageRoot)) {
                for (Path pagePath : pages.filter(path -> path.getFileName().toString().endsWith(".eui.xml")).toList()) {
                    String page = Files.readString(pagePath);
                    helper.assertTrue(page.contains("styles=\"echothemecore:cyberglass_kit,"),
                            pagePath.getFileName() + " should include ThemeCore kit before Terminal-specific styles");
                    if (page.contains("echoterminal:terminal_shell")) {
                        helper.assertTrue(page.contains("fit-mode=\"canvas\"")
                                        && page.contains("design-width=\"1280\"")
                                        && page.contains("design-height=\"720\""),
                                pagePath.getFileName()
                                        + " should declare the 1280x720 ScreenCore canvas fit contract");
                        helper.assertFalse(page.contains("design-width=\"854\"") || page.contains("design-height=\"480\""),
                                pagePath.getFileName() + " should not rely on the old generic 854x480 fit canvas");
                    }
                    String pageWithoutCanvasContract = page
                            .replace("design-width=\"1280\"", "")
                            .replace("design-height=\"720\"", "");
                    helper.assertFalse(pageWithoutCanvasContract.contains("width=\"")
                                    || pageWithoutCanvasContract.contains("height=\"")
                                    || page.contains("min-width=\"") || page.contains("min-height=\""),
                            pagePath.getFileName()
                                    + " should keep fixed sizing in shared CSS classes instead of inline EUI attributes");
                }
            }
            try (var components = Files.list(euiRoot.resolve("components"))) {
                for (Path componentPath : components.filter(path -> path.getFileName().toString().endsWith(".eui.xml"))
                        .toList()) {
                    String component = Files.readString(componentPath);
                    helper.assertFalse(component.contains("width=\"") || component.contains("height=\"")
                                    || component.contains("min-width=\"") || component.contains("min-height=\""),
                            componentPath.getFileName()
                                    + " should keep fixed sizing in shared CSS classes instead of inline EUI attributes");
                }
            }
            for (Map.Entry<String, List<String>> entry : requiredPageClasses.entrySet()) {
                String page = Files.readString(pageRoot.resolve(entry.getKey()));
                for (String className : entry.getValue()) {
                    helper.assertTrue(page.contains(className),
                            entry.getKey() + " should use fixed ScreenCore layout class " + className);
                }
                if ("terminal_overview.eui.xml".equals(entry.getKey())) {
                    helper.assertFalse(page.contains("SYSTEM HEALTH")
                                    || page.contains("ECHO MODULES")
                                    || page.contains("terminal-system-health-panel")
                                    || page.contains("terminal-module-panel"),
                            "Command Deck overview should keep the freed right column dedicated to Route Status");
                    helper.assertFalse(page.contains("<scroll class=\"terminal-hazard-list\""),
                            "Route Status should use one parent scroll owner instead of a nested hazard warning scroller");
                    int routeScroll = page.indexOf("<scroll class=\"terminal-route-status-scroll\"");
                    int routeScrollEnd = routeScroll < 0 ? -1 : page.indexOf("</scroll>", routeScroll);
                    int hazardMapButton = page.indexOf("terminal.open_hazard_map");
                    helper.assertTrue(routeScroll >= 0 && routeScrollEnd > routeScroll,
                            "Route Status should expose a bounded scroll body");
                    helper.assertTrue(hazardMapButton > routeScrollEnd,
                            "Route Status hazard map button should stay outside the scroll body");
                    int nestedScroll = routeScroll < 0 ? -1 : page.indexOf("<scroll ", routeScroll + 1);
                    helper.assertTrue(nestedScroll < 0 || nestedScroll > routeScrollEnd,
                            "Route Status scroll body should not contain another scroll owner");
                }
                if ("terminal_mission_browser.eui.xml".equals(entry.getKey())) {
                    helper.assertFalse(page.contains("terminal-side-scroll"),
                            "Survival Route briefing should use one primary detail scroller instead of nested side scrolls");
                    int briefingScroll = page.indexOf("<scroll class=\"terminal-route-panel terminal-route-briefing-panel terminal-route-briefing-scroll\"");
                    int briefingScrollEnd = briefingScroll < 0 ? -1 : page.indexOf("</scroll>", briefingScroll);
                    int detailContent = page.indexOf("<column class=\"terminal-mission-detail-content\"");
                    int actionStrip = page.indexOf("<grid class=\"terminal-mission-action-strip\"");
                    int requirements = page.indexOf("title=\"REQUIREMENTS\"");
                    int rewards = page.indexOf("title=\"REWARDS\"");
                    int sideOps = page.indexOf("title=\"SIDE OPERATIONS\"");
                    int briefingScrollCount = page.split("terminal-route-briefing-scroll", -1).length - 1;
                    helper.assertTrue(briefingScrollCount == 1,
                            "Survival Route should declare exactly one right-column briefing scroll owner");
                    helper.assertTrue(briefingScroll >= 0 && briefingScrollEnd > briefingScroll,
                            "Survival Route right panel should expose one bounded full-column briefing scroll");
                    helper.assertTrue(detailContent > briefingScroll && detailContent < briefingScrollEnd,
                            "Survival Route requirements and rewards should be regular briefing-scroll content");
                    helper.assertTrue(requirements > detailContent && requirements < briefingScrollEnd
                                    && rewards > detailContent && rewards < briefingScrollEnd,
                            "Survival Route requirements and rewards should live inside the full-column briefing scroll");
                    helper.assertTrue(actionStrip > rewards && actionStrip < briefingScrollEnd
                                    && sideOps > actionStrip && sideOps < briefingScrollEnd,
                            "Survival Route actions and side operations should remain reachable inside the same briefing scroll");
                    helper.assertFalse(page.contains("terminal-mission-detail-scroll"),
                            "Survival Route requirements/rewards should not create a nested detail scroll");
                    helper.assertFalse(page.contains("terminal-route-sideops-scroll")
                                    || page.contains("state-key=\"terminal.missions.side_ops\""),
                            "Survival Route side operations should not create a second right-panel scroll owner");
                    int routeActionButtonCount = page.split("class=\"terminal-route-action-button", -1).length - 1;
                    helper.assertTrue(routeActionButtonCount == 5,
                            "Survival Route action footer should expose Track, Complete, Claim, Next/Unlock, and Map");
                    helper.assertTrue(page.contains("<grid class=\"terminal-mission-action-strip\" columns=\"1fr 1fr 1fr 1fr 1fr\" gap=\"5\" stack-below=\"300\">"),
                            "Survival Route action footer should be a five-column ScreenCore grid with stack-below");
                    int trackAction = page.indexOf("terminal.track_mission", actionStrip);
                    int completeAction = page.indexOf(">Complete</button>", actionStrip);
                    int claimAction = page.indexOf(">Claim</button>", actionStrip);
                    int primaryAction = page.indexOf("terminal.activate_selected_mission", actionStrip);
                    int mapAction = page.indexOf(">Map</button>", actionStrip);
                    helper.assertTrue(trackAction > actionStrip
                                    && completeAction > trackAction
                                    && claimAction > completeAction
                                    && primaryAction > claimAction
                                    && mapAction > primaryAction,
                            "Survival Route actions should stay ordered Track, Complete, Claim, primary command, Map");
                    helper.assertFalse(page.contains("Claim Rewards</button>"),
                            "Survival Route tight action strip should use the compact Claim label");
                    helper.assertFalse(page.contains("primaryActionDisabled"),
                            "Survival Route primary command should not render as a disabled No command button");
                    helper.assertFalse(page.contains("width=\"68px\"") || page.contains("width=\"82px\""),
                            "Survival Route action strip should not use per-button inline width tuning");
                    helper.assertFalse(page.contains("Open Map"),
                            "Survival Route tight action strip should use the compact Map label");
                    helper.assertFalse(page.contains("<status-chip class=\"terminal-route-state-chip\" status=\"{reward.status}\" value=\"{reward.stateLabel}\"/>"),
                            "Survival Route reward rows should render the reward item icon as the lead visual");
                }
                if ("terminal_reward_inbox.eui.xml".equals(entry.getKey())) {
                    helper.assertFalse(page.contains("terminal-side-scroll"),
                            "Reward Inbox should use readable reward summary/content scrollers instead of the tiny side-scroll class");
                }
                if ("terminal_recipe_index.eui.xml".equals(entry.getKey())) {
                    helper.assertFalse(page.contains("Legacy Recipe Controls")
                                    || page.contains("terminal.open_legacy_renderer"),
                            "Recipe Index should keep legacy renderer fallback out of the normal ScreenCore workflow");
                }
                if ("terminal_fallback.eui.xml".equals(entry.getKey())) {
                    helper.assertFalse(page.contains("Classic Renderer")
                                    || page.contains("terminal.open_legacy_renderer"),
                            "Module dossier should route users to provider diagnostics instead of a legacy renderer escape hatch");
                }
            }
        } catch (IOException exception) {
            helper.assertTrue(false, "Failed to inspect ScreenCore terminal EUI polish resources: "
                    + exception.getMessage());
        }

        EchoConfigRegistry.withClearedForTests(() -> {
            AtomicInteger serverCount = new AtomicInteger(2);
            AtomicInteger clientCount = new AtomicInteger(3);
            EchoConfigRegistry.register(EchoConfigProvider.of(EchoTerminal.MODID, () -> new EchoConfigModule(
                    EchoTerminal.MODID,
                    "ECHO Terminal",
                    List.of(
                            new EchoConfigCategory("server", "Server", List.of(
                                    EchoConfigEntry.intEntry("server_count", "Server Count", "",
                                            EchoConfigSide.COMMON, 2, 0, 10, serverCount::get, serverCount::set,
                                            null, false, true, false))),
                            new EchoConfigCategory("client", "Client", List.of(
                                    EchoConfigEntry.intEntry("client_count", "Client Count", "",
                                            EchoConfigSide.CLIENT, 3, 0, 10, clientCount::get, clientCount::set,
                                            null, true, false, false)))))));
            TerminalConfigClientState.apply(new TerminalConfigSyncPacket(
                    EchoConfigRegistry.snapshots(EchoConfigSide.COMMON), "Layout snapshot ready."));
        helper.assertTrue(BuiltinTerminalTabs.addonConfigSideTitlesForTests(EchoTerminal.MODID)
                            .equals(List.of("Server/Common", "Client Local")),
                    "Addon config polish should keep server/common before client-local sections");
        });
        TerminalConfigClientState.apply(null);
        TerminalClientOptions.VisualLevel previousVisualLevel = TerminalClientOptions.visualLevel;
        boolean previousReducedMotion = TerminalClientOptions.reducedMotion;
        try {
            helper.assertTrue(TerminalRenderCoreClientIntegration.screenProfileForTests()
                            .equals(id("screen/terminal_hud")),
                    "RenderCore terminal screen compat should keep the terminal HUD profile id");
            TerminalClientOptions.visualLevel = TerminalClientOptions.VisualLevel.MINIMAL;
            TerminalClientOptions.reducedMotion = false;
            helper.assertFalse(TerminalRenderCoreClientIntegration.shouldRenderScreenAccentForTests(),
                    "Minimal terminal visuals should skip the RenderCore screen accent");
            TerminalClientOptions.visualLevel = TerminalClientOptions.VisualLevel.BALANCED;
            helper.assertTrue(TerminalRenderCoreClientIntegration.shouldRenderScreenAccentForTests(),
                    "Balanced terminal visuals should allow the subtle RenderCore screen accent");
            TerminalThemeContext fullVisuals = new TerminalThemeContext(id("visual_test"), "chapters",
                    "echoindustrialnexus", "Industrial Nexus", "echoindustrialnexus", 0, true, false);
            TerminalThemeContext minimalVisuals = new TerminalThemeContext(id("visual_test"), "chapters",
                    "echoindustrialnexus", "Industrial Nexus", "echoindustrialnexus", 0, false, false);
            TerminalTheme terminalTheme = TerminalThemeRegistry.byId(BuiltinTerminalThemes.ECHO_CONSOLE);
            TerminalRenderContext fullContext = new TerminalRenderContext(null, null, 800, 600, 0, 0, 400, 300,
                    0, null, null, terminalTheme, fullVisuals);
            TerminalRenderContext minimalContext = new TerminalRenderContext(null, null, 800, 600, 0, 0, 400, 300,
                    0, null, null, terminalTheme, minimalVisuals);
            helper.assertTrue(TerminalUi.chapterPanel(fullContext) != null,
                    "Visual terminal contexts should resolve chapter panel texture assets");
            helper.assertTrue(TerminalUi.chapterPanel(minimalContext) == null,
                    "Minimal terminal visual contexts should skip image-backed panel texture assets");
            RenderCoreScreenFrameOptions normalFrame =
                    TerminalRenderCoreClientIntegration.screenFrameOptionsForTests(false);
            helper.assertTrue(normalFrame.style() == RenderCoreScreenChromeStyle.TERMINAL
                            && !normalFrame.drawScanlines()
                            && normalFrame.chromaticEdge()
                            && !normalFrame.glassGlints(),
                    "Balanced terminal RenderCore chrome should use the terminal cyberglass preset with clean glass and no scanlines");
            RenderCoreScreenFrameOptions reducedFrame =
                    TerminalRenderCoreClientIntegration.screenFrameOptionsForTests(true);
            helper.assertTrue(reducedFrame.style() == RenderCoreScreenChromeStyle.TERMINAL
                            && !reducedFrame.drawScanlines()
                            && !reducedFrame.edgeGlow()
                            && !reducedFrame.glassGlints()
                            && !reducedFrame.chromaticEdge(),
                    "Reduced-motion terminal RenderCore chrome should disable animated glass accents");
        } finally {
            TerminalClientOptions.visualLevel = previousVisualLevel;
            TerminalClientOptions.reducedMotion = previousReducedMotion;
        }
        helper.succeed();
    }

    private static void terminalResourceNameContracts(GameTestHelper helper) {
        Path root = Path.of("addons", "echoterminal", "src", "main", "resources",
                "assets", EchoTerminal.MODID, "textures", "gui");
        if (Files.isDirectory(root)) {
            try (var paths = Files.walk(root)) {
                List<String> unsafe = paths
                        .filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith(".png"))
                        .map(root::relativize)
                        .map(Path::toString)
                        .map(path -> path.replace('\\', '/'))
                        .filter(path -> !path.matches("[a-z0-9_./-]+"))
                        .toList();
                helper.assertTrue(unsafe.isEmpty(),
                        "Terminal GUI texture names must be lowercase identifier-safe: " + unsafe);
            } catch (IOException exception) {
                helper.assertTrue(false, "Terminal resource name scan failed: " + exception.getMessage());
            }
        }
        helper.succeed();
    }

    private static void terminalCommandDeckPriority(GameTestHelper helper) {
        helper.assertTrue(BuiltinTerminalTabs.commandDeckPriorityTabForTests(
                        true, true, 5, true, true, true, 2).equals(id("vitals")),
                "Command Deck priority should surface critical vitals before rewards or blockers");
        helper.assertTrue(BuiltinTerminalTabs.commandDeckPriorityTabForTests(true, 5, true, 2).equals(id("reward_inbox")),
                "Command Deck priority should open rewards before blockers or routes");
        helper.assertTrue(BuiltinTerminalTabs.commandDeckPriorityTabForTests(false, 5, true, 2).equals(id("reward_inbox")),
                "Command Deck priority should open reward inbox before routes");
        helper.assertTrue(BuiltinTerminalTabs.commandDeckPriorityTabForTests(true, 0, true, 2).equals(id("route_records")),
                "Command Deck priority should continue routes before inline blocker summaries");
        helper.assertTrue(BuiltinTerminalTabs.commandDeckPriorityTabForTests(true, 0, false, 2)
                        .equals(MainSurvivalQuestProvider.TAB_ID),
                "Command Deck priority should keep blockers inline and fall back to Survival Route");
        helper.assertTrue(BuiltinTerminalTabs.commandDeckPriorityTabForTests(
                        false, true, 0, true, false, true, 2).equals(MainSurvivalQuestProvider.TAB_ID),
                "Command Deck priority should send active survival objectives to the Survival Route before blockers");
        helper.assertTrue(BuiltinTerminalTabs.commandDeckPriorityTabForTests(false, 0, true, 2).equals(id("route_records")),
                "Command Deck priority should continue incomplete routes before fallback guidance");
        helper.assertTrue(BuiltinTerminalTabs.commandDeckPriorityTabForTests(false, 0, false, 2)
                        .equals(MainSurvivalQuestProvider.TAB_ID),
                "Command Deck priority should fall back to Survival Route before addon chapter review");
        helper.assertTrue(BuiltinTerminalTabs.commandDeckPriorityTabForTests(false, 0, false, 0)
                        .equals(MainSurvivalQuestProvider.TAB_ID),
                "Command Deck priority should still prefer Survival Route when no addons are linked");
        helper.assertTrue(BuiltinTerminalTabs.commandDeckPriorityTabForTests(false, 0, false, false, 2).equals(id("addons")),
                "Command Deck priority should open chapter review when Survival Route is unavailable");
        helper.assertTrue(BuiltinTerminalTabs.commandDeckRewardActionForTests().equals(id("claim_rewards")),
                "Command Deck reward shortcut should keep using the shared reward claim action");
        helper.succeed();
    }

    private static void terminalMissionActionRouting(GameTestHelper helper) {
        TerminalActionRegistry.withClearedForTests(() -> TerminalMissionRegistry.withClearedForTests(() -> {
            AtomicBoolean handled = new AtomicBoolean(false);
            Identifier chapter = id("test_chapter");
            Identifier mission = id("test_mission");
            Identifier tab = id("mission_tab");
            TerminalMissionRegistry.register(new DummyMissionProvider(chapter, 1, handled));
            TerminalMissionActions.registerForTab(tab);

            boolean routed = TerminalActionRegistry.handle(null, tab, TerminalMissionActions.MISSION_ACTION,
                    TerminalMissionActions.payload(chapter, mission, "claim_reward"));
            helper.assertTrue(routed, "Generic mission action should route through TerminalActionRegistry");
            helper.assertTrue(handled.get(), "Mission provider should receive generic mission action payload");
            BuiltinTerminalCommonIntegration.registerActionsForTests();
            helper.assertTrue(TerminalActionRegistry.handle(null,
                            BuiltinTerminalCommonIntegration.REWARD_INBOX,
                            BuiltinTerminalCommonIntegration.CLAIM_REWARDS,
                            ""),
                    "Built-in reward inbox action should be registered from common setup");
        }));
        helper.succeed();
    }

    private static void terminalLoreTaxonomy(GameTestHelper helper) {
        TerminalTabChrome command = TerminalTabChrome.fromDescriptor(
                new TerminalTabDescriptor(id("overview"), "OVERVIEW", 0, 0xFF66D9FF));
        helper.assertTrue("Command Deck".equals(command.shortTitle()),
                "Overview descriptor should render as Command Deck");
        helper.assertTrue(TerminalTabChrome.GROUP_PROTOCOL.equals(command.group()),
                "Command Deck should live in PROTOCOL");

        TerminalTabChrome roadmap = TerminalTabChrome.fromDescriptor(
                new TerminalTabDescriptor(id("missions"), "MISSIONS", 100, 0xFF66D9FF));
        helper.assertTrue("Protocol Roadmap".equals(roadmap.shortTitle()),
                "Missions descriptor should render as Protocol Roadmap");
        helper.assertTrue(TerminalTabChrome.GROUP_PROTOCOL.equals(roadmap.group()),
                "Protocol Roadmap should live in PROTOCOL");

        TerminalTabChrome nexus = TerminalTabChrome.fromDescriptor(
                new TerminalTabDescriptor(id("nexus"), "NEXUS", 220, 0xFFC77DFF));
        helper.assertTrue("Nexus Core".equals(nexus.shortTitle()),
                "Nexus descriptor should render as Nexus Core");
        helper.assertTrue(TerminalTabChrome.GROUP_NEXUS.equals(nexus.group()),
                "Nexus Core should live in NEXUS");

        TerminalTabChrome orbital = TerminalTabChrome.fromDescriptor(
                new TerminalTabDescriptor(id("orbital"), "ORBITAL", 300, 0xFF66D9FF));
        helper.assertTrue("Orbital Command".equals(orbital.shortTitle()),
                "Orbital descriptor should render as Orbital Command");
        helper.assertTrue(TerminalTabChrome.GROUP_ORBITAL.equals(orbital.group()),
                "Owned orbital tabs should live in ORBITAL");
        helper.assertTrue(TerminalIcon.fromGroup(TerminalTabChrome.GROUP_ORBITAL) == TerminalIcon.ORBITAL,
                "ORBITAL group should use the orbital icon");
        helper.assertTrue(TerminalIcon.fromTitle("ECHO-0 Records") == TerminalIcon.ORBITAL,
                "ECHO-0 records should use the orbital icon");
        helper.succeed();
    }

    private static void terminalEmptyProviderContracts(GameTestHelper helper) {
        TerminalMissionRegistry.withClearedForTests(() -> {
            TerminalMissionRegistry.register(new EmptyMissionProvider(id("empty_chapter"), 1));
            helper.assertTrue(TerminalMissionRegistry.providers().size() == 1,
                    "Terminal mission registry should keep empty providers registered");
            helper.assertTrue(TerminalMissionRegistry.providers().get(0).missions(null).isEmpty(),
                    "Empty mission providers should be valid for standalone installs");
            TerminalMissionSnapshot snapshot = TerminalMissionRegistry.providers().get(0)
                    .snapshot(null, id("missing_mission"));
            helper.assertTrue(snapshot.status() == TerminalMissionStatus.LOCKED,
                    "Empty provider snapshots should return stable locked state");
        });
        helper.succeed();
    }

    private static void terminalMainSurvivalRoute(GameTestHelper helper) {
        TerminalMissionRegistry.withClearedForTests(() -> {
            MainSurvivalQuestProvider.INSTANCE.clearCacheForTests();
            TerminalMissionRegistry.register(MainSurvivalQuestProvider.INSTANCE);
            Identifier reclaimPowerId = Identifier.fromNamespaceAndPath("echoindustrialnexus", "mission/reclaim_power");
            Identifier lockedFutureId = Identifier.fromNamespaceAndPath("echoindustrialnexus", "mission/locked_future");
            Identifier anchoredSideId = Identifier.fromNamespaceAndPath("echoindustrialnexus", "mission/power_logs");
            Identifier activeSideId = Identifier.fromNamespaceAndPath("echoindustrialnexus", "mission/local_grid_trace");
            Identifier lockedSideId = Identifier.fromNamespaceAndPath("echoindustrialnexus", "mission/warden_dossier");
            Identifier archivedSideId = Identifier.fromNamespaceAndPath("echoindustrialnexus", "mission/crash_perimeter");
            Identifier gatedSideId = Identifier.fromNamespaceAndPath("echoindustrialnexus", "mission/future_grid_signal");
            Identifier nestedSideId = Identifier.fromNamespaceAndPath("echoindustrialnexus", "mission/nested_future_signal");
            Identifier unanchoredSideId = Identifier.fromNamespaceAndPath("echoindustrialnexus", "mission/support_checklist");
            Identifier ashfallOutpostId = Identifier.fromNamespaceAndPath("echoashfallprotocol", "secure_crash_outpost");
            Identifier ashfallCleanWaterId = Identifier.fromNamespaceAndPath("echoashfallprotocol", "confirm_clean_water");
            Identifier ashfallScannerId = Identifier.fromNamespaceAndPath("echoashfallprotocol", "craft_portable_scanner");
            Identifier ashfallCrashSignalId = Identifier.fromNamespaceAndPath("echoashfallprotocol", "crash_blackbox_signal");
            Identifier ashfallSurfaceReportId = Identifier.fromNamespaceAndPath("echoashfallprotocol", "wasteland_surface_report");
            Identifier ashfallPoiSideId = Identifier.fromNamespaceAndPath("echoashfallprotocol", "first_ruin_signature");
            Identifier agricultureRouteId = Identifier.fromNamespaceAndPath("echoagriculturereclamation", "mission/recover_seed");
            Identifier hiddenRouteId = Identifier.fromNamespaceAndPath("echoagriculturereclamation", "mission/internal_hidden");
            TerminalMissionRegistry.register(new ConfigurableMissionProvider(
                    VanillaJourneyProvider.CHAPTER_ID,
                    "Baseline",
                    1,
                    List.of(new ConfiguredMission(
                            Identifier.withDefaultNamespace("story/mine_stone"),
                            "Stone Age",
                            "Story",
                            "Story",
                            "Task",
                            TerminalMissionRole.MAIN,
                            TerminalMissionStatus.CLAIMABLE,
                            List.of(TerminalMissionAction.enabled("claim_reward", "CLAIM CACHE"))))));
            TerminalMissionRegistry.register(new ConfigurableMissionProvider(
                    Identifier.fromNamespaceAndPath("echoindustrialnexus", "industrial_nexus"),
                    "Industrial Nexus",
                    2,
                    List.of(new ConfiguredMission(
                            reclaimPowerId,
                            "Reclaim Power",
                            "Stage 1",
                            "Factory",
                            "Production",
                            TerminalMissionRole.MAIN,
                            TerminalMissionStatus.UNLOCKED,
                            List.of(TerminalMissionAction.enabled("scan_factory", "SCAN FACTORY"))),
                            new ConfiguredMission(
                                    lockedFutureId,
                                    "Locked Future",
                                    "Stage 2",
                                    "Factory",
                                    "Production",
                                    TerminalMissionRole.MAIN,
                                    TerminalMissionStatus.LOCKED,
                                    List.of(TerminalMissionAction.disabled("scan_factory", "SCAN FACTORY",
                                            "Factory uplink offline."))),
                            new ConfiguredMission(
                                    anchoredSideId,
                                    "Power Logs",
                                    "Stage 1",
                                    "Intel",
                                    "Optional",
                                    TerminalMissionRole.OPTIONAL,
                                    TerminalMissionStatus.CLAIMABLE,
                                    List.of(TerminalMissionAction.enabled("archive_intel", "ARCHIVE INTEL")),
                                    Optional.of(reclaimPowerId),
                                    List.of(TerminalMissionIntelUnlock.archive(id("archive/power_logs"),
                                            "Power Logs", "Optional power-grid archive record."),
                                            TerminalMissionIntelUnlock.archive(id("archive/power_orders"),
                                                    "Power Orders", "Secondary power-grid archive record."))),
                            new ConfiguredMission(
                                    activeSideId,
                                    "Local Grid Trace",
                                    "Stage 1",
                                    "Route",
                                    "Optional",
                                    TerminalMissionRole.OPTIONAL,
                                    TerminalMissionStatus.UNLOCKED,
                                    List.of(TerminalMissionAction.enabled("track_signal", "TRACK SIGNAL")),
                                    Optional.of(reclaimPowerId),
                                    List.of(TerminalMissionIntelUnlock.route(id("route/local_grid"),
                                            "Local Grid", "Nearby power-grid signal route."))),
                            new ConfiguredMission(
                                    lockedSideId,
                                    "Warden Dossier",
                                    "Stage 1",
                                    "Faction",
                                    "Optional",
                                    TerminalMissionRole.OPTIONAL,
                                    TerminalMissionStatus.LOCKED,
                                    List.of(TerminalMissionAction.disabled("decode_dossier", "DECODE",
                                            "Faction key missing.")),
                                    Optional.of(reclaimPowerId),
                                    List.of(TerminalMissionIntelUnlock.faction(id("faction/warden_presence"),
                                            "Warden Presence", "Faction hint only."))),
                            new ConfiguredMission(
                                    archivedSideId,
                                    "Crash Perimeter",
                                    "Stage 1",
                                    "POI",
                                    "Optional",
                                    TerminalMissionRole.OPTIONAL,
                                    TerminalMissionStatus.COMPLETED,
                                    List.of(),
                                    Optional.of(reclaimPowerId),
                                    List.of(TerminalMissionIntelUnlock.poi(id("poi/crash_perimeter"),
                                            "Crash Perimeter", "Nearby crash POI."))),
                            new ConfiguredMission(
                                    gatedSideId,
                                    "Future Grid Signal",
                                    "Stage 1",
                                    "Route",
                                    "Optional",
                                    TerminalMissionRole.OPTIONAL,
                                    TerminalMissionStatus.UNLOCKED,
                                    List.of(TerminalMissionAction.enabled("track_future", "TRACK")),
                                    Optional.of(reclaimPowerId),
                                    List.of(lockedFutureId),
                                    List.of(TerminalMissionIntelUnlock.route(id("route/future_grid"),
                                            "Future Grid", "Route signal behind a future gate."))),
                            new ConfiguredMission(
                                    nestedSideId,
                                    "Nested Future Signal",
                                    "Stage 1",
                                    "Route",
                                    "Optional",
                                    TerminalMissionRole.OPTIONAL,
                                    TerminalMissionStatus.UNLOCKED,
                                    List.of(TerminalMissionAction.enabled("track_nested", "TRACK")),
                                    Optional.of(anchoredSideId),
                                    List.of(TerminalMissionIntelUnlock.route(id("route/nested_grid"),
                                            "Nested Grid", "Side card chained to another side card."))),
                            new ConfiguredMission(
                                    unanchoredSideId,
                                    "Support Checklist",
                                    "Stage 1",
                                    "Support",
                                    "Optional",
                                    TerminalMissionRole.OPTIONAL,
                                    TerminalMissionStatus.UNLOCKED,
                                    List.of()))));
            TerminalMissionRegistry.register(new ConfigurableMissionProvider(
                    Identifier.fromNamespaceAndPath("echoashfallprotocol", "ashfall_side_fixture"),
                    "Ashfall Protocol",
                    3,
                    List.of(new ConfiguredMission(
                            ashfallOutpostId,
                            "Anchor Pod Outpost",
                            "Podfall",
                            "Route",
                            "Main",
                            TerminalMissionRole.MAIN,
                            TerminalMissionStatus.UNLOCKED,
                            List.of()),
                            new ConfiguredMission(
                                    ashfallCleanWaterId,
                                    "Confirm Clean Water",
                                    "Podfall",
                                    "Route",
                                    "Main",
                                    TerminalMissionRole.MAIN,
                                    TerminalMissionStatus.LOCKED,
                                    List.of()),
                            new ConfiguredMission(
                                    ashfallScannerId,
                                    "Craft Portable Scanner",
                                    "Recon",
                                    "Route",
                                    "Main",
                                    TerminalMissionRole.MAIN,
                                    TerminalMissionStatus.LOCKED,
                                    List.of()),
                            new ConfiguredMission(
                                    ashfallCrashSignalId,
                                    "Crash Blackbox Signal",
                                    "Perimeter Signals",
                                    "Intel",
                                    "Optional",
                                    TerminalMissionRole.OPTIONAL,
                                    TerminalMissionStatus.UNLOCKED,
                                    List.of(),
                                    Optional.of(ashfallOutpostId),
                                    List.of(TerminalMissionIntelUnlock.archive(id("ashfall_progression_manual"),
                                            "Protocol Roadmap Rules", "Outpost context."))),
                            new ConfiguredMission(
                                    ashfallSurfaceReportId,
                                    "Wasteland Surface Report",
                                    "Perimeter Signals",
                                    "Intel",
                                    "Optional",
                                    TerminalMissionRole.OPTIONAL,
                                    TerminalMissionStatus.UNLOCKED,
                                    List.of(),
                                    Optional.of(ashfallOutpostId),
                                    List.of(TerminalMissionIntelUnlock.discovery(id("biome/the_wasteland"),
                                            "Wasteland Biome", "Surface context."))),
                            new ConfiguredMission(
                                    ashfallPoiSideId,
                                    "First Ruin Signature",
                                    "Route Records",
                                    "Intel",
                                    "Optional",
                                    TerminalMissionRole.OPTIONAL,
                                    TerminalMissionStatus.UNLOCKED,
                                    List.of(),
                                    Optional.of(ashfallScannerId),
                                    List.of(ashfallScannerId),
                                    List.of(TerminalMissionIntelUnlock.poi(id("poi/survivor_camp"),
                                            "Survivor Camp", "Recon context."))))));
            TerminalMissionRegistry.register(new ConfigurableMissionProvider(
                    id("reference_chapter"),
                    "Reference Chapter",
                    3,
                    List.of(new ConfiguredMission(
                            id("field_reference"),
                            "Field Reference",
                            "Reference",
                            "Reference",
                            "View",
                            TerminalMissionRole.REFERENCE,
                            TerminalMissionStatus.VIEW_ONLY,
                            List.of()))));
            TerminalMissionRegistry.register(new PlacedMissionProvider(
                    Identifier.fromNamespaceAndPath("echoagriculturereclamation", "field_reclamation"),
                    "FIELD > Reclamation",
                    4,
                    List.of(new ConfiguredMission(
                            agricultureRouteId,
                            "Recover Seed",
                            "Unsorted Local Phase",
                            "Field",
                            "Seed",
                            TerminalMissionRole.MAIN,
                            TerminalMissionStatus.UNLOCKED,
                            List.of(TerminalMissionAction.enabled("field_report", "FIELD REPORT"))),
                            new ConfiguredMission(
                                    hiddenRouteId,
                                    "Internal Hidden",
                                    "Unsorted Local Phase",
                                    "Field",
                                    "Hidden",
                                    TerminalMissionRole.MAIN,
                                    TerminalMissionStatus.UNLOCKED,
                                    List.of())),
                    Map.of(
                            agricultureRouteId, TerminalMissionRoutePlacement.optional(14, 42),
                            hiddenRouteId, TerminalMissionRoutePlacement.hidden())));
            TerminalMissionRegistry.register(new ThrowingMissionsProvider(id("throwing_missions"), 5));

            List<TerminalMissionDefinition> missions = MainSurvivalQuestProvider.INSTANCE.missions(null);
            helper.assertTrue(missions.stream()
                            .allMatch(definition -> MainSurvivalQuestProvider.CHAPTER_ID.equals(definition.chapterId())),
                    "Survival route definitions should render through the aggregate chapter");
            helper.assertFalse(missions.stream().anyMatch(definition -> "Stone Age".equals(definition.title())),
                    "Survival route should exclude vanilla Baseline records");
            helper.assertFalse(missions.stream()
                            .anyMatch(definition -> "minecraft".equals(definition.id().getNamespace())),
                    "Survival route should not contain vanilla namespaced missions");
            helper.assertTrue(missions.stream().anyMatch(definition -> "Reclaim Power".equals(definition.title())),
                    "Survival route should include installed addon main records");
            helper.assertTrue(missions.stream().anyMatch(definition -> "Machine Tools".equals(definition.phaseTitle())
                            && "Reclaim Power".equals(definition.title())),
                    "Industrial salvage records should land on the Machine Tools route stage");
            TerminalMissionDefinition agricultureRoute = missions.stream()
                    .filter(definition -> definition.id().equals(agricultureRouteId))
                    .findFirst()
                    .orElseThrow();
            helper.assertTrue(agricultureRoute.phaseOrder() == 14
                            && "Nexus Decision".equals(agricultureRoute.phaseTitle())
                            && agricultureRoute.missionOrder() == 42,
                    "Explicit route placement should override local phase/order metadata");
            TerminalMissionSnapshot agricultureSnapshot =
                    MainSurvivalQuestProvider.INSTANCE.snapshot(null, agricultureRouteId);
            helper.assertTrue(MainSurvivalQuestProvider.INSTANCE.role(null, agricultureRoute, agricultureSnapshot)
                            == TerminalMissionRole.OPTIONAL,
                    "Explicit route placement should override the source role for aggregate gating");
            TerminalMissionPresentation agriculturePresentation =
                    MainSurvivalQuestProvider.INSTANCE.presentation(null, agricultureRoute, agricultureSnapshot);
            String aggregateCopy = String.join(" ",
                    agricultureSnapshot.actionHint(),
                    agricultureSnapshot.unlockReason(),
                    agriculturePresentation.nextStep(),
                    agriculturePresentation.objectiveSummary(),
                    agriculturePresentation.routeHint(),
                    String.join(" ", agriculturePresentation.tags()));
            String noisySourceLabel = "Source:";
            String legacyCommandHint = "Command unlocks " + "after";
            helper.assertFalse(aggregateCopy.contains(noisySourceLabel) || aggregateCopy.contains(legacyCommandHint),
                    "Aggregate Survival Route player copy should hide provider/source noise");
            helper.assertFalse(missions.stream().anyMatch(definition -> definition.id().equals(hiddenRouteId)),
                    "Hidden explicit route placement should omit internal records from the aggregate Survival Route");
            helper.assertTrue(missions.stream().anyMatch(definition -> "Aftermath".equals(definition.phaseTitle())
                            && "Field Reference".equals(definition.title())),
                    "Survival route should include remaining MAIN/REFERENCE records in the final mastery stage");
            helper.assertTrue(missions.stream().allMatch(definition ->
                            definition.phaseTitle().equals(survivalRouteStageTitle(definition.phaseOrder()))),
                    "Survival route should expose named stage labels that match phase order");
            helper.assertTrue(missions.stream().allMatch(definition ->
                            definition.phaseId().equals(String.format(java.util.Locale.ROOT,
                                    "phase_%02d", definition.phaseOrder()))),
                    "Survival route should expose canonical numeric phase ids");
            long distinctMissionIds = missions.stream().map(TerminalMissionDefinition::id).distinct().count();
            helper.assertTrue(distinctMissionIds == missions.size(),
                    "Survival route should not duplicate authored records in the final mastery stage");
            TerminalMissionBrowser routeBrowser =
                    new TerminalMissionBrowser(MainSurvivalQuestProvider.INSTANCE, MainSurvivalQuestProvider.TAB_ID, true);
            TerminalRenderContext routeContext = new TerminalRenderContext(null, null,
                    800, 600, 0, 0, 640, 260, 0, null, null);
            helper.assertTrue("Industrial Nexus / Reclaim Power".equals(
                            routeBrowser.missionRowTitleForTests(routeContext, reclaimPowerId)),
                    "Aggregate route rows should show source context without mission order prefixes");
            helper.assertFalse(routeBrowser.roadmapMissionIdsForTests(routeContext).contains(anchoredSideId),
                    "Anchored optional intel should stay out of the main roadmap spine");
            helper.assertFalse(routeBrowser.roadmapMissionIdsForTests(routeContext).contains(unanchoredSideId),
                    "Unanchored optional support missions should not become main Survival Route rows");
            List<Identifier> sideCardIds = routeBrowser.sideCardMissionIdsForTests(routeContext, reclaimPowerId);
            helper.assertTrue(sideCardIds.contains(anchoredSideId),
                    "Anchored optional intel should render as a side card for its owning route mission");
            helper.assertTrue(sideCardIds.contains(activeSideId)
                            && sideCardIds.contains(lockedSideId)
                            && sideCardIds.contains(archivedSideId),
                    "Side card fixture should expose locked, active, ready, and archived states");
            helper.assertFalse(sideCardIds.contains(gatedSideId),
                    "Future side cards should stay hidden while their route prerequisite is incomplete");
            helper.assertFalse(sideCardIds.contains(nestedSideId),
                    "Side-card display should require an exact route anchor, not root-anchor grouping");
            List<Identifier> ashfallOutpostSideCardIds =
                    routeBrowser.sideCardMissionIdsForTests(routeContext, ashfallOutpostId);
            helper.assertTrue(ashfallOutpostSideCardIds.size() == 2
                            && ashfallOutpostSideCardIds.contains(ashfallCrashSignalId)
                            && ashfallOutpostSideCardIds.contains(ashfallSurfaceReportId),
                    "Active Anchor Pod Outpost should show only immediate perimeter side ops");
            helper.assertTrue(routeBrowser.enabledActionCountForTests(routeContext, ashfallCrashSignalId) == 0
                            && routeBrowser.enabledActionCountForTests(routeContext, ashfallSurfaceReportId) == 0,
                    "Early outpost side ops should be visible without exposing archive actions");
            helper.assertTrue(routeBrowser.sideCardMissionIdsForTests(routeContext, ashfallCleanWaterId).isEmpty(),
                    "Missions without eligible side ops should not show a side-op panel");
            helper.assertFalse(ashfallOutpostSideCardIds.contains(ashfallPoiSideId),
                    "Recon side ops should not leak into the early outpost panel");
            helper.assertTrue(routeBrowser.visibleMissionCountForTests(routeContext)
                            < routeBrowser.allMissionCountForTests(routeContext),
                    "Side-card missions should remain actionable without inflating the visible route count");
            helper.assertTrue(routeBrowser.selectMissionForTests(routeContext, anchoredSideId),
                    "Hidden side cards should remain selectable from the aggregate route model");
            helper.assertTrue(routeBrowser.sideCardBodySelectableForTests(routeContext, reclaimPowerId, anchoredSideId)
                            && routeBrowser.enabledActionCountForTests(routeContext, anchoredSideId) == 1,
                    "Side-card body should stay selectable when an enabled action button is present");
            helper.assertTrue("READY".equals(routeBrowser.sideCardStatusForTests(routeContext, anchoredSideId)),
                    "Claimable side cards should render READY state");
            helper.assertTrue("ACTIVE".equals(routeBrowser.sideCardStatusForTests(routeContext, activeSideId)),
                    "Unlocked side cards should render ACTIVE state");
            helper.assertTrue("LOCKED".equals(routeBrowser.sideCardStatusForTests(routeContext, lockedSideId)),
                    "Locked side cards should render LOCKED state");
            helper.assertTrue("ARCHIVED".equals(routeBrowser.sideCardStatusForTests(routeContext, archivedSideId)),
                    "Completed side cards should render ARCHIVED state");
            helper.assertTrue("Ready to archive".equals(routeBrowser.sideCardProgressForTests(routeContext, anchoredSideId))
                            && "Archived".equals(routeBrowser.sideCardProgressForTests(routeContext, archivedSideId)),
                    "Side-card progress labels should resolve stable state copy");
            helper.assertTrue(routeBrowser.sideCardsHeightForTests(routeContext, reclaimPowerId, 360) > 280,
                    "Side-card height calculation should include header, rail spacing, and all cards");
            int sideOpsMaxScroll = routeBrowser.largeDetailMaxScrollForTests(routeContext, reclaimPowerId, 520, 260);
            helper.assertTrue(sideOpsMaxScroll > 0,
                    "Survival Route detail cards with side ops should expose scrollable overflow");
            helper.assertTrue(routeBrowser.dragLargeDetailScrollbarToBottomForTests(routeContext, reclaimPowerId, 520, 260)
                            == sideOpsMaxScroll,
                    "Dragging the detail scrollbar should move side-op overflow to the bottom");
            helper.assertTrue(routeBrowser.selectMissionForTests(routeContext, ashfallCleanWaterId)
                            && routeBrowser.detailScrollForTests() == 0,
                    "Changing route selection should reset side-op detail scroll");
            helper.assertTrue(routeBrowser.intelGroupCountForTests(routeContext, reclaimPowerId,
                            TerminalMissionIntelKind.ARCHIVE) == 2,
                    "Intel panel should group archive unlock rows from side cards");
            helper.assertTrue("+1 more signals".equals(routeBrowser.intelOverflowSummaryForTests(routeContext,
                            reclaimPowerId, TerminalMissionIntelKind.ARCHIVE, 1)),
                    "Intel panel overflow helper should summarize hidden grouped rows");
            helper.assertTrue(routeBrowser.intelUnlockCountForTests(routeContext, anchoredSideId) == 2,
                    "Side cards should expose intel unlock targets for the summary panel");
            helper.assertTrue("Machine Tools".equals(routeBrowser.detailPhaseChipForTests(routeContext, reclaimPowerId)),
                    "Aggregate route detail cards should show named stage labels");
            TerminalMissionSnapshot reclaimSnapshot = MainSurvivalQuestProvider.INSTANCE.snapshot(null, reclaimPowerId);
            helper.assertTrue(reclaimSnapshot.actions().stream()
                            .anyMatch(action -> action.enabled() && "scan_factory".equals(action.id())),
                    "Survival route should preserve enabled child mission actions");
            helper.assertTrue(MainSurvivalQuestProvider.INSTANCE.handleAction(null, reclaimPowerId, "scan_factory"),
                    "Survival route should delegate child actions back to the source provider");
            TerminalMissionSnapshot lockedFutureSnapshot = MainSurvivalQuestProvider.INSTANCE.snapshot(null, lockedFutureId);
            helper.assertTrue(lockedFutureSnapshot.actions().stream().noneMatch(TerminalMissionAction::enabled),
                    "Locked Survival Route records should not expose enabled actions");
            helper.assertFalse(MainSurvivalQuestProvider.INSTANCE.handleAction(null, lockedFutureId, "scan_factory"),
                    "Survival route should not delegate disabled future actions");
            helper.assertTrue(MainSurvivalQuestProvider.INSTANCE.handleAction(null, anchoredSideId, "archive_intel"),
                    "Survival route should delegate side-card actions back to the owning provider");

            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            List<TerminalMissionDefinition> vanilla = VanillaJourneyProvider.INSTANCE.missions(player);
            for (Identifier id : List.of(
                    Identifier.withDefaultNamespace("husbandry/breed_an_animal"),
                    Identifier.withDefaultNamespace("husbandry/tame_an_animal"),
                    Identifier.withDefaultNamespace("husbandry/safely_harvest_honey"),
                    Identifier.withDefaultNamespace("husbandry/balanced_diet"),
                    Identifier.withDefaultNamespace("adventure/hero_of_the_village"),
                    Identifier.withDefaultNamespace("adventure/kill_all_mobs"),
                    Identifier.withDefaultNamespace("nether/all_potions"),
                    Identifier.withDefaultNamespace("nether/all_effects"))) {
                TerminalMissionDefinition definition = vanilla.stream()
                        .filter(candidate -> candidate.id().equals(id))
                        .findFirst()
                        .orElseThrow();
                TerminalMissionSnapshot snapshot = VanillaJourneyProvider.INSTANCE.snapshot(player, id);
                helper.assertTrue(VanillaJourneyProvider.INSTANCE.role(player, definition, snapshot)
                                == TerminalMissionRole.OPTIONAL,
                        "Wasteland-unsafe vanilla ecology and rare-effect goals should be optional");
            }
        });
        helper.succeed();
    }

    private static void terminalScreenCoreTextLayout(GameTestHelper helper) {
        if (!screenCoreLoaded()) {
            helper.succeed();
            return;
        }
        try {
            screenCoreDataProvidersClass().getMethod("register").invoke(null);
            Object overviewContext = screenCoreTerminalContext(id("overview"));
            Object routeContext = screenCoreTerminalContext(MainSurvivalQuestProvider.TAB_ID);
            List<String> overviewText = inspectScreenCoreTextNodes(id("terminal_overview"), overviewContext, 1024, 550);
            assertNonblankTextNodesHaveBounds(helper, overviewText, "overview");
            helper.assertTrue(hasDrawnText(overviewText, "Open Survival Route"),
                    "Command Deck real ScreenCore page should visibly draw row-copy text values.");
            int[][] routeViewports = {
                    {360, 240},
                    {854, 480},
                    {1280, 720}
            };
            for (int[] viewport : routeViewports) {
                String viewportLabel = "survival route " + viewport[0] + "x" + viewport[1];
                List<String> routeText = inspectScreenCoreTextNodes(
                        id("terminal_mission_browser"),
                        routeContext,
                        viewport[0],
                        viewport[1]);
                String routeTextSample = routeText.stream()
                        .filter(line -> line.contains("|drawCalled=true"))
                        .limit(12)
                        .toList()
                        .toString();
                assertNonblankTextNodesHaveBounds(helper, routeText, viewportLabel);
                helper.assertTrue(hasDrawnTextContaining(routeText, "Anchor Pod Outpost")
                                || hasDrawnTextContaining(routeText, "Podfall"),
                        "Survival Route real ScreenCore page should visibly draw phase and mission text values at "
                                + viewportLabel + ". Sample: " + routeTextSample);
                helper.assertFalse(routeText.stream().anyMatch(line -> line.contains("unbounded")
                                || line.contains("root_overflow")
                                || line.contains("row_overflow")
                                || line.contains("large_fixed_height")),
                        "Survival Route ScreenCore text inspection should not expose diagnostic evidence at "
                                + viewportLabel + ".");
            }
        } catch (ReflectiveOperationException exception) {
            helper.assertTrue(false, "Failed to inspect real ScreenCore terminal text layout: " + exception.getMessage());
        }
        helper.succeed();
    }

    private static void terminalScreenCoreClickActionDispatch(GameTestHelper helper) {
        if (!screenCoreLoaded()) {
            helper.succeed();
            return;
        }
        TerminalMissionRegistry.withClearedForTests(() -> {
            MainSurvivalQuestProvider.INSTANCE.clearCacheForTests();
            TerminalMissionRegistry.register(MainSurvivalQuestProvider.INSTANCE);
            TerminalMissionRegistry.register(new ConfigurableMissionProvider(
                    Identifier.fromNamespaceAndPath("echoashfallprotocol", "screen_click_route"),
                    "Ashfall Screen Click Route",
                    1,
                    List.of(new ConfiguredMission(
                            Identifier.fromNamespaceAndPath("echoashfallprotocol", "mission/screen_click_route_anchor"),
                            "Anchor Pod Outpost",
                            "Podfall",
                            "Route",
                            "Starter",
                            TerminalMissionRole.MAIN,
                            TerminalMissionStatus.UNLOCKED,
                            List.of()))));
            terminalScreenCoreClickActionDispatchWithProvider(helper);
        });
        helper.succeed();
    }

    private static void terminalScreenCoreClickActionDispatchWithProvider(GameTestHelper helper) {
        boolean previousDebugLogging = EchoNetCoreConfig.DEBUG_PACKET_LOGGING.get();
        boolean previousDroppedLogging = EchoNetCoreConfig.LOG_DROPPED_PACKETS.get();
        boolean previousDebugPackets = EchoNetCoreConfig.ENABLE_DEBUG_PACKETS.get();
        List<CustomPacketPayload> sentPayloads = new ArrayList<>();
        try {
            screenCoreActionsClass().getMethod("register").invoke(null);
            screenCoreDataProvidersClass().getMethod("register").invoke(null);
            screenCoreDataProvidersClass().getMethod("resetStateForTests").invoke(null);
            EchoNetDebug.clearCountersForTests();
            EchoNetCoreConfig.DEBUG_PACKET_LOGGING.set(true);
            EchoNetCoreConfig.LOG_DROPPED_PACKETS.set(true);
            EchoNetCoreConfig.ENABLE_DEBUG_PACKETS.set(true);

            Object routeContext = screenCoreTerminalContext(MainSurvivalQuestProvider.TAB_ID);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> visibleMissions = (List<Map<String, Object>>)
                    resolveScreenCoreData(routeContext, "missionBrowser.visibleMissions");
            helper.assertTrue(!visibleMissions.isEmpty(),
                    "Terminal ScreenCore route page should expose visible route missions before click dispatch proof.");
            String missionId = String.valueOf(visibleMissions.get(0).get("id"));
            helper.assertFalse(missionId.isBlank(),
                    "Terminal ScreenCore route mission rows should expose concrete mission ids.");
            helper.assertTrue(runScreenCoreAction(screenCoreActionId("SELECT_MISSION"), missionId, Map.of(), routeContext, null),
                    "Terminal ScreenCore route mission should be selectable before Track click dispatch proof.");
            helper.assertTrue(missionId.equals(resolveScreenCoreData(routeContext, "missionBrowser.selectedMissionId")),
                    "Terminal ScreenCore selected mission id should update visible route state before Track click.");
            helper.assertFalse(Boolean.TRUE.equals(resolveScreenCoreData(
                            routeContext, "missionBrowser.selectedMission.trackDisabled")),
                    "Terminal ScreenCore Track button should be enabled once a route mission is selected.");
            String trackAction = screenCoreActionId("TRACK_MISSION");
            try (EchoNetClientActions.TestActionOverrideHandle ignored =
                         EchoNetClientActions.installActionOverrideForTests(payload -> {
                             sentPayloads.add(payload);
                             return Optional.of(true);
                         })) {
                Object clickProbe = clickScreenCoreActionForTests(
                        id("terminal_mission_browser"),
                        routeContext,
                        trackAction,
                        1024,
                        550);
                String clickDiagnostics = screenCoreClickProbeDiagnostics(clickProbe);
                helper.assertTrue(screenCoreClickProbeBoolean(clickProbe, "found"),
                        "Real Terminal ScreenCore route page should expose a clickable Track mission button. "
                                + clickDiagnostics);
                helper.assertTrue(screenCoreClickProbeBoolean(clickProbe, "handled"),
                        "ScreenCore real Terminal page click should be handled by the input router. "
                                + clickDiagnostics);
                helper.assertTrue(trackAction.equals(screenCoreClickProbeString(clickProbe, "action")),
                        "Clicked ScreenCore component should resolve the Track mission action. "
                                + clickDiagnostics);
                helper.assertFalse(screenCoreClickProbeString(clickProbe, "actionValue").isBlank(),
                        "Clicked ScreenCore Track button should resolve a concrete mission id action value. "
                                + clickDiagnostics);
            }

            helper.assertTrue(!sentPayloads.isEmpty(),
                    "Clicking the authored Terminal ScreenCore Track button should send one serverbound NetCore action.");
            helper.assertTrue(sentPayloads.get(0) instanceof TerminalActionPacket,
                    "Terminal ScreenCore Track click should send a TerminalActionPacket, not a fake success result.");
            TerminalActionPacket packet = (TerminalActionPacket) sentPayloads.get(0);
            helper.assertTrue(MainSurvivalQuestProvider.TAB_ID.equals(packet.tabId()),
                    "Terminal ScreenCore Track click should dispatch through the Survival Route tab.");
            helper.assertTrue(TerminalMissionActions.TRACK_MISSION.equals(packet.actionId()),
                    "Terminal ScreenCore Track click should dispatch the real Terminal mission tracking action.");
            helper.assertTrue(packet.payload().contains("|track;" + MainSurvivalQuestProvider.TAB_ID),
                    "Terminal ScreenCore Track click should include a tracking payload that the server handler can parse.");
            helper.assertTrue(EchoNetDebug.counterSnapshot().entrySet().stream()
                            .anyMatch(entry -> TerminalActionPacket.ID.equals(entry.getKey().payloadId())
                                    && entry.getKey().direction() == EchoPacketDirection.SERVERBOUND
                                    && entry.getKey().kind() == EchoPacketKind.SERVERBOUND_ACTION
                                    && entry.getKey().accepted()
                                    && entry.getValue() >= 1L),
                    "NetCore counters should record the accepted serverbound Terminal action click.");
        } catch (ReflectiveOperationException | LinkageError exception) {
            helper.assertTrue(false, "Failed to prove Terminal ScreenCore click dispatch: "
                    + nestedFailureMessage(exception));
        } finally {
            EchoNetCoreConfig.DEBUG_PACKET_LOGGING.set(previousDebugLogging);
            EchoNetCoreConfig.LOG_DROPPED_PACKETS.set(previousDroppedLogging);
            EchoNetCoreConfig.ENABLE_DEBUG_PACKETS.set(previousDebugPackets);
            EchoNetDebug.clearCountersForTests();
            try {
                screenCoreDataProvidersClass().getMethod("resetStateForTests").invoke(null);
            } catch (ReflectiveOperationException | LinkageError ignored) {
            }
        }
    }


    private static void terminalMainSurvivalRouteGate(GameTestHelper helper) {
        TerminalMissionRegistry.withClearedForTests(() -> {
            MainSurvivalQuestProvider.INSTANCE.clearCacheForTests();
            GatedConvoyRouteProvider convoy = new GatedConvoyRouteProvider();
            TerminalMissionRegistry.register(MainSurvivalQuestProvider.INSTANCE);
            TerminalMissionRegistry.register(convoy);

            List<TerminalMissionDefinition> missions = MainSurvivalQuestProvider.INSTANCE.missions(null);
            TerminalMissionDefinition prep = missions.stream()
                    .filter(definition -> definition.id().equals(GatedConvoyRouteProvider.PREP_VEHICLE))
                    .findFirst()
                    .orElseThrow();
            helper.assertTrue(prep.phaseOrder() == 8 && "Faction/Drone".equals(prep.phaseTitle()),
                    "Convoy prep should live after route expansion and recon readiness");
            TerminalMissionSnapshot locked = MainSurvivalQuestProvider.INSTANCE.snapshot(null, prep.id());
            helper.assertTrue(locked.status() == TerminalMissionStatus.LOCKED,
                    "Build A Convoy Vehicle should stay locked before expedition readiness");
            helper.assertTrue(locked.actions().stream().noneMatch(TerminalMissionAction::enabled),
                    "Route-gated future missions should expose no enabled actions");
            helper.assertFalse(MainSurvivalQuestProvider.INSTANCE.handleAction(null, prep.id(), "scan_convoy"),
                    "Route-gated future missions should not delegate actions");

            convoy.gateStatus(TerminalMissionStatus.CLAIMED);
            TerminalMissionSnapshot ready = MainSurvivalQuestProvider.INSTANCE.snapshot(null, prep.id());
            helper.assertTrue(ready.status() == TerminalMissionStatus.UNLOCKED,
                    "Build A Convoy Vehicle should unlock after expedition readiness");
            helper.assertTrue(ready.actions().stream().anyMatch(action -> action.enabled()
                            && "scan_convoy".equals(action.id())),
                    "Unlocked gated missions should restore child actions");
            helper.assertTrue(MainSurvivalQuestProvider.INSTANCE.handleAction(null, prep.id(), "scan_convoy"),
                    "Unlocked gated missions should delegate actions");
        });
        helper.succeed();
    }

    private static void terminalMainSurvivalRouteCache(GameTestHelper helper) {
        TerminalMissionRegistry.withClearedForTests(() -> {
            MainSurvivalQuestProvider.INSTANCE.clearCacheForTests();
            CountingRouteMissionProvider provider = new CountingRouteMissionProvider(
                    Identifier.fromNamespaceAndPath("echoindustrialnexus", "industrial_nexus"),
                    "Industrial Nexus",
                    List.of(new ConfiguredMission(
                            Identifier.fromNamespaceAndPath("echoindustrialnexus", "mission/cache_probe"),
                            "Cache Probe",
                            "Stage 1",
                            "Factory",
                            "Production",
                            TerminalMissionRole.MAIN,
                            TerminalMissionStatus.UNLOCKED,
                            List.of())));
            TerminalMissionRegistry.register(MainSurvivalQuestProvider.INSTANCE);
            TerminalMissionRegistry.register(provider);

            List<TerminalMissionDefinition> missions = MainSurvivalQuestProvider.INSTANCE.missions(null);
            helper.assertTrue(missions.size() == 1, "Survival route should include the test provider mission");
            for (int i = 0; i < 5; i++) {
                TerminalMissionDefinition definition = missions.get(0);
                MainSurvivalQuestProvider.INSTANCE.snapshot(null, definition.id());
                MainSurvivalQuestProvider.INSTANCE.presentation(null, definition,
                        MainSurvivalQuestProvider.INSTANCE.snapshot(null, definition.id()));
                MainSurvivalQuestProvider.INSTANCE.role(null, definition,
                        MainSurvivalQuestProvider.INSTANCE.snapshot(null, definition.id()));
            }
            helper.assertTrue(provider.missionCalls().get() == 1,
                    "Survival route should not rebuild provider mission lists for repeated record lookups");
        });
        helper.succeed();
    }

    private static void terminalMainSurvivalRouteBounds(GameTestHelper helper) {
        TerminalMissionRegistry.withClearedForTests(() -> {
            MainSurvivalQuestProvider.INSTANCE.clearCacheForTests();
            TerminalMissionRegistry.register(MainSurvivalQuestProvider.INSTANCE);
            TerminalMissionRegistry.register(new ConfigurableMissionProvider(
                    Identifier.fromNamespaceAndPath("echoindustrialnexus", "industrial_nexus"),
                    "Industrial Nexus",
                    1,
                    generatedMissions(MainSurvivalQuestProvider.maxRouteRecordsForTests() + 25)));

            List<TerminalMissionDefinition> missions = MainSurvivalQuestProvider.INSTANCE.missions(null);
            helper.assertTrue(missions.size() == MainSurvivalQuestProvider.maxRouteRecordsForTests() + 1,
                    "Survival route should cap huge mission lists and append one overflow record");
            TerminalMissionDefinition overflow = missions.get(missions.size() - 1);
            helper.assertTrue("More Signals Available".equals(overflow.title()),
                    "Survival route overflow record should explain hidden records");
            helper.assertTrue(MainSurvivalQuestProvider.INSTANCE.snapshot(null, overflow.id()).status()
                            == TerminalMissionStatus.VIEW_ONLY,
                    "Survival route overflow record should be passive guidance");
        });
        helper.succeed();
    }

    private static void terminalRewardCache(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos terminalPos = new BlockPos(1, 1, 1);
        helper.setBlock(terminalPos, ModBlocks.ECHO_TERMINAL_BLOCK.get());
        EchoTerminalBlockEntity terminal = helper.getBlockEntity(terminalPos, EchoTerminalBlockEntity.class);
        terminal.setOwnerIfMissing(player);
        terminal.storeRewards("test_reward", List.of(new ItemStack(Items.BREAD, 3)));

        helper.assertTrue(EchoCoreServices.pendingTerminalRewardCount(player) == 0,
                "Render-facing reward lookup should not scan when no terminal is cached");
        EchoTerminalCoreServices.rememberTerminal(player, helper.absolutePos(terminalPos));
        helper.assertTrue(EchoCoreServices.pendingTerminalRewardCount(player) == 3,
                "Render-facing reward lookup should read a remembered owned terminal");
        helper.succeed();
    }

    private static void terminalRewardTransactional(GameTestHelper helper) {
        BlockPos terminalPos = new BlockPos(1, 1, 1);
        helper.setBlock(terminalPos, ModBlocks.ECHO_TERMINAL_BLOCK.get());
        EchoTerminalBlockEntity terminal = helper.getBlockEntity(terminalPos, EchoTerminalBlockEntity.class);
        helper.assertTrue(terminal.storeRewards("seed", List.of(new ItemStack(Items.BREAD, 63))),
                "Terminal should accept an initial partial stack");
        helper.assertTrue(terminal.storeRewards("fill", fullInboxStacks(26)),
                "Terminal should accept enough unique stacks to leave no empty reward slots");

        int before = terminal.getStoredRewardCount();
        helper.assertFalse(terminal.storeRewards("overflow", List.of(
                        new ItemStack(Items.BREAD, 2),
                        new ItemStack(Items.TORCH, 1))),
                "Terminal reward storage should reject mixed rewards when every stack cannot fit");
        helper.assertTrue(terminal.getStoredRewardCount() == before,
                "Rejected reward storage should not partially merge into existing inbox stacks");
        helper.succeed();
    }

    private static void terminalRewardClaimFlow(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos terminalPos = new BlockPos(1, 1, 1);
        helper.setBlock(terminalPos, ModBlocks.ECHO_TERMINAL_BLOCK.get());
        EchoTerminalBlockEntity terminal = helper.getBlockEntity(terminalPos, EchoTerminalBlockEntity.class);
        terminal.setOwnerIfMissing(player);

        helper.assertTrue(terminal.storeRewards("merge", List.of(
                        new ItemStack(Items.BREAD, 63),
                        new ItemStack(Items.BREAD, 1),
                        new ItemStack(Items.TORCH, 1))),
                "Terminal should merge partial stacks and store remaining successful rewards");
        helper.assertTrue(terminal.getStoredRewardCount() == 65,
                "Successful reward storage should preserve every inserted item");
        helper.assertTrue(terminal.claimAllRewards(player),
                "Terminal should claim stored rewards into the player inventory");
        helper.assertTrue(terminal.getStoredRewardCount() == 0,
                "Claiming rewards should clear the terminal inbox");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    private static void terminalRuntimeSpineActions(GameTestHelper helper) {
        EchoServiceRegistry.withClearedForTests(() -> TerminalActionRegistry.withClearedForTests(() -> {
            EchoRuntimeSpineBus.clearForTests();
            List<EchoRuntimeSpineEvent> events = new ArrayList<>();
            EchoRuntimeSpineBus.register(events::add);
            try {
                EchoTerminalCoreServices.register();
                BuiltinTerminalCommonIntegration.registerActionsForTests();

                ServerPlayer noTerminal = helper.makeMockServerPlayerInLevel();
                helper.assertTrue(TerminalActionRegistry.handle(noTerminal,
                                BuiltinTerminalCommonIntegration.REWARD_INBOX,
                                BuiltinTerminalCommonIntegration.CLAIM_REWARDS,
                                ""),
                        "Known Terminal reward action should be consumed by the server action registry.");
                helper.assertTrue(events.isEmpty(),
                        "Terminal reward action with no saved mutation must not publish runtime-spine success events.");

                ServerPlayer player = helper.makeMockServerPlayerInLevel();
                BlockPos terminalPos = new BlockPos(1, 1, 1);
                helper.setBlock(terminalPos, ModBlocks.ECHO_TERMINAL_BLOCK.get());
                EchoTerminalBlockEntity terminal = helper.getBlockEntity(terminalPos, EchoTerminalBlockEntity.class);
                terminal.setOwnerIfMissing(player);
                helper.assertTrue(terminal.storeRewards("runtime_spine", List.of(new ItemStack(Items.BREAD, 2))),
                        "Terminal runtime-spine proof should start with claimable saved rewards.");
                EchoTerminalCoreServices.rememberTerminal(player, helper.absolutePos(terminalPos));

                events.clear();
                helper.assertTrue(TerminalActionRegistry.handle(player,
                                BuiltinTerminalCommonIntegration.REWARD_INBOX,
                                BuiltinTerminalCommonIntegration.CLAIM_REWARDS,
                                ""),
                        "Terminal reward claim action should route through the real server action registry.");
                helper.assertTrue(terminal.getStoredRewardCount() == 0,
                        "Terminal reward claim action should mutate saved terminal reward state before publishing.");
                helper.assertTrue(events.size() == 1,
                        "Successful Terminal reward claim should publish one runtime-spine event.");
                EchoRuntimeSpineEvent rewardEvent = events.getFirst();
                helper.assertTrue(TerminalRuntimeSpineBridge.TERMINAL_REWARD_CLAIMED.equals(rewardEvent.eventId()),
                        "Terminal reward claim should publish the reward-claimed runtime-spine event.");
                helper.assertTrue("terminal".equals(rewardEvent.context().get("ui_surface"))
                                && BuiltinTerminalCommonIntegration.CLAIM_REWARDS.toString().equals(
                                rewardEvent.context().get("terminal_action")),
                        "Terminal reward runtime event should identify the Terminal UI action context.");

                events.clear();
                Identifier archiveId = id("archive/runtime_spine");
                helper.assertTrue(TerminalActionRegistry.handle(player,
                                BuiltinTerminalCommonIntegration.ARCHIVES,
                                BuiltinTerminalCommonIntegration.MARK_ARCHIVE_READ,
                                archiveId.toString()),
                        "Terminal archive action should route through the real server action registry.");
                helper.assertTrue(TerminalPlayerData.get(player).isArchiveRead(archiveId),
                        "Terminal archive action should save player archive-read state before publishing.");
                helper.assertTrue(events.size() == 1,
                        "Successful Terminal archive mutation should publish one runtime-spine event.");
                EchoRuntimeSpineEvent archiveEvent = events.getFirst();
                helper.assertTrue(TerminalRuntimeSpineBridge.TERMINAL_ARCHIVE_MARKED_READ.equals(archiveEvent.eventId()),
                        "Terminal archive action should publish the archive-read runtime-spine event.");
                helper.assertTrue(archiveId.toString().equals(archiveEvent.context().get("archive_id")),
                        "Terminal archive runtime event should carry the saved archive id.");
            } finally {
                EchoRuntimeSpineBus.clearForTests();
            }
        }));
        helper.succeed();
    }

    private static void terminalRewardExplicitOwner(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos terminalPos = new BlockPos(1, 1, 1);
        helper.setBlock(terminalPos, ModBlocks.ECHO_TERMINAL_BLOCK.get());
        EchoTerminalBlockEntity terminal = helper.getBlockEntity(terminalPos, EchoTerminalBlockEntity.class);
        terminal.storeRewards("unowned", List.of(new ItemStack(Items.BREAD, 1)));

        EchoTerminalCoreServices.rememberTerminal(player, helper.absolutePos(terminalPos));
        helper.assertTrue(EchoCoreServices.pendingTerminalRewardCount(player) == 0,
                "Reward service should not expose cached terminals without explicit ownership");
        terminal.setOwnerIfMissing(player);
        EchoTerminalCoreServices.rememberTerminal(player, helper.absolutePos(terminalPos));
        helper.assertTrue(EchoCoreServices.pendingTerminalRewardCount(player) == 1,
                "Reward service should expose cached terminals after ownership is assigned");
        helper.succeed();
    }

    private static void terminalMenuValidity(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        EchoTerminalMenu remoteMenu = new EchoTerminalMenu(1, player.getInventory());
        helper.assertTrue(remoteMenu.stillValid(player), "Key-opened terminal menus should use virtual access");

        BlockPos emptyPos = helper.absolutePos(new BlockPos(3, 1, 3));
        EchoTerminalMenu missingBlockMenu = new EchoTerminalMenu(2, player.getInventory(),
                ContainerLevelAccess.create(helper.getLevel(), emptyPos));
        helper.assertFalse(missingBlockMenu.stillValid(player), "Block-opened terminal menus should require a valid block");

        BlockPos terminalPos = new BlockPos(1, 1, 1);
        helper.setBlock(terminalPos, ModBlocks.ECHO_TERMINAL_BLOCK.get());
        BlockPos absolute = helper.absolutePos(terminalPos);
        player.setPos(absolute.getX() + 0.5D, absolute.getY() + 0.5D, absolute.getZ() + 0.5D);
        EchoTerminalMenu blockMenu = new EchoTerminalMenu(3, player.getInventory(),
                ContainerLevelAccess.create(helper.getLevel(), absolute));
        helper.assertTrue(blockMenu.stillValid(player), "Block-opened terminal menus should stay valid near their block");
        helper.succeed();
    }

    private static void terminalBaselineCacheContract(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Identifier missionId = Identifier.withDefaultNamespace("story/mine_stone");
        VanillaJourneyData data = VanillaJourneyData.get(player);
        TerminalMissionSnapshot open = VanillaJourneyProvider.INSTANCE.snapshot(player, missionId);
        helper.assertTrue(open.actions().stream().anyMatch(action -> action.enabled()
                        && "refresh".equals(action.id()) && "SYNC ADVANCEMENTS".equals(action.label())),
                "Open Baseline records should expose advancement sync before cache claims");

        data.setCompleted(List.of(missionId));
        TerminalMissionSnapshot claimable = VanillaJourneyProvider.INSTANCE.snapshot(player, missionId);
        helper.assertTrue(claimable.status() == TerminalMissionStatus.CLAIMABLE,
                "Completed Baseline records should expose a claimable cache state");
        helper.assertTrue(claimable.actions().stream().anyMatch(action -> action.enabled()
                        && "claim_reward".equals(action.id()) && "CLAIM CACHE".equals(action.label())),
                "Completed Baseline records should expose a cache claim action");

        data.markClaimed(missionId);
        TerminalMissionSnapshot claimed = VanillaJourneyProvider.INSTANCE.snapshot(player, missionId);
        helper.assertTrue(claimed.status() == TerminalMissionStatus.CLAIMED,
                "Claimed Baseline records should stay claimed");
        helper.assertTrue(claimed.actions().stream().anyMatch(action -> !action.enabled()
                        && action.disabledReason().contains("already claimed")),
                "Claimed Baseline records should explain that the cache is already claimed");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    private static void terminalBaselineAutoRefresh(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Identifier missionId = Identifier.withDefaultNamespace("story/mine_stone");
        VanillaJourneyData data = VanillaJourneyData.get(player);
        data.setCompleted(List.of());
        helper.assertFalse(data.isCompleted(missionId),
                "Baseline test should start with unsynced advancement data");

        AdvancementHolder holder = player.level().getServer().getAdvancements().get(missionId);
        helper.assertTrue(holder != null, "Tracked vanilla advancement should exist on the test server");
        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(holder);
        List<String> remainingCriteria = new ArrayList<>();
        for (String criterion : progress.getRemainingCriteria()) {
            remainingCriteria.add(criterion);
        }
        helper.assertFalse(remainingCriteria.isEmpty(),
                "Tracked vanilla advancement should have criteria to award");
        for (String criterion : remainingCriteria) {
            player.getAdvancements().award(holder, criterion);
        }

        helper.assertTrue(VanillaJourneyData.get(player).isCompleted(missionId),
                "Awarding a tracked vanilla advancement should automatically sync Baseline progress");
        helper.assertTrue(VanillaJourneyProvider.INSTANCE.snapshot(player, missionId).status()
                        == TerminalMissionStatus.CLAIMABLE,
                "Automatically synced Baseline progress should make the cache claimable");
        helper.succeed();
    }

    private static void terminalBaselineDataDefinitions(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        List<TerminalMissionDefinition> definitions = VanillaJourneyProvider.INSTANCE.missions(player);
        helper.assertTrue(definitions.size() == 39,
                "Baseline should load the full vanilla advancement route from bundled data definitions");

        TerminalMissionDefinition root = definitions.stream()
                .filter(definition -> definition.id().equals(Identifier.withDefaultNamespace("story/root")))
                .findFirst()
                .orElseThrow();
        TerminalMissionSnapshot rootSnapshot = VanillaJourneyProvider.INSTANCE.snapshot(player, root.id());
        helper.assertTrue(root.rewards().isEmpty() && root.requirements().isEmpty(),
                "Data-defined Baseline roots should remain guide records without rewards");
        helper.assertTrue(VanillaJourneyProvider.INSTANCE.role(player, root, rootSnapshot) == TerminalMissionRole.REFERENCE,
                "Data-defined Baseline roots should expose reference roles");

        Identifier stoneId = Identifier.withDefaultNamespace("story/mine_stone");
        TerminalMissionDefinition stone = definitions.stream()
                .filter(definition -> definition.id().equals(stoneId))
                .findFirst()
                .orElseThrow();
        helper.assertTrue("Stone Age".equals(stone.title())
                        && "story".equals(stone.phaseId())
                        && stone.phaseOrder() == 0
                        && stone.missionOrder() == 1,
                "Data-defined Baseline missions should preserve title and ordering metadata");
        helper.assertTrue("Task Cache".equals(stone.difficulty())
                        && stone.icon().getItem() == Items.COBBLESTONE,
                "Data-defined Baseline missions should preserve tier labels and icons");
        helper.assertTrue(stone.rewards().stream().anyMatch(reward ->
                        reward.stack().getItem() == Items.BREAD && reward.stack().getCount() == 4),
                "Data-defined task cache should include bread rewards");
        helper.assertTrue(stone.rewards().stream().anyMatch(reward ->
                        reward.stack().getItem() == Items.TORCH && reward.stack().getCount() == 12),
                "Data-defined task cache should include torch rewards");
        helper.assertTrue(stone.rewards().stream().anyMatch(reward ->
                        reward.stack().getItem() == Items.EXPERIENCE_BOTTLE && reward.stack().getCount() == 2),
                "Data-defined task cache should include experience bottle rewards");
        helper.assertTrue(VanillaJourneyProvider.INSTANCE.tracksAdvancement(stoneId),
                "Data-defined Baseline missions should be tracked for server refresh");
        helper.assertFalse(VanillaJourneyProvider.INSTANCE.tracksAdvancement(id("not_a_vanilla_advancement")),
                "Unknown advancement ids should not be tracked by the Baseline provider");

        Identifier optionalId = Identifier.withDefaultNamespace("nether/all_effects");
        TerminalMissionDefinition optional = definitions.stream()
                .filter(definition -> definition.id().equals(optionalId))
                .findFirst()
                .orElseThrow();
        TerminalMissionSnapshot optionalSnapshot = VanillaJourneyProvider.INSTANCE.snapshot(player, optionalId);
        helper.assertTrue(VanillaJourneyProvider.INSTANCE.role(player, optional, optionalSnapshot)
                        == TerminalMissionRole.OPTIONAL,
                "Data-defined Baseline roles should preserve optional high-risk records");
        helper.succeed();
    }

    private static void terminalMissionBrowserCache(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        CountingMissionProvider provider = new CountingMissionProvider();
        TerminalMissionBrowser browser = new TerminalMissionBrowser(provider, id("mission_cache_tab"), true);
        TerminalRenderContext context = new TerminalRenderContext(null, player,
                800, 600, 0, 0, 480, 240, 0, null, null);

        browser.onSelected(context);
        helper.assertTrue(provider.snapshotCalls.get() == 0,
                "Selecting mission browser should not eagerly build mission snapshots");
        helper.assertFalse(browser.hasCachedStateForTests(),
                "Selecting mission browser should leave route state lazy until first height or render query");

        int height = browser.contentHeight(context);
        helper.assertTrue(height >= context.contentHeight(), "Mission browser content height should remain valid");
        helper.assertTrue(provider.snapshotCalls.get() == provider.missionCount(),
                "First mission browser height query should build one snapshot per mission");

        browser.contentHeight(context);
        helper.assertTrue(provider.snapshotCalls.get() == provider.missionCount(),
                "Repeated contentHeight in the same refresh window should reuse cached mission state");
        for (TerminalClientOptions.MissionView legacyView : TerminalClientOptions.MissionView.values()) {
            TerminalClientOptions.resetMissionViewForTests(legacyView);
            helper.assertTrue(TerminalClientOptions.missionView == TerminalClientOptions.MissionView.GUIDED,
                    "Legacy mission view config values should normalize to GUIDED");
            browser.contentHeight(context);
            helper.assertTrue(provider.snapshotCalls.get() == provider.missionCount(),
                    "Legacy mission view aliases should not invalidate the guided-only browser cache");
        }

        TerminalRenderContext widerContext = new TerminalRenderContext(null, player,
                800, 600, 0, 0, 960, 240, 0, null, null);
        browser.contentHeight(widerContext);
        helper.assertTrue(provider.snapshotCalls.get() == provider.missionCount(),
                "A stale mission browser cache should be reusable for one frame after a width bucket change");
        browser.contentHeight(widerContext);
        helper.assertTrue(provider.snapshotCalls.get() == provider.missionCount() * 2,
                "Changing the width bucket should refresh the mission browser cache after the stale safety frame");
        helper.succeed();
    }

    private static void terminalMissionBrowserPhaseGating(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Identifier phase00Main = id("phase_00_main");
        Identifier phase00Optional = id("phase_00_optional");
        Identifier phase00Reference = id("phase_00_reference");
        Identifier phase01Main = id("phase_01_main");
        Identifier phase02Main = id("phase_02_main");
        PhaseGatingMissionProvider provider = new PhaseGatingMissionProvider(List.of(
                new PhaseGatingMission(phase00Main, "Awaken", "Provider Awakening", 0, 1,
                        TerminalMissionRole.MAIN, TerminalMissionStatus.CLAIMABLE,
                        List.of(TerminalMissionAction.enabled("claim_reward", "CLAIM"))),
                new PhaseGatingMission(phase00Optional, "Old Ecology", "Provider Awakening", 0, 2,
                        TerminalMissionRole.OPTIONAL, TerminalMissionStatus.UNLOCKED,
                        List.of(TerminalMissionAction.enabled("note", "NOTE"))),
                new PhaseGatingMission(phase00Reference, "Old World Note", "Provider Awakening", 0, 3,
                        TerminalMissionRole.REFERENCE, TerminalMissionStatus.VIEW_ONLY, List.of()),
                new PhaseGatingMission(phase01Main, "Hold Camp", "Provider Stability", 10, 1,
                        TerminalMissionRole.MAIN, TerminalMissionStatus.UNLOCKED,
                        List.of(TerminalMissionAction.enabled("start", "START"))),
                new PhaseGatingMission(phase02Main, "Build Relay", "Provider Machinery", 20, 1,
                        TerminalMissionRole.MAIN, TerminalMissionStatus.UNLOCKED,
                        List.of(TerminalMissionAction.enabled("power", "POWER")))));
        TerminalMissionBrowser browser = new TerminalMissionBrowser(provider, id("phase_gating_tab"), true);
        TerminalRenderContext context = new TerminalRenderContext(null, player,
                800, 600, 0, 0, 640, 260, 0, null, null);

        TerminalClientOptions.resetMissionViewForTests(TerminalClientOptions.MissionView.VISUAL_RPG);
        browser.onSelected(context);
        List<String> phases = browser.phaseDebugRowsForTests(context);
        helper.assertTrue(browser.visibleMissionCountForTests(context) == browser.allMissionCountForTests(context),
                "Mission browser should keep every record visible now that roadmap filters are removed");
        helper.assertTrue("ACTIONS".equals(browser.stickyActionsTitleForTests()),
                "Mission details should label the sticky footer as ACTIONS");
        helper.assertTrue(List.of(
                        browser.rowStatusLabelForTests(TerminalMissionStatus.CLAIMABLE, false),
                        browser.rowStatusLabelForTests(TerminalMissionStatus.UNLOCKED, false),
                        browser.rowStatusLabelForTests(TerminalMissionStatus.COMPLETED, false),
                        browser.rowStatusLabelForTests(TerminalMissionStatus.CLAIMED, false),
                        browser.rowStatusLabelForTests(TerminalMissionStatus.VIEW_ONLY, false),
                        browser.rowStatusLabelForTests(TerminalMissionStatus.UNLOCKED, true))
                        .equals(List.of("READY", "ACTIVE", "DONE", "DONE", "INFO", "LOCKED")),
                "Mission roadmap rows should use compact status labels while detail panels keep full context");
        helper.assertTrue(browser.enabledActionCountForTests(context, phase00Main) == 1,
                "Full-action mission browser pages should expose enabled mission actions");
        helper.assertFalse(browser.emptyRequirementsCopyForTests().contains("COMMAND")
                        || browser.metRequirementsCopyForTests().contains("COMMAND"),
                "Requirement helper copy should not point players back to a Command footer");
        int compactTreeHeight = browser.treePaneHeightForTests(context, 180);
        int wideTreeHeight = browser.treePaneHeightForTests(context, 640);
        helper.assertTrue(compactTreeHeight == wideTreeHeight,
                "Mission browser tree height should not reserve responsive filter or expand-control rows");
        Identifier initialSelection = browser.selectedMissionIdForTests(context);
        helper.assertFalse(browser.keyCodeForTests(context, GLFW.GLFW_KEY_LEFT),
                "Left arrow should no longer cycle hidden mission filters");
        helper.assertFalse(browser.keyCodeForTests(context, GLFW.GLFW_KEY_RIGHT),
                "Right arrow should no longer cycle hidden mission filters");
        helper.assertTrue(initialSelection.equals(browser.selectedMissionIdForTests(context)),
                "Removing hidden mission filters should leave arrow keys from changing the selected record");
        helper.assertFalse(browser.charTyped(context, null),
                "Typing should no longer feed a hidden mission search box");
        helper.assertTrue(browser.visibleMissionCountForTests(context) == 5,
                "Typing with hidden search removed should not hide mixed-status mission records");
        helper.assertTrue(phases.size() == 3, "Browser should expose every named phase, including locked previews");
        helper.assertTrue(phases.get(0).startsWith("Provider Awakening|COMPLETE|Provider Awakening"),
                "Claimable MAIN objectives should complete the Provider Awakening stage");
        helper.assertTrue(phases.get(1).startsWith("Provider Stability|ACTIVE|Provider Stability"),
                "A completed Provider Awakening stage should unlock Provider Stability");
        helper.assertTrue(phases.get(2).startsWith("Provider Machinery|LOCKED|Provider Machinery"),
                "Incomplete Provider Stability MAIN objectives should lock Provider Machinery");
        helper.assertFalse(browser.missionReadOnlyForTests(context, phase01Main),
                "Incomplete OPTIONAL and REFERENCE records in Provider Awakening should not block Provider Stability");
        helper.assertTrue(browser.phaseExpandedForTests(context, "Provider Awakening"),
                "Claimable phases should expand by default");
        helper.assertTrue(browser.phaseExpandedForTests(context, "Provider Stability"),
                "The current unlocked incomplete phase should expand by default");
        helper.assertFalse(browser.phaseExpandedForTests(context, "Provider Machinery"),
                "Locked future phases should stay collapsed by default");
        List<Identifier> roadmapIds = browser.roadmapMissionIdsForTests(context);
        helper.assertTrue(roadmapIds.size() == browser.visibleMissionCountForTests(context)
                        && roadmapIds.stream().distinct().count() == roadmapIds.size(),
                "Roadmap mission rows should not duplicate current or ready records above the phase checklist");
        helper.assertTrue("Awaken".equals(browser.missionRowTitleForTests(context, phase00Main)),
                "Mission row titles should omit technical mission-order prefixes");
        helper.assertTrue("Provider Awakening".equals(browser.detailPhaseChipForTests(context, phase00Main)),
                "Detail cards should use named phase labels instead of generated PHASE chips");
        helper.assertTrue(phase00Main.equals(browser.focusMissionIdForTests(context))
                        && phase00Main.equals(browser.selectedMissionIdForTests(context)),
                "Mission browser should auto-focus the current ready mission by default");
        int focusViewportHeight = 80;
        int focusRowOffset = browser.selectedRowOffsetForTests(context);
        int maxTreeScroll = browser.treeMaxScrollForTests(context, focusViewportHeight);
        helper.assertTrue(maxTreeScroll > focusRowOffset,
                "Mission browser test setup should allow the selected full-roadmap row to top-align");
        helper.assertTrue(browser.applyTreeFocusForTests(context, focusViewportHeight) == focusRowOffset,
                "Opening the mission browser should top-align the current ready mission in the full roadmap");
        TerminalMissionBrowser topFocusBrowser = new TerminalMissionBrowser(provider, id("phase_gating_top_tab"),
                true, TerminalMissionBrowser.ActionMode.FULL_ACTIONS, TerminalMissionBrowser.InitialTreeFocus.TOP);
        topFocusBrowser.onSelected(context);
        helper.assertTrue(phase00Main.equals(topFocusBrowser.selectedMissionIdForTests(context)),
                "Top-focused Survival Route pages should preserve the current ready mission selection");
        helper.assertTrue(topFocusBrowser.applyTreeFocusForTests(context, focusViewportHeight) == 0,
                "Top-focused Survival Route pages should open the roadmap column at the very top");
        TerminalMissionBrowser trackingBrowser = new TerminalMissionBrowser(provider, id("phase_gating_tracking_tab"),
                true, TerminalMissionBrowser.ActionMode.TRACKING_ONLY,
                TerminalMissionBrowser.InitialTreeFocus.ALIGN_SELECTED_TOP);
        trackingBrowser.onSelected(context);
        helper.assertTrue(trackingBrowser.trackingOnlyForTests(),
                "Chapter reference mission browsers should opt into tracking-only mode");
        helper.assertTrue(trackingBrowser.missionReadOnlyForTests(context, phase00Main),
                "Tracking-only mission browsers should treat actionable records as read-only references");
        helper.assertTrue(trackingBrowser.enabledActionCountForTests(context, phase00Main) == 0,
                "Tracking-only mission browsers should suppress claim, scan, and turn-in actions");
        helper.assertFalse(trackingBrowser.activateMissionActionForTests(context, phase00Main),
                "Tracking-only mission browsers should not execute mission actions on activation");
        helper.assertTrue(browser.keyCodeForTests(context, GLFW.GLFW_KEY_DOWN),
                "Down arrow should move from the ready mission to the next visible mission");
        helper.assertTrue(phase00Optional.equals(browser.selectedMissionIdForTests(context)),
                "Down arrow should select the next mission in the expanded roadmap");
        int scrollBeforeNavigationFocus = browser.treeScrollForTests();
        int navigatedRowOffset = browser.selectedRowOffsetForTests(context);
        int scrollAfterNavigationFocus = browser.applyTreeFocusForTests(context, focusViewportHeight);
        helper.assertTrue(scrollAfterNavigationFocus == scrollBeforeNavigationFocus,
                "Keyboard navigation should keep an already visible selected mission in place");
        helper.assertFalse(scrollAfterNavigationFocus == navigatedRowOffset,
                "Keyboard navigation should not top-align every newly selected mission");
        int headerHeight = browser.detailHeaderHeightForTests(context, phase02Main);
        helper.assertTrue(headerHeight >= 92 && headerHeight <= 104,
                "Guided mission browser detail header should stay compact for next-step-first scanning");
        helper.assertTrue(browser.selectMissionForTests(context, phase02Main),
                "Locked future missions should remain selectable for preview");
        helper.assertTrue(browser.phaseExpandedForTests(context, "Provider Machinery"),
                "Selecting a locked preview should expand its phase");
        helper.assertTrue(browser.missionReadOnlyForTests(context, phase02Main),
                "Locked future missions should be read-only");
        helper.assertTrue(browser.enabledActionCountForTests(context, phase02Main) == 0,
                "Locked future missions should not expose enabled actions");
        helper.assertFalse(browser.activateMissionActionForTests(context, phase02Main),
                "Locked future mission actions should not be sent");
        Identifier optionalReady = id("optional_ready");
        Identifier activeMain = id("active_main");
        Identifier completedMain = id("completed_main");
        PhaseGatingMissionProvider focusProvider = new PhaseGatingMissionProvider(List.of(
                new PhaseGatingMission(completedMain, "Completed Main", "Focus Opening", 0, 1,
                        TerminalMissionRole.MAIN, TerminalMissionStatus.COMPLETED, List.of()),
                new PhaseGatingMission(optionalReady, "Optional Cache", "Focus Opening", 0, 2,
                        TerminalMissionRole.OPTIONAL, TerminalMissionStatus.CLAIMABLE,
                        List.of(TerminalMissionAction.enabled("claim_reward", "CLAIM"))),
                new PhaseGatingMission(activeMain, "Active Main", "Focus Active", 1, 1,
                        TerminalMissionRole.MAIN, TerminalMissionStatus.UNLOCKED,
                        List.of(TerminalMissionAction.enabled("start", "START")))));
        TerminalMissionBrowser focusBrowser = new TerminalMissionBrowser(focusProvider, id("focus_order_tab"), true);
        focusBrowser.onSelected(context);
        helper.assertTrue(activeMain.equals(focusBrowser.focusMissionIdForTests(context)),
                "Active MAIN objectives should rank ahead of optional claimable rewards in the route summary");
        TerminalMissionBrowser survivalOpeningBrowser = new TerminalMissionBrowser(focusProvider,
                id("survival_opening_tab"), true, TerminalMissionBrowser.ActionMode.FULL_ACTIONS,
                TerminalMissionBrowser.InitialTreeFocus.TOP, TerminalMissionBrowser.InitialSelection.FIRST_RECORD,
                TerminalMissionBrowser.DefaultPhaseExpansion.FIRST_ONLY);
        survivalOpeningBrowser.onSelected(context);
        helper.assertTrue(completedMain.equals(survivalOpeningBrowser.selectedMissionIdForTests(context)),
                "Survival Route opening mode should select the first visible mission instead of the active focus");
        helper.assertTrue(survivalOpeningBrowser.applyTreeFocusForTests(context, focusViewportHeight) == 0,
                "Survival Route opening mode should keep the roadmap scrolled to the top");
        helper.assertTrue(survivalOpeningBrowser.phaseExpandedForTests(context, "Focus Opening"),
                "Survival Route opening mode should expand the first phase by default");
        helper.assertFalse(survivalOpeningBrowser.phaseExpandedForTests(context, "Focus Active"),
                "Survival Route opening mode should collapse later phases by default");
        helper.assertTrue(survivalOpeningBrowser.selectMissionForTests(context, activeMain)
                        && survivalOpeningBrowser.phaseExpandedForTests(context, "Focus Active"),
                "Selecting a later mission should still expand its phase in Survival Route opening mode");
        helper.succeed();
    }

    private static void terminalMissionHudNotifications(GameTestHelper helper) {
        TerminalMissionRegistry.withClearedForTests(() -> {
            TerminalMissionHudController controller = new TerminalMissionHudController();
            MutableHudMissionProvider provider = new MutableHudMissionProvider(id("hud_chapter"), "HUD Chapter", 25);
            Identifier relay = id("hud_relay");
            Identifier camp = id("hud_camp");
            Identifier burstA = id("hud_burst_a");
            Identifier burstB = id("hud_burst_b");
            Identifier burstC = id("hud_burst_c");
            Identifier burstD = id("hud_burst_d");
            provider.add(relay, "Repair Relay", "Relay Phase", 10, 1,
                    TerminalMissionRole.MAIN, TerminalMissionStatus.LOCKED, 0.0F);
            provider.add(camp, "Stabilize Camp", "Camp Phase", 0, 1,
                    TerminalMissionRole.MAIN, TerminalMissionStatus.UNLOCKED, 0.45F);
            provider.add(burstA, "Burst A", "Factory Phase", 20, 1,
                    TerminalMissionRole.MAIN, TerminalMissionStatus.LOCKED, 0.0F);
            provider.add(burstB, "Burst B", "Factory Phase", 20, 2,
                    TerminalMissionRole.MAIN, TerminalMissionStatus.LOCKED, 0.0F);
            provider.add(burstC, "Burst C", "Factory Phase", 20, 3,
                    TerminalMissionRole.MAIN, TerminalMissionStatus.LOCKED, 0.0F);
            provider.add(burstD, "Burst D", "Factory Phase", 20, 4,
                    TerminalMissionRole.MAIN, TerminalMissionStatus.LOCKED, 0.0F);

            TerminalMissionRegistry.register(MainSurvivalQuestProvider.INSTANCE);
            TerminalMissionRegistry.register(provider);
            TerminalMissionRegistry.register(new ThrowingMissionsProvider(id("hud_throwing"), 50));

            controller.scanForTests(null, 100L);
            helper.assertTrue(controller.drainQueuedNoticesForTests().isEmpty(),
                    "First mission HUD scan should baseline without startup notices");

            provider.set(relay, TerminalMissionStatus.UNLOCKED, 0.25F);
            controller.scanForTests(null, 120L);
            List<TerminalMissionNotice> relayNotices = controller.drainQueuedNoticesForTests();
            helper.assertTrue(hasNotice(relayNotices, TerminalMissionNoticeType.MISSION_AVAILABLE),
                    "Locked-to-unlocked missions should raise a mission available notice");
            helper.assertTrue(hasNotice(relayNotices, TerminalMissionNoticeType.PHASE_ONLINE),
                    "The first active mission in a phase should raise a phase online notice");
            helper.assertFalse(relayNotices.stream()
                            .anyMatch(notice -> MainSurvivalQuestProvider.CHAPTER_ID.equals(notice.chapterId())),
                    "Mission HUD should skip the aggregate Survival Route provider");

            provider.set(relay, TerminalMissionStatus.LOCKED, 0.0F);
            controller.scanForTests(null, 125L);
            provider.set(relay, TerminalMissionStatus.UNLOCKED, 0.25F);
            controller.scanForTests(null, 130L);
            helper.assertTrue(controller.drainQueuedNoticesForTests().isEmpty(),
                    "Repeated mission available signals should respect the notice cooldown");

            provider.set(camp, TerminalMissionStatus.COMPLETED, 1.0F);
            controller.scanForTests(null, 160L);
            helper.assertTrue(hasNotice(controller.drainQueuedNoticesForTests(), TerminalMissionNoticeType.OBJECTIVE_READY),
                    "Unlocked-to-completed missions should raise an objective ready notice");

            provider.set(relay, TerminalMissionStatus.CLAIMABLE, 1.0F);
            controller.scanForTests(null, 220L);
            helper.assertTrue(hasNotice(controller.drainQueuedNoticesForTests(), TerminalMissionNoticeType.CACHE_READY),
                    "Claimable missions should raise a cache ready notice");

            provider.set(relay, TerminalMissionStatus.CLAIMED, 1.0F);
            controller.scanForTests(null, 300L);
            helper.assertTrue(hasNotice(controller.drainQueuedNoticesForTests(), TerminalMissionNoticeType.CACHE_CLAIMED),
                    "Claimed rewards should raise a cache claimed notice");

            provider.set(burstA, TerminalMissionStatus.UNLOCKED, 0.1F);
            provider.set(burstB, TerminalMissionStatus.UNLOCKED, 0.1F);
            provider.set(burstC, TerminalMissionStatus.UNLOCKED, 0.1F);
            provider.set(burstD, TerminalMissionStatus.UNLOCKED, 0.1F);
            controller.scanForTests(null, 400L);
            List<TerminalMissionNotice> burstNotices = controller.drainQueuedNoticesForTests();
            helper.assertTrue(burstNotices.size() == 1
                            && burstNotices.get(0).type() == TerminalMissionNoticeType.SUMMARY,
                    "Large mission update bursts should collapse into a single summary card");
        });
        helper.succeed();
    }

    private static void terminalHudNoticeSurface(GameTestHelper helper) {
        TerminalHudNoticeSurface.resetForTests();
        helper.assertFalse(TerminalHudNoticeSurface.externalSurfaceClaimed(),
                "Terminal notice surface should start unclaimed");
        helper.assertTrue(TerminalHudNoticeSurface.shouldRenderInternalCards(),
                "Terminal should render internal cards before an external surface claims notices");
        TerminalHudNoticeSurface.claimExternalSurface("echoashfallprotocol");
        helper.assertTrue(TerminalHudNoticeSurface.externalSurfaceClaimed(),
                "External notice surface should be claimable");
        helper.assertFalse(TerminalHudNoticeSurface.shouldRenderInternalCards(),
                "Terminal internal cards should be suppressed while an external surface is claimed");
        helper.assertTrue("echoashfallprotocol".equals(TerminalHudNoticeSurface.externalSurfaceOwner()),
                "External surface owner should be tracked");

        TerminalHudNotice mission = TerminalMissionHudController.noticeForHudForTests(new TerminalMissionNotice(
                TerminalMissionNoticeType.SUMMARY,
                id("hud_chapter"),
                id("mission/sync"),
                "HUD Chapter",
                "3 mission signals updated",
                "Open the ECHO Terminal to review the refreshed route state.",
                "Provider sync",
                "SYNC",
                ItemStack.EMPTY,
                0xFF66E8FF,
                0.75F,
                3));
        helper.assertTrue("MISSION SYNC".equals(mission.sourceLabel()),
                "Mission notice source should use the notice type label");
        helper.assertTrue("SYNC".equals(mission.statusLabel()),
                "Mission notice status should survive compact mapping");
        helper.assertTrue(mission.hasProgress() && mission.progress() == 0.75F,
                "Mission notice progress should survive compact mapping");
        helper.assertTrue(mission.hasCountBadge() && mission.count() == 3,
                "Mission notice count should survive compact mapping");

        TerminalHudNotice discovery = DiscoveryToastHud.noticeForHudForTests(new EchoDiscoveryToast(
                id("discovery/ruined_plains"),
                "Biomes",
                "Ruined Plains",
                "Added to Discovery Grid",
                "",
                "",
                0xFF92F7A6));
        helper.assertTrue("DISCOVERY".equals(discovery.sourceLabel()),
                "Discovery notices should name their source");
        helper.assertTrue("Biomes".equals(discovery.statusLabel()),
                "Discovery category should become compact status text");
        helper.assertTrue("Discovery Grid".equals(discovery.footer()),
                "Discovery notices should keep the grid footer");
        helper.assertFalse(discovery.hasProgress(),
                "Discovery notices should not draw progress");

        TerminalHudNotice external = new TerminalHudNotice(
                "ECHO-7",
                "GUIDE",
                "Guide Card",
                "Unlocked: First Hour Survival",
                "",
                0xFF92F7A6,
                0.0F,
                1);
        TerminalHudNoticeSurface.registerExternalNoticeSupplier("echotutorialcore", () -> Optional.of(external));
        helper.assertTrue(TerminalHudNoticeSurface.activeNotices().stream()
                        .anyMatch(notice -> "Guide Card".equals(notice.title())
                                && "GUIDE".equals(notice.statusLabel())),
                "External notice suppliers should publish compact rows after Terminal notices");
        TerminalHudNoticeSurface.unregisterExternalNoticeSupplier("echotutorialcore");
        helper.assertFalse(TerminalHudNoticeSurface.activeNotices().stream()
                        .anyMatch(notice -> "Guide Card".equals(notice.title())),
                "Unregistered external notice suppliers should stop publishing rows");

        TerminalHudNoticeSurface.releaseExternalSurface("other");
        helper.assertTrue(TerminalHudNoticeSurface.externalSurfaceClaimed(),
                "Mismatched surface release should not clear ownership");
        TerminalHudNoticeSurface.releaseExternalSurface("echoashfallprotocol");
        helper.assertFalse(TerminalHudNoticeSurface.externalSurfaceClaimed(),
                "Matching surface release should clear ownership");
        helper.assertTrue(TerminalHudNoticeSurface.shouldRenderInternalCards(),
                "Terminal internal cards should resume after the external surface is released");
        TerminalHudNoticeSurface.registerExternalNoticeSupplier("echotutorialcore", () -> Optional.of(external));
        TerminalHudNoticeSurface.resetForTests();
        helper.assertFalse(TerminalHudNoticeSurface.externalSurfaceClaimed(),
                "Reset should clear the claimed notice surface");
        helper.assertFalse(TerminalHudNoticeSurface.activeNotices().stream()
                        .anyMatch(notice -> "Guide Card".equals(notice.title())),
                "Reset should clear external notice suppliers");
        helper.succeed();
    }

    private static boolean hasNotice(List<TerminalMissionNotice> notices, TerminalMissionNoticeType type) {
        return notices.stream().anyMatch(notice -> notice.type() == type);
    }

    private static void assertRuntimeTextureBudget(GameTestHelper helper) {
        Path root = terminalGuiTextureRoot();
        if (!Files.isDirectory(root)) {
            return;
        }
        try (var paths = Files.walk(root)) {
            long bytes = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".png"))
                    .mapToLong(path -> {
                        try {
                            return Files.size(path);
                        } catch (IOException exception) {
                            return TERMINAL_TEXTURE_BUDGET_BYTES + 1L;
                        }
                    })
                    .sum();
            helper.assertTrue(bytes <= TERMINAL_TEXTURE_BUDGET_BYTES,
                    "Terminal runtime PNGs should stay under "
                            + (TERMINAL_TEXTURE_BUDGET_BYTES / 1024L / 1024L)
                            + " MB after optimization; found " + (bytes / 1024L / 1024L) + " MB");
        } catch (IOException exception) {
            helper.assertTrue(false, "Terminal runtime texture budget scan failed: " + exception.getMessage());
        }
    }

    private static void assertRuntimeTextureDimensionCaps(GameTestHelper helper) {
        Path root = terminalGuiTextureRoot();
        if (!Files.isDirectory(root)) {
            return;
        }
        try (var paths = Files.walk(root)) {
            List<String> issues = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".png"))
                    .map(path -> textureDimensionIssue(root, path))
                    .filter(issue -> !issue.isBlank())
                    .toList();
            helper.assertTrue(issues.isEmpty(),
                    "Terminal runtime PNGs should obey balanced dimension caps: " + issues);
        } catch (IOException exception) {
            helper.assertTrue(false, "Terminal runtime texture dimension scan failed: " + exception.getMessage());
        }
    }

    private static Path terminalGuiTextureRoot() {
        return Path.of("addons", "echoterminal", "src", "main", "resources",
                "assets", EchoTerminal.MODID, "textures", "gui");
    }

    private static String textureDimensionIssue(Path root, Path path) {
        String rel = root.relativize(path).toString().replace('\\', '/');
        int maxW = 0;
        int maxH = 0;
        if (rel.endsWith("backgrounds/terminal_backdrop.png")
                || rel.endsWith("terminal/terminal_frame_backdrop.png")) {
            maxW = 1280;
            maxH = 720;
        } else if (rel.contains("mission_heroes/")) {
            maxW = 512;
            maxH = 256;
        } else if (rel.contains("mission_icons/")) {
            maxW = 128;
            maxH = 128;
        } else if (rel.startsWith("terminal/") || rel.contains("/terminal/")) {
            maxW = 1024;
            maxH = 512;
        }
        if (maxW <= 0) {
            return "";
        }
        try {
            BufferedImage image = ImageIO.read(path.toFile());
            if (image == null) {
                return rel + " is unreadable";
            }
            if (image.getWidth() > maxW || image.getHeight() > maxH) {
                return rel + " is " + image.getWidth() + "x" + image.getHeight()
                        + ", max " + maxW + "x" + maxH;
            }
            return "";
        } catch (IOException exception) {
            return rel + " failed to read: " + exception.getMessage();
        }
    }

    private record DummyTab(TerminalTabDescriptor descriptor) implements ClientTerminalTab {
        DummyTab(Identifier id, String title, int order) {
            this(new TerminalTabDescriptor(id, title, order, 0xFF66D9FF));
        }

        public void render(TerminalRenderContext context, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        }
    }

    private record DummyChromeTab(TerminalTabDescriptor descriptor, TerminalTabChrome chrome) implements ClientTerminalTab {
        public void render(TerminalRenderContext context, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        }
    }

    private record DummyAddonChapter(String id, String displayName) implements EchoAddonChapter {
        @Override
        public String modId() {
            return "echotest";
        }

        @Override
        public String summary() {
            return "Test addon chapter.";
        }
    }

    private record DummyAddonInfoProvider(String chapterId, TerminalAddonInfo info) implements TerminalAddonInfoProvider {
        @Override
        public TerminalAddonInfo info(Player player) {
            return info;
        }
    }

    private record DummyRecipeProvider(Identifier providerId, int order) implements TerminalRecipeProvider {
        @Override
        public Identifier id() {
            return providerId;
        }

        @Override
        public List<TerminalRecipeCategory> categories(Player player) {
            return List.of(new TerminalRecipeCategory(
                    ModGameTests.id(providerId.getPath().replace("_provider", "_category")),
                    providerId.getPath(),
                    new ItemStack(Items.CRAFTING_TABLE),
                    0xFFFFD166,
                    order));
        }

        @Override
        public List<TerminalRecipeEntry> recipes(Player player) {
            Identifier category = ModGameTests.id(providerId.getPath().replace("_provider", "_category"));
            return List.of(new TerminalRecipeEntry(
                    ModGameTests.id(providerId.getPath() + "/recipe"),
                    category,
                    "Recipe " + providerId.getPath(),
                    new ItemStack(Items.CRAFTING_TABLE),
                    List.of(TerminalRecipeSlot.input(new ItemStack(Items.STICK)),
                            TerminalRecipeSlot.output(new ItemStack(Items.APPLE))),
                    List.of(TerminalRecipeNote.info("Test recipe")),
                    20,
                    false));
        }
    }

    private record LargeRecipeProvider(
            Identifier providerId,
            int count,
            AtomicInteger categoryCalls,
            AtomicInteger recipeCalls) implements TerminalRecipeProvider {
        @Override
        public Identifier id() {
            return providerId;
        }

        @Override
        public List<TerminalRecipeCategory> categories(Player player) {
            categoryCalls.incrementAndGet();
            return List.of(new TerminalRecipeCategory(
                    ModGameTests.id("large_category"),
                    "Large Category",
                    new ItemStack(Items.CRAFTING_TABLE),
                    0xFFFFD166,
                    10));
        }

        @Override
        public List<TerminalRecipeEntry> recipes(Player player) {
            recipeCalls.incrementAndGet();
            List<TerminalRecipeEntry> recipes = new ArrayList<>();
            for (int index = 0; index < count; index++) {
                boolean needle = index == count - 1;
                recipes.add(new TerminalRecipeEntry(
                        ModGameTests.id(providerId.getPath() + "/recipe_" + index),
                        ModGameTests.id("large_category"),
                        needle ? "Needle Recipe" : "Bulk Recipe " + index,
                        new ItemStack(Items.CRAFTING_TABLE),
                        List.of(TerminalRecipeSlot.input(new ItemStack(Items.STICK, index % 3 + 1)),
                                TerminalRecipeSlot.output(new ItemStack(Items.APPLE))),
                        List.of(TerminalRecipeNote.info(needle ? "needle searchable row" : "bulk recipe row")),
                        20,
                        false));
            }
            return recipes;
        }
    }

    private record DuplicateRecipeProvider() implements TerminalRecipeProvider {
        @Override
        public Identifier id() {
            return ModGameTests.id("duplicate_provider");
        }

        @Override
        public List<TerminalRecipeCategory> categories(Player player) {
            return List.of(new TerminalRecipeCategory(
                    ModGameTests.id("alpha_category"),
                    "duplicate category",
                    new ItemStack(Items.CRAFTING_TABLE),
                    0xFFFFD166,
                    5));
        }

        @Override
        public List<TerminalRecipeEntry> recipes(Player player) {
            return List.of(new TerminalRecipeEntry(
                    ModGameTests.id("alpha_provider/recipe"),
                    ModGameTests.id("alpha_category"),
                    "Duplicate Recipe",
                    new ItemStack(Items.CRAFTING_TABLE),
                    List.of(TerminalRecipeSlot.input(new ItemStack(Items.STICK)),
                            TerminalRecipeSlot.output(new ItemStack(Items.APPLE))),
                    List.of(),
                    20,
                    false));
        }
    }

    private record ThrowingRecipeProvider() implements TerminalRecipeProvider {
        @Override
        public Identifier id() {
            return ModGameTests.id("throwing_provider");
        }

        @Override
        public List<TerminalRecipeCategory> categories(Player player) {
            throw new IllegalStateException("test terminal recipe category failure");
        }

        @Override
        public List<TerminalRecipeEntry> recipes(Player player) {
            throw new IllegalStateException("test terminal recipe list failure");
        }
    }

    private record LinkageErrorRecipeProvider() implements TerminalRecipeProvider {
        @Override
        public Identifier id() {
            return ModGameTests.id("linkage_error_provider");
        }

        @Override
        public List<TerminalRecipeCategory> categories(Player player) {
            throw new NoClassDefFoundError("test missing recipe bridge");
        }

        @Override
        public List<TerminalRecipeEntry> recipes(Player player) {
            throw new NoClassDefFoundError("test missing recipe bridge");
        }
    }

    private record NullAddonInfoProvider(String chapterId) implements TerminalAddonInfoProvider {
        @Override
        public TerminalAddonInfo info(Player player) {
            return null;
        }
    }

    private record ThrowingAddonInfoProvider(String chapterId) implements TerminalAddonInfoProvider {
        @Override
        public TerminalAddonInfo info(Player player) {
            throw new IllegalStateException("test terminal addon info failure");
        }
    }

    private record ThrowingAddonChapterIdProvider() implements TerminalAddonInfoProvider {
        @Override
        public String chapterId() {
            throw new IllegalStateException("test terminal addon chapter id failure");
        }

        @Override
        public TerminalAddonInfo info(Player player) {
            return TerminalAddonInfo.empty();
        }
    }

    private record PhaseGatingMission(
            Identifier id,
            String title,
            String phaseTitle,
            int phaseOrder,
            int missionOrder,
            TerminalMissionRole role,
            TerminalMissionStatus status,
            List<TerminalMissionAction> actions) {
    }

    private record PhaseGatingMissionProvider(List<PhaseGatingMission> missions) implements TerminalMissionProvider {
        private static final Identifier CHAPTER_ID = Identifier.fromNamespaceAndPath(EchoTerminal.MODID, "phase_gating");

        @Override
        public TerminalMissionChapter chapter() {
            return new TerminalMissionChapter(CHAPTER_ID, "Phase Gating", "Phase gating test provider",
                    1, 0xFF66D9FF, true);
        }

        @Override
        public List<TerminalMissionDefinition> missions(Player player) {
            return missions.stream()
                    .map(mission -> new TerminalMissionDefinition(
                            mission.id(),
                            CHAPTER_ID,
                            mission.phaseTitle().toLowerCase(java.util.Locale.ROOT).replace(' ', '_'),
                            mission.phaseTitle(),
                            mission.phaseOrder(),
                            mission.missionOrder(),
                            mission.title(),
                            mission.title() + " briefing",
                            mission.title() + " field guide",
                            "Test",
                            "Test",
                            ItemStack.EMPTY,
                            List.of(),
                            List.of(),
                            List.of()))
                    .toList();
        }

        @Override
        public TerminalMissionSnapshot snapshot(Player player, Identifier missionId) {
            PhaseGatingMission mission = missions.stream()
                    .filter(candidate -> candidate.id().equals(missionId))
                    .findFirst()
                    .orElse(null);
            return mission == null
                    ? new TerminalMissionSnapshot(missionId, TerminalMissionStatus.LOCKED, 0.0F,
                            "LOCKED", "Missing phase test mission.", "Missing phase test mission.", List.of())
                    : new TerminalMissionSnapshot(mission.id(), mission.status(),
                            mission.status() == TerminalMissionStatus.CLAIMABLE ? 1.0F : 0.25F,
                            mission.status().name(), "", mission.title() + " next step", mission.actions());
        }

        @Override
        public TerminalMissionRole role(Player player, TerminalMissionDefinition definition, TerminalMissionSnapshot snapshot) {
            return missions.stream()
                    .filter(candidate -> candidate.id().equals(definition.id()))
                    .map(PhaseGatingMission::role)
                    .findFirst()
                    .orElse(TerminalMissionRole.MAIN);
        }
    }

    private static final class CountingMissionProvider implements TerminalMissionProvider {
        private final AtomicInteger snapshotCalls = new AtomicInteger();
        private final Identifier chapterId = id("cache_chapter");
        private final List<TerminalMissionDefinition> definitions = List.of(
                definition(id("cache_mission_a"), 0, 1, "Cache Mission A"),
                definition(id("cache_mission_b"), 0, 2, "Cache Mission B"));

        int missionCount() {
            return definitions.size();
        }

        @Override
        public TerminalMissionChapter chapter() {
            return new TerminalMissionChapter(chapterId, "Cache Chapter", "Cache test provider",
                    1, 0xFF66D9FF, true);
        }

        @Override
        public List<TerminalMissionDefinition> missions(Player player) {
            return definitions;
        }

        @Override
        public TerminalMissionSnapshot snapshot(Player player, Identifier missionId) {
            snapshotCalls.incrementAndGet();
            return new TerminalMissionSnapshot(
                    missionId,
                    TerminalMissionStatus.UNLOCKED,
                    0.25F,
                    "UNLOCKED",
                    "",
                    "Cache test",
                    List.of(TerminalMissionAction.enabled("claim_reward", "CLAIM")));
        }

        private TerminalMissionDefinition definition(Identifier missionId, int phaseOrder, int missionOrder, String title) {
            return new TerminalMissionDefinition(
                    missionId,
                    chapterId,
                    "cache",
                    "Cache",
                    phaseOrder,
                    missionOrder,
                    title,
                    "Cache test briefing",
                    "Cache test field guide",
                    "Test",
                    "Test",
                    ItemStack.EMPTY,
                    List.of(),
                    List.of(),
                    List.of());
        }
    }

    private static final class MutableHudMissionProvider implements TerminalMissionProvider {
        private final Identifier chapterId;
        private final String title;
        private final int order;
        private final Map<Identifier, MutableHudMission> missions = new LinkedHashMap<>();

        private MutableHudMissionProvider(Identifier chapterId, String title, int order) {
            this.chapterId = chapterId;
            this.title = title;
            this.order = order;
        }

        void add(Identifier missionId, String missionTitle, String phaseTitle, int phaseOrder, int missionOrder,
                TerminalMissionRole role, TerminalMissionStatus status, float progress) {
            missions.put(missionId, new MutableHudMission(
                    missionId, missionTitle, phaseTitle, phaseOrder, missionOrder, role, status, progress));
        }

        void set(Identifier missionId, TerminalMissionStatus status, float progress) {
            MutableHudMission mission = missions.get(missionId);
            if (mission != null) {
                mission.status = status;
                mission.progress = progress;
            }
        }

        @Override
        public TerminalMissionChapter chapter() {
            return new TerminalMissionChapter(chapterId, title, "HUD notice test provider", order, 0xFF66D9FF, true);
        }

        @Override
        public List<TerminalMissionDefinition> missions(Player player) {
            return missions.values().stream()
                    .map(mission -> new TerminalMissionDefinition(
                            mission.id,
                            chapterId,
                            mission.phaseTitle.toLowerCase(java.util.Locale.ROOT).replace(' ', '_'),
                            mission.phaseTitle,
                            mission.phaseOrder,
                            mission.missionOrder,
                            mission.title,
                            mission.title + " briefing",
                            mission.title + " guide",
                            "HUD Test",
                            "Test",
                            new ItemStack(Items.COMPASS),
                            List.of(),
                            List.of(),
                            List.of()))
                    .toList();
        }

        @Override
        public TerminalMissionSnapshot snapshot(Player player, Identifier missionId) {
            MutableHudMission mission = missions.get(missionId);
            return mission == null
                    ? new TerminalMissionSnapshot(missionId, TerminalMissionStatus.LOCKED, 0.0F,
                            "LOCKED", "Missing HUD test mission.", "Missing HUD test mission.", List.of())
                    : new TerminalMissionSnapshot(mission.id, mission.status, mission.progress,
                            mission.status.name(), "", mission.title + " next step",
                            mission.status == TerminalMissionStatus.CLAIMABLE
                                    ? List.of(TerminalMissionAction.enabled("claim_reward", "CLAIM CACHE"))
                                    : List.of());
        }

        @Override
        public TerminalMissionPresentation presentation(
                Player player, TerminalMissionDefinition definition, TerminalMissionSnapshot snapshot) {
            return new TerminalMissionPresentation(
                    definition.title(),
                    definition.briefing(),
                    snapshot.actionHint(),
                    definition.phaseTitle(),
                    snapshot.status().name().toLowerCase(java.util.Locale.ROOT),
                    List.of("HUD Test"),
                    "");
        }

        @Override
        public TerminalMissionRole role(Player player, TerminalMissionDefinition definition, TerminalMissionSnapshot snapshot) {
            MutableHudMission mission = missions.get(definition.id());
            return mission == null ? TerminalMissionRole.MAIN : mission.role;
        }
    }

    private static final class MutableHudMission {
        private final Identifier id;
        private final String title;
        private final String phaseTitle;
        private final int phaseOrder;
        private final int missionOrder;
        private final TerminalMissionRole role;
        private TerminalMissionStatus status;
        private float progress;

        private MutableHudMission(Identifier id, String title, String phaseTitle, int phaseOrder, int missionOrder,
                TerminalMissionRole role, TerminalMissionStatus status, float progress) {
            this.id = id;
            this.title = title;
            this.phaseTitle = phaseTitle;
            this.phaseOrder = phaseOrder;
            this.missionOrder = missionOrder;
            this.role = role;
            this.status = status;
            this.progress = progress;
        }
    }

    private record DummyMissionProvider(Identifier chapterId, int order, AtomicBoolean handled) implements TerminalMissionProvider {
        @Override
        public TerminalMissionChapter chapter() {
            return new TerminalMissionChapter(chapterId, chapterId.getPath(), "Test provider", order, 0xFF66D9FF, true);
        }

        @Override
        public List<TerminalMissionDefinition> missions(Player player) {
            return List.of(new TerminalMissionDefinition(
                    id("test_mission"),
                    chapterId,
                    "test",
                    "Test",
                    0,
                    1,
                    "Test Mission",
                    "Test briefing",
                    "Test field guide",
                    "Test",
                    "Test",
                    ItemStack.EMPTY,
                    List.of(),
                    List.of(),
                    List.of()));
        }

        @Override
        public TerminalMissionSnapshot snapshot(Player player, Identifier missionId) {
            return new TerminalMissionSnapshot(
                    missionId,
                    TerminalMissionStatus.UNLOCKED,
                    0.0F,
                    "UNLOCKED",
                    "",
                    "Test",
                    List.of(TerminalMissionAction.enabled("claim_reward", "CLAIM")));
        }

        @Override
        public boolean handleAction(ServerPlayer player, Identifier missionId, String actionId) {
            handled.set(true);
            return true;
        }
    }

    private record EmptyMissionProvider(Identifier chapterId, int order) implements TerminalMissionProvider {
        @Override
        public TerminalMissionChapter chapter() {
            return new TerminalMissionChapter(chapterId, chapterId.getPath(), "Empty provider", order, 0xFF66D9FF, true);
        }

        @Override
        public List<TerminalMissionDefinition> missions(Player player) {
            return List.of();
        }

        @Override
        public TerminalMissionSnapshot snapshot(Player player, Identifier missionId) {
            return new TerminalMissionSnapshot(
                    missionId,
                    TerminalMissionStatus.LOCKED,
                    0.0F,
                    "LOCKED",
                    "No mission records registered.",
                    "Install or enable a chapter provider.",
                    List.of());
        }
    }

    private record ConfiguredMission(
            Identifier id,
            String title,
            String phase,
            String category,
            String difficulty,
            TerminalMissionRole role,
            TerminalMissionStatus status,
            List<TerminalMissionAction> actions,
            Optional<Identifier> routeAnchor,
            List<Identifier> routePrerequisites,
            List<TerminalMissionIntelUnlock> intelUnlocks,
            List<TerminalMissionRequirement> requirements,
            List<TerminalMissionReward> rewards) {
        private ConfiguredMission(
                Identifier id,
                String title,
                String phase,
                String category,
                String difficulty,
                TerminalMissionRole role,
                TerminalMissionStatus status,
            List<TerminalMissionAction> actions) {
            this(id, title, phase, category, difficulty, role, status, actions,
                    Optional.empty(), List.of(), List.of(), List.of(), List.of());
        }

        private ConfiguredMission(
                Identifier id,
                String title,
                String phase,
                String category,
                String difficulty,
                TerminalMissionRole role,
                TerminalMissionStatus status,
                List<TerminalMissionAction> actions,
                List<TerminalMissionReward> rewards) {
            this(id, title, phase, category, difficulty, role, status, actions,
                    Optional.empty(), List.of(), List.of(), List.of(), rewards);
        }

        private ConfiguredMission(
                Identifier id,
                String title,
                String phase,
                String category,
                String difficulty,
                TerminalMissionRole role,
                TerminalMissionStatus status,
                List<TerminalMissionAction> actions,
                List<TerminalMissionRequirement> requirements,
                List<TerminalMissionReward> rewards) {
            this(id, title, phase, category, difficulty, role, status, actions,
                    Optional.empty(), List.of(), List.of(), requirements, rewards);
        }

        private ConfiguredMission(
                Identifier id,
                String title,
                String phase,
                String category,
                String difficulty,
                TerminalMissionRole role,
                TerminalMissionStatus status,
                List<TerminalMissionAction> actions,
                Optional<Identifier> routeAnchor,
                List<TerminalMissionIntelUnlock> intelUnlocks) {
            this(id, title, phase, category, difficulty, role, status, actions,
                    routeAnchor, List.of(), intelUnlocks, List.of(), List.of());
        }

        private ConfiguredMission(
                Identifier id,
                String title,
                String phase,
                String category,
                String difficulty,
                TerminalMissionRole role,
                TerminalMissionStatus status,
                List<TerminalMissionAction> actions,
                Optional<Identifier> routeAnchor,
                List<Identifier> routePrerequisites,
                List<TerminalMissionIntelUnlock> intelUnlocks) {
            this(id, title, phase, category, difficulty, role, status, actions,
                    routeAnchor, routePrerequisites, intelUnlocks, List.of(), List.of());
        }

        private ConfiguredMission(
                Identifier id,
                String title,
                String phase,
                String category,
                String difficulty,
                TerminalMissionRole role,
                TerminalMissionStatus status,
                List<TerminalMissionAction> actions,
                Optional<Identifier> routeAnchor,
                List<Identifier> routePrerequisites,
                List<TerminalMissionIntelUnlock> intelUnlocks,
                List<TerminalMissionReward> rewards) {
            this(id, title, phase, category, difficulty, role, status, actions,
                    routeAnchor, routePrerequisites, intelUnlocks, List.of(), rewards);
        }

        private ConfiguredMission {
            routeAnchor = routeAnchor == null ? Optional.empty() : routeAnchor;
            routePrerequisites = List.copyOf(routePrerequisites == null ? List.of() : routePrerequisites);
            intelUnlocks = List.copyOf(intelUnlocks == null ? List.of() : intelUnlocks);
            requirements = List.copyOf(requirements == null ? List.of() : requirements);
            rewards = List.copyOf(rewards == null ? List.of() : rewards);
        }
    }

    private record ConfigurableMissionProvider(
            Identifier chapterId,
            String title,
            int order,
            List<ConfiguredMission> configuredMissions) implements TerminalMissionProvider {
        @Override
        public TerminalMissionChapter chapter() {
            return new TerminalMissionChapter(chapterId, title, "Configurable test provider", order, 0xFF66D9FF, true);
        }

        @Override
        public List<TerminalMissionDefinition> missions(Player player) {
            return configuredMissions.stream()
                    .map(mission -> new TerminalMissionDefinition(
                            mission.id(),
                            chapterId,
                            mission.phase().toLowerCase(java.util.Locale.ROOT).replace(' ', '_'),
                            mission.phase(),
                            0,
                            configuredMissions.indexOf(mission),
                            mission.title(),
                            mission.title() + " briefing",
                            mission.title() + " guide",
                            mission.category(),
                            mission.difficulty(),
                            ItemStack.EMPTY,
                            List.of(),
                            mission.requirements(),
                            mission.rewards()))
                    .toList();
        }

        @Override
        public TerminalMissionSnapshot snapshot(Player player, Identifier missionId) {
            ConfiguredMission mission = configuredMissions.stream()
                    .filter(candidate -> candidate.id().equals(missionId))
                    .findFirst()
                    .orElse(null);
            return mission == null
                    ? new TerminalMissionSnapshot(missionId, TerminalMissionStatus.LOCKED, 0.0F,
                            "LOCKED", "Missing test mission.", "Missing test mission.", List.of())
                    : new TerminalMissionSnapshot(mission.id(), mission.status(),
                            mission.status() == TerminalMissionStatus.CLAIMABLE ? 1.0F : 0.0F,
                            mission.status().name(), "", mission.title() + " next step", mission.actions());
        }

        @Override
        public TerminalMissionRole role(Player player, TerminalMissionDefinition definition, TerminalMissionSnapshot snapshot) {
            return configuredMissions.stream()
                    .filter(candidate -> candidate.id().equals(definition.id()))
                    .map(ConfiguredMission::role)
                    .findFirst()
                    .orElse(TerminalMissionRole.MAIN);
        }

        @Override
        public Optional<Identifier> routeAnchor(
                Player player,
                TerminalMissionDefinition definition,
                TerminalMissionSnapshot snapshot,
                TerminalMissionRole role) {
            return configuredMissions.stream()
                    .filter(candidate -> candidate.id().equals(definition.id()))
                    .findFirst()
                    .flatMap(ConfiguredMission::routeAnchor);
        }

        @Override
        public List<Identifier> routePrerequisites(
                Player player,
                TerminalMissionDefinition definition,
                TerminalMissionSnapshot snapshot,
                TerminalMissionRole role) {
            return configuredMissions.stream()
                    .filter(candidate -> candidate.id().equals(definition.id()))
                    .findFirst()
                    .map(ConfiguredMission::routePrerequisites)
                    .orElseGet(List::of);
        }

        @Override
        public List<TerminalMissionIntelUnlock> intelUnlocks(
                Player player,
                TerminalMissionDefinition definition,
                TerminalMissionSnapshot snapshot,
                TerminalMissionRole role) {
            return configuredMissions.stream()
                    .filter(candidate -> candidate.id().equals(definition.id()))
                    .findFirst()
                    .map(ConfiguredMission::intelUnlocks)
                    .orElseGet(List::of);
        }

        @Override
        public boolean handleAction(ServerPlayer player, Identifier missionId, String actionId) {
            return configuredMissions.stream()
                    .filter(candidate -> candidate.id().equals(missionId))
                    .flatMap(candidate -> candidate.actions().stream())
                    .anyMatch(action -> action.enabled() && action.id().equals(actionId));
        }
    }

    private static final class PlacedMissionProvider implements TerminalMissionProvider {
        private final ConfigurableMissionProvider delegate;
        private final Map<Identifier, TerminalMissionRoutePlacement> placements;

        private PlacedMissionProvider(
                Identifier chapterId,
                String title,
                int order,
                List<ConfiguredMission> configuredMissions,
                Map<Identifier, TerminalMissionRoutePlacement> placements) {
            this.delegate = new ConfigurableMissionProvider(chapterId, title, order, configuredMissions);
            this.placements = Map.copyOf(placements);
        }

        @Override
        public TerminalMissionChapter chapter() {
            return delegate.chapter();
        }

        @Override
        public List<TerminalMissionDefinition> missions(Player player) {
            return delegate.missions(player);
        }

        @Override
        public TerminalMissionSnapshot snapshot(Player player, Identifier missionId) {
            return delegate.snapshot(player, missionId);
        }

        @Override
        public TerminalMissionRole role(
                Player player, TerminalMissionDefinition definition, TerminalMissionSnapshot snapshot) {
            return delegate.role(player, definition, snapshot);
        }

        @Override
        public Optional<TerminalMissionRoutePlacement> routePlacement(
                Player player,
                TerminalMissionDefinition definition,
                TerminalMissionSnapshot snapshot,
                TerminalMissionRole role) {
            return definition == null ? Optional.empty() : Optional.ofNullable(placements.get(definition.id()));
        }

        @Override
        public boolean handleAction(ServerPlayer player, Identifier missionId, String actionId) {
            return delegate.handleAction(player, missionId, actionId);
        }
    }

    private static final class GatedConvoyRouteProvider implements TerminalMissionProvider {
        private static final Identifier CHAPTER =
                Identifier.fromNamespaceAndPath("echoconvoyprotocol", "convoy_protocol");
        private static final Identifier EXPEDITION_READINESS =
                Identifier.fromNamespaceAndPath("echoashfallprotocol", "expedition_readiness");
        private static final Identifier PREP_VEHICLE =
                Identifier.fromNamespaceAndPath("echoconvoyprotocol", "prep_vehicle");
        private TerminalMissionStatus gateStatus = TerminalMissionStatus.LOCKED;

        void gateStatus(TerminalMissionStatus gateStatus) {
            this.gateStatus = gateStatus;
        }

        @Override
        public TerminalMissionChapter chapter() {
            return new TerminalMissionChapter(CHAPTER, "Convoy Protocol", "Gated convoy route test", 4,
                    0xFF92D66B, true);
        }

        @Override
        public List<TerminalMissionDefinition> missions(Player player) {
            return List.of(
                    definition(EXPEDITION_READINESS, "Expedition Readiness", "Recon", 7, 0),
                    definition(PREP_VEHICLE, "Build A Convoy Vehicle", "Convoy", 0, 10));
        }

        @Override
        public TerminalMissionSnapshot snapshot(Player player, Identifier missionId) {
            if (EXPEDITION_READINESS.equals(missionId)) {
                return new TerminalMissionSnapshot(missionId, gateStatus,
                        gateStatus == TerminalMissionStatus.CLAIMED ? 1.0F : 0.0F,
                        gateStatus.name(), "", "Complete expedition readiness.", List.of());
            }
            if (PREP_VEHICLE.equals(missionId)) {
                return new TerminalMissionSnapshot(missionId, TerminalMissionStatus.UNLOCKED, 0.0F,
                        "READY", "", "Build and scan a convoy vehicle.",
                        List.of(TerminalMissionAction.enabled("scan_convoy", "SCAN CONVOY")));
            }
            return new TerminalMissionSnapshot(missionId, TerminalMissionStatus.LOCKED, 0.0F,
                    "LOCKED", "Missing convoy test mission.", "Missing convoy test mission.", List.of());
        }

        @Override
        public TerminalMissionRole role(Player player, TerminalMissionDefinition definition, TerminalMissionSnapshot snapshot) {
            return PREP_VEHICLE.equals(definition.id()) ? TerminalMissionRole.OPTIONAL : TerminalMissionRole.MAIN;
        }

        @Override
        public Optional<TerminalMissionRoutePlacement> routePlacement(
                Player player,
                TerminalMissionDefinition definition,
                TerminalMissionSnapshot snapshot,
                TerminalMissionRole role) {
            if (definition == null) {
                return Optional.empty();
            }
            if (EXPEDITION_READINESS.equals(definition.id())) {
                return Optional.of(TerminalMissionRoutePlacement.hidden());
            }
            if (PREP_VEHICLE.equals(definition.id())) {
                return Optional.of(TerminalMissionRoutePlacement.optional(8, 10));
            }
            return Optional.empty();
        }

        @Override
        public List<Identifier> routePrerequisites(
                Player player,
                TerminalMissionDefinition definition,
                TerminalMissionSnapshot snapshot,
                TerminalMissionRole role) {
            return definition != null && PREP_VEHICLE.equals(definition.id())
                    ? List.of(EXPEDITION_READINESS)
                    : List.of();
        }

        @Override
        public boolean handleAction(ServerPlayer player, Identifier missionId, String actionId) {
            return PREP_VEHICLE.equals(missionId) && "scan_convoy".equals(actionId);
        }

        private static TerminalMissionDefinition definition(
                Identifier id, String title, String phaseTitle, int phaseOrder, int missionOrder) {
            return new TerminalMissionDefinition(
                    id,
                    CHAPTER,
                    phaseTitle.toLowerCase(java.util.Locale.ROOT).replace(' ', '_'),
                    phaseTitle,
                    phaseOrder,
                    missionOrder,
                    title,
                    title + " briefing",
                    title + " guide",
                    "Route",
                    "Test",
                    ItemStack.EMPTY,
                    List.of(),
                    List.of(),
                    List.of());
        }
    }

    private static final class CountingRouteMissionProvider implements TerminalMissionProvider {
        private final ConfigurableMissionProvider delegate;
        private final AtomicInteger missionCalls = new AtomicInteger();

        private CountingRouteMissionProvider(
                Identifier chapterId, String title, List<ConfiguredMission> configuredMissions) {
            this.delegate = new ConfigurableMissionProvider(chapterId, title, 1, configuredMissions);
        }

        @Override
        public TerminalMissionChapter chapter() {
            return delegate.chapter();
        }

        @Override
        public List<TerminalMissionDefinition> missions(Player player) {
            missionCalls.incrementAndGet();
            return delegate.missions(player);
        }

        @Override
        public TerminalMissionSnapshot snapshot(Player player, Identifier missionId) {
            return delegate.snapshot(player, missionId);
        }

        @Override
        public TerminalMissionRole role(
                Player player, TerminalMissionDefinition definition, TerminalMissionSnapshot snapshot) {
            return delegate.role(player, definition, snapshot);
        }

        @Override
        public Optional<Identifier> routeAnchor(
                Player player,
                TerminalMissionDefinition definition,
                TerminalMissionSnapshot snapshot,
                TerminalMissionRole role) {
            return delegate.routeAnchor(player, definition, snapshot, role);
        }

        @Override
        public List<Identifier> routePrerequisites(
                Player player,
                TerminalMissionDefinition definition,
                TerminalMissionSnapshot snapshot,
                TerminalMissionRole role) {
            return delegate.routePrerequisites(player, definition, snapshot, role);
        }

        @Override
        public List<TerminalMissionIntelUnlock> intelUnlocks(
                Player player,
                TerminalMissionDefinition definition,
                TerminalMissionSnapshot snapshot,
                TerminalMissionRole role) {
            return delegate.intelUnlocks(player, definition, snapshot, role);
        }

        @Override
        public boolean handleAction(ServerPlayer player, Identifier missionId, String actionId) {
            return delegate.handleAction(player, missionId, actionId);
        }

        AtomicInteger missionCalls() {
            return missionCalls;
        }
    }

    private static final class NativeLateProgressMissionProvider implements TerminalMissionProvider {
        private final ConfigurableMissionProvider delegate;

        private NativeLateProgressMissionProvider(
                Identifier chapterId, String title, int order, List<ConfiguredMission> configuredMissions) {
            this.delegate = new ConfigurableMissionProvider(chapterId, title, order, configuredMissions);
        }

        @Override
        public TerminalMissionChapter chapter() {
            return delegate.chapter();
        }

        @Override
        public List<TerminalMissionDefinition> missions(Player player) {
            return player == null ? delegate.missions(null) : List.of();
        }

        @Override
        public TerminalMissionSnapshot snapshot(Player player, Identifier missionId) {
            return delegate.snapshot(player, missionId);
        }

        @Override
        public TerminalMissionRole role(
                Player player, TerminalMissionDefinition definition, TerminalMissionSnapshot snapshot) {
            return delegate.role(player, definition, snapshot);
        }

        @Override
        public Optional<TerminalMissionRoutePlacement> routePlacement(
                Player player,
                TerminalMissionDefinition definition,
                TerminalMissionSnapshot snapshot,
                TerminalMissionRole role) {
            return delegate.routePlacement(player, definition, snapshot, role);
        }

        @Override
        public Optional<Identifier> routeAnchor(
                Player player,
                TerminalMissionDefinition definition,
                TerminalMissionSnapshot snapshot,
                TerminalMissionRole role) {
            return delegate.routeAnchor(player, definition, snapshot, role);
        }

        @Override
        public List<Identifier> routePrerequisites(
                Player player,
                TerminalMissionDefinition definition,
                TerminalMissionSnapshot snapshot,
                TerminalMissionRole role) {
            return delegate.routePrerequisites(player, definition, snapshot, role);
        }

        @Override
        public List<TerminalMissionIntelUnlock> intelUnlocks(
                Player player,
                TerminalMissionDefinition definition,
                TerminalMissionSnapshot snapshot,
                TerminalMissionRole role) {
            return delegate.intelUnlocks(player, definition, snapshot, role);
        }

        @Override
        public boolean handleAction(ServerPlayer player, Identifier missionId, String actionId) {
            return delegate.handleAction(player, missionId, actionId);
        }
    }

    private record ThrowingMissionsProvider(Identifier chapterId, int order) implements TerminalMissionProvider {
        @Override
        public TerminalMissionChapter chapter() {
            return new TerminalMissionChapter(chapterId, "Throwing Missions", "Throws during missions", order,
                    0xFF66D9FF, true);
        }

        @Override
        public List<TerminalMissionDefinition> missions(Player player) {
            throw new IllegalStateException("test terminal mission list failure");
        }

        @Override
        public TerminalMissionSnapshot snapshot(Player player, Identifier missionId) {
            return new TerminalMissionSnapshot(missionId, TerminalMissionStatus.LOCKED, 0.0F,
                    "LOCKED", "Provider failed.", "Retry later.", List.of());
        }
    }

    private record ThrowingChapterProvider() implements TerminalMissionProvider {
        @Override
        public TerminalMissionChapter chapter() {
            throw new IllegalStateException("test terminal mission chapter failure");
        }

        @Override
        public List<TerminalMissionDefinition> missions(Player player) {
            return List.of();
        }

        @Override
        public TerminalMissionSnapshot snapshot(Player player, Identifier missionId) {
            return new TerminalMissionSnapshot(
                    missionId,
                    TerminalMissionStatus.LOCKED,
                    0.0F,
                    "LOCKED",
                    "Provider failed.",
                    "Retry later.",
                    List.of());
        }
    }

    private static Properties loadProperties(Path path) {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(path)) {
            properties.load(input);
        } catch (IOException exception) {
            throw new AssertionError("Failed to load properties from " + path, exception);
        }
        return properties;
    }

    private static void deleteRecursively(Path root) {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        } catch (IOException exception) {
            EchoTerminal.LOGGER.warn("Failed to clean up temporary terminal options test directory: {}", root, exception);
        }
    }

    private static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment,
            String testName, Identifier functionId) {
        TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(
                environment,
                Identifier.withDefaultNamespace("empty"),
                100,
                0,
                true,
                net.minecraft.world.level.block.Rotation.NONE,
                false,
                1,
                1,
                false,
                2);
        event.registerTest(id(testName), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, functionId), data));
    }

    private static boolean shouldRegisterTests() {
        String namespaces = System.getProperty("neoforge.enabledGameTestNamespaces", "");
        if (namespaces == null || namespaces.isBlank()) {
            return true;
        }
        for (String namespace : namespaces.split(",")) {
            String normalized = namespace.trim();
            if (normalized.equals(EchoTerminal.MODID) || normalized.equals("*") || normalized.equalsIgnoreCase("all")) {
                return true;
            }
        }
        return false;
    }

    private static Path euiSourceRoot() {
        Path addonRunRelative = Path.of("..", "src", "main", "resources", "assets", EchoTerminal.MODID, "eui")
                .normalize();
        if (Files.exists(addonRunRelative)) {
            return addonRunRelative;
        }
        return Path.of("addons", "echoterminal", "src", "main", "resources", "assets", EchoTerminal.MODID, "eui");
    }

    private static boolean screenCoreLoaded() {
        return ModList.get().isLoaded("echoscreencore");
    }

    private static Class<?> screenCoreDataContextClass() throws ClassNotFoundException {
        return Class.forName("com.knoxhack.echoscreencore.api.EchoDataContext");
    }

    private static Class<?> screenCoreActionContextClass() throws ClassNotFoundException {
        return Class.forName("com.knoxhack.echoscreencore.api.action.EchoActionContext");
    }

    private static Class<?> screenCoreActionRegistryClass() throws ClassNotFoundException {
        return Class.forName("com.knoxhack.echoscreencore.api.action.EchoActionRegistry");
    }

    private static Class<?> screenCoreActionClass() throws ClassNotFoundException {
        return Class.forName("com.knoxhack.echoscreencore.api.action.EchoAction");
    }

    private static Class<?> screenCoreControlsClass() throws ClassNotFoundException {
        return Class.forName("com.knoxhack.echoscreencore.api.action.EchoActionContext$ScreenControls");
    }

    private static Class<?> screenCoreActionIdsClass() throws ClassNotFoundException {
        return Class.forName("com.knoxhack.echoterminal.client.screencore.TerminalScreenCoreActionIds");
    }

    private static Class<?> screenCoreActionsClass() throws ClassNotFoundException {
        return Class.forName("com.knoxhack.echoterminal.client.screencore.TerminalScreenCoreActions");
    }

    private static Class<?> screenCoreDataProvidersClass() throws ClassNotFoundException {
        return Class.forName("com.knoxhack.echoterminal.client.screencore.TerminalScreenCoreDataProviders");
    }

    @SuppressWarnings("unchecked")
    private static Set<String> screenCoreActionCatalog() throws ReflectiveOperationException {
        return (Set<String>) screenCoreActionIdsClass()
                .getMethod("registeredActionIdSet")
                .invoke(null);
    }

    private static Map<?, ?> screenCoreRegisteredActions() throws ReflectiveOperationException {
        return (Map<?, ?>) screenCoreActionRegistryClass()
                .getMethod("actions")
                .invoke(null);
    }

    private static String screenCoreActionId(String fieldName) throws ReflectiveOperationException {
        return (String) screenCoreActionIdsClass().getField(fieldName).get(null);
    }

    private static Object newScreenCoreDataContext() throws ReflectiveOperationException {
        return screenCoreDataContextClass().getMethod("empty").invoke(null);
    }

    private static Object screenCoreMissingPlaceholder(Object dataContext, String placeholder)
            throws ReflectiveOperationException {
        return screenCoreDataContextClass()
                .getMethod("missingPlaceholder", String.class)
                .invoke(dataContext, placeholder);
    }

    private static Object putScreenCoreData(Object dataContext, String path, Object value)
            throws ReflectiveOperationException {
        return screenCoreDataContextClass()
                .getMethod("put", String.class, Object.class)
                .invoke(dataContext, path, value);
    }

    private static String resolveMissionActionIdForTests(
            TerminalMissionProvider provider,
            Identifier missionId,
            String actionId) throws ReflectiveOperationException {
        return (String) screenCoreActionsClass()
                .getMethod("resolveMissionActionIdForTests", TerminalMissionProvider.class, Identifier.class, String.class)
                .invoke(null, provider, missionId, actionId);
    }

    private static String preferredScreenCoreMissionActionIdForTests(List<TerminalMissionAction> actions)
            throws ReflectiveOperationException {
        return (String) screenCoreDataProvidersClass()
                .getMethod("preferredMissionActionIdForTests", List.class)
                .invoke(null, actions);
    }

    private static Identifier providerRouteTargetForTests(String providerId) throws ReflectiveOperationException {
        return (Identifier) screenCoreActionsClass()
                .getMethod("providerRouteTargetForTests", String.class)
                .invoke(null, providerId);
    }

    private static Identifier missionActionDispatchTabForTests(Identifier activeTabId, TerminalMissionProvider provider)
            throws ReflectiveOperationException {
        return (Identifier) screenCoreActionsClass()
                .getMethod("missionActionDispatchTabForTests", Identifier.class, TerminalMissionProvider.class)
                .invoke(null, activeTabId, provider);
    }

    private static Object resolveScreenCoreData(Object dataContext, String path) throws ReflectiveOperationException {
        return screenCoreDataProvidersClass()
                .getMethod("resolveForTests", screenCoreDataContextClass(), String.class)
                .invoke(null, dataContext, path);
    }

    private static long recipeUiBuildCountForTests() throws ReflectiveOperationException {
        return ((Number) screenCoreDataProvidersClass()
                .getMethod("recipeUiBuildCountForTests")
                .invoke(null)).longValue();
    }

    private static long missionBrowserUiBuildCountForTests() throws ReflectiveOperationException {
        return ((Number) screenCoreDataProvidersClass()
                .getMethod("missionBrowserUiBuildCountForTests")
                .invoke(null)).longValue();
    }

    private static boolean runScreenCoreAction(
            String action,
            String value,
            Map<String, String> params,
            Object dataContext,
            Object controls) throws ReflectiveOperationException {
        Class<?> dataContextClass = screenCoreDataContextClass();
        Class<?> actionContextClass = screenCoreActionContextClass();
        Object context = actionContextClass.getConstructor(
                        Identifier.class,
                        String.class,
                        dataContextClass,
                        dataContextClass,
                        String.class,
                        String.class,
                        String.class,
                        Map.class,
                        String.class,
                        screenCoreControlsClass())
                .newInstance(
                id("terminal_test"),
                "",
                dataContext == null ? newScreenCoreDataContext() : dataContext,
                null,
                action,
                value,
                value,
                params == null ? Map.of() : params,
                "",
                controls);
        Optional<?> handler = (Optional<?>) screenCoreActionRegistryClass()
                .getMethod("action", String.class)
                .invoke(null, action);
        if (handler.isEmpty()) {
            return false;
        }
        return Boolean.TRUE.equals(screenCoreActionClass()
                .getMethod("run", actionContextClass)
                .invoke(handler.get(), context));
    }

    private static Object screenCoreControls(Identifier[] openedPage) throws ClassNotFoundException {
        Class<?> controlsClass = screenCoreControlsClass();
        return java.lang.reflect.Proxy.newProxyInstance(
                controlsClass.getClassLoader(),
                new Class<?>[]{controlsClass},
                (proxy, method, args) -> {
                    if ("open".equals(method.getName()) && args != null && args.length > 0) {
                        openedPage[0] = (Identifier) args[0];
                        return true;
                    }
                    return false;
                });
    }

    private static Object screenCoreTerminalContext(Identifier activeTabId) throws ReflectiveOperationException {
        return Class.forName("com.knoxhack.echoterminal.client.screencore.TerminalScreenCoreBridge")
                .getMethod("screenContext", Identifier.class)
                .invoke(null, activeTabId);
    }

    @SuppressWarnings("unchecked")
    private static List<String> inspectScreenCoreTextNodes(
            Identifier pageId,
            Object dataContext,
            int width,
            int height) throws ReflectiveOperationException {
        return (List<String>) Class.forName("com.knoxhack.echoscreencore.client.engine.EchoScreenEngine")
                .getMethod("inspectTextNodesForTests", Identifier.class, screenCoreDataContextClass(), int.class, int.class)
                .invoke(null, pageId, dataContext, width, height);
    }

    private static Object clickScreenCoreActionForTests(
            Identifier pageId,
            Object dataContext,
            String action,
            int width,
            int height) throws ReflectiveOperationException {
        return Class.forName("com.knoxhack.echoscreencore.client.engine.EchoScreenEngine")
                .getMethod("clickActionForTests", Identifier.class, screenCoreDataContextClass(), String.class, int.class, int.class)
                .invoke(null, pageId, dataContext, action, width, height);
    }

    private static boolean screenCoreClickProbeBoolean(Object probe, String methodName)
            throws ReflectiveOperationException {
        return Boolean.TRUE.equals(probe.getClass().getMethod(methodName).invoke(probe));
    }

    private static String screenCoreClickProbeString(Object probe, String methodName)
            throws ReflectiveOperationException {
        Object value = probe.getClass().getMethod(methodName).invoke(probe);
        return value == null ? "" : String.valueOf(value);
    }

    private static String screenCoreClickProbeDiagnostics(Object probe)
            throws ReflectiveOperationException {
        Object value = probe.getClass().getMethod("diagnostics").invoke(probe);
        if (value instanceof List<?> lines) {
            return String.join(" | ", lines.stream().map(String::valueOf).toList());
        }
        return value == null ? "" : String.valueOf(value);
    }

    private static void assertNonblankTextNodesHaveBounds(GameTestHelper helper, List<String> lines, String label) {
        for (String line : lines) {
            if (!line.contains("value=") || line.contains("value=|")) {
                continue;
            }
            int bounds = line.indexOf("|bounds=");
            if (bounds < 0) {
                helper.assertTrue(false, "Missing text bounds in " + label + ": " + line);
                return;
            }
            String rawBounds = line.substring(bounds + "|bounds=".length());
            int next = rawBounds.indexOf('|');
            if (next >= 0) {
                rawBounds = rawBounds.substring(0, next);
            }
            int comma = rawBounds.lastIndexOf(',');
            int x = rawBounds.indexOf('x', comma + 1);
            int width = comma < 0 || x < 0 ? 0 : number(rawBounds.substring(comma + 1, x));
            int height = x < 0 ? 0 : number(rawBounds.substring(x + 1));
            helper.assertTrue(width > 0 && height >= 8,
                    "Nonblank " + label + " ScreenCore text should have drawable bounds: " + line);
            int alphaIndex = line.indexOf("|alpha=");
            int alphaEnd = alphaIndex < 0 ? -1 : line.indexOf('|', alphaIndex + 1);
            String alphaRaw = alphaIndex < 0 ? "0" : line.substring(alphaIndex + "|alpha=".length(),
                    alphaEnd < 0 ? line.length() : alphaEnd);
            helper.assertTrue(number(alphaRaw) > 0,
                    "Nonblank " + label + " ScreenCore text should resolve to visible alpha: " + line);
            helper.assertFalse(line.contains("|status=skipped_") || line.contains("|status=clipped"),
                    "Nonblank " + label + " ScreenCore text should not be skipped or clipped away: " + line);
        }
        helper.assertTrue(lines.stream().anyMatch(line -> line.contains("|drawCalled=true")),
                "At least one nonblank " + label + " ScreenCore text record should prove an actual draw call.");
    }

    private static boolean hasDrawnText(List<String> lines, String expected) {
        return lines.stream().anyMatch(line -> line.contains("value=" + expected)
                && line.contains("|drawCalled=true")
                && line.contains("|status=draw_called"));
    }

    private static boolean hasDrawnTextContaining(List<String> lines, String expected) {
        return lines.stream().anyMatch(line -> line.contains("value=")
                && line.contains(expected)
                && line.contains("|drawCalled=true")
                && line.contains("|status=draw_called"));
    }

    private static String nestedFailureMessage(Throwable exception) {
        Throwable cause = exception instanceof java.lang.reflect.InvocationTargetException
                ? ((java.lang.reflect.InvocationTargetException) exception).getCause()
                : exception;
        if (cause == null) {
            cause = exception;
        }
        String message = cause.getMessage();
        return cause.getClass().getSimpleName() + (message == null || message.isBlank() ? "" : ": " + message);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoTerminal.MODID, path);
    }

    private static int number(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private static String survivalRouteStageTitle(int phaseOrder) {
        return switch (phaseOrder) {
            case 0 -> "Podfall";
            case 1 -> "First Night";
            case 2 -> "Water Security";
            case 3 -> "Field Kit";
            case 4 -> "Powered Workshop";
            case 5 -> "Machine Tools";
            case 6 -> "Hazard Filters";
            case 7 -> "Recon";
            case 8 -> "Faction/Drone";
            case 9 -> "Biohazard Medicine";
            case 10 -> "Deep Extraction";
            case 11 -> "Grid Restoration";
            case 12 -> "Wasteland Bosses";
            case 13 -> "Cryogenic Route";
            case 14 -> "Nexus Decision";
            case 15 -> "Aftermath";
            default -> "";
        };
    }

    private static EchoDiscoveryEntry discoveryEntry(
            String path, EchoDiscoveryCategory category, String title, int sortOrder) {
        return new EchoDiscoveryEntry(
                id(path),
                id("test_chapter"),
                category,
                title,
                "Unknown Signal",
                "Find this signal in the field.",
                title + " summary.",
                null,
                null,
                0xFF66E8FF,
                null,
                sortOrder);
    }

    private static boolean recordDiscoveredForTest(Player player, Identifier id) {
        return EchoDiscoveryData.get(player).discover(id);
    }

    private static EchoDiscoveryState stateOf(List<EchoResolvedDiscoveryEntry> entries, EchoDiscoveryEntry target) {
        return entries.stream()
                .filter(entry -> entry.entry().id().equals(target.id()))
                .map(EchoResolvedDiscoveryEntry::state)
                .findFirst()
                .orElseThrow();
    }

    private static boolean classpathResourceExists(Identifier id) {
        if (id == null) {
            return false;
        }
        String path = "assets/" + id.getNamespace() + "/" + id.getPath();
        return ModGameTests.class.getClassLoader().getResource(path) != null;
    }

    private static boolean pngHasTransparentCorners(Identifier id) {
        if (id == null) {
            return false;
        }
        String path = "assets/" + id.getNamespace() + "/" + id.getPath();
        try (InputStream stream = ModGameTests.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                return false;
            }
            BufferedImage image = ImageIO.read(stream);
            if (image == null || !image.getColorModel().hasAlpha()) {
                return false;
            }
            return alphaAt(image, 0, 0) == 0
                    && alphaAt(image, image.getWidth() - 1, 0) == 0
                    && alphaAt(image, 0, image.getHeight() - 1) == 0
                    && alphaAt(image, image.getWidth() - 1, image.getHeight() - 1) == 0;
        } catch (IOException exception) {
            return false;
        }
    }

    private static int alphaAt(BufferedImage image, int x, int y) {
        return image.getRGB(x, y) >>> 24;
    }

    private static List<ConfiguredMission> generatedMissions(int count) {
        List<ConfiguredMission> missions = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            missions.add(new ConfiguredMission(
                    Identifier.fromNamespaceAndPath("echoindustrialnexus", "mission/generated_" + i),
                    "Generated " + i,
                    i < MainSurvivalQuestProvider.maxRouteRecordsForTests() ? "Stage 1" : "Stage 2",
                    "Factory",
                    "Production",
                    TerminalMissionRole.MAIN,
                    TerminalMissionStatus.UNLOCKED,
                    List.of()));
        }
        return List.copyOf(missions);
    }

    private static List<ItemStack> fullInboxStacks(int count) {
        List<net.minecraft.world.item.Item> items = List.of(
                Items.COBBLESTONE,
                Items.DIRT,
                Items.OAK_LOG,
                Items.SPRUCE_LOG,
                Items.BIRCH_LOG,
                Items.JUNGLE_LOG,
                Items.ACACIA_LOG,
                Items.DARK_OAK_LOG,
                Items.MANGROVE_LOG,
                Items.CHERRY_LOG,
                Items.SAND,
                Items.GRAVEL,
                Items.COAL,
                Items.RAW_IRON,
                Items.RAW_COPPER,
                Items.RAW_GOLD,
                Items.REDSTONE,
                Items.LAPIS_LAZULI,
                Items.EMERALD,
                Items.DIAMOND,
                Items.QUARTZ,
                Items.NETHERRACK,
                Items.BASALT,
                Items.BLACKSTONE,
                Items.END_STONE,
                Items.CLAY_BALL,
                Items.FLINT);
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            net.minecraft.world.item.Item item = items.get(i);
            stacks.add(new ItemStack(item, item.getDefaultMaxStackSize()));
        }
        return stacks;
    }
}
