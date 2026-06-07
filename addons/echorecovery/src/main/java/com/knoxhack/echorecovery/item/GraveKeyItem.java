package com.knoxhack.echorecovery.item;

import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;

public class GraveKeyItem extends Item {
    public GraveKeyItem(Properties properties) {
        super(properties);
    }

    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag tooltipFlag) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data != null) {
            String graveId = data.copyTag().getStringOr("GraveId", "");
            String dim = data.copyTag().getStringOr("Dimension", "");
            if (!graveId.isBlank()) {
                tooltip.accept(Component.literal("Bound to: " + graveId.substring(0, Math.min(8, graveId.length()))));
            }
            if (!dim.isBlank()) {
                tooltip.accept(Component.literal("Dimension: " + dim));
            }
        }
    }

    public static void bindToGrave(ItemStack stack, UUID graveId, net.minecraft.core.BlockPos pos, Identifier dimension) {
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        tag.putString("GraveId", graveId.toString());
        tag.putLong("Pos", pos.asLong());
        tag.putString("Dimension", dimension.toString());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static UUID getGraveId(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data != null) {
            String id = data.copyTag().getStringOr("GraveId", "");
            if (!id.isBlank()) {
                try {
                    return UUID.fromString(id);
                } catch (IllegalArgumentException ignored) {}
            }
        }
        return null;
    }
}
