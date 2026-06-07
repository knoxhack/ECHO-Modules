package com.knoxhack.echoashfallprotocol.client.screen;

import com.knoxhack.echoashfallprotocol.block.menu.BatterySlot;
import com.knoxhack.echoashfallprotocol.block.menu.MachineMenuLayout;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

/**
 * Shared ECHO-styled base for compact machine GUIs.
 *
 * Compact machine skins render as larger 2x panels, while slots stay vanilla
 * sized so inventory behavior and tooltips remain familiar.
 */
public abstract class MachineScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {

    protected static final int COL_BG          = 0xEE0A0F1A;
    protected static final int COL_HEADER      = 0xFF162535;
    protected static final int COL_PANEL       = 0xAA101A26;
    protected static final int COL_ACCENT      = 0xFF4DBAF4;
    protected static final int COL_ACCENT_DIM  = 0x664DBAF4;
    protected static final int COL_TEXT        = 0xFFE8F0F5;
    protected static final int COL_DIM         = 0xFF8A9BB0;
    protected static final int COL_GREEN       = 0xFF42D67E;
    protected static final int COL_YELLOW      = 0xFFF0C94B;
    protected static final int COL_RED         = 0xFFE25959;
    protected static final int COL_ORANGE      = 0xFFFFA94D;
    protected static final int COL_SLOT        = 0xFF252B39;
    protected static final int COL_SLOT_OUTPUT = 0xFF183525;
    protected static final int COL_SLOT_FUEL   = 0xFF35271A;

    protected static final int GUI_WIDTH = MachineMenuLayout.COMPACT_WIDTH;
    protected static final int GUI_HEIGHT = MachineMenuLayout.COMPACT_HEIGHT;
    protected static final int HEADER_HEIGHT = MachineMenuLayout.HEADER_HEIGHT;
    protected static final int MACHINE_TOP = MachineMenuLayout.MACHINE_TOP;
    protected static final int MACHINE_BOTTOM = MachineMenuLayout.MACHINE_BOTTOM;
    protected static final int PLAYER_INV_TOP = MachineMenuLayout.PLAYER_INV_Y;
    protected static final int HOTBAR_TOP = MachineMenuLayout.HOTBAR_Y;

    protected long animTick = 0;

    public MachineScreen(T menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, GUI_WIDTH, GUI_HEIGHT);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        animTick++;
        this.leftPos = (width - imageWidth) / 2;
        this.topPos = (height - imageHeight) / 2;

        drawBackground(graphics, leftPos, topPos);
        drawHeader(graphics, leftPos, topPos);
        drawMachineArea(graphics, leftPos, topPos);
        drawMachineContent(graphics, leftPos, topPos);
        drawBatterySlots(graphics, leftPos, topPos);
        drawPlayerInventory(graphics, leftPos, topPos);

        super.extractContents(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // Custom screens render all labels in absolute coordinates. This keeps
        // vanilla title/inventory labels from bleeding through the ECHO chrome.
    }

    protected abstract void drawMachineContent(GuiGraphicsExtractor graphics, int x, int y);

    protected abstract String getMachineTitle();

    protected abstract boolean isMachineActive();

    protected void drawBackground(GuiGraphicsExtractor g, int x, int y) {
        if (!MachineGuiSkins.renderCompact(g, getMachineSkinName(), x, y, imageWidth, imageHeight)) {
            g.fill(x, y, x + imageWidth, y + imageHeight, COL_BG);
        } else {
            g.fill(x, y, x + imageWidth, y + imageHeight, 0x55050A12);
        }

        for (int row = y + 2; row < y + imageHeight - 2; row += 3) {
            int lineColor = (((row / 3) % 3 == 0) ? 0x10 : 0x08) << 24 | 0x00FFFFFF;
            g.fill(x + 2, row, x + imageWidth - 2, row + 1, lineColor);
        }

        int pulse = 190 + (int) (45 * Math.sin(animTick / 48.0));
        int border = (Math.max(120, pulse) << 24) | 0x004DBAF4;
        g.fill(x, y, x + imageWidth, y + 2, border);
        g.fill(x, y + imageHeight - 2, x + imageWidth, y + imageHeight, border);
        g.fill(x, y, x + 2, y + imageHeight, border);
        g.fill(x + imageWidth - 2, y, x + imageWidth, y + imageHeight, border);
    }

    protected String getMachineSkinName() {
        String simpleName = getClass().getSimpleName();
        String baseName = simpleName.endsWith("Screen")
                ? simpleName.substring(0, simpleName.length() - "Screen".length())
                : simpleName;
        StringBuilder result = new StringBuilder(baseName.length() + 4);
        for (int i = 0; i < baseName.length(); i++) {
            char ch = baseName.charAt(i);
            if (Character.isUpperCase(ch) && i > 0) {
                result.append('_');
            }
            result.append(Character.toLowerCase(ch));
        }
        return result.toString();
    }

    protected void drawHeader(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x + 2, y + 2, x + imageWidth - 2, y + HEADER_HEIGHT - 3, COL_HEADER);
        g.fill(x + 2, y + HEADER_HEIGHT - 3, x + imageWidth - 2, y + HEADER_HEIGHT, COL_ACCENT);

        String title = getMachineTitle();
        g.text(font, fit(title, 210), x + 16, y + 14, COL_ACCENT, false);

        String status = isMachineActive() ? "* ONLINE" : "* OFFLINE";
        int color = isMachineActive() ? COL_GREEN : COL_DIM;
        g.text(font, status, x + imageWidth - font.width(status) - 18, y + 14, color, false);
    }

    protected void drawMachineArea(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x + 18, y + MACHINE_TOP, x + imageWidth - 18, y + MACHINE_BOTTOM, COL_PANEL);
        g.fill(x + 18, y + MACHINE_BOTTOM, x + imageWidth - 18, y + MACHINE_BOTTOM + 1, COL_ACCENT_DIM);
    }

    protected void drawPlayerInventory(GuiGraphicsExtractor g, int x, int y) {
        g.text(font, "INVENTORY", x + MachineMenuLayout.PLAYER_INV_X, y + PLAYER_INV_TOP - 13, COL_DIM, false);
        g.fill(x + 48, y + PLAYER_INV_TOP - 5, x + imageWidth - 48, y + PLAYER_INV_TOP - 4, COL_ACCENT_DIM);

        int machineSlots = getMachineSlotCount();
        for (int i = machineSlots; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            drawSlotBg(g, x + slot.x, y + slot.y, COL_SLOT);
        }
    }

    protected int getMachineSlotCount() {
        return Math.max(0, menu.slots.size() - 36);
    }

    protected void drawMachineSlot(GuiGraphicsExtractor g, int slotIndex, int color) {
        if (slotIndex < 0 || slotIndex >= menu.slots.size()) {
            return;
        }
        Slot slot = menu.slots.get(slotIndex);
        drawSlotBg(g, leftPos + slot.x, topPos + slot.y, color);
    }

    protected void drawSlotLabel(GuiGraphicsExtractor g, int slotIndex, String label, int dy, int color) {
        if (slotIndex < 0 || slotIndex >= menu.slots.size()) {
            return;
        }
        Slot slot = menu.slots.get(slotIndex);
        int labelX = leftPos + slot.x + 8 - font.width(label) / 2;
        g.text(font, label, labelX, topPos + slot.y + dy, color, false);
    }

    protected void drawSlotBg(GuiGraphicsExtractor g, int x, int y, int color) {
        g.fill(x - 2, y - 2, x + 18, y + 18, 0xFF4C5668);
        g.fill(x - 1, y - 1, x + 17, y + 17, 0xFF111722);
        g.fill(x, y, x + 16, y + 16, color);
        g.fill(x, y, x + 16, y + 1, 0x665D708A);
        g.fill(x, y, x + 1, y + 16, 0x665D708A);
    }

    protected void drawBatterySlots(GuiGraphicsExtractor g, int x, int y) {
        int machineSlots = getMachineSlotCount();
        for (int i = 0; i < machineSlots; i++) {
            Slot slot = menu.slots.get(i);
            if (slot instanceof BatterySlot) {
                drawSlotBg(g, x + slot.x, y + slot.y, 0xFF132B3A);
                String label = "BAT";
                g.text(font, label, x + slot.x + 8 - font.width(label) / 2, y + slot.y + 22, COL_ACCENT, false);
            }
        }
    }

    protected void drawProgressBar(GuiGraphicsExtractor g, int x, int y, int width, float percent,
                                   boolean active, int fillColor) {
        int height = 8;
        float clamped = Math.max(0.0f, Math.min(1.0f, percent));
        g.fill(x, y, x + width, y + height, 0xFF151B29);
        g.fill(x, y, x + width, y + 1, 0x553A4D66);
        if (clamped > 0.0f) {
            int fillWidth = Math.max(1, (int) (width * clamped));
            g.fill(x, y, x + fillWidth, y + height, active ? fillColor : COL_DIM);
            if (active && fillWidth > 2) {
                g.fill(x + fillWidth - 2, y + 1, x + fillWidth, y + height - 1, 0xCCB8E8FF);
            }
        }
    }

    protected void drawPowerBar(GuiGraphicsExtractor g, int x, int y, int height, float percent, boolean hasPower) {
        float clamped = Math.max(0.0f, Math.min(1.0f, percent));
        int width = 8;
        g.fill(x, y, x + width, y + height, 0xFF111722);
        if (hasPower && clamped > 0.0f) {
            int fillHeight = Math.max(1, (int) (height * clamped));
            int fillY = y + height - fillHeight;
            g.fill(x + 1, fillY, x + width - 1, y + height - 1, COL_ACCENT);
        }
    }

    protected void drawFlameGauge(GuiGraphicsExtractor g, int x, int y, int height, float percent, boolean active) {
        float clamped = Math.max(0.0f, Math.min(1.0f, percent));
        int width = 18;
        g.fill(x, y, x + width, y + height, 0xFF231409);
        if (active && clamped > 0.0f) {
            int fillHeight = Math.max(1, (int) (height * clamped));
            int fillY = y + height - fillHeight;
            g.fill(x + 2, fillY, x + width - 2, y + height - 1, clamped > 0.5f ? 0xFFFF7722 : 0xFFFF3D1F);
        }
    }

    protected void drawEnergyBar(GuiGraphicsExtractor g, int x, int y, int width, int height,
                                 float percent, int energy, int maxEnergy, boolean failed) {
        float clamped = Math.max(0.0f, Math.min(1.0f, percent));
        g.fill(x, y, x + width, y + height, 0xFF111722);
        if (clamped > 0.0f) {
            int fillHeight = Math.max(1, (int) (height * clamped));
            int fillY = y + height - fillHeight;
            g.fill(x + 1, fillY, x + width - 1, y + height - 1, failed ? COL_RED : COL_ACCENT);
        }
        String text = energy + "/" + maxEnergy;
        g.text(font, fit(text, 76), x + width / 2 - font.width(fit(text, 76)) / 2,
                y + height + 5, failed ? COL_RED : COL_TEXT, false);
    }

    protected void drawWearBar(GuiGraphicsExtractor g, int x, int y, int width, int percent, int color) {
        int height = 5;
        int clamped = Math.max(0, Math.min(100, percent));
        g.fill(x, y, x + width, y + height, 0xFF151B29);
        int fillWidth = (width * clamped) / 100;
        if (fillWidth > 0) {
            g.fill(x, y, x + fillWidth, y + height, color);
        }
    }

    protected void drawStatusLine(GuiGraphicsExtractor g, String text, int x, int y, int color) {
        g.text(font, text, x, y, color, true);
    }

    protected void drawWearStatus(GuiGraphicsExtractor g, int x, int y, int wearPercent, String wearStatus, int wearColor) {
        g.text(font, "WEAR", x, y, COL_DIM, false);
        drawWearBar(g, x + 42, y + 2, 94, wearPercent, wearColor);
        g.text(font, fit(wearStatus, 82), x + 144, y, wearColor, false);
    }

    protected void drawFeStatus(GuiGraphicsExtractor g, int x, int y, int energy, int maxEnergy) {
        String text = maxEnergy > 0 ? "FE " + energy + "/" + maxEnergy : "FE --";
        int color = energy > 0 ? COL_ACCENT : COL_DIM;
        g.text(font, fit(text, 112), x, y, color, false);
    }

    protected void drawFeReadout(GuiGraphicsExtractor g, int x, int y, int energy, int maxEnergy) {
        String text = maxEnergy > 0 ? energy + "/" + maxEnergy + " FE" : "-- FE";
        int color = energy > 0 ? COL_ACCENT : COL_DIM;
        String fitted = fit(text, 112);
        g.text(font, fitted, x + imageWidth - font.width(fitted) - 26, y + 58, color, false);
    }

    protected void drawStandardProgress(GuiGraphicsExtractor g, float percent, boolean active, int color) {
        int barX = leftPos + 132;
        int barY = topPos + 100;
        int barW = 92;
        drawProgressBar(g, barX, barY, barW, percent, active, color);
        if (percent > 0.0f) {
            String label = (int) (percent * 100) + "%";
            g.text(font, label, barX + barW / 2 - font.width(label) / 2, topPos + 84, COL_TEXT, false);
        }
    }

    protected void drawRightPowerMeter(GuiGraphicsExtractor g, boolean hasPower) {
        int meterX = leftPos + 316;
        int meterY = topPos + 142;
        drawPowerBar(g, meterX, meterY, 42, hasPower ? 1.0f : 0.0f, hasPower);
        g.text(font, "PWR", meterX - 4, meterY + 48, hasPower ? COL_ACCENT : COL_DIM, false);
    }

    protected void drawStandardWear(GuiGraphicsExtractor g, int wearPercent, String wearStatus, int wearColor) {
        drawWearStatus(g, leftPos + 28, topPos + 184, wearPercent, wearStatus, wearColor);
    }

    protected void drawStandardStatus(GuiGraphicsExtractor g, String text, int color) {
        drawStatusLine(g, fit(text, 190), leftPos + 28, topPos + 158, color);
    }

    protected String fit(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }
        String suffix = "...";
        int suffixW = font.width(suffix);
        if (maxWidth <= suffixW) {
            return font.plainSubstrByWidth(text, maxWidth);
        }
        return font.plainSubstrByWidth(text, maxWidth - suffixW) + suffix;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }
}
