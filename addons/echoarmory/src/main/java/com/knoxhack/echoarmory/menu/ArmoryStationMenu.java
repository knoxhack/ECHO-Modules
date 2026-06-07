package com.knoxhack.echoarmory.menu;

import com.knoxhack.echoarmory.block.ArmoryStationBlock.StationKind;
import com.knoxhack.echoarmory.block.entity.ArmoryStationBlockEntity;
import com.knoxhack.echoarmory.registry.ModMenus;
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

public class ArmoryStationMenu extends AbstractContainerMenu {
   public static final int GUI_WIDTH = 352;
   public static final int GUI_HEIGHT = 286;
   public static final int BUTTON_SCAN = 0;
   public static final int BUTTON_APPLY = 1;
   public static final int BUTTON_CYCLE = 2;
   private static final int PLAYER_INV_START = ArmoryStationBlockEntity.SLOT_COUNT;
   private static final int PLAYER_INV_END = PLAYER_INV_START + 27;
   private static final int HOTBAR_END = PLAYER_INV_END + 9;
   private final Container container;
   private final ContainerData data;
   private final ArmoryStationBlockEntity station;

   public ArmoryStationMenu(int containerId, Inventory playerInventory, Container container) {
      this(containerId, playerInventory, container, new SimpleContainerData(ArmoryStationBlockEntity.DATA_COUNT));
   }

   public ArmoryStationMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
      super(ModMenus.ARMORY_STATION.get(), containerId);
      checkContainerSize(container, ArmoryStationBlockEntity.SLOT_COUNT);
      checkContainerDataCount(data, ArmoryStationBlockEntity.DATA_COUNT);
      this.container = container;
      this.data = data;
      this.station = container instanceof ArmoryStationBlockEntity blockEntity ? blockEntity : null;

      this.addSlot(validatingSlot(container, ArmoryStationBlockEntity.GEAR_SLOT, 44, 70));
      this.addSlot(validatingSlot(container, ArmoryStationBlockEntity.MODULE_SLOT, 80, 70));
      this.addSlot(validatingSlot(container, ArmoryStationBlockEntity.AUX_SLOT, 116, 70));
      for (int i = 3; i < ArmoryStationBlockEntity.SLOT_COUNT; i++) {
         this.addSlot(new Slot(container, i, 44 + (i - 3) * 18, 112));
      }
      this.addStandardInventorySlots(playerInventory, 97, 150);
      this.addDataSlots(data);
   }

   public static ArmoryStationMenu fromNetwork(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
      BlockPos pos = buffer.readBlockPos();
      BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
      if (blockEntity instanceof ArmoryStationBlockEntity station) {
         return new ArmoryStationMenu(containerId, inventory, station, station.data());
      }
      return new ArmoryStationMenu(containerId, inventory, new SimpleContainer(ArmoryStationBlockEntity.SLOT_COUNT));
   }

   @Override
   public ItemStack quickMoveStack(Player player, int slotIndex) {
      ItemStack copy = ItemStack.EMPTY;
      Slot slot = this.slots.get(slotIndex);
      if (slot != null && slot.hasItem()) {
         ItemStack stack = slot.getItem();
         copy = stack.copy();
         if (station != null && station.isOperationActive() && station.isProtectedOperationSlot(slotIndex)) {
            return ItemStack.EMPTY;
         }
         if (slotIndex < ArmoryStationBlockEntity.SLOT_COUNT) {
            if (!this.moveItemStackTo(stack, PLAYER_INV_START, HOTBAR_END, true)) {
               return ItemStack.EMPTY;
            }
         } else if (!this.moveItemStackTo(stack, 0, ArmoryStationBlockEntity.SLOT_COUNT, false)) {
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
      return station != null && station.handleMenuButton(player, id);
   }

   public StationKind kind() {
      StationKind[] values = StationKind.values();
      int id = data.get(ArmoryStationBlockEntity.DATA_KIND);
      return id >= 0 && id < values.length ? values[id] : StationKind.ARMORY_BENCH;
   }

   public int progress() {
      return data.get(ArmoryStationBlockEntity.DATA_PROGRESS);
   }

   public int energy() {
      return data.get(ArmoryStationBlockEntity.DATA_ENERGY);
   }

   public int moduleCount() {
      return data.get(ArmoryStationBlockEntity.DATA_MODULES);
   }

   public int instability() {
      return data.get(ArmoryStationBlockEntity.DATA_INSTABILITY);
   }

   public int energyCapacity() {
      return data.get(ArmoryStationBlockEntity.DATA_ENERGY_CAPACITY);
   }

   public boolean previewAccepted() {
      return data.get(ArmoryStationBlockEntity.DATA_PREVIEW_ACCEPTED) > 0;
   }

   public int fuelCount() {
      return data.get(ArmoryStationBlockEntity.DATA_FUEL_COUNT);
   }

   private static Slot validatingSlot(Container container, int slot, int x, int y) {
      return new Slot(container, slot, x, y) {
         @Override
         public boolean mayPlace(ItemStack stack) {
            return container.canPlaceItem(slot, stack);
         }

         @Override
         public boolean mayPickup(Player player) {
            return !(container instanceof ArmoryStationBlockEntity station)
               || !station.isOperationActive()
               || !station.isProtectedOperationSlot(slot);
         }
      };
   }
}
