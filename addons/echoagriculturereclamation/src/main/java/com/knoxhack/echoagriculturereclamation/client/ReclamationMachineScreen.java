package com.knoxhack.echoagriculturereclamation.client;

import com.knoxhack.echoagriculturereclamation.block.entity.ReclamationMachineBlockEntity;
import com.knoxhack.echoagriculturereclamation.menu.ReclamationMachineMenu;
import com.knoxhack.echocore.client.ui.EchoCyberGlassUi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class ReclamationMachineScreen extends AbstractContainerScreen<ReclamationMachineMenu> {
   private static final int PANEL = EchoCyberGlassUi.color("reclamation.panel", 0xEE0B1114);
   private static final int FIELD = EchoCyberGlassUi.color("reclamation.field", 0xFF92F7A6);
   private static final int BLUE = EchoCyberGlassUi.color("reclamation.water", 0xFF73D7FF);
   private static final int AMBER = EchoCyberGlassUi.color("reclamation.nutrient", 0xFFFFD166);
   private static final int RED = EchoCyberGlassUi.color("reclamation.warning", 0xFFFF6B5D);
   private static final int TEXT = EchoCyberGlassUi.color("reclamation.text", 0xFFD8FBE4);
   private static final int MUTED = EchoCyberGlassUi.color("reclamation.muted", 0xFF8EA59B);

   public ReclamationMachineScreen(ReclamationMachineMenu menu, Inventory inventory, Component title) {
      super(menu, inventory, title, ReclamationMachineMenu.GUI_WIDTH, ReclamationMachineMenu.GUI_HEIGHT);
      titleLabelX = 16;
      titleLabelY = 13;
      inventoryLabelX = 62;
      inventoryLabelY = 112;
   }

   @Override
   public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      int x = leftPos;
      int y = topPos;
      EchoCyberGlassUi.panel(graphics, x, y, imageWidth, imageHeight, PANEL, FIELD);
      graphics.fill(x + 2, y + 2, x + imageWidth - 2, y + 40, 0xDD101A16);
      graphics.fill(x + 24, y + 54, x + imageWidth - 24, y + 106, 0x99070D0D);
      for (Slot slot : menu.slots) {
         drawSlot(graphics, x + slot.x, y + slot.y);
      }
      drawProgress(graphics, x + 112, y + 77);
      drawButton(graphics, x + 26, y + 208, 70, 18, "SCAN", mouseX, mouseY, true);
      drawButton(graphics, x + 104, y + 208, 70, 18, "RUN", mouseX, mouseY, true);
      drawButton(graphics, x + 182, y + 208, 70, 18, "RECALL", mouseX, mouseY, menu.recallVisible());
      super.extractContents(graphics, mouseX, mouseY, partialTick);
      drawReadouts(graphics, x, y);
   }

   @Override
   protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
      graphics.text(font, Component.literal(fit("FIELD // " + menu.kind().displayName(), 230)), titleLabelX, titleLabelY, FIELD, true);
      graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
      int x = leftPos;
      int y = topPos;
      if (clickButton(event, x + 26, y + 208, 70, 18, ReclamationMachineMenu.BUTTON_SCAN, true)) {
         return true;
      }
      if (clickButton(event, x + 104, y + 208, 70, 18, ReclamationMachineMenu.BUTTON_RUN, true)) {
         return true;
      }
      if (clickButton(event, x + 182, y + 208, 70, 18, ReclamationMachineMenu.BUTTON_RECALL, menu.recallVisible())) {
         return true;
      }
      return super.mouseClicked(event, doubleClick);
   }

   private void drawReadouts(GuiGraphicsExtractor graphics, int x, int y) {
      graphics.text(font, Component.literal("INPUT"), x + 38, y + 61, MUTED, false);
      graphics.text(font, Component.literal("CAT"), x + 78, y + 61, MUTED, false);
      graphics.text(font, Component.literal("OUTPUT"), x + 199, y + 61, MUTED, false);
      graphics.text(font, Component.literal("AUX"), x + 242, y + 61, MUTED, false);
      graphics.text(font, Component.literal(fit(menu.processTitle(), 200)), x + 52, y + 43, BLUE, false);
      graphics.text(font, Component.literal(statusLabel()), x + 112, y + 61, statusColor(), false);
      graphics.text(font, Component.literal(fit(menu.statusLine(), 250)), x + 26, y + 152, statusColor(), false);
      graphics.text(font, Component.literal(fit(menu.nextAction(), 250)), x + 26, y + 166, MUTED, false);
      graphics.text(font, Component.literal("Power " + (menu.powered() ? "assisted" : "standalone") + " | Output " + menu.outputCount()),
         x + 26, y + 184, TEXT, false);
   }

   private void drawProgress(GuiGraphicsExtractor graphics, int x, int y) {
      int width = 76;
      int max = Math.max(1, menu.progressMax());
      int filled = Math.max(0, Math.min(width, menu.progress() * width / max));
      EchoCyberGlassUi.meter(graphics, x, y, width, 8, filled, statusColor());
   }

   private String statusLabel() {
      return switch (menu.status()) {
         case ReclamationMachineBlockEntity.STATUS_ACTIVE -> "PROCESSING " + menu.progress() + "/" + Math.max(1, menu.progressMax());
         case ReclamationMachineBlockEntity.STATUS_BLOCKED -> "BLOCKED";
         case ReclamationMachineBlockEntity.STATUS_COMPLETE -> "READY RESULT";
         default -> "READY";
      };
   }

   private int statusColor() {
      return switch (menu.status()) {
         case ReclamationMachineBlockEntity.STATUS_ACTIVE -> BLUE;
         case ReclamationMachineBlockEntity.STATUS_BLOCKED -> RED;
         case ReclamationMachineBlockEntity.STATUS_COMPLETE -> FIELD;
         default -> AMBER;
      };
   }

   private boolean clickButton(MouseButtonEvent event, int x, int y, int w, int h, int id, boolean enabled) {
      if (!enabled || event.button() != 0 || !inside(event.x(), event.y(), x, y, w, h)) {
         return false;
      }
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.gameMode != null) {
         minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
      }
      return true;
   }

   private void drawButton(GuiGraphicsExtractor graphics, int x, int y, int w, int h, String label, int mouseX, int mouseY, boolean enabled) {
      EchoCyberGlassUi.button(graphics, font, x, y, w, h, label, mouseX, mouseY, enabled);
   }

   private void drawSlot(GuiGraphicsExtractor graphics, int x, int y) {
      EchoCyberGlassUi.slot(graphics, x, y, 0xFF0B1210);
   }

   private void frame(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int color) {
      EchoCyberGlassUi.frame(graphics, x, y, w, h, color);
   }

   private boolean inside(double px, double py, int x, int y, int w, int h) {
      return px >= x && px < x + w && py >= y && py < y + h;
   }

   private String fit(String text, int maxWidth) {
      if (text == null) {
         return "";
      }
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
