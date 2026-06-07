package com.knoxhack.echoashfallprotocol.event;

import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeMutationLedgerEntry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/**
 * Records mutations completed through the Native Loader live Minecraft runtime host.
 */
public interface NativeLoaderRuntimeMutationLedgerSink extends MinecraftRuntimeMutationLedgerSink {
    String LEDGER_ROOT = "echoashfallprotocol.native_loader.runtime_host.mutation_ledger";

    static NativeLoaderRuntimeMutationLedgerSink noop() {
        return entry -> {
        };
    }

    static NativeLoaderRuntimeMutationLedgerSink playerPersistent(ServerPlayer player) {
        if (player == null) {
            return noop();
        }
        return new PlayerPersistentLedgerSink(player);
    }

    final class PlayerPersistentLedgerSink implements NativeLoaderRuntimeMutationLedgerSink {
        private final ServerPlayer player;

        private PlayerPersistentLedgerSink(ServerPlayer player) {
            this.player = player;
        }

        @Override
        public void record(NativeMutationLedgerEntry entry) {
            if (entry == null) {
                return;
            }
            CompoundTag root = player.getPersistentData().getCompoundOrEmpty(LEDGER_ROOT).copy();
            int nextIndex = root.getIntOr("entryCount", 0) + 1;
            root.putInt("entryCount", nextIndex);
            root.putString("lastActionId", entry.actionId());
            root.putString("lastRuntimeHostId", entry.runtimeHostId());
            root.putString("lastResultStatus", entry.resultStatus().name());
            root.putString("lastFailureReason", entry.failureReason());
            root.putBoolean("lastSaveTouched", entry.saveTouched());
            root.putBoolean("lastHudOrEventEmitted", entry.hudOrEventEmitted());
            root.putLong("lastRecordedAtGameTime", player.level().getGameTime());
            root.putString("runtimeLane", "Native Loader");
            player.getPersistentData().put(LEDGER_ROOT, root);
        }
    }
}
