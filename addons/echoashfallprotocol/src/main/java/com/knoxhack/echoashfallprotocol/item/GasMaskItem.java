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
 * Gas Mask — equippable in the head slot.
 * Provides atmospheric protection inside toxic hazard zones.
 */
public class GasMaskItem extends Item {

    public GasMaskItem(Properties properties) {
        super(properties.durability(500).stacksTo(1));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        ItemStack stack = player.getItemInHand(hand);
        NativeResult result = AshfallAdapterCoreEarlyEventRuntime.gasMaskUsed(serverPlayer, stack, hand);
        if (result.terminalFailure()) {
            return InteractionResult.FAIL;
        }
        return result.mutated() ? InteractionResult.CONSUME : InteractionResult.SUCCESS;
    }
}
