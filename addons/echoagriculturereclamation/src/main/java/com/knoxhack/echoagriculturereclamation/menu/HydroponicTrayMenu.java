package com.knoxhack.echoagriculturereclamation.menu;

import com.knoxhack.echoagriculturereclamation.block.entity.HydroponicTrayBlockEntity;
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

public class HydroponicTrayMenu extends AbstractContainerMenu {
   public static final int GUI_WIDTH = 236;
   public static final int GUI_HEIGHT = 214;
   private static final int PLAYER_INV_START = HydroponicTrayBlockEntity.SLOT_COUNT;
   private static final int PLAYER_INV_END = PLAYER_INV_START + 27;
   private static final int HOTBAR_END = PLAYER_INV_END + 9;
   private final Container container;
   private final ContainerData data;
   private final HydroponicTrayBlockEntity tray;

   public HydroponicTrayMenu(int containerId, Inventory playerInventory, Container container) {
      this(containerId, playerInventory, container, new SimpleContainerData(HydroponicTrayBlockEntity.DATA_COUNT));
   }

   public HydroponicTrayMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
      super(ModMenus.HYDROPONIC_TRAY.get(), containerId);
      checkContainerSize(container, HydroponicTrayBlockEntity.SLOT_COUNT);
      checkContainerDataCount(data, HydroponicTrayBlockEntity.DATA_COUNT);
      this.container = container;
      this.data = data;
      this.tray = container instanceof HydroponicTrayBlockEntity blockEntity ? blockEntity : null;

      addSlot(validatingSlot(container, HydroponicTrayBlockEntity.SEED_SLOT, 32, 58));
      addSlot(validatingSlot(container, HydroponicTrayBlockEntity.NUTRIENT_SLOT, 32, 88));
      addSlot(outputSlot(container, HydroponicTrayBlockEntity.OUTPUT_SLOT, 188, 74));
      addStandardInventorySlots(playerInventory, 38, 118);
      addDataSlots(data);
   }

   public static HydroponicTrayMenu fromNetwork(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
      BlockPos pos = buffer.readBlockPos();
      BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
      if (blockEntity instanceof HydroponicTrayBlockEntity tray) {
         return new HydroponicTrayMenu(containerId, inventory, tray, tray.data());
      }
      return new HydroponicTrayMenu(containerId, inventory, new SimpleContainer(HydroponicTrayBlockEntity.SLOT_COUNT));
   }

   @Override
   public ItemStack quickMoveStack(Player player, int slotIndex) {
      ItemStack copy = ItemStack.EMPTY;
      Slot slot = slots.get(slotIndex);
      if (slot != null && slot.hasItem()) {
         ItemStack stack = slot.getItem();
         copy = stack.copy();
         if (slotIndex < HydroponicTrayBlockEntity.SLOT_COUNT) {
            if (!moveItemStackTo(stack, PLAYER_INV_START, HOTBAR_END, true)) {
               return ItemStack.EMPTY;
            }
         } else if (!moveItemStackTo(stack, HydroponicTrayBlockEntity.SEED_SLOT, HydroponicTrayBlockEntity.OUTPUT_SLOT, false)) {
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

   public boolean hasProfile() {
      return data.get(HydroponicTrayBlockEntity.DATA_HAS_PROFILE) > 0;
   }

   public int age() {
      return data.get(HydroponicTrayBlockEntity.DATA_AGE);
   }

   public int growthTicks() {
      return data.get(HydroponicTrayBlockEntity.DATA_GROWTH_TICKS);
   }

   public int growthTicksMax() {
      return Math.max(1, data.get(HydroponicTrayBlockEntity.DATA_GROWTH_TICKS_MAX));
   }

   public int nutrient() {
      return data.get(HydroponicTrayBlockEntity.DATA_NUTRIENT);
   }

   public int nutrientCap() {
      return Math.max(1, data.get(HydroponicTrayBlockEntity.DATA_NUTRIENT_CAP));
   }

   public int stability() {
      return data.get(HydroponicTrayBlockEntity.DATA_STABILITY);
   }

   public int contamination() {
      return data.get(HydroponicTrayBlockEntity.DATA_CONTAMINATION);
   }

   public int greenhouseSafety() {
      return data.get(HydroponicTrayBlockEntity.DATA_GREENHOUSE);
   }

   public String statusLine() {
      return tray == null ? "Awaiting tray telemetry." : tray.statusLine();
   }

   public String statusLabel() {
      return switch (data.get(HydroponicTrayBlockEntity.DATA_STATUS)) {
         case 1 -> "GROWING";
         case 2 -> "NUTRIENT LOW";
         case 3 -> "READY";
         default -> "EMPTY";
      };
   }

   private static Slot validatingSlot(Container container, int slot, int x, int y) {
      return new Slot(container, slot, x, y) {
         @Override
         public boolean mayPlace(ItemStack stack) {
            return container.canPlaceItem(slot, stack);
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
