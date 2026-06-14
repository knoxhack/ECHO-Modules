package com.knoxhack.echo.hazardcore.source;

import com.knoxhack.echo.hazardcore.api.HazardExposure;
import com.knoxhack.echo.hazardcore.api.HazardType;
import com.knoxhack.echo.hazardcore.api.IHazardSource;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Corruption exposure when standing near blocks tagged {@code echohazardcore:corruption_source}.
 */
public final class BlockCorruptionSource implements IHazardSource {
    public static final TagKey<Block> CORRUPTION_SOURCE = TagKey.create(
            Registries.BLOCK,
            Identifier.fromNamespaceAndPath("echohazardcore", "corruption_source"));
    public static final BlockCorruptionSource INSTANCE = new BlockCorruptionSource();

    private BlockCorruptionSource() {}

    @Override
    public boolean produces(HazardType hazard) {
        return hazard.equals(HazardType.CORRUPTION);
    }

    @Override
    public HazardExposure computeExposure(ServerPlayer player, HazardType hazard) {
        BlockPos pos = player.blockPosition();
        int radius = 4;
        float intensity = 0.0f;
        for (int dx = -radius; dx <= radius && intensity < 5.0f; dx++) {
            for (int dy = -radius; dy <= radius && intensity < 5.0f; dy++) {
                for (int dz = -radius; dz <= radius && intensity < 5.0f; dz++) {
                    BlockState state = player.level().getBlockState(pos.offset(dx, dy, dz));
                    if (state.is(CORRUPTION_SOURCE)) {
                        intensity += 0.5f;
                    }
                }
            }
        }
        return new HazardExposure(hazard, intensity, 1.0f, "corruption_block");
    }
}
