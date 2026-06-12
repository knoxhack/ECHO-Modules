package com.echoplatform.echocore.api.index;

import java.util.ArrayList;
import java.util.List;

public final class IndexMachineLayoutTemplates {
    private IndexMachineLayoutTemplates() {
    }

    public static List<IndexMachineLayout> representativeAll(List<IndexRecipeView> recipes, String templateId) {
        if (recipes == null) {
            return List.of();
        }
        return recipes.stream()
                .map(recipe -> process(recipe, templateId, false))
                .filter(layout -> layout != null && !layout.empty())
                .toList();
    }

    public static IndexMachineLayout process(IndexRecipeView recipe, String templateId, boolean compact) {
        if (recipe == null || recipe.id() == null) {
            return null;
        }
        List<IndexMachineLayoutSlot> slots = new ArrayList<>();
        int x = 8;
        for (IndexRecipeSlot slot : recipe.slots()) {
            slots.add(new IndexMachineLayoutSlot(slot.role(), x, slot.role() == IndexSlotRole.OUTPUT ? 34 : 8,
                    compact ? 18 : 22, compact ? 18 : 22, slot.label()));
            x += compact ? 24 : 30;
        }
        return new IndexMachineLayout(recipe.id(), templateId, Math.max(96, x + 8), compact ? 48 : 68,
                slots, List.of());
    }

    public static IndexMachineLayout sourceStation(IndexRecipeView recipe, String templateId) {
        return process(recipe, templateId, true);
    }
}
