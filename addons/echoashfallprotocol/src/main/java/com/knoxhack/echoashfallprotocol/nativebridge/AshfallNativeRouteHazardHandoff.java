package com.knoxhack.echoashfallprotocol.nativebridge;

import com.knoxhack.echo.adaptercore.EchoNativeCommandBridge;
import com.knoxhack.echo.adaptercore.EchoNativeAtmosphereStateBridge;
import com.knoxhack.echo.adaptercore.EchoNativeAtmosphereRuntimeTarget;
import com.knoxhack.echo.adaptercore.EchoNativeSurfaceHostInvocationContract;
import com.knoxhack.echo.adaptercore.EchoNativeSurfaceHostCallQueue;

import java.util.List;
import java.util.Map;

public final class AshfallNativeRouteHazardHandoff {
    private static final String MODULE_ID = "echoashfallprotocol";

    private AshfallNativeRouteHazardHandoff() {
    }

    public static Map<String, Object> describe(Map<String, Object> firstJoinProfile,
                                               Map<String, Object> weatherSoundAtmosphereConsumers) {
        Map<String, Object> atmosphereHooks = childMap(firstJoinProfile, "atmosphereHooks");
        Map<String, Object> screenSafeSurfaces = childMap(firstJoinProfile, "screenSafeSurfaces");
        EchoNativeCommandBridge commandBridge = new EchoNativeCommandBridge(MODULE_ID)
                .command(10, "weather_sound_atmosphere", "weather.feed_route_hazard_context",
                        "echoweathercore:weather_state",
                        "AshfallNativeFirstJoinProfile.atmosphereHooks.routeHazards",
                        map("hazards", atmosphereHooks.getOrDefault("routeHazards", List.of()),
                                "defaultWeatherReadout", atmosphereHooks.getOrDefault("defaultWeatherReadout", "CLEAR"),
                                "weatherProfileConsumer", atmosphereHooks.getOrDefault("weatherProfileConsumer", "echoweathercore:weather_state")))
                .command(20, "weather_sound_atmosphere", "sound.map_weather_state_to_ambience_cues",
                        "echosoundcore:ambience",
                        "Ashfall weather cue ids and SoundCore ambience profile",
                        map("weatherState", atmosphereHooks.getOrDefault("defaultWeatherReadout", "CLEAR"),
                                "soundCues", atmosphereHooks.getOrDefault("soundCues", List.of()),
                                "ambienceMode", "route_weather_context",
                                "warningCue", "echosoundcore:ui_warning"))
                .command(30, "weather_sound_atmosphere", "atmosphere.publish_visibility_particle_sky_fog_profile",
                        "echoatmospherecore:storm_visibility",
                        "Ashfall opening route visibility, particle, and sky-fog profile",
                        map("profile", atmosphereHooks.getOrDefault("atmosphereProfileConsumer", "echoatmospherecore:storm_visibility"),
                                "fog", "ashfall_opening_route_haze",
                                "skyFog", "ashfall_opening_route_sky_fog",
                                "screenHazeIntensity", 0.18D,
                                "stormVisibility", 0.62D,
                                "particleProfile", "echoashfallprotocol:opening_route_ash_particles",
                                "particleDensity", 0.2D))
                .command(40, "ui_hud_screen_safe", "hud.feed_hazard_weather_readout",
                        "echohudcore:hazard_readout",
                        "Ashfall welcome HUD hazard line backed by route weather context",
                        map("line", screenSafeSurfaces.getOrDefault("hudHazardLine", "AIR stable; hazards marked."),
                                "source", "echoashfallprotocol:route_hazard_context",
                                "screenSafe", true));

        Map<String, Object> report = commandBridge.describe(
                "echoashfallprotocol:route_hazard_atmosphere_handoff",
                "AdapterCore WeatherCore SoundCore AtmosphereCore route hazard command handoff",
                valueOrDefault(firstJoinProfile, "id", "echoashfallprotocol:first_join_crash_recovery"),
                valueOrDefault(weatherSoundAtmosphereConsumers, "id", "echoashfallprotocol:route_weather_sound_atmosphere_consumers"),
                requiredOperationIds(),
                pendingConcreteRuntimeBridges(),
                "AdapterCore command handoff publishes Ashfall route hazards to WeatherCore, maps weather state to SoundCore ambience/cues, feeds AtmosphereCore visibility/particle/sky-fog profiles, and updates the HUD hazard readout.",
                "AdapterCore route hazard handoff is missing required weather, sound, atmosphere, or HUD commands.");
        report.put("surface", "weather_sound_atmosphere");
        report.put("surfaces", List.of("weather_sound_atmosphere", "ui_hud_screen_safe"));
        report.put("consumerEvidence", valueOrDefault(weatherSoundAtmosphereConsumers, "id",
                "echoashfallprotocol:route_weather_sound_atmosphere_consumers"));
        report.put("appliedAtmosphereState", new EchoNativeAtmosphereStateBridge(MODULE_ID).apply(
                "echoashfallprotocol:route_hazard_atmosphere_state_application",
                childList(report, "commands")));
        Map<String, Object> atmosphereRuntimeTarget = new EchoNativeAtmosphereRuntimeTarget(MODULE_ID).execute(
                "echoashfallprotocol:route_hazard_atmosphere_runtime_target",
                childList(report, "commands"));
        report.put("atmosphereRuntimeTarget", atmosphereRuntimeTarget);
        Map<String, Object> atmosphereHostInvocationContract = new EchoNativeSurfaceHostInvocationContract(MODULE_ID).prepare(
                "echoashfallprotocol:route_hazard_atmosphere_host_invocation_contract",
                "adaptercore.weather_sound_atmosphere_host_invocation",
                "weather_sound_atmosphere",
                childList(report, "commands"),
                atmosphereRuntimeTarget,
                atmosphereHostRequirements());
        report.put("atmosphereHostInvocationContract", atmosphereHostInvocationContract);
        report.put("atmosphereHostCallQueue", new EchoNativeSurfaceHostCallQueue(MODULE_ID).prepare(
                "echoashfallprotocol:route_hazard_atmosphere_host_call_queue",
                atmosphereHostInvocationContract));
        return report;
    }

    private static List<String> requiredOperationIds() {
        return List.of(
                "weather.feed_route_hazard_context",
                "sound.map_weather_state_to_ambience_cues",
                "atmosphere.publish_visibility_particle_sky_fog_profile",
                "hud.feed_hazard_weather_readout"
        );
    }

    private static List<String> pendingConcreteRuntimeBridges() {
        return List.of(
                "native_weather_state_bridge",
                "native_sound_ambience_bridge",
                "native_atmosphere_visibility_bridge",
                "native_hud_notification_bridge"
        );
    }

    private static List<EchoNativeSurfaceHostInvocationContract.HostRequirement> atmosphereHostRequirements() {
        return List.of(
                hostRequirement("native_weather_state_adapter", "echoweathercore:weather_state",
                        "weathercore.feed_route_hazard_context", "weatherState"),
                hostRequirement("native_sound_ambience_adapter", "echosoundcore:ambience",
                        "soundcore.map_weather_state_to_ambience_cues", "soundState"),
                hostRequirement("native_atmosphere_visibility_adapter", "echoatmospherecore:storm_visibility",
                        "atmospherecore.publish_visibility_particle_sky_fog", "atmosphereState"),
                hostRequirement("native_hud_hazard_weather_adapter", "echohudcore:hazard_readout",
                        "hudcore.publish_hazard_weather_readout", "hudHazardReadout")
        );
    }

    private static EchoNativeSurfaceHostInvocationContract.HostRequirement hostRequirement(
            String invocationId,
            String targetBridgeId,
            String hostSurfaceApi,
            String payloadSource) {
        return new EchoNativeSurfaceHostInvocationContract.HostRequirement(
                invocationId,
                targetBridgeId,
                hostSurfaceApi,
                payloadSource);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> childMap(Map<String, Object> parent, String key) {
        if (parent != null && parent.get(key) instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> childList(Map<String, Object> parent, String key) {
        if (parent != null && parent.get(key) instanceof List<?> list) {
            return (List<Map<String, Object>>) list;
        }
        return List.of();
    }

    private static Object valueOrDefault(Map<String, Object> map, String key, Object fallback) {
        return map == null ? fallback : map.getOrDefault(key, fallback);
    }

    private static Map<String, Object> map(Object... entries) {
        if (entries.length % 2 != 0) {
            throw new IllegalArgumentException("map entries must be key/value pairs");
        }
        java.util.LinkedHashMap<String, Object> result = new java.util.LinkedHashMap<>();
        for (int index = 0; index < entries.length; index += 2) {
            result.put((String) entries[index], entries[index + 1]);
        }
        return result;
    }
}
