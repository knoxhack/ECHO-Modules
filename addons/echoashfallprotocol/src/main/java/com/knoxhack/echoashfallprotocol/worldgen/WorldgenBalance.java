package com.knoxhack.echoashfallprotocol.worldgen;

/**
 * Shared world-generation density targets for the Sparse Survival pass.
 *
 * Values are in chunks and mirror the datapack structure_set JSONs. Keeping the
 * numbers here gives the legacy Java spawner and future debug tools one clear
 * reference for the intended cadence.
 *
 * POI Category Definitions (Sparse Survival target):
 *
 * | Category   | Spacing | Separation | Examples                                  |
 * |------------|---------|------------|-------------------------------------------|
 * | Global     | 34      | 13         | survivor_cache, road_wreck, radio_relay   |
 * | Biome/Micro| 22      | 9          | nomad_camp, windmill_ruin, scrap_piles    |
 * | Crash      | 18      | 7          | crash debris, wreck camps, salvage scars   |
 * | Urban      | 18      | 7          | city blocks, factories, industrial yards   |
 * | Camp       | 22      | 9          | scavenger_camp, supply_drop (ruined_plains)|
 * | Landmark   | 52      | 20         | faction villages, industrial_factory, subway|
 * | Major      | 60      | 24         | bio_lab, data_center, military_vault, reactor|
 *
 * Datapack structure_set JSONs are the authority for POI generation.
 * The legacy POIStructureSpawner is disabled by default.
 */
public final class WorldgenBalance {
    private WorldgenBalance() {}

    /** Biome-specific micro POIs (nomad_camp, windmill_ruin, etc.) */
    public static final int MICRO_POI_SPACING = 22;
    public static final int MICRO_POI_SEPARATION = 9;

    /** Crash-zone POIs: denser wreckage without becoming a city biome. */
    public static final int CRASH_POI_SPACING = 18;
    public static final int CRASH_POI_SEPARATION = 7;

    /** Urban and industrial POIs: denser ruin fields for built-up biomes. */
    public static final int URBAN_POI_SPACING = 18;
    public static final int URBAN_POI_SEPARATION = 7;

    /** Global POIs that can spawn in any biome */
    public static final int GLOBAL_POI_SPACING = 34;
    public static final int GLOBAL_POI_SEPARATION = 13;

    /** Camp-tier POIs (scavenger_camp, supply_drop) */
    public static final int CAMP_SPACING = 22;
    public static final int CAMP_SEPARATION = 9;

    /** Landmark structures (faction villages, factories) */
    public static final int LANDMARK_SPACING = 52;
    public static final int LANDMARK_SEPARATION = 20;

    /** Major rare structures (bio_lab, data_center, etc.) */
    public static final int MAJOR_SPACING = 60;
    public static final int MAJOR_SEPARATION = 24;
}
