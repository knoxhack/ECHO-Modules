package com.knoxhack.echoconvoyprotocol.client;

import com.knoxhack.echocore.client.ui.EchoCyberGlassUi;
import com.knoxhack.echoconvoyprotocol.menu.ConvoyUpgradeMenu;
import com.knoxhack.echoconvoyprotocol.upgrade.ConvoyUpgradeSlot;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class ConvoyUpgradeScreen extends AbstractContainerScreen<ConvoyUpgradeMenu> {
   public ConvoyUpgradeScreen(ConvoyUpgradeMenu menu, Inventory inventory, Component title) {
      super(menu, inventory, title, ConvoyUpgradeMenu.GUI_WIDTH, ConvoyUpgradeMenu.GUI_HEIGHT);
      this.titleLabelX = 16;
      this.titleLabelY = 12;
      this.inventoryLabelX = 58;
      this.inventoryLabelY = 117;
   }

   @Override
   public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      int x = leftPos;
      int y = topPos;
      EchoCyberGlassUi.panel(graphics, x, y, imageWidth, imageHeight, 0xEE101511, 0xFF66E8FF);
      graphics.fill(x, y + 34, x + imageWidth, y + 36, 0xFF183340);
      graphics.fill(x + 18, y + 46, x + 132, y + 102, 0xAA071018);
      graphics.fill(x + 150, y + 46, x + imageWidth - 18, y + 102, 0xAA071018);
      for (Slot slot : menu.slots) {
         drawSlot(graphics, x + slot.x, y + slot.y);
      }
      super.extractContents(graphics, mouseX, mouseY, partialTick);
      drawStatus(graphics, x, y);
      drawStats(graphics, x, y);
   }

   @Override
   protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
      graphics.text(font, Component.translatable("screen.echoconvoyprotocol.vehicle_upgrades.title"), titleLabelX, titleLabelY, 0x66E8FF, true);
      for (ConvoyUpgradeSlot slot : ConvoyUpgradeSlot.values()) {
         graphics.text(font, slotLabel(slot), 25 + slot.ordinal() * 30, 52, 0xD8F6FF, false);
      }
      graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xD8F6FF, false);
   }

   private void drawStatus(GuiGraphicsExtractor graphics, int x, int y) {
      Component status = menu.hasVehicle()
         ? Component.translatable("screen.echoconvoyprotocol.vehicle_upgrades.vehicle", menu.vehicleKind().displayName())
         : Component.translatable("screen.echoconvoyprotocol.vehicle_upgrades.no_vehicle");
      graphics.text(font, status, x + 16, y + 24, menu.hasVehicle() ? 0x92D66B : 0xFFD166, false);
   }

   private void drawStats(GuiGraphicsExtractor graphics, int x, int y) {
      int sx = x + 160;
      int sy = y + 52;
      graphics.text(font, Component.translatable("screen.echoconvoyprotocol.vehicle_upgrades.stats"), sx, sy, 0x66E8FF, false);
      graphics.text(font, Component.literal("Fuel " + menu.fuel() + "/" + menu.maxFuel()), sx, sy + 12, 0xD8F6FF, false);
      graphics.text(font, Component.literal("Battery " + menu.battery() + "/" + menu.maxBattery()), sx, sy + 24, 0xD8F6FF, false);
      graphics.text(font, Component.literal("Damage " + menu.damage() + "/" + menu.maxDamage()), sx, sy + 36, 0xD8F6FF, false);
      graphics.text(font, Component.literal("Cargo " + menu.cargo() + "/" + menu.maxCargo()), sx, sy + 48, 0xD8F6FF, false);
      graphics.text(font, Component.literal("Scan " + menu.scannerRange() + "m  Speed " + String.format(Locale.ROOT, "%.3f", menu.speed())), sx, sy + 60, 0xD8F6FF, false);
      graphics.text(font, Component.literal("Turn " + String.format(Locale.ROOT, "%.1f", menu.turnRate()) + "  Hazard " + menu.hazardReductionPercent() + "%"), sx, sy + 72, 0xD8F6FF, false);
   }

   private void drawSlot(GuiGraphicsExtractor graphics, int x, int y) {
      EchoCyberGlassUi.slot(graphics, x, y, 0xFF0B1217);
   }

   private static Component slotLabel(ConvoyUpgradeSlot slot) {
      return Component.translatable("screen.echoconvoyprotocol.vehicle_upgrades.slot." + slot.getSerializedName());
   }
}
