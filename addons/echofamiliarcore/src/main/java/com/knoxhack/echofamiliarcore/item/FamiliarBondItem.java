package com.knoxhack.echofamiliarcore.item;

import com.knoxhack.echofamiliarcore.api.FamiliarCoreApi;
import com.knoxhack.echofamiliarcore.menu.FamiliarCommandMenu;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

public class FamiliarBondItem extends Item {
    private final Identifier familiarId;
    private final String tooltipKey;

    public FamiliarBondItem(Properties props, Identifier familiarId, String tooltipKey) {
        super(props.stacksTo(1));
        this.familiarId = familiarId;
        this.tooltipKey = tooltipKey;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer && player.isShiftKeyDown()
                && FamiliarCoreApi.activeFamiliar(player) != null) {
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (containerId, inventory, operator) -> new FamiliarCommandMenu(containerId, inventory, serverPlayer),
                    Component.translatable("screen.echofamiliarcore.familiar_command")));
            return InteractionResult.SUCCESS_SERVER;
        }
        if (player instanceof ServerPlayer serverPlayer && FamiliarCoreApi.bind(serverPlayer, familiarId, "item_bind")) {
            if (!player.getAbilities().instabuild) {
                player.getItemInHand(hand).shrink(1);
            }
            return InteractionResult.SUCCESS_SERVER;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
            Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable(tooltipKey));
    }
}
