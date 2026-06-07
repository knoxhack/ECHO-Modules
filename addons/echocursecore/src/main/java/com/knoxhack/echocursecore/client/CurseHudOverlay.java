package com.knoxhack.echocursecore.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class CurseHudOverlay {
    private static final int PANEL = 0xAA10070E;
    private static final int BORDER = 0xAAFF5F93;
    private static final int TEXT = 0xFFEDE6FF;
    private static final int DIM = 0xFFB58FA8;
    private static final int WARNING = 0xFFFFC35A;
    private static final int ECHO_ROT = 0xFFFF6AF3;
    private static final int GLASS = 0xFF9BFFD9;
    private static final int RIFT = 0xFF7D79FF;
    private static final int SOUL = 0xFF80FFD1;
    private static final int PHANTOM = 0xFFFFA24A;
    private static final int BLOOD = 0xFFFF5465;
    private static final int VOID = 0xFFB394FF;

    private CurseHudOverlay() {
    }

    public static void render(GuiGraphicsExtractor graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || minecraft.screen != null
                || !CurseHudClientState.hasActiveCurse()) {
            return;
        }
        Font font = minecraft.font;
        int width = 224;
        int rows = activeRows();
        int statusRows = (CurseHudClientState.contractCount() > 0 ? 1 : 0)
                + (CurseHudClientState.cleanseableCount() > 0 ? 1 : 0);
        int height = 24 + rows * 14 + statusRows * 12;
        int x = 12;
        int y = minecraft.getWindow().getGuiScaledHeight() - 112 - height;
        graphics.fill(x, y, x + width, y + height, PANEL);
        graphics.outline(x, y, width, height, BORDER);
        graphics.text(font, "CurseCore // live symptom trace", x + 8, y + 6,
                CurseHudClientState.recentlySynced() ? BORDER : DIM, false);
        int rowY = y + 20;
        rowY = drawCurse(graphics, font, x + 8, rowY, "Echo Rot", CurseHudClientState.echoRotStage(), ECHO_ROT);
        rowY = drawCurse(graphics, font, x + 8, rowY, "Glass Veins", CurseHudClientState.glassVeinsStage(), GLASS);
        rowY = drawCurse(graphics, font, x + 8, rowY, "Rift Hunger", CurseHudClientState.riftHungerStage(), RIFT);
        rowY = drawCurse(graphics, font, x + 8, rowY, "Soul Static", CurseHudClientState.soulStaticStage(), SOUL);
        rowY = drawCurse(graphics, font, x + 8, rowY, "Phantom Burn", CurseHudClientState.phantomBurnStage(), PHANTOM);
        rowY = drawCurse(graphics, font, x + 8, rowY, "Blood Debt", CurseHudClientState.bloodDebtStage(), BLOOD);
        rowY = drawCurse(graphics, font, x + 8, rowY, "Void Mark", CurseHudClientState.voidMarkStage(), VOID);
        if (CurseHudClientState.contractCount() > 0) {
            graphics.text(font, "contracts " + CurseHudClientState.contractCount() + " // cleansing constrained",
                    x + 8, rowY + 1, WARNING, false);
            rowY += 12;
        }
        if (CurseHudClientState.cleanseableCount() > 0) {
            graphics.text(font, "cleanseable vectors " + CurseHudClientState.cleanseableCount(),
                    x + 8, rowY + 1, 0xFF70E6A5, false);
        }
    }

    private static int activeRows() {
        int rows = 0;
        rows += CurseHudClientState.echoRotStage() > 0 ? 1 : 0;
        rows += CurseHudClientState.glassVeinsStage() > 0 ? 1 : 0;
        rows += CurseHudClientState.riftHungerStage() > 0 ? 1 : 0;
        rows += CurseHudClientState.soulStaticStage() > 0 ? 1 : 0;
        rows += CurseHudClientState.phantomBurnStage() > 0 ? 1 : 0;
        rows += CurseHudClientState.bloodDebtStage() > 0 ? 1 : 0;
        rows += CurseHudClientState.voidMarkStage() > 0 ? 1 : 0;
        return rows;
    }

    private static int drawCurse(GuiGraphicsExtractor graphics, Font font, int x, int y,
            String name, int stage, int color) {
        if (stage <= 0) {
            return y;
        }
        graphics.text(font, name + " " + stage, x, y, color, false);
        for (int i = 0; i < 5; i++) {
            int sx = x + 108 + i * 12;
            graphics.fill(sx, y + 2, sx + 8, y + 8, i < stage ? color : 0x55332234);
            graphics.outline(sx, y + 2, 8, 6, 0x66FFFFFF);
        }
        if (stage >= 4) {
            graphics.text(font, "critical", x + 140, y, 0xFFFFC35A, false);
        }
        return y + 14;
    }
}
