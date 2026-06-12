package com.knoxhack.echoterminal.client.screencore;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.knoxhack.echoscreencore.api.EchoDataContext;
import com.knoxhack.echoscreencore.api.EchoScreenRegistry;
import com.knoxhack.echoscreencore.api.EchoScreens;
import com.knoxhack.echoscreencore.api.action.EchoActionContext;
import com.knoxhack.echoscreencore.client.state.EchoPageStateStore;
import com.knoxhack.echonetcore.client.EchoNetClientActions;
import com.knoxhack.echoterminal.BuiltinTerminalCommonIntegration;
import com.knoxhack.echoterminal.EchoTerminal;
import com.knoxhack.echoterminal.EchoTerminalClient;
import com.knoxhack.echoterminal.api.TerminalNavigationProfile;
import com.knoxhack.echoterminal.api.TerminalNavigationProfiles;
import com.knoxhack.echoterminal.api.TerminalTab;
import com.knoxhack.echoterminal.api.mission.TerminalMissionAction;
import com.knoxhack.echoterminal.api.mission.TerminalMissionActions;
import com.knoxhack.echoterminal.api.mission.TerminalMissionDefinition;
import com.knoxhack.echoterminal.api.mission.TerminalMissionProvider;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRegistry;
import com.knoxhack.echoterminal.api.mission.TerminalMissionSnapshot;
import com.knoxhack.echoterminal.api.mission.TerminalMissionStatus;
import com.knoxhack.echoterminal.api.theme.TerminalThemeRegistry;
import com.knoxhack.echoterminal.client.screen.TerminalClientOptions;
import com.knoxhack.echoterminal.mission.MainSurvivalQuestProvider;
import com.knoxhack.echoterminal.mission.VanillaJourneyProvider;
import com.knoxhack.echoterminal.network.TerminalActionPacket;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public final class TerminalScreenCoreActions {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);

    private TerminalScreenCoreActions() {
    }

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }
        register("terminal.open_tab", TerminalScreenCoreActions::openTab);
        register("terminal.open_related_tab", TerminalScreenCoreActions::openTab);
        register("terminal.open_mission", TerminalScreenCoreActions::openMission);
        register("terminal.open_rewards", context -> openKnown(context, "reward_inbox"));
        register("terminal.open_vitals", context -> openKnown(context, "vitals"));
        register("terminal.open_diagnostics", context -> openKnown(context, "data_core"));
        register("terminal.open_addons", context -> openKnown(context, "addons"));
        register("terminal.open_hazard_map", context -> openKnown(context, "vitals"));
        register("terminal.ping_scan", TerminalScreenCoreActions::pingScan);
        register("terminal.deploy_probe", TerminalScreenCoreActions::deployProbe);
        register("terminal.close", EchoActionContext::close);
        register("terminal.open_legacy_renderer", context -> TerminalScreenCoreBridge.openLegacyRenderer());
        register("terminal.theme_changed", TerminalScreenCoreActions::themeChanged);
        register("terminal.rewardInbox.claim_all", TerminalScreenCoreActions::claimAllRewards);
        register("terminal.rewardInbox.claim_reward", TerminalScreenCoreActions::claimReward);
        register("terminal.rewardInbox.refresh", TerminalScreenCoreActions::refresh);
        register("terminal.rewardInbox.defer", TerminalScreenCoreActions::deferRewards);
        register("terminal.rewardInbox.mark_viewed", TerminalScreenCoreActions::markRewardsViewed);
        register("terminal.archives.mark_read", TerminalScreenCoreActions::markArchiveRead);
        register("terminal.refresh_diagnostics", TerminalScreenCoreActions::refresh);
        register("terminal.select_mission", TerminalScreenCoreActions::selectMission);
        register("terminal.focus_active_mission", TerminalScreenCoreActions::focusActiveMission);
        register("terminal.track_mission", TerminalScreenCoreActions::trackMission);
        register("terminal.untrack_mission", TerminalScreenCoreActions::untrackMission);
        register("terminal.activate_selected_mission", TerminalScreenCoreActions::activateSelectedMission);
        register("terminal.claim_reward", TerminalScreenCoreActions::performMissionAction);
        register("terminal.perform_mission_action", TerminalScreenCoreActions::performMissionAction);
        register("terminal.filter_missions", TerminalScreenCoreActions::filterMissions);
        register("terminal.sort_missions", TerminalScreenCoreActions::refresh);
        register("terminal.select_mission_provider", TerminalScreenCoreActions::selectMissionProvider);
        register("terminal.open_provider_route", TerminalScreenCoreActions::openProviderRoute);
        register("terminal.open_provider_diagnostics", TerminalScreenCoreActions::openProviderDiagnostics);
        register("terminal.recipeIndex.select_item", TerminalScreenCoreActions::selectRecipeItem);
        register("terminal.recipeIndex.select_recipe", TerminalScreenCoreActions::selectRecipe);
        register("terminal.recipeIndex.set_mode", TerminalScreenCoreActions::setRecipeMode);
        register("terminal.recipeIndex.set_source_filter", TerminalScreenCoreActions::setRecipeMode);
        register("terminal.recipeIndex.toggle_category", TerminalScreenCoreActions::toggleRecipeCategory);
        register("terminal.recipeIndex.search_changed", TerminalScreenCoreActions::recipeSearchChanged);
        register("terminal.recipeIndex.clear_search", TerminalScreenCoreActions::clearRecipeSearch);
        register("terminal.recipeIndex.scroll", TerminalScreenCoreActions::refresh);
        register("terminal.recipeIndex.open_info", TerminalScreenCoreActions::refresh);
        register("terminal.select_route_record", TerminalScreenCoreActions::selectRouteRecord);
        register("terminal.filter_route_records", TerminalScreenCoreActions::filterRouteRecords);
        register("terminal.discoveryGrid.filter_category", TerminalScreenCoreActions::filterDiscoveryCategory);
        register("terminal.discoveryGrid.filter_state", TerminalScreenCoreActions::filterDiscoveryState);
        register("terminal.discoveryGrid.select_card", TerminalScreenCoreActions::selectDiscovery);
        register("terminal.discoveryGrid.mark_checked", TerminalScreenCoreActions::selectDiscovery);
        register("terminal.factions.filter_namespace", TerminalScreenCoreActions::filterFactionNamespace);
        register("terminal.factions.select_faction", TerminalScreenCoreActions::selectFaction);
        register("terminal.factions.open_contract", TerminalScreenCoreActions::selectFaction);
        register("terminal.factions.open_route", TerminalScreenCoreActions::selectFaction);
        register("terminal.archives.filter_state", TerminalScreenCoreActions::filterArchiveState);
        register("terminal.archives.filter_group", TerminalScreenCoreActions::filterArchiveGroup);
        register("terminal.archives.select_record", TerminalScreenCoreActions::selectArchive);
        register("terminal.vitals.refresh", TerminalScreenCoreActions::refresh);
        register("terminal.vitals.open_detail", TerminalScreenCoreActions::refresh);
        register("terminal.dataCore.refresh", TerminalScreenCoreActions::refresh);
        register("terminal.dataCore.toggle_raw_debug", context -> toggle(context,
                TerminalClientOptions.hideDebugInfo(),
                TerminalClientOptions::setHideDebugInfo));
        register("terminal.select_addon", TerminalScreenCoreActions::selectAddon);
        register("terminal.open_addon_link", TerminalScreenCoreActions::openAddonRoute);
        register("terminal.open_addon_route", TerminalScreenCoreActions::openAddonRoute);
        register("terminal.open_addon_archives", TerminalScreenCoreActions::openAddonArchives);
        register("terminal.open_addon_diagnostics", TerminalScreenCoreActions::openAddonDiagnostics);
        register("terminal.open_addon_config", TerminalScreenCoreActions::openAddonConfig);
        register("terminal.settings.toggle_debug_visibility", context -> toggle(context,
                TerminalClientOptions.hideDebugInfo(),
                TerminalClientOptions::setHideDebugInfo));
        register("terminal.settings.toggle_mission_hud_notices", context -> toggle(context,
                TerminalClientOptions.missionHudNotifications,
                TerminalClientOptions::setMissionHudNotifications));
        register("terminal.settings.toggle_screencore_debug", context -> toggle(context,
                TerminalClientOptions.screenCoreDebug(),
                TerminalClientOptions::setScreenCoreDebug));
        register("terminal.settings.toggle_screencore_experimental", context -> toggle(context,
                TerminalClientOptions.screenCoreExperimentalTabs(),
                TerminalClientOptions::setScreenCoreExperimentalTabs));
        register("terminal.settings.set_density", TerminalScreenCoreActions::setDensity);
        register("terminal.settings.set_zoom", TerminalScreenCoreActions::setZoom);
        register("terminal.settings.toggle_visual_treatment", TerminalScreenCoreActions::toggleVisualTreatment);
        register("terminal.settings.toggle_readability_option", TerminalScreenCoreActions::toggleReadabilityOption);
        register("terminal.settings.set_navigation_style", TerminalScreenCoreActions::setNavigationStyle);
        register("terminal.settings.reset_defaults", TerminalScreenCoreActions::resetPresentationDefaults);
    }

    private static void register(String id, com.knoxhack.echoscreencore.api.action.EchoAction action) {
        if (!TerminalScreenCoreActionIds.registeredActionIdSet().contains(id)) {
            throw new IllegalArgumentException("Terminal ScreenCore action is missing from catalog: " + id);
        }
        EchoScreenRegistry.registerAction(id, context -> {
            if (EchoTerminalClient.nativeLoaderClientActiveForScreens()) {
                return EchoTerminalClient.dispatchNativeScreenCoreAction(id, context, action);
            }
            return action.run(context);
        });
    }

    private static boolean openKnown(EchoActionContext context, String path) {
        return openTab(context, Identifier.fromNamespaceAndPath(EchoTerminal.MODID, path));
    }

    private static boolean openTab(EchoActionContext context) {
        Identifier tabId = parseIdentifier(firstNonBlank(
                context.actionValue(),
                context.param("tab"),
                context.param("tab_id"),
                context.argument()));
        return openTab(context, tabId);
    }

    private static boolean openTab(EchoActionContext context, Identifier tabId) {
        if (tabId == null || TerminalScreenCoreBridge.tab(tabId).isEmpty()) {
            return false;
        }
        EchoDataContext next = TerminalScreenCoreBridge.screenContext(tabId);
        return context.controls() != null
                && context.controls().open(TerminalScreenCoreBridge.pageForTab(tabId), next);
    }

    private static boolean openMission(EchoActionContext context) {
        Identifier valueId = parseIdentifier(firstNonBlank(
                context.actionValue(),
                context.param("mission"),
                context.param("mission_id"),
                context.param("missionId"),
                context.argument()));
        if (valueId != null && TerminalScreenCoreBridge.tab(valueId).isPresent()) {
            return openTab(context, valueId);
        }
        if (valueId != null) {
            state().selectMission(valueId);
        }
        String providerId = firstNonBlank(
                context.param("provider"),
                context.param("provider_id"),
                context.param("providerId"),
                context.param("chapter"),
                context.param("chapter_id"),
                context.param("chapterId"));
        if (!providerId.isBlank() && providerKnown(providerId)) {
            state().selectMissionProvider(providerId);
            state().missionProviderFilter(providerFilterFor(providerId));
        }
        EchoScreens.invalidateData();
        Identifier targetTab = parseIdentifier(firstNonBlank(context.param("tab"), context.param("tab_id")));
        if (targetTab == null) {
            targetTab = MainSurvivalQuestProvider.TAB_ID;
        }
        resetMissionRouteScroll(context, targetTab);
        return openTab(context, targetTab);
    }

    private static boolean themeChanged(EchoActionContext context) {
        Identifier themeId = parseIdentifier(firstNonBlank(context.actionValue(), context.param("theme")));
        if (themeId == null || !TerminalThemeRegistry.contains(themeId)) {
            return false;
        }
        TerminalClientOptions.selectTheme(themeId);
        EchoScreens.invalidateData();
        return true;
    }

    private static boolean pingScan(EchoActionContext context) {
        state().discoveryCategory("all");
        state().discoveryState("discovered");
        EchoScreens.invalidateData();
        return openKnown(context, "discovery_grid");
    }

    private static boolean deployProbe(EchoActionContext context) {
        state().selectMissionProvider("all");
        state().missionProviderFilter("all");
        EchoScreens.invalidateData();
        return openKnown(context, "mission_graph");
    }

    private static boolean claimAllRewards(EchoActionContext context) {
        if (EchoCoreServices.pendingTerminalRewardCount(player()) <= 0) {
            return false;
        }
        EchoNetClientActions.sendServerboundAction(new TerminalActionPacket(
                BuiltinTerminalCommonIntegration.REWARD_INBOX,
                BuiltinTerminalCommonIntegration.CLAIM_REWARDS,
                ""));
        EchoScreens.invalidateData();
        return true;
    }

    private static boolean claimReward(EchoActionContext context) {
        String rewardId = value(context);
        if (!rewardId.isBlank()) {
            state().selectReward(rewardId);
        }
        state().rewardViewed(false);
        state().rewardDeferred(false);
        return claimAllRewards(context);
    }

    private static boolean deferRewards(EchoActionContext context) {
        String rewardId = value(context);
        if (!rewardId.isBlank()) {
            state().selectReward(rewardId);
        }
        state().rewardDeferred(true);
        EchoScreens.invalidateData();
        return true;
    }

    private static boolean markRewardsViewed(EchoActionContext context) {
        String rewardId = value(context);
        if (!rewardId.isBlank()) {
            state().selectReward(rewardId);
        }
        state().rewardViewed(true);
        EchoScreens.invalidateData();
        return openKnown(context, "overview");
    }

    private static boolean markArchiveRead(EchoActionContext context) {
        String recordId = firstNonBlank(context.actionValue(), context.param("record"));
        if (recordId.isBlank()) {
            return false;
        }
        EchoNetClientActions.sendServerboundAction(new TerminalActionPacket(
                BuiltinTerminalCommonIntegration.ARCHIVES,
                BuiltinTerminalCommonIntegration.MARK_ARCHIVE_READ,
                recordId));
        EchoScreens.invalidateData();
        return true;
    }

    private static boolean refresh(EchoActionContext context) {
        EchoScreens.invalidateData();
        return true;
    }

    private static boolean selectMission(EchoActionContext context) {
        Identifier id = parseIdentifier(value(context));
        if (id == null) {
            return false;
        }
        state().selectMission(id);
        resetMissionDetailScroll(context);
        EchoScreens.invalidateData();
        return true;
    }

    private static boolean focusActiveMission(EchoActionContext context) {
        Identifier tabId = activeTab(context);
        TerminalMissionProvider provider = missionProviderFor(tabId);
        if (provider == null) {
            return false;
        }
        try {
            Player player = player();
            List<TerminalMissionDefinition> missions = provider.missions(player);
            if (missions == null) {
                return false;
            }
            for (TerminalMissionDefinition mission : missions) {
                TerminalMissionSnapshot snapshot = provider.snapshot(player, mission.id());
                if (snapshot != null
                        && (snapshot.status() == TerminalMissionStatus.UNLOCKED
                                || snapshot.status() == TerminalMissionStatus.CLAIMABLE)) {
                    state().selectMission(mission.id());
                    resetMissionRouteScroll(context, tabId);
                    EchoScreens.invalidateData();
                    return true;
                }
            }
        } catch (RuntimeException ignored) {
            return false;
        }
        return false;
    }

    private static boolean trackMission(EchoActionContext context) {
        return sendMissionTracking(context, false);
    }

    private static boolean untrackMission(EchoActionContext context) {
        return sendMissionTracking(context, true);
    }

    private static boolean sendMissionTracking(EchoActionContext context, boolean clear) {
        Identifier tabId = activeTab(context);
        TerminalMissionProvider provider = missionProviderFor(tabId);
        Identifier missionId = missionIdFromActionValueOrSelection(context);
        if (provider == null || missionId == null) {
            return false;
        }
        Identifier dispatchTabId = missionActionDispatchTab(tabId, provider);
        boolean clearRequested = clear || Boolean.parseBoolean(firstNonBlank(context.param("clear"), context.param("untrack")));
        EchoNetClientActions.sendServerboundAction(new TerminalActionPacket(
                dispatchTabId,
                TerminalMissionActions.TRACK_MISSION,
                TerminalMissionActions.trackingPayload(dispatchTabId, safeChapterId(provider), missionId, clearRequested)));
        EchoScreens.invalidateData();
        return true;
    }

    private static boolean performMissionAction(EchoActionContext context) {
        Identifier tabId = activeTab(context);
        TerminalMissionProvider provider = missionProviderFor(tabId);
        Identifier missionId = missionIdFromActionValueOrSelection(context);
        if (provider == null || missionId == null) {
            return false;
        }
        TerminalMissionAction action = resolveMissionAction(provider, missionId,
                firstNonBlank(context.param("action_id"), context.param("actionId")));
        if (action == null) {
            return false;
        }
        return sendMissionAction(tabId, provider, missionId, action);
    }

    private static boolean activateSelectedMission(EchoActionContext context) {
        Identifier tabId = activeTab(context);
        TerminalMissionProvider provider = missionProviderFor(tabId);
        Identifier missionId = missionIdFromActionValueOrSelection(context);
        if (provider == null || missionId == null) {
            return openKnown(context, "mission_graph");
        }
        TerminalMissionSnapshot snapshot;
        try {
            snapshot = provider.snapshot(player(), missionId);
        } catch (RuntimeException exception) {
            snapshot = null;
        }
        TerminalMissionAction action = resolveMissionAction(provider, missionId,
                firstNonBlank(context.param("action_id"), context.param("actionId")));
        if (action != null) {
            return sendMissionAction(tabId, provider, missionId, action);
        }
        if (snapshot != null && (snapshot.status() == TerminalMissionStatus.COMPLETED
                || snapshot.status() == TerminalMissionStatus.CLAIMED)) {
            if (focusActiveMission(context)) {
                return true;
            }
            return openKnown(context, "mission_graph");
        }
        if (snapshot != null && snapshot.status() == TerminalMissionStatus.LOCKED) {
            resetMissionRouteScroll(context, tabId);
            resetMissionDetailScroll(context);
            EchoScreens.invalidateData();
            return true;
        }
        return openKnown(context, "mission_graph");
    }

    private static boolean sendMissionAction(
            Identifier activeTabId,
            TerminalMissionProvider provider,
            Identifier missionId,
            TerminalMissionAction action) {
        Identifier dispatchTabId = missionActionDispatchTab(activeTabId, provider);
        EchoNetClientActions.sendServerboundAction(new TerminalActionPacket(
                dispatchTabId,
                TerminalMissionActions.MISSION_ACTION,
                TerminalMissionActions.payload(safeChapterId(provider), missionId, action.id())));
        EchoScreens.invalidateData();
        return true;
    }

    private static boolean filterMissions(EchoActionContext context) {
        state().missionSearch(value(context));
        EchoScreens.invalidateData();
        return true;
    }

    private static boolean selectMissionProvider(EchoActionContext context) {
        String providerId = value(context);
        if (!providerKnown(providerId)) {
            return false;
        }
        state().selectMissionProvider(providerId);
        EchoScreens.invalidateData();
        return true;
    }

    private static boolean openProviderRoute(EchoActionContext context) {
        String providerId = value(context);
        if (!providerKnown(providerId)) {
            return false;
        }
        state().selectMissionProvider(providerId);
        state().missionProviderFilter(providerFilterFor(providerId));
        resetMissionRouteScroll(context, providerRouteTarget(providerId));
        EchoScreens.invalidateData();
        return openTab(context, providerRouteTarget(providerId));
    }

    private static boolean openProviderDiagnostics(EchoActionContext context) {
        String providerId = value(context);
        if (!providerKnown(providerId)) {
            return false;
        }
        state().selectMissionProvider(providerId);
        state().diagnosticsChapterFilter(providerFilterFor(providerId));
        EchoScreens.invalidateData();
        return openKnown(context, "data_core");
    }

    private static boolean selectRecipe(EchoActionContext context) {
        Identifier id = parseIdentifier(value(context));
        if (id == null) {
            return false;
        }
        state().selectRecipe(id);
        EchoScreens.invalidateData();
        return true;
    }

    private static boolean selectRecipeItem(EchoActionContext context) {
        String query = value(context);
        if (query.isBlank()) {
            return false;
        }
        state().recipeSearch(query);
        String requestedMode = firstNonBlank(context.param("mode"), context.param("recipe_mode"));
        if (TerminalScreenCoreUiState.isRecipeMode(requestedMode)) {
            state().recipeMode(requestedMode);
        } else if ("recipes".equals(state().recipeMode())) {
            state().recipeMode("uses");
        }
        EchoScreens.invalidateData();
        return true;
    }

    private static boolean setRecipeMode(EchoActionContext context) {
        String mode = value(context);
        if (!TerminalScreenCoreUiState.isRecipeMode(mode)) {
            return false;
        }
        state().recipeMode(mode);
        EchoScreens.invalidateData();
        return true;
    }

    private static boolean toggleRecipeCategory(EchoActionContext context) {
        String category = value(context);
        if (category.isBlank()) {
            return false;
        }
        state().recipeCategory(category);
        state().selectRecipe(null);
        EchoScreens.invalidateData();
        return true;
    }

    private static boolean recipeSearchChanged(EchoActionContext context) {
        state().recipeSearch(value(context));
        EchoScreens.invalidateData();
        return true;
    }

    private static boolean clearRecipeSearch(EchoActionContext context) {
        state().recipeSearch("");
        EchoScreens.invalidateData();
        return true;
    }

    private static boolean selectRouteRecord(EchoActionContext context) {
        Identifier id = parseIdentifier(value(context));
        if (id == null) {
            return false;
        }
        state().selectRouteRecord(id);
        EchoScreens.invalidateData();
        return true;
    }

    private static boolean filterRouteRecords(EchoActionContext context) {
        state().routeFilter(value(context));
        EchoScreens.invalidateData();
        return true;
    }

    private static boolean selectDiscovery(EchoActionContext context) {
        Identifier id = parseIdentifier(value(context));
        if (id == null) {
            return false;
        }
        state().selectDiscovery(id);
        EchoScreens.invalidateData();
        return true;
    }

    private static boolean filterDiscoveryCategory(EchoActionContext context) {
        state().discoveryCategory(value(context));
        EchoScreens.invalidateData();
        return true;
    }

    private static boolean filterDiscoveryState(EchoActionContext context) {
        state().discoveryState(value(context));
        EchoScreens.invalidateData();
        return true;
    }

    private static boolean selectFaction(EchoActionContext context) {
        Identifier id = parseIdentifier(value(context));
        if (id == null) {
            return false;
        }
        state().selectFaction(id);
        EchoScreens.invalidateData();
        return true;
    }

    private static boolean filterFactionNamespace(EchoActionContext context) {
        state().factionNamespace(value(context));
        EchoScreens.invalidateData();
        return true;
    }

    private static boolean selectArchive(EchoActionContext context) {
        Identifier id = parseIdentifier(value(context));
        if (id == null) {
            return false;
        }
        state().selectArchive(id);
        EchoScreens.invalidateData();
        return true;
    }

    private static boolean filterArchiveState(EchoActionContext context) {
        state().archiveState(value(context));
        EchoScreens.invalidateData();
        return true;
    }

    private static boolean filterArchiveGroup(EchoActionContext context) {
        state().archiveGroup(value(context));
        EchoScreens.invalidateData();
        return true;
    }

    private static boolean selectAddon(EchoActionContext context) {
        String id = value(context);
        if (id.isBlank()) {
            return false;
        }
        state().selectAddon(id);
        EchoScreens.invalidateData();
        return true;
    }

    private static boolean openAddonRoute(EchoActionContext context) {
        String addonId = selectedAddonValue(context);
        if (addonId.isBlank()) {
            return false;
        }
        state().selectAddon(addonId);
        if (providerKnown(addonId)) {
            state().selectMissionProvider(addonId);
            state().missionProviderFilter(providerFilterFor(addonId));
        } else {
            state().missionProviderFilter("all");
        }
        Identifier targetTab = addonRouteTarget(addonId);
        resetMissionRouteScroll(context, targetTab);
        EchoScreens.invalidateData();
        return openTab(context, targetTab);
    }

    private static boolean openAddonArchives(EchoActionContext context) {
        String addonId = selectedAddonValue(context);
        if (addonId.isBlank()) {
            return false;
        }
        state().selectAddon(addonId);
        state().archiveGroup(addonId);
        EchoScreens.invalidateData();
        return openKnown(context, "archives");
    }

    private static boolean openAddonDiagnostics(EchoActionContext context) {
        String addonId = selectedAddonValue(context);
        if (addonId.isBlank()) {
            return false;
        }
        state().selectAddon(addonId);
        state().diagnosticsChapterFilter(addonId);
        EchoScreens.invalidateData();
        return openKnown(context, "data_core");
    }

    private static boolean openAddonConfig(EchoActionContext context) {
        String addonId = selectedAddonValue(context);
        if (addonId.isBlank()) {
            return false;
        }
        state().selectAddon(addonId);
        state().diagnosticsChapterFilter(addonId);
        EchoScreens.invalidateData();
        return openKnown(context, "settings");
    }

    private static boolean setDensity(EchoActionContext context) {
        try {
            TerminalClientOptions.selectInterfaceDensity(TerminalClientOptions.InterfaceDensity.valueOf(value(context).toUpperCase(java.util.Locale.ROOT)));
            EchoScreens.invalidateData();
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static boolean setZoom(EchoActionContext context) {
        String raw = value(context).toUpperCase(java.util.Locale.ROOT);
        if (!raw.startsWith("ZOOM_")) {
            raw = "ZOOM_" + raw.replace("%", "");
        }
        try {
            TerminalClientOptions.selectTerminalZoom(TerminalClientOptions.TerminalZoom.valueOf(raw));
            EchoScreens.invalidateData();
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static boolean toggleVisualTreatment(EchoActionContext context) {
        String value = value(context);
        if ("minimal".equals(value)) {
            TerminalClientOptions.selectVisualLevel(TerminalClientOptions.VisualLevel.MINIMAL);
        } else if ("reduced_motion".equals(value)) {
            TerminalClientOptions.selectVisualLevel(TerminalClientOptions.VisualLevel.REDUCED_MOTION);
        } else {
            TerminalClientOptions.selectVisualLevel(TerminalClientOptions.VisualLevel.BALANCED);
        }
        EchoScreens.invalidateData();
        return true;
    }

    private static boolean toggleReadabilityOption(EchoActionContext context) {
        return switch (value(context)) {
            case "large_text" -> toggle(context, TerminalClientOptions.largeTextMode(), TerminalClientOptions::setLargeTextMode);
            case "high_contrast" -> toggle(context, TerminalClientOptions.highContrastMode(), TerminalClientOptions::setHighContrastMode);
            case "reduced_clutter" -> toggle(context, TerminalClientOptions.reducedClutterMode(), TerminalClientOptions::setReducedClutterMode);
            default -> false;
        };
    }

    private static boolean setNavigationStyle(EchoActionContext context) {
        try {
            TerminalClientOptions.selectNavigationStyle(TerminalClientOptions.NavigationStyle.valueOf(value(context).toUpperCase(java.util.Locale.ROOT)));
            EchoScreens.invalidateData();
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static boolean resetPresentationDefaults(EchoActionContext context) {
        TerminalClientOptions.selectInterfaceDensity(TerminalClientOptions.InterfaceDensity.COMFORTABLE);
        TerminalClientOptions.selectTerminalZoom(TerminalClientOptions.TerminalZoom.ZOOM_90);
        TerminalClientOptions.selectNavigationStyle(TerminalClientOptions.NavigationStyle.APP_HUB);
        TerminalClientOptions.selectVisualLevel(TerminalClientOptions.VisualLevel.BALANCED);
        TerminalClientOptions.setHighContrastMode(false);
        TerminalClientOptions.setLargeTextMode(false);
        TerminalClientOptions.setReducedClutterMode(false);
        EchoScreens.invalidateData();
        return true;
    }

    private static boolean toggle(EchoActionContext context, boolean current, java.util.function.Consumer<Boolean> setter) {
        setter.accept(!current);
        EchoScreens.invalidateData();
        return true;
    }

    private static TerminalScreenCoreUiState state() {
        return TerminalScreenCoreUiState.current();
    }

    private static String value(EchoActionContext context) {
        return firstNonBlank(context.actionValue(), context.param("value"), context.argument());
    }

    private static String selectedAddonValue(EchoActionContext context) {
        return firstNonBlank(value(context), state().selectedAddonId());
    }

    private static Identifier activeTab(EchoActionContext context) {
        String raw = context == null || context.dataContext() == null
                ? ""
                : context.dataContext().resolveToString("terminal.activeTabId");
        return TerminalScreenCoreBridge.normalizeTab(parseIdentifier(raw));
    }

    private static Identifier selectedMissionId(EchoActionContext context) {
        Identifier explicit = parseIdentifier(value(context));
        if (explicit != null) {
            state().selectMission(explicit);
            return explicit;
        }
        return state().selectedMissionId();
    }

    private static Identifier missionIdFromActionValueOrSelection(EchoActionContext context) {
        Identifier explicit = parseIdentifier(firstNonBlank(
                context.actionValue(),
                context.param("mission"),
                context.param("mission_id"),
                context.param("missionId"),
                context.param("value"),
                context.argument()));
        if (explicit != null) {
            state().selectMission(explicit);
            return explicit;
        }
        return state().selectedMissionId();
    }

    private static TerminalMissionProvider missionProviderFor(Identifier tabId) {
        if (VanillaJourneyProvider.TAB_ID.equals(tabId)) {
            return VanillaJourneyProvider.INSTANCE;
        }
        return MainSurvivalQuestProvider.INSTANCE;
    }

    private static Identifier missionActionDispatchTab(Identifier activeTabId, TerminalMissionProvider provider) {
        Identifier chapterId = safeChapterId(provider);
        if (VanillaJourneyProvider.TAB_ID.equals(activeTabId)
                || VanillaJourneyProvider.CHAPTER_ID.equals(chapterId)) {
            return VanillaJourneyProvider.TAB_ID;
        }
        return MainSurvivalQuestProvider.TAB_ID;
    }

    private static boolean providerKnown(String providerId) {
        Identifier id = parseIdentifier(providerId);
        if (id == null) {
            return false;
        }
        return TerminalMissionRegistry.provider(id).isPresent()
                || VanillaJourneyProvider.CHAPTER_ID.equals(id)
                || MainSurvivalQuestProvider.CHAPTER_ID.equals(id);
    }

    private static String providerFilterFor(String providerId) {
        Identifier id = parseIdentifier(providerId);
        return id == null || MainSurvivalQuestProvider.CHAPTER_ID.equals(id) ? "all" : id.toString();
    }

    private static Identifier providerRouteTarget(String providerId) {
        Identifier id = parseIdentifier(providerId);
        return VanillaJourneyProvider.CHAPTER_ID.equals(id)
                ? VanillaJourneyProvider.TAB_ID
                : MainSurvivalQuestProvider.TAB_ID;
    }

    private static Identifier addonRouteTarget(String addonId) {
        Identifier direct = parseIdentifier(addonId);
        if (direct != null && TerminalScreenCoreBridge.tab(direct).isPresent()) {
            return direct;
        }
        String normalized = normalize(addonId);
        if (!normalized.isBlank()) {
            for (TerminalTab tab : TerminalScreenCoreBridge.tabs()) {
                Identifier tabId = tab.descriptor().id();
                if (tabId.equals(Identifier.fromNamespaceAndPath(EchoTerminal.MODID, "addons"))) {
                    continue;
                }
                TerminalNavigationProfile profile = TerminalNavigationProfiles.profileFor(tab);
                if (normalized.equals(normalize(profile.chapterId()))
                        || normalized.equals(normalize(tabId.getNamespace()))
                        || normalize(tabId.toString()).contains(normalized)
                        || normalize(tab.descriptor().title()).contains(normalized)) {
                    return tabId;
                }
            }
        }
        return providerKnown(addonId) ? providerRouteTarget(addonId) : MainSurvivalQuestProvider.TAB_ID;
    }

    private static void resetMissionRouteScroll(EchoActionContext context, Identifier tabId) {
        Identifier targetTab = tabId == null ? MainSurvivalQuestProvider.TAB_ID : tabId;
        EchoPageStateStore.clear(TerminalScreenCoreBridge.pageForTab(targetTab));
        if (context == null || context.dataContext() == null) {
            return;
        }
        EchoPageStateStore.put(context.dataContext(), "terminal.missions", 0);
        EchoPageStateStore.put(context.dataContext(), "terminal.missions.roadmap", 0);
        EchoPageStateStore.put(context.dataContext(), "terminal.missions.detail", 0);
        EchoPageStateStore.put(context.dataContext(), "terminal.missions.requirements", 0);
        EchoPageStateStore.put(context.dataContext(), "terminal.missions.sideops", 0);
    }

    private static void resetMissionDetailScroll(EchoActionContext context) {
        if (context == null || context.dataContext() == null) {
            return;
        }
        EchoPageStateStore.put(context.dataContext(), "terminal.missions.detail", 0);
        EchoPageStateStore.put(context.dataContext(), "terminal.missions.requirements", 0);
        EchoPageStateStore.put(context.dataContext(), "terminal.missions.sideops", 0);
    }

    private static Identifier safeChapterId(TerminalMissionProvider provider) {
        try {
            return provider.chapter().id();
        } catch (RuntimeException exception) {
            return MainSurvivalQuestProvider.INSTANCE.chapter().id();
        }
    }

    private static TerminalMissionAction resolveMissionAction(
            TerminalMissionProvider provider,
            Identifier missionId,
            String requestedActionId) {
        try {
            Player player = player();
            List<TerminalMissionDefinition> missions = provider.missions(player);
            if (missions == null || missions.stream().noneMatch(mission -> mission.id().equals(missionId))) {
                return null;
            }
            TerminalMissionSnapshot snapshot = provider.snapshot(player, missionId);
            if (snapshot == null || snapshot.actions() == null) {
                return null;
            }
            if (!requestedActionId.isBlank()) {
                return snapshot.actions().stream()
                        .filter(action -> action.enabled() && requestedActionId.equals(action.id()))
                        .findFirst()
                        .orElse(null);
            }
            return preferredEnabledMissionAction(snapshot.actions());
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static TerminalMissionAction preferredEnabledMissionAction(List<TerminalMissionAction> actions) {
        TerminalMissionAction best = null;
        int bestPriority = Integer.MAX_VALUE;
        for (TerminalMissionAction action : actions == null ? List.<TerminalMissionAction>of() : actions) {
            if (!action.enabled()) {
                continue;
            }
            int priority = missionActionPriority(action);
            if (best == null || priority < bestPriority) {
                best = action;
                bestPriority = priority;
            }
        }
        return best;
    }

    private static int missionActionPriority(TerminalMissionAction action) {
        String signal = missionActionSignal(action);
        if (signal.contains("turnin") || signal.contains("complete")) {
            return 0;
        }
        if (signal.contains("claim")) {
            return 1;
        }
        return 10;
    }

    private static String missionActionSignal(TerminalMissionAction action) {
        if (action == null) {
            return "";
        }
        return normalize(action.id() + " " + action.label())
                .replaceAll("[^a-z0-9]+", "");
    }

    private static Player player() {
        try {
            return Minecraft.getInstance().player;
        } catch (RuntimeException | LinkageError ignored) {
            return null;
        }
    }

    public static String resolveMissionActionIdForTests(
            TerminalMissionProvider provider,
            Identifier missionId,
            String requestedActionId) {
        TerminalMissionAction action = resolveMissionAction(provider, missionId,
                requestedActionId == null ? "" : requestedActionId);
        return action == null ? "" : action.id();
    }

    public static Identifier providerRouteTargetForTests(String providerId) {
        return providerRouteTarget(providerId);
    }

    public static Identifier missionActionDispatchTabForTests(Identifier activeTabId, TerminalMissionProvider provider) {
        return missionActionDispatchTab(activeTabId, provider);
    }

    private static Identifier parseIdentifier(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return Identifier.tryParse(raw.strip());
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.strip();
            }
        }
        return "";
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip().toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_|_$", "");
    }
}
