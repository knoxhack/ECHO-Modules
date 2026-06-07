package com.knoxhack.echoashfallprotocol.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * BiomeTintedGrassBlock - Grass that tints according to biome grass color.
 * Used for wasteland, toxic, and other biome-specific grass variants.
 */
public class BiomeTintedGrassBlock extends BushBlock {
    public static final MapCodec<BiomeTintedGrassBlock> CODEC = simpleCodec(BiomeTintedGrassBlock::new);

    public BiomeTintedGrassBlock(Properties properties) {
        super(properties);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public MapCodec<BushBlock> codec() {
        return (MapCodec) CODEC;
    }

    /**
     * Get the tint color for this block based on biome.
     * This is called by the block color provider on the client.
     * Returns -1 to indicate "use biome grass color".
     */
    public static int getTintColor(BlockState state, BlockGetter level, BlockPos pos) {
        // Return -1 to use the default biome grass color
        // The actual tinting is handled by BlockColors registration
        return -1;
    }
}
