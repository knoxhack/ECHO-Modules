package com.knoxhack.echorelictech.item;

import com.knoxhack.echorelictech.api.RelicTechApi;
import com.knoxhack.echorelictech.api.event.RelicTechEvents;
import com.knoxhack.echorelictech.api.relic.RelicCondition;
import com.knoxhack.echorelictech.api.relic.RelicInstanceData;
import com.knoxhack.echorelictech.config.RelicTechConfig;
import com.knoxhack.echorelictech.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

public class PhaseAnchorItem extends Item {
    public PhaseAnchorItem(Properties props) {
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
                Identifier.fromNamespaceAndPath("echorelictech", "phase_anchor"),
                RelicCondition.DAMAGED, 0, BlockPos.ZERO, "", 0, false, false, false, false, 0));
            return InteractionResult.SUCCESS;
        }

        if (!data.identified()) {
            serverPlayer.sendSystemMessage(Component.translatable("item.echorelictech.relic.unidentified"));
            return InteractionResult.FAIL;
        }

        int cooldown = getCooldownTicks(stack);
        if (data.cooldownRemaining() > 0) {
            serverPlayer.sendSystemMessage(Component.translatable("item.echorelictech.phase_anchor.cooldown"));
            return InteractionResult.FAIL;
        }

        if (player.isShiftKeyDown()) {
            RelicTechApi.bindPhaseAnchor(serverPlayer, stack, serverPlayer.blockPosition());
            var boundData = stack.get(ModDataComponents.RELIC_DATA.get());
            if (boundData != null) {
                stack.set(ModDataComponents.RELIC_DATA.get(), boundData.withCooldown(cooldown));
            }
            RelicTechEvents.fireUse(serverPlayer, Identifier.fromNamespaceAndPath("echorelictech", "phase_anchor"), stack);
            return InteractionResult.SUCCESS;
        }

        if (RelicTechApi.tryUsePhaseAnchor(serverPlayer, stack)) {
            stack.set(ModDataComponents.RELIC_DATA.get(), data.withCooldown(cooldown));
            RelicTechEvents.fireUse(serverPlayer, Identifier.fromNamespaceAndPath("echorelictech", "phase_anchor"), stack);
            RelicTechApi.tryTriggerFailure(serverPlayer, stack, new com.knoxhack.echorelictech.api.relic.RelicUseContext(level, player, stack, player.blockPosition(), false));
        }
        return InteractionResult.SUCCESS;
    }

    private int getCooldownTicks(ItemStack stack) {
        return RelicTechApi.cooldownTicks(stack, RelicTechConfig.PHASE_ANCHOR_COOLDOWN_TICKS.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        var data = stack.get(ModDataComponents.RELIC_DATA.get());
        if (data == null) {
            tooltip.accept(Component.translatable("item.echorelictech.relic.unidentified_hint"));
            return;
        }
        tooltip.accept(Component.translatable("relictech.condition.label", Component.translatable("relictech.condition." + data.condition().name().toLowerCase())));
        tooltip.accept(Component.literal(data.boundPos().equals(BlockPos.ZERO) ? Component.translatable("item.echorelictech.phase_anchor.unbound").getString() : Component.translatable("item.echorelictech.phase_anchor.bound", data.boundPos().toShortString()).getString()));
        tooltip.accept(Component.translatable("item.echorelictech.phase_anchor.instability_cost", RelicTechConfig.PHASE_ANCHOR_INSTABILITY_COST.get()));
        if (data.cooldownRemaining() > 0) tooltip.accept(Component.translatable("item.echorelictech.relic.cooldown_ticks", data.cooldownRemaining()));
        tooltip.accept(Component.translatable("item.echorelictech.phase_anchor.warning"));
    }
}
