package com.echoplatform.echocore.api;

import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public interface EchoFactionActionHandlerService {
    default boolean supports(Identifier factionId) {
        return false;
    }

    default List<EchoFactionAction> actions(Player player, EchoFactionProfile profile, String roleId) {
        return List.of();
    }

    default String localContext(Player player, EchoFactionProfile profile, String roleId) {
        return "";
    }

    default EchoFactionContractState contractState(Player player, EchoFactionProfile profile,
            EchoFactionContract contract, String roleId) {
        return new EchoFactionContractState(contract == null ? null : contract.id(), false, false, false, false, "", "");
    }

    default EchoFactionActionResult acceptContract(ServerPlayer player, EchoFactionProfile profile,
            EchoFactionContract contract, String roleId) {
        return EchoFactionActionResult.failure("Unavailable", "Contract acceptance is unavailable.");
    }

    default EchoFactionActionResult completeContract(ServerPlayer player, EchoFactionProfile profile,
            EchoFactionContract contract, String roleId) {
        return EchoFactionActionResult.failure("Unavailable", "Contract completion is unavailable.");
    }

    default EchoFactionActionResult handle(ServerPlayer player, Identifier factionId, Identifier actionId,
            String roleId, Identifier targetId) {
        return EchoFactionActionResult.failure("Unavailable", "Faction action is unavailable.");
    }
}
