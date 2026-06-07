package com.knoxhack.echorecovery.grave;

import com.knoxhack.echorecovery.api.RecoveryItemRuleResult;
import com.knoxhack.echorecovery.api.RecoveryRuleProvider;
import com.knoxhack.echorecovery.content.RecoveryContent;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class RecoveryRuleEngine {
    private static final List<RecoveryRuleProvider> PROVIDERS = new CopyOnWriteArrayList<>();

    private RecoveryRuleEngine() {
    }

    public static void registerProvider(RecoveryRuleProvider provider) {
        if (provider != null && !PROVIDERS.contains(provider)) {
            PROVIDERS.add(provider);
        }
    }

    public static RecoveryItemRuleResult evaluate(ServerPlayer player, ItemStack stack, String deathCause) {
        if (stack == null || stack.isEmpty()) {
            return RecoveryItemRuleResult.NO_GRAVE;
        }
        for (RecoveryRuleProvider provider : PROVIDERS) {
            Optional<RecoveryItemRuleResult> result = provider.evaluate(player, stack, deathCause);
            if (result.isPresent()) {
                return result.get();
            }
        }
        Optional<RecoveryItemRuleResult> dataRule = RecoveryContent.evaluateDataRules(stack);
        if (dataRule.isPresent()) {
            return dataRule.get();
        }
        if (stack.is(RecoveryTags.SOULBOUND)) {
            return RecoveryItemRuleResult.SOULBOUND;
        }
        if (stack.is(RecoveryTags.PROTECTED)) {
            return RecoveryItemRuleResult.PROTECTED;
        }
        if (stack.is(RecoveryTags.NO_GRAVE)) {
            return RecoveryItemRuleResult.NO_GRAVE;
        }
        if (stack.is(RecoveryTags.DESTROY_ON_DEATH)) {
            return RecoveryItemRuleResult.DESTROY_ON_DEATH;
        }
        if (stack.is(RecoveryTags.DROP_ON_DEATH)) {
            return RecoveryItemRuleResult.DROP_ON_DEATH;
        }
        if (stack.is(RecoveryTags.ALWAYS_GRAVE)) {
            return RecoveryItemRuleResult.ALWAYS_GRAVE;
        }
        return RecoveryItemRuleResult.ALWAYS_GRAVE;
    }
}
