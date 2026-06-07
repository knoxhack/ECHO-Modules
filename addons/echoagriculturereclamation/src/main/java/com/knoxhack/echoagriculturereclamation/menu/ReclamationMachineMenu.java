package com.knoxhack.echoagriculturereclamation.menu;

import com.knoxhack.echoagriculturereclamation.block.ReclamationMachineBlock;
import com.knoxhack.echoagriculturereclamation.block.entity.ReclamationMachineBlockEntity;
import com.knoxhack.echoagriculturereclamation.registry.ModMenus;
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

public class ReclamationMachineMenu extends AbstractContainerMenu {
   public static final int GUI_WIDTH = 304;
   public static final int GUI_HEIGHT = 242;
   public static final int BUTTON_SCAN = 0;
   public static final int BUTTON_RUN = 1;
   public static final int BUTTON_RECALL = 2;
   private static final int PLAYER_INV_START = ReclamationMachineBlockEntity.SLOT_COUNT;
   private static final int PLAYER_INV_END = PLAYER_INV_START + 27;
   private static final int HOTBAR_END = PLAYER_INV_END + 9;
   private final Container container;
   private final ContainerData data;
   private final ReclamationMachineBlockEntity machine;

   public ReclamationMachineMenu(int containerId, Inventory playerInventory, Container container) {
      this(containerId, playerInventory, container, new SimpleContainerData(ReclamationMachineBlockEntity.DATA_COUNT));
   }

   public ReclamationMachineMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
      super(ModMenus.RECLAMATION_MACHINE.get(), containerId);
      checkContainerSize(container, ReclamationMachineBlockEntity.SLOT_COUNT);
      checkContainerDataCount(data, ReclamationMachineBlockEntity.DATA_COUNT);
      this.container = container;
      this.data = data;
      this.machine = container instanceof ReclamationMachineBlockEntity blockEntity ? blockEntity : null;

      addSlot(validatingSlot(container, ReclamationMachineBlockEntity.INPUT_SLOT, 42, 74));
      addSlot(validatingSlot(container, ReclamationMachineBlockEntity.CATALYST_SLOT, 78, 74));
      addSlot(outputSlot(container, ReclamationMachineBlockEntity.OUTPUT_SLOT, 206, 74));
      addSlot(validatingSlot(container, ReclamationMachineBlockEntity.AUX_SLOT, 242, 74));
      addStandardInventorySlots(playerInventory, 62, 124);
      addDataSlots(data);
   }

   public static ReclamationMachineMenu fromNetwork(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
      BlockPos pos = buffer.readBlockPos();
      BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
      if (blockEntity instanceof ReclamationMachineBlockEntity machine) {
         return new ReclamationMachineMenu(containerId, inventory, machine, machine.data());
      }
      return new ReclamationMachineMenu(containerId, inventory, new SimpleContainer(ReclamationMachineBlockEntity.SLOT_COUNT));
   }

   @Override
   public ItemStack quickMoveStack(Player player, int slotIndex) {
      ItemStack copy = ItemStack.EMPTY;
      Slot slot = slots.get(slotIndex);
      if (slot != null && slot.hasItem()) {
         ItemStack stack = slot.getItem();
         copy = stack.copy();
         if (machine != null && machine.isProtectedOperationSlot(slotIndex)) {
            return ItemStack.EMPTY;
         }
         if (slotIndex < ReclamationMachineBlockEntity.SLOT_COUNT) {
            if (!moveItemStackTo(stack, PLAYER_INV_START, HOTBAR_END, true)) {
               return ItemStack.EMPTY;
            }
         } else if (!moveItemStackTo(stack, 0, ReclamationMachineBlockEntity.OUTPUT_SLOT, false)) {
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
      return container.stillValid(player);
   }

   @Override
   public boolean clickMenuButton(Player player, int id) {
      return machine != null && machine.handleMenuButton(player, id);
   }

   public ReclamationMachineBlock.MachineKind kind() {
      ReclamationMachineBlock.MachineKind[] values = ReclamationMachineBlock.MachineKind.values();
      int ordinal = data.get(ReclamationMachineBlockEntity.DATA_KIND);
      return ordinal >= 0 && ordinal < values.length ? values[ordinal] : ReclamationMachineBlock.MachineKind.ECOLOGY_SCANNER;
   }

   public int progress() {
      return data.get(ReclamationMachineBlockEntity.DATA_PROGRESS);
   }

   public int progressMax() {
      return data.get(ReclamationMachineBlockEntity.DATA_PROGRESS_MAX);
   }

   public int status() {
      return data.get(ReclamationMachineBlockEntity.DATA_STATUS);
   }

   public boolean powered() {
      return data.get(ReclamationMachineBlockEntity.DATA_POWERED) > 0;
   }

   public int outputCount() {
      return data.get(ReclamationMachineBlockEntity.DATA_OUTPUT_COUNT);
   }

   public String processTitle() {
      return machine == null ? kind().displayName() + " Diagnostics" : machine.processTitle();
   }

   public String statusLine() {
      return machine == null ? "Awaiting server diagnostics." : machine.statusLine();
   }

   public String nextAction() {
      return machine == null ? "Insert input, then run the machine." : machine.nextAction();
   }

   public boolean recallVisible() {
      return kind() == ReclamationMachineBlock.MachineKind.POLLINATOR_DRONE_DOCK;
   }

   private static Slot validatingSlot(Container container, int slot, int x, int y) {
      return new Slot(container, slot, x, y) {
         @Override
         public boolean mayPlace(ItemStack stack) {
            return container.canPlaceItem(slot, stack);
         }

         @Override
         public boolean mayPickup(Player player) {
            return !(container instanceof ReclamationMachineBlockEntity machine) || !machine.isProtectedOperationSlot(slot);
         }
      };
   }

   private static Slot outputSlot(Container container, int slot, int x, int y) {
      return new Slot(container, slot, x, y) {
         @Override
         public boolean mayPlace(ItemStack stack) {
            return false;
         }
      };
   }
}
