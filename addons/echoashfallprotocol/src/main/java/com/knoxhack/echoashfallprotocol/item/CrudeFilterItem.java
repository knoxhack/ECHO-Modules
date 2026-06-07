package com.knoxhack.echoashfallprotocol.item;

import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResult;
import com.knoxhack.echoashfallprotocol.event.AshfallAdapterCoreEarlyEventRuntime;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/**
 * Crude Filter - handheld filter for converting dirty water to filtered water.
 * Has 4 uses before breaking.
 */
public class CrudeFilterItem extends Item {

    private static final int MAX_USES = 4;

    public CrudeFilterItem(Properties properties) {
        super(properties.durability(MAX_USES));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack filterStack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.FAIL;
        }

        NativeResult result = AshfallAdapterCoreEarlyEventRuntime.crudeFilterUsed(serverPlayer, filterStack, hand);
        if (result.mutated()) {
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.FAIL;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag tooltipFlag) {
        int remainingUses = stack.getMaxDamage() - stack.getDamageValue();
        tooltip.accept(Component.translatable("tooltip.EchoAshfallProtocol.crude_filter.desc", remainingUses));
    }
}
