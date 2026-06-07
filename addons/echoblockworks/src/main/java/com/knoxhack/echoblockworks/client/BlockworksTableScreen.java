package com.knoxhack.echoblockworks.client;

import com.knoxhack.echocore.client.ui.EchoCyberGlassUi;
import com.knoxhack.echoblockworks.content.BlockworksBlockInfo;
import com.knoxhack.echoblockworks.menu.BlockworksTableMenu;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class BlockworksTableScreen extends AbstractContainerScreen<BlockworksTableMenu> {
   public BlockworksTableScreen(BlockworksTableMenu menu, Inventory inventory, Component title) {
      super(menu, inventory, title, BlockworksTableMenu.GUI_WIDTH, BlockworksTableMenu.GUI_HEIGHT);
      this.titleLabelX = 14;
      this.titleLabelY = 10;
      this.inventoryLabelX = BlockworksTableMenu.PLAYER_INV_X;
      this.inventoryLabelY = BlockworksTableMenu.PLAYER_INV_Y - 12;
   }

   @Override
   public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      int x = leftPos;
      int y = topPos;
      EchoCyberGlassUi.panel(graphics, x, y, imageWidth, imageHeight, 0xEE101417, 0xFF66E8FF);
      graphics.fill(x + 10, y + 30, x + imageWidth - 10, y + 111, 0xAA071014);

      for (Slot slot : menu.slots) {
         drawSlot(graphics, x + slot.x, y + slot.y);
      }

      drawVariantButtons(graphics, mouseX, mouseY);
      drawModeControls(graphics, mouseX, mouseY);
      super.extractContents(graphics, mouseX, mouseY, partialTick);
      drawFamilyText(graphics);
   }

   @Override
   protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
      graphics.text(this.font, fit(title.getString(), 180), titleLabelX, titleLabelY, 0x66E8FF, true);
      graphics.text(this.font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xD8F6FF, false);
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
      List<BlockworksBlockInfo> targets = menu.visibleTargets();
      for (int i = 0; i < targets.size(); i++) {
         int bx = leftPos + BlockworksTableMenu.VARIANT_BUTTON_X;
         int rowY = topPos + BlockworksTableMenu.VARIANT_BUTTON_Y + i * (BlockworksTableMenu.VARIANT_BUTTON_HEIGHT + 2);
         if (event.button() == 0
            && event.x() >= bx && event.x() < bx + BlockworksTableMenu.VARIANT_BUTTON_WIDTH
            && event.y() >= rowY && event.y() < rowY + BlockworksTableMenu.VARIANT_BUTTON_HEIGHT) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.gameMode != null) {
               minecraft.gameMode.handleInventoryButtonClick(menu.containerId, i);
            }
            return true;
         }
      }
      if (event.button() == 0 && clickPageButton(event.x(), event.y(), leftPos + BlockworksTableMenu.VARIANT_BUTTON_X, topPos + BlockworksTableMenu.PAGE_BUTTON_Y, true)) {
         return true;
      }
      if (event.button() == 0 && clickPageButton(event.x(), event.y(), leftPos + BlockworksTableMenu.VARIANT_BUTTON_X + BlockworksTableMenu.VARIANT_BUTTON_WIDTH - BlockworksTableMenu.PAGE_BUTTON_WIDTH, topPos + BlockworksTableMenu.PAGE_BUTTON_Y, false)) {
         return true;
      }
      if (event.button() == 0 && clickButton(event.x(), event.y(), leftPos + BlockworksTableMenu.MODE_BUTTON_X, topPos + BlockworksTableMenu.MODE_BUTTON_Y,
         BlockworksTableMenu.MODE_BUTTON_WIDTH, BlockworksTableMenu.VARIANT_BUTTON_HEIGHT, BlockworksTableMenu.BUTTON_TOGGLE_VIEW, true)) {
         return true;
      }
      if (event.button() == 0 && clickButton(event.x(), event.y(), leftPos + BlockworksTableMenu.MODE_BUTTON_X + 48, topPos + BlockworksTableMenu.MODE_BUTTON_Y,
         BlockworksTableMenu.KIT_BUTTON_WIDTH, BlockworksTableMenu.VARIANT_BUTTON_HEIGHT, BlockworksTableMenu.BUTTON_PREVIOUS_KIT, true)) {
         return true;
      }
      if (event.button() == 0 && clickButton(event.x(), event.y(), leftPos + BlockworksTableMenu.MODE_BUTTON_X + 70, topPos + BlockworksTableMenu.MODE_BUTTON_Y,
         BlockworksTableMenu.KIT_BUTTON_WIDTH, BlockworksTableMenu.VARIANT_BUTTON_HEIGHT, BlockworksTableMenu.BUTTON_NEXT_KIT, true)) {
         return true;
      }
      return super.mouseClicked(event, doubleClick);
   }

   private void drawVariantButtons(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
      List<BlockworksBlockInfo> targets = menu.visibleTargets();
      for (int i = 0; i < targets.size(); i++) {
         int bx = leftPos + BlockworksTableMenu.VARIANT_BUTTON_X;
         int by = topPos + BlockworksTableMenu.VARIANT_BUTTON_Y + i * (BlockworksTableMenu.VARIANT_BUTTON_HEIGHT + 2);
         boolean selected = i == menu.selectedVisibleIndex();
         boolean hover = mouseX >= bx && mouseX < bx + BlockworksTableMenu.VARIANT_BUTTON_WIDTH
            && mouseY >= by && mouseY < by + BlockworksTableMenu.VARIANT_BUTTON_HEIGHT;
         int frame = selected ? 0xFF66E8FF : hover ? 0xFF42515A : 0xFF27323A;
         int fill = selected ? 0xFF12333A : 0xFF101B20;
         graphics.fill(bx, by, bx + BlockworksTableMenu.VARIANT_BUTTON_WIDTH, by + BlockworksTableMenu.VARIANT_BUTTON_HEIGHT, fill);
         EchoCyberGlassUi.frame(graphics, bx, by, BlockworksTableMenu.VARIANT_BUTTON_WIDTH, BlockworksTableMenu.VARIANT_BUTTON_HEIGHT, frame);
         graphics.text(font, fit(targets.get(i).variant().displayName(), 112), bx + 5, by + 3, selected ? 0xFFFFFF : 0xB9DDE8, false);
      }
      drawPageButton(graphics, mouseX, mouseY, leftPos + BlockworksTableMenu.VARIANT_BUTTON_X, topPos + BlockworksTableMenu.PAGE_BUTTON_Y, "<", menu.hasPreviousPage(), BlockworksTableMenu.BUTTON_PREVIOUS_PAGE);
      drawPageButton(graphics, mouseX, mouseY, leftPos + BlockworksTableMenu.VARIANT_BUTTON_X + BlockworksTableMenu.VARIANT_BUTTON_WIDTH - BlockworksTableMenu.PAGE_BUTTON_WIDTH, topPos + BlockworksTableMenu.PAGE_BUTTON_Y, ">", menu.hasNextPage(), BlockworksTableMenu.BUTTON_NEXT_PAGE);
      String pageText = "Page " + (menu.selectedPage() + 1) + "/" + (BlockworksTableMenu.maxPage(menu.targets().size()) + 1);
      graphics.text(font, pageText, leftPos + BlockworksTableMenu.VARIANT_BUTTON_X + 38, topPos + BlockworksTableMenu.PAGE_BUTTON_Y + 4, 0x8CB8C2, false);
   }

   private void drawModeControls(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
      int x = leftPos + BlockworksTableMenu.MODE_BUTTON_X;
      int y = topPos + BlockworksTableMenu.MODE_BUTTON_Y;
      drawSmallButton(graphics, mouseX, mouseY, x, y, BlockworksTableMenu.MODE_BUTTON_WIDTH, menu.kitMode() ? "Kit" : "All", true);
      drawSmallButton(graphics, mouseX, mouseY, x + 48, y, BlockworksTableMenu.KIT_BUTTON_WIDTH, "<", menu.kitMode());
      drawSmallButton(graphics, mouseX, mouseY, x + 70, y, BlockworksTableMenu.KIT_BUTTON_WIDTH, ">", menu.kitMode());
      String kit = menu.activeKit().map(value -> value.displayName()).orElse("No Kits");
      int kitColor = menu.kitMode() ? (menu.kitFallbackActive() ? 0xFFCA66 : 0xB9DDE8) : 0x667982;
      graphics.text(font, fit(kit, 92), x + 94, y + 3, kitColor, false);
   }

   private void drawFamilyText(GuiGraphicsExtractor graphics) {
      int x = leftPos;
      int y = topPos;
      Component input = menu.inputInfo()
         .map(info -> Component.literal(info.family().displayName() + " / " + info.shape().displayName()).withStyle(ChatFormatting.AQUA))
         .orElse(Component.literal("Insert a Blockworks block").withStyle(ChatFormatting.GRAY));
      Component output = menu.selectedTarget()
         .map(info -> Component.literal(fit("Output: " + info.displayName() + " x1", 178)).withStyle(ChatFormatting.WHITE))
         .orElse(Component.literal("No conversion available").withStyle(ChatFormatting.DARK_GRAY));
      graphics.text(font, input, x + 18, y + 116, 0xD8F6FF, false);
      graphics.text(font, output, x + 18, y + 28, 0xD8F6FF, false);
      menu.selectedTarget().ifPresent(info -> graphics.text(font,
         Component.literal("Selected: " + fit(info.variant().displayName(), 92)).withStyle(ChatFormatting.GRAY),
         x + 18, y + 102, 0x8CB8C2, false));
      if (menu.kitMode()) {
         String status = menu.kitFallbackActive() ? "Kit: no family match" : "Kit: filtered palette";
         graphics.text(font, Component.literal(status).withStyle(menu.kitFallbackActive() ? ChatFormatting.GOLD : ChatFormatting.GRAY),
            x + 176, y + 116, menu.kitFallbackActive() ? 0xFFCA66 : 0x8CB8C2, false);
      }
      graphics.text(font, Component.literal("IN"), x + BlockworksTableMenu.INPUT_X + 3, y + 57, 0x8CB8C2, false);
      graphics.text(font, Component.literal("OUT"), x + BlockworksTableMenu.OUTPUT_X - 1, y + 57, 0x8CB8C2, false);
   }

   private void drawPageButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int x, int y, String label, boolean enabled, int id) {
      boolean hover = enabled && mouseX >= x && mouseX < x + BlockworksTableMenu.PAGE_BUTTON_WIDTH
         && mouseY >= y && mouseY < y + BlockworksTableMenu.VARIANT_BUTTON_HEIGHT;
      EchoCyberGlassUi.button(graphics, font, x, y, BlockworksTableMenu.PAGE_BUTTON_WIDTH, BlockworksTableMenu.VARIANT_BUTTON_HEIGHT,
         label, hover, enabled, 0xFF66E8FF);
   }

   private void drawSmallButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int x, int y, int width, String label, boolean enabled) {
      boolean hover = enabled && mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + BlockworksTableMenu.VARIANT_BUTTON_HEIGHT;
      EchoCyberGlassUi.button(graphics, font, x, y, width, BlockworksTableMenu.VARIANT_BUTTON_HEIGHT,
         fit(label, width - 6), hover, enabled, 0xFF66E8FF);
   }

   private boolean clickPageButton(double mouseX, double mouseY, int x, int y, boolean previous) {
      boolean enabled = previous ? menu.hasPreviousPage() : menu.hasNextPage();
      if (!enabled || mouseX < x || mouseX >= x + BlockworksTableMenu.PAGE_BUTTON_WIDTH
         || mouseY < y || mouseY >= y + BlockworksTableMenu.VARIANT_BUTTON_HEIGHT) {
         return false;
      }
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.gameMode != null) {
         minecraft.gameMode.handleInventoryButtonClick(menu.containerId, previous ? BlockworksTableMenu.BUTTON_PREVIOUS_PAGE : BlockworksTableMenu.BUTTON_NEXT_PAGE);
      }
      return true;
   }

   private boolean clickButton(double mouseX, double mouseY, int x, int y, int width, int height, int buttonId, boolean enabled) {
      if (!enabled || mouseX < x || mouseX >= x + width || mouseY < y || mouseY >= y + height) {
         return false;
      }
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.gameMode != null) {
         minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
      }
      return true;
   }

   private void drawSlot(GuiGraphicsExtractor graphics, int x, int y) {
      EchoCyberGlassUi.slot(graphics, x, y, 0xFF0B1116);
   }

   private String fit(String text, int maxWidth) {
      if (font.width(text) <= maxWidth) {
         return text;
      }
      String suffix = "...";
      int suffixWidth = font.width(suffix);
      if (maxWidth <= suffixWidth) {
         return font.plainSubstrByWidth(text, maxWidth);
      }
      return font.plainSubstrByWidth(text, maxWidth - suffixWidth) + suffix;
   }
}
