package com.knoxhack.echoorbitalremnants.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

public record GroundRecoverySite(GroundRecoverySiteType type, BlockPos pos, boolean complete) {
    private static final int SCAN_RADIUS_SQR = 16 * 16;

    public GroundRecoverySite completed() {
        return new GroundRecoverySite(type, pos, true);
    }

    public boolean near(Player player) {
        return player.blockPosition().distSqr(pos) <= SCAN_RADIUS_SQR;
    }

    public int distanceTo(Player player) {
        return (int) Math.round(Math.sqrt(player.blockPosition().distSqr(pos)));
    }

    public String serialize() {
        return type.name() + "," + pos.getX() + "," + pos.getY() + "," + pos.getZ() + "," + complete;
    }

    public static GroundRecoverySite deserialize(String value) {
        String[] parts = value.split(",", -1);
        if (parts.length < 5) {
            return null;
        }
        try {
            GroundRecoverySiteType type = GroundRecoverySiteType.byName(parts[0]);
            BlockPos pos = new BlockPos(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
            return new GroundRecoverySite(type, pos, Boolean.parseBoolean(parts[4]));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
