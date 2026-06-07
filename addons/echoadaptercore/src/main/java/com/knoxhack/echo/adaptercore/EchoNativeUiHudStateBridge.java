package com.knoxhack.echo.adaptercore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeUiHudStateBridge {
    private final String moduleId;
    private final Map<String, Object> missionTracker = new LinkedHashMap<>();
    private final Map<String, Object> hazardReadout = new LinkedHashMap<>();
    private final Map<String, Object> welcomeSurface = new LinkedHashMap<>();
    private final Map<String, Object> terminalLink = new LinkedHashMap<>();
    private final Map<String, Object> wikiLink = new LinkedHashMap<>();
    private final List<String> lensProfiles = new ArrayList<>();
    private final List<String> holomapLayers = new ArrayList<>();
    private final List<String> codexEntries = new ArrayList<>();

    public EchoNativeUiHudStateBridge(String moduleId) {
        this.moduleId = AdapterContractGuards.requireText(moduleId, "module id");
    }

    public Map<String, Object> apply(String id, List<Map<String, Object>> commands) {
        if (commands != null) {
            for (Map<String, Object> command : commands) {
                if ("prepared_as_adaptercore_command".equals(command.get("status"))) {
                    applyCommand(command);
                }
            }
        }

        List<String> diagnostics = validate();
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("id", AdapterContractGuards.requireText(id, "state report id"));
        report.put("moduleId", moduleId);
        report.put("bridge", "adaptercore.native_ui_hud_state");
        report.put("adapterCoreBridge", true);
        report.put("implementationTarget", "AdapterCore native UI/HUD command application");
        report.put("executionMode", "adaptercore_jdk_only_ui_hud_state_application");
        report.put("standaloneDuplicateGameplaySystem", false);
        report.put("minecraftRuntimeAccessed", false);
        report.put("minecraftRegistryMutated", false);
        report.put("liveRuntimeMutation", false);
        report.put("missionTracker", Map.copyOf(missionTracker));
        report.put("hazardReadout", Map.copyOf(hazardReadout));
        report.put("welcomeSurface", Map.copyOf(welcomeSurface));
        report.put("terminalLink", Map.copyOf(terminalLink));
        report.put("wikiLink", Map.copyOf(wikiLink));
        report.put("lensProfiles", List.copyOf(lensProfiles));
        report.put("holomapLayers", List.copyOf(holomapLayers));
        report.put("codexEntries", List.copyOf(codexEntries));
        report.put("diagnostics", diagnostics);
        report.put("status", diagnostics.isEmpty() ? "PASS" : "FAIL");
        report.put("summary", diagnostics.isEmpty()
                ? "AdapterCore applied first-join UI/HUD commands to a JDK-only screen-safe state report for HUD, welcome screen, Terminal, Wiki, Lens, HoloMap, and Codex surfaces."
                : "AdapterCore native UI/HUD state report is missing required screen-safe outcomes.");
        return report;
    }

    @SuppressWarnings("unchecked")
    private void applyCommand(Map<String, Object> command) {
        String operationId = String.valueOf(command.get("operationId"));
        Map<String, Object> payload = command.get("payload") instanceof Map<?, ?> map
                ? (Map<String, Object>) map
                : Map.of();
        switch (operationId) {
            case "hud.publish_mission_tracker_line" ->
                    missionTracker.putAll(payload);
            case "hud.publish_hazard_weather_readout" ->
                    hazardReadout.putAll(payload);
            case "screen.dispatch_welcome_onboarding_surface" ->
                    welcomeSurface.putAll(payload);
            case "terminal.publish_first_ten_minutes_link" ->
                    terminalLink.putAll(payload);
            case "wiki.publish_ashfall_guide_link" ->
                    wikiLink.putAll(payload);
            case "lens.publish_opening_route_scan" ->
                    addStrings(lensProfiles, payload.get("profiles"));
            case "holomap.publish_opening_recovery_layers" ->
                    addStrings(holomapLayers, payload.get("layers"));
            case "codex.publish_opening_route_entries" ->
                    addStrings(codexEntries, payload.get("entries"));
            default -> {
            }
        }
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

    private List<String> validate() {
        List<String> diagnostics = new ArrayList<>();
        requireValue(missionTracker, "line", "Missing HUD mission tracker line.", diagnostics);
        requireValue(hazardReadout, "line", "Missing HUD hazard readout line.", diagnostics);
        requireValue(welcomeSurface, "screen", "Missing welcome onboarding surface.", diagnostics);
        requireValue(welcomeSurface, "packet", "Missing welcome screen packet.", diagnostics);
        requireValue(terminalLink, "card", "Missing Terminal first-ten-minutes link.", diagnostics);
        requireFalse(terminalLink, "standaloneCopy", "Terminal link must not be a standalone copy.", diagnostics);
        requireValue(wikiLink, "guide", "Missing Wiki guide link.", diagnostics);
        requireFalse(wikiLink, "standaloneCopy", "Wiki link must not be a standalone copy.", diagnostics);
        if (!lensProfiles.contains("echoashfallprotocol:ashfall_major_route_scans")) {
            diagnostics.add("Missing Lens opening route scan profile.");
        }
        if (!holomapLayers.contains("echoashfallprotocol:first_month_field_intel")) {
            diagnostics.add("Missing HoloMap first-month field intel layer.");
        }
        if (!holomapLayers.contains("echoashfallprotocol:first_major_route")) {
            diagnostics.add("Missing HoloMap first-major-route layer.");
        }
        if (!codexEntries.contains("echoashfallprotocol:hazard_route_prep")) {
            diagnostics.add("Missing Codex hazard route prep entry.");
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
