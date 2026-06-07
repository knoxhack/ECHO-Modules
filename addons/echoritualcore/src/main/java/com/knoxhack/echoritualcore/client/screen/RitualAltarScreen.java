package com.knoxhack.echoritualcore.client.screen;

import com.knoxhack.echocore.client.ui.EchoCyberGlassUi;
import com.knoxhack.echoritualcore.block.entity.BasicAltarBlockEntity;
import com.knoxhack.echoritualcore.menu.RitualAltarMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class RitualAltarScreen extends AbstractContainerScreen<RitualAltarMenu> {
    private static final int PANEL = 0xEE0A0F16;
    private static final int ACCENT = 0xFFB072FF;
    private static final int TEXT = 0xFFEDE8FF;
    private static final int DIM = 0xFF9E90B8;
    private static final int GOOD = 0xFF70E6A5;
    private static final int WARNING = 0xFFFFC24B;
    private static final int DANGER = 0xFFFF6E8A;

    public RitualAltarScreen(RitualAltarMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, RitualAltarMenu.GUI_WIDTH, RitualAltarMenu.GUI_HEIGHT);
        this.titleLabelX = 16;
        this.titleLabelY = 14;
        this.inventoryLabelX = 9999;
        this.inventoryLabelY = 9999;
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int x = leftPos;
        int y = topPos;
        EchoCyberGlassUi.panel(graphics, x, y, imageWidth, imageHeight, PANEL, accentColor());
        graphics.fill(x + 2, y + 2, x + imageWidth - 2, y + 42, 0xDD101820);
        graphics.fill(x + 18, y + 56, x + imageWidth - 18, y + imageHeight - 22, 0x99071014);
        drawStatus(graphics, x, y);
        drawMeters(graphics, x, y);
        super.extractContents(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, Component.literal("RITUALCORE // BASIC ALTAR"), titleLabelX, titleLabelY, accentColor(), true);
    }

    private void drawStatus(GuiGraphicsExtractor graphics, int x, int y) {
        String state = menu.structureReady() ? "ARRAY READY" : "ARRAY INCOMPLETE";
        graphics.text(font, Component.literal(state), x + 24, y + 58, menu.structureReady() ? GOOD : WARNING, false);
        graphics.text(font, Component.literal(resultName(menu.result())), x + 24, y + 74, resultColor(menu.result()), false);
        graphics.text(font, Component.literal("Rune circuit " + menu.runes() + "/" + RitualCoreMenuNumbers.REQUIRED_RUNES),
                x + 24, y + 104, TEXT, false);
        graphics.text(font, Component.literal("Pedestals " + menu.pedestals() + " | Pylons " + menu.pylons()
                + " | Augments " + menu.augments()), x + 24, y + 120, TEXT, false);
        String missing = menu.missing() == 0 ? "No missing structure anchors." : "Missing anchors: " + menu.missing();
        graphics.text(font, Component.literal(missing), x + 24, y + 136, menu.missing() == 0 ? GOOD : WARNING, false);
    }

    private void drawMeters(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.text(font, Component.literal("Stability " + menu.stability() + "%"), x + 154, y + 88,
                menu.stability() >= 70 ? GOOD : menu.stability() >= 40 ? WARNING : DANGER, false);
        EchoCyberGlassUi.meter(graphics, x + 154, y + 104, 92, 8, Math.max(0, Math.min(92, menu.stability() * 92 / 100)),
                menu.stability() >= 70 ? GOOD : menu.stability() >= 40 ? WARNING : DANGER);
        graphics.text(font, Component.literal("Use pedestals for inputs."), x + 154, y + 126, DIM, false);
        graphics.text(font, Component.literal("Lens scan shows exact gaps."), x + 154, y + 140, DIM, false);
    }

    private int accentColor() {
        return menu.structureReady() ? ACCENT : WARNING;
    }

    private static String resultName(int result) {
        return switch (result) {
            case BasicAltarBlockEntity.RESULT_READY -> "Last result: ready";
            case BasicAltarBlockEntity.RESULT_COMPLETE -> "Last result: complete";
            case BasicAltarBlockEntity.RESULT_WARNING -> "Last result: warning";
            case BasicAltarBlockEntity.RESULT_FAILURE -> "Last result: failure";
            default -> "Last result: idle";
        };
    }

    private static int resultColor(int result) {
        return switch (result) {
            case BasicAltarBlockEntity.RESULT_COMPLETE, BasicAltarBlockEntity.RESULT_READY -> GOOD;
            case BasicAltarBlockEntity.RESULT_WARNING -> WARNING;
            case BasicAltarBlockEntity.RESULT_FAILURE -> DANGER;
            default -> DIM;
        };
    }

    private static final class RitualCoreMenuNumbers {
        private static final int REQUIRED_RUNES = 4;
    }
}
