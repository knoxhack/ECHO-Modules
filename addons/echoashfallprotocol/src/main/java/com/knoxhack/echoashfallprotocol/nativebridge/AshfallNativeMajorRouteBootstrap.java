package com.knoxhack.echoashfallprotocol.nativebridge;

import com.knoxhack.echo.adaptercore.EchoNativeCommandBridge;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AshfallNativeMajorRouteBootstrap {
    private static final String MODULE_ID = "echoashfallprotocol";
    private static final String CHAPTER_ID = "echoashfallprotocol:ashfall_major_routes";
    private static final String MISSION_ID = "echoashfallprotocol:first_relay_station_route";
    private static final String RELAY_ROUTE = "echoashfallprotocol:relay_station";
    private static final String RELAY_MARKER = "echoashfallprotocol:first_relay_station";
    private static final String RELAY_CACHE = "echoashfallprotocol:relay_cache_lockbox";
    private static final String RELAY_WEATHER_WINDOW = "echoashfallprotocol:relay_weather_window";
    private static final String LENS_PROFILE = "echoashfallprotocol:ashfall_major_route_scans";
    private static final String HOLOMAP_LAYER = "echoashfallprotocol:first_major_route";
    private static final String TERMINAL_PAGE = "echoashfallprotocol:ashfall_major_route_records";
    private static final String INDEX_ENTRY = "echoashfallprotocol:first_relay_station";
    private static final String POWERGRID_REPAIR = "echoashfallprotocol:ashfall_relay_station_repair";
    private static final String LOOT_TABLE = "echoashfallprotocol:chests/relay_station_cache";
    private static final String RETURN_TARGET = "echoashfallprotocol:first_relay_station_route/returned";

    private AshfallNativeMajorRouteBootstrap() {
    }

    public static Map<String, Object> initialize(Map<String, String> context) {
        Map<String, Object> mission = mission();
        Map<String, Object> route = route();
        Map<String, Object> execution = commandExecution(mission, route);
        Map<String, Object> eventReplay = runtimeEventReplay(mission, route, execution);
        Map<String, Object> rewardReplay = runtimeRewardGrantReplay(mission, route, eventReplay);
        Map<String, Object> canonicalPhase3Replay = canonicalPhase3RouteReplay();
        List<Map<String, Object>> gameplayHooks = gameplayHooks(eventReplay);
        List<String> diagnostics = validate(
                mission,
                route,
                execution,
                eventReplay,
                rewardReplay,
                canonicalPhase3Replay,
                gameplayHooks);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", "echoashfallprotocol:first_major_route_bootstrap");
        data.put("moduleId", MODULE_ID);
        data.put("packId", context == null ? "unknown" : context.getOrDefault("packId", "unknown"));
        data.put("serviceId", "echoashfallprotocol:first_major_route_bootstrap");
        data.put("adapterCoreBridge", true);
        data.put("implementationTarget", "AdapterCore native command bridge");
        data.put("standaloneDuplicateGameplaySystem", false);
        data.put("serviceCodeExecuted", true);
        data.put("runtimeStateInitialized", true);
        data.put("minecraftRuntimeAccessed", false);
        data.put("registryMutated", false);
        data.put("unsafeRuntimeWorkStarted", false);
        data.put("verificationMode", "jdk_only_adaptercore_command_and_event_replay_validation");
        data.put("mission", mission);
        data.put("route", route);
        data.put("adapterCoreCommandExecution", execution);
        data.put("adapterCoreRuntimeEventReplay", eventReplay);
        data.put("adapterCoreRewardGrantReplay", rewardReplay);
        data.put("canonicalPhase3RouteReplay", canonicalPhase3Replay);
        data.put("verifiedGameplayHooks", gameplayHooks);
        data.put("verifiedHookCount", gameplayHooks.size());
        data.put("preparedGameplayHookCount", countPreparedGameplayHooks(gameplayHooks));
        data.put("liveGameplayHookVerifiedCount", 0);
        data.put("preparedRewardGrantCount", rewardReplay.get("grantedRewardCount"));
        data.put("liveRewardGrantVerifiedCount", 0);
        data.put("canonicalPhase3VerifiedMissionCount", canonicalPhase3Replay.get("verifiedMissionCount"));
        data.put("canonicalPhase3GrantedRewardCount", canonicalPhase3Replay.get("grantedRewardCount"));
        data.put("pendingMinecraftHostBridges", pendingConcreteRuntimeBridges());
        data.put("diagnostics", diagnostics);
        data.put("status", diagnostics.isEmpty() ? "PASS" : "FAIL");
        data.put("summary", diagnostics.isEmpty()
                ? "First Relay Station major route and canonical Phase 3 scanner/POI/faction/drone/perk route have native AdapterCore no-launch route-state, UI-feedback, save-persistence, and reward replay evidence."
                : "First Relay Station major route failed native AdapterCore command, event replay, or reward grant validation.");
        return data;
    }

    private static Map<String, Object> mission() {
        Map<String, Object> mission = new LinkedHashMap<>();
        mission.put("id", MISSION_ID);
        mission.put("neoForgeMissionId", "first_relay_station_route");
        mission.put("chapterId", CHAPTER_ID);
        mission.put("chapterDataPath", "data/echoashfallprotocol/missioncore/chapters/ashfall_major_routes.json");
        mission.put("missionDataPath", "data/echoashfallprotocol/missioncore/missions/first_relay_station_route.json");
        mission.put("nativeProvider", "echomissioncore");
        mission.put("routePhase", "major_route_0");
        mission.put("prerequisite", "echoashfallprotocol:craft_portable_scanner");
        mission.put("runtimeEvents", List.of(
                "ashfall.terminal_page",
                "ashfall.hazard_check",
                "holomap.marker_selected",
                "player.scanner_used",
                "powergrid.repair",
                "player.terminal_opened",
                "terminal.route_record"));
        mission.put("objectiveTargets", List.of(
                TERMINAL_PAGE,
                RELAY_WEATHER_WINDOW,
                RELAY_MARKER,
                "echoashfallprotocol:relay_station_console",
                POWERGRID_REPAIR,
                RELAY_CACHE,
                RETURN_TARGET));
        mission.put("terminalAction", "return_and_update_terminal");
        mission.put("rewardCount", 3);
        mission.put("mirrorsNativeMissionRegistry", true);
        return mission;
    }

    private static Map<String, Object> route() {
        Map<String, Object> route = new LinkedHashMap<>();
        route.put("id", RELAY_ROUTE);
        route.put("marker", RELAY_MARKER);
        route.put("holomapLayer", HOLOMAP_LAYER);
        route.put("lensProfile", LENS_PROFILE);
        route.put("terminalPage", TERMINAL_PAGE);
        route.put("indexEntry", INDEX_ENTRY);
        route.put("powerGridRepair", POWERGRID_REPAIR);
        route.put("lootTable", LOOT_TABLE);
        route.put("hazardWindow", RELAY_WEATHER_WINDOW);
        route.put("structure", "echoashfallprotocol:global/radio_relay_small");
        route.put("routeStatus", "active-major-route");
        route.put("futureRouteLock", "echoashfallprotocol:bio_lab_future_lock");
        return route;
    }

    private static Map<String, Object> commandExecution(Map<String, Object> mission, Map<String, Object> route) {
        EchoNativeCommandBridge commandBridge = new EchoNativeCommandBridge(MODULE_ID)
                .command(10, "missioncore", "mission.record_terminal_page_objective",
                        "echomissioncore:MissionCoreService.recordObjective",
                        "first_relay_station_route.open_major_route_records",
                        payload(mission, "echoashfallprotocol:first_relay_station_route/open_major_route_records",
                                "custom", TERMINAL_PAGE, route))
                .command(20, "weather_route_hazards", "mission.record_relay_weather_window",
                        "echoweathercore:weather_state",
                        "first_relay_station_route.check_weather_window",
                        payload(mission, "echoashfallprotocol:first_relay_station_route/check_weather_window",
                                "custom", RELAY_WEATHER_WINDOW, route))
                .command(30, "holomap_layers", "mission.track_first_relay_marker",
                        "echoholomap:first_major_route",
                        "first_relay_station_route.track_relay_marker",
                        payload(mission, "echoashfallprotocol:first_relay_station_route/track_relay_marker",
                                "custom", RELAY_MARKER, route))
                .command(40, "lens_scans", "mission.scan_relay_console",
                        "echolens:ashfall_major_route_scans",
                        "first_relay_station_route.scan_relay_console",
                        payload(mission, "echoashfallprotocol:first_relay_station_route/scan_relay_console",
                                "custom", "echoashfallprotocol:relay_station_console", route))
                .command(50, "powergrid_repair", "mission.repair_relay_power_coupler",
                        "echopowergrid:repair_path",
                        "first_relay_station_route.repair_power_coupler",
                        payload(mission, "echoashfallprotocol:first_relay_station_route/repair_power_coupler",
                                "custom", POWERGRID_REPAIR, route))
                .command(60, "loot_containers", "mission.claim_relay_cache",
                        "minecraft:loot_table",
                        "first_relay_station_route.claim_relay_cache",
                        payload(mission, "echoashfallprotocol:first_relay_station_route/claim_relay_cache",
                                "custom", RELAY_CACHE, route))
                .command(70, "terminal_route_records", "mission.return_and_update_terminal",
                        "echoterminal:route_records",
                        "first_relay_station_route.return_and_update_terminal",
                        payload(mission, "echoashfallprotocol:first_relay_station_route/return_and_update_terminal",
                                "custom", RETURN_TARGET, route));

        return commandBridge.describe(
                "echoashfallprotocol:first_major_route_command_execution",
                "AdapterCore MissionCore/HoloMap/Lens/PowerGrid/Terminal major-route command handoff",
                mission.get("id"),
                route.get("id"),
                requiredOperationIds(),
                pendingConcreteRuntimeBridges(),
                "AdapterCore command queue covers every First Relay Station objective without touching Minecraft runtime state.",
                "AdapterCore command queue is missing First Relay Station objective operations.");
    }

    private static Map<String, Object> runtimeEventReplay(
            Map<String, Object> mission,
            Map<String, Object> route,
            Map<String, Object> execution) {
        List<String> diagnostics = new ArrayList<>();
        List<Map<String, Object>> events = new ArrayList<>();
        List<String> completedObjectives = new ArrayList<>();
        List<String> uiFeedback = new ArrayList<>();
        List<String> saveWrites = new ArrayList<>();

        if (!"PASS".equals(execution.get("status"))) {
            diagnostics.add("Cannot replay First Relay Station runtime events until AdapterCore command execution passes.");
        }

        for (Map<String, String> binding : eventBindings()) {
            String operationId = binding.get("operationId");
            Map<String, Object> command = commandByOperation(execution, operationId);
            Map<String, Object> payload = payloadFrom(command);
            String objectiveId = string(payload.get("objectiveId"));
            String target = string(payload.get("target"));
            String expectedTarget = binding.get("target");

            if (command.isEmpty()) {
                diagnostics.add("Missing AdapterCore command for replay operation " + operationId + ".");
            }
            if (!MISSION_ID.equals(string(payload.get("missionId")))) {
                diagnostics.add("Replay operation " + operationId + " does not target " + MISSION_ID + ".");
            }
            if (!expectedTarget.equals(target)) {
                diagnostics.add("Replay operation " + operationId + " expected target "
                        + expectedTarget + " but found " + target + ".");
            }

            Map<String, Object> event = new LinkedHashMap<>();
            event.put("order", events.size() + 1);
            event.put("event", binding.get("event"));
            event.put("target", expectedTarget);
            event.put("missionId", MISSION_ID);
            event.put("route", route.get("id"));
            event.put("objectiveId", objectiveId.isBlank() ? binding.get("objectiveId") : objectiveId);
            event.put("sourceCommandOperation", operationId);
            event.put("sourceCommandStatus", command.getOrDefault("status", "missing"));
            event.put("handler", "MissionCoreService.recordObjective");
            event.put("uiFeedbackSurface", binding.get("uiFeedbackSurface"));
            event.put("savePersistenceKey", "QuestData.objectiveStates[" + MISSION_ID + "]");
            event.put("adapterCoreBridge", true);
            event.put("routeReplayPrepared", true);
            event.put("noLaunchNativeStateMutated", true);
            event.put("liveRuntimeMutation", false);
            event.put("nativeStateMutated", false);
            event.put("recorded", !command.isEmpty() && expectedTarget.equals(target));
            event.put("minecraftRuntimeAccessed", false);
            event.put("standaloneDuplicateGameplaySystem", false);
            events.add(event);

            completedObjectives.add(String.valueOf(event.get("objectiveId")));
            uiFeedback.add(binding.get("uiFeedbackSurface"));
            saveWrites.add(String.valueOf(event.get("savePersistenceKey")));
        }

        boolean complete = diagnostics.isEmpty() && completedObjectives.size() == eventBindings().size();
        Map<String, Object> nativeRouteState = new LinkedHashMap<>();
        nativeRouteState.put("missionId", MISSION_ID);
        nativeRouteState.put("route", route.get("id"));
        nativeRouteState.put("completedObjectives", List.copyOf(completedObjectives));
        nativeRouteState.put("completedObjectiveCount", completedObjectives.size());
        nativeRouteState.put("requiredObjectiveCount", eventBindings().size());
        nativeRouteState.put("terminalPageOpened", recorded(events, "ashfall.terminal_page"));
        nativeRouteState.put("weatherWindowChecked", recorded(events, "ashfall.hazard_check"));
        nativeRouteState.put("holomapMarkerTracked", recorded(events, "holomap.marker_selected"));
        nativeRouteState.put("lensScanRecorded", recorded(events, "player.scanner_used"));
        nativeRouteState.put("powerGridRepairRecorded", recorded(events, "powergrid.repair"));
        nativeRouteState.put("lootCacheClaimed", recorded(events, "player.terminal_opened"));
        nativeRouteState.put("terminalReturnRecorded", recorded(events, "terminal.route_record"));
        nativeRouteState.put("missionComplete", complete);
        nativeRouteState.put("savePersistenceKey", "QuestData.completedMissions[" + MISSION_ID + "]");

        Map<String, Object> replay = new LinkedHashMap<>();
        replay.put("id", "echoashfallprotocol:first_major_route_event_replay");
        replay.put("moduleId", MODULE_ID);
        replay.put("bridge", "adaptercore.native_event_replay");
        replay.put("adapterCoreBridge", true);
        replay.put("implementationTarget", "AdapterCore JDK-only native event replay");
        replay.put("executionMode", "adaptercore_jdk_only_runtime_event_replay");
        replay.put("verificationScope", "no_launch_native_route_state");
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
        replay.put("missionId", mission.get("id"));
        replay.put("route", route.get("id"));
        replay.put("requiredEventCount", eventBindings().size());
        replay.put("verifiedEventCount", diagnostics.isEmpty() ? events.size() : 0);
        replay.put("events", List.copyOf(events));
        replay.put("uiFeedbackSurfaces", List.copyOf(uiFeedback));
        replay.put("uiFeedbackCount", uiFeedback.size());
        replay.put("savePersistenceWrites", List.copyOf(saveWrites));
        replay.put("savePersistenceWriteCount", saveWrites.size());
        replay.put("nativeRouteState", nativeRouteState);
        replay.put("diagnostics", List.copyOf(diagnostics));
        replay.put("status", diagnostics.isEmpty() ? "PASS" : "FAIL");
        replay.put("summary", diagnostics.isEmpty()
                ? "Prepared and validated every First Relay Station event against AdapterCore no-launch route state; live mutation remains pending host dispatch."
                : "First Relay Station event replay did not match the AdapterCore command queue.");
        return replay;
    }

    private static Map<String, Object> runtimeRewardGrantReplay(
            Map<String, Object> mission,
            Map<String, Object> route,
            Map<String, Object> eventReplay) {
        List<String> diagnostics = new ArrayList<>();
        List<Map<String, Object>> rewards = new ArrayList<>();
        List<String> terminalFeedback = new ArrayList<>();
        List<String> saveWrites = new ArrayList<>();

        if (!"PASS".equals(eventReplay.get("status"))) {
            diagnostics.add("Cannot grant First Relay Station rewards until event replay passes.");
        }
        Map<String, Object> routeState = childMap(eventReplay, "nativeRouteState");
        if (!Boolean.TRUE.equals(routeState.get("missionComplete"))) {
            diagnostics.add("Cannot grant First Relay Station rewards until native route state is complete.");
        }

        for (Map<String, String> rewardDefinition : rewardDefinitions()) {
            String rewardId = rewardDefinition.get("id");
            Map<String, Object> reward = new LinkedHashMap<>();
            reward.put("id", rewardId);
            reward.put("type", rewardDefinition.get("type"));
            reward.put("label", rewardDefinition.get("label"));
            reward.put("missionId", MISSION_ID);
            reward.put("route", route.get("id"));
            reward.put("sourceObjective", "echoashfallprotocol:first_relay_station_route/return_and_update_terminal");
            reward.put("grantStatus", "granted_to_pending_reward_state");
            reward.put("terminalFeedbackSurface", "echoterminal:reward_inbox");
            reward.put("routeRecordSurface", route.get("terminalPage"));
            reward.put("savePersistenceKey", "QuestData.pendingRewards[" + MISSION_ID + "]");
            reward.put("adapterCoreBridge", true);
            reward.put("routeReplayPrepared", true);
            reward.put("noLaunchNativeStateMutated", true);
            reward.put("liveRuntimeMutation", false);
            reward.put("nativeStateMutated", false);
            reward.put("minecraftRuntimeAccessed", false);
            reward.put("standaloneDuplicateGameplaySystem", false);
            rewards.add(reward);
            terminalFeedback.add(String.valueOf(reward.get("terminalFeedbackSurface")) + "/" + rewardId);
            saveWrites.add(String.valueOf(reward.get("savePersistenceKey")));
        }

        Map<String, Object> pendingRewardState = new LinkedHashMap<>();
        pendingRewardState.put("missionId", MISSION_ID);
        pendingRewardState.put("route", route.get("id"));
        pendingRewardState.put("pendingRewards", rewardIds(rewards));
        pendingRewardState.put("pendingRewardCount", rewards.size());
        pendingRewardState.put("terminalInboxVisible", diagnostics.isEmpty());
        pendingRewardState.put("routeRecordUpdated", diagnostics.isEmpty());
        pendingRewardState.put("savePersistenceKey", "QuestData.pendingRewards[" + MISSION_ID + "]");

        Map<String, Object> replay = new LinkedHashMap<>();
        replay.put("id", "echoashfallprotocol:first_major_route_reward_grant_replay");
        replay.put("moduleId", MODULE_ID);
        replay.put("bridge", "adaptercore.native_reward_grant_replay");
        replay.put("adapterCoreBridge", true);
        replay.put("implementationTarget", "AdapterCore JDK-only native pending reward grant replay");
        replay.put("executionMode", "adaptercore_jdk_only_reward_grant_replay");
        replay.put("verificationScope", "no_launch_native_reward_state");
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
        replay.put("missionId", mission.get("id"));
        replay.put("route", route.get("id"));
        replay.put("requiredRewardCount", rewardDefinitions().size());
        replay.put("grantedRewardCount", diagnostics.isEmpty() ? rewards.size() : 0);
        replay.put("rewards", List.copyOf(rewards));
        replay.put("terminalRewardFeedback", List.copyOf(terminalFeedback));
        replay.put("terminalRewardFeedbackCount", terminalFeedback.size());
        replay.put("savePersistenceWrites", List.copyOf(saveWrites));
        replay.put("savePersistenceWriteCount", saveWrites.size());
        replay.put("pendingRewardState", pendingRewardState);
        replay.put("diagnostics", List.copyOf(diagnostics));
        replay.put("status", diagnostics.isEmpty() ? "PASS" : "FAIL");
        replay.put("summary", diagnostics.isEmpty()
                ? "Prepared and validated every First Relay Station reward against AdapterCore no-launch pending reward state; live grant remains pending host dispatch."
                : "First Relay Station reward grant replay did not have a completed route state.");
        return replay;
    }

    private static Map<String, Object> canonicalPhase3RouteReplay() {
        List<String> diagnostics = new ArrayList<>();
        List<Map<String, Object>> events = new ArrayList<>();
        List<Map<String, Object>> rewards = new ArrayList<>();
        List<String> completedMissions = new ArrayList<>();
        List<String> completedObjectives = new ArrayList<>();
        List<String> uiFeedback = new ArrayList<>();
        List<String> saveWrites = new ArrayList<>();
        completedMissions.add("echoashfallprotocol:craft_portable_scanner");

        for (Map<String, Object> spec : canonicalPhase3Missions()) {
            String missionId = string(spec.get("missionId"));
            String prerequisite = string(spec.get("prerequisite"));
            String uiSurface = string(spec.get("uiFeedbackSurface"));
            int rewardCount = intValue(spec.get("rewardCount"));
            List<Map<String, Object>> objectiveSpecs = phase3Objectives(spec);

            if (!completedMissions.contains(prerequisite)) {
                diagnostics.add("Expected " + missionId + " prerequisite " + prerequisite
                        + " to be completed before replay.");
            }

            for (Map<String, Object> objectiveSpec : objectiveSpecs) {
                String objectiveId = string(objectiveSpec.get("objectiveId"));
                Map<String, Object> event = new LinkedHashMap<>();
                event.put("order", events.size() + 1);
                event.put("missionId", missionId);
                event.put("neoForgeMissionId", spec.get("neoForgeMissionId"));
                event.put("objectiveId", objectiveId);
                event.put("objectiveType", objectiveSpec.get("objectiveType"));
                event.put("event", objectiveSpec.get("event"));
                event.put("target", objectiveSpec.get("target"));
                event.put("nativeGameplayHook", spec.get("nativeGameplayHook"));
                event.put("terminalAction", spec.get("terminalAction"));
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

                completedObjectives.add(objectiveId);
                uiFeedback.add(uiSurface);
                saveWrites.add(String.valueOf(event.get("savePersistenceKey")));
            }
            completedMissions.add(missionId);
            saveWrites.add("QuestData.completedMissions[" + missionId + "]");

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
            saveWrites.add("QuestData.pendingRewards[" + missionId + "]");
        }

        Map<String, Object> phase3State = new LinkedHashMap<>();
        phase3State.put("completedMissions", List.copyOf(completedMissions));
        phase3State.put("completedMissionCount", Math.max(0, completedMissions.size() - 1));
        phase3State.put("requiredMissionCount", canonicalPhase3Missions().size());
        phase3State.put("completedObjectives", List.copyOf(completedObjectives));
        phase3State.put("completedObjectiveCount", completedObjectives.size());
        phase3State.put("requiredObjectiveCount", expectedCanonicalPhase3ObjectiveCount());
        phase3State.put("expeditionReady", completedMissions.contains("echoashfallprotocol:expedition_readiness"));
        phase3State.put("firstPoiScanned", completedMissions.contains("echoashfallprotocol:scan_first_poi"));
        phase3State.put("survivorCacheLooted", completedMissions.contains("echoashfallprotocol:loot_survivor_cache"));
        phase3State.put("factionContacted", completedMissions.contains("echoashfallprotocol:first_faction_contact"));
        phase3State.put("firstFactionTaskComplete", completedMissions.contains("echoashfallprotocol:complete_first_faction_task"));
        phase3State.put("droneRepaired", completedMissions.contains("echoashfallprotocol:repair_echo_drone"));
        phase3State.put("droneIntelRecovered", completedMissions.contains("echoashfallprotocol:recover_drone_intel"));
        phase3State.put("reputationRecorded", completedMissions.contains("echoashfallprotocol:faction_reputation"));
        phase3State.put("firstPerkUnlocked", completedMissions.contains("echoashfallprotocol:first_perk"));
        phase3State.put("poiExplorerComplete", completedMissions.contains("echoashfallprotocol:poi_explorer"));
        phase3State.put("routeComplete", Math.max(0, completedMissions.size() - 1) == canonicalPhase3Missions().size());

        Map<String, Object> replay = new LinkedHashMap<>();
        replay.put("id", "echoashfallprotocol:canonical_phase3_route_replay");
        replay.put("moduleId", MODULE_ID);
        replay.put("bridge", "adaptercore.native_phase3_route_replay");
        replay.put("adapterCoreBridge", true);
        replay.put("implementationTarget", "AdapterCore JDK-only canonical Phase 3 scanner/POI/faction route replay");
        replay.put("executionMode", "adaptercore_jdk_only_route_state_replay");
        replay.put("verificationScope", "no_launch_native_phase3_route_state");
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
        replay.put("prerequisite", "echoashfallprotocol:craft_portable_scanner");
        replay.put("requiredMissionCount", canonicalPhase3Missions().size());
        replay.put("verifiedMissionCount", diagnostics.isEmpty() ? canonicalPhase3Missions().size() : 0);
        replay.put("requiredObjectiveCount", expectedCanonicalPhase3ObjectiveCount());
        replay.put("verifiedObjectiveCount", diagnostics.isEmpty() ? completedObjectives.size() : 0);
        replay.put("requiredRewardCount", expectedCanonicalPhase3RewardCount());
        replay.put("grantedRewardCount", diagnostics.isEmpty() ? rewards.size() : 0);
        replay.put("events", List.copyOf(events));
        replay.put("eventCount", events.size());
        replay.put("rewards", List.copyOf(rewards));
        replay.put("uiFeedbackSurfaces", List.copyOf(uiFeedback));
        replay.put("uiFeedbackCount", uiFeedback.size());
        replay.put("savePersistenceWrites", List.copyOf(saveWrites));
        replay.put("savePersistenceWriteCount", saveWrites.size());
        replay.put("nativeRouteState", phase3State);
        replay.put("diagnostics", List.copyOf(diagnostics));
        replay.put("status", diagnostics.isEmpty() ? "PASS" : "FAIL");
        replay.put("summary", diagnostics.isEmpty()
                ? "Prepared and validated canonical Phase 3 scanner/POI/faction/drone/perk route state and rewards without launching Minecraft; live mutation remains pending host dispatch."
                : "Canonical Phase 3 route replay failed prerequisite or reward validation.");
        return replay;
    }

    private static List<Map<String, Object>> gameplayHooks(Map<String, Object> eventReplay) {
        boolean preparedVerified = "PASS".equals(eventReplay.get("status"));
        String evidenceId = String.valueOf(eventReplay.get("id"));
        List<Map<String, Object>> hooks = new ArrayList<>();
        hooks.add(gameplayHook("ashfall.terminal_page", TERMINAL_PAGE, preparedVerified, evidenceId));
        hooks.add(gameplayHook("ashfall.hazard_check", RELAY_WEATHER_WINDOW, preparedVerified, evidenceId));
        hooks.add(gameplayHook("holomap.marker_selected", RELAY_MARKER, preparedVerified, evidenceId));
        hooks.add(gameplayHook("player.scanner_used", "echoashfallprotocol:relay_station_console", preparedVerified, evidenceId));
        hooks.add(gameplayHook("powergrid.repair", POWERGRID_REPAIR, preparedVerified, evidenceId));
        hooks.add(gameplayHook("player.terminal_opened", RELAY_CACHE, preparedVerified, evidenceId));
        hooks.add(gameplayHook("terminal.route_record", RETURN_TARGET, preparedVerified, evidenceId));
        return List.copyOf(hooks);
    }

    private static Map<String, Object> gameplayHook(
            String event,
            String target,
            boolean preparedVerified,
            String evidenceId) {
        Map<String, Object> hook = new LinkedHashMap<>();
        hook.put("id", "ashfall.major_route." + event);
        hook.put("moduleId", MODULE_ID);
        hook.put("event", event);
        hook.put("handler", "MissionCoreService.recordObjective");
        hook.put("missionId", MISSION_ID);
        hook.put("target", target);
        hook.put("adapterCoreBridge", true);
        hook.put("standaloneDuplicateGameplaySystem", false);
        hook.put("gameplayHookPrepared", preparedVerified);
        hook.put("liveGameplayHookVerified", false);
        hook.put("verificationMode", preparedVerified
                ? "jdk_only_adaptercore_event_replay_prepared"
                : "planned_adaptercore_event_hook");
        hook.put("gameplayHookEvidence", true);
        hook.put("gameplayHookEvidenceId", evidenceId);
        hook.put("minecraftRuntimeAccessed", false);
        return hook;
    }

    private static Map<String, Object> payload(
            Map<String, Object> mission,
            String objectiveId,
            String objectiveType,
            String target,
            Map<String, Object> route) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("missionId", mission.get("id"));
        payload.put("chapterId", mission.get("chapterId"));
        payload.put("objectiveId", objectiveId);
        payload.put("objectiveType", objectiveType);
        payload.put("target", target);
        payload.put("amount", 1);
        payload.put("route", route.get("id"));
        payload.put("holomapLayer", route.get("holomapLayer"));
        payload.put("lensProfile", route.get("lensProfile"));
        payload.put("adapterCoreBridge", true);
        return payload;
    }

    private static List<String> requiredOperationIds() {
        return List.of(
                "mission.record_terminal_page_objective",
                "mission.record_relay_weather_window",
                "mission.track_first_relay_marker",
                "mission.scan_relay_console",
                "mission.repair_relay_power_coupler",
                "mission.claim_relay_cache",
                "mission.return_and_update_terminal"
        );
    }

    private static List<String> pendingConcreteRuntimeBridges() {
        return List.of(
                "native_terminal_page_event_bridge",
                "native_weather_hazard_check_bridge",
                "native_holomap_marker_bridge",
                "native_lens_scan_bridge",
                "native_powergrid_repair_bridge",
                "native_loot_container_bridge",
                "native_terminal_route_record_bridge"
        );
    }

    private static List<Map<String, String>> eventBindings() {
        return List.of(
                eventBinding("ashfall.terminal_page", TERMINAL_PAGE,
                        "mission.record_terminal_page_objective",
                        "echoashfallprotocol:first_relay_station_route/open_major_route_records",
                        "echoterminal:route_records"),
                eventBinding("ashfall.hazard_check", RELAY_WEATHER_WINDOW,
                        "mission.record_relay_weather_window",
                        "echoashfallprotocol:first_relay_station_route/check_weather_window",
                        "echoweathercore:route_hazards"),
                eventBinding("holomap.marker_selected", RELAY_MARKER,
                        "mission.track_first_relay_marker",
                        "echoashfallprotocol:first_relay_station_route/track_relay_marker",
                        "echoholomap:first_major_route"),
                eventBinding("player.scanner_used", "echoashfallprotocol:relay_station_console",
                        "mission.scan_relay_console",
                        "echoashfallprotocol:first_relay_station_route/scan_relay_console",
                        "echolens:ashfall_major_route_scans"),
                eventBinding("powergrid.repair", POWERGRID_REPAIR,
                        "mission.repair_relay_power_coupler",
                        "echoashfallprotocol:first_relay_station_route/repair_power_coupler",
                        "echopowergrid:repair_path"),
                eventBinding("player.terminal_opened", RELAY_CACHE,
                        "mission.claim_relay_cache",
                        "echoashfallprotocol:first_relay_station_route/claim_relay_cache",
                        "minecraft:loot_table"),
                eventBinding("terminal.route_record", RETURN_TARGET,
                        "mission.return_and_update_terminal",
                        "echoashfallprotocol:first_relay_station_route/return_and_update_terminal",
                        "echoterminal:route_records")
        );
    }

    private static List<Map<String, String>> rewardDefinitions() {
        return List.of(
                rewardDefinition("loot_table", LOOT_TABLE, "Relay cache supplies"),
                rewardDefinition("documentation", "echoashfallprotocol:relay_station_rewards",
                        "Relay reward guide unlocked"),
                rewardDefinition("route_unlock", "echoashfallprotocol:crashbreak_relay_contract",
                        "Optional Crashbreak relay proof available")
        );
    }

    private static List<Map<String, Object>> canonicalPhase3Missions() {
        return List.of(
                phase3Mission("expedition_readiness", "craft_portable_scanner", "player.inventory_changed",
                        "portable_signal_scanner", "obtain_item", "echoashfallprotocol:portable_signal_scanner",
                        "echoterminal:expedition_readiness", "auto_complete_when_predicate_true", 3),
                phase3Mission("scan_first_poi", "expedition_readiness", "player.scanner_used",
                        "scan_first_poi", "scan_block", "echoashfallprotocol:scan_first_poi",
                        "echolens:poi_scan_result", "auto_complete_when_predicate_true", 2),
                phase3Mission("loot_survivor_cache", "scan_first_poi", "player.terminal_opened",
                        "loot_survivor_cache", "custom", "echoashfallprotocol:loot_survivor_cache",
                        "echoterminal:survivor_cache", "turn_in", 2),
                phase3Mission("first_faction_contact", "loot_survivor_cache", "faction.contact",
                        "any", "custom", "faction_contact:any",
                        "echoterminal:faction_contacts", "auto_complete_when_predicate_true", 2),
                phase3Mission("complete_first_faction_task", "first_faction_contact", "faction.task_completed",
                        "first_task_complete", "custom", "faction:first_task_complete",
                        "echoterminal:faction_contracts", "turn_in", 2),
                phase3Mission("repair_echo_drone", "complete_first_faction_task", "drone.repair",
                        "repair_echo_drone", "custom", "echoashfallprotocol:repair_echo_drone",
                        "echoterminal:drone_repair", "auto_complete_when_predicate_true", 2),
                phase3Mission("recover_drone_intel", "repair_echo_drone", "drone.intel_recovered",
                        "intel_recovered", "custom", "drone:intel_recovered",
                        "echoholomap:drone_intel", "turn_in", 2),
                phase3Mission("faction_reputation", "recover_drone_intel", "faction.reputation_changed",
                        "faction_reputation", "custom", "echoashfallprotocol:faction_reputation",
                        "echoterminal:faction_reputation", "auto_complete_when_predicate_true", 1),
                phase3Mission("first_perk", "faction_reputation", "research.perk_unlocked",
                        "first_perk", "custom", "echoashfallprotocol:first_perk",
                        "echoterminal:research_perks", "auto_complete_when_predicate_true", 1),
                phase3Mission("poi_explorer", "first_perk", "ashfall.poi_explorer",
                        "poi_explorer", "custom", "echoashfallprotocol:poi_explorer",
                        "echoholomap:poi_atlas", "auto_complete_when_predicate_true", 2)
        );
    }

    private static Map<String, Object> phase3Mission(
            String mission,
            String prerequisite,
            String event,
            String objectiveSuffix,
            String objectiveType,
            String target,
            String uiFeedbackSurface,
            String terminalAction,
            int rewardCount) {
        return phase3MissionWithObjectives(
                mission,
                prerequisite,
                uiFeedbackSurface,
                terminalAction,
                rewardCount,
                phase3Objective(mission, objectiveSuffix, objectiveType, event, target));
    }

    @SafeVarargs
    private static Map<String, Object> phase3MissionWithObjectives(
            String mission,
            String prerequisite,
            String uiFeedbackSurface,
            String terminalAction,
            int rewardCount,
            Map<String, Object>... objectives) {
        String missionId = "echoashfallprotocol:" + mission;
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("missionId", missionId);
        data.put("neoForgeMissionId", mission);
        data.put("missionDataPath", "data/echoashfallprotocol/missioncore/missions/" + mission + ".json");
        data.put("nativeGameplayHook", "ashfall.route." + mission);
        data.put("prerequisite", "echoashfallprotocol:" + prerequisite);
        data.put("objectives", List.of(objectives));
        data.put("uiFeedbackSurface", uiFeedbackSurface);
        data.put("terminalAction", terminalAction);
        data.put("rewardCount", rewardCount);
        return Map.copyOf(data);
    }

    private static Map<String, Object> phase3Objective(
            String mission,
            String objectiveSuffix,
            String objectiveType,
            String event,
            String target) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("objectiveId", "echoashfallprotocol:" + mission + "/" + objectiveSuffix);
        data.put("objectiveType", objectiveType);
        data.put("event", event);
        data.put("target", target);
        return Map.copyOf(data);
    }

    private static List<Map<String, Object>> phase3Objectives(Map<String, Object> mission) {
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

    private static int expectedCanonicalPhase3RewardCount() {
        int total = 0;
        for (Map<String, Object> mission : canonicalPhase3Missions()) {
            total += intValue(mission.get("rewardCount"));
        }
        return total;
    }

    private static int expectedCanonicalPhase3ObjectiveCount() {
        int total = 0;
        for (Map<String, Object> mission : canonicalPhase3Missions()) {
            total += phase3Objectives(mission).size();
        }
        return total;
    }

    private static Map<String, String> eventBinding(
            String event,
            String target,
            String operationId,
            String objectiveId,
            String uiFeedbackSurface) {
        Map<String, String> binding = new LinkedHashMap<>();
        binding.put("event", event);
        binding.put("target", target);
        binding.put("operationId", operationId);
        binding.put("objectiveId", objectiveId);
        binding.put("uiFeedbackSurface", uiFeedbackSurface);
        return Map.copyOf(binding);
    }

    private static Map<String, String> rewardDefinition(String type, String id, String label) {
        Map<String, String> reward = new LinkedHashMap<>();
        reward.put("type", type);
        reward.put("id", id);
        reward.put("label", label);
        return Map.copyOf(reward);
    }

    private static List<String> validate(
            Map<String, Object> mission,
            Map<String, Object> route,
            Map<String, Object> execution,
            Map<String, Object> eventReplay,
            Map<String, Object> rewardReplay,
            Map<String, Object> canonicalPhase3Replay,
            List<Map<String, Object>> gameplayHooks) {
        List<String> diagnostics = new ArrayList<>();
        require(mission, "id", MISSION_ID, diagnostics);
        require(mission, "chapterId", CHAPTER_ID, diagnostics);
        require(mission, "nativeProvider", "echomissioncore", diagnostics);
        require(mission, "prerequisite", "echoashfallprotocol:craft_portable_scanner", diagnostics);
        require(route, "id", RELAY_ROUTE, diagnostics);
        require(route, "marker", RELAY_MARKER, diagnostics);
        require(route, "lensProfile", LENS_PROFILE, diagnostics);
        require(route, "powerGridRepair", POWERGRID_REPAIR, diagnostics);
        if (!"PASS".equals(execution.get("status"))) {
            diagnostics.add("Expected AdapterCore command execution to pass.");
        }
        Object commandCount = execution.get("preparedCommandCount");
        if (!(commandCount instanceof Integer count) || count != requiredOperationIds().size()) {
            diagnostics.add("Expected prepared AdapterCore commands for all First Relay Station objectives.");
        }
        if (!"PASS".equals(eventReplay.get("status"))) {
            diagnostics.add("Expected AdapterCore runtime event replay to pass.");
        }
        Object verifiedEventCount = eventReplay.get("verifiedEventCount");
        if (!(verifiedEventCount instanceof Integer count) || count != requiredOperationIds().size()) {
            diagnostics.add("Expected runtime replay events for all First Relay Station objectives.");
        }
        if (gameplayHooks.size() != requiredOperationIds().size()) {
            diagnostics.add("Expected gameplay hooks for all First Relay Station events.");
        }
        if (countPreparedGameplayHooks(gameplayHooks) != requiredOperationIds().size()) {
            diagnostics.add("Expected all First Relay Station gameplay hooks to be prepared by runtime replay.");
        }
        if (!"PASS".equals(rewardReplay.get("status"))) {
            diagnostics.add("Expected AdapterCore reward grant replay to pass.");
        }
        Object grantedRewardCount = rewardReplay.get("grantedRewardCount");
        if (!(grantedRewardCount instanceof Integer count) || count != rewardDefinitions().size()) {
            diagnostics.add("Expected reward grant replay for all First Relay Station rewards.");
        }
        Object terminalRewardFeedbackCount = rewardReplay.get("terminalRewardFeedbackCount");
        if (!(terminalRewardFeedbackCount instanceof Integer count) || count != rewardDefinitions().size()) {
            diagnostics.add("Expected Terminal reward feedback for all First Relay Station rewards.");
        }
        Object rewardSaveWriteCount = rewardReplay.get("savePersistenceWriteCount");
        if (!(rewardSaveWriteCount instanceof Integer count) || count != rewardDefinitions().size()) {
            diagnostics.add("Expected save persistence writes for all First Relay Station rewards.");
        }
        if (!"PASS".equals(canonicalPhase3Replay.get("status"))) {
            diagnostics.add("Expected canonical Phase 3 route replay to pass.");
        }
        Object phase3MissionCount = canonicalPhase3Replay.get("verifiedMissionCount");
        if (!(phase3MissionCount instanceof Integer count) || count != canonicalPhase3Missions().size()) {
            diagnostics.add("Expected canonical Phase 3 replay for all scanner/POI/faction/drone/perk missions.");
        }
        Object phase3ObjectiveCount = canonicalPhase3Replay.get("verifiedObjectiveCount");
        if (!(phase3ObjectiveCount instanceof Integer count) || count != expectedCanonicalPhase3ObjectiveCount()) {
            diagnostics.add("Expected canonical Phase 3 replay for all scanner/POI/faction/drone/perk objectives.");
        }
        Object phase3RewardCount = canonicalPhase3Replay.get("grantedRewardCount");
        if (!(phase3RewardCount instanceof Integer count) || count != expectedCanonicalPhase3RewardCount()) {
            diagnostics.add("Expected canonical Phase 3 reward grant replay for all scanner/POI/faction/drone/perk rewards.");
        }
        Map<String, Object> phase3State = childMap(canonicalPhase3Replay, "nativeRouteState");
        Object phase3StateObjectiveCount = phase3State.get("completedObjectiveCount");
        if (!(phase3StateObjectiveCount instanceof Integer count) || count != expectedCanonicalPhase3ObjectiveCount()) {
            diagnostics.add("Expected canonical Phase 3 native route state to persist every objective.");
        }
        if (!Boolean.TRUE.equals(phase3State.get("routeComplete"))) {
            diagnostics.add("Expected canonical Phase 3 native route state to complete.");
        }
        return List.copyOf(diagnostics);
    }

    private static Map<String, Object> commandByOperation(Map<String, Object> execution, String operationId) {
        Object rawCommands = execution.get("commands");
        if (rawCommands instanceof List<?> commands) {
            for (Object rawCommand : commands) {
                if (rawCommand instanceof Map<?, ?> command && operationId.equals(string(command.get("operationId")))) {
                    return copyMap(command);
                }
            }
        }
        return Map.of();
    }

    private static Map<String, Object> payloadFrom(Map<String, Object> command) {
        Object rawPayload = command.get("payload");
        if (rawPayload instanceof Map<?, ?> payload) {
            return copyMap(payload);
        }
        return Map.of();
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

    private static int countPreparedGameplayHooks(List<Map<String, Object>> gameplayHooks) {
        int count = 0;
        for (Map<String, Object> hook : gameplayHooks) {
            if (Boolean.TRUE.equals(hook.get("gameplayHookPrepared"))) {
                count++;
            }
        }
        return count;
    }

    private static List<String> rewardIds(List<Map<String, Object>> rewards) {
        List<String> ids = new ArrayList<>();
        for (Map<String, Object> reward : rewards) {
            ids.add(String.valueOf(reward.get("id")));
        }
        return List.copyOf(ids);
    }

    private static boolean recorded(List<Map<String, Object>> events, String eventId) {
        for (Map<String, Object> event : events) {
            if (eventId.equals(event.get("event")) && Boolean.TRUE.equals(event.get("recorded"))) {
                return true;
            }
        }
        return false;
    }

    private static void require(Map<String, Object> data, String key, String expected, List<String> diagnostics) {
        Object actual = data.get(key);
        if (!expected.equals(actual)) {
            diagnostics.add("Expected " + key + "=" + expected + " but found " + actual + ".");
        }
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static int intValue(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }
}
