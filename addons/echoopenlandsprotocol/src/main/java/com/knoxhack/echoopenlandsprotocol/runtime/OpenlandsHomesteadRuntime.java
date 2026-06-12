package com.knoxhack.echoopenlandsprotocol.runtime;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class OpenlandsHomesteadRuntime {
    public static final int COOKPOT_REQUIRED_INGREDIENTS = 3;
    public static final int COOKPOT_COOK_TICKS = 200;

    private static final Map<String, Integer> GROWTH_STAGES = Map.of(
            "grain", 5,
            "root_crop", 4,
            "berries", 3
    );

    private static final Map<String, Integer> BASE_GROWTH_MINUTES = Map.of(
            "grain", 24,
            "root_crop", 20,
            "berries", 18
    );

    private static final Map<String, String> STANDARD_FAILURE = Map.of(
            "grain", "pauses_when_unwatered",
            "root_crop", "pauses_when_unwatered",
            "berries", "none"
    );

    private OpenlandsHomesteadRuntime() {
    }

    public static List<String> cropIds() {
        return List.of("grain", "root_crop", "berries");
    }

    public static OpenlandsCropGrowthResult advanceCrop(OpenlandsCropGrowthSnapshot snapshot) {
        String cropId = snapshot.cropId();
        Integer stages = GROWTH_STAGES.get(cropId);
        if (stages == null) {
            return result(snapshot, snapshot.currentStage(), false, false, true, "unknown_crop", 0.0);
        }

        int maximumStage = stages - 1;
        int beforeStage = Math.min(snapshot.currentStage(), maximumStage);
        if (beforeStage >= maximumStage) {
            return new OpenlandsCropGrowthResult(cropId, beforeStage, beforeStage, true, false, false, "already_harvest_ready", 1.0);
        }

        if (snapshot.hardlands() && !snapshot.watered()) {
            return result(snapshot, beforeStage, false, false, true, "hardlands_neglect_failure", 0.0);
        }

        if (!snapshot.hardlands() && !snapshot.watered() && "pauses_when_unwatered".equals(STANDARD_FAILURE.get(cropId))) {
            return result(snapshot, beforeStage, false, true, false, "standard_growth_paused_without_water", 0.0);
        }

        double multiplier = 1.0;
        if (snapshot.watered()) multiplier += 0.25;
        if (snapshot.composted()) multiplier += 0.35;

        int requiredMinutes = Math.max(1, (int) Math.ceil(BASE_GROWTH_MINUTES.get(cropId) / multiplier));
        if (snapshot.elapsedMinutes() < requiredMinutes) {
            return new OpenlandsCropGrowthResult(cropId, beforeStage, beforeStage, false, true, false, "waiting_for_growth_time", multiplier);
        }

        int afterStage = Math.min(maximumStage, beforeStage + 1);
        return new OpenlandsCropGrowthResult(cropId, beforeStage, afterStage, afterStage >= maximumStage, false, false, "advanced", multiplier);
    }

    public static boolean cookpotMealReady(int ingredientCount, int elapsedCookTicks, boolean outputAlreadyReady) {
        return outputAlreadyReady || ingredientCount >= COOKPOT_REQUIRED_INGREDIENTS && elapsedCookTicks >= COOKPOT_COOK_TICKS;
    }

    public static Map<String, Object> adapterRecord() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("cropIds", cropIds());
        result.put("growthStages", GROWTH_STAGES);
        result.put("baseGrowthMinutes", BASE_GROWTH_MINUTES);
        result.put("standardFailure", STANDARD_FAILURE);
        result.put("cookpotRequiredIngredients", COOKPOT_REQUIRED_INGREDIENTS);
        result.put("cookpotCookTicks", COOKPOT_COOK_TICKS);
        result.put("standardWateringRequired", false);
        result.put("standardCompostRequired", false);
        result.put("standardSpoilage", false);
        return Map.copyOf(result);
    }

    private static OpenlandsCropGrowthResult result(
            OpenlandsCropGrowthSnapshot snapshot,
            int stage,
            boolean harvestReady,
            boolean paused,
            boolean failed,
            String reason,
            double multiplier
    ) {
        return new OpenlandsCropGrowthResult(snapshot.cropId(), stage, stage, harvestReady, paused, failed, reason, multiplier);
    }
}
