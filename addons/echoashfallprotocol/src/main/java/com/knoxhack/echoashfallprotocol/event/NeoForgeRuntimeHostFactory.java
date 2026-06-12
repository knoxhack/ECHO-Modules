package com.knoxhack.echoashfallprotocol.event;

import com.knoxhack.echoashfallprotocol.EchoAshfallProtocol;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Compatibility factory for the pre-Native runtime host class names.
 */
@Deprecated(forRemoval = false)
public final class NeoForgeRuntimeHostFactory {
    private NeoForgeRuntimeHostFactory() {
    }

    public static NeoForgeEchoRuntimeHost create(ServerPlayer player, ServerLevel level) {
        return create(player, level, NeoForgeRuntimeMutationLedgerSink.playerPersistent(player));
    }

    public static NeoForgeEchoRuntimeHost create(
            ServerPlayer player,
            ServerLevel level,
            NativeRuntimeMutationLedgerSink ledgerSink) {
        return new NeoForgeEchoRuntimeHost(new NativeRuntimeHostContext(
                player,
                level,
                EchoAshfallProtocol.MODID,
                NativeEchoRuntimeHost.RUNTIME_HOST_ID,
                ledgerSink));
    }

    public static NeoForgeEchoRuntimeHost create(NativeRuntimeHostContext context) {
        return new NeoForgeEchoRuntimeHost(context);
    }
}
