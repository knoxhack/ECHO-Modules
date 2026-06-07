package com.knoxhack.echoorbitalremnants.progression;

import com.knoxhack.echoorbitalremnants.registry.ModBlocks;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class LaunchPadLocator {
    private static final int HORIZONTAL_RADIUS = 12;
    private static final int MIN_Y_OFFSET = -6;
    private static final int MAX_Y_OFFSET = 2;

    private LaunchPadLocator() {
    }

    public static Optional<BlockPos> findNearbyPlatformCenter(Player player) {
        BlockPos center = player.blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int y = center.getY() + MIN_Y_OFFSET; y <= center.getY() + MAX_Y_OFFSET; y++) {
            for (int x = center.getX() - HORIZONTAL_RADIUS; x <= center.getX() + HORIZONTAL_RADIUS; x++) {
                for (int z = center.getZ() - HORIZONTAL_RADIUS; z <= center.getZ() + HORIZONTAL_RADIUS; z++) {
                    BlockPos candidate = new BlockPos(x, y, z);
                    if (platformGridAt(player.level(), candidate)) {
                        double distance = distanceToPlayerSqr(player, candidate);
                        if (distance < bestDistance) {
                            best = candidate;
                            bestDistance = distance;
                        }
                    }
                }
            }
        }
        return Optional.ofNullable(best);
    }

    public static boolean platformGridAt(Level level, BlockPos center) {
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                if (level.getBlockState(center.offset(x, 0, z)).getBlock() != ModBlocks.LAUNCH_PLATFORM.get()) {
                    return false;
                }
            }
        }
        return true;
    }

    private static double distanceToPlayerSqr(Player player, BlockPos center) {
        double dx = center.getX() + 0.5D - player.getX();
        double dy = center.getY() + 0.5D - player.getY();
        double dz = center.getZ() + 0.5D - player.getZ();
        return dx * dx + dy * dy + dz * dz;
    }
}
