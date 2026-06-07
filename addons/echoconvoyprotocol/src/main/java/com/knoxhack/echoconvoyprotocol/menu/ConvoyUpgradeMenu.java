package com.knoxhack.echoconvoyprotocol.menu;

import com.knoxhack.echoconvoyprotocol.block.entity.ConvoyStationBlockEntity;
import com.knoxhack.echoconvoyprotocol.entity.ConvoyVehicleEntity;
import com.knoxhack.echoconvoyprotocol.entity.ConvoyVehicleKind;
import com.knoxhack.echoconvoyprotocol.item.VehicleUpgradeItem;
import com.knoxhack.echoconvoyprotocol.registry.ModMenus;
import com.knoxhack.echoconvoyprotocol.upgrade.ConvoyUpgradeSlot;
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
import org.jspecify.annotations.Nullable;

public class ConvoyUpgradeMenu extends AbstractContainerMenu {
   public static final int GUI_WIDTH = 276;
   public static final int GUI_HEIGHT = 236;
   public static final int UPGRADE_SLOT_COUNT = ConvoyUpgradeSlot.values().length;
   public static final int DATA_HAS_VEHICLE = 0;
   public static final int DATA_KIND = 1;
   public static final int DATA_FUEL = 2;
   public static final int DATA_MAX_FUEL = 3;
   public static final int DATA_BATTERY = 4;
   public static final int DATA_MAX_BATTERY = 5;
   public static final int DATA_DAMAGE = 6;
   public static final int DATA_MAX_DAMAGE = 7;
   public static final int DATA_CARGO = 8;
   public static final int DATA_MAX_CARGO = 9;
   public static final int DATA_SCANNER = 10;
   public static final int DATA_SPEED = 11;
   public static final int DATA_TURN = 12;
   public static final int DATA_ARMOR = 13;
   public static final int DATA_COUNT = 14;
   private static final int PLAYER_INV_START = UPGRADE_SLOT_COUNT;
   private static final int PLAYER_INV_END = PLAYER_INV_START + 27;
   private static final int HOTBAR_END = PLAYER_INV_END + 9;

   private final Container container;
   private final ContainerData data;
   @Nullable
   private final ConvoyStationBlockEntity station;

   public ConvoyUpgradeMenu(int containerId, Inventory playerInventory, ConvoyStationBlockEntity station) {
      this(containerId, playerInventory, new VehicleUpgradeContainer(station.nearestUpgradeVehicle(playerInventory.player), station),
         new VehicleUpgradeData(station.nearestUpgradeVehicle(playerInventory.player)), station);
   }

   private ConvoyUpgradeMenu(
      int containerId,
      Inventory playerInventory,
      Container container,
      ContainerData data,
      @Nullable ConvoyStationBlockEntity station
   ) {
      super(ModMenus.VEHICLE_UPGRADES.get(), containerId);
      checkContainerSize(container, UPGRADE_SLOT_COUNT);
      checkContainerDataCount(data, DATA_COUNT);
      this.container = container;
      this.data = data;
      this.station = station;

      ConvoyUpgradeSlot[] slots = ConvoyUpgradeSlot.values();
      for (int i = 0; i < slots.length; i++) {
         this.addSlot(new UpgradeSlot(container, i, 42 + i * 30, 64, slots[i]));
      }
      this.addStandardInventorySlots(playerInventory, 58, 130);
      this.addDataSlots(data);
   }

   public static ConvoyUpgradeMenu fromNetwork(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
      BlockPos pos = buffer.readBlockPos();
      BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
      if (blockEntity instanceof ConvoyStationBlockEntity station) {
         return new ConvoyUpgradeMenu(containerId, inventory, new SimpleContainer(UPGRADE_SLOT_COUNT),
            new SimpleContainerData(DATA_COUNT), station);
      }
      return new ConvoyUpgradeMenu(containerId, inventory, new SimpleContainer(UPGRADE_SLOT_COUNT),
         new SimpleContainerData(DATA_COUNT), null);
   }

   @Override
   public ItemStack quickMoveStack(Player player, int slotIndex) {
      ItemStack copy = ItemStack.EMPTY;
      Slot slot = this.slots.get(slotIndex);
      if (slot != null && slot.hasItem()) {
         ItemStack stack = slot.getItem();
         copy = stack.copy();
         if (slotIndex < UPGRADE_SLOT_COUNT) {
            if (!moveItemStackTo(stack, PLAYER_INV_START, HOTBAR_END, true)) {
               return ItemStack.EMPTY;
            }
         } else if (stack.getItem() instanceof VehicleUpgradeItem upgrade) {
            int target = upgrade.slot().ordinal();
            if (!moveItemStackTo(stack, target, target + 1, false)) {
               return ItemStack.EMPTY;
            }
         } else {
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
      return station == null || station.stillValid(player);
   }

   public boolean hasVehicle() {
      return data.get(DATA_HAS_VEHICLE) > 0;
   }

   public ConvoyVehicleKind vehicleKind() {
      return ConvoyVehicleKind.byId(data.get(DATA_KIND));
   }

   public int fuel() {
      return data.get(DATA_FUEL);
   }

   public int maxFuel() {
      return data.get(DATA_MAX_FUEL);
   }

   public int battery() {
      return data.get(DATA_BATTERY);
   }

   public int maxBattery() {
      return data.get(DATA_MAX_BATTERY);
   }

   public int damage() {
      return data.get(DATA_DAMAGE);
   }

   public int maxDamage() {
      return data.get(DATA_MAX_DAMAGE);
   }

   public int cargo() {
      return data.get(DATA_CARGO);
   }

   public int maxCargo() {
      return data.get(DATA_MAX_CARGO);
   }

   public int scannerRange() {
      return data.get(DATA_SCANNER);
   }

   public double speed() {
      return data.get(DATA_SPEED) / 1000.0D;
   }

   public double turnRate() {
      return data.get(DATA_TURN) / 10.0D;
   }

   public int hazardReductionPercent() {
      return data.get(DATA_ARMOR);
   }

   private static final class UpgradeSlot extends Slot {
      private final ConvoyUpgradeSlot upgradeSlot;

      private UpgradeSlot(Container container, int slot, int x, int y, ConvoyUpgradeSlot upgradeSlot) {
         super(container, slot, x, y);
         this.upgradeSlot = upgradeSlot;
      }

      @Override
      public boolean mayPlace(ItemStack stack) {
         return container instanceof VehicleUpgradeContainer upgrades
            ? upgrades.canInstall(upgradeSlot, stack)
            : stack.getItem() instanceof VehicleUpgradeItem upgrade && upgrade.slot() == upgradeSlot && stack.getCount() == 1;
      }

      @Override
      public boolean mayPickup(Player player) {
         return !(container instanceof VehicleUpgradeContainer upgrades) || upgrades.canRemove(upgradeSlot);
      }

      @Override
      public int getMaxStackSize() {
         return 1;
      }

      @Override
      public int getMaxStackSize(ItemStack stack) {
         return 1;
      }
   }

   private static final class VehicleUpgradeContainer implements Container {
      @Nullable
      private final ConvoyVehicleEntity vehicle;
      private final ConvoyStationBlockEntity station;

      private VehicleUpgradeContainer(@Nullable ConvoyVehicleEntity vehicle, ConvoyStationBlockEntity station) {
         this.vehicle = vehicle;
         this.station = station;
      }

      @Override
      public int getContainerSize() {
         return UPGRADE_SLOT_COUNT;
      }

      @Override
      public boolean isEmpty() {
         for (ConvoyUpgradeSlot slot : ConvoyUpgradeSlot.values()) {
            if (!getItem(slot.ordinal()).isEmpty()) {
               return false;
            }
         }
         return true;
      }

      @Override
      public ItemStack getItem(int slot) {
         return vehicle == null ? ItemStack.EMPTY : vehicle.upgradeStack(ConvoyUpgradeSlot.byId(slot));
      }

      @Override
      public ItemStack removeItem(int slot, int amount) {
         if (vehicle == null || amount <= 0) {
            return ItemStack.EMPTY;
         }
         ItemStack stack = vehicle.upgradeStack(ConvoyUpgradeSlot.byId(slot));
         if (stack.isEmpty()) {
            return ItemStack.EMPTY;
         }
         return amount >= stack.getCount() ? removeItemNoUpdate(slot) : ItemStack.EMPTY;
      }

      @Override
      public ItemStack removeItemNoUpdate(int slot) {
         if (vehicle == null) {
            return ItemStack.EMPTY;
         }
         ItemStack removed = vehicle.removeUpgrade(ConvoyUpgradeSlot.byId(slot));
         setChanged();
         return removed;
      }

      @Override
      public void setItem(int slot, ItemStack stack) {
         if (vehicle == null) {
            return;
         }
         vehicle.setUpgrade(ConvoyUpgradeSlot.byId(slot), stack);
         setChanged();
      }

      @Override
      public void setChanged() {
         station.setChanged();
      }

      @Override
      public boolean stillValid(Player player) {
         return station.stillValid(player);
      }

      @Override
      public void clearContent() {
         if (vehicle == null) {
            return;
         }
         for (ConvoyUpgradeSlot slot : ConvoyUpgradeSlot.values()) {
            vehicle.setUpgrade(slot, ItemStack.EMPTY);
         }
         setChanged();
      }

      private boolean canInstall(ConvoyUpgradeSlot slot, ItemStack stack) {
         return vehicle != null && vehicle.canInstallUpgrade(slot, stack);
      }

      private boolean canRemove(ConvoyUpgradeSlot slot) {
         return vehicle != null && vehicle.canRemoveUpgrade(slot);
      }
   }

   private record VehicleUpgradeData(@Nullable ConvoyVehicleEntity vehicle) implements ContainerData {
      @Override
      public int get(int index) {
         if (vehicle == null) {
            return 0;
         }
         return switch (index) {
            case DATA_HAS_VEHICLE -> 1;
            case DATA_KIND -> vehicle.kind().ordinal();
            case DATA_FUEL -> vehicle.fuel();
            case DATA_MAX_FUEL -> vehicle.maxFuel();
            case DATA_BATTERY -> vehicle.battery();
            case DATA_MAX_BATTERY -> vehicle.maxBattery();
            case DATA_DAMAGE -> vehicle.damage();
            case DATA_MAX_DAMAGE -> vehicle.maxDamage();
            case DATA_CARGO -> vehicle.filledCargoSlots();
            case DATA_MAX_CARGO -> vehicle.cargoSlots();
            case DATA_SCANNER -> vehicle.scannerRange();
            case DATA_SPEED -> (int)Math.round(vehicle.speed() * 1000.0D);
            case DATA_TURN -> Math.round(vehicle.turnRate() * 10.0F);
            case DATA_ARMOR -> (int)Math.round(vehicle.hazardDamageReduction() * 100.0D);
            default -> 0;
         };
      }

      @Override
      public void set(int index, int value) {
      }

      @Override
      public int getCount() {
         return DATA_COUNT;
      }
   }
}
