package com.knoxhack.echoashfallprotocol.nativebridge;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AshfallNativeFirstSpawnEquivalenceHarness {
    private static final String MODULE_ID = "echoashfallprotocol";

    private AshfallNativeFirstSpawnEquivalenceHarness() {
    }

    public static Map<String, Object> evaluate(
            Map<String, Object> firstJoinProfile,
            Map<String, Object> minecraftRuntimeAdapterReadiness,
            Map<String, Object> existingPlayerRepairRuntime,
            Map<String, Object> gameplayLifecycleHostRuntime,
            Map<String, Object> firstJoinParityVerifier) {
        Map<String, Object> hostSnapshot = childMap(gameplayLifecycleHostRuntime, "hostStateSnapshot");
        List<Map<String, Object>> testCases = List.of(
                newPlayerSpawnCase(hostSnapshot),
                returningPlayerRepairCase(existingPlayerRepairRuntime));
        List<String> diagnostics = validate(
                firstJoinProfile,
                minecraftRuntimeAdapterReadiness,
                existingPlayerRepairRuntime,
                gameplayLifecycleHostRuntime,
                firstJoinParityVerifier,
                testCases);

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("id", "echoashfallprotocol:first_spawn_equivalence_harness");
        report.put("moduleId", MODULE_ID);
        report.put("bridge", "adaptercore.first_spawn_equivalence_harness");
        report.put("adapterCoreBridge", true);
        report.put("implementationTarget", "AdapterCore no-launch first-spawn equivalence harness for future native host integration tests");
        report.put("sourceRuntimeHandler", "PlayerStartingKitHandler.onPlayerLoggedIn");
        report.put("sourceProfile", value(firstJoinProfile, "id"));
        report.put("sourceHostRuntime", value(gameplayLifecycleHostRuntime, "id"));
        report.put("sourceParityVerifier", value(firstJoinParityVerifier, "id"));
        report.put("executionMode", "adaptercore_jdk_first_spawn_equivalence_harness");
        report.put("standaloneDuplicateGameplaySystem", false);
        report.put("runtimeStateConsumed", false);
        report.put("noLaunchRuntimeStateValidated", diagnostics.isEmpty());
        report.put("nativeStateConsumed", false);
        report.put("nativeStateValidatedForHostDispatch", diagnostics.isEmpty());
        report.put("liveRuntimeMutationConsumed", false);
        report.put("minecraftRuntimeAccessed", false);
        report.put("minecraftRuntimeMutated", false);
        report.put("minecraftRegistryMutated", false);
        report.put("safeNativeHostAdaptersRequired", true);
        report.put("liveSpawnIntegrationTestReady", false);
        report.put("liveSpawnIntegrationTestBlockedBy", List.of(
                "minecraft_backed_inventory_writer",
                "minecraft_backed_structure_placer",
                "minecraft_backed_player_positioner",
                "minecraft_backed_respawn_binder",
                "minecraft_backed_player_state_writer",
                "minecraft_backed_advancement_granter",
                "minecraft_backed_screen_packet_sender",
                "minecraft_backed_hud_notification_sender"));
        report.put("noLaunchEquivalenceVerified", diagnostics.isEmpty());
        report.put("testCaseCount", testCases.size());
        report.put("testCases", testCases);
        report.put("diagnostics", List.copyOf(diagnostics));
        report.put("status", diagnostics.isEmpty() ? "PASS" : "FAIL");
        report.put("summary", diagnostics.isEmpty()
                ? "AdapterCore first-spawn equivalence harness has a passing no-launch new-player spawn case and returning-player repair case; promotion to live integration remains gated on safe native host adapters."
                : "AdapterCore first-spawn equivalence harness found drift in no-launch first-spawn expectations.");
        return Map.copyOf(report);
    }

    private static Map<String, Object> newPlayerSpawnCase(Map<String, Object> hostSnapshot) {
        Map<String, Object> before = new LinkedHashMap<>();
        before.put("playerHasReceivedKit", false);
        before.put("echoterminalLoaded", true);
        before.put("echowikiLoaded", true);
        before.put("personalDropPodPresent", false);
        before.put("respawnBound", false);
        before.put("minecraftRuntimeAccessed", false);

        Map<String, Object> expectedAfter = new LinkedHashMap<>();
        expectedAfter.put("inventoryItemIds", List.of("echoashfallprotocol:field_manual", "echoterminal:echo_terminal_remote"));
        expectedAfter.put("structureIds", List.of("echoashfallprotocol:drop_pod"));
        expectedAfter.put("teleportAnchor", "personal_starting_drop_pod.interior");
        expectedAfter.put("respawnAnchor", "personal_starting_drop_pod.interior");
        expectedAfter.put("firstJoinFlagValue", true);
        expectedAfter.put("questDropPodInitialized", true);
        expectedAfter.put("advancements", List.of("echoashfallprotocol:find_drop_pod"));
        expectedAfter.put("welcomeScreen", "echoashfallprotocol:welcome_screen");
        expectedAfter.put("missionTrackerLine", "Place an Ash Campfire near the crash site");
        expectedAfter.put("hazardReadoutLine", "AIR stable; hazards marked.");
        expectedAfter.put("terminalCard", "ashfall:first_ten_minutes");
        expectedAfter.put("wikiGuide", "echowiki:ashfall");
        expectedAfter.put("atmosphereProfile", "echoatmospherecore:storm_visibility");

        List<String> assertions = new ArrayList<>();
        assertContains(assertions, hostSnapshot.get("inventoryItemIds"), "echoashfallprotocol:field_manual", "starter_note_inventory");
        assertContains(assertions, hostSnapshot.get("inventoryItemIds"), "echoterminal:echo_terminal_remote", "terminal_remote_inventory");
        assertContains(assertions, hostSnapshot.get("structureIds"), "echoashfallprotocol:drop_pod", "drop_pod_structure");
        assertEquals(assertions, hostSnapshot.get("teleportAnchor"), "personal_starting_drop_pod.interior", "teleport_anchor");
        assertEquals(assertions, hostSnapshot.get("respawnAnchor"), "personal_starting_drop_pod.interior", "respawn_anchor");
        assertEquals(assertions, hostSnapshot.get("firstJoinFlagValue"), true, "first_join_flag");
        assertEquals(assertions, hostSnapshot.get("questDropPodInitialized"), true, "drop_pod_initialized");
        assertContains(assertions, hostSnapshot.get("advancements"), "echoashfallprotocol:find_drop_pod", "advancement");
        assertEquals(assertions, hostSnapshot.get("welcomeScreen"), "echoashfallprotocol:welcome_screen", "welcome_screen");
        assertEquals(assertions, hostSnapshot.get("missionTrackerLine"), "Place an Ash Campfire near the crash site", "mission_tracker");
        assertEquals(assertions, hostSnapshot.get("hazardReadoutLine"), "AIR stable; hazards marked.", "hazard_readout");
        assertEquals(assertions, hostSnapshot.get("terminalCard"), "ashfall:first_ten_minutes", "terminal_card");
        assertEquals(assertions, hostSnapshot.get("wikiGuide"), "echowiki:ashfall", "wiki_guide");
        assertEquals(assertions, hostSnapshot.get("atmosphereProfile"), "echoatmospherecore:storm_visibility", "atmosphere_profile");

        return testCase("new_player_first_native_spawn", before, expectedAfter, assertions);
    }

    private static Map<String, Object> returningPlayerRepairCase(Map<String, Object> existingPlayerRepairRuntime) {
        Map<String, Object> before = new LinkedHashMap<>();
        before.put("playerHasReceivedKit", true);
        before.put("currentYBelowMinimumStartingSurface", true);
        before.put("respawnMissing", true);
        before.put("terminalRemoteMissing", true);
        before.put("minecraftRuntimeAccessed", false);

        Map<String, Object> expectedAfter = new LinkedHashMap<>();
        expectedAfter.put("repairActions", List.of(
                "rescue_underground_starting_pod_below_y_48",
                "repair_missing_drop_pod_respawn",
                "reissue_terminal_remote_if_available"));
        expectedAfter.put("scenarioIds", List.of(
                "existing_player_underground_pod_rescue",
                "existing_player_missing_respawn_repair",
                "existing_player_terminal_remote_reissue"));

        List<String> assertions = new ArrayList<>();
        assertEquals(assertions, existingPlayerRepairRuntime.get("status"), "PASS", "repair_runtime_status");
        assertEquals(assertions, existingPlayerRepairRuntime.get("undergroundPodRescueCovered"), true, "underground_repair");
        assertEquals(assertions, existingPlayerRepairRuntime.get("missingRespawnRepairCovered"), true, "respawn_repair");
        assertEquals(assertions, existingPlayerRepairRuntime.get("terminalRemoteReissueCovered"), true, "terminal_reissue");
        assertEquals(assertions, existingPlayerRepairRuntime.get("scenarioCount"), 3, "repair_scenarios");

        return testCase("returning_player_repair_path", before, expectedAfter, assertions);
    }

    private static Map<String, Object> testCase(
            String id,
            Map<String, Object> before,
            Map<String, Object> expectedAfter,
            List<String> assertions) {
        Map<String, Object> testCase = new LinkedHashMap<>();
        testCase.put("id", id);
        testCase.put("adapterCoreTestCase", true);
        testCase.put("sourceRuntimeHandler", "PlayerStartingKitHandler.onPlayerLoggedIn");
        testCase.put("before", Map.copyOf(before));
        testCase.put("expectedAfter", Map.copyOf(expectedAfter));
        testCase.put("assertions", List.copyOf(assertions));
        testCase.put("assertionCount", assertions.size());
        testCase.put("minecraftRuntimeAccessed", false);
        testCase.put("status", assertions.stream().allMatch("PASS"::equals) ? "PASS" : "FAIL");
        return Map.copyOf(testCase);
    }

    private static List<String> validate(
            Map<String, Object> firstJoinProfile,
            Map<String, Object> minecraftRuntimeAdapterReadiness,
            Map<String, Object> existingPlayerRepairRuntime,
            Map<String, Object> gameplayLifecycleHostRuntime,
            Map<String, Object> firstJoinParityVerifier,
            List<Map<String, Object>> testCases) {
        List<String> diagnostics = new ArrayList<>();
        if (!"PlayerStartingKitHandler.onPlayerLoggedIn".equals(value(firstJoinProfile, "sourceParity"))) {
            diagnostics.add("First-join profile source handler drifted.");
        }
        if (!"PASS".equals(value(minecraftRuntimeAdapterReadiness, "status"))) {
            diagnostics.add("Minecraft runtime adapter readiness did not pass.");
        }
        if (!Boolean.TRUE.equals(minecraftRuntimeAdapterReadiness.get("nativeHostImplementationMetadataReady"))) {
            diagnostics.add("Minecraft runtime adapter readiness metadata is not ready for native host implementation.");
        }
        if (!"PASS".equals(value(existingPlayerRepairRuntime, "status"))) {
            diagnostics.add("Existing-player repair runtime did not pass.");
        }
        if (!"PASS".equals(value(gameplayLifecycleHostRuntime, "status"))) {
            diagnostics.add("Gameplay lifecycle host runtime did not pass.");
        }
        if (!"PASS".equals(value(firstJoinParityVerifier, "status"))) {
            diagnostics.add("First-join parity verifier did not pass.");
        }
        if (!Boolean.TRUE.equals(firstJoinParityVerifier.get("parityVerified"))) {
            diagnostics.add("First-join parity verifier did not prove parity.");
        }
        if (testCases.size() != 2) {
            diagnostics.add("Expected two first-spawn equivalence test cases.");
        }
        for (Map<String, Object> testCase : testCases) {
            if (!"PASS".equals(testCase.get("status"))) {
                diagnostics.add("Equivalence test case " + testCase.get("id") + " did not pass.");
            }
            if (Boolean.TRUE.equals(testCase.get("minecraftRuntimeAccessed"))) {
                diagnostics.add("Equivalence test case " + testCase.get("id") + " accessed Minecraft runtime.");
            }
        }
        return List.copyOf(diagnostics);
    }

    private static void assertEquals(List<String> assertions, Object actual, Object expected, String assertionId) {
        assertions.add(expected.equals(actual) ? "PASS" : "FAIL:" + assertionId);
    }

    private static void assertContains(List<String> assertions, Object actual, String expected, String assertionId) {
        if (actual instanceof Iterable<?> values) {
            for (Object value : values) {
                if (expected.equals(value)) {
                    assertions.add("PASS");
                    return;
                }
            }
        }
        assertions.add("FAIL:" + assertionId);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> childMap(Map<String, Object> parent, String key) {
        if (parent != null && parent.get(key) instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private static String value(Map<String, Object> map, String key) {
        Object value = map == null ? null : map.get(key);
        return value == null ? "" : String.valueOf(value);
    }
}
