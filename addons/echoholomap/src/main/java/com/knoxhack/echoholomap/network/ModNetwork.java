package com.knoxhack.echoholomap.network;

import com.echoplatform.echocore.api.network.EchoPacketKind;
import com.knoxhack.echonetcore.api.EchoNetPayloads;
import com.knoxhack.echonetcore.api.EchoNetSend;
import com.knoxhack.echonetcore.api.EchoPayloadContext;
import com.knoxhack.echonetcore.api.EchoPayloadRegistrar;
import com.knoxhack.echoholomap.api.HoloMapChunkActionResult;
import com.knoxhack.echoholomap.api.HoloMapChunkSelection;
import com.knoxhack.echoholomap.integration.HoloMapSoundHooks;
import com.knoxhack.echoholomap.map.HoloMapChunkActions;
import com.knoxhack.echoholomap.map.HoloMapTerrainScanner;
import com.knoxhack.echoholomap.world.HoloMapTerrainSavedData;
import com.knoxhack.echoholomap.world.HoloMapWaypointSavedData;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.level.Level;

public final class ModNetwork {
    private ModNetwork() {
    }

    public static void registerPayloads(Object event) {
        EchoPayloadRegistrar registrar = EchoNetPayloads.optional();
        EchoNetPayloads.clientboundSync(registrar, HoloMapSnapshotPacket.TYPE, HoloMapSnapshotPacket.CODEC,
                ModNetwork::handleSnapshot);
        EchoNetPayloads.clientboundSync(registrar, HoloMapTileBatchPacket.TYPE, HoloMapTileBatchPacket.CODEC,
                ModNetwork::handleTileBatch);
        EchoNetPayloads.clientboundSync(registrar, HoloMapWaypointSyncPacket.TYPE, HoloMapWaypointSyncPacket.CODEC,
                ModNetwork::handleWaypointSync);
        EchoNetPayloads.serverboundAction(registrar, HoloMapTileRequestPacket.TYPE, HoloMapTileRequestPacket.CODEC,
                EchoNetPayloads.defaultActionPolicy("holomap_tiles"), ModNetwork::handleTileRequest);
        EchoNetPayloads.serverboundAction(registrar, HoloMapWaypointActionPacket.TYPE, HoloMapWaypointActionPacket.CODEC,
                EchoNetPayloads.defaultActionPolicy("holomap_waypoints"), ModNetwork::handleWaypointAction);
        EchoNetPayloads.serverboundAction(registrar, HoloMapChunkActionPacket.TYPE, HoloMapChunkActionPacket.CODEC,
                EchoNetPayloads.defaultActionPolicy("holomap_chunk_actions"), ModNetwork::handleChunkAction);
        EchoNetPayloads.serverboundAction(registrar, HoloMapSyncRequestPacket.TYPE, HoloMapSyncRequestPacket.CODEC,
                EchoNetPayloads.defaultActionPolicy("holomap_sync"), ModNetwork::handleSyncRequest);
    }

    private static void handleSnapshot(HoloMapSnapshotPacket packet,
            net.minecraft.world.entity.player.Player player, EchoPayloadContext context) {
        HoloMapClientState.apply(packet);
    }

    private static void handleTileBatch(HoloMapTileBatchPacket packet,
            net.minecraft.world.entity.player.Player player, EchoPayloadContext context) {
        HoloMapTerrainClientState.apply(packet);
    }

    private static void handleWaypointSync(HoloMapWaypointSyncPacket packet,
            net.minecraft.world.entity.player.Player player, EchoPayloadContext context) {
        HoloMapWaypointClientState.apply(packet);
    }

    private static void handleTileRequest(HoloMapTileRequestPacket packet,
            net.minecraft.server.level.ServerPlayer player, EchoPayloadContext context) {
        if (packet != null) {
            HoloMapTerrainScanner.scanRequestedViewport(player, packet.dimension(),
                    packet.centerChunkX(), packet.centerChunkZ(), packet.safeRadius());
        }
        EchoNetSend.toPlayer(player, HoloMapTileBatchPacket.from(player, packet), EchoPacketKind.CLIENTBOUND_SYNC);
    }

    private static void handleWaypointAction(HoloMapWaypointActionPacket packet,
            net.minecraft.server.level.ServerPlayer player, EchoPayloadContext context) {
        if (packet == null || player.level().getServer() == null) {
            return;
        }
        HoloMapWaypointSavedData data = HoloMapWaypointSavedData.get(player.level().getServer());
        boolean mayEditShared = player.createCommandSourceStack()
                .permissions()
                .hasPermission(Permissions.COMMANDS_GAMEMASTER);
        boolean changed = false;
        switch (packet.action()) {
            case UPSERT -> {
                changed = data.upsert(player, packet.waypoint(), mayEditShared);
                if (changed) {
                    HoloMapSoundHooks.play(player, HoloMapSoundHooks.WAYPOINT_CREATE);
                }
            }
            case DELETE -> {
                changed = data.delete(player, packet.waypointId(), mayEditShared);
                if (changed) {
                    HoloMapSoundHooks.play(player, HoloMapSoundHooks.WAYPOINT_DELETE);
                }
            }
            case REQUEST_SYNC -> {
            }
        }
        EchoNetSend.toPlayer(player, HoloMapWaypointSyncPacket.from(player), EchoPacketKind.CLIENTBOUND_SYNC);
    }

    private static void handleChunkAction(HoloMapChunkActionPacket packet,
            net.minecraft.server.level.ServerPlayer player, EchoPayloadContext context) {
        if (packet == null || player == null || !(player.level() instanceof net.minecraft.server.level.ServerLevel level)) {
            return;
        }
        Identifier dimensionId = Identifier.tryParse(packet.dimension());
        if (dimensionId == null || !player.level().dimension().identifier().equals(dimensionId)) {
            sendChunkActionStatus(player, HoloMapChunkActionResult.failure("Wrong Dimension",
                    "HoloMap chunk actions only apply in your current dimension."));
            return;
        }
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, dimensionId);
        boolean known = HoloMapTerrainSavedData.get(level)
                .hasRenderableTile(player.getUUID(), dimension, packet.chunkX(), packet.chunkZ());
        if (!known) {
            sendChunkActionStatus(player, HoloMapChunkActionResult.failure("Pending Scan",
                    "Load this chunk first so HoloMap can sample real terrain."));
            return;
        }
        HoloMapChunkSelection selection = new HoloMapChunkSelection(dimension, packet.chunkX(), packet.chunkZ());
        HoloMapChunkActionResult result =
                HoloMapChunkActions.handle(player, selection, packet.providerId(), packet.actionId());
        sendChunkActionStatus(player, result);
        HoloMapSync.send(player);
    }

    private static void sendChunkActionStatus(net.minecraft.server.level.ServerPlayer player,
            HoloMapChunkActionResult result) {
        if (player == null || result == null || result.statusLine().isBlank()) {
            return;
        }
        player.sendSystemMessage(Component.literal("[ECHO-7] " + result.statusLine()), true);
    }

    private static void handleSyncRequest(HoloMapSyncRequestPacket packet,
            net.minecraft.server.level.ServerPlayer player, EchoPayloadContext context) {
        HoloMapSync.send(player);
    }
}
