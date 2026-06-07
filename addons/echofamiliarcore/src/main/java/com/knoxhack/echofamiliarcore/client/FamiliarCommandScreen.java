package com.knoxhack.echofamiliarcore.client;

import com.knoxhack.echocore.client.ui.EchoCyberGlassUi;
import com.knoxhack.echofamiliarcore.entity.ArcanaFamiliarEntity;
import com.knoxhack.echofamiliarcore.menu.FamiliarCommandMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class FamiliarCommandScreen extends AbstractContainerScreen<FamiliarCommandMenu> {
    private static final int PANEL = 0xEE071019;
    private static final int ACCENT = 0xFF79FFD7;
    private static final int SECONDARY = 0xFF86B8FF;
    private static final int TEXT = 0xFFE8FBFF;
    private static final int DIM = 0xFF8AA5B8;

    public FamiliarCommandScreen(FamiliarCommandMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, FamiliarCommandMenu.GUI_WIDTH, FamiliarCommandMenu.GUI_HEIGHT);
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
        graphics.fill(x + 14, y + 42, x + imageWidth - 14, y + 118, 0x88070D14);
        drawStatus(graphics, x, y);
        drawButtons(graphics, x, y, mouseX, mouseY);
        super.extractContents(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, Component.literal("FAMILIARCORE // COMMAND LINK"), titleLabelX, titleLabelY, ACCENT, true);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int x = leftPos;
        int y = topPos;
        for (int i = 0; i < 4; i++) {
            if (clickButton(event, x + 18 + i * 54, y + 128, 48, 18, i, true)) {
                return true;
            }
        }
        if (clickButton(event, x + 18, y + 150, 74, 18, FamiliarCommandMenu.BUTTON_TRAIN, true)) {
            return true;
        }
        if (clickButton(event, x + 96, y + 150, 42, 18, FamiliarCommandMenu.BUTTON_UPGRADE_ATTUNEMENT,
                menu.upgradePoints() > 0 && menu.attunementRank() < 3)) {
            return true;
        }
        if (clickButton(event, x + 142, y + 150, 42, 18, FamiliarCommandMenu.BUTTON_UPGRADE_WARDING,
                menu.upgradePoints() > 0 && menu.wardingRank() < 3)) {
            return true;
        }
        if (clickButton(event, x + 188, y + 150, 42, 18, FamiliarCommandMenu.BUTTON_UPGRADE_SCOUTING,
                menu.upgradePoints() > 0 && menu.scoutingRank() < 3)) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void drawStatus(GuiGraphicsExtractor graphics, int x, int y) {
        String kind = menu.kind() == ArcanaFamiliarEntity.KIND_SPIRIT_DRONE ? "Spirit Drone"
                : menu.kind() == ArcanaFamiliarEntity.KIND_AETHER_WISP ? "Aether Wisp" : "No active familiar";
        int xpWidth = Math.max(0, Math.min(132, menu.bondXp() * 132 / menu.nextXp()));
        graphics.text(font, kind + " // " + FamiliarCommandMenu.commandName(menu.command()),
                x + 24, y + 52, TEXT, false);
        graphics.text(font, "Bond " + menu.bondLevel() + "   XP " + menu.bondXp() + "/" + menu.nextXp(),
                x + 24, y + 72, ACCENT, false);
        EchoCyberGlassUi.meter(graphics, x + 24, y + 90, 132, 8, xpWidth, ACCENT);
        graphics.text(font, "Integrity " + menu.healthPercent() + "%", x + 24, y + 104,
                menu.healthPercent() < 35 ? 0xFFFF8C8C : SECONDARY, false);
        graphics.text(font, "Evolution " + evolutionName(menu.evolutionTier()),
                x + 164, y + 64, SECONDARY, false);
        graphics.text(font, "Form " + formName(menu.evolutionFormCode()),
                x + 164, y + 80, DIM, false);
        graphics.text(font, "Power " + menu.evolutionPower() + "   Pts " + menu.upgradePoints(),
                x + 164, y + 96, DIM, false);
        graphics.text(font, "Skill " + abilityName(menu.evolutionAbilityCode()),
                x + 164, y + 110, DIM, false);
        graphics.text(font, "A" + menu.attunementRank() + " W" + menu.wardingRank() + " S" + menu.scoutingRank(),
                x + 164, y + 122, DIM, false);
    }

    private void drawButtons(GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY) {
        drawButton(graphics, x + 18, y + 128, 48, 18, "FOLLOW", mouseX, mouseY, true);
        drawButton(graphics, x + 72, y + 128, 48, 18, "STAY", mouseX, mouseY, true);
        drawButton(graphics, x + 126, y + 128, 48, 18, "SCOUT", mouseX, mouseY, true);
        drawButton(graphics, x + 180, y + 128, 48, 18, "DEFEND", mouseX, mouseY, true);
        drawButton(graphics, x + 18, y + 150, 74, 18, "TRAIN", mouseX, mouseY, true);
        drawButton(graphics, x + 96, y + 150, 42, 18, "ATT", mouseX, mouseY,
                menu.upgradePoints() > 0 && menu.attunementRank() < 3);
        drawButton(graphics, x + 142, y + 150, 42, 18, "WARD", mouseX, mouseY,
                menu.upgradePoints() > 0 && menu.wardingRank() < 3);
        drawButton(graphics, x + 188, y + 150, 42, 18, "SCAN", mouseX, mouseY,
                menu.upgradePoints() > 0 && menu.scoutingRank() < 3);
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

    private static String evolutionName(int tier) {
        return switch (tier) {
            case 5 -> "Mythic";
            case 4 -> "Ascended";
            case 3 -> "Bound";
            case 2 -> "Trusted";
            case 1 -> "Awakened";
            default -> "Dormant";
        };
    }

    private static String formName(int code) {
        return switch (code) {
            case 2 -> "Ward";
            case 3 -> "Scout";
            default -> "Attune";
        };
    }

    private static String abilityName(int code) {
        return switch (code) {
            case 6 -> "Sweep";
            case 5 -> "Guard";
            case 4 -> "Uplink";
            case 3 -> "Trace";
            case 2 -> "Ward";
            default -> "Bloom";
        };
    }
}
