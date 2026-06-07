package com.knoxhack.echoashfallprotocol.client.screen;

import com.knoxhack.echoashfallprotocol.block.menu.OreGrinderMenu;
import com.knoxhack.echoashfallprotocol.block.entity.OreGrinderBlockEntity;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class OreGrinderScreen extends MachineScreen<OreGrinderMenu> {

    public OreGrinderScreen(OreGrinderMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected String getMachineTitle() {
        return "Substrate Grinder";
    }

    @Override
    protected boolean isMachineActive() {
        return menu.hasPower() && !menu.isJammed() && menu.getProgressPercent() > 0;
    }

    @Override
    protected void drawMachineContent(GuiGraphicsExtractor g, int x, int y) {
        boolean hasPower = menu.hasPower();
        float progress = menu.getProgressPercent();
        boolean hasInput = menu.slots.get(0).hasItem() || menu.slots.get(1).hasItem();
        boolean outputBlocked = isSlotFull(2) || isSlotFull(3);
        OreGrinderBlockEntity.GrinderRecipe previewRecipe = menu.getPreviewRecipe();

        drawMachineSlot(g, 0, COL_SLOT_FUEL);
        drawMachineSlot(g, 1, COL_SLOT_FUEL);
        drawMachineSlot(g, 2, COL_SLOT_OUTPUT);
        drawMachineSlot(g, 3, 0xFF183048);
        drawSlotLabel(g, 0, "INPUT", -12, COL_ORANGE);
        drawSlotLabel(g, 2, "OUT", -12, COL_GREEN);
        drawSlotLabel(g, 3, "SIDE", 20, COL_ACCENT);

        drawStandardProgress(g, progress, hasPower && !menu.isJammed(), COL_ACCENT);
        drawFeReadout(g, x, y, menu.getEnergy(), menu.getMaxEnergy());

        drawRightPowerMeter(g, hasPower);

        drawRecipePreview(g, previewRecipe, x + 118, y + 142);
        drawStandardWear(g, menu.getWearPercent(), menu.getWearStatus(), menu.getWearColor());
        if (menu.isJammed()) {
            drawStandardStatus(g, "! CLEAR JAM", COL_RED);
        } else if (!hasInput) {
            drawStandardStatus(g, "> ADD SUBSTRATE", COL_DIM);
        } else if (previewRecipe != null && !menu.hasRecipeReady()) {
            drawStandardStatus(g, "! NEED " + previewRecipe.inputCount() + "x", COL_YELLOW);
        } else if (outputBlocked) {
            drawStandardStatus(g, "! OUTPUT FULL", COL_YELLOW);
        } else if (!hasPower) {
            drawStandardStatus(g, "! NO POWER", COL_YELLOW);
        } else if (progress > 0.0f) {
            drawStandardStatus(g, "* GRINDING", COL_GREEN);
        } else {
            drawStandardStatus(g, "* READY", COL_ACCENT);
        }
    }

    private void drawRecipePreview(GuiGraphicsExtractor g, OreGrinderBlockEntity.GrinderRecipe recipe, int x, int y) {
        if (recipe == null) {
            g.text(font, "NO RECIPE", x, y, COL_DIM, false);
            return;
        }

        String output = new ItemStack(recipe.output()).getHoverName().getString();
        String text = recipe.inputCount() + "x > " + recipe.outputCount() + "x " + output;
        if (recipe.byproduct() != null) {
            text += " +" + Math.round(recipe.byproductChance() * 100.0F) + "%";
        }
        int color = menu.hasRecipeReady() ? COL_TEXT : COL_YELLOW;
        g.text(font, fit(text, 178), x, y, color, false);
    }

    private boolean isSlotFull(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= menu.slots.size() || !menu.slots.get(slotIndex).hasItem()) {
            return false;
        }
        var stack = menu.slots.get(slotIndex).getItem();
        return stack.getCount() >= stack.getMaxStackSize();
    }
}
