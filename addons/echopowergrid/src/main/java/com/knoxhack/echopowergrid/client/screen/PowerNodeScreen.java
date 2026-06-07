package com.knoxhack.echopowergrid.client.screen;

import com.knoxhack.echocore.client.ui.EchoCyberGlassUi;
import com.knoxhack.echopowergrid.api.EchoGridState;
import com.knoxhack.echopowergrid.api.EchoPowerQuality;
import com.knoxhack.echopowergrid.menu.PowerNodeMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class PowerNodeScreen extends AbstractContainerScreen<PowerNodeMenu> {
    private static final int PANEL = 0xEE0A1015;
    private static final int PANEL_SOFT = 0xAA111B22;
    private static final int ACCENT = 0xFF55DFFF;
    private static final int TEXT = 0xFFE8F5F8;
    private static final int DIM = 0xFF8FA8B2;
    private static final int GREEN = 0xFF58D878;
    private static final int AMBER = 0xFFFFC24B;
    private static final int RED = 0xFFFF5D4D;
    private static final int STEEL = 0xFF34434B;

    public PowerNodeScreen(PowerNodeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, menu.hasFuelSlot() ? PowerNodeMenu.GUI_WIDTH : PowerNodeMenu.STATUS_WIDTH,
                menu.hasFuelSlot() ? PowerNodeMenu.GUI_HEIGHT : PowerNodeMenu.STATUS_HEIGHT);
        this.titleLabelX = 16;
        this.titleLabelY = 14;
        this.inventoryLabelX = PowerNodeMenu.PLAYER_INV_X;
        this.inventoryLabelY = PowerNodeMenu.PLAYER_INV_Y - 13;
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int x = leftPos;
        int y = topPos;
        EchoCyberGlassUi.panel(graphics, x, y, imageWidth, imageHeight, PANEL, accentColor());
        graphics.fill(x + 2, y + 2, x + imageWidth - 2, y + 42, 0xDD101820);
        graphics.fill(x + 18, y + 54, x + imageWidth - 18,
                y + (menu.hasFuelSlot() ? 136 : 150), 0x99071014);

        if (menu.hasFuelSlot()) {
            drawSlotBackground(graphics, x + PowerNodeMenu.FUEL_X, y + PowerNodeMenu.FUEL_Y, 0xFF13232B);
            for (int i = 1; i < menu.slots.size(); i++) {
                Slot slot = menu.slots.get(i);
                drawSlotBackground(graphics, x + slot.x, y + slot.y, 0xFF0B1116);
            }
            graphics.fill(x + 48, y + PowerNodeMenu.PLAYER_INV_Y - 5, x + imageWidth - 48,
                    y + PowerNodeMenu.PLAYER_INV_Y - 4, 0x3355DFFF);
        }

        drawBars(graphics, x, y);
        drawButtons(graphics, x, y, mouseX, mouseY);
        super.extractContents(graphics, mouseX, mouseY, partialTick);
        drawReadouts(graphics, x, y);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, Component.literal(fit("ECHO GRID // " + title.getString(), 250)), titleLabelX,
                titleLabelY, accentColor(), true);
        if (menu.hasFuelSlot()) {
            graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xD8F6FF, false);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int x = leftPos;
        int y = topPos;
        int buttonY = y + imageHeight - 30;
        if (menu.kind() == PowerNodeMenu.KIND_BREAKER
                && clickButton(event, x + 24, buttonY, 92, 18, PowerNodeMenu.BUTTON_RESET_BREAKER, menu.isTripped())) {
            return true;
        }
        if (clickButton(event, x + imageWidth - 116, buttonY, 92, 18, PowerNodeMenu.BUTTON_REFRESH_GRID, true)) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void drawBars(GuiGraphicsExtractor graphics, int x, int y) {
        int rightX = menu.hasFuelSlot() ? x + 148 : x + 154;
        int top = y + 72;
        drawBar(graphics, rightX, top, 134, 8, pixels(menu.networkGeneration(), Math.max(1L, menu.networkDemand()), 134),
                menu.networkGeneration() >= menu.networkDemand() ? GREEN : AMBER);
        drawBar(graphics, rightX, top + 28, 134, 8, pixels(menu.networkStored(), menu.networkCapacity(), 134), ACCENT);

        if (menu.localCapacity() > 0) {
            drawBar(graphics, rightX, top + 56, 134, 8, pixels(menu.localEnergy(), menu.localCapacity(), 134), GREEN);
        } else if (menu.localDemand() > 0) {
            drawBar(graphics, rightX, top + 56, 134, 8, menu.isOnline() ? 134 : 0, menu.isOnline() ? GREEN : RED);
        }

        if (menu.hasFuelSlot() && menu.totalBurnTime() > 0) {
            int burn = pixels(menu.burnTime(), menu.totalBurnTime(), 62);
            drawBar(graphics, x + PowerNodeMenu.FUEL_X - 22, y + PowerNodeMenu.FUEL_Y + 25, 62, 6, burn, AMBER);
        }
    }

    private void drawReadouts(GuiGraphicsExtractor graphics, int x, int y) {
        String state = stateName(menu.stateId());
        graphics.text(font, Component.literal(fit(menu.kindName() + " | " + state, 256)), x + 24, y + 58,
                stateColor(menu.stateId()), false);
        if (menu.hasFuelSlot()) {
            graphics.text(font, Component.literal("FUEL"), x + PowerNodeMenu.FUEL_X - 2, y + PowerNodeMenu.FUEL_Y - 14,
                    DIM, false);
        }

        int labelX = menu.hasFuelSlot() ? x + 148 : x + 24;
        int valueX = menu.hasFuelSlot() ? x + 148 : x + 160;
        int lineY = y + 72;
        readout(graphics, labelX, valueX, lineY, "Supply", ep(menu.networkGeneration()) + "/t", TEXT);
        readout(graphics, labelX, valueX, lineY + 28, "Demand", ep(menu.networkDemand()) + "/t", TEXT);
        readout(graphics, labelX, valueX, lineY + 56, "Stored", ep(menu.networkStored()) + "/" + ep(menu.networkCapacity()),
                menu.networkStored() > 0 ? GREEN : DIM);
        readout(graphics, labelX, valueX, lineY + 84, "Drawable", ep(menu.networkAvailable()),
                menu.networkAvailable() > 0 ? GREEN : AMBER);

        int detailY = menu.hasFuelSlot() ? y + 126 : y + 142;
        String local = switch (menu.kind()) {
            case PowerNodeMenu.KIND_GENERATOR -> "Local " + ep(menu.localGeneration()) + "/t | Buffer "
                    + ep(menu.localEnergy()) + "/" + ep(menu.localCapacity());
            case PowerNodeMenu.KIND_BATTERY -> "I/O " + ep(menu.localInput()) + "/" + ep(menu.localOutput()) + "/t";
            case PowerNodeMenu.KIND_CONSUMER -> "Local demand " + ep(menu.localDemand()) + "/t | "
                    + (menu.isOnline() ? "POWERED" : "NO POWER");
            case PowerNodeMenu.KIND_BREAKER -> menu.isTripped() ? "Breaker tripped. Reset is available." : "Breaker nominal.";
            default -> "Nodes " + menu.nodeCount() + " | Quality " + qualityName(menu.qualityId())
                    + " | Limit " + limit(menu.transferLimit());
        };
        graphics.text(font, Component.literal(fit(local, imageWidth - 48)), x + 24, detailY,
                menu.isTripped() ? RED : TEXT, false);

        if (menu.hasFuelSlot()) {
            String burn = menu.burnTime() > 0 ? "Burn " + menu.burnTime() + "/" + Math.max(1, menu.totalBurnTime())
                    : "Burn idle";
            graphics.text(font, Component.literal(burn), x + PowerNodeMenu.FUEL_X - 22,
                    y + PowerNodeMenu.FUEL_Y + 36, menu.burnTime() > 0 ? AMBER : DIM, false);
        }
    }

    private void readout(GuiGraphicsExtractor graphics, int labelX, int valueX, int y, String label, String value, int color) {
        graphics.text(font, label, labelX, y, DIM, false);
        graphics.text(font, Component.literal(fit(value, leftPos + imageWidth - valueX - 28)), valueX, y + 10, color, false);
    }

    private void drawButtons(GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY) {
        int buttonY = y + imageHeight - 30;
        if (menu.kind() == PowerNodeMenu.KIND_BREAKER) {
            drawButton(graphics, x + 24, buttonY, 92, 18, "RESET", mouseX, mouseY, menu.isTripped());
        }
        drawButton(graphics, x + imageWidth - 116, buttonY, 92, 18, "REFRESH", mouseX, mouseY, true);
    }

    private boolean clickButton(MouseButtonEvent event, int x, int y, int w, int h, int id, boolean enabled) {
        if (!enabled || event.button() != 0 || !inside(event.x(), event.y(), x, y, w, h)) {
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
        return true;
    }

    private void drawButton(GuiGraphicsExtractor graphics, int x, int y, int w, int h, String label, int mouseX, int mouseY,
            boolean enabled) {
        EchoCyberGlassUi.button(graphics, font, x, y, w, h, label, inside(mouseX, mouseY, x, y, w, h), enabled,
                ACCENT);
    }

    private void drawSlotBackground(GuiGraphicsExtractor graphics, int x, int y, int fill) {
        EchoCyberGlassUi.slot(graphics, x, y, fill);
    }

    private void drawBar(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int filled, int color) {
        EchoCyberGlassUi.meter(graphics, x, y, w, h, filled, color);
    }

    private void frame(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int color) {
        EchoCyberGlassUi.frame(graphics, x, y, w, h, color);
    }

    private int accentColor() {
        if (menu.isTripped() || menu.stateId() >= EchoGridState.OVERLOADED.ordinal()) {
            return RED;
        }
        return menu.isPowered() || menu.isOnline() ? ACCENT : AMBER;
    }

    private static int pixels(long current, long max, int width) {
        if (max <= 0L || current <= 0L) {
            return 0;
        }
        if (current >= max) {
            return width;
        }
        return Math.max(0, Math.min(width, (int) ((double) current * width / (double) max)));
    }

    private static String stateName(int id) {
        EchoGridState[] values = EchoGridState.values();
        return id >= 0 && id < values.length ? values[id].name() : "UNKNOWN";
    }

    private static String qualityName(int id) {
        EchoPowerQuality[] values = EchoPowerQuality.values();
        return id >= 0 && id < values.length ? values[id].name() : "UNKNOWN";
    }

    private static int stateColor(int id) {
        EchoGridState[] values = EchoGridState.values();
        EchoGridState state = id >= 0 && id < values.length ? values[id] : EchoGridState.OFFLINE;
        return switch (state) {
            case STABLE, CHARGING -> GREEN;
            case DISCHARGING, BROWNOUT -> AMBER;
            case OVERLOADED, TRIPPED, EMERGENCY -> RED;
            default -> DIM;
        };
    }

    private static String ep(long value) {
        if (value >= Long.MAX_VALUE / 8L) {
            return "INF";
        }
        return Long.toString(Math.max(0L, value));
    }

    private static String limit(long value) {
        return value >= Long.MAX_VALUE / 8L ? "unlimited" : ep(value) + "/t";
    }

    private boolean inside(double px, double py, int x, int y, int w, int h) {
        return px >= x && px < x + w && py >= y && py < y + h;
    }

    private String fit(String text, int maxWidth) {
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
}
