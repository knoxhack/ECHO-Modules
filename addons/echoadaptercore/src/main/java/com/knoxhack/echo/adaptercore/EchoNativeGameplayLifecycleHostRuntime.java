package com.knoxhack.echo.adaptercore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class EchoNativeGameplayLifecycleHostRuntime {
    private final String moduleId;
    private final List<Map<String, Object>> inventoryWrites = new ArrayList<>();
    private final List<Map<String, Object>> structurePlacements = new ArrayList<>();
    private final Set<String> advancements = new LinkedHashSet<>();
    private final List<Map<String, Object>> screenPackets = new ArrayList<>();
    private final List<Map<String, Object>> hudNotifications = new ArrayList<>();
    private final List<Map<String, Object>> recoveryContexts = new ArrayList<>();
    private final List<Map<String, Object>> mapVisibilityContexts = new ArrayList<>();
    private final Set<String> holomapLayers = new LinkedHashSet<>();
    private final Set<String> repairActions = new LinkedHashSet<>();
    private final Set<String> lensProfiles = new LinkedHashSet<>();
    private final Set<String> codexEntries = new LinkedHashSet<>();
    private final Set<String> routeHazards = new LinkedHashSet<>();
    private final Set<String> soundCues = new LinkedHashSet<>();
    private final List<Map<String, Object>> appliedInvocations = new ArrayList<>();
    private final Map<String, Object> playerStateWrites = new LinkedHashMap<>();
    private final Map<String, Object> missionTracker = new LinkedHashMap<>();
    private final Map<String, Object> hazardReadout = new LinkedHashMap<>();
    private final Map<String, Object> welcomeSurface = new LinkedHashMap<>();
    private final Map<String, Object> terminalLink = new LinkedHashMap<>();
    private final Map<String, Object> wikiLink = new LinkedHashMap<>();
    private final Map<String, Object> weatherState = new LinkedHashMap<>();
    private final Map<String, Object> soundState = new LinkedHashMap<>();
    private final Map<String, Object> atmosphereState = new LinkedHashMap<>();
    private String teleportAnchor = "";
    private String respawnAnchor = "";

    public EchoNativeGameplayLifecycleHostRuntime(String moduleId) {
        this.moduleId = AdapterContractGuards.requireText(moduleId, "module id");
    }

    public Map<String, Object> execute(
            String id,
            Map<String, Object> lifecyclePlan,
            Map<String, Object> firstJoinHostContract,
            Map<String, Object> recoveryHostContract,
            Map<String, Object> uiHudHostContract,
            Map<String, Object> atmosphereHostContract) {
        applyContract("player.first_join", firstJoinHostContract);
        applyContract("player.recovery_navigation", recoveryHostContract);
        applyContract("ui_hud_screen_safe", uiHudHostContract);
        applyContract("weather_sound_atmosphere", atmosphereHostContract);

        List<String> diagnostics = validate(lifecyclePlan);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("id", AdapterContractGuards.requireText(id, "gameplay lifecycle host runtime id"));
        report.put("moduleId", moduleId);
        report.put("bridge", "adaptercore.gameplay_lifecycle_host_runtime");
        report.put("adapterCoreBridge", true);
        report.put("implementationTarget", "AdapterCore no-launch host runtime for Ashfall gameplay lifecycle invocations");
        report.put("executionMode", "adaptercore_jdk_gameplay_lifecycle_host_runtime");
        report.put("standaloneDuplicateGameplaySystem", false);
        report.put("runtimeStateInitialized", true);
        report.put("serviceCodeExecuted", true);
        report.put("hostLifecycleRuntimeExecuted", false);
        report.put("noLaunchHostStateSnapshotProduced", diagnostics.isEmpty());
        report.put("nativeStateMutated", false);
        report.put("noLaunchNativeStateMutated", !appliedInvocations.isEmpty());
        report.put("nativeStateConsumed", false);
        report.put("nativeStateValidatedForHostDispatch", diagnostics.isEmpty());
        report.put("liveRuntimeMutationConsumed", false);
        report.put("minecraftRuntimeAccessed", false);
        report.put("minecraftRuntimeMutated", false);
        report.put("minecraftRegistryMutated", false);
        report.put("sourceLifecyclePlan", value(lifecyclePlan, "id"));
        report.put("plannedInvocationCount", numberValue(lifecyclePlan, "readyInvocationCount"));
        report.put("appliedInvocationCount", appliedInvocations.size());
        report.put("appliedInvocations", List.copyOf(appliedInvocations));
        report.put("inventoryWrites", List.copyOf(inventoryWrites));
        report.put("structurePlacements", List.copyOf(structurePlacements));
        report.put("teleportAnchor", teleportAnchor);
        report.put("respawnAnchor", respawnAnchor);
        report.put("playerStateWrites", Map.copyOf(playerStateWrites));
        report.put("advancements", List.copyOf(advancements));
        report.put("screenPackets", List.copyOf(screenPackets));
        report.put("hudNotifications", List.copyOf(hudNotifications));
        report.put("recoveryContexts", List.copyOf(recoveryContexts));
        report.put("mapVisibilityContexts", List.copyOf(mapVisibilityContexts));
        report.put("holomapLayers", List.copyOf(holomapLayers));
        report.put("repairActions", List.copyOf(repairActions));
        report.put("missionTracker", Map.copyOf(missionTracker));
        report.put("hazardReadout", Map.copyOf(hazardReadout));
        report.put("welcomeSurface", Map.copyOf(welcomeSurface));
        report.put("terminalLink", Map.copyOf(terminalLink));
        report.put("wikiLink", Map.copyOf(wikiLink));
        report.put("lensProfiles", List.copyOf(lensProfiles));
        report.put("codexEntries", List.copyOf(codexEntries));
        report.put("weatherState", Map.copyOf(weatherState));
        report.put("routeHazards", List.copyOf(routeHazards));
        report.put("soundState", Map.copyOf(soundState));
        report.put("soundCues", List.copyOf(soundCues));
        report.put("atmosphereState", Map.copyOf(atmosphereState));
        report.put("hostStateSnapshot", snapshot());
        report.put("diagnostics", List.copyOf(diagnostics));
        report.put("status", diagnostics.isEmpty() ? "PASS" : "FAIL");
        report.put("summary", diagnostics.isEmpty()
                ? "AdapterCore produced a deterministic no-launch host-state snapshot for prepared first-join gameplay surfaces; it does not claim live host execution or Minecraft state mutation."
                : "AdapterCore host runtime did not produce the required Ashfall first-join gameplay state.");
        return Map.copyOf(report);
    }

    private void applyContract(String lifecycleEvent, Map<String, Object> contract) {
        for (Map<String, Object> invocation : invocations(contract)) {
            String status = value(invocation, "status");
            if (!"READY_FOR_HOST_ADAPTER".equals(status) && !"READY_FOR_HOST_SURFACE".equals(status)) {
                continue;
            }
            applyInvocation(lifecycleEvent, invocation);
        }
    }

    private void applyInvocation(String lifecycleEvent, Map<String, Object> invocation) {
        String invocationId = invocationId(invocation);
        List<Object> payloads = payloads(invocation);
        switch (invocationId) {
            case "minecraft_backed_inventory_writer" -> addMaps(inventoryWrites, payloads);
            case "minecraft_backed_structure_placer" -> addMaps(structurePlacements, payloads);
            case "minecraft_backed_player_positioner" -> teleportAnchor = firstText(payloads);
            case "minecraft_backed_respawn_binder" -> respawnAnchor = firstText(payloads);
            case "minecraft_backed_player_state_writer" -> putAll(playerStateWrites, payloads);
            case "minecraft_backed_advancement_granter" -> addStrings(advancements, payloads);
            case "minecraft_backed_screen_packet_sender" -> addMaps(screenPackets, payloads);
            case "minecraft_backed_hud_notification_sender" -> addMaps(hudNotifications, payloads);
            case "native_recovery_field_cache_adapter" -> addMaps(recoveryContexts, payloads);
            case "native_holomap_recovery_visibility_adapter" -> {
                addMaps(mapVisibilityContexts, payloads);
                addNestedStrings(holomapLayers, payloads, "layers");
            }
            case "native_existing_player_repair_adapter" -> addStrings(repairActions, payloads);
            case "native_hud_mission_tracker_adapter" -> putAll(missionTracker, payloads);
            case "native_hud_hazard_readout_adapter", "native_hud_hazard_weather_adapter" ->
                    putAll(hazardReadout, payloads);
            case "native_welcome_surface_adapter" -> putAll(welcomeSurface, payloads);
            case "native_terminal_surface_adapter" -> putAll(terminalLink, payloads);
            case "native_wiki_surface_adapter" -> putAll(wikiLink, payloads);
            case "native_lens_surface_adapter" -> addStrings(lensProfiles, payloads);
            case "native_holomap_surface_adapter" -> addStrings(holomapLayers, payloads);
            case "native_codex_surface_adapter" -> addStrings(codexEntries, payloads);
            case "native_weather_state_adapter" -> {
                putAll(weatherState, payloads);
                addNestedStrings(routeHazards, payloads, "hazards");
            }
            case "native_sound_ambience_adapter" -> {
                putAll(soundState, payloads);
                addNestedStrings(soundCues, payloads, "soundCues");
            }
            case "native_atmosphere_visibility_adapter" -> putAll(atmosphereState, payloads);
            default -> {
            }
        }
        Map<String, Object> applied = new LinkedHashMap<>();
        applied.put("lifecycleEvent", lifecycleEvent);
        applied.put("invocationId", invocationId);
        applied.put("payloadCount", payloads.size());
        applied.put("adapterCoreHostRuntime", true);
        applied.put("minecraftRuntimeAccessed", false);
        appliedInvocations.add(Map.copyOf(applied));
    }

    private Map<String, Object> snapshot() {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("inventoryItemIds", itemIds(inventoryWrites));
        snapshot.put("structureIds", structureIds(structurePlacements));
        snapshot.put("teleportAnchor", teleportAnchor);
        snapshot.put("respawnAnchor", respawnAnchor);
        snapshot.put("firstJoinFlagValue", playerStateWrites.getOrDefault("firstJoinFlagValue", false));
        snapshot.put("questDropPodInitialized", playerStateWrites.getOrDefault("questDropPodInitialized", false));
        snapshot.put("advancements", List.copyOf(advancements));
        snapshot.put("screenPacketCount", screenPackets.size());
        snapshot.put("hudNotificationCount", hudNotifications.size());
        snapshot.put("recoveryContextCount", recoveryContexts.size());
        snapshot.put("mapVisibilityContextCount", mapVisibilityContexts.size());
        snapshot.put("holomapLayers", List.copyOf(holomapLayers));
        snapshot.put("repairActions", List.copyOf(repairActions));
        snapshot.put("missionTrackerLine", missionTracker.getOrDefault("line", ""));
        snapshot.put("hazardReadoutLine", hazardReadout.getOrDefault("line", ""));
        snapshot.put("welcomeScreen", welcomeSurface.getOrDefault("screen", ""));
        snapshot.put("terminalCard", terminalLink.getOrDefault("card", ""));
        snapshot.put("wikiGuide", wikiLink.getOrDefault("guide", ""));
        snapshot.put("lensProfiles", List.copyOf(lensProfiles));
        snapshot.put("codexEntries", List.copyOf(codexEntries));
        snapshot.put("routeHazards", List.copyOf(routeHazards));
        snapshot.put("soundCues", List.copyOf(soundCues));
        snapshot.put("atmosphereProfile", atmosphereState.getOrDefault("profile", ""));
        snapshot.put("particleProfile", atmosphereState.getOrDefault("particleProfile", ""));
        snapshot.put("skyFog", atmosphereState.getOrDefault("skyFog", ""));
        snapshot.put("nativeStateMutated", !appliedInvocations.isEmpty());
        snapshot.put("noLaunchNativeStateMutated", !appliedInvocations.isEmpty());
        snapshot.put("minecraftRuntimeAccessed", false);
        return snapshot;
    }

    private List<String> validate(Map<String, Object> lifecyclePlan) {
        List<String> diagnostics = new ArrayList<>();
        if (!"PASS".equals(value(lifecyclePlan, "status"))) {
            diagnostics.add("Gameplay lifecycle host plan did not pass.");
        }
        if (appliedInvocations.size() != numberValue(lifecyclePlan, "readyInvocationCount")) {
            diagnostics.add("Applied host invocation count does not match lifecycle plan.");
        }
        requireContains(itemIds(inventoryWrites), "echoashfallprotocol:field_manual", "Missing starter note inventory write.", diagnostics);
        requireContains(itemIds(inventoryWrites), "echoterminal:echo_terminal_remote", "Missing Terminal remote inventory write.", diagnostics);
        requireContains(structureIds(structurePlacements), "echoashfallprotocol:drop_pod", "Missing drop-pod structure placement.", diagnostics);
        requireText(teleportAnchor, "Missing drop-pod teleport anchor.", diagnostics);
        requireText(respawnAnchor, "Missing drop-pod respawn anchor.", diagnostics);
        if (!Boolean.TRUE.equals(playerStateWrites.get("firstJoinFlagValue"))) {
            diagnostics.add("Missing first-join flag write.");
        }
        if (!Boolean.TRUE.equals(playerStateWrites.get("questDropPodInitialized"))) {
            diagnostics.add("Missing quest drop-pod initialized write.");
        }
        requireContains(advancements, "echoashfallprotocol:find_drop_pod", "Missing find_drop_pod advancement.", diagnostics);
        if (screenPackets.isEmpty()) {
            diagnostics.add("Missing welcome screen packet.");
        }
        if (hudNotifications.isEmpty()) {
            diagnostics.add("Missing HUD notification.");
        }
        if (recoveryContexts.isEmpty()) {
            diagnostics.add("Missing Recovery field-cache context.");
        }
        if (mapVisibilityContexts.isEmpty()) {
            diagnostics.add("Missing HoloMap recovery visibility context.");
        }
        requireContains(holomapLayers, "echoashfallprotocol:first_month_field_intel", "Missing first-month HoloMap layer.", diagnostics);
        requireContains(holomapLayers, "echoashfallprotocol:first_major_route", "Missing first-major-route HoloMap layer.", diagnostics);
        requireContains(repairActions, "rescue_underground_starting_pod_below_y_48", "Missing underground pod rescue repair.", diagnostics);
        requireContains(repairActions, "repair_missing_drop_pod_respawn", "Missing missing-respawn repair.", diagnostics);
        requireEquals(missionTracker.get("line"), "Place an Ash Campfire near the crash site", "Missing mission tracker line.", diagnostics);
        requireEquals(hazardReadout.get("line"), "AIR stable; hazards marked.", "Missing hazard readout line.", diagnostics);
        requireEquals(welcomeSurface.get("screen"), "echoashfallprotocol:welcome_screen", "Missing welcome surface.", diagnostics);
        requireEquals(terminalLink.get("card"), "ashfall:first_ten_minutes", "Missing Terminal card.", diagnostics);
        requireEquals(wikiLink.get("guide"), "echowiki:ashfall", "Missing Wiki guide.", diagnostics);
        requireContains(lensProfiles, "echoashfallprotocol:ashfall_major_route_scans", "Missing Lens route scan.", diagnostics);
        requireContains(codexEntries, "echoashfallprotocol:hazard_route_prep", "Missing Codex hazard route prep.", diagnostics);
        requireContains(routeHazards, "echoweathercore:ash_storm", "Missing WeatherCore ash storm.", diagnostics);
        requireContains(routeHazards, "echoashfallprotocol:radiation", "Missing Ashfall radiation hazard.", diagnostics);
        requireContains(soundCues, "echoashfallprotocol:event.ash_storm", "Missing ash storm sound cue.", diagnostics);
        requireContains(soundCues, "echosoundcore:ambient_loop", "Missing SoundCore ambience cue.", diagnostics);
        requireEquals(atmosphereState.get("profile"), "echoatmospherecore:storm_visibility", "Missing atmosphere profile.", diagnostics);
        requireEquals(atmosphereState.get("particleProfile"), "echoashfallprotocol:opening_route_ash_particles", "Missing atmosphere particle profile.", diagnostics);
        requireEquals(atmosphereState.get("skyFog"), "ashfall_opening_route_sky_fog", "Missing atmosphere sky-fog profile.", diagnostics);
        return diagnostics;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> invocations(Map<String, Object> contract) {
        Object rawInvocations = contract == null ? null : contract.get("invocations");
        if (rawInvocations instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }
        return List.of();
    }

    private static List<Object> payloads(Map<String, Object> invocation) {
        Object rawPayloads = invocation.get("payloads");
        return rawPayloads instanceof List<?> list ? List.copyOf(list) : List.of();
    }

    private static void addMaps(List<Map<String, Object>> target, List<Object> payloads) {
        for (Object payload : payloads) {
            if (payload instanceof Map<?, ?> map) {
                target.add(copyStringObjectMap(map));
            }
        }
    }

    private static void putAll(Map<String, Object> target, List<Object> payloads) {
        for (Object payload : payloads) {
            if (payload instanceof Map<?, ?> map) {
                target.putAll(copyStringObjectMap(map));
            }
        }
    }

    private static void addStrings(Set<String> target, List<Object> payloads) {
        for (Object payload : payloads) {
            if (payload instanceof String text && !text.isBlank()) {
                target.add(text);
            } else if (payload instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof String text && !text.isBlank()) {
                        target.add(text);
                    }
                }
            }
        }
    }

    private static void addNestedStrings(Set<String> target, List<Object> payloads, String key) {
        for (Object payload : payloads) {
            if (payload instanceof Map<?, ?> map && map.get(key) instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof String text && !text.isBlank()) {
                        target.add(text);
                    }
                }
            }
        }
    }

    private static String firstText(List<Object> payloads) {
        for (Object payload : payloads) {
            if (payload instanceof String text && !text.isBlank()) {
                return text;
            }
        }
        return "";
    }

    private static List<String> itemIds(List<Map<String, Object>> writes) {
        List<String> ids = new ArrayList<>();
        for (Map<String, Object> write : writes) {
            String item = value(write, "item");
            if (!item.isBlank()) {
                ids.add(item);
            }
        }
        return List.copyOf(ids);
    }

    private static List<String> structureIds(List<Map<String, Object>> placements) {
        List<String> ids = new ArrayList<>();
        for (Map<String, Object> placement : placements) {
            String structure = value(placement, "structure");
            if (!structure.isBlank()) {
                ids.add(structure);
            }
        }
        return List.copyOf(ids);
    }

    private static Map<String, Object> copyStringObjectMap(Map<?, ?> source) {
        Map<String, Object> copy = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            copy.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return Map.copyOf(copy);
    }

    private static void requireText(String value, String message, List<String> diagnostics) {
        if (value == null || value.isBlank()) {
            diagnostics.add(message);
        }
    }

    private static void requireEquals(Object value, String expected, String message, List<String> diagnostics) {
        if (!expected.equals(value)) {
            diagnostics.add(message);
        }
    }

    private static void requireContains(Iterable<String> values, String expected, String message, List<String> diagnostics) {
        for (String value : values) {
            if (expected.equals(value)) {
                return;
            }
        }
        diagnostics.add(message);
    }

    private static int numberValue(Map<String, Object> values, String key) {
        return values != null && values.get(key) instanceof Number number ? number.intValue() : 0;
    }

    private static String invocationId(Map<String, Object> invocation) {
        String id = value(invocation, "adapterId");
        if (id.isBlank()) {
            id = value(invocation, "invocationId");
        }
        return id;
    }

    private static String value(Map<String, Object> map, String key) {
        Object value = map == null ? null : map.get(key);
        return value == null ? "" : String.valueOf(value);
    }
}
