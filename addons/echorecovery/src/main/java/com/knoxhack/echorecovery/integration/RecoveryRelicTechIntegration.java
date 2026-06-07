package com.knoxhack.echorecovery.integration;

import com.knoxhack.echorecovery.EchoRecovery;
import com.knoxhack.echorecovery.api.RecoveryItemRuleResult;
import com.knoxhack.echorecovery.grave.RecoveryRuleEngine;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;

public final class RecoveryRelicTechIntegration {
    private RecoveryRelicTechIntegration() {}
    public static void registerCommon() {
        RecoveryRuleEngine.registerProvider((player, stack, deathCause) -> {
            var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            return "echorelictech".equals(id.getNamespace()) ? Optional.of(RecoveryItemRuleResult.SOULBOUND) : Optional.empty();
        });
        EchoRecovery.LOGGER.info("Recovery RelicTech soulbound rules registered.");
    }
}
