package com.knoxhack.echoashfallprotocol.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Player-facing atlas for concrete POI templates.
 *
 * Scanner/save identity stays profile-level in ExplorationSiteRegistry; this
 * catalog exposes the template-pool variants for docs and terminal reference.
 */
public final class ExplorationPoiCatalog {
    private static final List<Entry> ENTRIES = List.of(
        entry("echoashfallprotocol:biomes/crash_zone_wasteland/ash_covered_ruin", "Ash Covered Ruin", "crash_zone_wasteland", "crash_zone_wasteland", List.of("crash_zone_wasteland"), false, false),
        entry("echoashfallprotocol:biomes/crash_zone_wasteland/burned_convoy", "Burned Convoy", "crash_zone_wasteland", "crash_zone_wasteland", List.of("crash_zone_wasteland"), false, false),
        entry("echoashfallprotocol:biomes/crash_zone_wasteland/cargo_lift_wreck", "Cargo Lift Wreck", "crash_zone_wasteland", "crash_zone_wasteland", List.of("crash_zone_wasteland"), false, false),
        entry("echoashfallprotocol:biomes/crash_zone_wasteland/cargo_module_field", "Cargo Module Field", "crash_zone_wasteland", "crash_zone_wasteland", List.of("crash_zone_landmarks"), true, false),
        entry("echoashfallprotocol:biomes/crash_zone_wasteland/containment_facility_ruin", "Containment Facility Ruin", "crash_zone_wasteland", "crash_zone_wasteland", List.of("crash_zone_wasteland"), false, false),
        entry("echoashfallprotocol:biomes/crash_zone_wasteland/crash_site_large", "Crash Site Large", "crash_zone_wasteland", "crash_zone_wasteland", List.of("crash_zone_wasteland"), false, false),
        entry("echoashfallprotocol:biomes/crash_zone_wasteland/radiation_field", "Radiation Field", "crash_zone_wasteland", "crash_zone_wasteland", List.of("crash_zone_wasteland"), false, false),
        entry("echoashfallprotocol:biomes/crash_zone_wasteland/crashbreak_hut", "Crashbreak Hut", "crash_zone_wasteland", "crash_zone_wasteland", List.of("crash_zone_wasteland"), false, false),
        entry("echoashfallprotocol:biomes/crash_zone_wasteland/crashbreak_worksite", "Crashbreak Worksite", "crash_zone_wasteland", "crash_zone_wasteland", List.of("crash_zone_wasteland"), false, false),
        entry("echoashfallprotocol:biomes/crash_zone_wasteland/scrap_pile_medium", "Scrap Pile Medium", "crash_zone_wasteland", "crash_zone_wasteland", List.of("crash_zone_wasteland"), false, false),
        entry("echoashfallprotocol:biomes/crash_zone_wasteland/scrap_pile_small", "Scrap Pile Small", "crash_zone_wasteland", "crash_zone_wasteland", List.of("crash_zone_wasteland"), false, false),
        entry("echoashfallprotocol:biomes/crash_zone_wasteland/ship_breaking_yard", "Ship Breaking Yard", "crash_zone_wasteland", "crash_zone_wasteland", List.of("crash_zone_wasteland"), false, false),
        entry("echoashfallprotocol:biomes/crash_zone_wasteland/wreckage_cluster", "Wreckage Cluster", "crash_zone_wasteland", "crash_zone_wasteland", List.of("crash_zone_wasteland"), false, false),
        entry("echoashfallprotocol:biomes/crash_zone_wasteland/wreckage_command_post", "Wreckage Command Post", "crash_zone_wasteland", "crash_zone_wasteland", List.of("crash_zone_wasteland"), false, false),
        entry("echoashfallprotocol:biomes/cryogenic_ruins/broken_tank", "Broken Tank", "cryogenic_ruins", "cryogenic_ruins", List.of("cryogenic_ruins"), false, false),
        entry("echoashfallprotocol:biomes/cryogenic_ruins/cryo_tank_field", "Cryo Tank Field", "cryogenic_ruins", "cryogenic_ruins", List.of("cryogenic_ruins"), false, false),
        entry("echoashfallprotocol:biomes/cryogenic_ruins/frozen_cache", "Frozen Cache", "cryogenic_ruins", "cryogenic_ruins", List.of("cryogenic_ruins"), false, false),
        entry("echoashfallprotocol:biomes/cryogenic_ruins/frozen_comms_tower", "Frozen Comms Tower", "cryogenic_ruins", "cryogenic_ruins", List.of("cryogenic_ruins"), false, false),
        entry("echoashfallprotocol:biomes/cryogenic_ruins/frozen_lab_large", "Frozen Lab Large", "cryogenic_ruins", "cryogenic_ruins", List.of("cryogenic_ruins_landmarks"), true, false),
        entry("echoashfallprotocol:biomes/cryogenic_ruins/frozen_vehicle", "Frozen Vehicle", "cryogenic_ruins", "cryogenic_ruins", List.of("cryogenic_ruins"), false, false),
        entry("echoashfallprotocol:biomes/cryogenic_ruins/ice_covered_ruin", "Ice Covered Ruin", "cryogenic_ruins", "cryogenic_ruins", List.of("cryogenic_ruins"), false, false),
        entry("echoashfallprotocol:biomes/industrial_ruins/conveyor_ruin", "Conveyor Ruin", "industrial_ruins", "industrial_ruins", List.of("industrial_ruins"), false, false),
        entry("echoashfallprotocol:biomes/industrial_ruins/crane_wreck", "Crane Wreck", "industrial_ruins", "industrial_ruins", List.of("industrial_ruins"), false, false),
        entry("echoashfallprotocol:biomes/industrial_ruins/factory_pipe_gate", "Factory Pipe Gate", "industrial_ruins", "industrial_ruins", List.of("industrial_ruins"), false, false),
        entry("echoashfallprotocol:biomes/industrial_ruins/industrial_factory", "Industrial Factory", "industrial_factory", "industrial_ruins", List.of("industrial_factory/start", "industrial_ruins"), false, false),
        entry("echoashfallprotocol:biomes/industrial_ruins/industrial_factory_shell", "Industrial Factory Shell", "industrial_factory", "industrial_ruins", List.of("industrial_ruins_landmarks"), true, false),
        entry("echoashfallprotocol:biomes/industrial_ruins/pipe_cluster", "Pipe Cluster", "industrial_ruins", "industrial_ruins", List.of("industrial_ruins"), false, false),
        entry("echoashfallprotocol:biomes/industrial_ruins/rail_signal_yard", "Rail Signal Yard", "industrial_ruins", "industrial_ruins", List.of("industrial_ruins"), false, false),
        entry("echoashfallprotocol:biomes/industrial_ruins/storage_yard", "Storage Yard", "industrial_ruins", "industrial_ruins", List.of("industrial_ruins"), false, false),
        entry("echoashfallprotocol:biomes/nexus_scar/floating_obelisk_cluster", "Floating Obelisk Cluster", "nexus_scar", "nexus_scar", List.of("nexus_scar_landmarks"), true, false),
        entry("echoashfallprotocol:biomes/nexus_scar/nexus_pylon", "Nexus Pylon", "nexus_scar", "nexus_scar", List.of("nexus_scar_landmarks"), true, false),
        entry("echoashfallprotocol:biomes/radiation_zone/containment_breach", "Containment Breach", "radiation_zone", "radiation_zone", List.of("radiation_zone"), false, false),
        entry("echoashfallprotocol:biomes/radiation_zone/contaminated_lab", "Contaminated Lab", "radiation_zone", "radiation_zone", List.of("radiation_zone"), false, false),
        entry("echoashfallprotocol:biomes/radiation_zone/fallout_shelter", "Fallout Shelter", "radiation_zone", "radiation_zone", List.of("radiation_zone"), false, false),
        entry("echoashfallprotocol:biomes/radiation_zone/irradiated_vehicle", "Irradiated Vehicle", "radiation_zone", "radiation_zone", List.of("radiation_zone"), false, false),
        entry("echoashfallprotocol:biomes/radiation_zone/power_plant_ruin", "Power Plant Ruin", "reactor_ruin", "radiation_zone", List.of("radiation_zone"), false, false),
        entry("echoashfallprotocol:biomes/radiation_zone/radiation_beacon_line", "Radiation Beacon Line", "radiation_zone", "radiation_zone", List.of("radiation_zone"), false, false),
        entry("echoashfallprotocol:biomes/radiation_zone/radiation_crater", "Radiation Crater", "radiation_zone", "radiation_zone", List.of("radiation_zone"), false, false),
        entry("echoashfallprotocol:biomes/radiation_zone/reactor_containment_ruin", "Reactor Containment Ruin", "reactor_ruin", "radiation_zone", List.of("radiation_zone_landmarks"), true, false),
        entry("echoashfallprotocol:biomes/radiation_zone/reactor_gatehouse", "Reactor Gatehouse", "reactor_ruin", "radiation_zone", List.of("radiation_zone"), false, false),
        entry("echoashfallprotocol:biomes/radiation_zone/reactor_ruin", "Reactor Ruin", "reactor_ruin", "radiation_zone", List.of("nexus_scar", "radiation_zone", "reactor_ruin/start"), false, false),
        entry("echoashfallprotocol:biomes/radiation_zone/waste_barrel_cluster", "Waste Barrel Cluster", "radiation_zone", "radiation_zone", List.of("radiation_zone"), false, false),
        entry("echoashfallprotocol:biomes/ruined_cityscape/bunker_complex", "Bunker Complex", "military_vault", "ruined_cityscape", List.of("ruined_cityscape"), false, false),
        entry("echoashfallprotocol:biomes/ruined_cityscape/collapsed_building_small", "Collapsed Building Small", "ruined_cityscape", "ruined_cityscape", List.of("ruined_cityscape"), false, false),
        entry("echoashfallprotocol:biomes/ruined_cityscape/collapsed_building_tall", "Collapsed Building Tall", "ruined_cityscape", "ruined_cityscape", List.of("ruined_cityscape"), false, false),
        entry("echoashfallprotocol:biomes/ruined_cityscape/collapsed_tower_large", "Collapsed Tower Large", "ruined_cityscape", "ruined_cityscape", List.of("ruined_city_landmarks"), true, false),
        entry("echoashfallprotocol:biomes/ruined_cityscape/data_center_ruin", "Data Center Ruin", "data_center_ruin", "ruined_cityscape", List.of("data_center_ruin", "ruined_cityscape"), false, false),
        entry("echoashfallprotocol:biomes/ruined_cityscape/military_vault", "Military Vault", "military_vault", "ruined_cityscape", List.of("military_vault", "ruined_cityscape"), false, false),
        entry("echoashfallprotocol:biomes/ruined_cityscape/parking_ruin", "Parking Ruin", "ruined_cityscape", "ruined_cityscape", List.of("ruined_cityscape"), false, false),
        entry("echoashfallprotocol:biomes/ruined_cityscape/server_farm", "Server Farm", "data_center_ruin", "ruined_cityscape", List.of("ruined_cityscape"), false, false),
        entry("echoashfallprotocol:biomes/ruined_cityscape/street_barricade", "Street Barricade", "ruined_cityscape", "ruined_cityscape", List.of("ruined_cityscape"), false, false),
        entry("echoashfallprotocol:biomes/ruined_cityscape/subway_stairwell", "Subway Stairwell", "subway_station", "ruined_cityscape", List.of("ruined_cityscape"), false, false),
        entry("echoashfallprotocol:biomes/ruined_cityscape/subway_station", "Subway Station", "subway_station", "ruined_cityscape", List.of("ruined_cityscape", "subway_station/start"), false, false),
        entry("echoashfallprotocol:biomes/ruined_plains/abandoned_homestead", "Abandoned Homestead", "ruined_plains", "ruined_plains", List.of("ruined_plains"), false, false),
        entry("echoashfallprotocol:biomes/ruined_plains/impact_crater", "Impact Crater", "ruined_plains", "ruined_plains", List.of("ruined_plains"), false, false),
        entry("echoashfallprotocol:biomes/ruined_plains/nomad_camp", "Nomad Camp", "ruined_plains", "ruined_plains", List.of("ruined_plains", "scavenger_camp"), false, false),
        entry("echoashfallprotocol:biomes/ruined_plains/relay_tower", "Relay Tower", "ruined_plains", "ruined_plains", List.of("ruined_plains"), false, false),
        entry("echoashfallprotocol:biomes/ruined_plains/scavenger_camp", "Scavenger Camp", "scavenger_camp", "ruined_plains", List.of("ruined_plains", "scavenger_camp"), false, false),
        entry("echoashfallprotocol:biomes/ruined_plains/settlement_ruins", "Settlement Ruins", "ruined_plains", "ruined_plains", List.of("ruined_plains"), false, false),
        entry("echoashfallprotocol:biomes/ruined_plains/supply_drop", "Supply Drop", "ruined_plains", "ruined_plains", List.of("ruined_plains", "scavenger_camp"), false, false),
        entry("echoashfallprotocol:biomes/ruined_plains/trader_post", "Trader Post", "ruined_plains", "ruined_plains", List.of("ruined_plains"), false, false),
        entry("echoashfallprotocol:biomes/ruined_plains/walled_encampment", "Walled Encampment", "ruined_plains", "ruined_plains", List.of("ruined_plains"), false, false),
        entry("echoashfallprotocol:biomes/ruined_plains/wasteland_bunker_ruin", "Wasteland Bunker Ruin", "ruined_plains", "ruined_plains", List.of("wasteland_landmarks"), true, false),
        entry("echoashfallprotocol:biomes/ruined_plains/windmill_ruin", "Windmill Ruin", "ruined_plains", "ruined_plains", List.of("ruined_plains"), false, false),
        entry("echoashfallprotocol:biomes/toxic_swamp/abandoned_shed", "Abandoned Shed", "toxic_swamp", "toxic_swamp", List.of("toxic_swamp"), false, false),
        entry("echoashfallprotocol:biomes/toxic_swamp/bio_facility", "Bio Facility", "bio_lab", "toxic_swamp", List.of("toxic_swamp"), false, false),
        entry("echoashfallprotocol:biomes/toxic_swamp/bio_lab", "Bio Lab", "bio_lab", "toxic_swamp", List.of("bio_lab", "toxic_swamp"), false, false),
        entry("echoashfallprotocol:biomes/toxic_swamp/broken_pipeline", "Broken Pipeline", "toxic_swamp", "toxic_swamp", List.of("toxic_swamp"), false, false),
        entry("echoashfallprotocol:biomes/toxic_swamp/chemical_spill", "Chemical Spill", "toxic_swamp", "toxic_swamp", List.of("toxic_swamp"), false, false),
        entry("echoashfallprotocol:biomes/toxic_swamp/corroded_pipe_network", "Corroded Pipe Network", "toxic_swamp", "toxic_swamp", List.of("toxic_swamp_landmarks"), true, false),
        entry("echoashfallprotocol:biomes/toxic_swamp/pipe_pump_house", "Pipe Pump House", "toxic_swamp", "toxic_swamp", List.of("toxic_swamp"), false, false),
        entry("echoashfallprotocol:biomes/toxic_swamp/sludge_drain", "Sludge Drain", "toxic_swamp", "toxic_swamp", List.of("toxic_swamp"), false, false),
        entry("echoashfallprotocol:biomes/toxic_swamp/spore_research_hut", "Spore Research Hut", "toxic_swamp", "toxic_swamp", List.of("toxic_swamp"), false, false),
        entry("echoashfallprotocol:biomes/toxic_swamp/stilted_outpost", "Stilted Outpost", "toxic_swamp", "toxic_swamp", List.of("toxic_swamp"), false, false),
        entry("echoashfallprotocol:biomes/toxic_swamp/toxic_pool_small", "Toxic Pool Small", "toxic_swamp", "toxic_swamp", List.of("toxic_swamp"), false, false),
        entry("echoashfallprotocol:faction/sporebound_sanctum/biodome_hub", "Biodome Hub", "sporebound_sanctum", "faction", List.of("sporebound_sanctum/town_centers"), false, true),
        entry("echoashfallprotocol:faction/sporebound_sanctum/processing_hut", "Processing Hut", "sporebound_sanctum", "faction", List.of("sporebound_sanctum/houses"), false, true),
        entry("echoashfallprotocol:faction/radwarden_outpost/armory", "Armory", "radwarden_outpost", "faction", List.of("radwarden_outpost/houses"), false, true),
        entry("echoashfallprotocol:faction/radwarden_outpost/barracks", "Barracks", "radwarden_outpost", "faction", List.of("radwarden_outpost/houses"), false, true),
        entry("echoashfallprotocol:faction/radwarden_outpost/command_bunker", "Command Bunker", "radwarden_outpost", "faction", List.of("radwarden_outpost/town_centers"), false, true),
        entry("echoashfallprotocol:faction/radwarden_outpost/guard_post", "Guard Post", "radwarden_outpost", "faction", List.of("radwarden_outpost/houses"), false, true),
        entry("echoashfallprotocol:faction/radwarden_outpost/street_corner", "Street Corner", "radwarden_outpost", "faction", List.of("radwarden_outpost/streets"), false, true),
        entry("echoashfallprotocol:faction/radwarden_outpost/street_cross", "Street Cross", "radwarden_outpost", "faction", List.of("radwarden_outpost/streets"), false, true),
        entry("echoashfallprotocol:faction/radwarden_outpost/street_straight", "Street Straight", "radwarden_outpost", "faction", List.of("radwarden_outpost/streets"), false, true),
        entry("echoashfallprotocol:faction/radwarden_outpost/supply_depot", "Supply Depot", "radwarden_outpost", "faction", List.of("radwarden_outpost/houses"), false, true),
        entry("echoashfallprotocol:faction/radwarden_outpost/wall_corner", "Wall Corner", "radwarden_outpost", "faction", List.of("radwarden_outpost/terminators"), false, true),
        entry("echoashfallprotocol:faction/radwarden_outpost/wall_section", "Wall Section", "radwarden_outpost", "faction", List.of("radwarden_outpost/terminators"), false, true),
        entry("echoashfallprotocol:faction/crashbreak_salvage/market_plaza", "Market Plaza", "crashbreak_salvage_yard", "faction", List.of("crashbreak_salvage_yard/town_centers"), false, true),
        entry("echoashfallprotocol:faction/crashbreak_salvage/warehouse", "Warehouse", "crashbreak_salvage_yard", "faction", List.of("crashbreak_salvage_yard/houses"), false, true),
        entry("echoashfallprotocol:global/abandoned_camp", "Abandoned Camp", "survivor_cache", "global", List.of("global"), false, false),
        entry("echoashfallprotocol:global/debris_field_large", "Debris Field Large", "survivor_cache", "global", List.of("global"), false, false),
        entry("echoashfallprotocol:global/debris_field_small", "Debris Field Small", "survivor_cache", "global", List.of("global"), false, false),
        entry("echoashfallprotocol:global/radio_relay_small", "Radio Relay Small", "survivor_cache", "global", List.of("global"), false, false),
        entry("echoashfallprotocol:global/road_checkpoint", "Road Checkpoint", "survivor_cache", "global", List.of("global"), false, false),
        entry("echoashfallprotocol:global/road_wreck", "Road Wreck", "survivor_cache", "global", List.of("global"), false, false),
        entry("echoashfallprotocol:global/survivor_cache", "Survivor Cache", "survivor_cache", "global", List.of("global"), false, false),
        entry("echoashfallprotocol:reactor_ruin", "Reactor Ruin Alias", "reactor_ruin", "radiation_zone", List.of("nexus_scar"), false, false)
    );

    private static final Map<String, List<Entry>> BY_PROFILE = buildByProfile();

    private ExplorationPoiCatalog() {
    }

    public static List<Entry> all() {
        return ENTRIES;
    }

    public static List<Entry> forProfile(String profileId) {
        return BY_PROFILE.getOrDefault(ExplorationSiteRegistry.normalize(profileId), List.of());
    }

    public static Map<String, List<Entry>> byProfile() {
        return BY_PROFILE;
    }

    public static int totalTemplateCount() {
        return ENTRIES.size();
    }

    public static List<String> validationWarnings() {
        List<String> warnings = new ArrayList<>();
        Set<String> seenTemplates = new HashSet<>();
        for (Entry entry : ENTRIES) {
            if (!seenTemplates.add(entry.templateLocation())) {
                warnings.add("duplicate template location " + entry.templateLocation());
            }
            if (!entry.templateLocation().startsWith("echoashfallprotocol:")) {
                warnings.add(entry.templateLocation() + " is not an Ashfall template location");
            }
            if (ExplorationSiteRegistry.get(entry.profileId()).isEmpty()) {
                warnings.add(entry.templateLocation() + " maps to missing profile " + entry.profileId());
            }
            if (entry.displayName().isBlank()) {
                warnings.add(entry.templateLocation() + " has no display name");
            }
            if (entry.category().isBlank()) {
                warnings.add(entry.templateLocation() + " has no category");
            }
            if (entry.pools().isEmpty()) {
                warnings.add(entry.templateLocation() + " has no template pool");
            }
        }
        return warnings;
    }

    private static Entry entry(String templateLocation, String displayName, String profileId, String category,
            List<String> pools, boolean landmark, boolean faction) {
        return new Entry(templateLocation, displayName, ExplorationSiteRegistry.normalize(profileId),
                category, pools, landmark, faction);
    }

    private static Map<String, List<Entry>> buildByProfile() {
        Map<String, List<Entry>> grouped = new LinkedHashMap<>();
        for (Entry entry : ENTRIES) {
            grouped.computeIfAbsent(entry.profileId(), ignored -> new ArrayList<>()).add(entry);
        }
        Map<String, List<Entry>> immutable = new LinkedHashMap<>();
        for (Map.Entry<String, List<Entry>> group : grouped.entrySet()) {
            immutable.put(group.getKey(), List.copyOf(group.getValue()));
        }
        return Collections.unmodifiableMap(immutable);
    }

    public record Entry(
            String templateLocation,
            String displayName,
            String profileId,
            String category,
            List<String> pools,
            boolean landmark,
            boolean faction
    ) {
        public Entry {
            templateLocation = templateLocation == null ? "" : templateLocation.trim();
            displayName = displayName == null ? "" : displayName.trim();
            profileId = ExplorationSiteRegistry.normalize(profileId);
            category = category == null ? "" : category.trim();
            pools = List.copyOf(pools == null ? List.of() : pools);
        }

        public String templatePath() {
            int colon = templateLocation.indexOf(':');
            return colon >= 0 && colon + 1 < templateLocation.length()
                    ? templateLocation.substring(colon + 1)
                    : templateLocation;
        }

        public String poolSummary() {
            return String.join(", ", pools);
        }

        public String markerLabel() {
            if (faction) {
                return "faction";
            }
            return landmark ? "landmark" : category;
        }
    }
}
