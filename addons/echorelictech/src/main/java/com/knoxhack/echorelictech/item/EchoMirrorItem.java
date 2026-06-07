package com.knoxhack.echorelictech.item;

import com.knoxhack.echorelictech.api.RelicTechApi;
import com.knoxhack.echorelictech.api.event.RelicTechEvents;
import com.knoxhack.echorelictech.api.relic.RelicCondition;
import com.knoxhack.echorelictech.api.relic.RelicInstanceData;
import com.knoxhack.echorelictech.config.RelicTechConfig;
import com.knoxhack.echorelictech.registry.ModDataComponents;
import com.knoxhack.echorelictech.server.RelicInstabilityManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

public class EchoMirrorItem extends Item {
    public EchoMirrorItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) return InteractionResult.SUCCESS;

        var data = stack.get(ModDataComponents.RELIC_DATA.get());
        if (data == null) {
            stack.set(ModDataComponents.RELIC_DATA.get(), new RelicInstanceData(
                Identifier.fromNamespaceAndPath("echorelictech", "echo_mirror"),
                RelicCondition.DAMAGED, 0, BlockPos.ZERO, "", 0, false, false, false, false, 0));
            return InteractionResult.SUCCESS;
        }

        if (!data.identified()) {
            serverPlayer.sendSystemMessage(Component.translatable("item.echorelictech.relic.unidentified"));
            return InteractionResult.FAIL;
        }

        int cooldown = getCooldownTicks(stack);
        if (data.cooldownRemaining() > 0) {
            serverPlayer.sendSystemMessage(Component.translatable("item.echorelictech.echo_mirror.cooldown"));
            return InteractionResult.FAIL;
        }

        int chargeCost = getNullChargeCost(stack);
        if (chargeCost > 0 && !RelicTechApi.consumeNullCharge(serverPlayer, chargeCost)) {
            serverPlayer.sendSystemMessage(Component.translatable("item.echorelictech.relic.no_null_charge"));
            return InteractionResult.FAIL;
        }

        var activation = RelicTechApi.activation(stack);
        int duration = activation.durationTicks() > 0 ? activation.durationTicks() : 200;
        int amplifier = Math.max(0, activation.effectAmplifier());
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, duration, 0));
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, Math.max(80, duration / 2), amplifier));

        // Spawn decoy projections
        EchoMirrorDecoyTracker.spawnDecoys(serverPlayer);

        serverPlayer.sendSystemMessage(Component.translatable("item.echorelictech.echo_mirror.activated"));
        RelicInstabilityManager.addInstability(serverPlayer, RelicTechApi.instabilityCost(stack, 12));
        stack.set(ModDataComponents.RELIC_DATA.get(), data.withCooldown(cooldown));
        RelicTechEvents.fireUse(serverPlayer, Identifier.fromNamespaceAndPath("echorelictech", "echo_mirror"), stack);
        RelicTechApi.tryTriggerFailure(serverPlayer, stack, new com.knoxhack.echorelictech.api.relic.RelicUseContext(level, player, stack, player.blockPosition(), false));
        return InteractionResult.SUCCESS;
    }

    private int getCooldownTicks(ItemStack stack) {
        return RelicTechApi.cooldownTicks(stack, RelicTechConfig.ECHO_MIRROR_COOLDOWN_TICKS.get());
    }

    private int getNullChargeCost(ItemStack stack) {
        int cost = RelicTechApi.nullChargeCost(stack);
        return cost > 0 ? cost : 1;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.echorelictech.echo_mirror.description"));
        tooltip.accept(Component.translatable("relictech.tier.label", Component.translatable("relictech.tier.forbidden")));
        tooltip.accept(Component.translatable("item.echorelictech.echo_mirror.risk"));
    }
}
