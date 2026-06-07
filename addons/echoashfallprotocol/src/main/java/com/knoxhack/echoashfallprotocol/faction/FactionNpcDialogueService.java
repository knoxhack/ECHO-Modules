package com.knoxhack.echoashfallprotocol.faction;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.EchoFactionAction;
import com.knoxhack.echocore.api.EchoFactionActionResult;
import com.knoxhack.echocore.api.EchoFactionContract;
import com.knoxhack.echocore.api.EchoFactionContractState;
import com.knoxhack.echocore.api.EchoFactionInteractionSnapshot;
import com.knoxhack.echocore.api.EchoFactionProfile;
import com.knoxhack.echocore.api.network.EchoPacketKind;
import com.knoxhack.echoashfallprotocol.entity.faction.FactionNpcEntity;
import com.knoxhack.echoashfallprotocol.event.AshfallAdapterCoreExplorationRuntime;
import com.knoxhack.echoashfallprotocol.network.FactionDialogueOpenPacket;
import com.knoxhack.echoashfallprotocol.network.FactionNpcActionPacket;
import com.knoxhack.echonetcore.api.EchoNetSend;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * Server-side coordinator for faction NPC conversation state.
 */
public final class FactionNpcDialogueService {
    private static final double MAX_INTERACTION_DISTANCE_SQR = 64.0D;

    private FactionNpcDialogueService() {
    }

    public static void open(ServerPlayer player, FactionNpcEntity npc) {
        if (player == null || npc == null || npc.isRemoved() || npc.distanceToSqr(player) > MAX_INTERACTION_DISTANCE_SQR) {
            return;
        }

        Identifier factionId = npc.factionId();
        String roleId = npc.roleId();
        EchoCoreServices.recordFactionInteraction(player, factionId, roleId, player.level().getGameTime());
        EchoCoreServices.factionInteractionSnapshot(player, factionId, roleId)
                .map(snapshot -> packet(player, npc, snapshot))
                .ifPresent(packet -> EchoNetSend.toPlayer(player, packet, EchoPacketKind.CLIENTBOUND_SYNC));
    }

    public static void handleAction(ServerPlayer player, FactionNpcActionPacket packet) {
        if (player == null || packet == null) {
            return;
        }
        Entity entity = player.level().getEntity(packet.entityId());
        if (!(entity instanceof FactionNpcEntity npc) || npc.isRemoved()
                || npc.distanceToSqr(player) > MAX_INTERACTION_DISTANCE_SQR) {
            player.sendSystemMessage(Component.literal("[ECHO-7] Faction contact signal out of range."), true);
            return;
        }

        Identifier actionId = parse(packet.actionId());
        Identifier targetId = parse(packet.targetId());
        EchoFactionActionResult result = EchoCoreServices.performFactionAction(
                player, npc.factionId(), actionId, npc.roleId(), targetId);
        AshfallAdapterCoreExplorationRuntime.factionAction(
                player, npc.factionId(), actionId, npc.roleId(), targetId, result);
        if (result.success() && EchoCoreServices.COMPLETE_FACTION_CONTRACT_ACTION.equals(actionId)) {
            int reputationReward = contractReputationReward(player, npc.factionId(), targetId);
            if (reputationReward > 0) {
                AshfallAdapterCoreExplorationRuntime.reputationUpdated(
                        player, npc.factionId(), reputationReward, "faction_contract");
            }
        }
        player.sendSystemMessage(Component.literal("[ECHO-7] " + result.title() + ": " + result.message()), false);
        if (result.refresh()) {
            open(player, npc);
        }
    }

    private static int contractReputationReward(ServerPlayer player, Identifier factionId, Identifier contractId) {
        if (player == null || factionId == null || contractId == null) {
            return 0;
        }
        return EchoCoreServices.factionProfile(player, factionId)
                .flatMap(profile -> profile.definition().contracts().stream()
                        .filter(contract -> contract.id().equals(contractId))
                        .findFirst())
                .map(EchoFactionContract::reputationReward)
                .orElse(0);
    }

    private static FactionDialogueOpenPacket packet(ServerPlayer player, FactionNpcEntity npc,
            EchoFactionInteractionSnapshot snapshot) {
        EchoFactionProfile profile = snapshot.profile();
        String activeId = profile.activeContractId().map(Identifier::toString).orElse("");
        List<FactionDialogueOpenPacket.ActionEntry> actions = snapshot.actions().stream()
                .map(action -> actionEntry(action, profile.reputation()))
                .toList();
        List<FactionDialogueOpenPacket.ContractEntry> contracts = snapshot.contracts().stream()
                .map(contract -> contractEntry(player, contract, profile, activeId, snapshot.roleId()))
                .toList();
        return new FactionDialogueOpenPacket(
                npc.getId(),
                profile.definition().id().toString(),
                profile.definition().displayName(),
                profile.definition().shortName(),
                snapshot.roleId(),
                snapshot.roleName(),
                profile.standing().displayName(),
                profile.reputation(),
                profile.contactCount(),
                profile.lastInteractionTick(),
                profile.lastRoleId(),
                profile.npcMemory(),
                snapshot.greeting(),
                snapshot.localContext(),
                activeId,
                actions,
                contracts);
    }

    private static FactionDialogueOpenPacket.ActionEntry actionEntry(EchoFactionAction action, int reputation) {
        boolean enabled = reputation >= action.requiredReputation();
        return new FactionDialogueOpenPacket.ActionEntry(
                action.id().toString(),
                action.label(),
                action.description(),
                action.requiredReputation(),
                action.service(),
                enabled,
                enabled ? "" : "Requires faction standing " + action.requiredReputation());
    }

    private static FactionDialogueOpenPacket.ContractEntry contractEntry(ServerPlayer player, EchoFactionContract contract,
            EchoFactionProfile profile, String activeId, String roleId) {
        boolean active = contract.id().toString().equals(activeId);
        boolean completed = profile.completedContractIds().contains(contract.id());
        EchoFactionContractState state = EchoCoreServices.factionContractState(
                player, profile.definition().id(), contract.id(), roleId);
        boolean canAccept = state.canAccept();
        boolean canComplete = state.canComplete();
        return new FactionDialogueOpenPacket.ContractEntry(
                contract.id().toString(),
                contract.title(),
                contract.summary(),
                contract.objective(),
                contract.reward(),
                contract.route(),
                state.progressLine(),
                completed ? "This field contract is already archived." : state.lockedReason(),
                active,
                completed,
                canAccept,
                canComplete);
    }

    private static Identifier parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Identifier.parse(value);
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
