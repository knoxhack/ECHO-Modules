package com.knoxhack.echoholomap.network;

import com.knoxhack.echoholomap.Config;
import com.knoxhack.echoholomap.EchoHoloMap;
import com.knoxhack.echoholomap.waypoint.HoloMapWaypoint;
import com.knoxhack.echoholomap.world.HoloMapWaypointSavedData;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public record HoloMapWaypointSyncPacket(
        List<HoloMapWaypoint> waypoints,
        long gameTime) implements CustomPacketPayload {
    private static final int MAX_WAYPOINTS_PACKET = 2048;

    public static final Identifier ID = Identifier.fromNamespaceAndPath(EchoHoloMap.MODID, "waypoint_sync");
    public static final Type<HoloMapWaypointSyncPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, HoloMapWaypointSyncPacket> CODEC =
            StreamCodec.of(HoloMapWaypointSyncPacket::write, HoloMapWaypointSyncPacket::read);

    public HoloMapWaypointSyncPacket {
        waypoints = copyWaypoints(waypoints);
        gameTime = Math.max(0L, gameTime);
    }

    public static HoloMapWaypointSyncPacket from(ServerPlayer player) {
        if (player == null || player.level().getServer() == null) {
            return new HoloMapWaypointSyncPacket(List.of(), 0L);
        }
        HoloMapWaypointSavedData data = HoloMapWaypointSavedData.get(player.level().getServer());
        return new HoloMapWaypointSyncPacket(data.waypointsFor(player, maxSyncLimit()),
                player.level().getGameTime());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(RegistryFriendlyByteBuf buffer, HoloMapWaypointSyncPacket packet) {
        buffer.writeVarInt(packet.waypoints().size());
        for (HoloMapWaypoint waypoint : packet.waypoints()) {
            HoloMapWaypointCodecs.writeWaypoint(buffer, waypoint);
        }
        buffer.writeVarLong(packet.gameTime());
    }

    private static HoloMapWaypointSyncPacket read(RegistryFriendlyByteBuf buffer) {
        int count = Math.max(0, Math.min(MAX_WAYPOINTS_PACKET, buffer.readVarInt()));
        List<HoloMapWaypoint> waypoints = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            waypoints.add(HoloMapWaypointCodecs.readWaypoint(buffer));
        }
        return new HoloMapWaypointSyncPacket(waypoints, buffer.readVarLong());
    }

    private static List<HoloMapWaypoint> copyWaypoints(List<HoloMapWaypoint> waypoints) {
        if (waypoints == null || waypoints.isEmpty()) {
            return List.of();
        }
        return waypoints.stream()
                .filter(waypoint -> waypoint != null)
                .limit(MAX_WAYPOINTS_PACKET)
                .toList();
    }

    public static int maxSyncLimit() {
        try {
            return Math.max(16, Math.min(MAX_WAYPOINTS_PACKET, Config.WAYPOINT_SYNC_LIMIT.get()));
        } catch (RuntimeException exception) {
            return 256;
        }
    }
}
