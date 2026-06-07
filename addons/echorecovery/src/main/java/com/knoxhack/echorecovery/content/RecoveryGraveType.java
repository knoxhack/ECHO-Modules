package com.knoxhack.echorecovery.content;

import java.util.List;
import net.minecraft.resources.Identifier;

public record RecoveryGraveType(
        Identifier id,
        String displayName,
        Identifier blockId,
        Identifier texture,
        boolean contaminated,
        List<String> hazardNotes) {
    public RecoveryGraveType {
        displayName = displayName == null || displayName.isBlank() ? id.toString() : displayName.strip();
        blockId = blockId == null ? Identifier.fromNamespaceAndPath("echorecovery", "grave") : blockId;
        texture = texture == null ? Identifier.fromNamespaceAndPath("minecraft", "block/stone") : texture;
        hazardNotes = List.copyOf(hazardNotes == null ? List.of() : hazardNotes);
    }
}
