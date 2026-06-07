package com.knoxhack.echoashfallprotocol.client;

import com.knoxhack.echoashfallprotocol.Config;
import com.knoxhack.echoashfallprotocol.block.entity.OreGrinderBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Adds lightweight discoverability for all Substrate Grinder inputs.
 */
public final class SubstrateTooltipHandler {
    private SubstrateTooltipHandler() {
    }

    public static void onItemTooltip(Object event) {
        ClientTooltipEventView view = ClientTooltipEventView.from(event);
        if (view == null) {
            return;
        }
        ItemStack stack = view.itemStack();
        OreGrinderBlockEntity.GrinderRecipe recipe = OreGrinderBlockEntity.getSubstrateRecipe(stack);
        if (recipe == null) {
            return;
        }

        List<Component> tooltip = view.tooltip();
        tooltip.add(Component.empty());
        tooltip.add(Component.literal(stack.getItem() instanceof BlockItem ? recipe.categoryLabel() : "Substrate Grinder input")
                .withStyle(ChatFormatting.DARK_AQUA));
        tooltip.add(recipeLine(recipe));
        if (recipe.byproduct() != null) {
            tooltip.add(Component.literal(Math.round(recipe.byproductChance() * 100.0F) + "% side output: ")
                    .withStyle(ChatFormatting.DARK_GRAY)
                    .append(itemName(recipe.byproduct(), recipe.byproductCount()).withStyle(ChatFormatting.GRAY)));
        }
        if (Config.VERBOSE_TOOLTIPS.get()) {
            tooltip.add(Component.literal(recipe.handlingHint())
                    .withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.literal(recipe.powerPerOperation() + " FE / " + recipe.processTime() + " ticks")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private static Component recipeLine(OreGrinderBlockEntity.GrinderRecipe recipe) {
        MutableComponent line = Component.literal("Grinder: ").withStyle(ChatFormatting.GRAY);
        line.append(Component.literal(recipe.inputCount() + "x").withStyle(ChatFormatting.YELLOW));
        line.append(Component.literal(" -> ").withStyle(ChatFormatting.DARK_GRAY));
        line.append(itemName(recipe.output(), recipe.outputCount()).withStyle(ChatFormatting.WHITE));
        return line;
    }

    private static MutableComponent itemName(Item item, int count) {
        return Component.literal(count + "x " + new ItemStack(item).getHoverName().getString());
    }
}
