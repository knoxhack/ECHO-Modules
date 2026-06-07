package com.knoxhack.echoagriculturereclamation.client;

import com.knoxhack.echocore.client.ui.EchoCyberGlassUi;
import com.knoxhack.echoagriculturereclamation.menu.HydroponicTrayMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class HydroponicTrayScreen extends AbstractContainerScreen<HydroponicTrayMenu> {
   private static final int PANEL = EchoCyberGlassUi.color("reclamation.panel", 0xEE0B1114);
   private static final int FIELD = EchoCyberGlassUi.color("reclamation.field", 0xFF92F7A6);
   private static final int BLUE = EchoCyberGlassUi.color("reclamation.water", 0xFF73D7FF);
   private static final int AMBER = EchoCyberGlassUi.color("reclamation.nutrient", 0xFFFFD166);
   private static final int RED = EchoCyberGlassUi.color("reclamation.warning", 0xFFFF6B5D);
   private static final int TEXT = EchoCyberGlassUi.color("reclamation.text", 0xFFD8FBE4);
   private static final int MUTED = EchoCyberGlassUi.color("reclamation.muted", 0xFF8EA59B);

   public HydroponicTrayScreen(HydroponicTrayMenu menu, Inventory inventory, Component title) {
      super(menu, inventory, title, HydroponicTrayMenu.GUI_WIDTH, HydroponicTrayMenu.GUI_HEIGHT);
      titleLabelX = 16;
      titleLabelY = 13;
      inventoryLabelX = 38;
      inventoryLabelY = 106;
   }

   @Override
   public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      int x = leftPos;
      int y = topPos;
      EchoCyberGlassUi.panel(graphics, x, y, imageWidth, imageHeight, PANEL, FIELD);
      graphics.fill(x + 2, y + 2, x + imageWidth - 2, y + 38, 0xDD101A16);
      graphics.fill(x + 76, y + 52, x + 176, y + 98, 0x99070D0D);
      for (Slot slot : menu.slots) {
         EchoCyberGlassUi.slot(graphics, x + slot.x, y + slot.y, 0xFF0B1210);
      }
      drawMeters(graphics, x, y);
      super.extractContents(graphics, mouseX, mouseY, partialTick);
      drawReadouts(graphics, x, y);
   }

   @Override
   protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
      graphics.text(font, Component.literal("FIELD // Hydroponic Tray"), titleLabelX, titleLabelY, FIELD, true);
      graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);
   }

   private void drawMeters(GuiGraphicsExtractor graphics, int x, int y) {
      drawMeter(graphics, x + 84, y + 62, "Growth", menu.age(), 7, FIELD);
      drawMeter(graphics, x + 84, y + 77, "Cycle", menu.growthTicks(), menu.growthTicksMax(), BLUE);
      drawMeter(graphics, x + 84, y + 92, "Nutrient", menu.nutrient(), menu.nutrientCap(), AMBER);
   }

   private void drawReadouts(GuiGraphicsExtractor graphics, int x, int y) {
      graphics.text(font, Component.literal("SEED"), x + 27, y + 47, MUTED, false);
      graphics.text(font, Component.literal("NUTRIENT"), x + 19, y + 77, MUTED, false);
      graphics.text(font, Component.literal("OUTPUT"), x + 181, y + 63, MUTED, false);
      graphics.text(font, Component.literal(menu.statusLabel()), x + 84, y + 44, statusColor(), false);
      graphics.text(font, Component.literal("Stability " + menu.stability() + "% | Contam " + menu.contamination()), x + 84, y + 30, TEXT, false);
      graphics.text(font, Component.literal("Greenhouse safety " + menu.greenhouseSafety()), x + 84, y + 108, BLUE, false);
      graphics.text(font, Component.literal(fit(menu.statusLine(), 196)), x + 20, y + 190, statusColor(), false);
   }

   private void drawMeter(GuiGraphicsExtractor graphics, int x, int y, String label, int value, int max, int color) {
      int width = 78;
      int safeMax = Math.max(1, max);
      int filled = Math.max(0, Math.min(width, value * width / safeMax));
      graphics.text(font, Component.literal(label), x, y - 8, MUTED, false);
      EchoCyberGlassUi.meter(graphics, x + 52, y - 1, width, 7, filled, color);
      graphics.text(font, Component.literal(value + "/" + safeMax), x + 134, y - 8, TEXT, false);
   }

   private int statusColor() {
      return switch (menu.statusLabel()) {
         case "GROWING" -> BLUE;
         case "NUTRIENT LOW" -> RED;
         case "READY" -> FIELD;
         default -> AMBER;
      };
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
