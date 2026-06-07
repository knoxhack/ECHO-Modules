package com.knoxhack.echoindex.client;

import java.util.Optional;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

record IndexBlockPreviewTooltipData(ItemStack stack, Optional<TooltipComponent> vanillaImage, int previewSize)
        implements TooltipComponent {
    IndexBlockPreviewTooltipData {
        stack = stack == null ? ItemStack.EMPTY : stack.copy();
        stack.setCount(1);
        vanillaImage = vanillaImage == null ? Optional.empty() : vanillaImage;
        previewSize = Math.max(32, Math.min(64, previewSize));
    }
}
