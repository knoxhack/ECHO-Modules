package com.knoxhack.echoashfallprotocol.client.screen;

import com.knoxhack.echoashfallprotocol.block.menu.ThermalBurnerMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class ThermalBurnerScreen extends MachineScreen<ThermalBurnerMenu> {

    public ThermalBurnerScreen(ThermalBurnerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected String getMachineTitle() {
        return "Thermal Burner";
    }

    @Override
    protected boolean isMachineActive() {
        return menu.getBurnProgress() > 0;
    }

    @Override
    protected void drawMachineContent(GuiGraphicsExtractor g, int x, int y) {
        int burn = menu.getBurnProgress();
        int maxBurn = menu.getMaxBurnProgress();
        float burnPercent = maxBurn > 0 ? (float) burn / maxBurn : 0.0f;

        drawMachineSlot(g, 0, COL_SLOT_FUEL);
        drawMachineSlot(g, 1, 0xFF2A2818);
        drawSlotLabel(g, 0, "FUEL", -12, COL_ORANGE);
        drawSlotLabel(g, 1, "ASH", -12, COL_DIM);

        drawFlameGauge(g, x + 154, y + 72, 60, burnPercent, burn > 0);
        if (burn > 0) {
            g.text(font, "HEAT " + (int) (burnPercent * 100) + "%", x + 132, y + 140, COL_ORANGE, false);
        }

        int maxEnergy = menu.getMaxEnergy();
        float energyPercent = maxEnergy > 0 ? (float) menu.getEnergy() / maxEnergy : 0.0f;
        drawEnergyBar(g, x + 202, y + 72, 32, 60, energyPercent, menu.getEnergy(), maxEnergy, false);

        if (burn > maxBurn / 2 && maxBurn > 0) {
            drawStandardStatus(g, "HIGH HEAT", COL_RED);
        } else if (burn > 0) {
            drawStandardStatus(g, "HEATING", COL_YELLOW);
        }

        drawStandardWear(g, menu.getWearPercent(), menu.getWearStatus(), menu.getWearColor());
        if (menu.isJammed()) {
            drawStandardStatus(g, "! CLEAR JAM", COL_RED);
        }
    }
}
