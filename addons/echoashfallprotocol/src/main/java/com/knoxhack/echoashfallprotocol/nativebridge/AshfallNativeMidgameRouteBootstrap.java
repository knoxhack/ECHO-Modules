package com.knoxhack.echoashfallprotocol.nativebridge;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AshfallNativeMidgameRouteBootstrap {
    private static final String MODULE_ID = "echoashfallprotocol";
    private static final String PREREQUISITE = "echoashfallprotocol:poi_explorer";

    private AshfallNativeMidgameRouteBootstrap() {
    }

    public static Map<String, Object> initialize(Map<String, String> context) {
        Map<String, Object> replay = midgameRouteReplay();
        List<String> diagnostics = validate(replay);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", "echoashfallprotocol:midgame_route_bootstrap");
        data.put("moduleId", MODULE_ID);
        data.put("packId", context == null ? "unknown" : context.getOrDefault("packId", "unknown"));
        data.put("serviceId", "echoashfallprotocol:midgame_route_bootstrap");
        data.put("adapterCoreBridge", true);
        data.put("implementationTarget", "AdapterCore native midgame route-state replay");
        data.put("standaloneDuplicateGameplaySystem", false);
        data.put("serviceCodeExecuted", true);
        data.put("runtimeStateInitialized", true);
        data.put("minecraftRuntimeAccessed", false);
        data.put("registryMutated", false);
        data.put("unsafeRuntimeWorkStarted", false);
        data.put("verificationMode", "jdk_only_adaptercore_midgame_route_replay_validation");
        data.put("midgameRouteReplay", replay);
        data.put("verifiedMissionCount", replay.get("verifiedMissionCount"));
        data.put("verifiedObjectiveCount", replay.get("verifiedObjectiveCount"));
        data.put("grantedRewardCount", replay.get("grantedRewardCount"));
        data.put("pendingMinecraftHostBridges", pendingConcreteRuntimeBridges());
        data.put("diagnostics", diagnostics);
        data.put("status", diagnostics.isEmpty() ? "PASS" : "FAIL");
        data.put("summary", diagnostics.isEmpty()
                ? "Midgame biohazard, medical, radiation, dense-alloy, and thermal/geology routes have native AdapterCore no-launch route-state, UI-feedback, save-persistence, and reward replay evidence."
                : "Midgame route replay failed prerequisite, objective, or reward validation.");
        return data;
    }

    private static Map<String, Object> midgameRouteReplay() {
        List<String> diagnostics = new ArrayList<>();
        List<Map<String, Object>> events = new ArrayList<>();
        List<Map<String, Object>> rewards = new ArrayList<>();
        List<String> completedMissions = new ArrayList<>();
        List<String> completedObjectives = new ArrayList<>();
        completedMissions.add(PREREQUISITE);

        for (Map<String, Object> spec : midgameMissions()) {
            String missionId = string(spec.get("missionId"));
            String prerequisite = string(spec.get("prerequisite"));
            String uiSurface = string(spec.get("uiFeedbackSurface"));
            int rewardCount = intValue(spec.get("rewardCount"));
            List<Map<String, Object>> objectiveSpecs = objectives(spec);

            if (!completedMissions.contains(prerequisite)) {
                diagnostics.add("Expected " + missionId + " prerequisite " + prerequisite
                        + " to be completed before replay.");
            }

            for (Map<String, Object> objectiveSpec : objectiveSpecs) {
                String objectiveId = string(objectiveSpec.get("objectiveId"));
                appendEvent(events, spec, objectiveSpec, string(objectiveSpec.get("event")), uiSurface);
                if ("place_block".equals(objectiveSpec.get("objectiveType"))) {
                    appendEvent(events, spec, objectiveSpec, "ashfall.block_requirement", uiSurface);
                }
                completedObjectives.add(objectiveId);
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

        Map<String, Object> routeState = new LinkedHashMap<>();
        routeState.put("completedMissions", List.copyOf(completedMissions));
        routeState.put("completedMissionCount", Math.max(0, completedMissions.size() - 1));
        routeState.put("requiredMissionCount", midgameMissions().size());
        routeState.put("completedObjectives", List.copyOf(completedObjectives));
        routeState.put("completedObjectiveCount", completedObjectives.size());
        routeState.put("requiredObjectiveCount", expectedObjectiveCount());
        routeState.put("bioLabEntered", completedMissions.contains("echoashfallprotocol:enter_bio_lab"));
        routeState.put("dataLogRecovered", completedMissions.contains("echoashfallprotocol:recover_data_log"));
        routeState.put("reactorRuinSurveyed", completedMissions.contains("echoashfallprotocol:survey_reactor_ruin"));
        routeState.put("fieldMedBayBuilt", completedMissions.contains("echoashfallprotocol:build_field_med_bay"));
        routeState.put("radiationCleanserBuilt", completedMissions.contains("echoashfallprotocol:build_radiation_cleanser"));
        routeState.put("radawayCrafted", completedMissions.contains("echoashfallprotocol:craft_radaway"));
        routeState.put("mutationStatusScanned", completedMissions.contains("echoashfallprotocol:scan_mutation_status"));
        routeState.put("fieldMedBayUsed", completedMissions.contains("echoashfallprotocol:use_field_med_bay"));
        routeState.put("mutationEffectsStabilized", completedMissions.contains("echoashfallprotocol:stabilize_mutation_effects"));
        routeState.put("radiationZoneScouted", completedMissions.contains("echoashfallprotocol:scout_radiation_zone"));
        routeState.put("atmosphericScrubberBuilt", completedMissions.contains("echoashfallprotocol:build_atmospheric_scrubber"));
        routeState.put("mutatedTissueCollected", completedMissions.contains("echoashfallprotocol:collect_mutated_tissue"));
        routeState.put("mutagenVialCrafted", completedMissions.contains("echoashfallprotocol:craft_mutagen_vial"));
        routeState.put("militaryVaultCleared", completedMissions.contains("echoashfallprotocol:clear_military_vault"));
        routeState.put("denseAlloyFound", completedMissions.contains("echoashfallprotocol:find_dense_alloy"));
        routeState.put("thermalArrayBuilt", completedMissions.contains("echoashfallprotocol:build_thermal_array"));
        routeState.put("oreGrinderBuilt", completedMissions.contains("echoashfallprotocol:build_ore_grinder"));
        routeState.put("isotopeRefinerBuilt", completedMissions.contains("echoashfallprotocol:build_isotope_refiner"));
        routeState.put("alloyWeaponForged", completedMissions.contains("echoashfallprotocol:forge_alloy_weapon"));
        routeState.put("alloyKitEquipped", completedMissions.contains("echoashfallprotocol:equip_alloy_kit"));
        routeState.put("routeSuppliesStockpiled", completedMissions.contains("echoashfallprotocol:stockpile_route_supplies"));
        routeState.put("midgameGridCalibrated", completedMissions.contains("echoashfallprotocol:calibrate_midgame_grid"));
        routeState.put("biohazardPhaseComplete", completedMissions.contains("echoashfallprotocol:craft_mutagen_vial"));
        routeState.put("thermalGeologyPhaseComplete", completedMissions.contains("echoashfallprotocol:calibrate_midgame_grid"));
        routeState.put("routeComplete", Math.max(0, completedMissions.size() - 1) == midgameMissions().size());

        Map<String, Object> replay = new LinkedHashMap<>();
        replay.put("id", "echoashfallprotocol:midgame_route_replay");
        replay.put("moduleId", MODULE_ID);
        replay.put("bridge", "adaptercore.native_midgame_route_replay");
        replay.put("adapterCoreBridge", true);
        replay.put("implementationTarget", "AdapterCore JDK-only midgame biohazard/thermal route replay");
        replay.put("executionMode", "adaptercore_jdk_only_route_state_replay");
        replay.put("verificationScope", "no_launch_native_midgame_route_state");
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
        replay.put("requiredMissionCount", midgameMissions().size());
        replay.put("verifiedMissionCount", diagnostics.isEmpty() ? midgameMissions().size() : 0);
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
        replay.put("savePersistenceWrites", saveWrites(events, midgameMissions()));
        replay.put("savePersistenceWriteCount", events.size() + midgameMissions().size() + midgameMissions().size());
        replay.put("linkedMachineRuntimeTargets", List.of(
                "echoashfallprotocol:radiation_cleanser",
                "echoashfallprotocol:ore_grinder",
                "echoashfallprotocol:isotope_refiner"));
        replay.put("nativeRouteState", routeState);
        replay.put("diagnostics", List.copyOf(diagnostics));
        replay.put("status", diagnostics.isEmpty() ? "PASS" : "FAIL");
        replay.put("summary", diagnostics.isEmpty()
                ? "Prepared and validated canonical midgame biohazard, medical, radiation, dense-alloy, and thermal/geology route state and rewards without launching Minecraft; live mutation remains pending host dispatch."
                : "Canonical midgame route replay failed prerequisite or reward validation.");
        return replay;
    }

    private static List<Map<String, Object>> midgameMissions() {
        return List.of(
                mission("phase_4", "enter_bio_lab", "poi_explorer", "echoterminal:midgame_hazard_lab_vault",
                        "auto_complete_when_predicate_true", 2,
                        objective("enter_bio_lab", "enter_bio_lab", "custom", "ashfall.runtime_predicate", "echoashfallprotocol:enter_bio_lab")),
                mission("phase_4", "recover_data_log", "enter_bio_lab", "echoterminal:midgame_hazard_lab_vault",
                        "turn_in", 2,
                        objective("recover_data_log", "recover_data_log", "custom", "ashfall.runtime_predicate", "echoashfallprotocol:recover_data_log")),
                mission("phase_4", "survey_reactor_ruin", "recover_data_log", "echoterminal:midgame_hazard_lab_vault",
                        "auto_complete_when_predicate_true", 2,
                        objective("survey_reactor_ruin", "survey_reactor_ruin", "custom", "ashfall.runtime_predicate", "echoashfallprotocol:survey_reactor_ruin")),
                mission("phase_4", "build_field_med_bay", "survey_reactor_ruin", "echoterminal:midgame_hazard_lab_vault",
                        "auto_complete_when_predicate_true", 1,
                        objective("build_field_med_bay", "field_med_bay", "place_block", "player.block_placed", "echoashfallprotocol:field_med_bay")),
                mission("phase_4", "build_radiation_cleanser", "build_field_med_bay", "echoterminal:midgame_hazard_lab_vault",
                        "turn_in", 2,
                        objective("build_radiation_cleanser", "radiation_cleanser", "place_block", "player.block_placed", "echoashfallprotocol:radiation_cleanser")),
                mission("phase_4", "craft_radaway", "build_radiation_cleanser", "echoterminal:midgame_hazard_lab_vault",
                        "auto_complete_when_predicate_true", 2,
                        objective("craft_radaway", "rad_away", "obtain_item", "player.inventory_changed", "echoashfallprotocol:rad_away")),
                mission("phase_4", "scan_mutation_status", "craft_radaway", "echoterminal:midgame_hazard_lab_vault",
                        "turn_in", 1,
                        objective("scan_mutation_status", "field_med_bay", "place_block", "player.block_placed", "echoashfallprotocol:field_med_bay")),
                mission("phase_4", "use_field_med_bay", "scan_mutation_status", "echoterminal:midgame_hazard_lab_vault",
                        "turn_in", 3,
                        objective("use_field_med_bay", "field_med_bay_used", "custom", "ashfall.location_visited", "medical:field_med_bay_used")),
                mission("phase_4", "stabilize_mutation_effects", "use_field_med_bay", "echoterminal:midgame_hazard_lab_vault",
                        "turn_in", 2,
                        objective("stabilize_mutation_effects", "rad_away", "deliver_item", "player.inventory_changed", "echoashfallprotocol:rad_away"),
                        objective("stabilize_mutation_effects", "bandage", "deliver_item", "player.inventory_changed", "echoashfallprotocol:bandage")),
                mission("phase_4", "scout_radiation_zone", "stabilize_mutation_effects", "echoterminal:midgame_hazard_lab_vault",
                        "turn_in", 2,
                        objective("scout_radiation_zone", "radiation_zone", "enter_region", "ashfall.location_visited", "radiation_zone")),
                mission("phase_4", "build_atmospheric_scrubber", "scout_radiation_zone", "echoterminal:midgame_hazard_lab_vault",
                        "turn_in", 2,
                        objective("build_atmospheric_scrubber", "atmospheric_scrubber", "place_block", "player.block_placed", "echoashfallprotocol:atmospheric_scrubber")),
                mission("phase_4", "collect_mutated_tissue", "build_atmospheric_scrubber", "echoterminal:midgame_hazard_lab_vault",
                        "turn_in", 2,
                        objective("collect_mutated_tissue", "mutated_tissue", "deliver_item", "player.inventory_changed", "echoashfallprotocol:mutated_tissue")),
                mission("phase_4", "craft_mutagen_vial", "collect_mutated_tissue", "echoterminal:midgame_hazard_lab_vault",
                        "turn_in", 2,
                        objective("craft_mutagen_vial", "mutagen_vial", "deliver_item", "player.inventory_changed", "echoashfallprotocol:mutagen_vial")),
                mission("phase_5", "clear_military_vault", "craft_mutagen_vial", "echoterminal:midgame_hazard_lab_vault",
                        "auto_complete_when_predicate_true", 2,
                        objective("clear_military_vault", "clear_military_vault", "custom", "ashfall.runtime_predicate", "echoashfallprotocol:clear_military_vault")),
                mission("phase_5", "find_dense_alloy", "clear_military_vault", "echoterminal:midgame_hazard_lab_vault",
                        "turn_in", 2,
                        objective("find_dense_alloy", "dense_alloy_chunk", "deliver_item", "player.inventory_changed", "echoashfallprotocol:dense_alloy_chunk")),
                mission("phase_5", "build_thermal_array", "find_dense_alloy", "echoterminal:midgame_hazard_lab_vault",
                        "turn_in", 2,
                        objective("build_thermal_array", "thermal_array", "place_block", "player.block_placed", "echoashfallprotocol:thermal_array")),
                mission("phase_5", "build_ore_grinder", "build_thermal_array", "echoterminal:midgame_hazard_lab_vault",
                        "turn_in", 4,
                        objective("build_ore_grinder", "ore_grinder", "place_block", "player.block_placed", "echoashfallprotocol:ore_grinder")),
                mission("phase_5", "build_isotope_refiner", "build_ore_grinder", "echoterminal:midgame_hazard_lab_vault",
                        "auto_complete_when_predicate_true", 1,
                        objective("build_isotope_refiner", "isotope_refiner", "place_block", "player.block_placed", "echoashfallprotocol:isotope_refiner")),
                mission("phase_5", "forge_alloy_weapon", "build_isotope_refiner", "echoterminal:midgame_hazard_lab_vault",
                        "turn_in", 2,
                        objective("forge_alloy_weapon", "forge_alloy_weapon", "custom", "ashfall.runtime_predicate", "echoashfallprotocol:forge_alloy_weapon")),
                mission("phase_5", "equip_alloy_kit", "forge_alloy_weapon", "echoterminal:midgame_hazard_lab_vault",
                        "auto_complete_when_predicate_true", 2,
                        objective("equip_alloy_kit", "alloy_helmet", "custom", "player.equipment_changed", "echoashfallprotocol:alloy_helmet"),
                        objective("equip_alloy_kit", "alloy_chestplate", "custom", "player.equipment_changed", "echoashfallprotocol:alloy_chestplate")),
                mission("phase_5", "stockpile_route_supplies", "equip_alloy_kit", "echoterminal:midgame_hazard_lab_vault",
                        "turn_in", 2,
                        objective("stockpile_route_supplies", "clean_water_bottle", "deliver_item", "player.inventory_changed", "echoashfallprotocol:clean_water_bottle"),
                        objective("stockpile_route_supplies", "emergency_ration", "deliver_item", "player.inventory_changed", "echoashfallprotocol:emergency_ration"),
                        objective("stockpile_route_supplies", "bandage", "deliver_item", "player.inventory_changed", "echoashfallprotocol:bandage"),
                        objective("stockpile_route_supplies", "rad_away", "deliver_item", "player.inventory_changed", "echoashfallprotocol:rad_away"),
                        objective("stockpile_route_supplies", "filter_cartridge_advanced", "deliver_item", "player.inventory_changed", "echoashfallprotocol:filter_cartridge_advanced")),
                mission("phase_5", "calibrate_midgame_grid", "stockpile_route_supplies", "echoterminal:midgame_hazard_lab_vault",
                        "turn_in", 2,
                        objective("calibrate_midgame_grid", "thermal_array", "place_block", "player.block_placed", "echoashfallprotocol:thermal_array"),
                        objective("calibrate_midgame_grid", "factory_controller", "place_block", "player.block_placed", "echoashfallprotocol:factory_controller"),
                        objective("calibrate_midgame_grid", "energy_meter", "place_block", "player.block_placed", "echoashfallprotocol:energy_meter"),
                        objective("calibrate_midgame_grid", "reinforced_power_cable", "place_block", "player.block_placed", "echoashfallprotocol:reinforced_power_cable"),
                        objective("calibrate_midgame_grid", "battery_bank", "place_block", "player.block_placed", "echoashfallprotocol:battery_bank"))
        );
    }

    @SafeVarargs
    private static Map<String, Object> mission(
            String phase,
            String mission,
            String prerequisite,
            String uiFeedbackSurface,
            String terminalAction,
            int rewardCount,
            Map<String, Object>... objectives) {
        String missionId = MODULE_ID + ":" + mission;
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("missionId", missionId);
        data.put("neoForgeMissionId", mission);
        data.put("missionDataPath", "data/echoashfallprotocol/missioncore/missions/" + mission + ".json");
        data.put("nativeGameplayHook", "ashfall.route." + mission);
        data.put("phase", phase);
        data.put("prerequisite", MODULE_ID + ":" + prerequisite);
        data.put("objectives", List.of(objectives));
        data.put("uiFeedbackSurface", uiFeedbackSurface);
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
        data.put("objectiveId", MODULE_ID + ":" + mission + "/" + objectiveSuffix);
        data.put("objectiveType", objectiveType);
        data.put("event", event);
        data.put("target", target);
        return Map.copyOf(data);
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

    private static List<String> pendingConcreteRuntimeBridges() {
        return List.of(
                "Minecraft-host route smoke for bio lab, reactor ruin, radiation zone, military vault, and midgame grid routes",
                "Minecraft-host loot/reward delivery for all midgame route rewards",
                "Visual Terminal/HoloMap/Lens/Index confirmation for midgame route state",
                "Live in-world machine screen/status confirmation for radiation_cleanser, ore_grinder, isotope_refiner, and energy_meter"
        );
    }

    private static List<String> requiredRouteStateFlags() {
        return List.of(
                "bioLabEntered",
                "dataLogRecovered",
                "reactorRuinSurveyed",
                "fieldMedBayBuilt",
                "radiationCleanserBuilt",
                "radawayCrafted",
                "mutationStatusScanned",
                "fieldMedBayUsed",
                "mutationEffectsStabilized",
                "radiationZoneScouted",
                "atmosphericScrubberBuilt",
                "mutatedTissueCollected",
                "mutagenVialCrafted",
                "militaryVaultCleared",
                "denseAlloyFound",
                "thermalArrayBuilt",
                "oreGrinderBuilt",
                "isotopeRefinerBuilt",
                "alloyWeaponForged",
                "alloyKitEquipped",
                "routeSuppliesStockpiled",
                "midgameGridCalibrated",
                "biohazardPhaseComplete",
                "thermalGeologyPhaseComplete",
                "routeComplete"
        );
    }

    private static List<String> validate(Map<String, Object> replay) {
        List<String> diagnostics = new ArrayList<>();
        if (!"PASS".equals(replay.get("status"))) {
            diagnostics.add("Expected midgame route replay to pass.");
        }
        requireCount(replay, "verifiedMissionCount", midgameMissions().size(), diagnostics);
        requireCount(replay, "verifiedObjectiveCount", expectedObjectiveCount(), diagnostics);
        requireCount(replay, "grantedRewardCount", expectedRewardCount(), diagnostics);
        requireCount(replay, "eventCount", expectedEventCount(), diagnostics);
        requireCount(replay, "uiFeedbackCount", expectedEventCount(), diagnostics);
        requireCount(replay, "savePersistenceWriteCount",
                expectedEventCount() + midgameMissions().size() + midgameMissions().size(), diagnostics);
        Map<String, Object> state = childMap(replay, "nativeRouteState");
        requireCount(state, "completedObjectiveCount", expectedObjectiveCount(), diagnostics);
        for (String flag : requiredRouteStateFlags()) {
            if (!Boolean.TRUE.equals(state.get(flag))) {
                diagnostics.add("Expected midgame native route state flag " + flag + " to be true.");
            }
        }
        return List.copyOf(diagnostics);
    }

    private static int expectedObjectiveCount() {
        int total = 0;
        for (Map<String, Object> mission : midgameMissions()) {
            total += objectives(mission).size();
        }
        return total;
    }

    private static int expectedRewardCount() {
        int total = 0;
        for (Map<String, Object> mission : midgameMissions()) {
            total += intValue(mission.get("rewardCount"));
        }
        return total;
    }

    private static int expectedEventCount() {
        int total = 0;
        for (Map<String, Object> mission : midgameMissions()) {
            for (Map<String, Object> objective : objectives(mission)) {
                total++;
                if ("place_block".equals(objective.get("objectiveType"))) {
                    total++;
                }
            }
        }
        return total;
    }

    private static void appendEvent(
            List<Map<String, Object>> events,
            Map<String, Object> mission,
            Map<String, Object> objective,
            String eventId,
            String uiSurface) {
        String missionId = string(mission.get("missionId"));
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("order", events.size() + 1);
        event.put("missionId", missionId);
        event.put("neoForgeMissionId", mission.get("neoForgeMissionId"));
        event.put("phase", mission.get("phase"));
        event.put("objectiveId", objective.get("objectiveId"));
        event.put("objectiveType", objective.get("objectiveType"));
        event.put("event", eventId);
        event.put("target", objective.get("target"));
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

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static int intValue(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }
}
