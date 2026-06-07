package com.knoxhack.echomultiblockcore.api;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;

public record MultiblockMaterialSummary(Identifier definitionId, List<Entry> entries) {
    public MultiblockMaterialSummary {
        entries = List.copyOf(entries == null ? List.of() : entries);
    }

    public static MultiblockMaterialSummary empty(Identifier definitionId) {
        return new MultiblockMaterialSummary(definitionId, List.of());
    }

    public static MultiblockMaterialSummary from(MultiblockDefinition definition) {
        Map<Key, Integer> counts = new LinkedHashMap<>();
        for (int y = 0; y < definition.height(); y++) {
            for (int z = 0; z < definition.depth(); z++) {
                for (int x = 0; x < definition.width(); x++) {
                    StructureBlockRequirement requirement = definition.requirementAt(x, y, z);
                    Key key = Key.from(requirement);
                    counts.put(key, counts.getOrDefault(key, 0) + 1);
                }
            }
        }
        List<Entry> entries = counts.entrySet().stream()
                .map(entry -> new Entry(entry.getKey().kind(), entry.getKey().expected(), entry.getValue(),
                        entry.getKey().optional(), entry.getKey().placeable()))
                .toList();
        return new MultiblockMaterialSummary(definition.id(), entries);
    }

    public List<Entry> placeableEntries() {
        return entries.stream().filter(Entry::placeable).toList();
    }

    public String compactLine(int maxEntries) {
        return compactLine(maxEntries, true);
    }

    public String compactLine(int maxEntries, boolean includeExtra) {
        List<Entry> placeable = placeableEntries();
        if (placeable.isEmpty()) {
            return "No required placeable materials.";
        }
        int limit = Math.max(1, maxEntries);
        String line = placeable.stream()
                .limit(limit)
                .map(Entry::line)
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
        int extra = placeable.size() - Math.min(placeable.size(), limit);
        return !includeExtra || extra <= 0 ? line : line + ", +" + extra + " more";
    }

    public int extraPlaceableEntries(int maxEntries) {
        return Math.max(0, placeableEntries().size() - Math.max(0, maxEntries));
    }

    public record Entry(
            StructureBlockRequirement.SlotKind kind,
            String expected,
            int count,
            boolean optional,
            boolean placeable) {
        public Entry {
            expected = expected == null || expected.isBlank() ? "unknown" : expected.strip();
            count = Math.max(0, count);
        }

        public String line() {
            return count + "x " + (optional ? "Optional " : "") + expected;
        }
    }

    private record Key(StructureBlockRequirement.SlotKind kind, String expected, boolean optional, boolean placeable) {
        static Key from(StructureBlockRequirement requirement) {
            requirement = requirement == null ? StructureBlockRequirement.wildcard() : requirement;
            boolean placeable = requirement.kind() != StructureBlockRequirement.SlotKind.AIR
                    && requirement.kind() != StructureBlockRequirement.SlotKind.WILDCARD;
            return new Key(requirement.kind(), requirement.expectedName(), requirement.optional(), placeable);
        }
    }
}
