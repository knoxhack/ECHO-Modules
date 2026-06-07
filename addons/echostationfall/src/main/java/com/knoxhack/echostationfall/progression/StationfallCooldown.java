package com.knoxhack.echostationfall.progression;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public final class StationfallCooldown {
    public static final String ROOT = "echostationfall_cooldowns";

    private StationfallCooldown() {
    }

    public static boolean ready(Player player, String key, int cooldownTicks) {
        CompoundTag cooldowns = player.getPersistentData().getCompoundOrEmpty(ROOT).copy();
        int now = player.tickCount;
        int last = cooldowns.getIntOr(key, Integer.MIN_VALUE);
        if (last > 0 && now - last < cooldownTicks) {
            return false;
        }
        cooldowns.putInt(key, now);
        player.getPersistentData().put(ROOT, cooldowns);
        return true;
    }

    public static void copy(Player from, Player to) {
        to.getPersistentData().put(ROOT, from.getPersistentData().getCompoundOrEmpty(ROOT).copy());
    }
}
