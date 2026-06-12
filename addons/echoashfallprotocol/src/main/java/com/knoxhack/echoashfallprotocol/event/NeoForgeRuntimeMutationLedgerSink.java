package com.knoxhack.echoashfallprotocol.event;

import net.minecraft.server.level.ServerPlayer;

/**
 * Compatibility alias for {@link NativeRuntimeMutationLedgerSink}.
 */
@Deprecated(forRemoval = false)
public interface NeoForgeRuntimeMutationLedgerSink extends NativeRuntimeMutationLedgerSink {
    String LEDGER_ROOT = NativeRuntimeMutationLedgerSink.LEDGER_ROOT;

    static NeoForgeRuntimeMutationLedgerSink noop() {
        return entry -> {
        };
    }

    static NeoForgeRuntimeMutationLedgerSink playerPersistent(ServerPlayer player) {
        NativeRuntimeMutationLedgerSink sink = NativeRuntimeMutationLedgerSink.playerPersistent(player);
        return sink::record;
    }
}
