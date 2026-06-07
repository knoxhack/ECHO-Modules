package com.knoxhack.echoprimecore.item;

import com.knoxhack.echoprimecore.progression.PrimeStarterFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class CrudeScannerItem extends Item {
    public CrudeScannerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            PrimeStarterFlow.useCrudeScanner(serverPlayer);
            player.getItemInHand(hand).hurtAndBreak(1, player, hand);
        }
        return InteractionResult.SUCCESS;
    }
}
