package com.knoxhack.echonetcore.network;

import com.echoplatform.echocore.api.EchoFactionDataService;
import com.echoplatform.echocore.api.network.EchoDiscoveryToast;
import com.echoplatform.echocore.api.network.EchoPacketDirection;
import com.echoplatform.echocore.api.network.EchoPacketKind;
import com.knoxhack.echonetcore.EchoNetCore;
import com.knoxhack.echonetcore.api.EchoClientSyncRegistry;
import com.knoxhack.echonetcore.api.EchoDebugCommandRegistry;
import com.knoxhack.echonetcore.api.EchoNetPayloads;
import com.knoxhack.echonetcore.api.EchoPayloadContext;
import com.knoxhack.echonetcore.api.EchoPayloadRegistrar;
import com.knoxhack.echonetcore.api.EchoServerActionGuards;
import com.knoxhack.echonetcore.config.EchoNetCoreConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class EchoNetCorePackets {
    private EchoNetCorePackets() {
    }

    public static EchoPayloadRegistrar register() {
        return register(EchoNetPayloads.optional());
    }

    public static EchoPayloadRegistrar register(EchoPayloadRegistrar registrar) {
        EchoNetPayloads.clientboundSync(registrar, EchoFactionSyncPacket.TYPE, EchoFactionSyncPacket.CODEC,
                EchoNetCorePackets::handleFactionSync);
        EchoNetPayloads.clientboundSync(registrar, DiscoveryToastPacket.TYPE, DiscoveryToastPacket.CODEC,
                EchoNetCorePackets::handleDiscoveryToast);
        EchoNetPayloads.clientboundSync(registrar, EchoSyncPayload.TYPE, EchoSyncPayload.CODEC,
                (packet, player, context) -> EchoClientSyncRegistry.dispatch(packet));
        EchoNetPayloads.debugServerbound(registrar, EchoDebugCommandPacket.TYPE, EchoDebugCommandPacket.CODEC,
                EchoNetPayloads.debugActionPolicy("debug_command"),
                EchoNetCorePackets::handleDebugCommand);
        return registrar;
    }

    private static void handleFactionSync(EchoFactionSyncPacket packet, Player player, EchoPayloadContext context) {
        if (player != null) {
            EchoFactionDataService.importRoot(player, packet.factionRoot());
        }
    }

    private static void handleDiscoveryToast(DiscoveryToastPacket packet, Player player, EchoPayloadContext context) {
        try {
            Class<?> hud = Class.forName("com.knoxhack.echoterminal.client.discovery.DiscoveryToastHud");
            hud.getMethod("push", EchoDiscoveryToast.class).invoke(null, packet.toast());
        } catch (ReflectiveOperationException ignored) {
            EchoNetCore.LOGGER.debug("Discovery toast received without a terminal HUD consumer.");
        }
    }

    private static void handleDebugCommand(EchoDebugCommandPacket packet, ServerPlayer player, EchoPayloadContext context) {
        if (!EchoNetCoreConfig.ENABLE_DEBUG_PACKETS.get()) {
            EchoNetDebug.emit(packet.type().id(), EchoPacketDirection.SERVERBOUND, EchoPacketKind.DEBUG_DEV,
                    player.getScoreboardName(), false, "debug-disabled");
            return;
        }
        EchoServerActionGuards.GuardResult<ServerPlayer> op = EchoServerActionGuards.requireOp(player);
        if (op.rejected()) {
            EchoNetDebug.emit(packet.type().id(), EchoPacketDirection.SERVERBOUND, EchoPacketKind.DEBUG_DEV,
                    player.getScoreboardName(), false, op.reason());
            return;
        }
        if (!EchoDebugCommandRegistry.handle(player, packet.commandId(), packet.payload())) {
            EchoNetDebug.emit(packet.type().id(), EchoPacketDirection.SERVERBOUND, EchoPacketKind.DEBUG_DEV,
                    player.getScoreboardName(), false, "unknown-command");
        }
    }
}
