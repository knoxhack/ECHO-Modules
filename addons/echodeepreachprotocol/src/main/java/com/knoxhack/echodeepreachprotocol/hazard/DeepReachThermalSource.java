package com.knoxhack.echodeepreachprotocol.hazard;

import com.knoxhack.echo.hazardcore.api.HazardExposure;
import com.knoxhack.echo.hazardcore.api.HazardType;
import com.knoxhack.echo.hazardcore.api.IHazardSource;
import com.knoxhack.echodeepreachprotocol.season.DeepReachSeasonManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Deep Reach thermal hazards.
 *
 * <p>Heat radiates from magma and active thermal vents; cold settles into the
 * lightless depths below the Abyssal Rifts. Both use the same scan radius so a
 * player standing near a vent in the trenches can experience both simultaneously.
 */
public final class DeepReachThermalSource implements IHazardSource {
    public static final DeepReachThermalSource INSTANCE = new DeepReachThermalSource();

    public static final TagKey<Block> THERMAL_SOURCE = TagKey.create(
            Registries.BLOCK,
            Identifier.fromNamespaceAndPath("echodeepreachprotocol", "thermal_source"));

    private static final int RADIUS = 4;
    private static final float MAX_HEAT = 4.0f;
    private static final float MAX_COLD = 3.0f;

    private DeepReachThermalSource() {
    }

    @Override
    public boolean produces(HazardType hazard) {
        return hazard.equals(HazardType.HEAT) || hazard.equals(HazardType.COLD);
    }

    @Override
    public HazardExposure computeExposure(ServerPlayer player, HazardType hazard) {
        if (hazard.equals(HazardType.HEAT)) {
            return computeHeat(player);
        }
        if (hazard.equals(HazardType.COLD)) {
            return computeCold(player);
        }
        return new HazardExposure(hazard, 0.0f, 1.0f, "deep_reach_thermal");
    }

    private HazardExposure computeHeat(ServerPlayer player) {
        BlockPos pos = player.blockPosition();
        float intensity = 0.0f;
        for (int dx = -RADIUS; dx <= RADIUS && intensity < MAX_HEAT; dx++) {
            for (int dy = -RADIUS; dy <= RADIUS && intensity < MAX_HEAT; dy++) {
                for (int dz = -RADIUS; dz <= RADIUS && intensity < MAX_HEAT; dz++) {
                    BlockState state = player.level().getBlockState(pos.offset(dx, dy, dz));
                    if (state.is(THERMAL_SOURCE)) {
                        intensity += 0.75f;
                    }
                }
            }
        }
        float multiplier = DeepReachSeasonManager.INSTANCE.getMultiplier(HazardType.HEAT);
        return new HazardExposure(HazardType.HEAT, Math.min(intensity * multiplier, MAX_HEAT), 1.0f, "deep_reach_thermal");
    }

    private HazardExposure computeCold(ServerPlayer player) {
        double y = player.getY();
        if (y >= -20.0) {
            return new HazardExposure(HazardType.COLD, 0.0f, 1.0f, "deep_reach_thermal");
        }
        float intensity = (float) ((-20.0 - y) / 20.0);
        float multiplier = DeepReachSeasonManager.INSTANCE.getMultiplier(HazardType.COLD);
        return new HazardExposure(HazardType.COLD, Math.min(intensity * multiplier, MAX_COLD), 1.0f, "deep_reach_thermal");
    }
}
