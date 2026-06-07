package com.knoxhack.echobasegrid.network;

import com.knoxhack.echobasegrid.EchoBaseGrid;
import com.knoxhack.echobasegrid.api.ClaimPermission;
import com.knoxhack.echobasegrid.api.ClaimRole;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record BaseGridClaimActionPacket(
        Action action,
        String dimension,
        int chunkX,
        int chunkZ,
        UUID targetPlayerId,
        String targetPlayerName,
        ClaimRole role,
        ClaimPermission permission) implements CustomPacketPayload {
    private static final int MAX_TEXT = 160;

    public static final Identifier ID = EchoBaseGrid.id("claim_action");
    public static final Type<BaseGridClaimActionPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, BaseGridClaimActionPacket> CODEC =
            StreamCodec.of(BaseGridClaimActionPacket::write, BaseGridClaimActionPacket::read);

    public BaseGridClaimActionPacket {
        action = action == null ? Action.REFRESH : action;
        dimension = dimension == null ? "" : dimension.strip();
        targetPlayerName = targetPlayerName == null ? "" : targetPlayerName.strip();
        role = role == null ? ClaimRole.MEMBER : role;
        permission = permission == null ? ClaimPermission.BUILD : permission;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(RegistryFriendlyByteBuf buffer, BaseGridClaimActionPacket packet) {
        buffer.writeUtf(packet.action().name(), 48);
        buffer.writeUtf(packet.dimension(), MAX_TEXT);
        buffer.writeInt(packet.chunkX());
        buffer.writeInt(packet.chunkZ());
        buffer.writeBoolean(packet.targetPlayerId() != null);
        if (packet.targetPlayerId() != null) {
            buffer.writeUUID(packet.targetPlayerId());
        }
        buffer.writeUtf(packet.targetPlayerName(), MAX_TEXT);
        buffer.writeUtf(packet.role().name(), 48);
        buffer.writeUtf(packet.permission().name(), 48);
    }

    private static BaseGridClaimActionPacket read(RegistryFriendlyByteBuf buffer) {
        Action action = safeAction(buffer.readUtf(48));
        String dimension = buffer.readUtf(MAX_TEXT);
        int chunkX = buffer.readInt();
        int chunkZ = buffer.readInt();
        UUID target = buffer.readBoolean() ? buffer.readUUID() : null;
        String name = buffer.readUtf(MAX_TEXT);
        ClaimRole role = ClaimRole.fromId(buffer.readUtf(48));
        ClaimPermission permission = ClaimPermission.fromId(buffer.readUtf(48));
        return new BaseGridClaimActionPacket(action, dimension, chunkX, chunkZ, target, name, role, permission);
    }

    private static Action safeAction(String raw) {
        if (raw == null || raw.isBlank()) {
            return Action.REFRESH;
        }
        try {
            return Action.valueOf(raw.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return Action.REFRESH;
        }
    }

    public enum Action {
        REFRESH,
        CLAIM,
        UNCLAIM,
        ADD_MEMBER,
        REMOVE_MEMBER,
        SET_ROLE,
        TOGGLE_PERMISSION
    }
}
