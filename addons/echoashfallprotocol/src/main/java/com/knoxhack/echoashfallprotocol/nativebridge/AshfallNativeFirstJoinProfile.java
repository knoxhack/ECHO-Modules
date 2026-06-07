package com.knoxhack.echoashfallprotocol.nativebridge;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * JDK-only mirror of the native first-join crash recovery flow.
 *
 * <p>The native loader can consume this as deterministic gameplay data before
 * Minecraft-bound player, world, inventory, and advancement APIs are available.</p>
 */
public final class AshfallNativeFirstJoinProfile {
    private AshfallNativeFirstJoinProfile() {
    }

    public static Map<String, Object> describe(Map<String, String> context) {
        Set<String> loadedModules = loadedModules(context);

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id", "echoashfallprotocol:first_join_crash_recovery");
        profile.put("surface", "player_recovery");
        profile.put("nativeDataPath", "adaptercore.native_gameplay_profile");
        profile.put("sourceParity", "PlayerStartingKitHandler.onPlayerLoggedIn");
        profile.put("mirrorsNativeContract", true);
        profile.put("minecraftRuntimeAccessed", false);
        profile.put("registryMutated", false);
        profile.put("safeToEvaluateDuringNativeActivation", true);
        profile.put("loadedModuleHints", List.copyOf(loadedModules));
        profile.put("dropPodPlacement", dropPodPlacement());
        profile.put("playerStateWrites", playerStateWrites());
        profile.put("inventoryPlan", inventoryPlan(loadedModules));
        profile.put("recoveryPlan", recoveryPlan());
        profile.put("screenSafeSurfaces", screenSafeSurfaces());
        profile.put("atmosphereHooks", atmosphereHooks());
        Map<String, Object> transaction = AshfallNativeFirstJoinTransaction.describe(profile);
        profile.put("adapterCoreTransaction", transaction);
        Map<String, Object> execution = AshfallNativeFirstJoinExecution.execute(profile, context);
        Map<String, Object> bridgeDispatch = childMap(execution, "firstJoinBridgeDispatch");
        Map<String, Object> minecraftRuntimeAdapterContract = childMap(execution, "minecraftRuntimeAdapterContract");
        Map<String, Object> minecraftRuntimeHostCallQueue = childMap(execution, "minecraftRuntimeHostCallQueue");
        profile.put("adapterCoreExecution", execution);
        profile.put("liveAdapterCoreExecution", false);
        profile.put("nativePlayerRecoveryBridgesWired", false);
        profile.put("nativePlayerRecoveryBridgesPrepared", "PASS".equals(bridgeDispatch.get("status")));
        profile.put("minecraftRuntimeAdapterContractPrepared", "PASS".equals(minecraftRuntimeAdapterContract.get("status")));
        profile.put("minecraftRuntimeAdapterReadyInvocationCount",
                minecraftRuntimeAdapterContract.getOrDefault("readyInvocationCount", 0));
        profile.put("minecraftRuntimeHostCallQueuePrepared", "PREPARED_UNCONSUMED".equals(minecraftRuntimeHostCallQueue.get("status")));
        profile.put("minecraftRuntimeHostCallQueueConsumed", false);
        profile.put("minecraftRuntimeHostCallQueueCount",
                minecraftRuntimeHostCallQueue.getOrDefault("queuedHostCallCount", 0));
        profile.put("validationSignals", validationSignals());
        profile.put("remainingNativeBlockers", remainingNativeBlockers(minecraftRuntimeAdapterContract, bridgeDispatch));
        profile.put("summary",
                "Native activation carries the Ashfall first-join crash recovery plan and prepared host-call metadata; live execution is claimed only after runtime mutation evidence is recorded.");
        return profile;
    }

    private static Map<String, Object> dropPodPlacement() {
        Map<String, Object> placement = new LinkedHashMap<>();
        placement.put("structure", "echoashfallprotocol:drop_pod");
        placement.put("interiorAnchor", "personal_starting_drop_pod.interior");
        placement.put("serverSpawnReference", "level.respawnData.pos");
        placement.put("minStartingPodRadiusChunks", 2);
        placement.put("maxStartingPodRadiusChunks", 8);
        placement.put("minStartingPodSpacingChunks", 3);
        placement.put("chunkSize", 16);
        placement.put("candidateAttemptsPerBand", 16);
        placement.put("startingSurfaceSearchRadius", 24);
        placement.put("startingSurfaceFallbackSearchRadius", 96);
        placement.put("minimumSafeSurfaceAboveBottom", 16);
        placement.put("minimumStartingSurfaceY", 48);
        placement.put("requiresSkyVisible", true);
        placement.put("requiresTwoAirBlocks", true);
        placement.put("requiresPlacementSupport", true);
        placement.put("fallbackBehavior", List.of(
                "try_random_surface_between_2_and_8_chunks",
                "try_outer_radius_band",
                "try_nearest_safe_surface",
                "cap_last_resort_y_without_mutating_native_world"
        ));
        return placement;
    }

    private static Map<String, Object> playerStateWrites() {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("firstJoinFlag", "ashes_of_tomorrow.received_kit");
        state.put("firstJoinFlagValue", true);
        state.put("questDropPodInitialized", true);
        state.put("respawnBinding", "drop_pod_interior_for_player");
        state.put("advancement", "echoashfallprotocol:find_drop_pod");
        state.put("advancementCriterion", "found_drop_pod");
        state.put("welcomePacket", "echoashfallprotocol:welcome_screen");
        state.put("repairExistingPlayers", List.of(
                "rescue_underground_starting_pod_below_y_48",
                "repair_missing_drop_pod_respawn",
                "reissue_terminal_remote_if_available"
        ));
        return state;
    }

    private static Map<String, Object> inventoryPlan(Set<String> loadedModules) {
        boolean terminalLoaded = loadedModules.contains("echoterminal");
        boolean wikiLoaded = loadedModules.contains("echowiki");

        Map<String, Object> inventory = new LinkedHashMap<>();
        inventory.put("directStarterItems", List.of(Map.of(
                "item", "echoashfallprotocol:field_manual",
                "count", 1,
                "customNameKey", "item.EchoAshfallProtocol.echo_starter_note.name",
                "role", "first_ten_minutes_checklist"
        )));
        inventory.put("optionalItems", optionalItems(terminalLoaded));
        inventory.put("podLockerFlow", List.of(
                "open_oxygen_locker",
                "open_tools_locker",
                "open_scrap_locker",
                "open_logs_locker"
        ));
        inventory.put("firstTenMinuteChecklist", List.of(
                "drink_clean_water_before_scouting",
                "deploy_ash_campfire_chest_and_torches_near_ramp",
                "take_basic_weapon_then_craft_scrap_knife",
                "store_signal_scanner_until_base_power_is_online",
                wikiLoaded ? "use_wiki_field_manuals_when_discovered" : "use_hud_chat_goals_for_briefing",
                terminalLoaded ? "use_terminal_remote_or_m_key_for_objectives" : "press_n_to_reopen_briefing"
        ));
        inventory.put("starterSuppliesSource", "drop_pod_lockers");
        return inventory;
    }

    private static List<Map<String, Object>> optionalItems(boolean terminalLoaded) {
        List<Map<String, Object>> items = new ArrayList<>();
        items.add(Map.of(
                "item", "echoterminal:echo_terminal_remote",
                "count", 1,
                "condition", "module_loaded:echoterminal",
                "grantedInCurrentNativeContext", terminalLoaded
        ));
        return List.copyOf(items);
    }

    private static Map<String, Object> recoveryPlan() {
        Map<String, Object> recovery = new LinkedHashMap<>();
        recovery.put("initialObjective", "echoashfallprotocol:find_drop_pod");
        recovery.put("routeObjective", "ashfall:recover_crash_cache");
        recovery.put("deathRecoveryHandoff", "echorecovery:field_cache_service");
        recovery.put("mapLayerHints", List.of(
                "echoashfallprotocol:first_month_field_intel",
                "echoashfallprotocol:first_major_route"
        ));
        recovery.put("lensProfiles", List.of(
                "echoashfallprotocol:ashfall_major_route_scans"
        ));
        recovery.put("codexEntries", List.of(
                "echoashfallprotocol:hazard_route_prep",
                "echoashfallprotocol:weather_route_planning",
                "echoashfallprotocol:relay_hazard_prep"
        ));
        recovery.put("safeRuntimeAction", "plan_only_until_native_player_and_world_bridges_exist");
        return recovery;
    }

    private static Map<String, Object> screenSafeSurfaces() {
        Map<String, Object> surfaces = new LinkedHashMap<>();
        surfaces.put("welcomeScreen", "echoashfallprotocol:welcome_screen");
        surfaces.put("hudMissionLine", "Place an Ash Campfire near the crash site");
        surfaces.put("hudHazardLine", "AIR stable; hazards marked.");
        surfaces.put("notificationAnchor", "top_left_safe_area");
        surfaces.put("terminalCard", "ashfall:first_ten_minutes");
        surfaces.put("wikiGuide", "echowiki:ashfall");
        surfaces.put("screenSafe", true);
        return surfaces;
    }

    private static Map<String, Object> atmosphereHooks() {
        Map<String, Object> hooks = new LinkedHashMap<>();
        hooks.put("defaultWeatherReadout", "CLEAR");
        hooks.put("routeHazards", List.of(
                "echoweathercore:ash_storm",
                "echoashfallprotocol:ashfall_toxic_front",
                "echoashfallprotocol:radiation"
        ));
        hooks.put("soundCues", List.of(
                "echoashfallprotocol:event.ash_storm",
                "echoashfallprotocol:ui.echo_message",
                "echosoundcore:ui_warning",
                "echosoundcore:ambient_loop"
        ));
        hooks.put("weatherProfileConsumer", "echoweathercore:weather_state");
        hooks.put("atmosphereProfileConsumer", "echoatmospherecore:storm_visibility");
        return hooks;
    }

    private static Map<String, Object> validationSignals() {
        Map<String, Object> validation = new LinkedHashMap<>();
        validation.put("provesGameplayParity", true);
        validation.put("nativeHookReportable", true);
        validation.put("expectedEventHooks", List.of(
                "player.first_join",
                "player.respawn_position",
                "server.started",
                "data.reload"
        ));
        validation.put("expectedServiceSurfaces", List.of(
                "player_recovery",
                "ui_hud_screen_safe",
                "weather_sound_atmosphere"
        ));
        return validation;
    }

    private static List<String> remainingNativeBlockers(
            Map<String, Object> minecraftRuntimeAdapterContract,
            Map<String, Object> bridgeDispatch) {
        if ("PASS".equals(minecraftRuntimeAdapterContract.get("status"))) {
            Object remaining = minecraftRuntimeAdapterContract.get("remainingExternalRuntimeWork");
            if (remaining instanceof List<?> list) {
                List<String> blockers = new ArrayList<>();
                for (Object item : list) {
                    if (item instanceof String text && !text.isBlank()) {
                        blockers.add(text);
                    }
                }
                return List.copyOf(blockers);
            }
        }
        if ("PASS".equals(bridgeDispatch.get("status"))) {
            Object remaining = bridgeDispatch.get("remainingMinecraftRuntimeAdapters");
            if (remaining instanceof List<?> list) {
                List<String> blockers = new ArrayList<>();
                for (Object item : list) {
                    if (item instanceof String text && !text.isBlank()) {
                        blockers.add(text);
                    }
                }
                return List.copyOf(blockers);
            }
        }
        return List.of(
                "native_player_inventory_bridge",
                "native_world_structure_placement_bridge",
                "native_respawn_position_bridge",
                "native_advancement_bridge",
                "native_screen_packet_bridge"
        );
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> childMap(Map<String, Object> parent, String key) {
        if (parent != null && parent.get(key) instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private static Set<String> loadedModules(Map<String, String> context) {
        TreeSet<String> modules = new TreeSet<>();
        if (context == null) {
            return modules;
        }
        addModules(modules, context.get("loadedModules"));
        addModules(modules, context.get("modules"));
        addModules(modules, context.get("optionalModules"));
        return modules;
    }

    private static void addModules(Set<String> modules, String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        for (String part : raw.split("[,;\\s]+")) {
            String module = part.trim().toLowerCase(Locale.ROOT);
            if (!module.isEmpty()) {
                modules.add(module);
            }
        }
    }
}
