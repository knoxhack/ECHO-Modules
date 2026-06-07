package com.knoxhack.echoarmory.client;

import com.knoxhack.echoarmory.block.entity.ArmoryStationBlockEntity;
import com.knoxhack.echoarmory.menu.ArmoryStationMenu;
import com.knoxhack.echocore.client.ui.EchoCyberGlassUi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class ArmoryStationScreen extends AbstractContainerScreen<ArmoryStationMenu> {
   private static final int PANEL = 0xEE0C1016;
   private static final int CYAN = 0xFF66E8FF;
   private static final int GREEN = 0xFF64D97B;
   private static final int AMBER = 0xFFFFD166;
   private static final int RED = 0xFFFF5D4D;

   public ArmoryStationScreen(ArmoryStationMenu menu, Inventory inventory, Component title) {
      super(menu, inventory, title, ArmoryStationMenu.GUI_WIDTH, ArmoryStationMenu.GUI_HEIGHT);
      this.titleLabelX = 16;
      this.titleLabelY = 14;
      this.inventoryLabelX = 97;
      this.inventoryLabelY = 137;
   }

   @Override
   public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      int x = leftPos;
      int y = topPos;
      EchoCyberGlassUi.panel(graphics, x, y, imageWidth, imageHeight, PANEL, CYAN);
      graphics.fill(x + 2, y + 2, x + imageWidth - 2, y + 42, 0xDD101820);
      graphics.fill(x + 24, y + 54, x + imageWidth - 24, y + 134, 0x99071014);

      for (Slot slot : menu.slots) {
         drawSlot(graphics, x + slot.x, y + slot.y);
      }

      drawBar(graphics, x + 170, y + 68, 120, 8, Math.min(120, menu.progress() * 120 / 80), GREEN);
      drawBar(graphics, x + 170, y + 92, 120, 8, Math.min(120, menu.energy() * 120 / Math.max(1, menu.energyCapacity())), CYAN);
      drawBar(graphics, x + 170, y + 116, 120, 8, Math.min(120, menu.instability() * 120 / 100), menu.instability() > 65 ? RED : AMBER);

      drawButton(graphics, x + 28, y + 184, 76, 18, "SCAN", mouseX, mouseY, true);
      drawButton(graphics, x + 112, y + 184, 76, 18, "APPLY", mouseX, mouseY, true);
      drawButton(graphics, x + 196, y + 184, 76, 18, "STANCE", mouseX, mouseY, true);

      super.extractContents(graphics, mouseX, mouseY, partialTick);
      drawReadouts(graphics, x, y);
   }

   @Override
   protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
      graphics.text(font, Component.literal(fit("ECHO ARMORY // " + menu.kind().displayName(), 250)), titleLabelX, titleLabelY, CYAN, true);
      graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xD8F6FF, false);
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
      int x = leftPos;
      int y = topPos;
      if (clickButton(event, x + 28, y + 184, 76, 18, ArmoryStationMenu.BUTTON_SCAN)) {
         return true;
      }
      if (clickButton(event, x + 112, y + 184, 76, 18, ArmoryStationMenu.BUTTON_APPLY)) {
         return true;
      }
      if (clickButton(event, x + 196, y + 184, 76, 18, ArmoryStationMenu.BUTTON_CYCLE)) {
         return true;
      }
      return super.mouseClicked(event, doubleClick);
   }

   private void drawReadouts(GuiGraphicsExtractor graphics, int x, int y) {
      graphics.text(font, Component.literal("GEAR"), x + 41, y + 58, 0x99C4D0, false);
      graphics.text(font, Component.literal("MOD"), x + 78, y + 58, 0x99C4D0, false);
      graphics.text(font, Component.literal("AUX"), x + 115, y + 58, 0x99C4D0, false);
      graphics.text(font, Component.literal("Progress " + menu.progress()), x + 170, y + 56, 0xD8F6FF, false);
      graphics.text(font, Component.literal("Energy " + menu.energy() + "/" + menu.energyCapacity()), x + 170, y + 80, 0xD8F6FF, false);
      graphics.text(font, Component.literal("Instability " + menu.instability() + "%"), x + 170, y + 104, menu.instability() > 65 ? RED : 0xD8F6FF, false);
      graphics.text(font, Component.literal("Installed modules " + menu.moduleCount()), x + 44, y + 101, 0xD8F6FF, false);
      graphics.text(font, Component.literal("Preview " + (menu.previewAccepted() ? "READY" : "BLOCKED") + " | Fuel " + menu.fuelCount()),
         x + 28, y + 208, menu.previewAccepted() ? GREEN : AMBER, false);
      graphics.text(font, Component.literal(fit(actionHint(), 290)), x + 28, y + 222, 0x99C4D0, false);
   }

   private String actionHint() {
      return switch (menu.kind()) {
         case MODULE_UPGRADE_TABLE -> "Apply installs the module from MOD into the gear slot.";
         case WEAPON_FORGE -> "Apply upgrades weapon gear using the AUX material.";
         case ARMOR_FORGE -> "Apply upgrades armor gear using the AUX material.";
         case ENERGY_CORE_CHARGING_STATION -> "Apply recharges the gear energy core.";
         case SIGIL_ENGRAVER -> "Apply engraves AUX as a cosmetic sigil.";
         case VEIL_INFUSER -> "Apply installs fracture or void modules.";
         case CONSTRUCT_DOCK -> "Apply installs construct support modules.";
         case LOADOUT_TERMINAL -> "Apply binds gear to the operator loadout.";
         case WEAPON_RACK, ARMOR_STAND -> "Apply stages this item for nearby route readiness.";
         default -> "Apply tunes gear and initializes modular telemetry.";
      };
   }

   private boolean clickButton(MouseButtonEvent event, int x, int y, int w, int h, int id) {
      if (event.button() != 0 || !inside(event.x(), event.y(), x, y, w, h)) {
         return false;
      }
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.gameMode != null) {
         minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
      }
      return true;
   }

   private void drawButton(GuiGraphicsExtractor graphics, int x, int y, int w, int h, String label, int mouseX, int mouseY, boolean enabled) {
      EchoCyberGlassUi.button(graphics, font, x, y, w, h, label, inside(mouseX, mouseY, x, y, w, h), enabled, CYAN);
   }

   private void drawSlot(GuiGraphicsExtractor graphics, int x, int y) {
      EchoCyberGlassUi.slot(graphics, x, y, 0xFF0B1116);
   }

   private void drawBar(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int filled, int color) {
      EchoCyberGlassUi.meter(graphics, x, y, w, h, filled, color);
   }

   private void frame(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int color) {
      EchoCyberGlassUi.frame(graphics, x, y, w, h, color);
   }

   private boolean inside(double px, double py, int x, int y, int w, int h) {
      return px >= x && px < x + w && py >= y && py < y + h;
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
