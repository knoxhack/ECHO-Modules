package com.knoxhack.echopowergrid.api;

import com.knoxhack.echopowergrid.grid.PowerNetworkManager;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class EchoPowerGridApi {
    private EchoPowerGridApi() {}

    public static Optional<EchoPowerNetwork> getNetwork(Level level, BlockPos pos) {
        if (level == null || pos == null) return Optional.empty();
        return Optional.ofNullable(PowerNetworkManager.get(level).getNetworkAt(pos));
    }

    public static Optional<EchoEnergyStorage> getEnergyStorage(Level level, BlockPos pos) {
        if (level == null || pos == null) return Optional.empty();
        return PowerNetworkManager.get(level).getEnergyStorageAt(pos);
    }

    public static boolean isPowered(Level level, BlockPos pos) {
        return getNetwork(level, pos).map(n -> n.state != EchoGridState.OFFLINE && n.state != EchoGridState.TRIPPED).orElse(false);
    }

    public static long getAvailablePower(Level level, BlockPos pos) {
        if (level == null || pos == null) {
            return 0L;
        }
        return PowerNetworkManager.get(level).drawPower(pos, Long.MAX_VALUE / 8L, true).drawn();
    }

    public static boolean requestPower(Level level, BlockPos pos, long epPerTick) {
        if (level == null || pos == null) return false;
        return PowerNetworkManager.get(level).requestPower(pos, epPerTick);
    }

    public static PowerGridDrawResult drawPower(Level level, BlockPos pos, long ep, boolean simulate) {
        if (level == null || pos == null || ep <= 0) {
            return PowerGridDrawResult.empty(ep, simulate);
        }
        return PowerNetworkManager.get(level).drawPower(pos, ep, simulate);
    }

    public static List<PowerGridNetworkSummary> loadedNetworkSummaries(ServerLevel level) {
        if (level == null) {
            return List.of();
        }
        return PowerNetworkManager.get(level).loadedNetworkSummaries();
    }

    public static List<PowerGridNodeSummary> loadedNodeSummaries(ServerLevel level) {
        if (level == null) {
            return List.of();
        }
        return PowerNetworkManager.get(level).loadedNodeSummaries();
    }

    public static List<PowerGridAlert> alerts(ServerLevel level) {
        if (level == null) {
            return List.of();
        }
        return PowerNetworkManager.get(level).alerts();
    }

    public static PowerGridRouteSummary routeSummary(Level level, BlockPos from, BlockPos to) {
        if (level == null || from == null || to == null) {
            return PowerGridRouteSummary.blocked(from, to, 0L, "Missing route endpoint.");
        }
        return PowerNetworkManager.get(level).routeSummary(from, to, 0L);
    }

    public static Optional<PowerGridNetworkSummary> networkAt(Level level, BlockPos pos) {
        if (level == null || pos == null) {
            return Optional.empty();
        }
        return PowerNetworkManager.get(level).networkSummaryAt(pos);
    }

    public static void markNetworkDirty(Level level, BlockPos pos) {
        if (level == null || pos == null) return;
        PowerNetworkManager.get(level).markDirty(pos);
    }

    public static PowerGridSnapshot getSnapshot(Level level, BlockPos pos) {
        return getNetwork(level, pos).map(EchoPowerNetwork::toSnapshot).orElse(
            new PowerGridSnapshot(new java.util.UUID(0, 0), 0, 0, 0, 0, EchoGridState.OFFLINE, EchoPowerQuality.STABLE, 0)
        );
    }
}
