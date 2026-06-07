package com.knoxhack.echoashfallprotocol.client.screen;

import com.knoxhack.echoashfallprotocol.block.menu.FilterWorkbenchMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class FilterWorkbenchScreen extends MachineScreen<FilterWorkbenchMenu> {

    public FilterWorkbenchScreen(FilterWorkbenchMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected String getMachineTitle() {
        return "Filter Workbench";
    }

    @Override
    protected boolean isMachineActive() {
        return menu.hasPower() && !menu.isJammed() && menu.getProgressPercent() > 0;
    }

    @Override
    protected void drawMachineContent(GuiGraphicsExtractor g, int x, int y) {
        drawMachineSlot(g, 0, COL_SLOT);
        drawMachineSlot(g, 1, COL_SLOT);
        drawMachineSlot(g, 2, COL_SLOT);
        drawMachineSlot(g, 3, COL_SLOT_OUTPUT);
        drawSlotLabel(g, 1, "REAGENTS", -12, COL_DIM);
        drawSlotLabel(g, 3, "FILTER", -12, COL_GREEN);

        float progress = menu.getProgressPercent();
        drawStandardProgress(g, progress, menu.hasPower() && !menu.isJammed(), COL_ACCENT);
        drawRightPowerMeter(g, menu.hasPower());
        drawStandardWear(g, menu.getWearPercent(), menu.getWearStatus(), menu.getWearColor());
        if (menu.isJammed()) {
            drawStandardStatus(g, "! CLEAR JAM", COL_RED);
        } else if (!menu.hasPower()) {
            drawStandardStatus(g, "! NO POWER", COL_YELLOW);
        }
    }
}
