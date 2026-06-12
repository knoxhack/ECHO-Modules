package com.knoxhack.echoopenlandsprotocol.runtime;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class OpenlandsFirstHourRuntime {
    public static final String MODULE_ID = "echoopenlandsprotocol";
    public static final String PACK_ID = "openlands";
    public static final String VERSION = "0.1.0";

    private OpenlandsFirstHourRuntime() {
    }

    public static OpenlandsStandardRules standardRules() {
        return OpenlandsStandardRules.relaxedDefault();
    }

    public static OpenlandsStarterSpawnResult validateStarterSpawn(OpenlandsStarterSpawnSnapshot snapshot) {
        return OpenlandsStarterSpawnGuarantees.validate(snapshot);
    }

    public static OpenlandsShelterScore scoreShelter(OpenlandsShelterSnapshot snapshot) {
        return OpenlandsShelterScoring.score(snapshot);
    }

    public static OpenlandsWaystoneTransition advanceWaystone(OpenlandsWaystoneState current, Map<String, Integer> availableInputs) {
        return OpenlandsWaystoneRuntime.advance(current, availableInputs);
    }

    public static OpenlandsCropGrowthResult advanceCrop(OpenlandsCropGrowthSnapshot snapshot) {
        return OpenlandsHomesteadRuntime.advanceCrop(snapshot);
    }

    public static boolean cookpotMealReady(int ingredientCount, int elapsedCookTicks, boolean outputAlreadyReady) {
        return OpenlandsHomesteadRuntime.cookpotMealReady(ingredientCount, elapsedCookTicks, outputAlreadyReady);
    }

    public static OpenlandsBuilderActionResult validateBuilderAction(OpenlandsBuilderActionSnapshot snapshot) {
        return OpenlandsBuilderUxRuntime.validateBuilderAction(snapshot);
    }

    public static List<String> firstHourStepIds() {
        return OpenlandsFirstHourStep.stepIds();
    }

    public static Map<String, Object> adapterBindingManifest() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("moduleId", MODULE_ID);
        result.put("packId", PACK_ID);
        result.put("version", VERSION);
        result.put("runtimeCorePackage", "com.knoxhack.echoopenlandsprotocol.runtime");
        result.put("standardRules", standardRules().asAdapterRecord());
        result.put("firstHourStepIds", firstHourStepIds());
        result.put("shelterMinimumForSleepMilestone", OpenlandsShelterScoring.MINIMUM_FOR_SLEEP_MILESTONE);
        result.put("starterSpawnAllowedBiomes", OpenlandsStarterSpawnGuarantees.ALLOWED_STARTER_BIOMES.stream().sorted().toList());
        result.put("starterSpawnResourceRadiusBlocks", OpenlandsStarterSpawnGuarantees.GUARANTEED_RESOURCE_RADIUS_BLOCKS);
        result.put("visibleLandmarkRadiusBlocks", OpenlandsStarterSpawnGuarantees.VISIBLE_LANDMARK_RADIUS_BLOCKS);
        result.put("minimumHostileClearRadiusBlocks", OpenlandsStarterSpawnGuarantees.MINIMUM_HOSTILE_CLEAR_RADIUS_BLOCKS);
        result.put("waystoneStateOrder", OpenlandsWaystoneState.stateIds());
        result.put("waystoneRequiredInputsByState", OpenlandsWaystoneRuntime.requiredInputsByState());
        result.put("activeStonesRequiredForFastTravel", OpenlandsWaystoneRuntime.ACTIVE_STONES_REQUIRED_FOR_FAST_TRAVEL);
        result.put("homesteadRuntime", OpenlandsHomesteadRuntime.adapterRecord());
        result.put("builderUxRuntime", OpenlandsBuilderUxRuntime.adapterRecord());
        result.put("callableHooks", List.of(
                "standardRules",
                "validateStarterSpawn",
                "scoreShelter",
                "advanceWaystone",
                "advanceCrop",
                "cookpotMealReady",
                "validateBuilderAction",
                "firstHourStepIds",
                "adapterBindingManifest"
        ));
        return Map.copyOf(result);
    }
}
