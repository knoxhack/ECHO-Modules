package com.knoxhack.echolens.api;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public record LensAction(
        Identifier id,
        Component label,
        Component hint,
        String icon,
        LensTone tone,
        boolean available) {
    public LensAction {
        if (id == null) {
            throw new IllegalArgumentException("Lens action id is required.");
        }
        label = label == null ? Component.literal(id.getPath()) : label;
        hint = hint == null ? Component.empty() : hint;
        icon = icon == null || icon.isBlank() ? ">" : icon.strip();
        tone = tone == null ? LensTone.NEUTRAL : tone;
    }
}
