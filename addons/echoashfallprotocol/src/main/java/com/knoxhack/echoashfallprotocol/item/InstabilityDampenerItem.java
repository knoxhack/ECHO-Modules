package com.knoxhack.echoashfallprotocol.item;

import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResult;
import com.knoxhack.echoashfallprotocol.event.AshfallAdapterCoreLateRuntime;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Consumable field stabilizer for the overworld Nexus instability meter.
 */
public class InstabilityDampenerItem extends Item {
    private static final int INSTABILITY_REDUCTION = 20;

    public InstabilityDampenerItem(Properties properties) {
        super(properties);
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
        NativeResult result = AshfallAdapterCoreLateRuntime.instabilityDampenerUsed(
                serverPlayer,
                stack,
                hand,
                INSTABILITY_REDUCTION);
        if (result.terminalFailure()) {
            return InteractionResult.FAIL;
        }
        return result.mutated() ? InteractionResult.CONSUME : InteractionResult.FAIL;
    }
}
