package com.knoxhack.echoashfallprotocol.client.screen;

import com.knoxhack.echoashfallprotocol.block.menu.CrystallineSynthesizerMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class CrystallineSynthesizerScreen extends MachineScreen<CrystallineSynthesizerMenu> {

    public CrystallineSynthesizerScreen(CrystallineSynthesizerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected String getMachineTitle() {
        return "Crystalline Synthesizer";
    }

    @Override
    protected boolean isMachineActive() {
        return !menu.isJammed() && menu.getProgressPercent() > 0;
    }

    @Override
    protected void drawMachineContent(GuiGraphicsExtractor g, int x, int y) {
        int phaseColor = menu.getPhaseColor();
        String phaseName = menu.getPhaseName();
        float progress = menu.getProgressPercent();

        drawMachineSlot(g, 0, 0xFF28183A);
        drawMachineSlot(g, 1, 0xFF28183A);
        drawMachineSlot(g, 2, 0xFF102C3A);
        drawMachineSlot(g, 3, COL_SLOT_OUTPUT);
        drawSlotLabel(g, 0, "INPUT", -11, COL_DIM);
        drawSlotLabel(g, 2, "CORE", 20, COL_ACCENT);
        drawSlotLabel(g, 3, "OUT", -11, COL_GREEN);

        drawProgressBar(g, x + 132, y + 102, 92, progress, progress > 0.0f && !menu.isJammed(), phaseColor);
        g.text(font, fit(phaseName, 112), x + 132, y + 124, phaseColor, false);
        if (progress > 0.0f) {
            String label = (int) (progress * 100) + "%";
            g.text(font, label, x + 178 - font.width(label) / 2, y + 86, COL_TEXT, false);
        }

        drawStandardWear(g, menu.getWearPercent(), menu.getWearStatus(), menu.getWearColor());
        if (menu.isJammed()) {
            drawStandardStatus(g, "! CLEAR JAM", COL_RED);
        }
    }
}
