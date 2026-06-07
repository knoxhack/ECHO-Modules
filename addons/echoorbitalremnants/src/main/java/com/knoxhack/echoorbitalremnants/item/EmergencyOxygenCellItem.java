package com.knoxhack.echoorbitalremnants.item;

import com.knoxhack.echoorbitalremnants.suit.SuitState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class EmergencyOxygenCellItem extends Item {
    public EmergencyOxygenCellItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            SuitState state = SuitState.get(player);
            state.useEmergencyOxygen();
            state.save(player);
            if (!player.hasInfiniteMaterials()) {
                ItemStack stack = player.getItemInHand(hand);
                stack.shrink(1);
            }
            player.sendSystemMessage(Component.literal("ECHO-7 // Emergency oxygen cell vented into suit reserve."));
        }
        return InteractionResult.SUCCESS_SERVER;
    }
}
