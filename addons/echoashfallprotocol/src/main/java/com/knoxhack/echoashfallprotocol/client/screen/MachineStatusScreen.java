package com.knoxhack.echoashfallprotocol.client.screen;

import com.knoxhack.echoashfallprotocol.block.menu.MachineStatusMenu;
import com.knoxhack.echoashfallprotocol.block.menu.MachineMenuLayout;
import com.knoxhack.echoashfallprotocol.power.PowerIssue;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class MachineStatusScreen extends AbstractContainerScreen<MachineStatusMenu> {
    private static final int W = MachineMenuLayout.STATUS_WIDTH;
    private static final int STATUS_H = MachineMenuLayout.STATUS_HEIGHT;
    private static final int INVENTORY_H = MachineMenuLayout.STATUS_INVENTORY_HEIGHT;
    private static final int BG = 0xEE0A0F1A;
    private static final int HEADER = 0xFF162535;
    private static final int ACCENT = 0xFF4DBAF4;
    private static final int DIM = 0xFF8A9BB0;
    private static final int TEXT = 0xFFE8F0F5;
    private static final int GREEN = 0xFF42D67E;
    private static final int YELLOW = 0xFFF0C94B;
    private static final int RED = 0xFFE25959;

    public MachineStatusScreen(MachineStatusMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, W, menu.hasInventorySlots() ? INVENTORY_H : STATUS_H);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.leftPos = (width - imageWidth) / 2;
        this.topPos = (height - imageHeight) / 2;
        int x = leftPos;
        int y = topPos;

        if (!MachineGuiSkins.renderStatus(graphics, menu.hasInventorySlots(), x, y, imageWidth, imageHeight)) {
            graphics.fill(x, y, x + imageWidth, y + imageHeight, BG);
        } else {
            graphics.fill(x, y, x + imageWidth, y + imageHeight, 0x5A050A12);
        }
        for (int row = y + 2; row < y + imageHeight - 2; row += 3) {
            graphics.fill(x + 2, row, x + imageWidth - 2, row + 1, 0x0AFFFFFF);
        }
        graphics.fill(x, y, x + imageWidth, y + 2, ACCENT);
        graphics.fill(x, y + imageHeight - 2, x + imageWidth, y + imageHeight, ACCENT);
        graphics.fill(x, y, x + 2, y + imageHeight, ACCENT);
        graphics.fill(x + imageWidth - 2, y, x + imageWidth, y + imageHeight, ACCENT);
        graphics.fill(x + 2, y + 2, x + imageWidth - 2, y + 40, HEADER);
        graphics.fill(x + 2, y + 40, x + imageWidth - 2, y + 43, ACCENT);

        graphics.text(font, fit(menu.getMachineTitle(), 214), x + 16, y + 14, ACCENT, false);
        String state = menu.isActive() ? "* ACTIVE" : "* IDLE";
        graphics.text(font, state, x + imageWidth - font.width(state) - 18, y + 14, menu.isActive() ? GREEN : DIM, false);

        PowerIssue issue = PowerIssue.fromCode(menu.getPowerIssueCode());
        int lineY = y + 54;
        drawLine(graphics, tr("screen.EchoAshfallProtocol.machine_status.power_link"),
                tr(menu.hasPower() ? "screen.EchoAshfallProtocol.machine_status.online" : "screen.EchoAshfallProtocol.machine_status.offline"),
                lineY, menu.hasPower() ? GREEN : RED);
        lineY += 18;
        drawLine(graphics, tr("screen.EchoAshfallProtocol.machine_status.fe_buffer"),
                menu.getLocalBuffer() + "/" + menu.getLocalCapacity(), lineY, menu.getLocalBuffer() > 0 ? GREEN : DIM);
        lineY += 18;
        drawLine(graphics, tr("screen.EchoAshfallProtocol.machine_status.network"),
                menu.getNetworkStored() + "/" + menu.getNetworkCapacity(), lineY, menu.getNetworkStored() > 0 ? GREEN : RED);
        lineY += 18;
        drawLine(graphics, tr("screen.EchoAshfallProtocol.machine_status.limit"),
                menu.getTransferLimit() <= 0 ? "--" : menu.getTransferLimit() + "/t", lineY,
                issue == PowerIssue.CABLE_BOTTLENECK ? YELLOW : DIM);
        lineY += 18;
        drawLine(graphics, tr("screen.EchoAshfallProtocol.machine_status.demand"),
                menu.getEstimatedDemand() + "/t", lineY, DIM);
        lineY += 18;
        drawLine(graphics, tr("screen.EchoAshfallProtocol.machine_status.stall_reason"),
                tr(issue.translationKey()), lineY, issue.isBlocking() ? RED : GREEN);
        lineY += 18;
        drawLine(graphics, tr("screen.EchoAshfallProtocol.machine_status.wear"),
                menu.getWearPercent() + "%", lineY, menu.getWearPercent() > 80 ? RED : menu.getWearPercent() > 50 ? YELLOW : GREEN);
        lineY += 18;
        drawLine(graphics, tr("screen.EchoAshfallProtocol.machine_status.fault"),
                tr(menu.isJammed() ? "screen.EchoAshfallProtocol.machine_status.jammed" : "screen.EchoAshfallProtocol.machine_status.none"),
                lineY, menu.isJammed() ? RED : GREEN);
        lineY += 22;
        for (String line : wrap(tr(issue.hintKey()), imageWidth - 48, 2)) {
            graphics.text(font, line, x + 24, lineY, DIM, false);
            lineY += 12;
        }

        if (menu.hasInventorySlots()) {
            drawBatteryInventory(graphics);
        } else {
            graphics.text(font, tr("screen.EchoAshfallProtocol.machine_status.close"), x + 18, y + imageHeight - 22, DIM, false);
        }

        super.extractContents(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
    }

    private void drawLine(GuiGraphicsExtractor g, String label, String value, int y, int valueColor) {
        label = fit(label, 150);
        value = fit(value, 154);
        g.text(font, label, leftPos + 24, y, DIM, false);
        g.text(font, value, leftPos + imageWidth - font.width(value) - 24, y, valueColor, false);
        g.fill(leftPos + 22, y + 12, leftPos + imageWidth - 22, y + 13, 0x334DBAF4);
    }

    private void drawValueLines(GuiGraphicsExtractor g, int y) {
        String leftLabel;
        String leftValue;
        String rightLabel;
        String rightValue;
        switch (menu.getKind()) {
            case MachineStatusMenu.KIND_BATTERY_BANK, MachineStatusMenu.KIND_NEXUS_CAPACITOR -> {
                leftLabel = "ENERGY";
                leftValue = menu.getValue1() + "/" + menu.getValue2();
                rightLabel = "TRANSFER";
                rightValue = menu.getKind() == MachineStatusMenu.KIND_NEXUS_CAPACITOR ? "1024/t" : "100/t";
            }
            case MachineStatusMenu.KIND_FACTORY_CONTROLLER -> {
                leftLabel = "CONNECTED";
                leftValue = String.valueOf(menu.getValue1());
                rightLabel = "ACTIVE";
                rightValue = String.valueOf(menu.getValue2());
            }
            case MachineStatusMenu.KIND_SIGNAL_SCANNER -> {
                leftLabel = "SCAN";
                leftValue = menu.getValue1() != 0 ? "COOLDOWN" : "READY";
                rightLabel = "COST";
                rightValue = menu.getValue2() + " EU";
            }
            case MachineStatusMenu.KIND_CONTAMINANT_CONDENSER -> {
                leftLabel = "CLEANED";
                leftValue = String.valueOf(menu.getValue1());
                rightLabel = "RADIUS";
                rightValue = String.valueOf(menu.getValue2());
            }
            default -> {
                leftLabel = "RADIUS";
                leftValue = String.valueOf(menu.getValue1());
                rightLabel = "COST";
                rightValue = menu.getValue2() + "/cycle";
            }
        }
        g.text(font, leftLabel, leftPos + 12, y, DIM, false);
        g.text(font, leftValue, leftPos + 12, y + 10, TEXT, false);
        g.text(font, rightLabel, leftPos + 98, y, DIM, false);
        g.text(font, rightValue, leftPos + 98, y + 10, TEXT, false);
    }

    private void drawBatteryInventory(GuiGraphicsExtractor g) {
        g.text(font, tr("screen.EchoAshfallProtocol.machine_status.battery"),
                leftPos + MachineMenuLayout.STATUS_BATTERY_X - 14,
                topPos + MachineMenuLayout.STATUS_BATTERY_Y - 15, DIM, false);
        g.text(font, tr("screen.EchoAshfallProtocol.machine_status.inventory"),
                leftPos + MachineMenuLayout.STATUS_PLAYER_INV_X,
                topPos + MachineMenuLayout.STATUS_PLAYER_INV_Y - 13, DIM, false);
        g.fill(leftPos + 48, topPos + MachineMenuLayout.STATUS_PLAYER_INV_Y - 5,
                leftPos + imageWidth - 48, topPos + MachineMenuLayout.STATUS_PLAYER_INV_Y - 4, 0x334DBAF4);
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            int color = i == 0 ? 0xFF132B3A : 0xFF252B39;
            drawSlotBg(g, leftPos + slot.x, topPos + slot.y, color);
        }
    }

    private void drawSlotBg(GuiGraphicsExtractor g, int x, int y, int color) {
        g.fill(x - 2, y - 2, x + 18, y + 18, 0xFF4C5668);
        g.fill(x - 1, y - 1, x + 17, y + 17, 0xFF111722);
        g.fill(x, y, x + 16, y + 16, color);
        g.fill(x, y, x + 16, y + 1, 0x665D708A);
        g.fill(x, y, x + 1, y + 16, 0x665D708A);
    }

    private String tr(String key) {
        return Component.translatable(key).getString();
    }

    private String fit(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;
        String suffix = "...";
        int suffixW = font.width(suffix);
        if (maxWidth <= suffixW) return font.plainSubstrByWidth(text, maxWidth);
        return font.plainSubstrByWidth(text, maxWidth - suffixW) + suffix;
    }

    private java.util.List<String> wrap(String text, int maxWidth, int maxLines) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        String current = "";
        for (String word : text.split(" ")) {
            String next = current.isEmpty() ? word : current + " " + word;
            if (font.width(next) <= maxWidth) {
                current = next;
                continue;
            }
            if (!current.isEmpty()) {
                lines.add(current);
                current = word;
            } else {
                String chunk = font.plainSubstrByWidth(word, maxWidth);
                lines.add(chunk);
                current = word.substring(Math.min(chunk.length(), word.length()));
            }
            if (lines.size() >= maxLines) {
                return trimLast(lines, maxWidth);
            }
        }
        if (!current.isEmpty()) {
            lines.add(current);
        }
        if (lines.size() > maxLines) {
            return trimLast(lines.subList(0, maxLines), maxWidth);
        }
        return lines;
    }

    private java.util.List<String> trimLast(java.util.List<String> source, int maxWidth) {
        java.util.List<String> lines = new java.util.ArrayList<>(source);
        if (!lines.isEmpty()) {
            int last = lines.size() - 1;
            lines.set(last, fit(lines.get(last) + "...", maxWidth));
        }
        return lines;
    }
}
