package com.echoplatform.echocore.api;

import net.minecraft.resources.Identifier;

public record EchoFactionContractState(
        Identifier id,
        boolean canAccept,
        boolean canComplete,
        boolean active,
        boolean completed,
        String progressLine,
        String lockedReason) {
    public EchoFactionContractState(Identifier id, boolean canAccept, boolean completed, String progressLine, String lockedReason) {
        this(id, canAccept, false, canAccept, completed, progressLine, lockedReason);
    }

    public EchoFactionContractState {
        progressLine = progressLine == null ? "" : progressLine;
        lockedReason = lockedReason == null ? "" : lockedReason;
    }

    public static EchoFactionContractState fromProfile(EchoFactionProfile profile, EchoFactionContract contract) {
        if (contract == null) {
            return new EchoFactionContractState(null, false, false, false, false, "", "Contract unavailable.");
        }
        if (profile == null) {
            return new EchoFactionContractState(contract.id(), false, false, false, false, contract.objective(),
                    "Faction profile unavailable.");
        }
        boolean completed = profile.completedContractIds().contains(contract.id());
        boolean active = profile.activeContractId().filter(contract.id()::equals).isPresent();
        boolean canAccept = !completed && !active && profile.reputation() >= contract.requiredReputation();
        boolean canComplete = active && !completed;
        String locked = canAccept || canComplete || completed ? "" : "Requires faction standing " + contract.requiredReputation();
        return new EchoFactionContractState(contract.id(), canAccept, canComplete, active, completed,
                contract.objective(), locked);
    }
}
