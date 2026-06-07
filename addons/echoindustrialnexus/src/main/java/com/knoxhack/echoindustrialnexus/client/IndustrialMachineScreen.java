package com.knoxhack.echoindustrialnexus.client;

import com.knoxhack.echocore.client.ui.EchoCyberGlassUi;
import com.knoxhack.echoindustrialnexus.block.IndustrialMachineBlock;
import com.knoxhack.echoindustrialnexus.block.entity.IndustrialMachineBlockEntity;
import com.knoxhack.echoindustrialnexus.menu.IndustrialMachineMenu;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class IndustrialMachineScreen extends AbstractContainerScreen<IndustrialMachineMenu> {
   private static final int PANEL = 0xEE0D1215;
   private static final int PANEL_SOFT = 0xAA141D22;
   private static final int STEEL = 0xFF34434B;
   private static final int CYAN = 0xFF66E8FF;
   private static final int AMBER = 0xFFFF9F3D;
   private static final int RED = 0xFFFF5D4D;
   private static final int GREEN = 0xFF64D97B;

   public IndustrialMachineScreen(IndustrialMachineMenu menu, Inventory inventory, Component title) {
      super(menu, inventory, title, IndustrialMachineMenu.GUI_WIDTH, IndustrialMachineMenu.GUI_HEIGHT);
      this.titleLabelX = 16;
      this.titleLabelY = 14;
      this.inventoryLabelX = IndustrialMachineMenu.PLAYER_INV_X;
      this.inventoryLabelY = IndustrialMachineMenu.PLAYER_INV_Y - 13;
   }

   @Override
   public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      int x = this.leftPos;
      int y = this.topPos;
      EchoCyberGlassUi.panel(graphics, x, y, this.imageWidth, this.imageHeight, PANEL, CYAN);
      graphics.fill(x + 2, y + 2, x + this.imageWidth - 2, y + 42, 0xDD101820);
      graphics.fill(x + 18, y + 56, x + this.imageWidth - 18, y + 212, 0x99071014);
      graphics.fill(x + 48, y + IndustrialMachineMenu.PLAYER_INV_Y - 5, x + this.imageWidth - 48,
         y + IndustrialMachineMenu.PLAYER_INV_Y - 4, 0x3366E8FF);

      for (int i = 0; i < menu.slots.size(); i++) {
         Slot slot = menu.slots.get(i);
         int fill = i == IndustrialMachineBlockEntity.OUTPUT_SLOT || i == IndustrialMachineBlockEntity.BYPRODUCT_SLOT ? 0xFF122820 : 0xFF0B1116;
         drawSlot(graphics, x + slot.x, y + slot.y, fill);
      }

      drawBar(graphics, x + 118, y + 88, 112, 8, progressPixels(112), GREEN);
      drawBar(graphics, x + 118, y + 112, 112, 8, fluxPixels(112), CYAN);
      drawBar(graphics, x + 118, y + 136, 112, 8, heatPixels(112), heatColor());
      drawBar(graphics, x + 244, y + 150, 72, 7, fluidPixels(menu.inputFluidAmount(), 72), 0xFF4FB8FF);
      drawBar(graphics, x + 244, y + 165, 72, 7, fluidPixels(menu.outputFluidAmount(), 72), 0xFF8AF6B6);

      drawButton(graphics, x + 30, y + 184, 70, 18, "MODE", mouseX, mouseY, !menu.remoteShutdown());
      drawButton(graphics, x + 106, y + 184, 70, 18, "SIDES", mouseX, mouseY, true);
      drawButton(graphics, x + 182, y + 184, 70, 18, menu.remoteShutdown() ? "RESUME" : "STOP", mouseX, mouseY, true);
      drawButton(graphics, x + 258, y + 184, 70, 18, "NET STOP", mouseX, mouseY, menu.machineKind().factoryController());

      super.extractContents(graphics, mouseX, mouseY, partialTick);
      drawReadouts(graphics, x, y);
      drawHoverHelp(graphics, x, y, mouseX, mouseY);
   }

   @Override
   protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
      graphics.text(this.font, Component.literal(fit("ECHO INDUSTRIAL INTERFACE // " + title.getString(), 238)), this.titleLabelX, this.titleLabelY, CYAN, true);
      graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xD8F6FF, false);
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
      int x = this.leftPos;
      int y = this.topPos;
      if (clickButton(event, x + 30, y + 184, 70, 18, IndustrialMachineMenu.BUTTON_CYCLE_SCRUBBER)) {
         return true;
      }
      if (clickButton(event, x + 106, y + 184, 70, 18, IndustrialMachineMenu.BUTTON_CYCLE_SIDE_CONFIG)) {
         return true;
      }
      if (clickButton(event, x + 182, y + 184, 70, 18, IndustrialMachineMenu.BUTTON_TOGGLE_SHUTDOWN)) {
         return true;
      }
      if (menu.machineKind().factoryController()
         && clickButton(event, x + 258, y + 184, 70, 18, IndustrialMachineMenu.BUTTON_CONTROLLER_SHUTDOWN)) {
         return true;
      }
      return super.mouseClicked(event, doubleClick);
   }

   private void drawReadouts(GuiGraphicsExtractor graphics, int x, int y) {
      IndustrialMachineBlock.MachineKind kind = menu.machineKind();
      String status = IndustrialMachineBlockEntity.MachineStatus.byId(menu.statusId()).label();
      graphics.text(this.font, Component.literal(fit(status, 282)), x + 30, y + 58, colorForStatus(), false);
      graphics.text(this.font, Component.literal("IN"), x + IndustrialMachineMenu.INPUT_X + 3, y + 75, 0x99C4D0, false);
      graphics.text(this.font, Component.literal("CAT"), x + IndustrialMachineMenu.CATALYST_X, y + 75, 0x99C4D0, false);
      graphics.text(this.font, Component.literal("OUT"), x + IndustrialMachineMenu.OUTPUT_X, y + 75, 0x99C4D0, false);
      graphics.text(this.font, Component.literal("BY"), x + IndustrialMachineMenu.BYPRODUCT_X + 2, y + 75, 0x99C4D0, false);
      graphics.text(this.font, Component.literal("UPGRADES"), x + IndustrialMachineMenu.UPGRADE_X, y + 136, 0x99C4D0, false);
      graphics.text(this.font, Component.literal("Progress " + menu.progress() + "/" + Math.max(1, menu.maxProgress())), x + 118, y + 76, 0xD8F6FF, false);
      graphics.text(this.font, Component.literal("Thermal Flux " + menu.flux() + "/" + Math.max(1, menu.maxFlux())), x + 118, y + 100, 0xD8F6FF, false);
      graphics.text(this.font, Component.literal("Heat " + heatTier() + " (" + menu.heat() + "%)"), x + 118, y + 124, heatColor(), false);
      graphics.text(this.font, Component.literal(fit("Input " + fluidName(menu.inputFluidId()) + " " + menu.inputFluidAmount() + " mB", 98)), x + 244, y + 138, 0xBFEFFF, false);
      graphics.text(this.font, Component.literal(fit("Output " + fluidName(menu.outputFluidId()) + " " + menu.outputFluidAmount() + " mB", 98)), x + 244, y + 153, 0xBFEFFF, false);
      graphics.text(this.font, Component.literal("Sides " + IndustrialMachineBlockEntity.sideConfigLabel(menu.sideConfigId())), x + 30, y + 166, 0xD8F6FF, false);
      graphics.text(this.font, Component.literal(sideDiagram()), x + 30, y + 154, 0x99C4D0, false);
      String controller = kind.factoryController()
         ? "Linked " + menu.linkedCount() + " | Alerts " + menu.alertCount()
         : "Warnings " + menu.alertCount() + (menu.remoteShutdown() ? " | Remote shutdown" : "");
      graphics.text(this.font, Component.literal(fit(controller, 230)), x + 118, y + 166, menu.alertCount() > 0 ? AMBER : 0xD8F6FF, false);
      graphics.text(this.font, Component.literal(fit(warningHint(), 296)), x + 30, y + 207, menu.alertCount() > 0 ? AMBER : 0x99C4D0, false);
      graphics.text(this.font, Component.literal(fit(routeHint(kind), 296)), x + 30, y + 218, 0x99C4D0, false);
      if (kind == IndustrialMachineBlock.MachineKind.INDUSTRIAL_SCRUBBER) {
         graphics.text(this.font, Component.literal("Scrubber " + IndustrialMachineBlockEntity.scrubberModeLabel(menu.scrubberModeId())),
            x + 30, y + 112, 0xD8F6FF, false);
      }
   }

   private boolean clickButton(MouseButtonEvent event, int x, int y, int w, int h, int id) {
      if (event.button() != 0 || !inside(event.x(), event.y(), x, y, w, h)) {
         return false;
      }
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.gameMode != null) {
         minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
      }
      return true;
   }

   private void drawButton(GuiGraphicsExtractor graphics, int x, int y, int w, int h, String label, int mouseX, int mouseY, boolean enabled) {
      EchoCyberGlassUi.button(graphics, this.font, x, y, w, h, label, inside(mouseX, mouseY, x, y, w, h), enabled, CYAN);
   }

   private void drawHoverHelp(GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY) {
      List<String> lines = null;
      if (inside(mouseX, mouseY, x + 30, y + 184, 70, 18)) {
         lines = List.of("Mode", "Cycles scrubber mode when available.", "Other machines keep their current process mode.");
      } else if (inside(mouseX, mouseY, x + 106, y + 184, 70, 18)) {
         lines = List.of("Side configuration", sideDiagram(), "Controls hopper and duct insert/extract faces.");
      } else if (inside(mouseX, mouseY, x + 182, y + 184, 70, 18)) {
         lines = List.of(menu.remoteShutdown() ? "Resume machine" : "Stop machine", "Toggles local remote shutdown.", "Stopped machines keep inventory and buffers.");
      } else if (inside(mouseX, mouseY, x + 258, y + 184, 70, 18)) {
         lines = List.of("Network stop", "Factory Controllers stop linked Flux-network machines.", "Rescans before applying shutdown.");
      } else if (inside(mouseX, mouseY, x + 244, y + 150, 72, 7)) {
         lines = List.of("Input tank", fluidName(menu.inputFluidId()), menu.inputFluidAmount() + " / " + IndustrialMachineBlockEntity.FLUID_TANK_CAPACITY + " mB");
      } else if (inside(mouseX, mouseY, x + 244, y + 165, 72, 7)) {
         lines = List.of("Output tank", fluidName(menu.outputFluidId()), menu.outputFluidAmount() + " / " + IndustrialMachineBlockEntity.FLUID_TANK_CAPACITY + " mB");
      } else if (inside(mouseX, mouseY, x + 118, y + 136, 112, 8)) {
         lines = List.of("Heat", heatTier() + " (" + menu.heat() + "%)", warningHint());
      } else if (inside(mouseX, mouseY, x + 30, y + 207, 296, 22)) {
         lines = List.of("Route and warning hint", warningHint(), routeHint(menu.machineKind()));
      }
      if (lines != null) {
         drawTooltipPanel(graphics, mouseX, mouseY, lines);
      }
   }

   private void drawTooltipPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY, List<String> lines) {
      EchoCyberGlassUi.tooltipPanel(graphics, this.font, mouseX, mouseY, this.leftPos, this.topPos, this.imageWidth,
         this.imageHeight, lines);
   }

   private void drawSlot(GuiGraphicsExtractor graphics, int x, int y, int fill) {
      EchoCyberGlassUi.slot(graphics, x, y, fill);
   }

   private void drawBar(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int filled, int color) {
      EchoCyberGlassUi.meter(graphics, x, y, w, h, filled, color);
   }

   private void frame(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int color) {
      EchoCyberGlassUi.frame(graphics, x, y, w, h, color);
   }

   private int progressPixels(int width) {
      int max = menu.maxProgress();
      return max <= 0 ? 0 : Math.min(width, menu.progress() * width / max);
   }

   private int fluxPixels(int width) {
      return Math.min(width, menu.flux() * width / Math.max(1, menu.maxFlux()));
   }

   private int heatPixels(int width) {
      return Math.min(width, menu.heat() * width / 100);
   }

   private int fluidPixels(int amount, int width) {
      return Math.min(width, Math.max(0, amount) * width / IndustrialMachineBlockEntity.FLUID_TANK_CAPACITY);
   }

   private int heatColor() {
      return menu.heat() >= 90 ? RED : menu.heat() >= 65 ? AMBER : CYAN;
   }

   private int colorForStatus() {
      return menu.remoteShutdown() || menu.statusId() == IndustrialMachineBlockEntity.MachineStatus.MELTDOWN.ordinal()
         ? RED
         : menu.alertCount() > 0 ? AMBER : 0xD8F6FF;
   }

   private String heatTier() {
      int heat = menu.heat();
      if (heat >= 96) {
         return "Meltdown";
      }
      if (heat >= 85) {
         return "Critical";
      }
      if (heat >= 65) {
         return "Hot";
      }
      if (heat >= 30) {
         return "Warm";
      }
      return "Cool";
   }

   private String fluidName(int id) {
      return id <= 0 ? "Empty" : IndustrialMachineBlockEntity.fluidLabel(id);
   }

   private String warningHint() {
      IndustrialMachineBlockEntity.MachineStatus status = IndustrialMachineBlockEntity.MachineStatus.byId(menu.statusId());
      return switch (status) {
         case CHARGING -> "Cause: low Thermal Flux or disconnected Flux duct.";
         case OUTPUT_BLOCKED -> "Cause: item output/byproduct slot blocked.";
         case FLUID_OUTPUT_BLOCKED -> "Cause: output tank is full or mixed with another fluid.";
         case CATALYST_REQUIRED -> "Cause: required catalyst/auxiliary item missing.";
         case NEXUS_CONTAMINATION -> "Cause: unstable Nexus material in a non-stabilized process.";
         case CRITICAL_HEAT, MELTDOWN -> "Cause: overheat risk. Add coolant, heat sinks, or shutdown module.";
         case REMOTE_SHUTDOWN -> "Cause: local or controller remote shutdown.";
         default -> menu.alertCount() > 0 ? "Cause: inspect heat, outputs, fluids, and Nexus-safe inputs." : "No active warning cause.";
      };
   }

   private String routeHint(IndustrialMachineBlock.MachineKind kind) {
      return switch (kind) {
         case SCRAP_DYNAMO -> "Route: fuel -> Scrap Dynamo -> Copper Flux Duct -> Ore Grinder.";
         case FILTER_PRESS -> "Route: Ash Powder + Membrane + Iron Nuggets -> Filter Press -> Gas Mask Filters.";
         case ALLOY_KILN -> "Route: Iron Dust + Copper Dust + Heat Coil -> Alloy Kiln -> Dense Alloy.";
         default -> "Tip: Smart Duct filters are set on-duct; wrench toggles whitelist/blacklist.";
      };
   }

   private String sideDiagram() {
      return switch (menu.sideConfigId()) {
         case 1 -> "Faces: all IN, no auto OUT";
         case 2 -> "Faces: all OUT, no auto IN";
         case 3 -> "Faces: automation locked";
         default -> "Faces: top/side IN, bottom OUT";
      };
   }

   private boolean inside(double px, double py, int x, int y, int w, int h) {
      return px >= x && px < x + w && py >= y && py < y + h;
   }

   private String fit(String text, int maxWidth) {
      if (font.width(text) <= maxWidth) {
         return text;
      }
      String suffix = "...";
      int suffixW = font.width(suffix);
      if (maxWidth <= suffixW) {
         return font.plainSubstrByWidth(text, maxWidth);
      }
      return font.plainSubstrByWidth(text, maxWidth - suffixW) + suffix;
   }
}
