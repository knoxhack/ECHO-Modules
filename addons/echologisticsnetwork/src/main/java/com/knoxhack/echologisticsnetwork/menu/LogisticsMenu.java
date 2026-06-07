package com.knoxhack.echologisticsnetwork.menu;

import com.knoxhack.echologisticsnetwork.block.LogisticsBlock.LogisticsKind;
import com.knoxhack.echologisticsnetwork.block.entity.LogisticsBlockEntity;
import com.knoxhack.echologisticsnetwork.registry.ModMenus;
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

public class LogisticsMenu extends AbstractContainerMenu {
   public static final int GUI_WIDTH = 352;
   public static final int GUI_HEIGHT = 286;
   public static final int BUTTON_SCAN = 0;
   public static final int BUTTON_REQUEST_LOADOUT = 1;
   public static final int BUTTON_CLAIM_RELAY = 2;
   public static final int BUTTON_DEPOT_EXCHANGE = 3;
   public static final int BUTTON_CYCLE_LOADOUT = 4;
   public static final int BUTTON_CANCEL_DELIVERIES = 5;
   public static final int BUTTON_REFRESH_OFFERS = 6;
   private static final int PLAYER_INV_START = LogisticsBlockEntity.SLOT_COUNT;
   private static final int PLAYER_INV_END = PLAYER_INV_START + 27;
   private static final int HOTBAR_END = PLAYER_INV_END + 9;

   private final Container container;
   private final ContainerData data;
   private final LogisticsBlockEntity logistics;

   public LogisticsMenu(int containerId, Inventory playerInventory, Container container) {
      this(containerId, playerInventory, container, new SimpleContainerData(LogisticsBlockEntity.DATA_COUNT));
   }

   public LogisticsMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
      super(ModMenus.LOGISTICS.get(), containerId);
      checkContainerSize(container, LogisticsBlockEntity.SLOT_COUNT);
      checkContainerDataCount(data, LogisticsBlockEntity.DATA_COUNT);
      this.container = container;
      this.data = data;
      this.logistics = container instanceof LogisticsBlockEntity blockEntity ? blockEntity : null;

      for (int row = 0; row < 3; row++) {
         for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(container, col + row * 9, 97 + col * 18, 70 + row * 18));
         }
      }
      this.addStandardInventorySlots(playerInventory, 97, 150);
      this.addDataSlots(data);
   }

   public static LogisticsMenu fromNetwork(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
      BlockPos pos = buffer.readBlockPos();
      BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
      if (blockEntity instanceof LogisticsBlockEntity logistics) {
         return new LogisticsMenu(containerId, inventory, logistics, logistics.data());
      }
      return new LogisticsMenu(containerId, inventory, new SimpleContainer(LogisticsBlockEntity.SLOT_COUNT));
   }

   @Override
   public ItemStack quickMoveStack(Player player, int slotIndex) {
      ItemStack copy = ItemStack.EMPTY;
      Slot slot = this.slots.get(slotIndex);
      if (slot != null && slot.hasItem()) {
         ItemStack stack = slot.getItem();
         copy = stack.copy();
         if (slotIndex < LogisticsBlockEntity.SLOT_COUNT) {
            if (!this.moveItemStackTo(stack, PLAYER_INV_START, HOTBAR_END, true)) {
               return ItemStack.EMPTY;
            }
         } else if (!this.moveItemStackTo(stack, 0, LogisticsBlockEntity.SLOT_COUNT, false)) {
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
      return logistics != null && logistics.handleMenuButton(player, id);
   }

   public LogisticsKind kind() {
      LogisticsKind[] values = LogisticsKind.values();
      int id = data.get(LogisticsBlockEntity.DATA_KIND);
      return id >= 0 && id < values.length ? values[id] : LogisticsKind.LOGISTICS_TERMINAL;
   }

   public int stockRows() {
      return data.get(LogisticsBlockEntity.DATA_STOCK_ROWS);
   }

   public int missingRows() {
      return data.get(LogisticsBlockEntity.DATA_MISSING_ROWS);
   }

   public int readyRows() {
      return data.get(LogisticsBlockEntity.DATA_READY_ROWS);
   }

   public int activeDeliveries() {
      return data.get(LogisticsBlockEntity.DATA_ACTIVE_DELIVERIES);
   }

   public int depotOffers() {
      return data.get(LogisticsBlockEntity.DATA_DEPOT_OFFERS);
   }

   public int cooldown() {
      return data.get(LogisticsBlockEntity.DATA_COOLDOWN);
   }

   public int rewardCount() {
      return data.get(LogisticsBlockEntity.DATA_REWARD_COUNT);
   }

   public int blockCount() {
      return data.get(LogisticsBlockEntity.DATA_BLOCK_COUNT);
   }

   public int endpointCount() {
      return data.get(LogisticsBlockEntity.DATA_ENDPOINT_COUNT);
   }

   public boolean dockOnline() {
      return data.get(LogisticsBlockEntity.DATA_DOCK_ONLINE) > 0;
   }

   public boolean relayOnline() {
      return data.get(LogisticsBlockEntity.DATA_RELAY_ONLINE) > 0;
   }

   public boolean depotOnline() {
      return data.get(LogisticsBlockEntity.DATA_DEPOT_ONLINE) > 0;
   }

   public int depotCooldown() {
      return data.get(LogisticsBlockEntity.DATA_DEPOT_COOLDOWN);
   }

   public boolean selectedReady() {
      return data.get(LogisticsBlockEntity.DATA_SELECTED_READY) > 0;
   }

   public int selectedMissing() {
      return data.get(LogisticsBlockEntity.DATA_SELECTED_MISSING);
   }

   public boolean canRequest() {
      return data.get(LogisticsBlockEntity.DATA_CAN_REQUEST) > 0;
   }

   public boolean canDispatch() {
      return data.get(LogisticsBlockEntity.DATA_CAN_DISPATCH) > 0;
   }

   public String dispatchButtonLabel() {
      if (cooldown() > 0) {
         return "COOLDOWN";
      }
      if (selectedEndpointKind() == null) {
         return "NO TARGET";
      }
      if (!dockOnline()) {
         return "NO DOCK";
      }
      if (!selectedReady()) {
         return selectedMissing() > 0 ? "MISSING" : "BLOCKED";
      }
      return "DISPATCH";
   }

   public String dispatchBlockedReason() {
      if (cooldown() > 0) {
         return "Controls cooling down " + cooldown() + "t";
      }
      if (selectedEndpointKind() == null) {
         return "No request endpoint";
      }
      if (!dockOnline()) {
         return "Dock offline";
      }
      if (!selectedReady()) {
         return selectedMissing() > 0 ? "Missing " + selectedMissing() : "Loadout blocked";
      }
      return "Ready";
   }

   public LogisticsKind selectedEndpointKind() {
      LogisticsKind[] values = LogisticsKind.values();
      int id = data.get(LogisticsBlockEntity.DATA_SELECTED_ENDPOINT_KIND);
      return id >= 0 && id < values.length ? values[id] : null;
   }

   public int firstDeliveryEta() {
      return data.get(LogisticsBlockEntity.DATA_FIRST_DELIVERY_ETA);
   }

   public String firstDeliveryStatus() {
      return switch (data.get(LogisticsBlockEntity.DATA_FIRST_DELIVERY_STATUS)) {
         case 1 -> "IN TRANSIT";
         case 2 -> "DELIVERED";
         case 3 -> "RECOVERING";
         case 4 -> "COMPLETE";
         default -> "IDLE";
      };
   }
}
