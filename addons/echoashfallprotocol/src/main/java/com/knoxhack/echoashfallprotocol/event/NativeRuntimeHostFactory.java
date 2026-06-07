package com.knoxhack.echoashfallprotocol.event;

import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class NativeRuntimeHostFactory {
    private NativeRuntimeHostFactory() {
    }

    public static NativeEchoRuntimeHost create(ServerPlayer player, ServerLevel level) {
        return create(player, level, NativeRuntimeMutationLedgerSink.playerPersistent(player));
    }

    public static NativeEchoRuntimeHost create(
            ServerPlayer player,
            ServerLevel level,
            NativeRuntimeMutationLedgerSink ledgerSink) {
        return new NativeEchoRuntimeHost(new NativeRuntimeHostContext(
                player,
                level,
                EchoAshfallProtocol.MODID,
                ledgerSink));
    }

    public static NativeEchoRuntimeHost create(NativeRuntimeHostContext context) {
        return new NativeEchoRuntimeHost(context);
    }
}
