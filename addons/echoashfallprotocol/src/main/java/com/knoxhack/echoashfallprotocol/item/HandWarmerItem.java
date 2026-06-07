package com.knoxhack.echoashfallprotocol.item;

import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResult;
import com.knoxhack.echoashfallprotocol.event.AshfallAdapterCoreEarlyEventRuntime;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Consumable emergency heat source for cryogenic exploration.
 */
public class HandWarmerItem extends Item {
    private static final int WARMTH_DELTA = 25;

    public HandWarmerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        NativeResult result = AshfallAdapterCoreEarlyEventRuntime.handWarmerUsed(serverPlayer, stack, hand, WARMTH_DELTA);
        if (result.terminalFailure()) {
            return InteractionResult.FAIL;
        }

        return result.mutated() ? InteractionResult.CONSUME : InteractionResult.SUCCESS;
    }
}
