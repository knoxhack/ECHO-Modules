package com.knoxhack.echolens.api;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public record LensInfoSection(
        Identifier id,
        LensDataCategory category,
        Component title,
        String icon,
        LensTone tone,
        LensVisibility visibility,
        List<LensInfoRow> rows) {
    public LensInfoSection {
        if (id == null) {
            throw new IllegalArgumentException("Lens section id is required.");
        }
        category = category == null ? LensDataCategory.IDENTITY : category;
        title = title == null ? Component.literal(id.getPath()) : title;
        icon = icon == null || icon.isBlank() ? "*" : icon.strip();
        tone = tone == null ? LensTone.NEUTRAL : tone;
        visibility = visibility == null ? LensVisibility.COMPACT : visibility;
        rows = List.copyOf(rows == null ? List.of() : rows.stream().filter(row -> row != null).toList());
    }

    public static LensInfoSection of(Identifier id, LensDataCategory category, String title, String icon,
            LensTone tone, LensVisibility visibility, List<LensInfoRow> rows) {
        return new LensInfoSection(id, category, Component.literal(title), icon, tone, visibility, rows);
    }

    public boolean visibleIn(LensScanMode mode) {
        return visibility.visibleIn(mode);
    }
}
