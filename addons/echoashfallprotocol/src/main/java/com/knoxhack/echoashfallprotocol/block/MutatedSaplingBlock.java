package com.knoxhack.echoashfallprotocol.block;

import com.knoxhack.echoashfallprotocol.registry.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Mutated sapling - grows only in ruined soils.
 */
public class MutatedSaplingBlock extends Block {
    
    public MutatedSaplingBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return isRuinedSoil(level.getBlockState(pos.below()));
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(7) == 0 && canSurvive(state, level, pos) && hasRoom(level, pos)) {
            growMutatedTree(level, pos, random);
        }
    }

    private boolean hasRoom(ServerLevel level, BlockPos pos) {
        for (int y = 1; y <= 6; y++) {
            if (!level.isEmptyBlock(pos.above(y))) {
                return false;
            }
        }
        return true;
    }

    private void growMutatedTree(ServerLevel level, BlockPos pos, RandomSource random) {
        int height = 4 + random.nextInt(3);
        level.setBlock(pos, ModBlocks.DEAD_WOOD_LOG.get().defaultBlockState(), 3);

        for (int y = 1; y < height; y++) {
            BlockState log = random.nextInt(4) == 0
                    ? ModBlocks.CHARRED_WOOD_LOG.get().defaultBlockState()
                    : ModBlocks.DEAD_WOOD_LOG.get().defaultBlockState();
            level.setBlock(pos.above(y), log, 3);
        }

        BlockState leafA = ModBlocks.MUTATED_LEAVES_PURPLE.get().defaultBlockState();
        BlockState leafB = ModBlocks.MUTATED_LEAVES_GRAY.get().defaultBlockState();
        BlockPos crown = pos.above(height);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    int distance = Math.abs(dx) + Math.abs(dz) + Math.max(0, dy);
                    if (distance <= 3 && random.nextInt(5) != 0) {
                        BlockPos leafPos = crown.offset(dx, dy, dz);
                        if (level.isEmptyBlock(leafPos) || level.getBlockState(leafPos).is(Blocks.AIR)) {
                            level.setBlock(leafPos, random.nextBoolean() ? leafA : leafB, 3);
                        }
                    }
                }
            }
        }
    }

    private boolean isRuinedSoil(BlockState state) {
        return state.is(ModBlocks.WASTELAND_DIRT.get())
                || state.is(ModBlocks.WASTELAND_GRASS_BLOCK.get())
                || state.is(ModBlocks.ASHEN_WASTELAND_DIRT.get())
                || state.is(ModBlocks.BURNT_WASTELAND_SOIL.get())
                || state.is(ModBlocks.TOXIC_WASTELAND_GRASS_BLOCK.get())
                || state.is(ModBlocks.MUTATED_WASTELAND_GRASS_BLOCK.get())
                || state.is(ModBlocks.CONTAMINATED_SOIL.get());
    }
}
