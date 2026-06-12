package com.echoplatform.echocore.api.index;

import java.util.List;
import net.minecraft.resources.Identifier;

public record IndexMachineLayout(
        Identifier recipeId,
        String templateId,
        String title,
        int width,
        int height,
        boolean exact,
        List<IndexMachineLayoutSlot> slots,
        List<IndexMachineLayoutGauge> gauges) {
    public IndexMachineLayout {
        templateId = templateId == null ? "" : templateId;
        title = title == null ? "" : title;
        width = Math.max(1, width);
        height = Math.max(1, height);
        slots = slots == null ? List.of() : List.copyOf(slots);
        gauges = gauges == null ? List.of() : List.copyOf(gauges);
    }

    public IndexMachineLayout(
            Identifier recipeId,
            String templateId,
            int width,
            int height,
            List<IndexMachineLayoutSlot> slots,
            List<IndexMachineLayoutGauge> gauges) {
        this(recipeId, templateId, "", width, height, false, slots, gauges);
    }

    public boolean empty() {
        return slots.isEmpty() && gauges.isEmpty();
    }
}
