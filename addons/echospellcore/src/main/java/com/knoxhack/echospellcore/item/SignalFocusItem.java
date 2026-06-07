package com.knoxhack.echospellcore.item;

import com.knoxhack.echospellcore.api.SpellCoreApi;
import com.knoxhack.echospellcore.spell.StarterSpell;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

public class SignalFocusItem extends Item {
    public SignalFocusItem(Properties properties) {
        super(properties.stacksTo(1).durability(256));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        if (player.isShiftKeyDown()) {
            ItemStack deck = SpellCoreApi.findDeck(serverPlayer);
            if (!deck.isEmpty()) {
                int nextSlot = Math.floorMod(SpellCoreApi.deckActiveSlot(deck) + 1, SpellCoreApi.LOADOUT_SLOTS);
                SpellCoreApi.applyLoadoutAction(serverPlayer, "select_slot", nextSlot, SpellCoreApi.selectedSpell(stack), "");
            } else {
                SpellCoreApi.cycleSpell(stack);
            }
            var selected = SpellCoreApi.activeSpellId(serverPlayer, stack);
            player.sendSystemMessage(Component.translatable("item.echospellcore.signal_focus.selected",
                    StarterSpell.safe(selected).title()));
            return InteractionResult.SUCCESS_SERVER;
        }
        boolean cast = SpellCoreApi.tryCast(serverPlayer, stack, SpellCoreApi.selectedSpell(stack));
        if (cast) {
            stack.hurtAndBreak(1, player, hand);
        }
        return cast ? InteractionResult.SUCCESS_SERVER : InteractionResult.FAIL;
    }

    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean selected) {
        if (!level.isClientSide() && entity instanceof ServerPlayer player) {
            SpellCoreApi.tickFocus(player, stack, selected);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
            Consumer<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = data(stack);
        StarterSpell spell = StarterSpell.safe(SpellCoreApi.selectedSpell(stack));
        tooltip.accept(Component.translatable("tooltip.echospellcore.signal_focus.selected", spell.title()));
        tooltip.accept(Component.translatable("tooltip.echospellcore.signal_focus.cost",
                (int) spell.cost(), spell.aetherType().serializedName()));
        long cooldown = tag.getLongOr("Cooldown", 0L);
        if (cooldown > 0L) {
            tooltip.accept(Component.translatable("tooltip.echospellcore.signal_focus.cooldown", cooldown));
        }
        String status = tag.getStringOr("Status", "");
        if (!status.isBlank()) {
            tooltip.accept(Component.translatable("tooltip.echospellcore.signal_focus.status", status));
        }
    }

    private static CompoundTag data(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? new CompoundTag() : data.copyTag();
    }
}
