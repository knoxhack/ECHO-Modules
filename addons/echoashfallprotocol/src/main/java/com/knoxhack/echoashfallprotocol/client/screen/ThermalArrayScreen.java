package com.knoxhack.echoashfallprotocol.client.screen;

import com.knoxhack.echoashfallprotocol.block.menu.ThermalArrayMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class ThermalArrayScreen extends MachineScreen<ThermalArrayMenu> {

    public ThermalArrayScreen(ThermalArrayMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected String getMachineTitle() {
        return menu.isFailed() ? "Thermal Array - Fault" : "Thermal Array";
    }

    @Override
    protected boolean isMachineActive() {
        return !menu.isFailed() && menu.getBurnTimeRemaining() > 0;
    }

    @Override
    protected void drawMachineContent(GuiGraphicsExtractor g, int x, int y) {
        drawMachineSlot(g, 0, COL_SLOT_FUEL);
        drawMachineSlot(g, 1, COL_SLOT_FUEL);
        drawMachineSlot(g, 2, COL_SLOT_FUEL);
        drawSlotLabel(g, 1, "FUEL ARRAY", -12, COL_ORANGE);

        int maxBurn = menu.getMaxBurnTime();
        float burnPercent = maxBurn > 0 ? (float) menu.getBurnTimeRemaining() / maxBurn : 0.0f;
        drawFlameGauge(g, x + 160, y + 72, 60, burnPercent, menu.getBurnTimeRemaining() > 0);

        int maxEnergy = menu.getMaxEnergy();
        float energyPercent = maxEnergy > 0 ? (float) menu.getEnergy() / maxEnergy : 0.0f;
        drawEnergyBar(g, x + 218, y + 72, 34, 60, energyPercent, menu.getEnergy(), maxEnergy, menu.isFailed());

        if (maxBurn > 0) {
            g.text(font, "BURN " + (int) (burnPercent * 100) + "%", x + 28, y + 150, COL_YELLOW, false);
        }
        if (menu.isFailed()) {
            drawStatusLine(g, "! RESTART REQUIRED", x + 28, y + 172, COL_RED);
        }
    }
}
