package com.knoxhack.echorendercore.profile;

import net.minecraft.core.Direction;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record BlockPartSelectorProfile(
        String id,
        List<Integer> indices,
        Set<Direction> directions,
        int materialFlags,
        Boolean ambientOcclusion,
        List<Integer> tintIndices,
        Map<String, Set<String>> blockState
) {
    public BlockPartSelectorProfile(String id, List<Integer> indices, Set<Direction> directions, int materialFlags,
          Boolean ambientOcclusion, List<Integer> tintIndices) {
        this(id, indices, directions, materialFlags, ambientOcclusion, tintIndices, Map.of());
    }

    public BlockPartSelectorProfile {
        indices = indices == null ? List.of() : List.copyOf(indices);
        directions = directions == null ? Set.of() : Set.copyOf(directions);
        tintIndices = tintIndices == null ? List.of() : List.copyOf(tintIndices);
        if (blockState == null || blockState.isEmpty()) {
            blockState = Map.of();
        } else {
            LinkedHashMap<String, Set<String>> normalized = new LinkedHashMap<>();
            for (Map.Entry<String, Set<String>> entry : blockState.entrySet()) {
                if (entry.getKey() == null || entry.getKey().isBlank()) {
                    continue;
                }
                normalized.put(entry.getKey(), entry.getValue() == null ? Set.of() : Set.copyOf(entry.getValue()));
            }
            blockState = Map.copyOf(normalized);
        }
    }

    public boolean isEmptySelector() {
        return indices.isEmpty() && directions.isEmpty() && materialFlags == 0 && ambientOcclusion == null && tintIndices.isEmpty()
           && blockState.isEmpty();
    }
}
