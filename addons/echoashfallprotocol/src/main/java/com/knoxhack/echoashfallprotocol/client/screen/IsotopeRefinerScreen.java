package com.knoxhack.echoashfallprotocol.client.screen;

import com.knoxhack.echoashfallprotocol.block.menu.IsotopeRefinerMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class IsotopeRefinerScreen extends MachineScreen<IsotopeRefinerMenu> {

    public IsotopeRefinerScreen(IsotopeRefinerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected String getMachineTitle() {
        return "Isotope Refiner";
    }

    @Override
    protected boolean isMachineActive() {
        return !menu.isJammed() && menu.getProgressPercent() > 0;
    }

    @Override
    protected void drawMachineContent(GuiGraphicsExtractor g, int x, int y) {
        float progress = menu.getProgressPercent();
        int contamination = menu.getContaminationLevel();
        boolean hasInput = menu.slots.get(0).hasItem();
        boolean hasCatalyst = menu.slots.get(1).hasItem();
        boolean outputBlocked = isSlotFull(2) || isSlotFull(3);

        drawMachineSlot(g, 0, 0xFF15353A);
        drawMachineSlot(g, 1, 0xFF1A233A);
        drawMachineSlot(g, 2, COL_SLOT_OUTPUT);
        drawMachineSlot(g, 3, 0xFF3A1A1A);
        drawSlotLabel(g, 0, "IN", -11, COL_DIM);
        drawSlotLabel(g, 1, "CAT", 20, COL_ACCENT);
        drawSlotLabel(g, 2, "OUT", -11, COL_GREEN);
        drawSlotLabel(g, 3, "RAD", 20, COL_RED);

        drawStandardProgress(g, progress, progress > 0.0f && !menu.isJammed(), COL_ACCENT);
        drawFeReadout(g, x, y, menu.getEnergy(), menu.getMaxEnergy());

        g.fill(x + 286, y + 72, x + 294, y + 150, 0x33FF4444);
        int radH = Math.max(0, Math.min(78, (78 * contamination) / 100));
        if (radH > 0) {
            g.fill(x + 286, y + 150 - radH, x + 294, y + 150, contamination > 70 ? COL_RED : COL_YELLOW);
        }
        g.text(font, "RAD", x + 280, y + 158, contamination > 70 ? COL_RED : COL_YELLOW, false);

        drawStandardWear(g, menu.getWearPercent(), menu.getWearStatus(), menu.getWearColor());
        if (menu.isJammed()) {
            drawStandardStatus(g, "! CLEAR JAM", COL_RED);
        } else if (!hasInput) {
            drawStandardStatus(g, "> ADD INPUT", COL_DIM);
        } else if (!hasCatalyst) {
            drawStandardStatus(g, "> ADD CATALYST", COL_ACCENT);
        } else if (outputBlocked) {
            drawStandardStatus(g, "! OUTPUT FULL", COL_YELLOW);
        } else if (progress <= 0.0f) {
            drawStandardStatus(g, "! CHECK POWER", COL_YELLOW);
        } else if (contamination > 75) {
            drawStandardStatus(g, "! CRITICAL", COL_RED);
        } else if (contamination > 50) {
            drawStandardStatus(g, "! HIGH RADS", COL_YELLOW);
        } else {
            drawStandardStatus(g, "* REFINING", COL_GREEN);
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
