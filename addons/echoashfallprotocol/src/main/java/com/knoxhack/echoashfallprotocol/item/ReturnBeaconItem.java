package com.knoxhack.echoashfallprotocol.item;

import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResult;
import com.knoxhack.echoashfallprotocol.event.AshfallAdapterCoreLateRuntime;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * Reusable late-game return utility to the saved Nexus Core anchor.
 */
public class ReturnBeaconItem extends Item {
    public ReturnBeaconItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(level instanceof ServerLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.CONSUME;
        }

        NativeResult result = AshfallAdapterCoreLateRuntime.returnBeaconUsed(serverPlayer, hand);
        if (result.terminalFailure() || result.completedWithoutMutation()) {
            return InteractionResult.FAIL;
        }
        return result.mutated() ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }
}
