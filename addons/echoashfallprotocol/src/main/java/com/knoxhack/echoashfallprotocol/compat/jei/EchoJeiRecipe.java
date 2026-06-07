package com.knoxhack.echoashfallprotocol.compat.jei;

import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record EchoJeiRecipe(
        Identifier id,
        IRecipeType<EchoJeiRecipe> type,
        List<EchoJeiSlot> inputs,
        List<EchoJeiSlot> outputs,
        List<Component> notes,
        int processTicks
) {
    public record EchoJeiSlot(int x, int y, List<ItemStack> stacks) {
        public static EchoJeiSlot of(int x, int y, ItemStack stack) {
            return new EchoJeiSlot(x, y, List.of(stack));
        }

        public static EchoJeiSlot of(int x, int y, List<ItemStack> stacks) {
            return new EchoJeiSlot(x, y, List.copyOf(stacks));
        }
    }
}
