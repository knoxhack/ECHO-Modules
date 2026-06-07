package com.knoxhack.echoorbitalremnants.network;

import com.knoxhack.echoorbitalremnants.EchoOrbitalRemnants;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record EchoTerminalActionPayload(Action action) implements CustomPacketPayload {
    public static final Type<EchoTerminalActionPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(EchoOrbitalRemnants.MODID, "echo_terminal_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EchoTerminalActionPayload> STREAM_CODEC =
            StreamCodec.of((buffer, payload) -> buffer.writeInt(payload.action().ordinal()),
                    buffer -> new EchoTerminalActionPayload(Action.byId(buffer.readInt())));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public enum Action {
        REFRESH,
        SCAN;

        private static final Action[] BY_ID = values();

        public static Action byId(int id) {
            return id >= 0 && id < BY_ID.length ? BY_ID[id] : REFRESH;
        }
    }
}
