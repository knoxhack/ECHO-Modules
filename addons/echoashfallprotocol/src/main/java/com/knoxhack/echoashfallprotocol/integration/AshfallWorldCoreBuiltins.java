package com.knoxhack.echoashfallprotocol.integration;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.IRegionService;
import com.knoxhack.echocore.api.WorldRegionDefinition;
import com.knoxhack.echocore.api.WorldRegionType;
import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import java.util.List;
import net.minecraft.resources.Identifier;

/**
 * Ashfall-specific world regions registered into WorldCore when both mods are present.
 * <p>
 * Previously these were hardcoded inside {@code WorldCoreBuiltins} and registered
 * unconditionally, causing the ECHO stack to ship Ashfall biome/structure references
 * even when running without Ashfall.
 */
public final class AshfallWorldCoreBuiltins {
    public static final Identifier CRASH_ZONE = ashfall("crash_zone_wasteland");
    public static final Identifier RUINED_CITY = ashfall("ruined_cityscape");
    public static final Identifier TOXIC_SWAMP = ashfall("toxic_swamp");
    public static final Identifier RADIATION_ZONE = ashfall("radiation_zone");
    public static final Identifier CRYOGENIC_RUINS = ashfall("cryogenic_ruins");
    public static final Identifier NEXUS_SCAR = ashfall("nexus_scar");
    public static final Identifier RADWARDEN_OUTPOST = ashfall("radwarden_outpost");

    private static final String WORLDCORE = "echoworldcore";

    private AshfallWorldCoreBuiltins() {
    }

    public static void register() {
        IRegionService regions = EchoCoreServices.regionService();
        registerRegions(regions);
        EchoAshfallProtocol.LOGGER.info("Registered Ashfall world regions into WorldCore.");
    }

    private static void registerRegions(IRegionService service) {
        service.registerRegionDefinition(region(CRASH_ZONE, WorldRegionType.CRASH_ZONE,
                "Crash Zone Wasteland",
                "Impact-scattered wreckage fields and Ashfall crash debris.",
                List.of(ashfall("crash_zone_wasteland")),
                List.of(ashfall("common_wasteland_biomes"), ashfall("has_structure/crash_zone_wasteland")),
                List.of(ashfall("crash_zone_wasteland"), ashfall("crash_zone_landmarks"), ashfall("drop_pod")),
                List.of(worldcore("hazard/salvage_debris")), 10));
        service.registerRegionDefinition(region(RUINED_CITY, WorldRegionType.RUINED_CITY,
                "Ruined Cityscape",
                "Collapsed urban grid, data ruins, and buried transit signals.",
                List.of(ashfall("ruined_cityscape")),
                List.of(ashfall("rare_wasteland_biomes"), ashfall("has_structure/ruined_cityscape")),
                List.of(ashfall("ruined_cityscape"), ashfall("ruined_city_landmarks"), ashfall("data_center_ruin"),
                        ashfall("subway_station"), ashfall("military_vault")),
                List.of(worldcore("hazard/salvage_debris")), 20));
        service.registerRegionDefinition(region(TOXIC_SWAMP, WorldRegionType.TOXIC_SWAMP,
                "Toxic Swamp",
                "Corroded wetlands, chemical runoff, bio-lab remains, and spore exposure.",
                List.of(ashfall("toxic_swamp")),
                List.of(ashfall("toxic_air_biomes"), ashfall("hazardous_wasteland_biomes"),
                        ashfall("has_structure/toxic_swamp")),
                List.of(ashfall("toxic_swamp"), ashfall("toxic_swamp_landmarks"), ashfall("bio_lab"),
                        ashfall("sporebound_sanctum")),
                List.of(worldcore("hazard/toxic_air")), 30));
        service.registerRegionDefinition(region(RADIATION_ZONE, WorldRegionType.RADIATION_ZONE,
                "Radiation Zone",
                "Fallout-heavy terrain around reactor ruins and exposed radiation pockets.",
                List.of(ashfall("radiation_zone")),
                List.of(ashfall("radiation_biomes"), ashfall("hazardous_wasteland_biomes"),
                        ashfall("has_structure/radiation_zone")),
                List.of(ashfall("radiation_zone"), ashfall("radiation_zone_landmarks"), ashfall("reactor_ruin")),
                List.of(worldcore("hazard/radiation")), 40));
        service.registerRegionDefinition(region(CRYOGENIC_RUINS, WorldRegionType.CRYOGENIC_RUINS,
                "Cryogenic Ruins",
                "Frozen laboratories, shattered cryo tanks, and cold-storage wreckage.",
                List.of(ashfall("cryogenic_ruins")),
                List.of(ashfall("cryogenic_biomes"), ashfall("has_structure/cryogenic_ruins")),
                List.of(ashfall("cryogenic_ruins"), ashfall("cryogenic_ruins_landmarks")),
                List.of(worldcore("hazard/cryo_cold")), 50));
        service.registerRegionDefinition(region(NEXUS_SCAR, WorldRegionType.NEXUS_SCAR,
                "Nexus Scar",
                "Corrupted rift terrain where Ashfall and Nexus field logic overlap.",
                List.of(ashfall("nexus_scar")),
                List.of(ashfall("nexus_anomaly_biomes"), ashfall("has_structure/nexus_scar")),
                List.of(ashfall("nexus_scar"), ashfall("nexus_scar_landmarks")),
                List.of(worldcore("hazard/nexus_anomaly"), worldcore("hazard/radiation")), 60));
        service.registerRegionDefinition(region(RADWARDEN_OUTPOST, WorldRegionType.SECURE_OUTPOST,
                "Secure Outpost",
                "Faction or recovery foothold with stabilized field infrastructure.",
                List.of(), List.of(),
                List.of(ashfall("radwarden_outpost"), ashfall("crashbreak_salvage_yard"), ashfall("scavenger_camp")),
                List.of(worldcore("hazard/secure_zone")), 90));
    }

    private static WorldRegionDefinition region(Identifier id, WorldRegionType type, String name, String summary,
            List<Identifier> biomes, List<Identifier> biomeTags, List<Identifier> structures,
            List<Identifier> hazards, int sortOrder) {
        return new WorldRegionDefinition(id, type, name, summary, biomes, biomeTags, structures, hazards,
                id, 96, Identifier.fromNamespaceAndPath(WORLDCORE, "region/" + id.getPath()),
                Identifier.fromNamespaceAndPath(WORLDCORE, "ambience/" + id.getPath()), sortOrder);
    }

    private static Identifier ashfall(String path) {
        return Identifier.fromNamespaceAndPath(EchoAshfallProtocol.MODID, path);
    }

    private static Identifier worldcore(String path) {
        return Identifier.fromNamespaceAndPath(WORLDCORE, path);
    }
}
