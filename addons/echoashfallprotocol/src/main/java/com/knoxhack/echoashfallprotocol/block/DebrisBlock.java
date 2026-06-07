package com.knoxhack.echoashfallprotocol.block;

import com.knoxhack.echoashfallprotocol.registry.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Debris Block — breakable for random scrap drops.
 * Core scavenging mechanic in the early game loop.
 */
public class DebrisBlock extends Block {
    private static final Random RANDOM = new Random();

    public DebrisBlock(Properties properties) {
        super(properties);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        List<ItemStack> drops = new ArrayList<>();

        // Random scrap drops (1-3 items)
        int dropCount = 1 + RANDOM.nextInt(3);
        for (int i = 0; i < dropCount; i++) {
            int roll = RANDOM.nextInt(100);
            if (roll < 35) {
                drops.add(new ItemStack(ModItems.SCRAP_METAL.get(), 1 + RANDOM.nextInt(2)));
            } else if (roll < 55) {
                drops.add(new ItemStack(ModItems.SCRAP_WIRE.get(), 1 + RANDOM.nextInt(2)));
            } else if (roll < 70) {
                drops.add(new ItemStack(ModItems.SCRAP_PLASTIC.get(), 1));
            } else if (roll < 82) {
                drops.add(new ItemStack(ModItems.SCRAP_CIRCUIT.get(), 1));
            } else if (roll < 90) {
                drops.add(new ItemStack(ModItems.ASH.get(), 1 + RANDOM.nextInt(3)));
            } else {
                // Rare drops
                drops.add(new ItemStack(ModItems.ENERGY_CELL.get(), 1));
            }
        }
        if (RANDOM.nextFloat() < 0.04F) {
            drops.add(new ItemStack(ModItems.FILTER_CARTRIDGE_BASIC.get(), 1));
        }
        return drops;
    }
}
