package com.knoxhack.echomultiblockcore.api;

import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public record BuildAssistMaterialChecklist(Identifier definitionId, List<Entry> entries) {
    public BuildAssistMaterialChecklist {
        entries = List.copyOf(entries == null ? List.of() : entries);
    }

    public static BuildAssistMaterialChecklist empty(Identifier definitionId) {
        return new BuildAssistMaterialChecklist(definitionId, List.of());
    }

    public static BuildAssistMaterialChecklist from(MultiblockMaterialSummary summary, Inventory inventory) {
        if (summary == null) {
            return empty(Identifier.fromNamespaceAndPath("echomultiblockcore", "empty"));
        }
        return new BuildAssistMaterialChecklist(summary.definitionId(), summary.entries().stream()
                .map(entry -> Entry.from(entry, inventory))
                .toList());
    }

    public List<Entry> placeableEntries() {
        return entries.stream().filter(Entry::placeable).toList();
    }

    public String compactLine(int maxEntries) {
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
        return extra <= 0 ? line : line + ", +" + extra + " more";
    }

    public int missingExactCount() {
        return entries.stream()
                .filter(Entry::counted)
                .mapToInt(Entry::missing)
                .sum();
    }

    public record Entry(
            StructureBlockRequirement.SlotKind kind,
            String expected,
            int required,
            int available,
            boolean optional,
            boolean placeable) {
        public Entry {
            expected = expected == null || expected.isBlank() ? "unknown" : expected.strip();
            required = Math.max(0, required);
            available = Math.max(-1, available);
        }

        public static Entry from(MultiblockMaterialSummary.Entry entry, Inventory inventory) {
            return new Entry(entry.kind(), entry.expected(), entry.count(), available(entry, inventory),
                    entry.optional(), entry.placeable());
        }

        public boolean counted() {
            return available >= 0;
        }

        public int missing() {
            return counted() ? Math.max(0, required - available) : 0;
        }

        public String line() {
            String base = required + "x " + (optional ? "Optional " : "") + expected;
            return counted() ? base + " (" + available + "/" + required + ")" : base;
        }

        private static int available(MultiblockMaterialSummary.Entry entry, Inventory inventory) {
            if (inventory == null || !entry.placeable() || !isExactBlockLike(entry.kind())) {
                return -1;
            }
            Identifier id = Identifier.tryParse(entry.expected());
            if (id == null) {
                return -1;
            }
            Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
            if (item == null) {
                return -1;
            }
            int count = 0;
            for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                ItemStack stack = inventory.getItem(slot);
                if (!stack.isEmpty() && stack.is(item)) {
                    count += stack.getCount();
                }
            }
            return count;
        }

        private static boolean isExactBlockLike(StructureBlockRequirement.SlotKind kind) {
            return kind == StructureBlockRequirement.SlotKind.EXACT_BLOCK
                    || kind == StructureBlockRequirement.SlotKind.CONTROLLER
                    || kind == StructureBlockRequirement.SlotKind.COMPONENT
                    || kind == StructureBlockRequirement.SlotKind.ROBOTICS
                    || kind == StructureBlockRequirement.SlotKind.UPGRADE;
        }
    }
}
