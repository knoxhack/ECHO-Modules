package com.knoxhack.echoholomap.network;

import com.knoxhack.echonetcore.api.EchoPayloadCodecs;
import com.knoxhack.echoholomap.EchoHoloMap;
import com.knoxhack.echoholomap.waypoint.HoloMapWaypoint;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record HoloMapWaypointActionPacket(
        Action action,
        HoloMapWaypoint waypoint,
        Identifier waypointId) implements CustomPacketPayload {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(EchoHoloMap.MODID, "waypoint_action");
    public static final Type<HoloMapWaypointActionPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, HoloMapWaypointActionPacket> CODEC =
            StreamCodec.of(HoloMapWaypointActionPacket::write, HoloMapWaypointActionPacket::read);

    public HoloMapWaypointActionPacket {
        action = action == null ? Action.REQUEST_SYNC : action;
        if (waypointId == null && waypoint != null) {
            waypointId = waypoint.id();
        }
        waypointId = waypointId == null
                ? Identifier.fromNamespaceAndPath(EchoHoloMap.MODID, "waypoint/none")
                : waypointId;
    }

    public static HoloMapWaypointActionPacket requestSync() {
        return new HoloMapWaypointActionPacket(Action.REQUEST_SYNC, null, null);
    }

    public static HoloMapWaypointActionPacket upsert(HoloMapWaypoint waypoint) {
        return new HoloMapWaypointActionPacket(Action.UPSERT, waypoint, waypoint == null ? null : waypoint.id());
    }

    public static HoloMapWaypointActionPacket delete(Identifier waypointId) {
        return new HoloMapWaypointActionPacket(Action.DELETE, null, waypointId);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(RegistryFriendlyByteBuf buffer, HoloMapWaypointActionPacket packet) {
        buffer.writeEnum(packet.action());
        buffer.writeBoolean(packet.waypoint() != null);
        if (packet.waypoint() != null) {
            HoloMapWaypointCodecs.writeWaypoint(buffer, packet.waypoint());
        }
        EchoPayloadCodecs.writeIdentifier(buffer, packet.waypointId());
    }

    private static HoloMapWaypointActionPacket read(RegistryFriendlyByteBuf buffer) {
        Action action = buffer.readEnum(Action.class);
        HoloMapWaypoint waypoint = buffer.readBoolean() ? HoloMapWaypointCodecs.readWaypoint(buffer) : null;
        Identifier waypointId = EchoPayloadCodecs.readIdentifier(buffer);
        return new HoloMapWaypointActionPacket(action, waypoint, waypointId);
    }

    public enum Action {
        REQUEST_SYNC,
        UPSERT,
        DELETE
    }
}
