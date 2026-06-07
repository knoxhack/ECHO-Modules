package com.knoxhack.echo.adaptercore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class EchoNativeUiHudRuntimeTarget {
    private static final String PREPARED = "prepared_as_adaptercore_command";

    private final String moduleId;
    private final Map<String, Object> missionTracker = new LinkedHashMap<>();
    private final Map<String, Object> hazardReadout = new LinkedHashMap<>();
    private final Map<String, Object> welcomeSurface = new LinkedHashMap<>();
    private final Map<String, Object> terminalLink = new LinkedHashMap<>();
    private final Map<String, Object> wikiLink = new LinkedHashMap<>();
    private final List<String> lensProfiles = new ArrayList<>();
    private final List<String> holomapLayers = new ArrayList<>();
    private final List<String> codexEntries = new ArrayList<>();
    private final List<Map<String, Object>> mutationLog = new ArrayList<>();
    private final Set<String> mutationSurfaces = new LinkedHashSet<>();
    private int preparedCommandCount = 0;

    public EchoNativeUiHudRuntimeTarget(String moduleId) {
        this.moduleId = AdapterContractGuards.requireText(moduleId, "module id");
    }

    public Map<String, Object> execute(String id, List<Map<String, Object>> commands) {
        if (commands != null) {
            for (Map<String, Object> command : commands) {
                if (PREPARED.equals(command.get("status"))) {
                    preparedCommandCount++;
                    executeCommand(command);
                }
            }
        }

        List<String> diagnostics = validate();
        boolean accepted = diagnostics.isEmpty();
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("id", AdapterContractGuards.requireText(id, "runtime target id"));
        report.put("moduleId", moduleId);
        report.put("bridge", "adaptercore.native_ui_hud_runtime");
        report.put("adapterCoreBridge", true);
        report.put("adapterSurface", "ui_hud_screen_safe.runtime_target");
        report.put("implementationTarget", "AdapterCore stateful native UI/HUD/Lens/Wiki/Codex runtime target");
        report.put("executionMode", "adaptercore_jdk_stateful_ui_hud_runtime_target");
        report.put("standaloneDuplicateGameplaySystem", false);
        report.put("runtimeStateInitialized", true);
        report.put("accepted", accepted);
        report.put("serviceCodeExecuted", accepted);
        report.put("liveRuntimeMutation", false);
        report.put("nativeStateMutated", !mutationLog.isEmpty());
        report.put("noLaunchNativeStateMutated", !mutationLog.isEmpty());
        report.put("minecraftRuntimeAccessed", false);
        report.put("minecraftRuntimeMutated", false);
        report.put("minecraftRegistryMutated", false);
        report.put("unsafeRuntimeWorkStarted", false);
        report.put("preparedCommandCount", preparedCommandCount);
        report.put("executedCommandCount", 0);
        report.put("mutatingOperationCount", mutationLog.size());
        report.put("mutationSurfaces", List.copyOf(mutationSurfaces));
        report.put("mutationLog", List.copyOf(mutationLog));
        report.put("missionTracker", Map.copyOf(missionTracker));
        report.put("hazardReadout", Map.copyOf(hazardReadout));
        report.put("welcomeSurface", Map.copyOf(welcomeSurface));
        report.put("terminalLink", Map.copyOf(terminalLink));
        report.put("wikiLink", Map.copyOf(wikiLink));
        report.put("lensProfiles", List.copyOf(lensProfiles));
        report.put("holomapLayers", List.copyOf(holomapLayers));
        report.put("codexEntries", List.copyOf(codexEntries));
        report.put("runtimeSnapshot", snapshot());
        report.put("diagnostics", diagnostics);
        report.put("status", accepted ? "PASS" : "FAIL");
        report.put("effect", accepted
                ? "ui_hud_host_call_queue_replay:prepared:" + preparedCommandCount
                : "ui_hud_host_call_queue_replay:rejected");
        report.put("summary", accepted
                ? "AdapterCore prepared Ashfall first-join UI/HUD commands against stateful no-launch screen-safe, Terminal, Wiki, Lens, HoloMap, and Codex packets without claiming live host mutation."
                : "AdapterCore UI/HUD runtime target is missing required screen-safe or knowledge-surface mutations.");
        return report;
    }

    @SuppressWarnings("unchecked")
    private void executeCommand(Map<String, Object> command) {
        String operationId = String.valueOf(command.get("operationId"));
        String targetBridge = String.valueOf(command.get("targetBridge"));
        Map<String, Object> payload = command.get("payload") instanceof Map<?, ?> map
                ? (Map<String, Object>) map
                : Map.of();
        switch (operationId) {
            case "hud.publish_mission_tracker_line" -> {
                missionTracker.putAll(payload);
                logMutation(operationId, targetBridge, payload.getOrDefault("line", "mission_tracker"));
            }
            case "hud.publish_hazard_weather_readout" -> {
                hazardReadout.putAll(payload);
                logMutation(operationId, targetBridge, payload.getOrDefault("line", "hazard_readout"));
            }
            case "screen.dispatch_welcome_onboarding_surface" -> {
                welcomeSurface.putAll(payload);
                logMutation(operationId, targetBridge, payload.getOrDefault("screen", "welcome_surface"));
            }
            case "terminal.publish_first_ten_minutes_link" -> {
                terminalLink.putAll(payload);
                logMutation(operationId, targetBridge, payload.getOrDefault("card", "terminal_card"));
            }
            case "wiki.publish_ashfall_guide_link" -> {
                wikiLink.putAll(payload);
                logMutation(operationId, targetBridge, payload.getOrDefault("guide", "wiki_guide"));
            }
            case "lens.publish_opening_route_scan" -> {
                addStrings(lensProfiles, payload.get("profiles"));
                logMutation(operationId, targetBridge, "lens_profiles");
            }
            case "holomap.publish_opening_recovery_layers" -> {
                addStrings(holomapLayers, payload.get("layers"));
                logMutation(operationId, targetBridge, "holomap_layers");
            }
            case "codex.publish_opening_route_entries" -> {
                addStrings(codexEntries, payload.get("entries"));
                logMutation(operationId, targetBridge, "codex_entries");
            }
            default -> {
            }
        }
    }

    private void logMutation(String operationId, String targetBridge, Object target) {
        Map<String, Object> mutation = new LinkedHashMap<>();
        mutation.put("operationId", AdapterContractGuards.requireText(operationId, "mutation operation id"));
        mutation.put("targetBridge", AdapterContractGuards.requireText(targetBridge, "mutation target bridge"));
        mutation.put("target", String.valueOf(target));
        mutation.put("adapterCoreMutation", true);
        mutation.put("minecraftRuntimeAccessed", false);
        mutationLog.add(mutation);
        mutationSurfaces.add(targetBridge);
    }

    private void addStrings(List<String> target, Object values) {
        if (values instanceof List<?> list) {
            for (Object value : list) {
                if (value instanceof String text && !text.isBlank()) {
                    target.add(text);
                }
            }
        }
    }

    private Map<String, Object> snapshot() {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("missionTrackerLine", missionTracker.getOrDefault("line", ""));
        snapshot.put("hazardReadoutLine", hazardReadout.getOrDefault("line", ""));
        snapshot.put("welcomeScreen", welcomeSurface.getOrDefault("screen", ""));
        snapshot.put("welcomePacket", welcomeSurface.getOrDefault("packet", ""));
        snapshot.put("terminalCard", terminalLink.getOrDefault("card", ""));
        snapshot.put("wikiGuide", wikiLink.getOrDefault("guide", ""));
        snapshot.put("lensProfileCount", lensProfiles.size());
        snapshot.put("holomapLayerCount", holomapLayers.size());
        snapshot.put("codexEntryCount", codexEntries.size());
        snapshot.put("nativeStateMutated", !mutationLog.isEmpty());
        snapshot.put("minecraftRuntimeAccessed", false);
        return snapshot;
    }

    private List<String> validate() {
        List<String> diagnostics = new ArrayList<>();
        requireValue(missionTracker, "line", "Missing runtime HUD mission tracker line.", diagnostics);
        requireValue(hazardReadout, "line", "Missing runtime HUD hazard readout line.", diagnostics);
        requireValue(welcomeSurface, "screen", "Missing runtime welcome onboarding surface.", diagnostics);
        requireValue(welcomeSurface, "packet", "Missing runtime welcome screen packet.", diagnostics);
        requireValue(terminalLink, "card", "Missing runtime Terminal first-ten-minutes link.", diagnostics);
        requireFalse(terminalLink, "standaloneCopy", "Runtime Terminal link must not be a standalone copy.", diagnostics);
        requireValue(wikiLink, "guide", "Missing runtime Wiki guide link.", diagnostics);
        requireFalse(wikiLink, "standaloneCopy", "Runtime Wiki link must not be a standalone copy.", diagnostics);
        if (!lensProfiles.contains("echoashfallprotocol:ashfall_major_route_scans")) {
            diagnostics.add("Missing runtime Lens opening route scan profile.");
        }
        if (!holomapLayers.contains("echoashfallprotocol:first_month_field_intel")
                || !holomapLayers.contains("echoashfallprotocol:first_major_route")) {
            diagnostics.add("Missing runtime HoloMap opening recovery layers.");
        }
        if (!codexEntries.contains("echoashfallprotocol:hazard_route_prep")) {
            diagnostics.add("Missing runtime Codex hazard route prep entry.");
        }
        if (mutationLog.size() < 8) {
            diagnostics.add("Expected all UI/HUD commands to mutate native state.");
        }
        return List.copyOf(diagnostics);
    }

    private static void requireValue(Map<String, Object> state, String key, String message, List<String> diagnostics) {
        Object value = state.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            diagnostics.add(message);
        }
    }

    private static void requireFalse(Map<String, Object> state, String key, String message, List<String> diagnostics) {
        if (!Boolean.FALSE.equals(state.get(key))) {
            diagnostics.add(message);
        }
    }
}
