package com.knoxhack.echo.adaptercore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoNativeAtmosphereStateBridge {
    private final String moduleId;
    private final List<String> routeHazards = new ArrayList<>();
    private final List<String> soundCues = new ArrayList<>();
    private final Map<String, Object> weatherState = new LinkedHashMap<>();
    private final Map<String, Object> soundState = new LinkedHashMap<>();
    private final Map<String, Object> atmosphereState = new LinkedHashMap<>();
    private final Map<String, Object> hudHazardReadout = new LinkedHashMap<>();

    public EchoNativeAtmosphereStateBridge(String moduleId) {
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
        report.put("bridge", "adaptercore.native_atmosphere_state");
        report.put("adapterCoreBridge", true);
        report.put("implementationTarget", "AdapterCore native weather/sound/atmosphere command application");
        report.put("executionMode", "adaptercore_jdk_only_atmosphere_state_application");
        report.put("standaloneDuplicateGameplaySystem", false);
        report.put("minecraftRuntimeAccessed", false);
        report.put("minecraftRegistryMutated", false);
        report.put("liveRuntimeMutation", false);
        report.put("weatherState", Map.copyOf(weatherState));
        report.put("routeHazards", List.copyOf(routeHazards));
        report.put("soundState", Map.copyOf(soundState));
        report.put("soundCues", List.copyOf(soundCues));
        report.put("atmosphereState", Map.copyOf(atmosphereState));
        report.put("hudHazardReadout", Map.copyOf(hudHazardReadout));
        report.put("diagnostics", diagnostics);
        report.put("status", diagnostics.isEmpty() ? "PASS" : "FAIL");
        report.put("summary", diagnostics.isEmpty()
                ? "AdapterCore applied route hazard commands to a JDK-only weather, SoundCore ambience, AtmosphereCore visibility/particle/sky-fog, and HUD hazard readout state report."
                : "AdapterCore native atmosphere-state report is missing required route hazard outcomes.");
        return report;
    }

    @SuppressWarnings("unchecked")
    private void applyCommand(Map<String, Object> command) {
        String operationId = String.valueOf(command.get("operationId"));
        Map<String, Object> payload = command.get("payload") instanceof Map<?, ?> map
                ? (Map<String, Object>) map
                : Map.of();
        switch (operationId) {
            case "weather.feed_route_hazard_context" -> {
                weatherState.putAll(payload);
                addStrings(routeHazards, payload.get("hazards"));
            }
            case "sound.map_weather_state_to_ambience_cues" -> {
                soundState.putAll(payload);
                addStrings(soundCues, payload.get("soundCues"));
            }
            case "atmosphere.publish_visibility_particle_sky_fog_profile" ->
                    atmosphereState.putAll(payload);
            case "hud.feed_hazard_weather_readout" ->
                    hudHazardReadout.putAll(payload);
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
        if (!routeHazards.contains("echoweathercore:ash_storm")) {
            diagnostics.add("Missing WeatherCore ash storm route hazard.");
        }
        if (!routeHazards.contains("echoashfallprotocol:radiation")) {
            diagnostics.add("Missing Ashfall radiation route hazard.");
        }
        if (!soundCues.contains("echoashfallprotocol:event.ash_storm")) {
            diagnostics.add("Missing Ashfall ash storm sound cue.");
        }
        if (!soundCues.contains("echosoundcore:ambient_loop")) {
            diagnostics.add("Missing SoundCore ambience cue.");
        }
        requireValue(atmosphereState, "profile", "Missing AtmosphereCore visibility profile.", diagnostics);
        requireValue(atmosphereState, "particleProfile", "Missing atmosphere particle profile.", diagnostics);
        requireValue(atmosphereState, "skyFog", "Missing atmosphere sky-fog profile.", diagnostics);
        requireValue(hudHazardReadout, "line", "Missing HUD hazard readout line.", diagnostics);
        return List.copyOf(diagnostics);
    }

    private static void requireValue(Map<String, Object> state, String key, String message, List<String> diagnostics) {
        Object value = state.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            diagnostics.add(message);
        }
    }
}
