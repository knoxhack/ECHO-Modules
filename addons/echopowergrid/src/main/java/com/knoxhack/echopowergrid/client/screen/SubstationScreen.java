package com.knoxhack.echopowergrid.client.screen;

import com.knoxhack.echocore.client.ui.EchoCyberGlassUi;
import com.knoxhack.echopowergrid.EchoPowerGrid;
import com.knoxhack.echopowergrid.menu.SubstationMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class SubstationScreen extends AbstractContainerScreen<SubstationMenu> {
    private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath(EchoPowerGrid.MODID, "textures/gui/substation.png");
    private static final int PANEL = 0xEE0C1116;
    private static final int CYAN = 0xFF00E5FF;
    private static final int ORANGE = 0xFFFF6A00;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GRAY = 0xFF999999;

    public SubstationScreen(SubstationMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 256, 180);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int x = leftPos;
        int y = topPos;
        EchoCyberGlassUi.panel(graphics, x, y, imageWidth, imageHeight, PANEL, CYAN);

        x += 12;
        y += 12;
        graphics.text(this.font, Component.literal("ECHO GRID // Outpost Substation"), x, y, CYAN, false);
        y += 18;

        graphics.text(this.font, Component.literal("Generation: " + menu.getGeneration() + " EP/t"), x, y, WHITE, false);
        y += 14;
        graphics.text(this.font, Component.literal("Demand: " + menu.getDemand() + " EP/t"), x, y, WHITE, false);
        y += 14;
        graphics.text(this.font, Component.literal("Stored: " + menu.getStored() + "/" + menu.getCapacity() + " EP"), x, y, WHITE, false);
        y += 14;
        graphics.text(this.font, Component.literal("State: " + stateName(menu.getState())), x, y, stateColor(menu.getState()), false);
        y += 14;
        graphics.text(this.font, Component.literal("Nodes: " + menu.getNodeCount()), x, y, GRAY, false);
        y += 14;
        graphics.text(this.font, Component.literal("Policy: " + policyName(menu.getPolicy())), x, y, CYAN, false);
    }

    private static String stateName(int state) {
        return switch (state) {
            case 0 -> "OFFLINE";
            case 1 -> "STABLE";
            case 2 -> "CHARGING";
            case 3 -> "DISCHARGING";
            case 4 -> "BROWNOUT";
            case 5 -> "OVERLOADED";
            case 6 -> "TRIPPED";
            case 7 -> "EMERGENCY";
            default -> "UNKNOWN";
        };
    }

    private static int stateColor(int state) {
        return switch (state) {
            case 1, 2 -> 0xFF00FF00;
            case 3 -> 0xFFFFFF00;
            case 4 -> 0xFFFFA500;
            case 5, 6, 7 -> 0xFFFF0000;
            default -> GRAY;
        };
    }

    private static String policyName(int policy) {
        return switch (policy) {
            case 1 -> "LIFE_SUPPORT_FIRST";
            case 2 -> "INDUSTRIAL_FIRST";
            case 3 -> "NEXUS_STABILIZATION";
            case 4 -> "MANUAL";
            default -> "BALANCED";
        };
    }
}
