package com.knoxhack.echoindustrialnexus.menu;

import com.knoxhack.echoindustrialnexus.block.IndustrialMachineBlock;
import com.knoxhack.echoindustrialnexus.block.entity.IndustrialMachineBlockEntity;
import com.knoxhack.echoindustrialnexus.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
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

public class IndustrialMachineMenu extends AbstractContainerMenu {
   public static final int GUI_WIDTH = 352;
   public static final int GUI_HEIGHT = 310;
   public static final int INPUT_X = 44;
   public static final int CATALYST_X = 82;
   public static final int OUTPUT_X = 276;
   public static final int BYPRODUCT_X = 314;
   public static final int UPGRADE_X = 44;
   public static final int PLAYER_INV_X = 99;
   public static final int PLAYER_INV_Y = 228;
   public static final int BUTTON_CYCLE_SCRUBBER = 0;
   public static final int BUTTON_CYCLE_SIDE_CONFIG = 1;
   public static final int BUTTON_TOGGLE_SHUTDOWN = 2;
   public static final int BUTTON_CONTROLLER_SHUTDOWN = 3;

   private static final int PLAYER_INV_START = IndustrialMachineBlockEntity.SLOT_COUNT;
   private static final int PLAYER_INV_END = PLAYER_INV_START + 27;
   private static final int HOTBAR_END = PLAYER_INV_END + 9;

   private final Container container;
   private final ContainerData data;
   private final IndustrialMachineBlockEntity machine;

   public IndustrialMachineMenu(int containerId, Inventory playerInventory, Container container) {
      this(containerId, playerInventory, container, new SimpleContainerData(IndustrialMachineBlockEntity.DATA_COUNT));
   }

   public IndustrialMachineMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
      super(ModMenus.INDUSTRIAL_MACHINE.get(), containerId);
      checkContainerSize(container, IndustrialMachineBlockEntity.SLOT_COUNT);
      checkContainerDataCount(data, IndustrialMachineBlockEntity.DATA_COUNT);
      this.container = container;
      this.data = data;
      this.machine = container instanceof IndustrialMachineBlockEntity industrialMachine ? industrialMachine : null;

      this.addSlot(new Slot(container, IndustrialMachineBlockEntity.INPUT_SLOT, INPUT_X, 92));
      this.addSlot(new OutputSlot(container, IndustrialMachineBlockEntity.OUTPUT_SLOT, OUTPUT_X, 92));
      this.addSlot(new OutputSlot(container, IndustrialMachineBlockEntity.BYPRODUCT_SLOT, BYPRODUCT_X, 92));
      this.addSlot(new Slot(container, IndustrialMachineBlockEntity.AUX_SLOT, CATALYST_X, 92));
      for (int slot = IndustrialMachineBlockEntity.UPGRADE_SLOT_START; slot <= IndustrialMachineBlockEntity.UPGRADE_SLOT_END; slot++) {
         this.addSlot(new Slot(container, slot, UPGRADE_X + (slot - IndustrialMachineBlockEntity.UPGRADE_SLOT_START) * 22, 150));
      }
      this.addStandardInventorySlots(playerInventory, PLAYER_INV_X, PLAYER_INV_Y);
      this.addDataSlots(data);
   }

   public static IndustrialMachineMenu fromNetwork(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
      BlockPos pos = buffer.readBlockPos();
      BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
      if (blockEntity instanceof IndustrialMachineBlockEntity machine) {
         return new IndustrialMachineMenu(containerId, inventory, machine, machine.data());
      }
      return new IndustrialMachineMenu(containerId, inventory, new SimpleContainer(IndustrialMachineBlockEntity.SLOT_COUNT));
   }

   @Override
   public ItemStack quickMoveStack(Player player, int slotIndex) {
      ItemStack copy = ItemStack.EMPTY;
      Slot slot = this.slots.get(slotIndex);
      if (slot != null && slot.hasItem()) {
         ItemStack stack = slot.getItem();
         copy = stack.copy();
         if (slotIndex == IndustrialMachineBlockEntity.OUTPUT_SLOT || slotIndex == IndustrialMachineBlockEntity.BYPRODUCT_SLOT) {
            if (!this.moveItemStackTo(stack, PLAYER_INV_START, HOTBAR_END, true)) {
               return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
         } else if (slotIndex < IndustrialMachineBlockEntity.SLOT_COUNT) {
            if (!this.moveItemStackTo(stack, PLAYER_INV_START, HOTBAR_END, false)) {
               return ItemStack.EMPTY;
            }
         } else if (IndustrialMachineBlockEntity.isUpgrade(stack)) {
            if (!this.moveItemStackTo(stack, IndustrialMachineBlockEntity.UPGRADE_SLOT_START, IndustrialMachineBlockEntity.UPGRADE_SLOT_END + 1, false)) {
               return ItemStack.EMPTY;
            }
         } else if (!this.moveItemStackTo(stack, IndustrialMachineBlockEntity.INPUT_SLOT, IndustrialMachineBlockEntity.INPUT_SLOT + 1, false)
            && !this.moveItemStackTo(stack, IndustrialMachineBlockEntity.AUX_SLOT, IndustrialMachineBlockEntity.AUX_SLOT + 1, false)) {
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
      return this.machine != null && this.machine.handleMenuButton(player, id);
   }

   public IndustrialMachineBlock.MachineKind machineKind() {
      IndustrialMachineBlock.MachineKind[] values = IndustrialMachineBlock.MachineKind.values();
      int id = this.data.get(IndustrialMachineBlockEntity.DATA_KIND);
      return id >= 0 && id < values.length ? values[id] : IndustrialMachineBlock.MachineKind.ORE_GRINDER;
   }

   public int flux() {
      return this.data.get(IndustrialMachineBlockEntity.DATA_FLUX);
   }

   public int maxFlux() {
      return this.data.get(IndustrialMachineBlockEntity.DATA_MAX_FLUX);
   }

   public int progress() {
      return this.data.get(IndustrialMachineBlockEntity.DATA_PROGRESS);
   }

   public int maxProgress() {
      return this.data.get(IndustrialMachineBlockEntity.DATA_MAX_PROGRESS);
   }

   public int heat() {
      return this.data.get(IndustrialMachineBlockEntity.DATA_HEAT);
   }

   public int statusId() {
      return this.data.get(IndustrialMachineBlockEntity.DATA_STATUS);
   }

   public int scrubberModeId() {
      return this.data.get(IndustrialMachineBlockEntity.DATA_SCRUBBER_MODE);
   }

   public int sideConfigId() {
      return this.data.get(IndustrialMachineBlockEntity.DATA_SIDE_CONFIG);
   }

   public boolean remoteShutdown() {
      return this.data.get(IndustrialMachineBlockEntity.DATA_REMOTE_SHUTDOWN) != 0;
   }

   public int alertCount() {
      return this.data.get(IndustrialMachineBlockEntity.DATA_ALERTS);
   }

   public int linkedCount() {
      return this.data.get(IndustrialMachineBlockEntity.DATA_LINKED);
   }

   public int inputFluidAmount() {
      return this.data.get(IndustrialMachineBlockEntity.DATA_INPUT_FLUID_AMOUNT);
   }

   public int inputFluidId() {
      return this.data.get(IndustrialMachineBlockEntity.DATA_INPUT_FLUID_ID);
   }

   public int outputFluidAmount() {
      return this.data.get(IndustrialMachineBlockEntity.DATA_OUTPUT_FLUID_AMOUNT);
   }

   public int outputFluidId() {
      return this.data.get(IndustrialMachineBlockEntity.DATA_OUTPUT_FLUID_ID);
   }

   private static class OutputSlot extends Slot {
      OutputSlot(Container container, int slot, int x, int y) {
         super(container, slot, x, y);
      }

      @Override
      public boolean mayPlace(ItemStack stack) {
         return false;
      }
   }
}
