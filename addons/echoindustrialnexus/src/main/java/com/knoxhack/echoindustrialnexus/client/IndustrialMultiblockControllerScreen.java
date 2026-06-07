package com.knoxhack.echoindustrialnexus.client;

import com.knoxhack.echocore.client.ui.EchoCyberGlassUi;
import com.knoxhack.echomultiblockcore.api.MultiblockState;
import com.knoxhack.echomultiblockcore.client.MultiblockClientPackets;
import com.knoxhack.echomultiblockcore.network.AutomationRecipeMetadataPacket;
import com.knoxhack.echoindustrialnexus.menu.IndustrialMultiblockControllerMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class IndustrialMultiblockControllerScreen extends AbstractContainerScreen<IndustrialMultiblockControllerMenu> {
   private static final int PANEL = 0xEE0C1116;
   private static final int PANEL_SOFT = 0xAA142027;
   private static final int STEEL = 0xFF34444C;
   private static final int CYAN = 0xFF66E8FF;
   private static final int AMBER = 0xFFFFA23F;
   private static final int RED = 0xFFFF5D4D;
   private static final int GREEN = 0xFF64D97B;

   public IndustrialMultiblockControllerScreen(IndustrialMultiblockControllerMenu menu, Inventory inventory, Component title) {
      super(menu, inventory, title, IndustrialMultiblockControllerMenu.GUI_WIDTH, IndustrialMultiblockControllerMenu.GUI_HEIGHT);
      this.titleLabelX = 16;
      this.titleLabelY = 12;
      this.inventoryLabelX = 0;
      this.inventoryLabelY = 0;
   }

   @Override
   public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      int x = this.leftPos;
      int y = this.topPos;
      EchoCyberGlassUi.panel(graphics, x, y, this.imageWidth, this.imageHeight, PANEL, CYAN);
      graphics.fill(x + 2, y + 2, x + this.imageWidth - 2, y + 38, 0xDD101820);
      graphics.fill(x + 18, y + 52, x + 180, y + 208, 0x99070D11);
      graphics.fill(x + 194, y + 52, x + this.imageWidth - 18, y + 208, 0x99070D11);

      drawStatusPanel(graphics, x, y, mouseX, mouseY);
      drawTaskPanel(graphics, x, y, mouseX, mouseY);

      drawButton(graphics, x + 18, y + 218, 92, 18, menu.formed() ? "REVALIDATE" : "FORM", mouseX, mouseY, true);
      drawButton(graphics, x + 118, y + 218, 82, 18, "CLEAR", mouseX, mouseY, menu.taskCount() > 0);
      drawButton(graphics, x + 208, y + 218, 70, 18, "RETRY", mouseX, mouseY, menu.blockedTaskIndex() >= 0);
      drawButton(graphics, x + 286, y + 218, 72, 18,
         menu.logisticsAvailable() ? (menu.logisticsRestockEnabled() ? "AUTO ON" : "AUTO OFF") : "NO LOGI",
         mouseX, mouseY, menu.logisticsAvailable());

      super.extractContents(graphics, mouseX, mouseY, partialTick);
      drawHoverHelp(graphics, x, y, mouseX, mouseY);
   }

   @Override
   protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
      graphics.text(this.font, Component.literal(fit("ECHO FACTORY OPS // " + menu.titleLine(), 316)),
         this.titleLabelX, this.titleLabelY, CYAN, true);
      graphics.text(this.font, Component.literal(fit(menu.controllerPos().toShortString(), 112)),
         this.imageWidth - 130, this.titleLabelY, 0x99C4D0, false);
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
      int x = this.leftPos;
      int y = this.topPos;
      if (clickButton(event, x + 18, y + 218, 92, 18, IndustrialMultiblockControllerMenu.BUTTON_FORM_OR_REVALIDATE, true)) {
         return true;
      }
      if (clickButton(event, x + 118, y + 218, 82, 18, IndustrialMultiblockControllerMenu.BUTTON_CLEAR_QUEUE, menu.taskCount() > 0)) {
         return true;
      }
      if (clickButton(event, x + 208, y + 218, 70, 18, IndustrialMultiblockControllerMenu.BUTTON_RETRY_BLOCKED, menu.blockedTaskIndex() >= 0)) {
         return true;
      }
      if (clickButton(event, x + 286, y + 218, 72, 18, IndustrialMultiblockControllerMenu.BUTTON_TOGGLE_LOGISTICS_RESTOCK, menu.logisticsAvailable())) {
         return true;
      }
      if (clickButton(event, x + 28, y + 198, 58, 16, IndustrialMultiblockControllerMenu.BUTTON_REQUEST_LOGISTICS_RESTOCK_NOW, menu.logisticsAvailable() && menu.logisticsRestockEnabled())) {
         return true;
      }
      if (clickButton(event, x + 92, y + 198, 58, 16, IndustrialMultiblockControllerMenu.BUTTON_CYCLE_LOGISTICS_RESTOCK_TARGET, menu.logisticsAvailable())) {
         return true;
      }
      for (int index = 0; index < menu.recipeCount(); index++) {
         int rowY = y + 72 + index * 26;
         if (clickButton(event, x + 284, rowY + 5, 18, 16,
            IndustrialMultiblockControllerMenu.BUTTON_QUEUE_TASK_BASE + index, menu.formed())) {
            return true;
         }
         if (clickButton(event, x + 304, rowY + 5, 18, 16,
            IndustrialMultiblockControllerMenu.BUTTON_QUEUE_TASK_X3_BASE + index, menu.formed() && menu.taskCount() < menu.queueCapacity())) {
            return true;
         }
         if (clickButton(event, x + 324, rowY + 5, 18, 16,
            IndustrialMultiblockControllerMenu.BUTTON_QUEUE_TASK_X5_BASE + index, menu.formed() && menu.taskCount() < menu.queueCapacity())) {
            return true;
         }
         if (clickButton(event, x + 344, rowY + 5, 18, 16,
            IndustrialMultiblockControllerMenu.BUTTON_REQUEST_LOGISTICS_BASE + index, menu.logisticsAvailable())) {
            return true;
         }
      }
      return super.mouseClicked(event, doubleClick);
   }

   private void drawStatusPanel(GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY) {
      graphics.text(this.font, Component.literal("FACILITY"), x + 28, y + 60, CYAN, false);
      graphics.text(this.font, Component.literal(fit(stateName(), 138)), x + 28, y + 74, statusColor(), false);
      graphics.text(this.font, Component.literal(fit(menu.statusLine(), 138)), x + 28, y + 86, 0xD8F6FF, false);
      drawBar(graphics, x + 28, y + 104, 122, 8, menu.integrity() * 122 / 100, integrityColor());
      graphics.text(this.font, Component.literal("Integrity " + menu.integrity() + "%"), x + 28, y + 116, 0xD8F6FF, false);
      drawBar(graphics, x + 28, y + 134, 122, 8, menu.completionPermille() * 122 / 1000, CYAN);
      graphics.text(this.font, Component.literal("Completion " + (menu.completionPermille() / 10) + "%"), x + 28, y + 146, 0xD8F6FF, false);
      graphics.text(this.font, Component.literal(fit(menu.robotLine(), 138)), x + 28, y + 164,
         menu.robotCount() > 0 ? GREEN : AMBER, false);
      graphics.text(this.font, Component.literal("Queue " + menu.taskCount() + "/" + menu.queueCapacity()), x + 28, y + 176,
         menu.taskCount() >= menu.queueCapacity() ? AMBER : 0xD8F6FF, false);
      graphics.text(this.font, Component.literal(fit(menu.warningLine(), 138)), x + 28, y + 188,
         menu.warningCount() > 0 ? AMBER : 0x99C4D0, false);
      drawButton(graphics, x + 28, y + 198, 58, 16, "RESTOCK", mouseX, mouseY, menu.logisticsAvailable() && menu.logisticsRestockEnabled());
      drawButton(graphics, x + 92, y + 198, 58, 16, "x" + menu.logisticsRestockTargetRuns(), mouseX, mouseY, menu.logisticsAvailable());
   }

   private void drawTaskPanel(GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY) {
      graphics.text(this.font, Component.literal("AUTOMATION RECIPES"), x + 204, y + 60, CYAN, false);
      graphics.text(this.font, Component.literal(fit(menu.activeTaskLine(), 156)), x + 204, y + 194,
         menu.activeTaskIndex() >= 0 ? GREEN : 0x99C4D0, false);
      String blocked = menu.blockedReason();
      graphics.text(this.font, Component.literal(fit(blocked, 156)), x + 204, y + 204,
         menu.blockedTaskIndex() >= 0 ? AMBER : 0x99C4D0, false);
      drawBar(graphics, x + 204, y + 182, 154, 7,
         Math.min(154, menu.activeProgress() * 154 / menu.activeDuration()), progressColor());

      for (int index = 0; index < menu.recipeCount(); index++) {
         Identifier recipeId = menu.recipeId(index);
         AutomationRecipeMetadataPacket.Entry metadata = MultiblockClientPackets.recipeMetadata(recipeId);
         int rowY = y + 72 + index * 26;
         int rowColor = index == menu.activeTaskIndex() ? 0xAA14312B : index == menu.blockedTaskIndex() ? 0xAA35221A : PANEL_SOFT;
         graphics.fill(x + 204, rowY, x + 364, rowY + 24, rowColor);
         frame(graphics, x + 204, rowY, 160, 24, index == menu.activeTaskIndex() ? GREEN : STEEL);
         String label = metadata == null ? titleFromId(recipeId) : metadata.displayName();
         graphics.text(this.font, Component.literal(fit(label, 72)), x + 210, rowY + 5, 0xD8F6FF, false);
         String meta = metadata == null ? recipeId.getPath() : metadata.requiredWorkcell() + " " + seconds(metadata.durationTicks()) + "s";
         graphics.text(this.font, Component.literal(fit(meta, 68)), x + 210, rowY + 15, 0x99C4D0, false);
         boolean canBatch = menu.formed() && menu.taskCount() < menu.queueCapacity();
         drawButton(graphics, x + 284, rowY + 5, 18, 16, "1", mouseX, mouseY, menu.formed());
         drawButton(graphics, x + 304, rowY + 5, 18, 16, "3", mouseX, mouseY, canBatch);
         drawButton(graphics, x + 324, rowY + 5, 18, 16, "5", mouseX, mouseY, canBatch);
         drawButton(graphics, x + 344, rowY + 5, 18, 16, "L", mouseX, mouseY, menu.logisticsAvailable());
      }
   }

   private boolean clickButton(MouseButtonEvent event, int x, int y, int w, int h, int id, boolean enabled) {
      if (!enabled || event.button() != 0 || !inside(event.x(), event.y(), x, y, w, h)) {
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
      if (inside(mouseX, mouseY, x + 18, y + 218, 92, 18)) {
         lines = List.of(menu.formed() ? "Revalidate" : "Form structure", "Runs MultiblockCore validation at this controller.");
      } else if (inside(mouseX, mouseY, x + 118, y + 218, 82, 18)) {
         lines = List.of("Clear queue", "Cancels active and waiting factory tasks.");
      } else if (inside(mouseX, mouseY, x + 208, y + 218, 70, 18)) {
         lines = List.of("Retry blocked", "Resets blocked tasks while preserving consumed inputs.");
      } else if (inside(mouseX, mouseY, x + 286, y + 218, 72, 18)) {
         lines = List.of("Auto-restock", menu.logisticsAvailable()
            ? menu.logisticsLine()
            : "Install ECHO Logistics Network to enable request buttons.");
      } else if (inside(mouseX, mouseY, x + 28, y + 198, 58, 16)) {
         lines = List.of("Restock now", "Asks Logistics to dispatch one eligible factory loadout now.");
      } else if (inside(mouseX, mouseY, x + 92, y + 198, 58, 16)) {
         lines = List.of("Target runs", "Cycles auto-restock target between x1, x3, and x5.");
      } else {
         for (int index = 0; index < menu.recipeCount(); index++) {
            int rowY = y + 72 + index * 26;
            if (inside(mouseX, mouseY, x + 204, rowY, 160, 24)) {
               lines = tooltipForRecipe(menu.recipeId(index));
               break;
            }
         }
      }
      if (lines != null) {
         drawTooltipPanel(graphics, mouseX, mouseY, lines);
      }
   }

   private List<String> tooltipForRecipe(Identifier recipeId) {
      AutomationRecipeMetadataPacket.Entry metadata = MultiblockClientPackets.recipeMetadata(recipeId);
      if (metadata == null) {
         return List.of(titleFromId(recipeId), recipeId.toString());
      }
      List<String> lines = new ArrayList<>();
      lines.add(metadata.displayName());
      lines.add("Requires " + metadata.requiredWorkcell() + (metadata.tools().isBlank() ? "" : " / " + metadata.tools()));
      lines.add("Input: " + metadata.inputs());
      lines.add("Output: " + metadata.outputs());
      return lines;
   }

   private void drawTooltipPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY, List<String> lines) {
      EchoCyberGlassUi.tooltipPanel(graphics, this.font, mouseX, mouseY, this.leftPos, this.topPos, this.imageWidth,
         this.imageHeight, lines);
   }

   private void drawBar(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int filled, int color) {
      EchoCyberGlassUi.meter(graphics, x, y, w, h, filled, color);
   }

   private void frame(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int color) {
      EchoCyberGlassUi.frame(graphics, x, y, w, h, color);
   }

   private int integrityColor() {
      return menu.integrity() < 50 ? RED : menu.integrity() < 80 ? AMBER : GREEN;
   }

   private int progressColor() {
      return menu.blockedTaskIndex() >= 0 ? AMBER : menu.activeTaskIndex() >= 0 ? GREEN : CYAN;
   }

   private int statusColor() {
      String state = stateName();
      return state.contains("BLOCKED") || state.contains("DAMAGED") || state.contains("JAMMED") ? AMBER
         : menu.formed() ? GREEN : RED;
   }

   private String stateName() {
      MultiblockState[] states = MultiblockState.values();
      int index = menu.stateId();
      return index >= 0 && index < states.length ? states[index].name() : "UNKNOWN";
   }

   private int seconds(int ticks) {
      return Math.max(1, Math.round(ticks / 20.0F));
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

   private String titleFromId(Identifier id) {
      String[] words = id.getPath().split("_");
      StringBuilder builder = new StringBuilder();
      for (String word : words) {
         if (!builder.isEmpty()) {
            builder.append(' ');
         }
         builder.append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
      }
      return builder.toString();
   }
}
