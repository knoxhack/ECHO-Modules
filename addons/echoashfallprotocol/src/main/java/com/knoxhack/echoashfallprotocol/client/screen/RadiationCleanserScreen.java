package com.knoxhack.echoashfallprotocol.client.screen;

import com.knoxhack.echoashfallprotocol.block.menu.RadiationCleanserMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class RadiationCleanserScreen extends MachineScreen<RadiationCleanserMenu> {

    public RadiationCleanserScreen(RadiationCleanserMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected String getMachineTitle() {
        return "Radiation Cleanser";
    }

    @Override
    protected boolean isMachineActive() {
        return menu.getProgressPercent() > 0;
    }

    @Override
    protected void drawMachineContent(GuiGraphicsExtractor g, int x, int y) {
        boolean hasInput = menu.slots.get(0).hasItem();
        boolean hasFilter = menu.slots.get(1).hasItem();
        boolean outputBlocked = isSlotFull(2);

        drawMachineSlot(g, 0, 0xFF3A231A);
        drawMachineSlot(g, 1, COL_SLOT);
        drawMachineSlot(g, 2, COL_SLOT_OUTPUT);
        drawSlotLabel(g, 0, "DIRTY", -12, COL_RED);
        drawSlotLabel(g, 1, "FILTER", -12, COL_ACCENT);
        drawSlotLabel(g, 2, "CLEAN", -12, COL_GREEN);

        float progress = menu.getProgressPercent();
        drawProgressBar(g, x + 166, y + 122, 58, progress, progress > 0.0f, COL_ACCENT);
        if (progress > 0.0f) {
            String label = (int) (progress * 100) + "%";
            g.text(font, label, x + 195 - font.width(label) / 2, y + 138, COL_TEXT, false);
        }
        drawFeReadout(g, x, y, menu.getEnergy(), menu.getMaxEnergy());

        int wear = Math.min(100, menu.getWearLevel());
        int wearColor = wear < 50 ? COL_GREEN : wear < 80 ? COL_YELLOW : COL_RED;
        drawStandardWear(g, wear, wear < 50 ? "Good" : wear < 80 ? "Worn" : "Critical", wearColor);

        if (!hasInput) {
            drawStandardStatus(g, "> ADD CONTAM", COL_DIM);
        } else if (!hasFilter) {
            drawStandardStatus(g, "> ADD ADV FILTER", COL_ACCENT);
        } else if (outputBlocked) {
            drawStandardStatus(g, "! OUTPUT FULL", COL_YELLOW);
        } else if (progress > 0.0f) {
            drawStandardStatus(g, "* CLEANSING", COL_GREEN);
        } else {
            drawStandardStatus(g, "! CHECK POWER", COL_YELLOW);
        }
    }

    private boolean isSlotFull(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= menu.slots.size() || !menu.slots.get(slotIndex).hasItem()) {
            return false;
        }
        var stack = menu.slots.get(slotIndex).getItem();
        return stack.getCount() >= stack.getMaxStackSize();
    }
}
