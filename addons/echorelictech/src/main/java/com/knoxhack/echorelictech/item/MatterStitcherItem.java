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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

public class MatterStitcherItem extends Item {
    public MatterStitcherItem(Properties props) {
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
                Identifier.fromNamespaceAndPath("echorelictech", "matter_stitcher"),
                RelicCondition.DAMAGED, 0, BlockPos.ZERO, "", 0, false, false, false, false, 0));
            return InteractionResult.SUCCESS;
        }

        if (!data.identified()) {
            serverPlayer.sendSystemMessage(Component.translatable("item.echorelictech.relic.unidentified"));
            return InteractionResult.FAIL;
        }

        int cooldown = getCooldownTicks(stack);
        if (data.cooldownRemaining() > 0) {
            serverPlayer.sendSystemMessage(Component.translatable("item.echorelictech.matter_stitcher.cooldown"));
            return InteractionResult.FAIL;
        }

        int chargeCost = getNullChargeCost(stack);
        if (chargeCost > 0 && !RelicTechApi.consumeNullCharge(serverPlayer, chargeCost)) {
            serverPlayer.sendSystemMessage(Component.translatable("item.echorelictech.relic.no_null_charge"));
            return InteractionResult.FAIL;
        }

        var activation = RelicTechApi.activation(stack);
        int armorRepairAmount = activation.armorRepairAmount() > 0 ? activation.armorRepairAmount() : 20;
        int healAmount = activation.healAmount() > 0 ? activation.healAmount() : 4;
        if (player.isShiftKeyDown()) {
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                ItemStack armor = player.getItemBySlot(slot);
                if (!armor.isEmpty() && armor.isDamaged()) {
                    armor.setDamageValue(Math.max(0, armor.getDamageValue() - armorRepairAmount));
                }
            }
            serverPlayer.sendSystemMessage(Component.translatable("item.echorelictech.matter_stitcher.armor"));
        } else {
            player.heal(healAmount);
            serverPlayer.sendSystemMessage(Component.translatable("item.echorelictech.matter_stitcher.heal"));
        }

        RelicInstabilityManager.addInstability(serverPlayer, RelicTechApi.instabilityCost(stack, 8));
        if (RelicInstabilityManager.getInstabilityLevel(serverPlayer) >= 3) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 300, 0));
        }
        stack.set(ModDataComponents.RELIC_DATA.get(), data.withCooldown(cooldown));
        RelicTechEvents.fireUse(serverPlayer, Identifier.fromNamespaceAndPath("echorelictech", "matter_stitcher"), stack);
        RelicTechApi.tryTriggerFailure(serverPlayer, stack, new com.knoxhack.echorelictech.api.relic.RelicUseContext(level, player, stack, player.blockPosition(), player.isShiftKeyDown()));
        return InteractionResult.SUCCESS;
    }

    private int getCooldownTicks(ItemStack stack) {
        return RelicTechApi.cooldownTicks(stack, RelicTechConfig.MATTER_STITCHER_COOLDOWN_TICKS.get());
    }

    private int getNullChargeCost(ItemStack stack) {
        int cost = RelicTechApi.nullChargeCost(stack);
        return cost > 0 ? cost : 1;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.echorelictech.matter_stitcher.description"));
        tooltip.accept(Component.translatable("item.echorelictech.matter_stitcher.cost"));
        tooltip.accept(Component.translatable("item.echorelictech.matter_stitcher.risk"));
    }
}
