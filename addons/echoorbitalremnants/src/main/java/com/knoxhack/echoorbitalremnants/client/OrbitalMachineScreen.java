package com.knoxhack.echoorbitalremnants.client;

import com.knoxhack.echocore.client.ui.EchoCyberGlassUi;
import com.knoxhack.echoorbitalremnants.block.OrbitalMachineBlock.MachineKind;
import com.knoxhack.echoorbitalremnants.block.entity.OrbitalMachineBlockEntity;
import com.knoxhack.echoorbitalremnants.block.entity.OrbitalMachineBlockEntity.MachineStatus;
import com.knoxhack.echoorbitalremnants.menu.OrbitalMachineMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class OrbitalMachineScreen extends AbstractContainerScreen<OrbitalMachineMenu> {
    public OrbitalMachineScreen(OrbitalMachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, OrbitalMachineMenu.GUI_WIDTH, OrbitalMachineMenu.GUI_HEIGHT);
        this.titleLabelX = 16;
        this.titleLabelY = 14;
        this.inventoryLabelX = OrbitalMachineMenu.PLAYER_INV_X;
        this.inventoryLabelY = OrbitalMachineMenu.PLAYER_INV_Y - 13;
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int x = this.leftPos;
        int y = this.topPos;
        if (!OrbitalMachineGuiSkins.render(graphics, menu.machineKind(), x, y, this.imageWidth, this.imageHeight)) {
            graphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xEE101820);
        } else {
            graphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0x55101820);
        }
        EchoCyberGlassUi.frame(graphics, x, y, this.imageWidth, this.imageHeight, 0xFF58D7FF);
        graphics.fill(x + 2, y + 2, x + this.imageWidth - 2, y + 40, 0xDD101820);
        graphics.fill(x + 2, y + 40, x + this.imageWidth - 2, y + 43, 0xFF58D7FF);
        graphics.fill(x + 18, y + 54, x + this.imageWidth - 18, y + 206, 0xAA071014);

        for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            drawSlot(graphics, x + slot.x, y + slot.y, i == OrbitalMachineBlockEntity.OUTPUT_SLOT ? 0xFF122820 : 0xFF0B1116);
        }

        EchoCyberGlassUi.meter(graphics, x + 118, y + 98, 112, 8, progressPixels(112), 0xFF57D68D);
        EchoCyberGlassUi.meter(graphics, x + 118, y + 120, 112, 8, chargePixels(112), 0xFF66E8FF);
        graphics.fill(x + 48, y + OrbitalMachineMenu.PLAYER_INV_Y - 5,
                x + this.imageWidth - 48, y + OrbitalMachineMenu.PLAYER_INV_Y - 4, 0x3358D7FF);
        super.extractContents(graphics, mouseX, mouseY, partialTick);
        drawStatusText(graphics, x, y);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(this.font, fit(title.getString(), 214), this.titleLabelX, this.titleLabelY, 0x66E8FF, true);
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xD8F6FF, false);
    }

    private void drawStatusText(GuiGraphicsExtractor graphics, int x, int y) {
        MachineKind kind = menu.machineKind();
        String status = MachineStatus.byId(menu.statusId()).label();
        if (kind == MachineKind.ROCKET_ASSEMBLY_FRAME) {
            status = menu.assemblyReadiness().ready() ? "Assembly ready" : "Assembly incomplete";
        } else if (kind == MachineKind.NAVIGATION_CONSOLE) {
            status = "Station signal: weak | Debris: high";
        } else if (kind == MachineKind.STATION_LIFE_SUPPORT_CORE) {
            status = "Life-support telemetry stable";
        } else if (MachineStatus.byId(menu.statusId()) == MachineStatus.BAD_INPUT) {
            status = "Input rejected: no matching orbital recipe";
        } else if (MachineStatus.byId(menu.statusId()) == MachineStatus.OUTPUT_BLOCKED) {
            status = "Output blocked: clear or stack the result slot";
        } else if (MachineStatus.byId(menu.statusId()) == MachineStatus.CHARGING) {
            status = "Charging internal systems";
        } else if (MachineStatus.byId(menu.statusId()) == MachineStatus.COMPLETE) {
            status = "Cycle complete";
        }
        graphics.text(this.font, Component.literal(fit(status, 284)), x + 28, y + 154, 0xD8F6FF, false);
        graphics.text(this.font, Component.literal(fit("Charge " + menu.charge() + "/" + Math.max(1, menu.maxCharge()), 148)),
                x + 118, y + 132, 0x88F4FF, false);
        if (kind.processingRecipeDriven()) {
            graphics.text(this.font, Component.literal("IN"), x + OrbitalMachineMenu.INPUT_X + 3, y + 76, 0x8CB8C2, false);
            graphics.text(this.font, Component.literal("OUT"), x + OrbitalMachineMenu.OUTPUT_X, y + 76, 0x8CB8C2, false);
        }
        if (kind == MachineKind.ROCKET_ASSEMBLY_FRAME && !menu.assemblyReadiness().ready()) {
            int offset = 0;
            for (Component missing : menu.assemblyReadiness().missing().stream().limit(3).toList()) {
                graphics.text(this.font, Component.literal(fit(missing.getString(), 178)), x + 28, y + 124 + offset, 0xFFD166, false);
                offset += 12;
            }
        }
    }

    private void drawSlot(GuiGraphicsExtractor graphics, int x, int y, int fill) {
        EchoCyberGlassUi.slot(graphics, x, y, fill);
    }

    private int progressPixels(int width) {
        int max = menu.maxProgress();
        return max <= 0 ? 0 : Math.min(width, menu.progress() * width / max);
    }

    private int chargePixels(int width) {
        int max = Math.max(1, menu.maxCharge());
        return Math.min(width, menu.charge() * width / max);
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
