package com.knoxhack.echoopenlandsprotocol.runtime;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class OpenlandsShelterScoring {
    public static final int MINIMUM_FOR_SLEEP_MILESTONE = 55;
    public static final int IDEAL_SCORE = 100;
    public static final int HOSTILE_CLEAR_RADIUS_BLOCKS = 16;

    private OpenlandsShelterScoring() {
    }

    public static OpenlandsShelterScore score(OpenlandsShelterSnapshot snapshot) {
        Map<String, Integer> scores = new LinkedHashMap<>();
        scores.put("roof_coverage", proportional(snapshot.roofCoverage(), 25));
        scores.put("wall_enclosure", proportional(snapshot.wallEnclosure(), 25));
        scores.put("entry_closure", snapshot.entryClosed() ? 15 : 0);
        scores.put("bedroll_present", snapshot.bedrollPresent() ? 15 : 0);
        scores.put("light_or_fire", snapshot.lightOrFirePresent() ? 10 : 0);
        scores.put("hostile_distance", hostileDistanceScore(snapshot.nearestHostileDistanceBlocks()));

        int total = scores.values().stream().mapToInt(Integer::intValue).sum();
        return new OpenlandsShelterScore(total, total >= MINIMUM_FOR_SLEEP_MILESTONE, scores, missingImprovements(scores));
    }

    private static int proportional(double ratio, int maxPoints) {
        return (int) Math.round(ratio * maxPoints);
    }

    private static int hostileDistanceScore(int nearestHostileDistanceBlocks) {
        if (nearestHostileDistanceBlocks >= HOSTILE_CLEAR_RADIUS_BLOCKS) return 10;
        if (nearestHostileDistanceBlocks >= 8) return 5;
        return 0;
    }

    private static List<String> missingImprovements(Map<String, Integer> scores) {
        List<String> missing = new ArrayList<>();
        if (scores.getOrDefault("roof_coverage", 0) < 13) missing.add("add_more_overhead_cover");
        if (scores.getOrDefault("wall_enclosure", 0) < 13) missing.add("close_more_walls_or_use_a_cave_corner");
        if (scores.getOrDefault("entry_closure", 0) == 0) missing.add("add_a_door_trapdoor_or_natural_choke");
        if (scores.getOrDefault("bedroll_present", 0) == 0) missing.add("place_a_bedroll");
        if (scores.getOrDefault("light_or_fire", 0) == 0) missing.add("place_a_torch_lantern_or_campfire");
        if (scores.getOrDefault("hostile_distance", 0) == 0) missing.add("move_or_wait_until_hostiles_are_farther_away");
        return missing;
    }
}
