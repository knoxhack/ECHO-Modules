package com.knoxhack.echoashfallprotocol.nativebridge;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AshfallNativeFirstJoinParityVerifier {
    private static final String MODULE_ID = "echoashfallprotocol";

    private AshfallNativeFirstJoinParityVerifier() {
    }

    public static Map<String, Object> verify(
            Map<String, Object> firstJoinProfile,
            Map<String, Object> firstJoinExecution,
            Map<String, Object> gameplayLifecycleHostPlan,
            Map<String, Object> gameplayLifecycleHostRuntime) {
        List<Map<String, Object>> requirements = new ArrayList<>();
        Map<String, Object> snapshot = childMap(gameplayLifecycleHostRuntime, "hostStateSnapshot");

        requireEquals(requirements, "source_handler", firstJoinProfile.get("sourceParity"),
                "PlayerStartingKitHandler.onPlayerLoggedIn");
        requireTrue(requirements, "profile_mirrors_native_contract", firstJoinProfile.get("mirrorsNativeContract"));
        requireEquals(requirements, "adaptercore_execution_status", firstJoinExecution.get("status"), "PASS");
        requireEquals(requirements, "host_plan_status", gameplayLifecycleHostPlan.get("status"), "PASS");
        requireEquals(requirements, "host_runtime_status", gameplayLifecycleHostRuntime.get("status"), "PASS");
        requireNumber(requirements, "host_plan_invocations", gameplayLifecycleHostPlan.get("readyInvocationCount"), 23);
        requireNumber(requirements, "host_runtime_applied_invocations",
                gameplayLifecycleHostRuntime.get("appliedInvocationCount"), 23);
        requireFalse(requirements, "no_minecraft_runtime_access", gameplayLifecycleHostRuntime.get("minecraftRuntimeAccessed"));
        requireFalse(requirements, "no_minecraft_runtime_mutation", gameplayLifecycleHostRuntime.get("minecraftRuntimeMutated"));
        requireFalse(requirements, "no_minecraft_registry_mutation", gameplayLifecycleHostRuntime.get("minecraftRegistryMutated"));
        requireFalse(requirements, "not_standalone_gameplay_runtime",
                gameplayLifecycleHostRuntime.get("standaloneDuplicateGameplaySystem"));
        requireTrue(requirements, "native_host_state_mutated", snapshot.get("nativeStateMutated"));
        requireFalse(requirements, "snapshot_no_minecraft_runtime_access", snapshot.get("minecraftRuntimeAccessed"));

        requireContains(requirements, "starter_note_inventory", snapshot.get("inventoryItemIds"),
                "echoashfallprotocol:field_manual");
        requireContains(requirements, "terminal_remote_inventory", snapshot.get("inventoryItemIds"),
                "echoterminal:echo_terminal_remote");
        requireContains(requirements, "drop_pod_structure", snapshot.get("structureIds"),
                "echoashfallprotocol:drop_pod");
        requireEquals(requirements, "drop_pod_teleport_anchor", snapshot.get("teleportAnchor"),
                "personal_starting_drop_pod.interior");
        requireEquals(requirements, "drop_pod_respawn_anchor", snapshot.get("respawnAnchor"),
                "personal_starting_drop_pod.interior");
        requireTrue(requirements, "first_join_flag", snapshot.get("firstJoinFlagValue"));
        requireTrue(requirements, "quest_drop_pod_initialized", snapshot.get("questDropPodInitialized"));
        requireContains(requirements, "find_drop_pod_advancement", snapshot.get("advancements"),
                "echoashfallprotocol:find_drop_pod");
        requireNumber(requirements, "welcome_packet", snapshot.get("screenPacketCount"), 1);
        requireNumber(requirements, "hud_notification", snapshot.get("hudNotificationCount"), 1);
        requireNumber(requirements, "recovery_context", snapshot.get("recoveryContextCount"), 1);
        requireNumber(requirements, "holomap_visibility_context", snapshot.get("mapVisibilityContextCount"), 1);
        requireContains(requirements, "first_month_holomap_layer", snapshot.get("holomapLayers"),
                "echoashfallprotocol:first_month_field_intel");
        requireContains(requirements, "first_route_holomap_layer", snapshot.get("holomapLayers"),
                "echoashfallprotocol:first_major_route");
        requireContains(requirements, "underground_pod_repair", snapshot.get("repairActions"),
                "rescue_underground_starting_pod_below_y_48");
        requireContains(requirements, "missing_respawn_repair", snapshot.get("repairActions"),
                "repair_missing_drop_pod_respawn");
        requireContains(requirements, "terminal_remote_reissue_repair", snapshot.get("repairActions"),
                "reissue_terminal_remote_if_available");
        requireEquals(requirements, "mission_tracker_line", snapshot.get("missionTrackerLine"),
                "Place an Ash Campfire near the crash site");
        requireEquals(requirements, "hazard_readout_line", snapshot.get("hazardReadoutLine"),
                "AIR stable; hazards marked.");
        requireEquals(requirements, "welcome_screen_surface", snapshot.get("welcomeScreen"),
                "echoashfallprotocol:welcome_screen");
        requireEquals(requirements, "terminal_card", snapshot.get("terminalCard"), "ashfall:first_ten_minutes");
        requireEquals(requirements, "wiki_guide", snapshot.get("wikiGuide"), "echowiki:ashfall");
        requireContains(requirements, "lens_route_profile", snapshot.get("lensProfiles"),
                "echoashfallprotocol:ashfall_major_route_scans");
        requireContains(requirements, "codex_route_prep", snapshot.get("codexEntries"),
                "echoashfallprotocol:hazard_route_prep");
        requireContains(requirements, "weather_ash_storm", snapshot.get("routeHazards"),
                "echoweathercore:ash_storm");
        requireContains(requirements, "ashfall_radiation_hazard", snapshot.get("routeHazards"),
                "echoashfallprotocol:radiation");
        requireContains(requirements, "ash_storm_sound", snapshot.get("soundCues"),
                "echoashfallprotocol:event.ash_storm");
        requireContains(requirements, "soundcore_ambience", snapshot.get("soundCues"),
                "echosoundcore:ambient_loop");
        requireEquals(requirements, "atmosphere_visibility_profile", snapshot.get("atmosphereProfile"),
                "echoatmospherecore:storm_visibility");
        requireEquals(requirements, "atmosphere_particle_profile", snapshot.get("particleProfile"),
                "echoashfallprotocol:opening_route_ash_particles");
        requireEquals(requirements, "atmosphere_sky_fog", snapshot.get("skyFog"), "ashfall_opening_route_sky_fog");

        List<String> diagnostics = diagnostics(requirements);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("id", "echoashfallprotocol:agent3_first_join_parity_verifier");
        report.put("moduleId", MODULE_ID);
        report.put("bridge", "adaptercore.ashfall_first_join_parity_verifier");
        report.put("adapterCoreBridge", true);
        report.put("implementationTarget", "AdapterCore Ashfall first-join gameplay parity verification");
        report.put("sourceRuntimeHandler", "PlayerStartingKitHandler.onPlayerLoggedIn");
        report.put("sourceProfile", value(firstJoinProfile, "id"));
        report.put("sourceExecution", value(firstJoinExecution, "id"));
        report.put("sourceLifecyclePlan", value(gameplayLifecycleHostPlan, "id"));
        report.put("sourceHostRuntime", value(gameplayLifecycleHostRuntime, "id"));
        report.put("executionMode", "adaptercore_jdk_first_join_parity_verifier");
        report.put("standaloneDuplicateGameplaySystem", false);
        report.put("runtimeStateConsumed", false);
        report.put("noLaunchRuntimeStateValidated", diagnostics.isEmpty());
        report.put("nativeStateConsumed", false);
        report.put("nativeStateValidatedForHostDispatch", diagnostics.isEmpty());
        report.put("liveRuntimeMutationConsumed", false);
        report.put("minecraftRuntimeAccessed", false);
        report.put("minecraftRuntimeMutated", false);
        report.put("minecraftRegistryMutated", false);
        report.put("validatedRequirementCount", requirements.size());
        report.put("passingRequirementCount", requirements.size() - diagnostics.size());
        report.put("requirements", List.copyOf(requirements));
        report.put("diagnostics", List.copyOf(diagnostics));
        report.put("parityVerified", diagnostics.isEmpty());
        report.put("status", diagnostics.isEmpty() ? "PASS" : "FAIL");
        report.put("summary", diagnostics.isEmpty()
                ? "AdapterCore no-launch host-state snapshot satisfies the Ashfall first-join native gameplay contract without claiming live Minecraft runtime mutation."
                : "AdapterCore host-state snapshot is missing Ashfall first-join gameplay parity requirements.");
        return Map.copyOf(report);
    }

    private static void requireEquals(List<Map<String, Object>> requirements, String id, Object actual, String expected) {
        addRequirement(requirements, id, expected.equals(actual), expected, actual);
    }

    private static void requireNumber(List<Map<String, Object>> requirements, String id, Object actual, int expected) {
        boolean passes = actual instanceof Number number && number.intValue() == expected;
        addRequirement(requirements, id, passes, expected, actual);
    }

    private static void requireTrue(List<Map<String, Object>> requirements, String id, Object actual) {
        addRequirement(requirements, id, Boolean.TRUE.equals(actual), true, actual);
    }

    private static void requireFalse(List<Map<String, Object>> requirements, String id, Object actual) {
        addRequirement(requirements, id, Boolean.FALSE.equals(actual), false, actual);
    }

    private static void requireContains(List<Map<String, Object>> requirements, String id, Object actual, String expected) {
        addRequirement(requirements, id, contains(actual, expected), expected, actual);
    }

    private static void addRequirement(
            List<Map<String, Object>> requirements,
            String id,
            boolean passes,
            Object expected,
            Object actual) {
        Map<String, Object> requirement = new LinkedHashMap<>();
        requirement.put("id", id);
        requirement.put("status", passes ? "PASS" : "FAIL");
        requirement.put("expected", expected);
        requirement.put("actual", actual == null ? "" : actual);
        requirements.add(Map.copyOf(requirement));
    }

    private static List<String> diagnostics(List<Map<String, Object>> requirements) {
        List<String> diagnostics = new ArrayList<>();
        for (Map<String, Object> requirement : requirements) {
            if (!"PASS".equals(requirement.get("status"))) {
                diagnostics.add("Requirement " + requirement.get("id") + " failed.");
            }
        }
        return List.copyOf(diagnostics);
    }

    private static boolean contains(Object actual, String expected) {
        if (actual instanceof Iterable<?> values) {
            for (Object value : values) {
                if (expected.equals(value)) {
                    return true;
                }
            }
        }
        return false;
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
