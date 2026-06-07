package com.knoxhack.echoashfallprotocol.item;

import com.knoxhack.echoashfallprotocol.event.AshfallAdapterCoreHazardRuntime;
import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;

/**
 * RadAway — Medical aid to flush radiation buildup from the system.
 */
public class RadAwayItem extends Item {

    public RadAwayItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            NativeResult result = AshfallAdapterCoreHazardRuntime.radAwayUsed(serverPlayer, stack, hand);
            if (result.terminalFailure() || result.completedWithoutMutation()) {
                return InteractionResult.FAIL;
            }
        }
        return InteractionResult.SUCCESS;
    }
}
