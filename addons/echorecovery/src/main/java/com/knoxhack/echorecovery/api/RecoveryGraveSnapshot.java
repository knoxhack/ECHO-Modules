package com.knoxhack.echorecovery.api;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;

public record RecoveryGraveSnapshot(
        String graveId,
        UUID ownerId,
        String ownerName,
        BlockPos pos,
        String dimension,
        String graveTypeId,
        int storedItemCount,
        int xpStored,
        long createdAt,
        long expiresAt,
        boolean recovered,
        boolean expired,
        boolean contaminated,
        boolean temporaryPlatform,
        List<String> hazardNotes) {
    public RecoveryGraveSnapshot {
        graveId = graveId == null ? "" : graveId;
        ownerName = ownerName == null ? "" : ownerName;
        dimension = dimension == null ? "" : dimension;
        graveTypeId = graveTypeId == null || graveTypeId.isBlank() ? "echorecovery:vanilla_grave" : graveTypeId;
        storedItemCount = Math.max(0, storedItemCount);
        xpStored = Math.max(0, xpStored);
        hazardNotes = List.copyOf(hazardNotes == null ? List.of() : hazardNotes);
    }
}
