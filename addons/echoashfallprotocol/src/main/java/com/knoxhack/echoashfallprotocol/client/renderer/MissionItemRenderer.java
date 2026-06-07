package com.knoxhack.echoashfallprotocol.client.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

/**
 * Utility class for rendering ItemStack icons in the mission UI.
 * Delegates to vanilla GuiGraphicsExtractor item rendering for accurate textures, models, and decorations.
 */
public class MissionItemRenderer {

    /** Render an item stack at (x, y) using vanilla item rendering. */
    public static void renderItem(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y) {
        if (stack.isEmpty()) return;
        graphics.item(stack, x, y);
        // Decorations (durability bar, cooldown overlay, stack count) — pass null countText so vanilla
        // shows count only when > 1, matching inventory behavior.
        graphics.itemDecorations(Minecraft.getInstance().font, stack, x, y, null);
    }

    /**
     * Render an item with progress overlay — used for required items in checklists.
     * Suppresses the vanilla stack-count badge and shows "have/need" instead, since the
     * stack's own count is the requirement, not the player's possessed amount.
     */
    public static void renderItemWithProgress(GuiGraphicsExtractor graphics, ItemStack stack,
                                              int x, int y, int have, int need) {
        if (stack.isEmpty()) return;
        graphics.item(stack, x, y);

        // Render durability/cooldown via vanilla, but pass an empty count string so the
        // vanilla "x N" overlay is suppressed (the parent screen renders its own progress UI).
        graphics.itemDecorations(Minecraft.getInstance().font, stack, x, y, "");

        // Subtle outline tint matching completion state — sits inside the slot frame the screen draws.
        int outline = (have >= need) ? 0x6055FF55 : (have > 0 ? 0x60FFC107 : 0x60FF5555);
        graphics.fill(x, y, x + 16, y + 1, outline);
        graphics.fill(x, y + 15, x + 16, y + 16, outline);
        graphics.fill(x, y, x + 1, y + 16, outline);
        graphics.fill(x + 15, y, x + 16, y + 16, outline);
    }

    /** Render a row of items with horizontal spacing. */
    public static void renderItemRow(GuiGraphicsExtractor graphics, Iterable<ItemStack> items,
                                     int x, int y, int spacing) {
        int currentX = x;
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                renderItem(graphics, stack, currentX, y);
                currentX += spacing;
            }
        }
    }

    public static String getItemDisplayName(ItemStack stack) {
        if (stack.isEmpty()) return "Empty";
        return stack.getHoverName().getString();
    }

    public static String formatQuantity(int count) {
        return "x" + count;
    }

    public static boolean isMouseOverItem(int mouseX, int mouseY, int itemX, int itemY) {
        return mouseX >= itemX && mouseX < itemX + 16 &&
               mouseY >= itemY && mouseY < itemY + 16;
    }
}
