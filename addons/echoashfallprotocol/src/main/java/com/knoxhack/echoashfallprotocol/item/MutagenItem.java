package com.knoxhack.echoashfallprotocol.item;

import com.knoxhack.echo.adaptercore.EchoNativeRuntimeHost.NativeResult;
import com.knoxhack.echoashfallprotocol.event.AshfallAdapterCoreHazardRuntime;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Consumer;

/**
 * Mutagen Vial — A dangerous consumable yielding a permanent genetic buff but inflicting sickness.
 */
public class MutagenItem extends Item {

    public MutagenItem(Properties properties) {
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
        NativeResult result = AshfallAdapterCoreHazardRuntime.mutagenUsed(serverPlayer, stack, hand);
        if (result.terminalFailure()) {
            return InteractionResult.FAIL;
        }
        return result.mutated() ? InteractionResult.CONSUME : InteractionResult.SUCCESS;
    }
}
