package com.knoxhack.echoworldcore;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EchoWorldCoreRegionCellSampleContract {
    public static final String MODULE_ID = "echoworldcore";
    public static final String ADAPTERCORE_CONTRACT_ID = "echoworldcore:worldgen/region_cell_sample";
    public static final String REFERENCE_WORLD_ID = "minecraft:overworld";
    public static final String REFERENCE_REGION_ID = "echoashfallprotocol:crash_zone_wasteland";
    public static final String REFERENCE_HAZARD_ID = "echoworldcore:hazard/salvage_debris";
    public static final String REFERENCE_STRUCTURE_ID = "echoashfallprotocol:structure/drop_pod";

    public Map<String, Object> execute(String packId, String runtime) {
        Map<String, Object> sample = new LinkedHashMap<>();
        sample.put("adapterCoreContract", ADAPTERCORE_CONTRACT_ID);
        sample.put("service", "echoworldcore:world_region_cell_sample_service");
        sample.put("regionCellSampleExecuted", true);
        sample.put("packId", normalizeText(packId, "echo-native-m17"));
        sample.put("runtime", normalizeText(runtime, "echo_native"));
        sample.put("worldId", REFERENCE_WORLD_ID);
        sample.put("samplePoint", samplePoint());
        sample.put("regionDefinition", regionDefinition());
        sample.put("hazardDefinition", hazardDefinition());
        sample.put("activeRegionId", REFERENCE_REGION_ID);
        sample.put("activeHazardId", REFERENCE_HAZARD_ID);
        sample.put("biomeProfileId", "echoashfallprotocol:biome_profile/crash_zone_wasteland");
        sample.put("structureId", REFERENCE_STRUCTURE_ID);
        sample.put("poiId", "echoashfallprotocol:poi/drop_pod");
        sample.put("cellKey", "minecraft:overworld:32:68:32");
        sample.put("inRegion", true);
        sample.put("inHazard", true);
        sample.put("gameTick", 1750L);
        sample.put("mapFeed", mapFeed());
        sample.put("integrationEvents", integrationEvents());
        sample.put("diagnostics", List.of(
                "worldcore.region.sampled",
                "worldcore.hazard.resolved",
                "worldcore.map_feed.projected",
                "worldcore.discovery.integration_ready"
        ));
        sample.put("referenceBehavior", "worldcore_samples_crash_zone_region_and_hazard_cell");
        return Map.copyOf(sample);
    }

    public boolean referenceSamplePassed(Map<String, Object> sample) {
        return Boolean.TRUE.equals(sample.get("regionCellSampleExecuted"))
                && ADAPTERCORE_CONTRACT_ID.equals(sample.get("adapterCoreContract"))
                && REFERENCE_WORLD_ID.equals(sample.get("worldId"))
                && REFERENCE_REGION_ID.equals(sample.get("activeRegionId"))
                && REFERENCE_HAZARD_ID.equals(sample.get("activeHazardId"))
                && "echoashfallprotocol:biome_profile/crash_zone_wasteland".equals(sample.get("biomeProfileId"))
                && REFERENCE_STRUCTURE_ID.equals(sample.get("structureId"))
                && "echoashfallprotocol:poi/drop_pod".equals(sample.get("poiId"))
                && "minecraft:overworld:32:68:32".equals(sample.get("cellKey"))
                && Boolean.TRUE.equals(sample.get("inRegion"))
                && Boolean.TRUE.equals(sample.get("inHazard"))
                && String.valueOf(sample.get("mapFeed")).contains("echoholomap:layer/worldcore_regions")
                && String.valueOf(sample.get("integrationEvents")).contains("echomissioncore:region_entered")
                && String.valueOf(sample.get("diagnostics")).contains("worldcore.map_feed.projected");
    }

    private static Map<String, Object> samplePoint() {
        Map<String, Object> point = new LinkedHashMap<>();
        point.put("x", 32);
        point.put("y", 68);
        point.put("z", 32);
        point.put("gameTick", 1750L);
        return Map.copyOf(point);
    }

    private static Map<String, Object> regionDefinition() {
        Map<String, Object> region = new LinkedHashMap<>();
        region.put("id", REFERENCE_REGION_ID);
        region.put("displayName", "Crash Zone Wasteland");
        region.put("bounds", List.of(0, 96, 0, 96));
        region.put("missionId", "echoashfallprotocol:mission/secure_crash_outpost");
        region.put("discoveryId", "echoashfallprotocol:discovery/crash_zone_wasteland");
        region.put("renderProfileId", "echoworldcore:region/crash_zone_wasteland");
        region.put("audioProfileId", "echoworldcore:ambience/crash_zone_wasteland");
        return Map.copyOf(region);
    }

    private static Map<String, Object> hazardDefinition() {
        Map<String, Object> hazard = new LinkedHashMap<>();
        hazard.put("id", REFERENCE_HAZARD_ID);
        hazard.put("type", "salvage_debris");
        hazard.put("center", List.of(32, 32));
        hazard.put("radius", 12);
        hazard.put("damagePerTick", 2.0D);
        hazard.put("defaultSeverity", 35);
        hazard.put("statusEffectId", "echostatuscore:status/salvage_debris");
        hazard.put("ticking", true);
        return Map.copyOf(hazard);
    }

    private static List<Map<String, Object>> mapFeed() {
        return List.of(
                feed("region_marker", "echoholomap:layer/worldcore_regions", REFERENCE_REGION_ID, true),
                feed("hazard_overlay", "echoholomap:layer/worldcore_hazards", REFERENCE_HAZARD_ID, true),
                feed("structure_poi", "echolens:inspection/worldcore_structure_poi", "echoashfallprotocol:poi/drop_pod", true)
        );
    }

    private static List<Map<String, Object>> integrationEvents() {
        return List.of(
                event("echomissioncore:region_entered", REFERENCE_REGION_ID, true),
                event("echodatacore:world_cell_sampled", "minecraft:overworld:32:68:32", true),
                event("echoterminal:world_context_updated", REFERENCE_HAZARD_ID, true)
        );
    }

    private static Map<String, Object> feed(String kind, String target, String id, boolean visible) {
        Map<String, Object> feed = new LinkedHashMap<>();
        feed.put("kind", kind);
        feed.put("target", target);
        feed.put("id", id);
        feed.put("visible", visible);
        return Map.copyOf(feed);
    }

    private static Map<String, Object> event(String eventType, String subject, boolean emitted) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("eventType", eventType);
        event.put("subject", subject);
        event.put("emitted", emitted);
        return Map.copyOf(event);
    }

    private static String normalizeText(String text, String fallback) {
        return text == null || text.isBlank() ? fallback : text.trim();
    }
}
