package com.knoxhack.echoterminal.network;

import com.knoxhack.echocore.api.config.EchoConfigSide;
import com.knoxhack.echoterminal.EchoTerminal;
import java.util.Locale;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record TerminalConfigActionPacket(
        Action action,
        EchoConfigSide side,
        String moduleId,
        String entryId,
        String value) implements CustomPacketPayload {
    private static final int MAX_ID = 160;
    private static final int MAX_VALUE = 512;

    public static final Identifier ID = Identifier.fromNamespaceAndPath(EchoTerminal.MODID, "terminal_config_action");
    public static final Type<TerminalConfigActionPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalConfigActionPacket> CODEC =
            StreamCodec.of(TerminalConfigActionPacket::write, TerminalConfigActionPacket::read);

    public TerminalConfigActionPacket {
        action = action == null ? Action.REQUEST : action;
        side = side == null ? EchoConfigSide.COMMON : side;
        moduleId = clean(moduleId);
        entryId = clean(entryId);
        value = value == null ? "" : value.strip();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(RegistryFriendlyByteBuf buffer, TerminalConfigActionPacket packet) {
        buffer.writeUtf(packet.action().name(), 32);
        buffer.writeUtf(packet.side().name(), 32);
        buffer.writeUtf(packet.moduleId(), MAX_ID);
        buffer.writeUtf(packet.entryId(), MAX_ID);
        buffer.writeUtf(packet.value(), MAX_VALUE);
    }

    private static TerminalConfigActionPacket read(RegistryFriendlyByteBuf buffer) {
        return new TerminalConfigActionPacket(
                safeAction(buffer.readUtf(32)),
                safeSide(buffer.readUtf(32)),
                buffer.readUtf(MAX_ID),
                buffer.readUtf(MAX_ID),
                buffer.readUtf(MAX_VALUE));
    }

    private static Action safeAction(String value) {
        try {
            return Action.valueOf(value);
        } catch (RuntimeException exception) {
            return Action.REQUEST;
        }
    }

    private static EchoConfigSide safeSide(String value) {
        try {
            return EchoConfigSide.valueOf(value);
        } catch (RuntimeException exception) {
            return EchoConfigSide.COMMON;
        }
    }

    private static String clean(String value) {
        return value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
    }

    public enum Action {
        REQUEST,
        SET,
        RESET
    }
}
