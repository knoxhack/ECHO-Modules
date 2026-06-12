package com.knoxhack.echoopenlandsprotocol.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class OpenlandsStarterSpawnGuarantees {
    public static final Set<String> ALLOWED_STARTER_BIOMES = Set.of("meadows", "woodlands");
    public static final int GUARANTEED_RESOURCE_RADIUS_BLOCKS = 64;
    public static final int VISIBLE_LANDMARK_RADIUS_BLOCKS = 128;
    public static final int MINIMUM_HOSTILE_CLEAR_RADIUS_BLOCKS = 16;

    private OpenlandsStarterSpawnGuarantees() {
    }

    public static OpenlandsStarterSpawnResult validate(OpenlandsStarterSpawnSnapshot snapshot) {
        List<String> satisfied = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        require(ALLOWED_STARTER_BIOMES.contains(snapshot.biomeId()), "allowed_starter_biome", satisfied, missing);
        require(snapshot.starterResourceRadiusBlocks() <= GUARANTEED_RESOURCE_RADIUS_BLOCKS, "starter_resources_inside_64_blocks", satisfied, missing);
        require(snapshot.visibleLandmarkDistanceBlocks() <= VISIBLE_LANDMARK_RADIUS_BLOCKS, "visible_landmark_inside_128_blocks", satisfied, missing);
        require(snapshot.nearestHostileDistanceBlocks() >= MINIMUM_HOSTILE_CLEAR_RADIUS_BLOCKS, "hostiles_outside_16_blocks", satisfied, missing);
        require(snapshot.hasWoodSource(), "wood_source_found", satisfied, missing);
        require(snapshot.hasLooseStone(), "loose_stone_found", satisfied, missing);
        require(snapshot.hasFiberSource(), "fiber_source_found", satisfied, missing);
        require(snapshot.hasStarterFood(), "starter_food_found", satisfied, missing);
        require(snapshot.hasWaterOrWellHint(), "water_or_well_hint_found", satisfied, missing);
        require(snapshot.hasExplorationHook(), "cave_road_or_ruin_hook_found", satisfied, missing);
        return new OpenlandsStarterSpawnResult(missing.isEmpty(), satisfied, missing, "reject_seed_or_regenerate_starter_area");
    }

    private static void require(boolean condition, String id, List<String> satisfied, List<String> missing) {
        if (condition) satisfied.add(id);
        else missing.add(id);
    }
}
