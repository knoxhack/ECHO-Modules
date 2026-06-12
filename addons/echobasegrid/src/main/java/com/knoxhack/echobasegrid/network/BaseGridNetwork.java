package com.knoxhack.echobasegrid.network;

import com.knoxhack.echobasegrid.api.ClaimActionResult;
import com.knoxhack.echobasegrid.service.BaseGridClaimService;
import com.echoplatform.echocore.api.network.EchoPacketKind;
import com.knoxhack.echonetcore.api.EchoNetPayloads;
import com.knoxhack.echonetcore.api.EchoNetSend;
import com.knoxhack.echonetcore.api.EchoPayloadContext;
import com.knoxhack.echonetcore.api.EchoPayloadRegistrar;
import com.knoxhack.echonetcore.api.EchoRateLimitPolicy;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class BaseGridNetwork {
    private BaseGridNetwork() {
    }

    public static void registerPayloads(Object event) {
        EchoPayloadRegistrar registrar = EchoNetPayloads.optional();
        EchoNetPayloads.serverboundAction(registrar, BaseGridSnapshotRequestPacket.TYPE,
                BaseGridSnapshotRequestPacket.CODEC,
                EchoRateLimitPolicy.of(5, "base_grid_snapshot"),
                BaseGridNetwork::handleSnapshotRequest);
        EchoNetPayloads.serverboundAction(registrar, BaseGridClaimActionPacket.TYPE,
                BaseGridClaimActionPacket.CODEC,
                EchoNetPayloads.terminalActionPolicy("base_grid_claim_action"),
                BaseGridNetwork::handleClaimAction);
        EchoNetPayloads.clientboundSync(registrar, BaseGridSnapshotPacket.TYPE,
                BaseGridSnapshotPacket.CODEC,
                (packet, player, context) -> BaseGridClientboundBridge.applySnapshot(packet));
    }

    private static void handleSnapshotRequest(BaseGridSnapshotRequestPacket packet, ServerPlayer player,
            EchoPayloadContext context) {
        sendSnapshot(player, packet.selectedDimension(), packet.selectedChunkX(), packet.selectedChunkZ(), "");
    }

    private static void handleClaimAction(BaseGridClaimActionPacket packet, ServerPlayer player,
            EchoPayloadContext context) {
        ClaimActionResult result = switch (packet.action()) {
            case REFRESH -> ClaimActionResult.success("Base Grid Refreshed", "");
            case CLAIM -> BaseGridClaimService.claim(player, packet.dimension(), packet.chunkX(), packet.chunkZ());
            case UNCLAIM -> BaseGridClaimService.unclaim(player, packet.dimension(), packet.chunkX(), packet.chunkZ());
            case ADD_MEMBER -> BaseGridClaimService.addMember(player, packet.dimension(), packet.chunkX(), packet.chunkZ(),
                    packet.targetPlayerId(), packet.targetPlayerName());
            case REMOVE_MEMBER -> BaseGridClaimService.removeMember(player, packet.dimension(), packet.chunkX(), packet.chunkZ(),
                    packet.targetPlayerId());
            case SET_ROLE -> BaseGridClaimService.setRole(player, packet.dimension(), packet.chunkX(), packet.chunkZ(),
                    packet.targetPlayerId(), packet.role());
            case TOGGLE_PERMISSION -> BaseGridClaimService.togglePermission(player, packet.dimension(), packet.chunkX(), packet.chunkZ(),
                    packet.targetPlayerId(), packet.permission());
        };
        if (!result.message().isBlank()) {
            player.sendSystemMessage(Component.literal("[ECHO-7] " + result.message()), true);
        }
        sendSnapshot(player, packet.dimension(), packet.chunkX(), packet.chunkZ(),
                (result.title().isBlank() ? "" : result.title() + ": ") + result.message());
    }

    public static void sendSnapshot(ServerPlayer player, String selectedDimension, int selectedChunkX, int selectedChunkZ,
            String status) {
        EchoNetSend.toPlayer(player,
                BaseGridSnapshotPacket.create(player, selectedDimension, selectedChunkX, selectedChunkZ, status),
                EchoPacketKind.CLIENTBOUND_SYNC);
    }
}
