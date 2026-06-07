package com.knoxhack.echoweathercore.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.knoxhack.echoweathercore.EchoWeatherCore;
import com.knoxhack.echoweathercore.api.weather.WeatherEffectModifiers;
import com.knoxhack.echoweathercore.api.weather.WeatherProfile;
import com.knoxhack.echoweathercore.api.weather.WeatherScope;
import com.knoxhack.echoweathercore.api.weather.WeatherSeverity;
import com.knoxhack.echoweathercore.api.weather.WeatherType;
import java.io.Reader;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

public class WeatherDataReloadListener extends SimplePreparableReloadListener<Map<Identifier, WeatherProfile>> {
    public static final WeatherDataReloadListener INSTANCE = new WeatherDataReloadListener();
    private static final List<String> PROFILE_DIRS = List.of(
            "weather_profiles",
            "echoweathercore/weather_profiles"
    );

    private final Map<Identifier, WeatherProfile> profiles = new HashMap<>();

    private WeatherDataReloadListener() {}

    @Override
    protected Map<Identifier, WeatherProfile> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<Identifier, WeatherProfile> result = new HashMap<>();
        for (String dir : PROFILE_DIRS) {
            for (Map.Entry<Identifier, Resource> entry : manager.listResources(dir, id -> id.getPath().endsWith(".json")).entrySet()) {
                try (Reader reader = entry.getValue().openAsReader()) {
                    JsonElement root = JsonParser.parseReader(reader);
                    if (root.isJsonObject()) {
                        WeatherProfile profile = parseProfile(entry.getKey(), root.getAsJsonObject());
                        if (profile != null) {
                            result.put(profile.id(), profile);
                        }
                    }
                } catch (Exception e) {
                    EchoWeatherCore.LOGGER.error("Failed to load weather profile {} from {}: {}", entry.getKey(), dir, e.getMessage());
                }
            }
        }
        return result;
    }

    @Override
    protected void apply(Map<Identifier, WeatherProfile> map, ResourceManager manager, ProfilerFiller profiler) {
        profiles.clear();
        profiles.putAll(map);
        EchoWeatherCore.LOGGER.info("Loaded {} weather profiles.", profiles.size());
    }

    public void registerProfile(WeatherProfile profile) {
        if (profile != null) {
            profiles.put(profile.id(), profile);
        }
    }

    private WeatherProfile parseProfile(Identifier fileId, JsonObject json) {
        Identifier id = Identifier.parse(json.get("id").getAsString());
        String displayName = json.has("displayName") ? json.get("displayName").getAsString() : id.toString();
        WeatherType type = WeatherType.valueOf(json.get("type").getAsString());
        WeatherSeverity severity = WeatherSeverity.valueOf(json.get("defaultSeverity").getAsString());
        WeatherScope scope = WeatherScope.valueOf(json.get("scope").getAsString());
        int durationTicks = json.has("durationTicks") ? json.get("durationTicks").getAsInt() : 12000;
        int warningTicks = json.has("warningTicks") ? json.get("warningTicks").getAsInt() : 2400;
        int weight = json.has("weight") ? json.get("weight").getAsInt() : 10;
        int cooldownTicks = json.has("cooldownTicks") ? json.get("cooldownTicks").getAsInt() : 24000;

        Set<Identifier> allowedDimensions = parseStringSet(json, "allowedDimensions");
        Set<Identifier> allowedBiomes = parseStringSet(json, "allowedBiomes");
        Set<Identifier> allowedRegionTags = parseStringSet(json, "allowedRegionTags");
        Set<Identifier> disallowedBiomes = parseStringSet(json, "disallowedBiomes");

        int minimumProgression = json.has("minimumProgression") ? json.get("minimumProgression").getAsInt() : 0;
        int earliestGameDay = json.has("earliestGameDay") ? json.get("earliestGameDay").getAsInt() : 0;

        WeatherEffectModifiers effects = parseEffects(json);
        List<Identifier> recommendedGear = parseResourceList(json, "recommendedGear");
        List<Identifier> possibleResources = parseResourceList(json, "possibleResources");
        String terminalWarning = json.has("terminalWarning") ? json.get("terminalWarning").getAsString() : "";
        List<String> echoLines = parseStringList(json, "echoLines");
        String holomapOverlayMetadata = json.has("holomapOverlayMetadata") ? json.get("holomapOverlayMetadata").getAsString() : "";
        String lensScanText = json.has("lensScanText") ? json.get("lensScanText").getAsString() : "";
        String soundCoreAmbienceId = json.has("soundCoreAmbienceId") ? json.get("soundCoreAmbienceId").getAsString() : "";
        String particleVisualProfileId = json.has("particleVisualProfileId") ? json.get("particleVisualProfileId").getAsString() : "";
        boolean enabled = !json.has("enabled") || json.get("enabled").getAsBoolean();

        return new WeatherProfile(id, displayName, type, severity, scope, durationTicks, warningTicks, weight,
            cooldownTicks, allowedDimensions, allowedBiomes, allowedRegionTags, disallowedBiomes,
            minimumProgression, earliestGameDay, effects, recommendedGear, possibleResources, terminalWarning,
            echoLines, holomapOverlayMetadata, lensScanText, soundCoreAmbienceId, particleVisualProfileId, enabled);
    }

    private Set<Identifier> parseStringSet(JsonObject json, String key) {
        if (!json.has(key) || !json.get(key).isJsonArray()) return Set.of();
        return json.getAsJsonArray(key).asList().stream()
            .map(e -> Identifier.parse(e.getAsString()))
            .collect(Collectors.toSet());
    }

    private List<Identifier> parseResourceList(JsonObject json, String key) {
        if (!json.has(key) || !json.get(key).isJsonArray()) return List.of();
        return json.getAsJsonArray(key).asList().stream()
            .map(e -> Identifier.parse(e.getAsString()))
            .collect(Collectors.toList());
    }

    private List<String> parseStringList(JsonObject json, String key) {
        if (!json.has(key) || !json.get(key).isJsonArray()) return List.of();
        return json.getAsJsonArray(key).asList().stream()
            .map(JsonElement::getAsString)
            .collect(Collectors.toList());
    }

    private WeatherEffectModifiers parseEffects(JsonObject json) {
        if (!json.has("effects")) return WeatherEffectModifiers.DEFAULT;
        JsonObject ef = json.getAsJsonObject("effects");
        return new WeatherEffectModifiers(
            getDouble(ef, "visibilityMultiplier", 1.0),
            getDouble(ef, "scannerRangeMultiplier", 1.0),
            getDouble(ef, "scannerReliabilityMultiplier", 1.0),
            getDouble(ef, "holomapReliabilityMultiplier", 1.0),
            getDouble(ef, "filterDrainMultiplier", 1.0),
            getDouble(ef, "radiationExposureMultiplier", 1.0),
            getDouble(ef, "toxicExposureMultiplier", 1.0),
            getDouble(ef, "coldExposureMultiplier", 1.0),
            getDouble(ef, "heatExposureMultiplier", 1.0),
            getDouble(ef, "hydrationDrainMultiplier", 1.0),
            getDouble(ef, "solarPowerMultiplier", 1.0),
            getDouble(ef, "powerGridInstabilityMultiplier", 1.0),
            getDouble(ef, "batteryEfficiencyMultiplier", 1.0),
            getDouble(ef, "machineHeatMultiplier", 1.0),
            getDouble(ef, "droneScoutReliability", 1.0),
            getDouble(ef, "droneRecallRisk", 1.0),
            getDouble(ef, "mobSightMultiplier", 1.0),
            getDouble(ef, "mobAggressionMultiplier", 1.0),
            getDouble(ef, "factionPatrolActivityMultiplier", 1.0),
            getDouble(ef, "routeRiskModifier", 1.0)
        );
    }

    private double getDouble(JsonObject obj, String key, double defaultVal) {
        return obj.has(key) ? obj.get(key).getAsDouble() : defaultVal;
    }

    public Map<Identifier, WeatherProfile> getProfiles() {
        return Collections.unmodifiableMap(profiles);
    }

    public WeatherProfile getProfile(Identifier id) {
        return profiles.get(id);
    }
}
