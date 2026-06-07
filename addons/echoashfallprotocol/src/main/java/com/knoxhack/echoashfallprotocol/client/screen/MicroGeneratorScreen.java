package com.knoxhack.echoashfallprotocol.client.screen;

import com.knoxhack.echoashfallprotocol.block.menu.MicroGeneratorMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class MicroGeneratorScreen extends MachineScreen<MicroGeneratorMenu> {

    public MicroGeneratorScreen(MicroGeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected String getMachineTitle() {
        return menu.isFailed() ? "Micro Generator - Fault" : "Micro Generator";
    }

    @Override
    protected boolean isMachineActive() {
        return !menu.isFailed() && menu.getBurnTimeRemaining() > 0;
    }

    @Override
    protected void drawMachineContent(GuiGraphicsExtractor g, int x, int y) {
        boolean failed = menu.isFailed();

        drawMachineSlot(g, 0, COL_SLOT_FUEL);
        drawSlotLabel(g, 0, "FUEL", -12, COL_ORANGE);

        int maxEnergy = menu.getMaxEnergy();
        float energyPercent = maxEnergy > 0 ? (float) menu.getEnergy() / maxEnergy : 0.0f;
        drawEnergyBar(g, x + 194, y + 72, 34, 60, energyPercent, menu.getEnergy(), maxEnergy, failed);

        int maxBurn = menu.getMaxBurnTime();
        if (maxBurn > 0) {
            float burnPercent = (float) menu.getBurnTimeRemaining() / maxBurn;
            drawProgressBar(g, x + 118, y + 102, 64, burnPercent, !failed, COL_ORANGE);
            g.text(font, "FUEL " + (int) (burnPercent * 100) + "%", x + 118, y + 86, COL_YELLOW, false);
        }

        if (failed) {
            drawStandardStatus(g, "! RESTART REQUIRED", COL_RED);
        }

        drawStandardWear(g, menu.getWearPercent(), menu.getWearStatus(), menu.getWearColor());
    }
}
