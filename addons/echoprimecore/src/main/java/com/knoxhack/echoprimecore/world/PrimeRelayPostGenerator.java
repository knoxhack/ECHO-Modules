package com.knoxhack.echoprimecore.world;

import com.knoxhack.echoprimecore.EchoPrimeCore;
import com.knoxhack.echoprimecore.registry.ModBlocks;
import com.knoxhack.echoprimecore.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.loot.LootTable;

public final class PrimeRelayPostGenerator {
    public static final ResourceKey<LootTable> ABANDONED_RELAY_POST_LOOT = ResourceKey.create(
            Registries.LOOT_TABLE, EchoPrimeCore.id("chests/abandoned_relay_post"));

    private PrimeRelayPostGenerator() {
    }

    public static BlockPos placeStarterRelay(ServerLevel level, BlockPos origin, RandomSource random) {
        int distance = 72 + random.nextInt(40);
        double angle = random.nextDouble() * Math.PI * 2.0D;
        int x = origin.getX() + (int) Math.round(Math.cos(angle) * distance);
        int z = origin.getZ() + (int) Math.round(Math.sin(angle) * distance);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos base = new BlockPos(x, Math.max(level.getMinY() + 8, y), z);
        placeRelay(level, base, random);
        return base;
    }

    private static void placeRelay(ServerLevel level, BlockPos base, RandomSource random) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) == 2 && Math.abs(dz) == 2) {
                    continue;
                }
                level.setBlock(base.offset(dx, -1, dz), Blocks.COBBLESTONE.defaultBlockState(), 3);
            }
        }
        for (int y = 0; y <= 3; y++) {
            level.setBlock(base.offset(0, y, 0), ModBlocks.CRACKED_RELAY_CASING.get().defaultBlockState(), 3);
        }
        level.setBlock(base.offset(0, 4, 0), ModBlocks.RELAY_BEACON.get().defaultBlockState(), 3);
        level.setBlock(base.offset(1, 0, 0), ModBlocks.DORMANT_RELAY_CORE.get().defaultBlockState(), 3);
        level.setBlock(base.offset(-1, 0, 0), ModBlocks.SCRAP_DEPOSIT.get().defaultBlockState(), 3);
        level.setBlock(base.offset(0, 0, 1), Blocks.CHEST.defaultBlockState(), 3);
        if (level.getBlockEntity(base.offset(0, 0, 1)) instanceof Container container) {
            container.setItem(0, new ItemStack(ModItems.RELAY_FRAGMENT.get(), 2 + random.nextInt(2)));
            container.setItem(1, new ItemStack(ModItems.CIRCUIT_PLATE.get(), 1 + random.nextInt(2)));
            container.setItem(2, new ItemStack(ModItems.WIRE_BUNDLE.get(), 3 + random.nextInt(4)));
            container.setItem(3, new ItemStack(ModItems.BROKEN_CIRCUIT.get(), 2 + random.nextInt(3)));
            container.setItem(4, new ItemStack(ModItems.SIGNAL_SHARD.get(), 1));
        }
    }
}
