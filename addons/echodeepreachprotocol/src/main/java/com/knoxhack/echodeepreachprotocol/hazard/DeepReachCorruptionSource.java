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
 * Deep Reach lattice corruption.
 *
 * <p>Standing near active Lattice blocks (especially {@code lattice_crystal}) exposes
 * the player to precursor corruption. Resistance from advanced suits and habitats
 * is required to survive extended expeditions into The Lattice.
 */
public final class DeepReachCorruptionSource implements IHazardSource {
    public static final DeepReachCorruptionSource INSTANCE = new DeepReachCorruptionSource();

    public static final TagKey<Block> LATTICE_SOURCE = TagKey.create(
            Registries.BLOCK,
            Identifier.fromNamespaceAndPath("echodeepreachprotocol", "lattice_source"));

    private static final int RADIUS = 4;
    private static final float MAX_INTENSITY = 5.0f;

    private DeepReachCorruptionSource() {
    }

    @Override
    public boolean produces(HazardType hazard) {
        return hazard.equals(HazardType.CORRUPTION);
    }

    @Override
    public HazardExposure computeExposure(ServerPlayer player, HazardType hazard) {
        BlockPos pos = player.blockPosition();
        float intensity = 0.0f;
        for (int dx = -RADIUS; dx <= RADIUS && intensity < MAX_INTENSITY; dx++) {
            for (int dy = -RADIUS; dy <= RADIUS && intensity < MAX_INTENSITY; dy++) {
                for (int dz = -RADIUS; dz <= RADIUS && intensity < MAX_INTENSITY; dz++) {
                    BlockState state = player.level().getBlockState(pos.offset(dx, dy, dz));
                    if (state.is(LATTICE_SOURCE)) {
                        intensity += 0.5f;
                    }
                }
            }
        }
        float multiplier = DeepReachSeasonManager.INSTANCE.getMultiplier(hazard);
        return new HazardExposure(hazard, intensity * multiplier, 1.0f, "deep_reach_lattice");
    }
}
