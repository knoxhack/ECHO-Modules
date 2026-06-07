package com.knoxhack.echoblackboxprotocol.menu;

import com.knoxhack.echoblackboxprotocol.block.entity.BlackboxMachineBlockEntity;
import com.knoxhack.echoblackboxprotocol.progression.BlackboxDungeon;
import com.knoxhack.echoblackboxprotocol.progression.BlackboxEnding;
import com.knoxhack.echoblackboxprotocol.progression.BlackboxMachineKind;
import com.knoxhack.echoblackboxprotocol.progression.BlackboxProgress;
import com.knoxhack.echoblackboxprotocol.progression.MemoryType;
import com.knoxhack.echoblackboxprotocol.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class BlackboxMachineMenu extends AbstractContainerMenu {
   public static final int GUI_WIDTH = 352;
   public static final int GUI_HEIGHT = 300;
   public static final int INPUT_X = 66;
   public static final int OUTPUT_X = 270;
   public static final int MACHINE_SLOT_Y = 122;
   public static final int PLAYER_INV_X = 96;
   public static final int PLAYER_INV_Y = 208;
   public static final int BUTTON_PRIMARY_ACTION = 0;
   public static final int PROGRESS_MEMORY_START = 0;
   public static final int PROGRESS_DUNGEON_START = PROGRESS_MEMORY_START + MemoryType.values().length;
   public static final int PROGRESS_BOSS_FALSE_ECHO = PROGRESS_DUNGEON_START + BlackboxDungeon.values().length;
   public static final int PROGRESS_BOSS_COMMAND_REMNANT = PROGRESS_BOSS_FALSE_ECHO + 1;
   public static final int PROGRESS_BOSS_NEXUS_GUARDIAN = PROGRESS_BOSS_COMMAND_REMNANT + 1;
   public static final int PROGRESS_HAS_CORE_KEY = PROGRESS_BOSS_NEXUS_GUARDIAN + 1;
   public static final int PROGRESS_ENDING = PROGRESS_HAS_CORE_KEY + 1;
   public static final int PROGRESS_STABILITY = PROGRESS_ENDING + 1;
   public static final int PROGRESS_FALSE_SIGNALS = PROGRESS_STABILITY + 1;
   public static final int PROGRESS_DATA_COUNT = PROGRESS_FALSE_SIGNALS + 1;
   private static final int MACHINE_SLOT_COUNT = 2;
   private static final int PLAYER_INV_START = 2;
   private static final int PLAYER_INV_END = PLAYER_INV_START + 27;
   private static final int HOTBAR_END = PLAYER_INV_END + 9;

   private final Inventory inventory;
   private final Player player;
   private final Container container;
   private final ContainerData data;
   private final ContainerData progressData;
   private final BlockPos pos;

   public BlackboxMachineMenu(int containerId, Inventory inventory, Container container, ContainerData data, BlockPos pos) {
      this(containerId, inventory, container, data, progressDataFor(inventory.player), pos);
   }

   public BlackboxMachineMenu(int containerId, Inventory inventory, Container container, ContainerData data, ContainerData progressData, BlockPos pos) {
      super(ModMenus.BLACKBOX_MACHINE.get(), containerId);
      checkContainerSize(container, MACHINE_SLOT_COUNT);
      checkContainerDataCount(data, BlackboxMachineBlockEntity.DATA_COUNT);
      checkContainerDataCount(progressData, PROGRESS_DATA_COUNT);
      this.inventory = inventory;
      this.player = inventory.player;
      this.container = container;
      this.data = data;
      this.progressData = progressData;
      this.pos = pos;
      this.addSlot(new Slot(container, BlackboxMachineBlockEntity.INPUT_SLOT, INPUT_X, MACHINE_SLOT_Y) {
         public boolean mayPlace(ItemStack stack) {
            return BlackboxMachineBlockEntity.validInput(BlackboxMachineMenu.this.kind(), stack);
         }
      });
      this.addSlot(new Slot(container, BlackboxMachineBlockEntity.OUTPUT_SLOT, OUTPUT_X, MACHINE_SLOT_Y) {
         public boolean mayPlace(ItemStack stack) {
            return false;
         }
      });
      this.addStandardInventorySlots(inventory, PLAYER_INV_X, PLAYER_INV_Y);
      this.addDataSlots(data);
      this.addDataSlots(progressData);
   }

   public static BlackboxMachineMenu fromNetwork(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
      BlockPos pos = buffer.readBlockPos();
      BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
      if (blockEntity instanceof BlackboxMachineBlockEntity machine) {
         return new BlackboxMachineMenu(containerId, inventory, machine, machine.data(), pos);
      }

      return new BlackboxMachineMenu(
         containerId,
         inventory,
         new SimpleContainer(MACHINE_SLOT_COUNT),
         new SimpleContainerData(BlackboxMachineBlockEntity.DATA_COUNT),
         new SimpleContainerData(PROGRESS_DATA_COUNT),
         pos
      );
   }

   @Override
   public ItemStack quickMoveStack(Player player, int slotIndex) {
      ItemStack copy = ItemStack.EMPTY;
      Slot slot = this.slots.get(slotIndex);
      if (slot != null && slot.hasItem()) {
         ItemStack stack = slot.getItem();
         copy = stack.copy();
         if (slotIndex == BlackboxMachineBlockEntity.OUTPUT_SLOT) {
            if (!this.moveItemStackTo(stack, PLAYER_INV_START, HOTBAR_END, true)) {
               return ItemStack.EMPTY;
            }

            slot.onTake(player, stack);
         } else if (slotIndex == BlackboxMachineBlockEntity.INPUT_SLOT) {
            if (!this.moveItemStackTo(stack, PLAYER_INV_START, HOTBAR_END, false)) {
               return ItemStack.EMPTY;
            }
         } else if (!this.moveItemStackTo(stack, BlackboxMachineBlockEntity.INPUT_SLOT, BlackboxMachineBlockEntity.INPUT_SLOT + 1, false)) {
            return ItemStack.EMPTY;
         }

         if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
         } else {
            slot.setChanged();
         }
      }

      return copy;
   }

   @Override
   public boolean stillValid(Player player) {
      return this.container.stillValid(player);
   }

   @Override
   public boolean clickMenuButton(Player player, int id) {
      if (id == BUTTON_PRIMARY_ACTION && player instanceof ServerPlayer serverPlayer && this.container instanceof BlackboxMachineBlockEntity machine) {
         machine.startOperation(serverPlayer);
         return true;
      }

      return false;
   }

   public Inventory inventory() {
      return this.inventory;
   }

   public Player player() {
      return this.player;
   }

   public int memoryCount(MemoryType type) {
      return this.progressData.get(PROGRESS_MEMORY_START + type.ordinal());
   }

   public int decodedMemoryTotal() {
      int total = 0;

      for (MemoryType type : MemoryType.values()) {
         total += this.memoryCount(type);
      }

      return total;
   }

   public boolean hasMemory(MemoryType type, int count) {
      return this.memoryCount(type) >= count;
   }

   public boolean completed(BlackboxDungeon dungeon) {
      return this.progressData.get(PROGRESS_DUNGEON_START + dungeon.ordinal()) != 0;
   }

   public boolean bossDefeated(String id) {
      return switch (id) {
         case "false_echo" -> this.progressData.get(PROGRESS_BOSS_FALSE_ECHO) != 0;
         case "command_remnant" -> this.progressData.get(PROGRESS_BOSS_COMMAND_REMNANT) != 0;
         case "nexus_guardian" -> this.progressData.get(PROGRESS_BOSS_NEXUS_GUARDIAN) != 0;
         default -> false;
      };
   }

   public boolean hasNexusCoreAccessKey() {
      return this.progressData.get(PROGRESS_HAS_CORE_KEY) != 0;
   }

   public BlackboxEnding ending() {
      return BlackboxEnding.values()[Math.max(0, Math.min(BlackboxEnding.values().length - 1, this.progressData.get(PROGRESS_ENDING)))];
   }

   public int stability() {
      return this.progressData.get(PROGRESS_STABILITY);
   }

   public int falseSignalCount() {
      return this.progressData.get(PROGRESS_FALSE_SIGNALS);
   }

   public BlackboxMachineKind kind() {
      int id = this.data.get(BlackboxMachineBlockEntity.DATA_KIND);
      BlackboxMachineKind[] values = BlackboxMachineKind.values();
      return id >= 0 && id < values.length ? values[id] : BlackboxMachineKind.BLACKBOX_DECODER;
   }

   public int progress() {
      return this.data.get(BlackboxMachineBlockEntity.DATA_PROGRESS);
   }

   public int maxProgress() {
      return this.data.get(BlackboxMachineBlockEntity.DATA_MAX_PROGRESS);
   }

   public int statusId() {
      return this.data.get(BlackboxMachineBlockEntity.DATA_STATUS);
   }

   public BlockPos pos() {
      return this.pos;
   }

   private static ContainerData progressDataFor(Player player) {
      if (player.level().isClientSide()) {
         return new SimpleContainerData(PROGRESS_DATA_COUNT);
      }

      return new ContainerData() {
         public int get(int index) {
            BlackboxProgress progress = BlackboxProgress.get(player);
            if (index >= PROGRESS_MEMORY_START && index < PROGRESS_DUNGEON_START) {
               return progress.memoryCount(MemoryType.values()[index - PROGRESS_MEMORY_START]);
            }

            if (index >= PROGRESS_DUNGEON_START && index < PROGRESS_BOSS_FALSE_ECHO) {
               return progress.completed(BlackboxDungeon.values()[index - PROGRESS_DUNGEON_START]) ? 1 : 0;
            }

            if (index == PROGRESS_BOSS_FALSE_ECHO) {
               return progress.bossDefeated("false_echo") ? 1 : 0;
            } else if (index == PROGRESS_BOSS_COMMAND_REMNANT) {
               return progress.bossDefeated("command_remnant") ? 1 : 0;
            } else if (index == PROGRESS_BOSS_NEXUS_GUARDIAN) {
               return progress.bossDefeated("nexus_guardian") ? 1 : 0;
            } else if (index == PROGRESS_HAS_CORE_KEY) {
               return progress.hasNexusCoreAccessKey() ? 1 : 0;
            } else if (index == PROGRESS_ENDING) {
               return progress.ending().ordinal();
            } else if (index == PROGRESS_STABILITY) {
               return progress.stability();
            } else if (index == PROGRESS_FALSE_SIGNALS) {
               return progress.falseSignalCount();
            } else {
               return 0;
            }
         }

         public void set(int index, int value) {
         }

         public int getCount() {
            return PROGRESS_DATA_COUNT;
         }
      };
   }
}
