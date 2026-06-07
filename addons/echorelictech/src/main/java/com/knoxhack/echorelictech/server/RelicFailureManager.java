package com.knoxhack.echorelictech.server;

import com.knoxhack.echorelictech.api.relic.RelicUseContext;
import com.knoxhack.echorelictech.config.RelicTechConfig;
import com.knoxhack.echorelictech.data.RelicDefinitionLoader;
import com.knoxhack.echorelictech.data.RelicFailureLoader;
import com.knoxhack.echorelictech.registry.ModDataComponents;
import com.knoxhack.echorelictech.api.RelicTechApi;
import com.knoxhack.echorelictech.api.relic.RelicCondition;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class RelicFailureManager {
    private RelicFailureManager() {}

    public static boolean tryTrigger(ServerPlayer player, ItemStack relic, RelicUseContext context) {
        if (!RelicTechConfig.ENABLE_RELIC_FAILURES.get()) return false;
        float baseChance = RelicTechApi.getRelicCondition(relic).failureMultiplier();
        int level = RelicInstabilityManager.getInstabilityLevel(player);
        float bonus = switch (level) {
            case 3 -> RelicTechConfig.LEVEL3_INSTABILITY_FAILURE_BONUS.get().floatValue();
            case 4 -> RelicTechConfig.LEVEL4_INSTABILITY_FAILURE_BONUS.get().floatValue();
            case 5 -> RelicTechConfig.LEVEL5_INSTABILITY_FAILURE_BONUS.get().floatValue();
            default -> 0.0f;
        };
        float chance = baseChance + bonus;
        if (player.getRandom().nextFloat() >= chance) return false;

        Identifier relicId = RelicTechApi.getRelicId(relic);
        var definition = RelicDefinitionLoader.get(relicId);
        Identifier tableId = definition != null ? definition.failureTable() : relicId;
        var table = RelicFailureLoader.get(tableId);
        if (table == null) table = RelicFailureLoader.get(Identifier.fromNamespaceAndPath("echorelictech", "generic_relic"));
        if (table == null) {
            fallbackFailure(player, relic);
            return true;
        }
        var entry = table.roll(player.getRandom());
        if (entry == null) {
            fallbackFailure(player, relic);
            return true;
        }
        player.sendSystemMessage(Component.literal(entry.message()));
        for (var effect : entry.effects()) {
            applyEffect(player, relic, effect);
        }
        com.knoxhack.echorelictech.api.event.RelicTechEvents.fireFailure(player, relicId, entry.severity().getSerializedName());
        com.knoxhack.echorelictech.integration.soundcore.RelicTechSoundCoreIntegration.playRelicMalfunction(player);
        var inst = RelicInstabilitySavedData.get((ServerLevel) player.level()).get(player.getUUID());
        inst.recentFailures++;
        RelicInstabilitySavedData.get((ServerLevel) player.level()).set(player.getUUID(), inst);
        return true;
    }

    private static void applyEffect(ServerPlayer player, ItemStack relic, com.knoxhack.echorelictech.api.failure.FailureEffectType effect) {
        switch (effect) {
            case COOLDOWN_MULTIPLY -> {
                var data = relic.get(ModDataComponents.RELIC_DATA.get());
                if (data != null) relic.set(ModDataComponents.RELIC_DATA.get(), data.withCooldown(data.cooldownRemaining() * 2));
            }
            case DAMAGE_ITEM -> relic.hurtAndBreak(8, player, net.minecraft.world.InteractionHand.MAIN_HAND);
            case ADD_DEBUFF -> player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NAUSEA, 200, 0));
            case TELEPORT_OFFSET -> {
                double dx = player.getRandom().nextInt(8) - 4;
                double dz = player.getRandom().nextInt(8) - 4;
                player.teleportTo(player.getX() + dx, player.getY(), player.getZ() + dz);
            }
            case HOSTILE_SIGNAL -> player.sendSystemMessage(Component.literal("Nexus echo followed the recall path."));
            case ADD_INSTABILITY -> RelicInstabilityManager.addInstability(player, 10);
            case DRAIN_CHARGE -> {
                var data = relic.get(ModDataComponents.RELIC_DATA.get());
                if (data != null) relic.set(ModDataComponents.RELIC_DATA.get(), data.withCharge(0));
            }
            case FIZZLE -> player.sendSystemMessage(Component.literal("The relic fizzled. Nothing happened."));
            case FORCE_COOLDOWN -> {
                var data = relic.get(ModDataComponents.RELIC_DATA.get());
                if (data != null) relic.set(ModDataComponents.RELIC_DATA.get(), data.withCooldown(6000));
            }
        }
    }

    private static void fallbackFailure(ServerPlayer player, ItemStack relic) {
        player.sendSystemMessage(Component.literal("Relic malfunction detected."));
        relic.hurtAndBreak(4, player, net.minecraft.world.InteractionHand.MAIN_HAND);
    }
}
