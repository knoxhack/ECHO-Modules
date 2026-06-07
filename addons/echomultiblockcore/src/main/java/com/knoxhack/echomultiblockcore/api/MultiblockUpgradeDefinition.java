package com.knoxhack.echomultiblockcore.api;

import java.util.List;
import net.minecraft.resources.Identifier;

public record MultiblockUpgradeDefinition(
        Identifier id,
        String displayName,
        String category,
        Identifier itemId,
        List<Identifier> allowedMultiblocks,
        List<UpgradeModifier> modifiers,
        List<String> notes) {
    public MultiblockUpgradeDefinition {
        if (id == null) {
            throw new IllegalArgumentException("Upgrade id is required.");
        }
        displayName = displayName == null || displayName.isBlank() ? id.getPath().replace('_', ' ') : displayName.strip();
        category = category == null || category.isBlank() ? "general" : category.strip();
        itemId = itemId == null ? id : itemId;
        allowedMultiblocks = List.copyOf(allowedMultiblocks == null ? List.of() : allowedMultiblocks);
        modifiers = List.copyOf(modifiers == null ? List.of() : modifiers);
        notes = List.copyOf(notes == null ? List.of() : notes.stream()
                .filter(note -> note != null && !note.isBlank())
                .map(String::strip)
                .toList());
    }

    public boolean allows(Identifier multiblockId) {
        return allowedMultiblocks.isEmpty() || allowedMultiblocks.contains(multiblockId);
    }
}
