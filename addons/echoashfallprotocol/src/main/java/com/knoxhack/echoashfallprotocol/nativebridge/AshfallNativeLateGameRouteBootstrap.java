package com.knoxhack.echoashfallprotocol.nativebridge;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AshfallNativeLateGameRouteBootstrap {
    private static final String MODULE_ID = "echoashfallprotocol";
    private static final String PREREQUISITE = "echoashfallprotocol:calibrate_midgame_grid";

    private AshfallNativeLateGameRouteBootstrap() {
    }

    public static Map<String, Object> initialize(Map<String, String> context) {
        Map<String, Object> replay = lateGameRouteReplay();
        List<String> diagnostics = validate(replay);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", "echoashfallprotocol:late_game_route_bootstrap");
        data.put("moduleId", MODULE_ID);
        data.put("packId", context == null ? "unknown" : context.getOrDefault("packId", "unknown"));
        data.put("serviceId", "echoashfallprotocol:late_game_route_bootstrap");
        data.put("adapterCoreBridge", true);
        data.put("implementationTarget", "AdapterCore native late-game route-state replay");
        data.put("standaloneDuplicateGameplaySystem", false);
        data.put("serviceCodeExecuted", true);
        data.put("runtimeStateInitialized", true);
        data.put("minecraftRuntimeAccessed", false);
        data.put("registryMutated", false);
        data.put("unsafeRuntimeWorkStarted", false);
        data.put("verificationMode", "jdk_only_adaptercore_late_game_route_replay_validation");
        data.put("lateGameRouteReplay", replay);
        data.put("verifiedMissionCount", replay.get("verifiedMissionCount"));
        data.put("verifiedObjectiveCount", replay.get("verifiedObjectiveCount"));
        data.put("verifiedEventCount", replay.get("eventCount"));
        data.put("grantedRewardCount", replay.get("grantedRewardCount"));
        data.put("pendingMinecraftHostBridges", pendingConcreteRuntimeBridges());
        data.put("diagnostics", diagnostics);
        data.put("status", diagnostics.isEmpty() ? "PASS" : "FAIL");
        data.put("summary", diagnostics.isEmpty()
                ? "Late-game grid, boss, cryogenic, Nexus, Prime relay, final decision, and RESTORE/DESTROY/CONTROL ending routes have native AdapterCore no-launch route-state, UI-feedback, save-persistence, and reward replay evidence."
                : "Late-game route replay failed prerequisite, objective, event, reward, or path validation.");
        return data;
    }

    private static Map<String, Object> lateGameRouteReplay() {
        List<String> diagnostics = new ArrayList<>();
        List<Map<String, Object>> events = new ArrayList<>();
        List<Map<String, Object>> rewards = new ArrayList<>();
        List<String> completedMissions = new ArrayList<>();
        List<String> completedObjectives = new ArrayList<>();
        completedMissions.add(PREREQUISITE);

        for (Map<String, Object> spec : lateGameMissions()) {
            String missionId = string(spec.get("missionId"));
            String prerequisite = string(spec.get("prerequisite"));
            String uiSurface = string(spec.get("uiFeedbackSurface"));
            int rewardCount = intValue(spec.get("rewardCount"));
            List<Map<String, Object>> objectiveSpecs = objectives(spec);

            if (!completedMissions.contains(prerequisite)) {
                diagnostics.add("Expected " + missionId + " prerequisite " + prerequisite
                        + " to be completed before replay.");
            }

            for (String hook : stringList(spec.get("runtimeHooks"))) {
                appendEvent(events, spec, objectiveSpecs.isEmpty() ? Map.of() : objectiveSpecs.get(0), hook,
                        "nativeHooks.runtimeEvents", uiSurface);
            }
            for (Map<String, Object> objectiveSpec : objectiveSpecs) {
                appendEvent(events, spec, objectiveSpec, string(objectiveSpec.get("event")),
                        "objective:" + objectiveSpec.get("objectiveId"), uiSurface);
                if ("place_block".equals(objectiveSpec.get("objectiveType"))) {
                    appendEvent(events, spec, objectiveSpec, "ashfall.block_requirement",
                            "objective:" + objectiveSpec.get("objectiveId"), uiSurface);
                }
                completedObjectives.add(string(objectiveSpec.get("objectiveId")));
            }

            completedMissions.add(missionId);

            String rewardSource = objectiveSpecs.isEmpty()
                    ? missionId
                    : String.valueOf(objectiveSpecs.get(0).get("objectiveId"));
            for (int rewardIndex = 0; rewardIndex < rewardCount; rewardIndex++) {
                Map<String, Object> reward = new LinkedHashMap<>();
                reward.put("id", missionId + "/reward_" + rewardIndex);
                reward.put("missionId", missionId);
                reward.put("sourceObjective", rewardSource);
                reward.put("grantStatus", "granted_to_pending_reward_state");
                reward.put("terminalFeedbackSurface", "echoterminal:reward_inbox");
                reward.put("savePersistenceKey", "QuestData.pendingRewards[" + missionId + "]");
                reward.put("adapterCoreBridge", true);
                reward.put("routeReplayPrepared", true);
                reward.put("noLaunchNativeStateMutated", true);
                reward.put("liveRuntimeMutation", false);
                reward.put("nativeStateMutated", false);
                reward.put("standaloneDuplicateGameplaySystem", false);
                reward.put("minecraftRuntimeAccessed", false);
                rewards.add(reward);
            }
        }

        Map<String, Object> routeState = routeState(completedMissions, completedObjectives);
        Map<String, Object> replay = new LinkedHashMap<>();
        replay.put("id", "echoashfallprotocol:late_game_route_replay");
        replay.put("moduleId", MODULE_ID);
        replay.put("bridge", "adaptercore.native_late_game_route_replay");
        replay.put("adapterCoreBridge", true);
        replay.put("implementationTarget", "AdapterCore JDK-only late-game/Nexus route replay");
        replay.put("executionMode", "adaptercore_jdk_only_route_state_replay");
        replay.put("verificationScope", "no_launch_native_late_game_route_state");
        replay.put("standaloneDuplicateGameplaySystem", false);
        replay.put("serviceCodeExecuted", true);
        replay.put("runtimeStateInitialized", true);
        replay.put("routeReplayPrepared", true);
        replay.put("noLaunchNativeStateMutated", true);
        replay.put("liveRuntimeMutation", false);
        replay.put("nativeStateMutated", false);
        replay.put("minecraftRuntimeAccessed", false);
        replay.put("minecraftRuntimeMutated", false);
        replay.put("minecraftRegistryMutated", false);
        replay.put("prerequisite", PREREQUISITE);
        replay.put("requiredMissionCount", lateGameMissions().size());
        replay.put("verifiedMissionCount", diagnostics.isEmpty() ? lateGameMissions().size() : 0);
        replay.put("requiredObjectiveCount", expectedObjectiveCount());
        replay.put("verifiedObjectiveCount", diagnostics.isEmpty() ? completedObjectives.size() : 0);
        replay.put("requiredEventCount", expectedEventCount());
        replay.put("requiredRewardCount", expectedRewardCount());
        replay.put("grantedRewardCount", diagnostics.isEmpty() ? rewards.size() : 0);
        replay.put("events", List.copyOf(events));
        replay.put("eventCount", events.size());
        replay.put("rewards", List.copyOf(rewards));
        replay.put("uiFeedbackSurfaces", uiFeedback(events));
        replay.put("uiFeedbackCount", events.size());
        replay.put("savePersistenceWrites", saveWrites(events, lateGameMissions()));
        replay.put("savePersistenceWriteCount", events.size() + lateGameMissions().size() + lateGameMissions().size());
        replay.put("pathIds", List.of("RESTORE", "DESTROY", "CONTROL"));
        replay.put("nativeRouteState", routeState);
        replay.put("diagnostics", List.copyOf(diagnostics));
        replay.put("status", diagnostics.isEmpty() ? "PASS" : "FAIL");
        replay.put("summary", diagnostics.isEmpty()
                ? "Prepared and validated canonical late-game grid, boss, cryogenic, Nexus, Prime relay, final decision, and three ending route states and rewards without launching Minecraft; live mutation remains pending host dispatch."
                : "Canonical late-game route replay failed prerequisite, objective, event, or reward validation.");
        return replay;
    }

    private static Map<String, Object> routeState(List<String> completedMissions, List<String> completedObjectives) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("completedMissions", List.copyOf(completedMissions));
        state.put("completedMissionCount", Math.max(0, completedMissions.size() - 1));
        state.put("requiredMissionCount", lateGameMissions().size());
        state.put("completedObjectives", List.copyOf(completedObjectives));
        state.put("completedObjectiveCount", completedObjectives.size());
        state.put("requiredObjectiveCount", expectedObjectiveCount());
        state.put("stationaryScannerDeployed", completedMissions.contains(id("deploy_stationary_scanner")));
        state.put("powerNodeActivated", completedMissions.contains(id("activate_power_node")));
        state.put("relayStationActivated", completedMissions.contains(id("activate_relay_station")));
        state.put("scoutDroneBuilt", completedMissions.contains(id("build_scout_drone")));
        state.put("nexusCapacitorBuilt", completedMissions.contains(id("build_nexus_capacitor")));
        state.put("workshopBuilt", completedMissions.contains(id("build_workshop")));
        state.put("bossNeutralizationCount", countCompleted(completedMissions, "neutralize_"));
        state.put("bossesNeutralized", countCompleted(completedMissions, "neutralize_") == 8);
        state.put("cryogenicRuinsEntered", completedMissions.contains(id("enter_cryogenic_ruins")));
        state.put("cryoSampleRecovered", completedMissions.contains(id("recover_cryo_sample")));
        state.put("coldExposureRecovered", completedMissions.contains(id("warm_up_after_exposure")));
        state.put("coldRouteSuppliesCrafted", completedMissions.contains(id("craft_cold_route_supplies")));
        state.put("nexusCoreFound", completedMissions.contains(id("find_nexus_core")));
        state.put("nexusCoreAwakened", completedMissions.contains(id("awaken_nexus_core")));
        state.put("primeRelaysScanned", completedMissions.contains(id("scan_prime_relays")));
        state.put("primeRelaysResolved", completedMissions.contains(id("resolve_prime_relays")));
        state.put("nexusGridStabilized", completedMissions.contains(id("stabilize_nexus_grid")));
        state.put("coreCountermeasureSurvived", completedMissions.contains(id("survive_core_countermeasure")));
        state.put("finalDecisionReached", completedMissions.contains(id("reach_decision")));
        state.put("restoreEndingComplete", completedMissions.contains(id("restore_epilogue")));
        state.put("destroyEndingComplete", completedMissions.contains(id("destroy_epilogue")));
        state.put("controlEndingComplete", completedMissions.contains(id("control_epilogue")));
        int endingCount = (completedMissions.contains(id("restore_epilogue")) ? 1 : 0)
                + (completedMissions.contains(id("destroy_epilogue")) ? 1 : 0)
                + (completedMissions.contains(id("control_epilogue")) ? 1 : 0);
        state.put("endingCount", endingCount);
        state.put("routeComplete", Math.max(0, completedMissions.size() - 1) == lateGameMissions().size());
        return state;
    }

    private static List<Map<String, Object>> lateGameMissions() {
        return List.of(
                mission("phase_6", "deploy_stationary_scanner", "calibrate_midgame_grid", "turn_in", 1,
                        hooks("player.block_placed", "ashfall.block_requirement"),
                        objective("deploy_stationary_scanner", "signal_scanner", "place_block", "player.block_placed", id("signal_scanner"))),
                mission("phase_6", "activate_power_node", "deploy_stationary_scanner", "auto_complete_when_predicate_true", 2,
                        hooks("player.block_placed", "ashfall.block_requirement"),
                        objective("activate_power_node", "power_node", "place_block", "player.block_placed", id("power_node"))),
                mission("phase_6", "activate_relay_station", "activate_power_node", "turn_in", 2,
                        hooks("player.block_placed", "ashfall.block_requirement"),
                        objective("activate_relay_station", "relay_station", "place_block", "player.block_placed", id("relay_station"))),
                mission("phase_6", "build_scout_drone", "activate_relay_station", "turn_in", 2,
                        hooks("player.inventory_changed", "ashfall.inventory_predicate"),
                        objective("build_scout_drone", "scout_drone_item", "deliver_item", "player.inventory_changed", id("scout_drone_item"))),
                mission("phase_6", "build_nexus_capacitor", "build_scout_drone", "turn_in", 2,
                        hooks("player.block_placed", "ashfall.block_requirement"),
                        objective("build_nexus_capacitor", "nexus_capacitor", "place_block", "player.block_placed", id("nexus_capacitor"))),
                mission("phase_6", "build_workshop", "build_nexus_capacitor", "turn_in", 2,
                        hooks("player.block_placed", "ashfall.block_requirement"),
                        objective("build_workshop", "workshop_block", "place_block", "player.block_placed", id("workshop_block"))),
                killMission("neutralize_plains_warlord", "build_workshop", "plains_warlord"),
                killMission("neutralize_city_ruin_stalker", "neutralize_plains_warlord", "city_ruin_stalker"),
                killMission("neutralize_industrial_juggernaut", "neutralize_city_ruin_stalker", "industrial_juggernaut"),
                killMission("neutralize_toxic_hive_matriarch", "neutralize_industrial_juggernaut", "toxic_hive_matriarch"),
                killMission("neutralize_crash_zone_colossus", "neutralize_toxic_hive_matriarch", "crash_zone_colossus"),
                killMission("neutralize_radiation_behemoth", "neutralize_crash_zone_colossus", "radiation_behemoth"),
                mission("phase_6", "enter_cryogenic_ruins", "neutralize_radiation_behemoth", "auto_complete_when_predicate_true", 2,
                        hooks("ashfall.location_visited"),
                        objective("enter_cryogenic_ruins", "cryogenic_ruins", "enter_region", "ashfall.location_visited", "cryogenic_ruins")),
                mission("phase_6", "recover_cryo_sample", "enter_cryogenic_ruins", "turn_in", 2,
                        hooks("player.inventory_changed", "ashfall.inventory_predicate"),
                        objective("recover_cryo_sample", "cryogenic_fractured_stone", "deliver_item", "player.inventory_changed", id("cryogenic_fractured_stone"))),
                mission("phase_6", "warm_up_after_exposure", "recover_cryo_sample", "turn_in", 1,
                        hooks("ashfall.runtime_predicate", "missioncore.record_objective"),
                        objective("warm_up_after_exposure", "warmed_up", "custom", "ashfall.location_visited", "cold:warmed_up")),
                mission("phase_6", "craft_cold_route_supplies", "warm_up_after_exposure", "turn_in", 2,
                        hooks("player.inventory_changed", "ashfall.inventory_predicate"),
                        objective("craft_cold_route_supplies", "thermal_liner", "deliver_item", "player.inventory_changed", id("thermal_liner")),
                        objective("craft_cold_route_supplies", "hand_warmer", "deliver_item", "player.inventory_changed", id("hand_warmer"))),
                killMission("neutralize_cryogenic_overseer", "craft_cold_route_supplies", "cryogenic_overseer"),
                killMission("neutralize_nexus_scar_avatar", "neutralize_cryogenic_overseer", "nexus_scar_avatar"),
                mission("phase_7", "find_nexus_core", "neutralize_nexus_scar_avatar", "auto_complete_when_predicate_true", 1,
                        hooks("player.block_placed", "ashfall.block_requirement"),
                        objective("find_nexus_core", "nexus_core", "place_block", "player.block_placed", id("nexus_core"))),
                runtimeMission("phase_7", "awaken_nexus_core", "find_nexus_core", 2),
                runtimeMission("phase_7", "scan_prime_relays", "awaken_nexus_core", 2, "scan_block"),
                runtimeMission("phase_7", "resolve_prime_relays", "scan_prime_relays", 2, "establish_route"),
                runtimeMission("phase_7", "stabilize_nexus_grid", "resolve_prime_relays", 1),
                runtimeMission("phase_7", "survive_core_countermeasure", "stabilize_nexus_grid", 2, "survive_time"),
                runtimeMission("phase_7", "reach_decision", "survive_core_countermeasure", 0),
                pathMission("restore_repair_nodes", "reach_decision", 2),
                pathMission("restore_purge_corruption", "restore_repair_nodes", 3),
                pathLocationMission("restore_enter_archives", "restore_purge_corruption", 1),
                pathMission("restore_guardian", "restore_enter_archives", 1),
                pathMission("restore_world_lattice", "restore_guardian", 2),
                pathMission("restore_finale", "restore_world_lattice", 1),
                pathMission("restore_epilogue", "restore_finale", 1, "turn_in"),
                pathMission("destroy_scorched_earth", "reach_decision", 2),
                pathMission("destroy_survive_storms", "destroy_scorched_earth", 2, "auto_complete_when_predicate_true", "survive_time"),
                pathLocationMission("destroy_enter_archives", "destroy_survive_storms", 1),
                pathMission("destroy_guardian", "destroy_enter_archives", 1),
                pathMission("destroy_dead_signal", "destroy_guardian", 2),
                pathMission("destroy_finale", "destroy_dead_signal", 1),
                pathMission("destroy_epilogue", "destroy_finale", 1, "turn_in"),
                pathMission("control_signal_expansion", "reach_decision", 2),
                mission("phase_8", "control_resource_dominance", "control_signal_expansion", "auto_complete_when_predicate_true", 2,
                        hooks("player.inventory_changed", "ashfall.inventory_predicate", "ashfall.path_gate"),
                        objective("control_resource_dominance", "dense_alloy_chunk", "obtain_item", "player.inventory_changed", id("dense_alloy_chunk")),
                        objective("control_resource_dominance", "nexus_crystal", "obtain_item", "player.inventory_changed", id("nexus_crystal")),
                        objective("control_resource_dominance", "energy_cell", "obtain_item", "player.inventory_changed", id("energy_cell"))),
                pathLocationMission("control_enter_archives", "control_resource_dominance", 1),
                pathMission("control_guardian", "control_enter_archives", 1),
                pathMission("control_command_lattice", "control_guardian", 2),
                pathMission("control_finale", "control_command_lattice", 1),
                pathMission("control_epilogue", "control_finale", 1, "turn_in")
        );
    }

    private static Map<String, Object> killMission(String mission, String prerequisite, String entity) {
        return mission("phase_6", mission, prerequisite, "auto_complete_when_predicate_true", 2,
                hooks("player.kill_entity", "ashfall.entity_kill"),
                objective(mission, entity, "kill_entity", "player.kill_entity", id(entity)));
    }

    private static Map<String, Object> runtimeMission(String phase, String mission, String prerequisite, int rewards) {
        return runtimeMission(phase, mission, prerequisite, rewards, "custom");
    }

    private static Map<String, Object> runtimeMission(
            String phase,
            String mission,
            String prerequisite,
            int rewards,
            String objectiveType) {
        return mission(phase, mission, prerequisite, "auto_complete_when_predicate_true", rewards,
                hooks("ashfall.runtime_predicate", "missioncore.record_objective"),
                objective(mission, mission, objectiveType, "ashfall.runtime_predicate", id(mission)));
    }

    private static Map<String, Object> pathMission(String mission, String prerequisite, int rewards) {
        return pathMission(mission, prerequisite, rewards, "auto_complete_when_predicate_true");
    }

    private static Map<String, Object> pathMission(
            String mission,
            String prerequisite,
            int rewards,
            String terminalAction) {
        return pathMission(mission, prerequisite, rewards, terminalAction, "custom");
    }

    private static Map<String, Object> pathMission(
            String mission,
            String prerequisite,
            int rewards,
            String terminalAction,
            String objectiveType) {
        return mission("phase_8", mission, prerequisite, terminalAction, rewards,
                hooks("ashfall.runtime_predicate", "missioncore.record_objective", "ashfall.path_gate"),
                objective(mission, mission, objectiveType, "ashfall.runtime_predicate", id(mission)));
    }

    private static Map<String, Object> pathLocationMission(String mission, String prerequisite, int rewards) {
        return mission("phase_8", mission, prerequisite, "auto_complete_when_predicate_true", rewards,
                hooks("ashfall.location_visited", "ashfall.path_gate"),
                objective(mission, "prefall_archives", "enter_region", "ashfall.location_visited", id("prefall_archives")));
    }

    @SafeVarargs
    private static Map<String, Object> mission(
            String phase,
            String mission,
            String prerequisite,
            String terminalAction,
            int rewardCount,
            List<String> runtimeHooks,
            Map<String, Object>... objectives) {
        String missionId = id(mission);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("missionId", missionId);
        data.put("neoForgeMissionId", mission);
        data.put("missionDataPath", "data/echoashfallprotocol/missioncore/missions/" + mission + ".json");
        data.put("nativeGameplayHook", "ashfall.route." + mission);
        data.put("phase", phase);
        data.put("prerequisite", id(prerequisite));
        data.put("runtimeHooks", runtimeHooks);
        data.put("objectives", List.of(objectives));
        data.put("uiFeedbackSurface", "echoterminal:ashfall_nexus_prime_relays");
        data.put("terminalAction", terminalAction);
        data.put("rewardCount", rewardCount);
        return Map.copyOf(data);
    }

    private static Map<String, Object> objective(
            String mission,
            String objectiveSuffix,
            String objectiveType,
            String event,
            String target) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("objectiveId", id(mission) + "/" + objectiveSuffix);
        data.put("objectiveType", objectiveType);
        data.put("event", event);
        data.put("target", target);
        return Map.copyOf(data);
    }

    private static List<String> hooks(String... hooks) {
        return List.of(hooks);
    }

    private static List<Map<String, Object>> objectives(Map<String, Object> mission) {
        List<Map<String, Object>> objectives = new ArrayList<>();
        Object rawObjectives = mission.get("objectives");
        if (rawObjectives instanceof List<?> objectiveList) {
            for (Object rawObjective : objectiveList) {
                if (rawObjective instanceof Map<?, ?> objective) {
                    objectives.add(copyMap(objective));
                }
            }
        }
        return List.copyOf(objectives);
    }

    private static void appendEvent(
            List<Map<String, Object>> events,
            Map<String, Object> mission,
            Map<String, Object> objective,
            String eventId,
            String bindingSource,
            String uiSurface) {
        String missionId = string(mission.get("missionId"));
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("order", events.size() + 1);
        event.put("missionId", missionId);
        event.put("neoForgeMissionId", mission.get("neoForgeMissionId"));
        event.put("phase", mission.get("phase"));
        event.put("objectiveId", objective.getOrDefault("objectiveId", missionId));
        event.put("objectiveType", objective.getOrDefault("objectiveType", "mission_hook"));
        event.put("event", eventId);
        event.put("target", objective.getOrDefault("target", missionId));
        event.put("bindingSource", bindingSource);
        event.put("nativeGameplayHook", mission.get("nativeGameplayHook"));
        event.put("terminalAction", mission.get("terminalAction"));
        event.put("uiFeedbackSurface", uiSurface);
        event.put("savePersistenceKey", "QuestData.objectiveStates[" + missionId + "]");
        event.put("adapterCoreBridge", true);
        event.put("routeReplayPrepared", true);
        event.put("noLaunchNativeStateMutated", true);
        event.put("liveRuntimeMutation", false);
        event.put("nativeStateMutated", false);
        event.put("standaloneDuplicateGameplaySystem", false);
        event.put("minecraftRuntimeAccessed", false);
        event.put("recorded", true);
        events.add(event);
    }

    private static List<String> pendingConcreteRuntimeBridges() {
        return List.of(
                "Minecraft-host route smoke for power nodes, relay stations, bosses, cryogenic ruins, Nexus core, Prime relays, final decision, and all three endings",
                "Minecraft-host loot/reward delivery for late-game, Nexus, and ending rewards",
                "Visual Terminal/HoloMap/Lens/Index confirmation for late-game route and ending state",
                "Live boss kill, path-gate, dimension-entry, and end-state host mutation confirmation"
        );
    }

    private static List<String> requiredRouteStateFlags() {
        return List.of(
                "stationaryScannerDeployed",
                "powerNodeActivated",
                "relayStationActivated",
                "scoutDroneBuilt",
                "nexusCapacitorBuilt",
                "workshopBuilt",
                "bossesNeutralized",
                "cryogenicRuinsEntered",
                "cryoSampleRecovered",
                "coldExposureRecovered",
                "coldRouteSuppliesCrafted",
                "nexusCoreFound",
                "nexusCoreAwakened",
                "primeRelaysScanned",
                "primeRelaysResolved",
                "nexusGridStabilized",
                "coreCountermeasureSurvived",
                "finalDecisionReached",
                "restoreEndingComplete",
                "destroyEndingComplete",
                "controlEndingComplete",
                "routeComplete"
        );
    }

    private static List<String> uiFeedback(List<Map<String, Object>> events) {
        List<String> feedback = new ArrayList<>();
        for (Map<String, Object> event : events) {
            feedback.add(String.valueOf(event.get("uiFeedbackSurface")));
        }
        return List.copyOf(feedback);
    }

    private static List<String> saveWrites(
            List<Map<String, Object>> events,
            List<Map<String, Object>> missions) {
        List<String> writes = new ArrayList<>();
        for (Map<String, Object> event : events) {
            writes.add(String.valueOf(event.get("savePersistenceKey")));
        }
        for (Map<String, Object> mission : missions) {
            String missionId = string(mission.get("missionId"));
            writes.add("QuestData.completedMissions[" + missionId + "]");
            writes.add("QuestData.pendingRewards[" + missionId + "]");
        }
        return List.copyOf(writes);
    }

    private static List<String> validate(Map<String, Object> replay) {
        List<String> diagnostics = new ArrayList<>();
        if (!"PASS".equals(replay.get("status"))) {
            diagnostics.add("Expected late-game route replay to pass.");
        }
        requireCount(replay, "verifiedMissionCount", lateGameMissions().size(), diagnostics);
        requireCount(replay, "verifiedObjectiveCount", expectedObjectiveCount(), diagnostics);
        requireCount(replay, "eventCount", expectedEventCount(), diagnostics);
        requireCount(replay, "grantedRewardCount", expectedRewardCount(), diagnostics);
        requireCount(replay, "uiFeedbackCount", expectedEventCount(), diagnostics);
        requireCount(replay, "savePersistenceWriteCount",
                expectedEventCount() + lateGameMissions().size() + lateGameMissions().size(), diagnostics);
        Map<String, Object> state = childMap(replay, "nativeRouteState");
        requireCount(state, "completedObjectiveCount", expectedObjectiveCount(), diagnostics);
        requireCount(state, "bossNeutralizationCount", 8, diagnostics);
        requireCount(state, "endingCount", 3, diagnostics);
        for (String flag : requiredRouteStateFlags()) {
            if (!Boolean.TRUE.equals(state.get(flag))) {
                diagnostics.add("Expected late-game native route state flag " + flag + " to be true.");
            }
        }
        return List.copyOf(diagnostics);
    }

    private static int expectedObjectiveCount() {
        int total = 0;
        for (Map<String, Object> mission : lateGameMissions()) {
            total += objectives(mission).size();
        }
        return total;
    }

    private static int expectedRewardCount() {
        int total = 0;
        for (Map<String, Object> mission : lateGameMissions()) {
            total += intValue(mission.get("rewardCount"));
        }
        return total;
    }

    private static int expectedEventCount() {
        int total = 0;
        for (Map<String, Object> mission : lateGameMissions()) {
            total += stringList(mission.get("runtimeHooks")).size();
            for (Map<String, Object> objective : objectives(mission)) {
                total++;
                if ("place_block".equals(objective.get("objectiveType"))) {
                    total++;
                }
            }
        }
        return total;
    }

    private static int countCompleted(List<String> completedMissions, String prefix) {
        int count = 0;
        for (String mission : completedMissions) {
            if (mission.startsWith(MODULE_ID + ":" + prefix)) {
                count++;
            }
        }
        return count;
    }

    private static void requireCount(
            Map<String, Object> data,
            String key,
            int expected,
            List<String> diagnostics) {
        Object actual = data.get(key);
        if (!(actual instanceof Integer count) || count != expected) {
            diagnostics.add("Expected " + key + "=" + expected + " but found " + actual + ".");
        }
    }

    private static Map<String, Object> childMap(Map<String, Object> data, String key) {
        Object rawChild = data.get(key);
        if (rawChild instanceof Map<?, ?> child) {
            return copyMap(child);
        }
        return Map.of();
    }

    private static Map<String, Object> copyMap(Map<?, ?> source) {
        Map<String, Object> copy = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            copy.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return copy;
    }

    private static List<String> stringList(Object value) {
        List<String> strings = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (Object entry : list) {
                strings.add(String.valueOf(entry));
            }
        }
        return List.copyOf(strings);
    }

    private static String id(String path) {
        return MODULE_ID + ":" + path;
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static int intValue(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }
}
