package com.knoxhack.echolens.api;

import net.minecraft.network.chat.Component;

public record LensInfoRow(
        Component label,
        Component value,
        String icon,
        LensTone tone,
        LensVisibility visibility) {
    public LensInfoRow {
        label = label == null ? Component.empty() : label;
        value = value == null ? Component.empty() : value;
        icon = icon == null || icon.isBlank() ? "-" : icon.strip();
        tone = tone == null ? LensTone.NEUTRAL : tone;
        visibility = visibility == null ? LensVisibility.COMPACT : visibility;
    }

    public static LensInfoRow of(String label, String value, String icon, LensTone tone, LensVisibility visibility) {
        return new LensInfoRow(Component.literal(label), Component.literal(value), icon, tone, visibility);
    }

    public boolean visibleIn(LensScanMode mode) {
        return visibility.visibleIn(mode);
    }
}
