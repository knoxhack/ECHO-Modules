package com.knoxhack.echopowergrid.api;

import net.minecraft.core.BlockPos;

public record PowerGridRouteSummary(
        BlockPos from,
        BlockPos to,
        int cableDistance,
        long pathTransferLimit,
        double lossPercent,
        long requestedEp,
        long lossEp,
        long deliverableEp,
        boolean blocked,
        String blockedReason) {
    public PowerGridRouteSummary {
        from = from == null ? BlockPos.ZERO : from.immutable();
        to = to == null ? BlockPos.ZERO : to.immutable();
        cableDistance = Math.max(0, cableDistance);
        pathTransferLimit = Math.max(0L, pathTransferLimit);
        lossPercent = Math.max(0.0D, lossPercent);
        requestedEp = Math.max(0L, requestedEp);
        lossEp = Math.max(0L, lossEp);
        deliverableEp = Math.max(0L, Math.min(deliverableEp, requestedEp));
        blockedReason = blockedReason == null ? "" : blockedReason;
    }

    public static PowerGridRouteSummary blocked(BlockPos from, BlockPos to, long requestedEp, String reason) {
        return new PowerGridRouteSummary(from, to, 0, 0, 0.0D, requestedEp, 0, 0, true, reason);
    }
}
