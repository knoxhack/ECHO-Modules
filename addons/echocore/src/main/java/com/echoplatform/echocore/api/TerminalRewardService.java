package com.echoplatform.echocore.api;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface TerminalRewardService {
    default boolean storeRewards(ServerPlayer player, String missionId, List<ItemStack> rewards) {
        return false;
    }

    default boolean claimRewards(ServerPlayer player) {
        return false;
    }

    default int pendingRewardCount(Player player) {
        return 0;
    }
}
