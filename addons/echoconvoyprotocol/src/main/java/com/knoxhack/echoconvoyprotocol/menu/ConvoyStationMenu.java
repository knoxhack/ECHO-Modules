package com.knoxhack.echoconvoyprotocol.menu;

import com.knoxhack.echoconvoyprotocol.block.ConvoyBlock.ConvoyBlockKind;
import com.knoxhack.echoconvoyprotocol.block.entity.ConvoyStationBlockEntity;
import com.knoxhack.echoconvoyprotocol.registry.ModMenus;
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

public class ConvoyStationMenu extends AbstractContainerMenu {
   public static final int GUI_WIDTH = 352;
   public static final int GUI_HEIGHT = 286;
   public static final int BUTTON_SCAN = 0;
   public static final int BUTTON_UNLOAD = 1;
   private static final int PLAYER_INV_START = ConvoyStationBlockEntity.SLOT_COUNT;
   private static final int PLAYER_INV_END = PLAYER_INV_START + 27;
   private static final int HOTBAR_END = PLAYER_INV_END + 9;

   private final Container container;
   private final ContainerData data;
   private final ConvoyStationBlockEntity station;

   public ConvoyStationMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
      super(ModMenus.CONVOY_STATION.get(), containerId);
      checkContainerSize(container, ConvoyStationBlockEntity.SLOT_COUNT);
      checkContainerDataCount(data, ConvoyStationBlockEntity.DATA_COUNT);
      this.container = container;
      this.data = data;
      this.station = container instanceof ConvoyStationBlockEntity blockEntity ? blockEntity : null;

      this.addSlot(new Slot(container, ConvoyStationBlockEntity.INPUT_SLOT, 72, 64));
      this.addSlot(new Slot(container, ConvoyStationBlockEntity.OUTPUT_SLOT, 246, 64) {
         @Override
         public boolean mayPlace(ItemStack stack) {
            return false;
         }
      });
      for (int row = 0; row < 1; row++) {
         for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(container, ConvoyStationBlockEntity.STORAGE_START + col + row * 9, 96 + col * 18, 106 + row * 18));
         }
      }
      this.addStandardInventorySlots(playerInventory, 96, 150);
      this.addDataSlots(data);
   }

   public static ConvoyStationMenu fromNetwork(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
      BlockPos pos = buffer.readBlockPos();
      BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
      if (blockEntity instanceof ConvoyStationBlockEntity station) {
         return new ConvoyStationMenu(containerId, inventory, station, station.data());
      }
      return new ConvoyStationMenu(containerId, inventory, new SimpleContainer(ConvoyStationBlockEntity.SLOT_COUNT),
         new SimpleContainerData(ConvoyStationBlockEntity.DATA_COUNT));
   }

   @Override
   public ItemStack quickMoveStack(Player player, int slotIndex) {
      ItemStack copy = ItemStack.EMPTY;
      Slot slot = this.slots.get(slotIndex);
      if (slot != null && slot.hasItem()) {
         ItemStack stack = slot.getItem();
         copy = stack.copy();
         if (slotIndex < ConvoyStationBlockEntity.SLOT_COUNT) {
            if (!moveItemStackTo(stack, PLAYER_INV_START, HOTBAR_END, true)) {
               return ItemStack.EMPTY;
            }
         } else if (!moveItemStackTo(stack, ConvoyStationBlockEntity.INPUT_SLOT, ConvoyStationBlockEntity.INPUT_SLOT + 1, false)) {
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

   public ConvoyBlockKind kind() {
      ConvoyBlockKind[] values = ConvoyBlockKind.values();
      int id = data.get(ConvoyStationBlockEntity.DATA_KIND);
      return id >= 0 && id < values.length ? values[id] : ConvoyBlockKind.VEHICLE_WORKBENCH;
   }

   public int progress() {
      return data.get(ConvoyStationBlockEntity.DATA_PROGRESS);
   }

   public int maxProgress() {
      return data.get(ConvoyStationBlockEntity.DATA_MAX_PROGRESS);
   }

   public int energy() {
      return data.get(ConvoyStationBlockEntity.DATA_ENERGY);
   }

   public int maxEnergy() {
      return data.get(ConvoyStationBlockEntity.DATA_MAX_ENERGY);
   }

   public int statusId() {
      return data.get(ConvoyStationBlockEntity.DATA_STATUS);
   }

   public int nearbyVehicles() {
      return data.get(ConvoyStationBlockEntity.DATA_NEARBY_VEHICLES);
   }
}
