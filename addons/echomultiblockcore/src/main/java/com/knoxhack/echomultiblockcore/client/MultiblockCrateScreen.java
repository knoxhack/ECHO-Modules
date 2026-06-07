package com.knoxhack.echomultiblockcore.client;

import com.knoxhack.echocore.client.ui.EchoCyberGlassUi;
import com.knoxhack.echomultiblockcore.menu.MultiblockCrateMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class MultiblockCrateScreen extends AbstractContainerScreen<MultiblockCrateMenu> {
    private static final int PANEL = 0xEE071018;
    private static final int CYAN = 0xFF66E8FF;

    public MultiblockCrateScreen(MultiblockCrateMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, MultiblockCrateMenu.GUI_WIDTH, MultiblockCrateMenu.GUI_HEIGHT);
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelY = 10000;
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int x = leftPos;
        int y = topPos;
        EchoCyberGlassUi.panel(graphics, x, y, imageWidth, imageHeight, PANEL, CYAN);
        super.extractContents(graphics, mouseX, mouseY, partialTick);
    }
}
