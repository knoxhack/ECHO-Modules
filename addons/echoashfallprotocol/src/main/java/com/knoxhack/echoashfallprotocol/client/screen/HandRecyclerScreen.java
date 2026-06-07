package com.knoxhack.echoashfallprotocol.client.screen;

import com.knoxhack.echoashfallprotocol.block.menu.HandRecyclerMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class HandRecyclerScreen extends MachineScreen<HandRecyclerMenu> {

    public HandRecyclerScreen(HandRecyclerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected String getMachineTitle() {
        return menu.isJammed() ? "Hand Recycler - Jammed" : "Hand Recycler";
    }

    @Override
    protected boolean isMachineActive() {
        return menu.hasPower() && !menu.isJammed() && menu.getProgressPercent() > 0;
    }

    @Override
    protected void drawMachineContent(GuiGraphicsExtractor g, int x, int y) {
        boolean hasPower = menu.hasPower();
        boolean jammed = menu.isJammed();
        float progress = menu.getProgressPercent();

        drawMachineSlot(g, 0, COL_SLOT);
        drawMachineSlot(g, 1, COL_SLOT_OUTPUT);
        drawMachineSlot(g, 2, 0xFF1A2D3A);
        drawSlotLabel(g, 0, "SCRAP", -12, COL_DIM);
        drawSlotLabel(g, 1, "OUTPUT", -12, COL_GREEN);
        drawSlotLabel(g, 2, "UPG", 20, menu.hasSpeedUpgrade() ? COL_GREEN : COL_DIM);

        drawStandardProgress(g, progress, hasPower && !jammed, jammed ? COL_RED : COL_ACCENT);
        drawRightPowerMeter(g, hasPower);
        drawStandardWear(g, menu.getWearPercent(), menu.getWearStatus(), menu.getWearColor());
        if (jammed) {
            drawStandardStatus(g, "! CLEAR JAM", COL_RED);
        } else if (!hasPower) {
            drawStandardStatus(g, "! NO POWER", COL_YELLOW);
        }
    }
}
