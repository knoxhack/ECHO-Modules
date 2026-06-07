package com.knoxhack.echoblackboxprotocol.client;

import com.knoxhack.echoblackboxprotocol.menu.BlackboxMachineMenu;
import com.knoxhack.echoblackboxprotocol.block.entity.BlackboxMachineBlockEntity;
import com.knoxhack.echoblackboxprotocol.progression.BlackboxDungeon;
import com.knoxhack.echoblackboxprotocol.progression.BlackboxEnding;
import com.knoxhack.echoblackboxprotocol.progression.BlackboxMachineKind;
import com.knoxhack.echoblackboxprotocol.progression.MemoryType;
import com.knoxhack.echoblackboxprotocol.registry.ModBlocks;
import com.knoxhack.echoblackboxprotocol.registry.ModItems;
import com.knoxhack.echocore.client.ui.EchoCyberGlassUi;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public class BlackboxMachineScreen extends AbstractContainerScreen<BlackboxMachineMenu> {
   private static final int PANEL = 0xF0070A12;
   private static final int PANEL_SOFT = 0xAA111827;
   private static final int CYAN = 0xFF7BDEFF;
   private static final int BLUE = 0xFF9FD1FF;
   private static final int PURPLE = 0xFFC09BFF;
   private static final int GREEN = 0xFF64D97B;
   private static final int AMBER = 0xFFFFB14A;
   private static final int RED = 0xFFFF5D73;

   public BlackboxMachineScreen(BlackboxMachineMenu menu, Inventory inventory, Component title) {
      super(menu, inventory, title, BlackboxMachineMenu.GUI_WIDTH, BlackboxMachineMenu.GUI_HEIGHT);
      this.titleLabelX = 16;
      this.titleLabelY = 14;
      this.inventoryLabelX = BlackboxMachineMenu.PLAYER_INV_X;
      this.inventoryLabelY = BlackboxMachineMenu.PLAYER_INV_Y - 13;
   }

   @Override
   public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
      int x = this.leftPos;
      int y = this.topPos;
      EchoCyberGlassUi.panel(graphics, x, y, this.imageWidth, this.imageHeight, PANEL, accent());
      graphics.fill(x + 2, y + 2, x + this.imageWidth - 2, y + 42, 0xDD101820);
      graphics.fill(x + 18, y + 54, x + this.imageWidth - 18, y + 178, 0xAA050914);
      graphics.fill(x + 48, y + BlackboxMachineMenu.PLAYER_INV_Y - 5, x + this.imageWidth - 48, y + BlackboxMachineMenu.PLAYER_INV_Y - 4, 0x337BDEFF);

      drawMachinePanel(graphics, x, y);
      drawButton(graphics, x + 226, y + 160, 96, 18, actionLabel(), mouseX, mouseY, this.menu.statusId() != BlackboxMachineBlockEntity.MachineStatus.PROCESSING.ordinal());

      super.extractContents(graphics, mouseX, mouseY, partialTick);
      drawReadouts(graphics, x, y);
   }

   @Override
   protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
      graphics.text(this.font, Component.literal(fit("ECHO-7 BLACKBOX // " + this.menu.kind().displayName(), 238)), this.titleLabelX, this.titleLabelY, accent(), true);
      graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xD8F6FF, false);
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
      if (clickButton(event, this.leftPos + 226, this.topPos + 160, 96, 18, BlackboxMachineMenu.BUTTON_PRIMARY_ACTION)) {
         return true;
      }

      return super.mouseClicked(event, doubleClick);
   }

   private void drawMachinePanel(GuiGraphicsExtractor graphics, int x, int y) {
      graphics.text(this.font, Component.literal("INPUT"), x + BlackboxMachineMenu.INPUT_X - 4, y + BlackboxMachineMenu.MACHINE_SLOT_Y - 14, 0xB9D6E2, false);
      graphics.text(this.font, Component.literal("OUTPUT"), x + BlackboxMachineMenu.OUTPUT_X - 8, y + BlackboxMachineMenu.MACHINE_SLOT_Y - 14, 0xB9D6E2, false);
      EchoCyberGlassUi.frame(graphics, x + BlackboxMachineMenu.INPUT_X - 4, y + BlackboxMachineMenu.MACHINE_SLOT_Y - 4, 24, 24, accent());
      EchoCyberGlassUi.frame(graphics, x + BlackboxMachineMenu.OUTPUT_X - 4, y + BlackboxMachineMenu.MACHINE_SLOT_Y - 4, 24, 24, GREEN);
      int max = Math.max(1, this.menu.maxProgress());
      int filled = this.menu.maxProgress() <= 0 ? 0 : Math.min(108, this.menu.progress() * 108 / max);
      drawBar(graphics, x + 122, y + 126, 108, 8, filled, accent());
      graphics.text(this.font, Component.literal(statusLabel()), x + 122, y + 142, statusColor(), false);
      graphics.text(this.font, Component.literal(machineHint()), x + 32, y + 160, 0xB9D6E2, false);
   }

   private void drawReadouts(GuiGraphicsExtractor graphics, int x, int y) {
      graphics.text(this.font, Component.literal("Stability " + this.menu.stability() + "%"), x + 32, y + 64, this.menu.stability() < 30 ? RED : 0xD8F6FF, false);
      graphics.text(this.font, Component.literal("False signals " + this.menu.falseSignalCount()), x + 32, y + 92, this.menu.falseSignalCount() > 6 ? AMBER : 0xD8F6FF, false);
      graphics.text(this.font, Component.literal("Decoded logs " + this.menu.decodedMemoryTotal()), x + 188, y + 64, 0xD8F6FF, false);
      graphics.text(this.font, Component.literal("Ending " + this.menu.ending().displayName()), x + 188, y + 92, this.menu.ending() == BlackboxEnding.NONE ? AMBER : GREEN, false);

      int cy = y + 182;
      for (String line : detailLines()) {
         graphics.text(this.font, Component.literal(fit(line, 190)), x + 32, cy, line.startsWith("[x]") ? GREEN : line.startsWith("[!]") ? AMBER : 0xB9D6E2, false);
         cy += 12;
      }
   }

   private List<String> detailLines() {
      List<String> lines = new ArrayList<>();
      Player player = this.menu.player();
      ItemStack held = player.getMainHandItem();
      switch (this.menu.kind()) {
         case BLACKBOX_DECODER:
            lines.add(mark(held.getItem() instanceof com.knoxhack.echoblackboxprotocol.item.BlackboxFragmentItem) + " slot accepts typed fragments");
            lines.add("[ ] outputs decoded memory records");
            break;
         case MEMORY_PROJECTOR:
            lines.add(mark(this.menu.hasMemory(MemoryType.ECHO, 2)) + " ECHO Logs " + this.menu.memoryCount(MemoryType.ECHO) + "/2");
            lines.add(mark(this.menu.bossDefeated("false_echo")) + " False ECHO defeated");
            lines.add(mark(this.menu.completed(BlackboxDungeon.LABYRINTH)) + " Labyrinth stabilized");
            break;
         case ARCHIVE_TERMINAL:
            lines.add(mark(this.menu.hasMemory(MemoryType.PERSONAL, 2)) + " Personal Logs " + this.menu.memoryCount(MemoryType.PERSONAL) + "/2");
            lines.add(mark(this.menu.hasMemory(MemoryType.SECURITY, 2)) + " Security Logs " + this.menu.memoryCount(MemoryType.SECURITY) + "/2");
            lines.add(mark(this.menu.completed(BlackboxDungeon.VAULT)) + " Vault route sealed");
            break;
         case CORE_KEY_ASSEMBLER:
            lines.add(mark(has(player, ModItems.CORE_ACCESS_KEY_LEFT.get()) || player.hasInfiniteMaterials()) + " left key segment");
            lines.add(mark(has(player, ModItems.CORE_ACCESS_KEY_RIGHT.get()) || player.hasInfiniteMaterials()) + " right key segment");
            lines.add(mark(has(player, ModItems.CORE_ACCESS_KEY_MATRIX.get()) || player.hasInfiniteMaterials()) + " key matrix");
            lines.add(mark(this.menu.bossDefeated("false_echo") && this.menu.bossDefeated("command_remnant")) + " boss proof");
            break;
         case TRUTH_ENGINE:
            lines.add(mark(this.menu.bossDefeated("nexus_guardian")) + " Guardian defeated");
            lines.add(mark(this.menu.hasNexusCoreAccessKey()) + " core key assembled");
            lines.add(mark(this.endingEligible(BlackboxEnding.MERGE)) + " Merge truth index");
            break;
         case MEMORY_STABILIZER:
            lines.add(mark(has(player, ModItems.STATIC_FLUID.get()) || has(player, ModItems.MEMORY_STABILIZER_CORE.get())) + " stabilizer reagent");
            lines.add(this.menu.stability() < 100 ? "[!] hallucination pressure active" : "[x] memory pressure stable");
            break;
         case PROTOCOL_EXTRACTOR:
            lines.add(mark(this.menu.completed(BlackboxDungeon.VAULT)) + " Vault route sealed");
            lines.add(mark(this.menu.hasMemory(MemoryType.COMMAND, 2)) + " Command Logs " + this.menu.memoryCount(MemoryType.COMMAND) + "/2");
            lines.add(mark(this.menu.bossDefeated("command_remnant")) + " Command Remnant defeated");
            lines.add(mark(has(player, ModBlocks.PROTOCOL_EXTRACTOR.get()) || player.hasInfiniteMaterials()) + " extractor installed");
            break;
      }

      return lines;
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
      EchoCyberGlassUi.button(graphics, this.font, x, y, w, h, label, mouseX, mouseY, enabled);
   }

   private void drawBar(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int filled, int color) {
      EchoCyberGlassUi.meter(graphics, x, y, w, h, filled, color);
   }

   private void frame(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int color) {
      EchoCyberGlassUi.frame(graphics, x, y, w, h, color);
   }

   private String actionLabel() {
      return switch (this.menu.kind()) {
         case BLACKBOX_DECODER -> "DECODE";
         case MEMORY_PROJECTOR -> "PROJECT";
         case ARCHIVE_TERMINAL -> "SEAL";
         case CORE_KEY_ASSEMBLER -> "ASSEMBLE";
         case TRUTH_ENGINE -> "COMMIT";
         case MEMORY_STABILIZER -> "STABILIZE";
         case PROTOCOL_EXTRACTOR -> "EXTRACT";
      };
   }

   private String machineHint() {
      return switch (this.menu.kind()) {
         case BLACKBOX_DECODER -> "Fragment input -> memory record output.";
         case MEMORY_PROJECTOR -> this.menu.bossDefeated("false_echo") ? "Projector can stabilize Labyrinth routing." : "Projector can summon False ECHO.";
         case ARCHIVE_TERMINAL -> this.menu.completed(BlackboxDungeon.VAULT) ? "Vault route is sealed." : "Seal Vault after Personal/Security logs.";
         case CORE_KEY_ASSEMBLER -> "Command Key input makes matrix; action assembles final key.";
         case TRUTH_ENGINE -> "Directive input commits the final world state.";
         case MEMORY_STABILIZER -> "Static Fluid or Stabilizer Core restores memory safety.";
         case PROTOCOL_EXTRACTOR -> "Deleted record input extracts fluid; action pulls command proof.";
      };
   }

   private String statusLabel() {
      BlackboxMachineBlockEntity.MachineStatus[] values = BlackboxMachineBlockEntity.MachineStatus.values();
      int id = this.menu.statusId();
      return id >= 0 && id < values.length ? values[id].label() : "Idle";
   }

   private int statusColor() {
      BlackboxMachineBlockEntity.MachineStatus[] values = BlackboxMachineBlockEntity.MachineStatus.values();
      int id = this.menu.statusId();
      BlackboxMachineBlockEntity.MachineStatus status = id >= 0 && id < values.length ? values[id] : BlackboxMachineBlockEntity.MachineStatus.IDLE;
      return switch (status) {
         case COMPLETE -> GREEN;
         case BAD_INPUT, OUTPUT_BLOCKED, LOCKED -> RED;
         case PROCESSING -> AMBER;
         case IDLE -> 0xB9D6E2;
      };
   }

   private int accent() {
      BlackboxMachineKind kind = this.menu.kind();
      return switch (kind) {
         case TRUTH_ENGINE, CORE_KEY_ASSEMBLER -> PURPLE;
         case PROTOCOL_EXTRACTOR -> RED;
         case MEMORY_PROJECTOR, MEMORY_STABILIZER -> BLUE;
         default -> CYAN;
      };
   }

   private String mark(boolean value) {
      return value ? "[x]" : "[ ]";
   }

   private boolean endingEligible(BlackboxEnding ending) {
      boolean finalBoss = this.menu.bossDefeated("nexus_guardian") || this.menu.player().hasInfiniteMaterials();
      boolean key = this.menu.hasNexusCoreAccessKey() || has(this.menu.player(), ModItems.NEXUS_CORE_ACCESS_KEY.get()) || this.menu.player().hasInfiniteMaterials();
      if (ending != BlackboxEnding.MERGE) {
         return finalBoss && key;
      }

      Player player = this.menu.player();
      return finalBoss
         && key
         && this.menu.memoryCount(MemoryType.DELETED) >= 3
         && has(player, ModItems.ECHO_IDENTITY_FRAGMENT.get())
         && has(player, ModItems.MEMORY_STABILIZER_CORE.get())
         && has(player, ModItems.COMMAND_KEY.get())
         && has(player, ModItems.PROTOCOL_EXTRACTOR_SCHEMATIC.get())
         && (has(player, ModItems.GUARDIAN_CORE.get()) || player.hasInfiniteMaterials())
         && (has(player, ModBlocks.PROTOCOL_EXTRACTOR.get()) || player.hasInfiniteMaterials());
   }

   private boolean has(Player player, ItemLike itemLike) {
      return has(player, itemLike.asItem());
   }

   private boolean has(Player player, Item item) {
      return player.getInventory().contains(new ItemStack(item));
   }

   private boolean inside(double px, double py, int x, int y, int w, int h) {
      return px >= x && px < x + w && py >= y && py < y + h;
   }

   private String fit(String text, int maxWidth) {
      if (this.font.width(text) <= maxWidth) {
         return text;
      }

      String suffix = "...";
      int suffixW = this.font.width(suffix);
      if (maxWidth <= suffixW) {
         return this.font.plainSubstrByWidth(text, maxWidth);
      }

      return this.font.plainSubstrByWidth(text, maxWidth - suffixW) + suffix;
   }
}
