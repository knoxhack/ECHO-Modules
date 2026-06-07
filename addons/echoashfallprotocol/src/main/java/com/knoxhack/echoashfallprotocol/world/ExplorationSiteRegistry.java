package com.knoxhack.echoashfallprotocol.world;

import com.knoxhack.echoashfallprotocol.faction.AshfallBiomeFactions;
import com.knoxhack.echoashfallprotocol.faction.AshfallFactionMap;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * One source of truth for exploration-facing POI identity.
 *
 * Registry/worldgen ids stay under EchoAshfallProtocol for compatibility, but the
 * scanner, missions, terminal copy, and old save aliases all resolve through
 * these profiles so sites do not collapse into broad legacy buckets.
 */
public final class ExplorationSiteRegistry {
    private static final Map<String, SiteProfile> SITES = new LinkedHashMap<>();
    private static final Map<String, String> STRUCTURE_TO_SITE = new HashMap<>();
    private static final Map<String, String> ALIASES = new HashMap<>();

    public static final String DEFAULT_SITE_ID = "unknown_signal";

    static {
        register(site(
                "drop_pod", "Drop Pod Wreck", "Crash Recovery Route",
                "Early salvage beacon from a failed rescue pod or emergency impact site, still warm enough to matter.",
                SiteKind.SURVIVAL_CACHE, POIData.DangerLevel.MEDIUM, HazardProfile.SALVAGE,
                "carry water, food, and a weapon; do not assume the beacon is alone",
                "Survival", "pod scrap, emergency water, rations, basic parts, damaged telemetry",
                "Recover the cache, confirm no survivor signal remains, and log the pod telemetry.",
                "drop_pod_survival", null, 5, false,
                List.of("drop_pod"),
                List.of("crash_pod", "pod_wreck")));

        register(site(
                "crash_zone_wasteland", "Crash Zone Wreck Field", "Crash Wreck Route",
                "Scattered command wreckage, salvage huts, and ash-buried caches around the first broken rescue route.",
                SiteKind.LANDMARK, POIData.DangerLevel.MEDIUM, HazardProfile.SALVAGE,
                "pack clean water, food, bandages, and tools; the ash hides sharp metal",
                "Survival", "starter salvage, clean water, medicine, scrap circuits, command notes",
                "Search the wreckage, recover any route notes, then return before night.",
                "crash_zone_wasteland_cache", null, 10, false,
                List.of("crash_zone_wasteland", "crash_zone_landmarks"),
                List.of("crash_zone", "wreckage_command_post", "crashbreak_hut")));

        register(site(
                "survivor_cache", "Survivor Cache Signal", "Global Recovery Route",
                "Small shelters, road wrecks, radio relays, and abandoned camps where somebody made one more plan than they lived to use.",
                SiteKind.SURVIVAL_CACHE, POIData.DangerLevel.SAFE, HazardProfile.SAFE,
                "carry water and a weapon; recovery stops still attract hunger",
                "Survival", "dirty water, food, torches, medicine, route notes, last messages",
                "Loot the cache, archive any survivor note, and keep moving.",
                "survivor_cache", null, 8, false,
                List.of("global"),
                List.of("observation_post", "abandoned_camp", "radio_relay_small", "road_wreck")));

        register(site(
                "ruined_plains", "Ruined Plains Expedition Site", "Open Wasteland Route",
                "Open-route camps, relay towers, windmill ruins, trader posts, and supply drops where the horizon looks safer than it is.",
                SiteKind.LANDMARK, POIData.DangerLevel.MEDIUM, HazardProfile.SALVAGE,
                "pack water, food, medicine, and scanner charge; open ground burns time",
                "Exploration", "maps, light salvage, trade goods, survivor supplies",
                "Scan the camp or relay, then mark the route for later faction work.",
                "crashbreak_salvage_yard_cache", null, 12, false,
                List.of("ruined_plains", "wasteland_landmarks"),
                List.of("relay_tower", "trader_post", "nomad_camp", "supply_drop")));

        register(site(
                "scavenger_camp", "Scavenger Camp", "Scavenger Camp Route",
                "A salvage camp with loose contracts, useful caches, and enough armed suspicion to keep trade honest.",
                SiteKind.LANDMARK, POIData.DangerLevel.MEDIUM, HazardProfile.COMBAT,
                "bring a weapon, bandages, water, and empty trade space",
                "Utility", "scavenger cache, route maps, scrap, filters, rumor value",
                "Secure the cache and decide whether the local Crashbreak crew is worth owing.",
                "scavenger_camp_cache", AshfallBiomeFactions.CRASHBREAK_SALVAGE, 12, false,
                List.of("scavenger_camp"),
                List.of("salvage_camp")));

        register(site(
                "crashbreak_salvage_yard", "Crashbreak Salvage Yard", "Crashbreak Salvage Route",
                "A neutral trade post where Crashbreak turns ruin logistics, rumor, and debt into survival.",
                SiteKind.FACTION_HUB, POIData.DangerLevel.SAFE, HazardProfile.SAFE,
                "safe zone; bring trade goods, maps, and empty inventory space",
                "Utility", "trades, maps, imported goods, rare salvage, contract leads",
                "Contact the trader and archive the route board before prices change.",
                "crashbreak_salvage_yard_cache", AshfallBiomeFactions.CRASHBREAK_SALVAGE, 15, true,
                List.of("crashbreak_salvage_yard"),
                List.of("crashbreak_salvage", "crashbreak_salvage_yard")));

        register(site(
                "radwarden_outpost", "Radwarden Outpost", "Radwarden Containment Route",
                "A disciplined Radwarden field base built around old security doctrine and new fear.",
                SiteKind.FACTION_HUB, POIData.DangerLevel.HIGH, HazardProfile.COMBAT,
                "bring armor, medicine, clean water, and visible respect for the perimeter",
                "Combat", "armor parts, ammunition stock, dense salvage, faction work",
                "Make contact or survey the perimeter without making their patrol write your ending.",
                "radwarden_outpost_cache", AshfallBiomeFactions.RADWARDEN_COMPACT, 18, false,
                List.of("radwarden_outpost"),
                List.of("radwarden_outpost", "military_outpost")));

        register(site(
                "sporebound_sanctum", "Sporebound Sanctum", "Sporebound Bio Route",
                "A living enclave where adapted survivors study spores, medicine, mutation pressure, and the price of staying human enough.",
                SiteKind.FACTION_HUB, POIData.DangerLevel.HIGH, HazardProfile.TOXIC_AIR,
                "bring a gas mask with filter, clean water, RadAway, and patience",
                "Bio", "medicine, tissue, mutagen, filters, bio schematics, adaptation notes",
                "Enter carefully, contact the elder, and log the bio-processing route.",
                "sporebound_sanctum_cache", AshfallBiomeFactions.SPOREBOUND_SANCTUM, 18, false,
                List.of("sporebound_sanctum"),
                List.of("sporebound_sanctum", "bio_dome", "sporebound_biodome")));

        register(site(
                "toxic_swamp", "Toxic Swamp Field Site", "Toxic Swamp Route",
                "Chemical spills, broken pipelines, stilted outposts, and spore huts in toxic-air pockets that test filters by the minute.",
                SiteKind.HAZARD_SITE, POIData.DangerLevel.HIGH, HazardProfile.TOXIC_AIR,
                "equip gas mask and filter; carry clean water, medicine, and a spare exit",
                "Bio", "filters, medicine, tissue samples, chemical salvage, spore readings",
                "Scan the toxic source, recover samples, and leave before the filter becomes a memory.",
                "bio_lab_cache", AshfallBiomeFactions.SPOREBOUND_SANCTUM, 16, false,
                List.of("toxic_swamp", "toxic_swamp_landmarks"),
                List.of("spore_research_hut", "stilted_outpost", "chemical_spill", "broken_pipeline")));

        register(site(
                "bio_lab", "Bio Lab", "Bio Lab Route",
                "A broken research lab with medicine, tissue samples, and unstable lifeforms still following trial protocol.",
                SiteKind.MAIN_SITE, POIData.DangerLevel.HIGH, HazardProfile.TOXIC_AIR,
                "bring gas mask and filter, bandages, clean water, RadAway, and room for samples",
                "Bio", "medicine, tissue, filters, restoration samples, schematics, failed cures",
                "Recover a data log or biological sample from the lab core without joining the experiment.",
                "bio_lab_cache", AshfallBiomeFactions.SPOREBOUND_SANCTUM, 20, false,
                List.of("bio_lab"),
                List.of("bio_facility", "bio_archive")));

        register(site(
                "industrial_ruins", "Industrial Ruins Worksite", "Industrial Worksite Route",
                "Conveyor shells, storage yards, pipe clusters, and corrosion-heavy factory grounds where old quotas still feel enforced.",
                SiteKind.HAZARD_SITE, POIData.DangerLevel.HIGH, HazardProfile.TOXIC_AIR,
                "bring gas mask and filter, repair supplies, water, bandages, and patience for dead machines",
                "Utility", "machine parts, circuits, filters, industrial scrap, repair leads",
                "Repair or scan the worksite marker, then recover the main cache.",
                "industrial_factory_cache", AshfallBiomeFactions.RUSTWORKS_UNION, 18, false,
                List.of("industrial_ruins", "industrial_ruins_landmarks"),
                List.of("conveyor_ruin", "storage_yard", "pipe_cluster")));

        register(site(
                "industrial_factory", "Industrial Factory", "Factory Route",
                "A larger factory shell with useful machinery, sealed workcells, and stronger local pressure.",
                SiteKind.MAIN_SITE, POIData.DangerLevel.HIGH, HazardProfile.TOXIC_AIR,
                "bring filters, repair tools, medicine, and an exit route that does not rely on luck",
                "Utility", "machine parts, factory components, circuits, schematics, controller residue",
                "Scan the factory controller remains and recover the cache.",
                "industrial_factory_cache", AshfallBiomeFactions.RUSTWORKS_UNION, 20, false,
                List.of("industrial_factory"),
                List.of("derelict_workshop", "factory_ruin")));

        register(site(
                "ruined_cityscape", "Ruined City Block", "Urban Block Route",
                "Collapsed towers, barricades, server shells, parking ruins, and urban ambush routes with too many windows.",
                SiteKind.LANDMARK, POIData.DangerLevel.MEDIUM, HazardProfile.URBAN_COMBAT,
                "bring a weapon, torches, water, bandages, and a habit of checking corners",
                "Utility", "data drives, circuits, survivor notes, city salvage, old access cards",
                "Archive the block and recover any data cache before pushing deeper.",
                "data_center_cache", null, 14, false,
                List.of("ruined_cityscape", "ruined_city_landmarks"),
                List.of("city_block", "collapsed_building", "parking_ruin")));

        register(site(
                "data_center_ruin", "Data Center Ruin", "Data Center Route",
                "A dead server complex where scanner parts, logs, and old-world data survive under bad power.",
                SiteKind.MAIN_SITE, POIData.DangerLevel.HIGH, HazardProfile.URBAN_COMBAT,
                "bring water, medicine, scanner charge, light, and something for close corridors",
                "Utility", "data logs, circuits, scanner parts, schematics, corrupted keys",
                "Recover a data log from the server core before the building remembers alarms.",
                "data_center_cache", AshfallBiomeFactions.METRO_ARCHIVISTS, 20, false,
                List.of("data_center_ruin"),
                List.of("data_center", "server_farm")));

        register(site(
                "subway_station", "Subway Station", "Subway Route",
                "Sheltered transit tunnels with emergency caches, old crowd-control gates, and dangerous sight lines.",
                SiteKind.LANDMARK, POIData.DangerLevel.MEDIUM, HazardProfile.URBAN_COMBAT,
                "bring torches, food, water, bandages, and a clean way back up",
                "Survival", "emergency caches, shelter supplies, route maps, tunnel notes",
                "Survey the platform and recover the emergency cache.",
                "subway_station_cache", AshfallBiomeFactions.METRO_ARCHIVISTS, 16, false,
                List.of("subway_station"),
                List.of("subway", "transit_tunnel")));

        register(site(
                "military_vault", "Military Vault", "Vault Route",
                "A hardened storage site with dense materials, combat pressure, and high-value tech behind old orders.",
                SiteKind.MAIN_SITE, POIData.DangerLevel.EXTREME, HazardProfile.COMBAT,
                "bring armor, medicine, clean water, and route-specific hazard gear",
                "Combat", "dense alloy, weapons, armor, power cells, sealed orders",
                "Secure the vault cache or defeat the local guardian.",
                "military_vault_cache", AshfallBiomeFactions.RADWARDEN_COMPACT, 25, false,
                List.of("military_vault"),
                List.of("vault", "bunker_complex")));

        register(site(
                "radiation_zone", "Radiation Zone Hotspot", "Radiation Hotspot Route",
                "Containment breaches, craters, fallout shelters, and irradiated vehicles where silence is not safety.",
                SiteKind.HAZARD_SITE, POIData.DangerLevel.CRITICAL, HazardProfile.RADIATION,
                "bring RadAway, clean water, hazmat if available, and a short route plan",
                "Recovery", "RadAway, isotopes, reactor salvage, sealed supplies, exposure readings",
                "Measure the hot zone, recover shelter supplies, then retreat and treat exposure.",
                "reactor_ruin_cache", AshfallBiomeFactions.RADWARDEN_COMPACT, 20, false,
                List.of("radiation_zone", "radiation_zone_landmarks"),
                List.of("contaminated_lab", "fallout_shelter", "radiation_crater")));

        register(site(
                "reactor_ruin", "Reactor Ruin", "Reactor Route",
                "A severe reactor site where scrubber pockets and RadAway matter more than bravery.",
                SiteKind.MAIN_SITE, POIData.DangerLevel.EXTREME, HazardProfile.RADIATION,
                "bring RadAway, hazmat or armor, clean water, and scrubber support",
                "Combat", "isotopes, Nexus crystals, power-node leads, dense components, hot-zone logs",
                "Scan the reactor core and recover the power-node lead before exposure writes the report.",
                "reactor_ruin_cache", AshfallBiomeFactions.RADWARDEN_COMPACT, 28, false,
                List.of("reactor_ruin"),
                List.of("relay_station_east", "power_plant_ruin", "reactor")));

        register(site(
                "cryogenic_ruins", "Cryogenic Ruins", "Cryogenic Route",
                "Frozen pods, broken tanks, cold caches, and brittle facility ruins where time stopped without permission.",
                SiteKind.MAIN_SITE, POIData.DangerLevel.CRITICAL, HazardProfile.CRYO_COLD,
                "bring hand warmers, food, thermal liner, clean water, and a warm exit",
                "Cryo", "cold gear, preserved schematics, ice salvage, data drives, thaw notes",
                "Recover a cryogenic sample and warm up before deeper entry.",
                "cryogenic_ruins_cache", AshfallBiomeFactions.THAWBOUND_COLLECTIVE, 24, false,
                List.of("cryogenic_ruins", "cryogenic_ruins_landmarks"),
                List.of("frozen_cache", "ice_covered_ruin", "cryo_ruins")));

        register(site(
                "nexus_scar", "Nexus Scar", "Nexus Route",
                "A late-route anomaly scar combining toxic pressure, radiation instability, mutation risk, and the Core's unfinished math.",
                SiteKind.MAIN_SITE, POIData.DangerLevel.EXTREME, HazardProfile.NEXUS_ANOMALY,
                "bring elite filters, RadAway, hazmat, full medicine, best weapons, and no loose pride",
                "Nexus", "Nexus crystals, anomaly data, endgame salvage, command residue",
                "Stabilize or destroy the scar anchor after the route is secured.",
                "reactor_ruin_cache", null, 30, false,
                List.of("nexus_scar", "nexus_scar_landmarks"),
                List.of("nexus_anchor", "scar_anchor")));

        register(site(
                "relay_station", "Relay Station", "Signal Route",
                "A damaged signal node that extends scanner range, supports distant outposts, and sometimes repeats voices it should not have.",
                SiteKind.RELAY, POIData.DangerLevel.MEDIUM, HazardProfile.SAFE,
                "bring repair parts, power cells, water, and a weapon",
                "Utility", "circuit boards, scrap circuits, energy cells, routing packets",
                "Repair the relay and archive the routing packet.",
                "relay_station_cache", null, 15, true,
                List.of("relay_station"),
                List.of("relay_station_north", "relay_station_south", "radio_tower")));

        register(site(
                "satellite_array", "Satellite Array", "Satellite Signal Route",
                "A broken skyward receiver still listening for pre-Gridfall routing packets and quarantine static.",
                SiteKind.LANDMARK, POIData.DangerLevel.HIGH, HazardProfile.SALVAGE,
                "bring repair parts, water, and combat supplies",
                "Utility", "satellite parts, circuits, data drives, orbital noise",
                "Scan the receiver dish and recover its data cache.",
                "satellite_cache", null, 18, false,
                List.of("satellite_array"),
                List.of("crashed_satellite")));

        register(site(
                "sewer_junction", "Sewer Junction", "City / Toxic Route",
                "A wet utility route with emergency supplies, toxic pockets, and city runoff that learned bad chemistry.",
                SiteKind.LANDMARK, POIData.DangerLevel.HIGH, HazardProfile.TOXIC_AIR,
                "bring mask/filter, torches, clean water, bandages, and spare patience",
                "Survival", "sewer salvage, clay route items, medicine, filters, maintenance tags",
                "Mark the junction and recover the maintenance cache.",
                "sewer_cache", AshfallBiomeFactions.SPOREBOUND_SANCTUM, 16, false,
                List.of("sewer_junction"),
                List.of("sewer")));

        register(site(
                "train_yard", "Train Yard", "Industrial / City Route",
                "Rusted transit stock and freight containers with good salvage, exposed combat lanes, and old cargo manifests.",
                SiteKind.LANDMARK, POIData.DangerLevel.HIGH, HazardProfile.COMBAT,
                "bring armor, water, medicine, and a spare weapon",
                "Utility", "freight salvage, rails, circuits, machine parts, cargo manifests",
                "Secure the freight cache and archive the yard route.",
                "train_yard_cache", AshfallBiomeFactions.RUSTWORKS_UNION, 16, false,
                List.of("train_yard"),
                List.of("rail_yard")));

        register(site(
                "abandoned_mine", "Abandoned Mine", "Resource Route",
                "A deep extraction site with cave-ins, materials, and enough darkness to punish greed.",
                SiteKind.RESOURCE_SITE, POIData.DangerLevel.HIGH, HazardProfile.COMBAT,
                "bring armor, food, water, light, and a retreat buffer",
                "Resource", "rare metals, minerals, fossils, mining salvage, sealed core samples",
                "Recover one deep-site cache without overextending.",
                "abandoned_mine_cache", null, 16, false,
                List.of("abandoned_mine"),
                List.of("mine")));
    }

    private ExplorationSiteRegistry() {
    }

    public static Optional<SiteProfile> get(String id) {
        return Optional.ofNullable(SITES.get(normalize(id)));
    }

    public static SiteProfile getOrFallback(String id) {
        return get(id).orElseGet(() -> fallback(id));
    }

    public static Optional<SiteProfile> findByStructure(String structureId) {
        String key = stripNamespace(structureId);
        String siteId = STRUCTURE_TO_SITE.getOrDefault(key, normalize(key));
        return get(siteId);
    }

    public static SiteProfile getByStructureOrFallback(String structureId) {
        return findByStructure(structureId).orElseGet(() -> fallback(structureId));
    }

    public static String normalize(String id) {
        String key = stripNamespace(id);
        if (key.isEmpty()) {
            return DEFAULT_SITE_ID;
        }
        return ALIASES.getOrDefault(key, key);
    }

    public static Set<String> aliasesFor(String id) {
        SiteProfile profile = get(normalize(id)).orElse(null);
        if (profile == null) {
            return Set.of(normalize(id));
        }
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        ids.add(profile.id());
        ids.addAll(profile.aliases());
        ids.addAll(profile.structureIds());
        return Collections.unmodifiableSet(ids);
    }

    public static Collection<SiteProfile> all() {
        return Collections.unmodifiableCollection(SITES.values());
    }

    public static List<SiteProfile> allSorted() {
        return SITES.values().stream()
                .sorted(Comparator.comparing(SiteProfile::route).thenComparing(SiteProfile::displayName))
                .toList();
    }

    public static List<String> validationWarnings() {
        List<String> warnings = new ArrayList<>();
        for (SiteProfile profile : SITES.values()) {
            if (profile.displayName().isBlank()) {
                warnings.add(profile.id() + " has no display name");
            }
            if (profile.route().isBlank()) {
                warnings.add(profile.id() + " has no route");
            }
            if (profile.description().isBlank()) {
                warnings.add(profile.id() + " has no description");
            }
            if (profile.structureIds().isEmpty()) {
                warnings.add(profile.id() + " has no structure ids");
            }
            if (profile.lootTable().isBlank()) {
                warnings.add(profile.id() + " has no loot table");
            }
            if (profile.prepHint().isBlank()) {
                warnings.add(profile.id() + " has no prep hint");
            }
            if (profile.rewardTrack().isBlank()) {
                warnings.add(profile.id() + " has no reward track");
            }
            if (profile.resourceProfile().isBlank()) {
                warnings.add(profile.id() + " has no resource profile");
            }
            if (profile.objective().isBlank()) {
                warnings.add(profile.id() + " has no objective text");
            }
            if (profile.hazardProfile() == HazardProfile.UNKNOWN) {
                warnings.add(profile.id() + " has unknown hazard profile");
            }
        }
        for (Map.Entry<String, String> entry : STRUCTURE_TO_SITE.entrySet()) {
            if (!SITES.containsKey(entry.getValue())) {
                warnings.add("structure " + entry.getKey() + " maps to missing site " + entry.getValue());
            }
        }
        return warnings;
    }

    public static List<String> templateLocationsForCategory(String category) {
        String normalized = stripNamespace(category);
        List<String> locations = new ArrayList<>();
        for (SiteProfile profile : SITES.values()) {
            for (String id : profile.structureIds()) {
                if (id.equals(normalized) || id.startsWith(normalized + "/")) {
                    locations.add(id);
                }
            }
        }
        return locations;
    }

    private static SiteProfile site(
            String id,
            String displayName,
            String route,
            String description,
            SiteKind kind,
            POIData.DangerLevel dangerLevel,
            HazardProfile hazardProfile,
            String prepHint,
            String rewardTrack,
            String resourceProfile,
            String objective,
            String lootTable,
            @Nullable Identifier faction,
            int researchPoints,
            boolean fastTravel,
            List<String> structureIds,
            List<String> aliases
    ) {
        return new SiteProfile(
                id, displayName, route, description, kind, dangerLevel, hazardProfile,
                prepHint, rewardTrack, resourceProfile, objective, lootTable,
                faction == null ? null : AshfallFactionMap.canonicalOrDefault(faction),
                researchPoints, fastTravel,
                Set.copyOf(structureIds), Set.copyOf(aliases)
        );
    }

    private static void register(SiteProfile profile) {
        SITES.put(profile.id(), profile);
        ALIASES.put(profile.id(), profile.id());
        for (String alias : profile.aliases()) {
            ALIASES.put(stripNamespace(alias), profile.id());
        }
        for (String structureId : profile.structureIds()) {
            STRUCTURE_TO_SITE.put(stripNamespace(structureId), profile.id());
        }
    }

    private static SiteProfile fallback(String rawId) {
        String id = normalize(rawId);
        String display = prettify(id);
        return new SiteProfile(
                id, display, "Uncatalogued", "Signal origin matches a recoverable point of interest, but ECHO lacks a clean profile.",
                SiteKind.LANDMARK, POIData.DangerLevel.MEDIUM, HazardProfile.UNKNOWN,
                "scan again closer before committing", "Exploration", "mixed salvage and uncertain route intel",
                "Approach, verify the site, and archive local hazards.", "survivor_cache", null,
                10, false, Set.of(id), Set.of()
        );
    }

    private static String stripNamespace(String id) {
        if (id == null) {
            return "";
        }
        String value = id.trim();
        if (value.isEmpty()) {
            return "";
        }
        int namespace = value.indexOf(':');
        if (namespace >= 0 && namespace + 1 < value.length()) {
            value = value.substring(namespace + 1);
        }
        int slash = value.lastIndexOf('/');
        if (slash >= 0 && slash + 1 < value.length()) {
            value = value.substring(slash + 1);
        }
        int bracket = value.lastIndexOf(']');
        if (bracket >= 0) {
            value = value.substring(0, bracket);
        }
        return value.toLowerCase(java.util.Locale.ROOT);
    }

    private static String prettify(String id) {
        String[] words = stripNamespace(id).replace('_', ' ').split(" ");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                builder.append(word.substring(1));
            }
        }
        return builder.isEmpty() ? "Unknown Signal" : builder.toString();
    }

    public static Identifier lootTableId(SiteProfile profile) {
        return Identifier.fromNamespaceAndPath("echoashfallprotocol", "chests/" + profile.lootTable());
    }

    public enum SiteKind {
        FACTION_HUB,
        MAIN_SITE,
        LANDMARK,
        SURVIVAL_CACHE,
        HAZARD_SITE,
        RELAY,
        RESOURCE_SITE
    }

    public enum HazardProfile {
        SAFE("Safe", "No major hazard profile; keep basic supplies anyway."),
        SALVAGE("Salvage", "Normal travel with local debris, sharp wreckage, and light combat."),
        TOXIC_AIR("Toxic Air", "Gas mask and filter matter inside marked toxic pockets."),
        RADIATION("Radiation", "RadAway, hazmat, and short exposure loops matter."),
        CRYO_COLD("Cryo Cold", "Warmth, food, shelter, and thermal gear matter."),
        NEXUS_ANOMALY("Nexus Anomaly", "Mixed endgame hazard pressure with mutation instability and command residue."),
        COMBAT("Combat", "Armor, medicine, and weapon durability matter."),
        URBAN_COMBAT("Urban Combat", "Light, cover, medicine, and route awareness matter."),
        UNKNOWN("Unknown", "Hazard profile not yet classified; trust distance before confidence.");

        private final String displayName;
        private final String guidance;

        HazardProfile(String displayName, String guidance) {
            this.displayName = displayName;
            this.guidance = guidance;
        }

        public String displayName() {
            return displayName;
        }

        public String guidance() {
            return guidance;
        }
    }

    public record SiteProfile(
            String id,
            String displayName,
            String route,
            String description,
            SiteKind kind,
            POIData.DangerLevel dangerLevel,
            HazardProfile hazardProfile,
            String prepHint,
            String rewardTrack,
            String resourceProfile,
            String objective,
            String lootTable,
            @Nullable Identifier faction,
            int researchPoints,
            boolean fastTravel,
            Set<String> structureIds,
            Set<String> aliases
    ) {
        public String hazardName() {
            return hazardProfile.displayName();
        }

        public String[] lootDescriptions() {
            return resourceProfile.split(", ");
        }

        public String[] features() {
            return new String[] {
                    "Route: " + route,
                    "Hazard: " + hazardProfile.displayName(),
                    "Objective: " + objective
            };
        }

        public String[] requiredGear() {
            return prepHint.split(", ");
        }

        public POIData.POIType poiType() {
            return switch (kind) {
                case FACTION_HUB -> POIData.POIType.FACTION_HUB;
                case RELAY -> POIData.POIType.FAST_TRAVEL;
                case RESOURCE_SITE -> POIData.POIType.RESOURCE_SITE;
                case SURVIVAL_CACHE -> POIData.POIType.TUTORIAL;
                default -> POIData.POIType.WORLD_LOCATION;
            };
        }
    }
}
