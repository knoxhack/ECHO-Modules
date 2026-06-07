package com.knoxhack.echoashfallprotocol.client.screen;

import com.knoxhack.echoashfallprotocol.block.menu.ScrapPressMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class ScrapPressScreen extends MachineScreen<ScrapPressMenu> {

    public ScrapPressScreen(ScrapPressMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected String getMachineTitle() {
        return "Scrap Press";
    }

    @Override
    protected boolean isMachineActive() {
        return menu.hasPower() && menu.isProcessing() && !menu.isJammed();
    }

    @Override
    protected void drawMachineContent(GuiGraphicsExtractor g, int x, int y) {
        drawMachineSlot(g, 0, COL_SLOT_FUEL);
        drawMachineSlot(g, 1, COL_SLOT_OUTPUT);
        drawSlotLabel(g, 0, "SCRAP", -12, COL_ORANGE);
        drawSlotLabel(g, 1, "BLOCK", -12, COL_GREEN);

        float progress = menu.getProgressPercent();
        drawStandardProgress(g, progress, menu.hasPower() && !menu.isJammed(), COL_ACCENT);
        drawRightPowerMeter(g, menu.hasPower());
        drawStandardWear(g, menu.getWearPercent(), menu.getWearStatus(), menu.getWearColor());
        drawFeStatus(g, x + 226, y + 174, menu.getEnergy(), menu.getMaxEnergy());
        if (menu.isJammed()) {
            drawStandardStatus(g, "! CLEAR JAM", COL_RED);
        } else if (!menu.hasPower()) {
            drawStandardStatus(g, "! NO POWER", COL_YELLOW);
        }
    }
}
