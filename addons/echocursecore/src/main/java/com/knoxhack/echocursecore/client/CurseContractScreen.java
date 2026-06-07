package com.knoxhack.echocursecore.client;

import com.knoxhack.echocore.client.ui.EchoCyberGlassUi;
import com.knoxhack.echocursecore.menu.CurseContractMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class CurseContractScreen extends AbstractContainerScreen<CurseContractMenu> {
    private static final int PANEL = 0xEE12070C;
    private static final int ACCENT = 0xFFFF6E8A;
    private static final int WARNING = 0xFFFFC35A;
    private static final int TEXT = 0xFFFFEEF2;
    private static final int DIM = 0xFFB08A96;

    public CurseContractScreen(CurseContractMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, CurseContractMenu.GUI_WIDTH, CurseContractMenu.GUI_HEIGHT);
        this.titleLabelX = 14;
        this.titleLabelY = 12;
        this.inventoryLabelX = 9999;
        this.inventoryLabelY = 9999;
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int x = leftPos;
        int y = topPos;
        EchoCyberGlassUi.panel(graphics, x, y, imageWidth, imageHeight, PANEL, ACCENT);
        graphics.fill(x + 16, y + 42, x + imageWidth - 16, y + 120, 0x99190912);
        drawStatus(graphics, x, y);
        drawButtons(graphics, x, y, mouseX, mouseY);
        super.extractContents(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, Component.literal("CURSECORE // CONTRACT LEDGER"), titleLabelX, titleLabelY, ACCENT, true);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int x = leftPos;
        int y = topPos;
        if (clickButton(event, x + 20, y + 132, 48, 18, CurseContractMenu.BUTTON_CLEANSE, menu.cleanseableCount() > 0)) {
            return true;
        }
        if (clickButton(event, x + 74, y + 132, 48, 18, CurseContractMenu.BUTTON_PAY_DEBT, menu.contractDebt() > 0)) {
            return true;
        }
        if (clickButton(event, x + 128, y + 132, 48, 18, CurseContractMenu.BUTTON_ACCEPT_BLOOD_DEBT, true)) {
            return true;
        }
        if (clickButton(event, x + 182, y + 132, 38, 18, CurseContractMenu.BUTTON_ACCEPT_VOID_MARK, true)) {
            return true;
        }
        if (clickButton(event, x + 224, y + 132, 28, 18, CurseContractMenu.BUTTON_BREAK_CONTRACT,
                menu.contractCount() > 0)) {
            return true;
        }
        if (clickButton(event, x + 20, y + 154, 48, 18, CurseContractMenu.BUTTON_CLEANSE_ECHO_ROT,
                menu.echoRotStage() > 0)) {
            return true;
        }
        if (clickButton(event, x + 74, y + 154, 48, 18, CurseContractMenu.BUTTON_CLEANSE_BLOOD_DEBT,
                menu.bloodDebtStage() > 0)) {
            return true;
        }
        if (clickButton(event, x + 128, y + 154, 48, 18, CurseContractMenu.BUTTON_CLEANSE_VOID_MARK,
                menu.voidMarkStage() > 0)) {
            return true;
        }
        if (clickButton(event, x + 182, y + 154, 70, 18, CurseContractMenu.BUTTON_SEVER_CONTRACT,
                menu.severReadyCount() > 0)) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void drawStatus(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.text(font, "Active " + menu.activeCount() + "   Contracts " + menu.contractCount()
                + "   Cleanseable " + menu.cleanseableCount(), x + 24, y + 54, TEXT, false);
        graphics.text(font, "Echo Rot " + menu.echoRotStage(), x + 24, y + 78,
                menu.echoRotStage() > 0 ? WARNING : DIM, false);
        graphics.text(font, "Blood Debt " + menu.bloodDebtStage(), x + 24, y + 94,
                menu.bloodDebtStage() > 0 ? ACCENT : DIM, false);
        graphics.text(font, "Void Mark " + menu.voidMarkStage(), x + 132, y + 94,
                menu.voidMarkStage() > 0 ? ACCENT : DIM, false);
        graphics.text(font, "Debt " + menu.contractDebt(), x + 132, y + 110,
                menu.contractDebt() > 0 ? WARNING : DIM, false);
        int pressure = Math.max(menu.contractResistance(), Math.min(100, menu.activeCount() * 18 + menu.contractCount() * 24));
        EchoCyberGlassUi.meter(graphics, x + 132, y + 76, 88, 8, pressure * 88 / 100,
                pressure >= 60 ? ACCENT : WARNING);
        graphics.text(font, "Ready " + menu.cleansingReadiness() + "%",
                x + 132, y + 62, menu.cleansingReadiness() > 45 ? ACCENT : DIM, false);
        graphics.text(font, "Plan " + planName(menu.cleansingPlanCode()) + " "
                        + targetName(menu.cleansingPlanTargetCode()) + "   Sever " + menu.severReadyCount(),
                x + 24, y + 110, menu.severReadyCount() > 0 ? ACCENT : DIM, false);
    }

    private void drawButtons(GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY) {
        drawButton(graphics, x + 20, y + 132, 48, 18, "CLEAN", mouseX, mouseY, menu.cleanseableCount() > 0);
        drawButton(graphics, x + 74, y + 132, 48, 18, "PAY", mouseX, mouseY, menu.contractDebt() > 0);
        drawButton(graphics, x + 128, y + 132, 48, 18, "BLOOD", mouseX, mouseY, true);
        drawButton(graphics, x + 182, y + 132, 38, 18, "VOID", mouseX, mouseY, true);
        drawButton(graphics, x + 224, y + 132, 28, 18, "END", mouseX, mouseY, menu.contractCount() > 0);
        drawButton(graphics, x + 20, y + 154, 48, 18, "ECHO", mouseX, mouseY, menu.echoRotStage() > 0);
        drawButton(graphics, x + 74, y + 154, 48, 18, "B-CLEAN", mouseX, mouseY, menu.bloodDebtStage() > 0);
        drawButton(graphics, x + 128, y + 154, 48, 18, "V-CLEAN", mouseX, mouseY, menu.voidMarkStage() > 0);
        drawButton(graphics, x + 182, y + 154, 70, 18, "SEVER", mouseX, mouseY,
                menu.severReadyCount() > 0);
        graphics.text(font, "FORBIDDEN LEDGER // CONFIRMATION TRACE ACTIVE", x + 24, y + 184, DIM, false);
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

    private static String planName(int code) {
        return switch (code) {
            case 3 -> "SEVER";
            case 2 -> "PAY";
            case 1 -> "CLEANSE";
            default -> "WATCH";
        };
    }

    private static String targetName(int code) {
        return switch (code) {
            case 4 -> "OTHER";
            case 3 -> "VOID";
            case 2 -> "BLOOD";
            case 1 -> "ECHO";
            default -> "NONE";
        };
    }
}
