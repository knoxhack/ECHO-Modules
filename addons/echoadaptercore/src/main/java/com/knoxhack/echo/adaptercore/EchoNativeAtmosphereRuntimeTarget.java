package com.knoxhack.echo.adaptercore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class EchoNativeAtmosphereRuntimeTarget {
    private static final String PREPARED = "prepared_as_adaptercore_command";

    private final String moduleId;
    private final Map<String, Object> weatherState = new LinkedHashMap<>();
    private final Map<String, Object> soundState = new LinkedHashMap<>();
    private final Map<String, Object> atmosphereState = new LinkedHashMap<>();
    private final Map<String, Object> hudHazardReadout = new LinkedHashMap<>();
    private final List<String> routeHazards = new ArrayList<>();
    private final List<String> soundCues = new ArrayList<>();
    private final List<Map<String, Object>> mutationLog = new ArrayList<>();
    private final Set<String> mutationSurfaces = new LinkedHashSet<>();
    private int preparedCommandCount = 0;

    public EchoNativeAtmosphereRuntimeTarget(String moduleId) {
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
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("id", AdapterContractGuards.requireText(id, "runtime target id"));
        report.put("moduleId", moduleId);
        report.put("bridge", "adaptercore.native_atmosphere_runtime");
        report.put("adapterCoreBridge", true);
        report.put("adapterSurface", "weather_sound_atmosphere.runtime_target");
        report.put("implementationTarget", "AdapterCore stateful native WeatherCore/SoundCore/Atmosphere runtime target");
        report.put("executionMode", "adaptercore_jdk_stateful_atmosphere_runtime_target");
        report.put("standaloneDuplicateGameplaySystem", false);
        report.put("runtimeStateInitialized", true);
        report.put("serviceCodeExecuted", true);
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
        report.put("weatherState", Map.copyOf(weatherState));
        report.put("routeHazards", List.copyOf(routeHazards));
        report.put("soundState", Map.copyOf(soundState));
        report.put("soundCues", List.copyOf(soundCues));
        report.put("atmosphereState", Map.copyOf(atmosphereState));
        report.put("hudHazardReadout", Map.copyOf(hudHazardReadout));
        report.put("runtimeSnapshot", snapshot());
        report.put("diagnostics", diagnostics);
        report.put("status", diagnostics.isEmpty() ? "PASS" : "FAIL");
        report.put("summary", diagnostics.isEmpty()
                ? "AdapterCore prepared Ashfall route-hazard commands against stateful no-launch WeatherCore, SoundCore, AtmosphereCore, and HUD hazard packets without claiming live host mutation."
                : "AdapterCore route-hazard runtime target is missing required weather, sound, atmosphere, or HUD mutations.");
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
            case "weather.feed_route_hazard_context" -> {
                weatherState.putAll(payload);
                addStrings(routeHazards, payload.get("hazards"));
                logMutation(operationId, targetBridge, payload.getOrDefault("weatherProfileConsumer", "weather_state"));
            }
            case "sound.map_weather_state_to_ambience_cues" -> {
                soundState.putAll(payload);
                addStrings(soundCues, payload.get("soundCues"));
                logMutation(operationId, targetBridge, payload.getOrDefault("ambienceMode", "sound_ambience"));
            }
            case "atmosphere.publish_visibility_particle_sky_fog_profile" -> {
                atmosphereState.putAll(payload);
                logMutation(operationId, targetBridge, payload.getOrDefault("profile", "atmosphere_profile"));
            }
            case "hud.feed_hazard_weather_readout" -> {
                hudHazardReadout.putAll(payload);
                logMutation(operationId, targetBridge, payload.getOrDefault("line", "hud_hazard_readout"));
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
        snapshot.put("weatherHazardCount", routeHazards.size());
        snapshot.put("soundCueCount", soundCues.size());
        snapshot.put("weatherReadout", weatherState.getOrDefault("defaultWeatherReadout", ""));
        snapshot.put("soundWeatherState", soundState.getOrDefault("weatherState", ""));
        snapshot.put("atmosphereProfile", atmosphereState.getOrDefault("profile", ""));
        snapshot.put("particleProfile", atmosphereState.getOrDefault("particleProfile", ""));
        snapshot.put("skyFog", atmosphereState.getOrDefault("skyFog", ""));
        snapshot.put("hudHazardLine", hudHazardReadout.getOrDefault("line", ""));
        snapshot.put("nativeStateMutated", !mutationLog.isEmpty());
        snapshot.put("minecraftRuntimeAccessed", false);
        return snapshot;
    }

    private List<String> validate() {
        List<String> diagnostics = new ArrayList<>();
        if (!routeHazards.contains("echoweathercore:ash_storm")) {
            diagnostics.add("Missing WeatherCore ash storm mutation.");
        }
        if (!routeHazards.contains("echoashfallprotocol:radiation")) {
            diagnostics.add("Missing Ashfall radiation mutation.");
        }
        if (!soundCues.contains("echoashfallprotocol:event.ash_storm")) {
            diagnostics.add("Missing Ashfall ash storm sound mutation.");
        }
        if (!soundCues.contains("echosoundcore:ambient_loop")) {
            diagnostics.add("Missing SoundCore ambience mutation.");
        }
        requireValue(atmosphereState, "profile", "Missing AtmosphereCore profile mutation.", diagnostics);
        requireValue(atmosphereState, "particleProfile", "Missing atmosphere particle mutation.", diagnostics);
        requireValue(atmosphereState, "skyFog", "Missing atmosphere sky-fog mutation.", diagnostics);
        requireValue(hudHazardReadout, "line", "Missing HUD hazard readout mutation.", diagnostics);
        if (mutationLog.size() < 4) {
            diagnostics.add("Expected all route-hazard commands to mutate native state.");
        }
        return List.copyOf(diagnostics);
    }

    private static void requireValue(Map<String, Object> state, String key, String message, List<String> diagnostics) {
        Object value = state.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            diagnostics.add(message);
        }
    }
}
