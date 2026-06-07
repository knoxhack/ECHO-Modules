package com.knoxhack.echorelictech.api.relic;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public record RelicUseContext(Level level, Player player, ItemStack stack, BlockPos pos, boolean sneaking) {
}
