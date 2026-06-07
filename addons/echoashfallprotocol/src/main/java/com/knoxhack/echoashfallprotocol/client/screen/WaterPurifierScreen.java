package com.knoxhack.echoashfallprotocol.client.screen;

import com.knoxhack.echoashfallprotocol.block.menu.WaterPurifierMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class WaterPurifierScreen extends MachineScreen<WaterPurifierMenu> {

    public WaterPurifierScreen(WaterPurifierMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected String getMachineTitle() {
        return "Water Purifier";
    }

    @Override
    protected boolean isMachineActive() {
        return menu.hasPower() && menu.getProgress() > 0 && !menu.isJammed();
    }

    @Override
    protected void drawMachineContent(GuiGraphicsExtractor g, int x, int y) {
        boolean hasPower = menu.hasPower();
        float progress = menu.getMaxProgress() > 0 ? (float) menu.getProgress() / menu.getMaxProgress() : 0.0f;

        drawMachineSlot(g, 0, 0xFF182C38);
        drawMachineSlot(g, 1, COL_SLOT);
        drawMachineSlot(g, 2, COL_SLOT_OUTPUT);
        drawSlotLabel(g, 0, "DIRTY", -12, COL_DIM);
        drawSlotLabel(g, 1, "FILTER", 20, COL_DIM);
        drawSlotLabel(g, 2, "CLEAN", -12, COL_GREEN);

        drawStandardProgress(g, progress, hasPower && !menu.isJammed(), COL_ACCENT);
        drawRightPowerMeter(g, hasPower);
        drawStandardWear(g, menu.getWearPercent(), menu.getWearStatus(), menu.getWearColor());
        drawFeStatus(g, x + 226, y + 174, menu.getEnergy(), menu.getMaxEnergy());
        if (menu.isJammed()) {
            drawStandardStatus(g, "! CLEAR JAM", COL_RED);
        } else if (!hasPower) {
            drawStandardStatus(g, "! NO POWER", COL_YELLOW);
        }
    }
}
