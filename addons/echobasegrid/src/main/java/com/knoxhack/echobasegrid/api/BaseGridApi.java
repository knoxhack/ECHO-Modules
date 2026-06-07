package com.knoxhack.echobasegrid.api;

import com.knoxhack.echobasegrid.service.BaseGridClaimService;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public final class BaseGridApi {
    private BaseGridApi() {
    }

    public static boolean can(ServerPlayer player, Level level, BlockPos pos, ClaimPermission permission) {
        return BaseGridClaimService.can(player, level, pos, permission);
    }

    public static Optional<ClaimRecord> claimAt(Level level, BlockPos pos) {
        return BaseGridClaimService.claimAt(level, pos);
    }
}
