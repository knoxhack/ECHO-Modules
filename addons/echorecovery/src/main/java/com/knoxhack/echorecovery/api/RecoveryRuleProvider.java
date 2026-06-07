package com.knoxhack.echorecovery.api;

import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

@FunctionalInterface
public interface RecoveryRuleProvider {
    Optional<RecoveryItemRuleResult> evaluate(ServerPlayer player, ItemStack stack, String deathCause);
}
