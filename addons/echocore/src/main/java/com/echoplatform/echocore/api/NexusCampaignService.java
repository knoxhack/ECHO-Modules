package com.echoplatform.echocore.api;

import java.util.List;
import net.minecraft.world.entity.player.Player;

public interface NexusCampaignService {
    default String pathId(Player player) {
        return "";
    }

    default int instability(Player player) {
        return 0;
    }

    default boolean isWarfrontComplete(Player player) {
        return false;
    }

    default boolean isFinalProtocolComplete(Player player) {
        return false;
    }

    default List<String> relaySummary(Player player) {
        return List.of();
    }

    default boolean isFinalBossDefeated(Player player) {
        return false;
    }

    default String statusLine(Player player) {
        return "";
    }
}
