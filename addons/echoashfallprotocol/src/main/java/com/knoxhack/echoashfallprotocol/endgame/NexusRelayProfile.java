package com.knoxhack.echoashfallprotocol.endgame;

import com.knoxhack.echoashfallprotocol.entity.NexusPressureMobEntity;
import com.knoxhack.echoashfallprotocol.worldgen.StructureType;
import java.util.function.Supplier;
import net.minecraft.world.entity.EntityType;

public record NexusRelayProfile(
        NexusRelayType type,
        StructureType structureType,
        String routeTitle,
        String hazardName,
        String prepHint,
        String objective,
        String rewardTrack,
        String resourceProfile,
        int requiredPressureKills,
        Supplier<EntityType<NexusPressureMobEntity>> primaryPressureType,
        int primarySpawnCount,
        Supplier<EntityType<NexusPressureMobEntity>> secondaryPressureType,
        int secondarySpawnCount,
        Supplier<EntityType<NexusPressureMobEntity>> commanderType
) {
    public NexusRelayProfile {
        routeTitle = routeTitle == null || routeTitle.isBlank() ? type.displayName() : routeTitle;
        hazardName = hazardName == null || hazardName.isBlank() ? type.routeIdentity() : hazardName;
        prepHint = prepHint == null ? "" : prepHint;
        objective = objective == null ? "" : objective;
        rewardTrack = rewardTrack == null ? "" : rewardTrack;
        resourceProfile = resourceProfile == null ? "" : resourceProfile;
        requiredPressureKills = Math.max(0, requiredPressureKills);
        primarySpawnCount = Math.max(0, primarySpawnCount);
        secondarySpawnCount = Math.max(0, secondarySpawnCount);
    }

    public boolean countsPressureKill(EntityType<?> entityType) {
        return entityType != null
                && ((primaryPressureType != null && entityType == primaryPressureType.get())
                || (secondaryPressureType != null && entityType == secondaryPressureType.get()));
    }

    public boolean isCommander(EntityType<?> entityType) {
        return entityType != null && commanderType != null && entityType == commanderType.get();
    }

    public boolean needsCommander() {
        return commanderType != null;
    }
}
