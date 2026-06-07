package com.knoxhack.echoashfallprotocol.client.screen;

import com.knoxhack.echoashfallprotocol.block.menu.DeepCoreMinerMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class DeepCoreMinerScreen extends MachineScreen<DeepCoreMinerMenu> {

    public DeepCoreMinerScreen(DeepCoreMinerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected String getMachineTitle() {
        return "Deep Core Miner";
    }

    @Override
    protected boolean isMachineActive() {
        return menu.isDeepEnough() && !menu.isJammed() && menu.getProgressPercent() > 0;
    }

    @Override
    protected void drawMachineContent(GuiGraphicsExtractor g, int x, int y) {
        drawMachineSlot(g, 0, COL_SLOT_OUTPUT);
        drawSlotLabel(g, 0, "OUTPUT", -12, COL_GREEN);

        float progress = menu.getProgressPercent();
        drawProgressBar(g, x + 72, y + 108, 146, progress, menu.isDeepEnough() && !menu.isJammed(), COL_ACCENT);
        g.text(font, "CORE SAMPLE", x + 72, y + 90, COL_DIM, false);
        if (progress > 0.0f) {
            String label = (int) (progress * 100) + "%";
            g.text(font, label, x + 145 - font.width(label) / 2, y + 124, COL_TEXT, false);
        }

        int wear = menu.getWearLevel();
        int wearColor = wear < 50 ? COL_GREEN : wear < 80 ? COL_YELLOW : COL_RED;
        drawStandardWear(g, wear, wear < 50 ? "Good" : wear < 80 ? "Worn" : "Critical", wearColor);
        if (menu.isJammed()) {
            drawStandardStatus(g, "! CLEAR JAM", COL_RED);
        } else if (!menu.isDeepEnough()) {
            drawStandardStatus(g, "! BELOW Y -32 REQUIRED", COL_YELLOW);
        }
    }
}
