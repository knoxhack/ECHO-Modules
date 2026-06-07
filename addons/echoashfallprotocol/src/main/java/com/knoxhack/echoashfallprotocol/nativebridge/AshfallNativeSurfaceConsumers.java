package com.knoxhack.echoashfallprotocol.nativebridge;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AshfallNativeSurfaceConsumers {
    private static final String MODULE_ID = "echoashfallprotocol";

    private AshfallNativeSurfaceConsumers() {
    }

    public static Map<String, Object> uiHudScreenSafe(Map<String, Object> firstJoinProfile) {
        Map<String, Object> screenSafeSurfaces = childMap(firstJoinProfile, "screenSafeSurfaces");
        Map<String, Object> recoveryPlan = childMap(firstJoinProfile, "recoveryPlan");

        List<Map<String, Object>> consumers = List.of(
                consumer("echohudcore:mission_tracker", "native_hud_mission_tracker",
                        "Consumes the first-join mission line for a screen-safe opening objective.",
                        map("line", screenSafeSurfaces.getOrDefault("hudMissionLine", "Place an Ash Campfire near the crash site"),
                                "anchor", screenSafeSurfaces.getOrDefault("notificationAnchor", "top_left_safe_area"))),
                consumer("echohudcore:hazard_readout", "native_hud_hazard_readout",
                        "Consumes the opening hazard line until live weather state is bridged.",
                        map("line", screenSafeSurfaces.getOrDefault("hudHazardLine", "AIR stable; hazards marked."),
                                "source", "firstJoinCrashRecoveryProfile")),
                consumer("echoscreencore:welcome_surface", "native_welcome_screen",
                        "Consumes the welcome screen target as a screen-safe native UI page.",
                        map("screen", screenSafeSurfaces.getOrDefault("welcomeScreen", "echoashfallprotocol:welcome_screen"),
                                "packet", "echoashfallprotocol:welcome_screen")),
                consumer("echoterminal:first_ten_minutes_card", "terminal_card",
                        "Consumes the first-ten-minutes card handoff for Terminal when installed.",
                        map("card", screenSafeSurfaces.getOrDefault("terminalCard", "ashfall:first_ten_minutes"),
                                "condition", "module_loaded:echoterminal")),
                consumer("echowiki:ashfall", "wiki_guide",
                        "Consumes the Ashfall guide handoff for Wiki/Codex opening guidance.",
                        map("guide", screenSafeSurfaces.getOrDefault("wikiGuide", "echowiki:ashfall"),
                                "condition", "module_loaded:echowiki")),
                consumer("echolens:opening_route_scan", "lens_scan_profile",
                        "Consumes first route Lens scan profiles for native route inspection.",
                        map("profiles", recoveryPlan.getOrDefault("lensProfiles", List.of()))),
                consumer("echoholomap:opening_recovery_layers", "holomap_layers",
                        "Consumes first route map layers for recovery and field-cache visibility.",
                        map("layers", recoveryPlan.getOrDefault("mapLayerHints", List.of()))),
                consumer("echocodexcore:opening_entries", "codex_entries",
                        "Consumes opening route Codex/Index entries for first route prep.",
                        map("entries", recoveryPlan.getOrDefault("codexEntries", List.of())))
        );

        return surface("echoashfallprotocol:first_join_ui_hud_consumers", "ui_hud_screen_safe",
                "AdapterCore UI/HUD/screen-safe consumers for the Ashfall first-join recovery profile.",
                consumers,
                List.of("hud.mission_tracker", "hud.hazard_readout", "screen.welcome",
                        "terminal.card", "wiki.guide", "lens.scans", "holomap.layers", "codex.entries"));
    }

    public static Map<String, Object> weatherSoundAtmosphere(Map<String, Object> firstJoinProfile) {
        Map<String, Object> atmosphereHooks = childMap(firstJoinProfile, "atmosphereHooks");

        List<Map<String, Object>> consumers = List.of(
                consumer("echoweathercore:weather_state", "weather_route_hazard_state",
                        "Consumes Ashfall opening route hazards as WeatherCore state input.",
                        map("hazards", atmosphereHooks.getOrDefault("routeHazards", List.of()),
                                "defaultWeatherReadout", atmosphereHooks.getOrDefault("defaultWeatherReadout", "CLEAR"))),
                consumer("echosoundcore:ambience", "soundcore_ambience_cues",
                        "Consumes Ashfall weather cue ids as SoundCore ambience and warning inputs.",
                        map("soundCues", atmosphereHooks.getOrDefault("soundCues", List.of()),
                                "mode", "route_weather_context")),
                consumer("echoatmospherecore:storm_visibility", "atmosphere_visibility_profile",
                        "Consumes Ashfall route hazard context as AtmosphereCore visibility profile input.",
                        map("profile", atmosphereHooks.getOrDefault("atmosphereProfileConsumer", "echoatmospherecore:storm_visibility"),
                                "fog", "ashfall_opening_route_haze",
                                "screenHazeIntensity", 0.18D,
                                "stormVisibility", 0.62D)),
                consumer("echoatmospherecore:ambient_particles", "atmosphere_particle_profile",
                        "Consumes route hazard context as an ambient ash particle profile input.",
                        map("particleProfile", "echoashfallprotocol:opening_route_ash_particles",
                                "density", 0.2D,
                                "weatherLinked", true))
        );

        return surface("echoashfallprotocol:route_weather_sound_atmosphere_consumers", "weather_sound_atmosphere",
                "AdapterCore WeatherCore/SoundCore/AtmosphereCore consumers for Ashfall opening route hazard context.",
                consumers,
                List.of("weather.route_hazards", "sound.weather_cues", "sound.ambience",
                        "atmosphere.visibility", "atmosphere.particles"));
    }

    private static Map<String, Object> surface(String id, String surface, String summary,
                                               List<Map<String, Object>> consumers, List<String> features) {
        List<String> diagnostics = validate(consumers);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", id);
        data.put("moduleId", MODULE_ID);
        data.put("surface", surface);
        data.put("adapterCoreBridge", true);
        data.put("implementationTarget", "AdapterCore native surface consumers");
        data.put("standaloneDuplicateGameplaySystem", false);
        data.put("executionMode", "jdk_only_consumer_binding_rehearsal");
        data.put("minecraftRuntimeAccessed", false);
        data.put("minecraftRegistryMutated", false);
        data.put("consumerCount", consumers.size());
        data.put("consumers", consumers);
        data.put("features", features);
        data.put("diagnostics", diagnostics);
        data.put("status", diagnostics.isEmpty() ? "PASS" : "FAIL");
        data.put("summary", summary);
        return data;
    }

    private static Map<String, Object> consumer(String id, String kind, String summary, Map<String, Object> payload) {
        Map<String, Object> consumer = new LinkedHashMap<>();
        consumer.put("id", id);
        consumer.put("kind", kind);
        consumer.put("summary", summary);
        consumer.put("adapterCoreConsumer", true);
        consumer.put("status", "pending_live_bridge");
        consumer.put("minecraftRuntimeAccessed", false);
        consumer.put("payload", payload);
        return consumer;
    }

    private static List<String> validate(List<Map<String, Object>> consumers) {
        List<String> diagnostics = new ArrayList<>();
        if (consumers.isEmpty()) {
            diagnostics.add("Expected at least one AdapterCore surface consumer.");
        }
        for (Map<String, Object> consumer : consumers) {
            if (!Boolean.TRUE.equals(consumer.get("adapterCoreConsumer"))) {
                diagnostics.add("Consumer " + consumer.get("id") + " is not marked as AdapterCore-backed.");
            }
            if (Boolean.TRUE.equals(consumer.get("minecraftRuntimeAccessed"))) {
                diagnostics.add("Consumer " + consumer.get("id") + " accessed Minecraft runtime.");
            }
        }
        return List.copyOf(diagnostics);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> childMap(Map<String, Object> parent, String key) {
        if (parent != null && parent.get(key) instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private static Map<String, Object> map(Object... entries) {
        if (entries.length % 2 != 0) {
            throw new IllegalArgumentException("map entries must be key/value pairs");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (int index = 0; index < entries.length; index += 2) {
            result.put((String) entries[index], entries[index + 1]);
        }
        return result;
    }
}
