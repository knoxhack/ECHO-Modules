package com.knoxhack.echoashfallprotocol.schematic;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Table mapping SchematicTier → list of recipe IDs that tier unlocks.
 *
 * Recipe IDs here are logical identifiers used by the schematic system;
 * they do not need to be real vanilla recipe ResourceLocations. Downstream
 * systems (JEI integration, recipe gating, tooltips) consult
 * UnlockedSchematicsData.isUnlocked(recipeId) before exposing a recipe.
 *
 * For v1.2 the full legacy roster below is pre-unlocked for upgrading saves
 * (see SchematicFragmentItem), so existing players don't regress. New worlds
 * get the full discovery experience.
 */
public final class SchematicUnlockTable {

    private static final Map<SchematicTier, List<String>> RECIPES = new EnumMap<>(SchematicTier.class);

    static {
        RECIPES.put(SchematicTier.BASIC, List.of(
                "echoashfallprotocol:hand_recycler",
                "echoashfallprotocol:thermal_burner",
                "echoashfallprotocol:water_purifier",
                "echoashfallprotocol:micro_generator",
                "echoashfallprotocol:machine_casing"
        ));
        RECIPES.put(SchematicTier.INDUSTRIAL, List.of(
                "echoashfallprotocol:filter_workbench",
                "echoashfallprotocol:filter_cartridge_basic",
                "echoashfallprotocol:filter_cartridge_advanced",
                "echoashfallprotocol:scrap_press",
                "echoashfallprotocol:battery_bank",
                "echoashfallprotocol:field_med_bay",
                "echoashfallprotocol:gas_mask"
        ));
        RECIPES.put(SchematicTier.REFINED, List.of(
                "echoashfallprotocol:signal_scanner",
                "echoashfallprotocol:atmospheric_scrubber",
                "echoashfallprotocol:autofeed_hopper",
                "echoashfallprotocol:contaminant_condenser",
                "echoashfallprotocol:ore_grinder",
                "echoashfallprotocol:isotope_refiner",
                "echoashfallprotocol:filter_cartridge_elite",
                "echoashfallprotocol:alloy_blade",
                "echoashfallprotocol:alloy_hammer",
                "echoashfallprotocol:alloy_helmet",
                "echoashfallprotocol:alloy_chestplate",
                "echoashfallprotocol:alloy_leggings",
                "echoashfallprotocol:alloy_boots"
        ));
        RECIPES.put(SchematicTier.CRYSTALLINE, List.of(
                "echoashfallprotocol:crystalline_synthesizer",
                "echoashfallprotocol:power_node",
                "echoashfallprotocol:nexus_blade"
        ));
        RECIPES.put(SchematicTier.NEXUS, List.of(
                "echoashfallprotocol:nexus_core",
                "echoashfallprotocol:nexus_annihilator"
        ));
    }

    private SchematicUnlockTable() {}

    public static List<String> recipesFor(SchematicTier tier) {
        return RECIPES.getOrDefault(tier, List.of());
    }

    /**
     * All recipe IDs this mod currently gates. Used by the "legacy upgrade"
     * path in SchematicFragmentItem to pre-unlock everything for saves that
     * predate v1.2.
     */
    public static java.util.stream.Stream<String> allGatedRecipes() {
        return RECIPES.values().stream().flatMap(List::stream);
    }
}
