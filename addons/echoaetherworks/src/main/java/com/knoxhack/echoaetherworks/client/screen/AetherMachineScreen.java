package com.knoxhack.echoaetherworks.client.screen;

import com.knoxhack.echoaetherworks.menu.AetherMachineMenu;
import com.knoxhack.echoarcanacore.api.AetherSignalType;
import com.knoxhack.echocore.client.ui.EchoCyberGlassUi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class AetherMachineScreen extends AbstractContainerScreen<AetherMachineMenu> {
    private static final int PANEL = 0xEE071019;
    private static final int ACCENT = 0xFF6FFFE7;
    private static final int AETHER = 0xFFB68CFF;
    private static final int TEXT = 0xFFE8FBFF;
    private static final int DIM = 0xFF8AA5B8;
    private static final int WARNING = 0xFFFFC35A;
    private static final int DANGER = 0xFFFF6E8A;

    public AetherMachineScreen(AetherMachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, AetherMachineMenu.GUI_WIDTH, AetherMachineMenu.GUI_HEIGHT);
        this.titleLabelX = 16;
        this.titleLabelY = 14;
        this.inventoryLabelX = 9999;
        this.inventoryLabelY = 9999;
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int x = leftPos;
        int y = topPos;
        EchoCyberGlassUi.panel(graphics, x, y, imageWidth, imageHeight, PANEL, ACCENT);
        graphics.fill(x + 2, y + 2, x + imageWidth - 2, y + 42, 0xDD0D1824);
        graphics.fill(x + 18, y + 54, x + 146, y + 128, 0x99070D14);
        graphics.fill(x + 154, y + 54, x + imageWidth - 18, y + 128, 0x66070D14);
        graphics.fill(x + 18, y + 138, x + 146, y + 170, 0x66070D14);
        drawBuffer(graphics, x, y);
        drawTopology(graphics, x, y);
        drawMachineSlots(graphics, x, y);
        drawControls(graphics, x, y, mouseX, mouseY);
        super.extractContents(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, Component.literal("AETHERWORKS // MACHINE NETWORK"), titleLabelX, titleLabelY, ACCENT, true);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int x = leftPos;
        int y = topPos;
        if (clickButton(event, x + 20, y + 176, 38, 18, AetherMachineMenu.BUTTON_CYCLE_MODE, true)) {
            return true;
        }
        if (clickButton(event, x + 62, y + 176, 38, 18, AetherMachineMenu.BUTTON_TOGGLE_AUTOMATION, true)) {
            return true;
        }
        if (clickButton(event, x + 104, y + 176, 38, 18, AetherMachineMenu.BUTTON_TOGGLE_REDSTONE_CONTROL, true)) {
            return true;
        }
        if (clickButton(event, x + 146, y + 176, 38, 18, AetherMachineMenu.BUTTON_CYCLE_REDSTONE_SIDE, true)) {
            return true;
        }
        if (clickButton(event, x + 188, y + 176, 30, 18, AetherMachineMenu.BUTTON_RUN_AUTOMATION_RECIPE,
                menu.automationRecipeCount() > 0)) {
            return true;
        }
        if (clickButton(event, x + 222, y + 176, 26, 18, AetherMachineMenu.BUTTON_DRAW, menu.stored() > 0)) {
            return true;
        }
        if (clickButton(event, x + 252, y + 176, 20, 18, AetherMachineMenu.BUTTON_PURIFY,
                menu.contaminationPercent() > 0)) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void drawBuffer(GuiGraphicsExtractor graphics, int x, int y) {
        int stored = menu.stored();
        int capacity = menu.capacity();
        int meterWidth = Math.max(0, Math.min(108, stored * 108 / capacity));
        graphics.text(font, "BUFFER " + stored + "/" + capacity + " // " + typeName(menu.typeOrdinal()),
                x + 24, y + 60, TEXT, false);
        EchoCyberGlassUi.meter(graphics, x + 24, y + 78, 108, 10, meterWidth, AETHER);
        graphics.text(font, "Mode " + menu.modeName() + " | Transfer " + menu.transferRate() + "/tick",
                x + 24, y + 98, ACCENT, false);
        int contamination = menu.contaminationPercent();
        graphics.text(font, "Contamination " + contamination + "%",
                x + 24, y + 114, contamination >= 35 ? DANGER : contamination > 0 ? WARNING : DIM, false);
        EchoCyberGlassUi.meter(graphics, x + 24, y + 126, 108, 8,
                Math.max(0, Math.min(108, contamination * 108 / 100)),
                contamination >= 35 ? DANGER : contamination > 0 ? WARNING : ACCENT);
    }

    private void drawTopology(GuiGraphicsExtractor graphics, int x, int y) {
        int cx = x + 214;
        int cy = y + 90;
        int neighbors = menu.neighborCount();
        int accepting = menu.acceptTargetCount();
        int pushTargets = menu.pushTargetCount();
        int graphNodes = menu.graphNodeCount();
        int graphStored = menu.graphStored();
        int graphCapacity = menu.graphCapacity();
        int graphFill = Math.max(0, Math.min(88, graphStored * 88 / graphCapacity));
        int line = menu.automationActive() ? ACCENT : DIM;
        graphics.text(font, "TOPOLOGY", x + 164, y + 60, ACCENT, false);
        graphics.text(font, "graph " + graphNodes + " nodes // depth " + menu.routeDepth(),
                x + 164, y + 112, DIM, false);
        EchoCyberGlassUi.meter(graphics, x + 164, y + 124, 88, 7, graphFill, AETHER);
        graphics.fill(cx - 10, cy - 10, cx + 10, cy + 10, 0xDD10202C);
        graphics.fill(cx - 3, cy - 3, cx + 3, cy + 3, menu.automationActive() ? AETHER : WARNING);
        for (int i = 0; i < 6; i++) {
            double angle = Math.PI * 2.0D * i / 6.0D - Math.PI / 2.0D;
            int nx = cx + (int) Math.round(Math.cos(angle) * 34.0D);
            int ny = cy + (int) Math.round(Math.sin(angle) * 24.0D);
            boolean linked = i < neighbors;
            boolean active = i < accepting;
            int color = linked ? active ? line : WARNING : 0xFF243647;
            graphics.fill(Math.min(cx, nx), Math.min(cy, ny), Math.max(cx, nx) + 1, Math.max(cy, ny) + 1, color);
            graphics.fill(nx - 4, ny - 4, nx + 4, ny + 4, color);
        }
        String status = menu.automationActive()
                ? "recipes queued " + menu.automationRecipeCount()
                : menu.redstoneControlEnabled()
                        ? "redstone " + menu.redstoneModeName().toLowerCase(java.util.Locale.ROOT) + " gated"
                        : "automation paused";
        graphics.text(font, status, x + 164, y + 130, menu.automationActive() ? ACCENT : WARNING, false);
        graphics.text(font, "risk " + menu.overloadRisk() + "% // sev " + menu.overloadSeverity()
                        + " // vents " + menu.overloadEvents(),
                x + 164, y + 140, DIM, false);
        graphics.text(font, "done " + menu.completedRecipeCount() + " // local " + neighbors
                        + " / accept " + accepting + " / push " + pushTargets,
                x + 164, y + 150, DIM, false);
        graphics.text(font, "stock in " + menu.automationInputStock() + " / out " + menu.automationOutputStock(),
                x + 164, y + 160, DIM, false);
    }

    private void drawMachineSlots(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.text(font, "INPUT", x + 24, y + 132, DIM, false);
        graphics.text(font, "CAT", x + 50, y + 132, DIM, false);
        graphics.text(font, "OUT", x + 94, y + 132, DIM, false);
        graphics.text(font, "side " + menu.redstoneSideName() + " // comp " + menu.comparatorSignal(),
                x + 24, y + 166, DIM, false);
    }

    private void drawControls(GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY) {
        drawButton(graphics, x + 20, y + 176, 38, 18, "MODE", mouseX, mouseY, true);
        drawButton(graphics, x + 62, y + 176, 38, 18, menu.automationEnabled() ? "AUTO" : "PAUS", mouseX, mouseY, true);
        drawButton(graphics, x + 104, y + 176, 38, 18, redstoneLabel(), mouseX, mouseY, true);
        drawButton(graphics, x + 146, y + 176, 38, 18, "SIDE", mouseX, mouseY, true);
        drawButton(graphics, x + 188, y + 176, 30, 18, "RUN", mouseX, mouseY, menu.automationRecipeCount() > 0);
        drawButton(graphics, x + 222, y + 176, 26, 18, "GET", mouseX, mouseY, menu.stored() > 0);
        drawButton(graphics, x + 252, y + 176, 20, 18, "CLR", mouseX, mouseY,
                menu.contaminationPercent() > 0);
        String lockout = menu.overloadLockoutTicks() > 0 ? " // lockout " + menu.overloadLockoutTicks() + "t" : "";
        graphics.text(font, "AETHERWORKS // PHYSICAL LANES ONLINE" + lockout, x + 24, y + 196, DIM, false);
    }

    private boolean clickButton(MouseButtonEvent event, int x, int y, int w, int h, int id, boolean enabled) {
        if (!enabled || event.button() != 0 || event.x() < x || event.x() >= x + w || event.y() < y || event.y() >= y + h) {
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
        return true;
    }

    private void drawButton(GuiGraphicsExtractor graphics, int x, int y, int w, int h, String label,
            int mouseX, int mouseY, boolean enabled) {
        EchoCyberGlassUi.button(graphics, font, x, y, w, h, label, mouseX, mouseY, enabled);
    }

    private String redstoneLabel() {
        return switch (menu.redstoneMode()) {
            case com.knoxhack.echoaetherworks.block.entity.AetherStorageBlockEntity.REDSTONE_HIGH -> "HIGH";
            case com.knoxhack.echoaetherworks.block.entity.AetherStorageBlockEntity.REDSTONE_LOW -> "LOW";
            case com.knoxhack.echoaetherworks.block.entity.AetherStorageBlockEntity.REDSTONE_PULSE -> "PULSE";
            default -> "FREE";
        };
    }

    private static String typeName(int ordinal) {
        AetherSignalType[] values = AetherSignalType.values();
        if (ordinal < 0 || ordinal >= values.length) {
            return "raw_aether";
        }
        return values[ordinal].serializedName();
    }
}
