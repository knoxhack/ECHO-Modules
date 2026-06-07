package com.knoxhack.echospellcore.item;

import com.knoxhack.echospellcore.api.SpellCoreApi;
import com.knoxhack.echospellcore.menu.SpellDeckMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class SpellDeckItem extends Item {
    public SpellDeckItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        SpellCoreApi.initializeDeck(stack);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (containerId, inventory, p) -> new SpellDeckMenu(containerId, inventory),
                    Component.translatable("screen.echospellcore.spell_deck")));
        }
        return InteractionResult.SUCCESS_SERVER;
    }
}
