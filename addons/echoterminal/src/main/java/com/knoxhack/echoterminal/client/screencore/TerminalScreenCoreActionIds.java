package com.knoxhack.echoterminal.client.screencore;

import java.util.List;
import java.util.Set;

/**
 * Internal catalog for built-in Terminal ScreenCore actions. Literal
 * terminal.* actions in bundled EUI resources should resolve here.
 */
public final class TerminalScreenCoreActionIds {
    public static final String OPEN_TAB = "terminal.open_tab";
    public static final String OPEN_RELATED_TAB = "terminal.open_related_tab";
    public static final String OPEN_MISSION = "terminal.open_mission";
    public static final String OPEN_REWARDS = "terminal.open_rewards";
    public static final String OPEN_VITALS = "terminal.open_vitals";
    public static final String OPEN_DIAGNOSTICS = "terminal.open_diagnostics";
    public static final String OPEN_ADDONS = "terminal.open_addons";
    public static final String OPEN_HAZARD_MAP = "terminal.open_hazard_map";
    public static final String PING_SCAN = "terminal.ping_scan";
    public static final String DEPLOY_PROBE = "terminal.deploy_probe";
    public static final String CLOSE = "terminal.close";
    public static final String THEME_CHANGED = "terminal.theme_changed";
    public static final String REWARD_CLAIM_ALL = "terminal.rewardInbox.claim_all";
    public static final String REWARD_CLAIM_REWARD = "terminal.rewardInbox.claim_reward";
    public static final String REWARD_REFRESH = "terminal.rewardInbox.refresh";
    public static final String REWARD_DEFER = "terminal.rewardInbox.defer";
    public static final String REWARD_MARK_VIEWED = "terminal.rewardInbox.mark_viewed";
    public static final String ARCHIVES_MARK_READ = "terminal.archives.mark_read";
    public static final String REFRESH_DIAGNOSTICS = "terminal.refresh_diagnostics";
    public static final String SELECT_MISSION = "terminal.select_mission";
    public static final String FOCUS_ACTIVE_MISSION = "terminal.focus_active_mission";
    public static final String TRACK_MISSION = "terminal.track_mission";
    public static final String UNTRACK_MISSION = "terminal.untrack_mission";
    public static final String ACTIVATE_SELECTED_MISSION = "terminal.activate_selected_mission";
    public static final String CLAIM_REWARD = "terminal.claim_reward";
    public static final String PERFORM_MISSION_ACTION = "terminal.perform_mission_action";
    public static final String FILTER_MISSIONS = "terminal.filter_missions";
    public static final String SORT_MISSIONS = "terminal.sort_missions";
    public static final String SELECT_MISSION_PROVIDER = "terminal.select_mission_provider";
    public static final String OPEN_PROVIDER_ROUTE = "terminal.open_provider_route";
    public static final String OPEN_PROVIDER_DIAGNOSTICS = "terminal.open_provider_diagnostics";
    public static final String RECIPE_SELECT_ITEM = "terminal.recipeIndex.select_item";
    public static final String RECIPE_SELECT_RECIPE = "terminal.recipeIndex.select_recipe";
    public static final String RECIPE_SET_MODE = "terminal.recipeIndex.set_mode";
    public static final String RECIPE_SET_SOURCE_FILTER = "terminal.recipeIndex.set_source_filter";
    public static final String RECIPE_TOGGLE_CATEGORY = "terminal.recipeIndex.toggle_category";
    public static final String RECIPE_SEARCH_CHANGED = "terminal.recipeIndex.search_changed";
    public static final String RECIPE_CLEAR_SEARCH = "terminal.recipeIndex.clear_search";
    public static final String RECIPE_SCROLL = "terminal.recipeIndex.scroll";
    public static final String RECIPE_OPEN_INFO = "terminal.recipeIndex.open_info";
    public static final String SELECT_ROUTE_RECORD = "terminal.select_route_record";
    public static final String FILTER_ROUTE_RECORDS = "terminal.filter_route_records";
    public static final String DISCOVERY_FILTER_CATEGORY = "terminal.discoveryGrid.filter_category";
    public static final String DISCOVERY_FILTER_STATE = "terminal.discoveryGrid.filter_state";
    public static final String DISCOVERY_SELECT_CARD = "terminal.discoveryGrid.select_card";
    public static final String DISCOVERY_MARK_CHECKED = "terminal.discoveryGrid.mark_checked";
    public static final String FACTIONS_FILTER_NAMESPACE = "terminal.factions.filter_namespace";
    public static final String FACTIONS_SELECT_FACTION = "terminal.factions.select_faction";
    public static final String FACTIONS_OPEN_CONTRACT = "terminal.factions.open_contract";
    public static final String FACTIONS_OPEN_ROUTE = "terminal.factions.open_route";
    public static final String ARCHIVES_FILTER_STATE = "terminal.archives.filter_state";
    public static final String ARCHIVES_FILTER_GROUP = "terminal.archives.filter_group";
    public static final String ARCHIVES_SELECT_RECORD = "terminal.archives.select_record";
    public static final String VITALS_REFRESH = "terminal.vitals.refresh";
    public static final String VITALS_OPEN_DETAIL = "terminal.vitals.open_detail";
    public static final String DATA_CORE_REFRESH = "terminal.dataCore.refresh";
    public static final String DATA_CORE_TOGGLE_RAW_DEBUG = "terminal.dataCore.toggle_raw_debug";
    public static final String SELECT_ADDON = "terminal.select_addon";
    public static final String OPEN_ADDON_LINK = "terminal.open_addon_link";
    public static final String OPEN_ADDON_ROUTE = "terminal.open_addon_route";
    public static final String OPEN_ADDON_ARCHIVES = "terminal.open_addon_archives";
    public static final String OPEN_ADDON_DIAGNOSTICS = "terminal.open_addon_diagnostics";
    public static final String OPEN_ADDON_CONFIG = "terminal.open_addon_config";
    public static final String SETTINGS_TOGGLE_DEBUG_VISIBILITY = "terminal.settings.toggle_debug_visibility";
    public static final String SETTINGS_TOGGLE_MISSION_HUD_NOTICES = "terminal.settings.toggle_mission_hud_notices";
    public static final String SETTINGS_TOGGLE_SCREENCORE_DEBUG = "terminal.settings.toggle_screencore_debug";
    public static final String SETTINGS_TOGGLE_SCREENCORE_EXPERIMENTAL = "terminal.settings.toggle_screencore_experimental";
    public static final String SETTINGS_SET_DENSITY = "terminal.settings.set_density";
    public static final String SETTINGS_SET_ZOOM = "terminal.settings.set_zoom";
    public static final String SETTINGS_TOGGLE_VISUAL_TREATMENT = "terminal.settings.toggle_visual_treatment";
    public static final String SETTINGS_TOGGLE_READABILITY_OPTION = "terminal.settings.toggle_readability_option";
    public static final String SETTINGS_SET_NAVIGATION_STYLE = "terminal.settings.set_navigation_style";
    public static final String SETTINGS_RESET_DEFAULTS = "terminal.settings.reset_defaults";

    private static final List<String> REGISTERED_ACTIONS = List.of(
            OPEN_TAB,
            OPEN_RELATED_TAB,
            OPEN_MISSION,
            OPEN_REWARDS,
            OPEN_VITALS,
            OPEN_DIAGNOSTICS,
            OPEN_ADDONS,
            OPEN_HAZARD_MAP,
            PING_SCAN,
            DEPLOY_PROBE,
            CLOSE,
            THEME_CHANGED,
            REWARD_CLAIM_ALL,
            REWARD_CLAIM_REWARD,
            REWARD_REFRESH,
            REWARD_DEFER,
            REWARD_MARK_VIEWED,
            ARCHIVES_MARK_READ,
            REFRESH_DIAGNOSTICS,
            SELECT_MISSION,
            FOCUS_ACTIVE_MISSION,
            TRACK_MISSION,
            UNTRACK_MISSION,
            ACTIVATE_SELECTED_MISSION,
            CLAIM_REWARD,
            PERFORM_MISSION_ACTION,
            FILTER_MISSIONS,
            SORT_MISSIONS,
            SELECT_MISSION_PROVIDER,
            OPEN_PROVIDER_ROUTE,
            OPEN_PROVIDER_DIAGNOSTICS,
            RECIPE_SELECT_ITEM,
            RECIPE_SELECT_RECIPE,
            RECIPE_SET_MODE,
            RECIPE_SET_SOURCE_FILTER,
            RECIPE_TOGGLE_CATEGORY,
            RECIPE_SEARCH_CHANGED,
            RECIPE_CLEAR_SEARCH,
            RECIPE_SCROLL,
            RECIPE_OPEN_INFO,
            SELECT_ROUTE_RECORD,
            FILTER_ROUTE_RECORDS,
            DISCOVERY_FILTER_CATEGORY,
            DISCOVERY_FILTER_STATE,
            DISCOVERY_SELECT_CARD,
            DISCOVERY_MARK_CHECKED,
            FACTIONS_FILTER_NAMESPACE,
            FACTIONS_SELECT_FACTION,
            FACTIONS_OPEN_CONTRACT,
            FACTIONS_OPEN_ROUTE,
            ARCHIVES_FILTER_STATE,
            ARCHIVES_FILTER_GROUP,
            ARCHIVES_SELECT_RECORD,
            VITALS_REFRESH,
            VITALS_OPEN_DETAIL,
            DATA_CORE_REFRESH,
            DATA_CORE_TOGGLE_RAW_DEBUG,
            SELECT_ADDON,
            OPEN_ADDON_LINK,
            OPEN_ADDON_ROUTE,
            OPEN_ADDON_ARCHIVES,
            OPEN_ADDON_DIAGNOSTICS,
            OPEN_ADDON_CONFIG,
            SETTINGS_TOGGLE_DEBUG_VISIBILITY,
            SETTINGS_TOGGLE_MISSION_HUD_NOTICES,
            SETTINGS_TOGGLE_SCREENCORE_DEBUG,
            SETTINGS_TOGGLE_SCREENCORE_EXPERIMENTAL,
            SETTINGS_SET_DENSITY,
            SETTINGS_SET_ZOOM,
            SETTINGS_TOGGLE_VISUAL_TREATMENT,
            SETTINGS_TOGGLE_READABILITY_OPTION,
            SETTINGS_SET_NAVIGATION_STYLE,
            SETTINGS_RESET_DEFAULTS);
    private static final Set<String> REGISTERED_ACTION_SET = Set.copyOf(REGISTERED_ACTIONS);

    private TerminalScreenCoreActionIds() {
    }

    public static List<String> registeredActionIds() {
        return REGISTERED_ACTIONS;
    }

    public static Set<String> registeredActionIdSet() {
        return REGISTERED_ACTION_SET;
    }
}
