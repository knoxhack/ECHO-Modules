package com.knoxhack.echoashfallprotocol.survival;

import com.knoxhack.echoashfallprotocol.registry.ModAttachments;
import com.knoxhack.echoashfallprotocol.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Shared radiation detection helpers. Extracted so fast-travel, commands, and HUD
 * code can answer "is this player irradiated?" without duplicating the scan logic
 * that lives in {@link SurvivalTickHandler}.
 */
public final class RadiationUtil {
    private RadiationUtil() {}

    /** Radius in blocks scanned for radiation blocks around the player. */
    private static final int SCAN_RADIUS = 3;

    /**
     * @return true if there is an EchoAshfallProtocol radiation block within SCAN_RADIUS of the player
     *         at any of y-1, y, y+1.
     */
    public static boolean isPlayerIrradiated(Player player) {
        if (player == null) return false;
        Level level = player.level();
        BlockPos playerPos = player.blockPosition();
        for (BlockPos.MutableBlockPos pos : BlockPos.spiralAround(
                playerPos, SCAN_RADIUS, Direction.EAST, Direction.SOUTH)) {
            if (level.getBlockState(pos.atY(playerPos.getY())).is(ModBlocks.RADIATION_BLOCK.get()) ||
                level.getBlockState(pos.atY(playerPos.getY() - 1)).is(ModBlocks.RADIATION_BLOCK.get()) ||
                level.getBlockState(pos.atY(playerPos.getY() + 1)).is(ModBlocks.RADIATION_BLOCK.get())) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return true if the player's accumulated radiation level is above a high-risk threshold.
     *         Used by fast-travel to block teleports while player is poisoned, even if they
     *         have stepped out of the immediate scan radius.
     */
    public static boolean isPlayerContaminated(Player player) {
        if (player == null) return false;
        SurvivalData data = player.getData(ModAttachments.SURVIVAL_DATA.get());
        return data != null && data.getRadiationLevel() >= 50.0f;
    }
}
