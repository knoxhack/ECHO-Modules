package com.knoxhack.echoholomap.network;

import com.knoxhack.echonetcore.api.EchoPayloadCodecs;
import com.knoxhack.echoholomap.waypoint.HoloMapWaypoint;
import com.knoxhack.echoholomap.waypoint.HoloMapWaypoint.Scope;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;

final class HoloMapWaypointCodecs {
    private static final int MAX_DIMENSION = 96;

    private HoloMapWaypointCodecs() {
    }

    static void writeWaypoint(RegistryFriendlyByteBuf buffer, HoloMapWaypoint waypoint) {
        EchoPayloadCodecs.writeIdentifier(buffer, waypoint.id());
        buffer.writeUUID(waypoint.owner());
        buffer.writeEnum(waypoint.scope());
        buffer.writeUtf(waypoint.dimension(), MAX_DIMENSION);
        buffer.writeDouble(waypoint.x());
        buffer.writeDouble(waypoint.y());
        buffer.writeDouble(waypoint.z());
        buffer.writeUtf(waypoint.title(), HoloMapWaypoint.MAX_TITLE);
        buffer.writeInt(waypoint.color());
        buffer.writeUtf(waypoint.icon(), HoloMapWaypoint.MAX_ICON);
        buffer.writeBoolean(waypoint.visible());
        buffer.writeVarLong(waypoint.createdTime());
        buffer.writeVarLong(waypoint.updatedTime());
    }

    static HoloMapWaypoint readWaypoint(RegistryFriendlyByteBuf buffer) {
        Identifier id = EchoPayloadCodecs.readIdentifier(buffer);
        UUID owner = buffer.readUUID();
        Scope scope = buffer.readEnum(Scope.class);
        String dimension = buffer.readUtf(MAX_DIMENSION);
        double x = buffer.readDouble();
        double y = buffer.readDouble();
        double z = buffer.readDouble();
        String title = buffer.readUtf(HoloMapWaypoint.MAX_TITLE);
        int color = buffer.readInt();
        String icon = buffer.readUtf(HoloMapWaypoint.MAX_ICON);
        boolean visible = buffer.readBoolean();
        long createdTime = buffer.readVarLong();
        long updatedTime = buffer.readVarLong();
        return new HoloMapWaypoint(id, owner, scope, dimension, x, y, z, title, color, icon,
                visible, createdTime, updatedTime);
    }
}
