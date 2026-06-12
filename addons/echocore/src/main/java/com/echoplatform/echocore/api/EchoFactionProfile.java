package com.echoplatform.echocore.api;

import java.util.Set;
import java.util.Optional;
import net.minecraft.resources.Identifier;

public record EchoFactionProfile(
        EchoFactionDefinition definition,
        int reputation,
        boolean contacted,
        int contactCount,
        long lastInteractionTick,
        String lastRoleId,
        EchoFactionStanding standing,
        String npcMemory,
        Optional<Identifier> activeContractId,
        Set<Identifier> completedContractIds) {
    public EchoFactionProfile {
        standing = standing == null ? standingFor(reputation) : standing;
        npcMemory = npcMemory == null ? "" : npcMemory;
        lastRoleId = lastRoleId == null ? "" : lastRoleId;
        activeContractId = activeContractId == null ? Optional.empty() : activeContractId;
        completedContractIds = completedContractIds == null ? Set.of() : Set.copyOf(completedContractIds);
    }

    public EchoFactionProfile(EchoFactionDefinition definition, int reputation, boolean contacted, int contactCount,
            long lastInteractionTick, String lastRoleId, String standing, String npcMemory,
            Optional<Identifier> activeContractId, Set<Identifier> completedContractIds) {
        this(definition, reputation, contacted, contactCount, lastInteractionTick, lastRoleId,
                EchoFactionStanding.fromName(standing), npcMemory, activeContractId, completedContractIds);
    }

    public Identifier factionId() {
        return definition == null ? null : definition.id();
    }

    public int completedContracts() {
        return completedContractIds.size();
    }

    public String standingLine() {
        return standing.displayName() + " (" + reputation + ")";
    }

    public EchoFactionProfile withReputation(int value) {
        return new EchoFactionProfile(definition, value, contacted, contactCount, lastInteractionTick, lastRoleId,
                standingFor(value), npcMemory, activeContractId, completedContractIds);
    }

    public EchoFactionProfile contacted(long tick, String roleId, String memory) {
        return new EchoFactionProfile(definition, reputation, true, contactCount + 1, tick, roleId,
                standingFor(reputation), memory, activeContractId, completedContractIds);
    }

    private static EchoFactionStanding standingFor(int reputation) {
        return EchoFactionStanding.fromReputation(reputation);
    }
}
