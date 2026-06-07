package com.knoxhack.echoashfallprotocol.block.menu;

import com.knoxhack.echoashfallprotocol.block.entity.AtmosphericScrubberBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.AutofeedHopperBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.BatteryBankBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.ContaminantCondenserBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.FactoryControllerBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.FieldMedBayBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.NexusCapacitorBlockEntity;
import com.knoxhack.echoashfallprotocol.block.entity.SignalScannerBlockEntity;
import com.knoxhack.echoashfallprotocol.capability.IEnergyStorage;
import com.knoxhack.echoashfallprotocol.energy.EnergyAccess;
import com.knoxhack.echoashfallprotocol.machine.MachineWearData;
import com.knoxhack.echoashfallprotocol.power.PowerDiagnostic;
import com.knoxhack.echoashfallprotocol.power.PowerNetwork;
import com.knoxhack.echoashfallprotocol.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

public class MachineStatusMenu extends AbstractContainerMenu {
    public static final int KIND_BATTERY_BANK = 1;
    public static final int KIND_SIGNAL_SCANNER = 2;
    public static final int KIND_FACTORY_CONTROLLER = 3;
    public static final int KIND_ATMOSPHERIC_SCRUBBER = 4;
    public static final int KIND_FIELD_MED_BAY = 5;
    public static final int KIND_AUTOFEED_HOPPER = 6;
    public static final int KIND_CONTAMINANT_CONDENSER = 7;
    public static final int KIND_NEXUS_CAPACITOR = 8;
    private static final int DATA_COUNT = 14;

    private final BlockEntity blockEntity;
    private final ContainerData data;

    public MachineStatusMenu(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(id, playerInventory, playerInventory.player.level().getBlockEntity(buf.readBlockPos()), new SimpleContainerData(DATA_COUNT));
    }

    public MachineStatusMenu(int id, Inventory playerInventory, BlockEntity blockEntity, ContainerData data) {
        super(ModMenuTypes.MACHINE_STATUS.get(), id);
        this.blockEntity = blockEntity;
        this.data = data;
        addDataSlots(data);

        if (blockEntity instanceof BatteryBankBlockEntity battery) {
            addSlot(new BatterySlot(battery.getInventory(), BatteryBankBlockEntity.BATTERY_SLOT,
                    MachineMenuLayout.STATUS_BATTERY_X, MachineMenuLayout.STATUS_BATTERY_Y));
            addPlayerInventory(playerInventory);
            addPlayerHotbar(playerInventory);
        }
    }

    public int getKind() { return data.get(0); }
    public boolean isActive() { return data.get(1) != 0; }
    public boolean hasPower() { return data.get(2) != 0; }
    public boolean isJammed() { return data.get(3) != 0; }
    public int getWearPercent() { return data.get(4); }
    public int getValue1() { return data.get(5); }
    public int getValue2() { return data.get(6); }
    public int getLocalBuffer() { return data.get(7); }
    public int getLocalCapacity() { return data.get(8); }
    public int getNetworkStored() { return data.get(9); }
    public int getNetworkCapacity() { return data.get(10); }
    public int getTransferLimit() { return data.get(11); }
    public int getEstimatedDemand() { return data.get(12); }
    public int getPowerIssueCode() { return data.get(13); }
    public boolean hasInventorySlots() { return blockEntity instanceof BatteryBankBlockEntity; }

    public String getMachineTitle() {
        return titleForKind(getKind());
    }

    @Override
    public boolean stillValid(Player player) {
        if (blockEntity == null || blockEntity.getLevel() == null) {
            return true;
        }
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
                player, blockEntity.getBlockState().getBlock());
    }

    public static String titleForKind(int kind) {
        return switch (kind) {
            case KIND_BATTERY_BANK -> "Battery Bank";
            case KIND_SIGNAL_SCANNER -> "Signal Scanner";
            case KIND_FACTORY_CONTROLLER -> "Factory Controller";
            case KIND_ATMOSPHERIC_SCRUBBER -> "Atmospheric Scrubber";
            case KIND_FIELD_MED_BAY -> "Field Med Bay";
            case KIND_AUTOFEED_HOPPER -> "Autofeed Hopper";
            case KIND_CONTAMINANT_CONDENSER -> "Contaminant Condenser";
            case KIND_NEXUS_CAPACITOR -> "Nexus Capacitor";
            default -> "Machine Status";
        };
    }

    public static final class Provider implements MenuProvider {
        private final BlockEntity blockEntity;

        public Provider(BlockEntity blockEntity) {
            this.blockEntity = blockEntity;
        }

        @Override
        public Component getDisplayName() {
            return Component.literal(titleForKind(kindOf(blockEntity)));
        }

        @Nullable
        @Override
        public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
            return new MachineStatusMenu(id, inventory, blockEntity, new LiveData(blockEntity));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (!hasInventorySlots() || index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }
        ItemStack original = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot.hasItem()) {
            ItemStack current = slot.getItem();
            original = current.copy();
            if (index == 0) {
                if (!moveItemStackTo(current, 1, 37, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (EnergyAccess.isEnergyItem(current)) {
                if (!moveItemStackTo(current, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                return ItemStack.EMPTY;
            }

            if (current.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return original;
    }

    private static final class LiveData implements ContainerData {
        private final BlockEntity blockEntity;

        private LiveData(BlockEntity blockEntity) {
            this.blockEntity = blockEntity;
        }

        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> kindOf(blockEntity);
                case 1 -> isActive(blockEntity) ? 1 : 0;
                case 2 -> hasPower(blockEntity) ? 1 : 0;
                case 3 -> isJammed(blockEntity) ? 1 : 0;
                case 4 -> wearPercent(blockEntity);
                case 5 -> value1(blockEntity);
                case 6 -> value2(blockEntity);
                case 7 -> diagnostic(blockEntity).localBuffer();
                case 8 -> diagnostic(blockEntity).localCapacity();
                case 9 -> diagnostic(blockEntity).networkStored();
                case 10 -> diagnostic(blockEntity).networkCapacity();
                case 11 -> diagnostic(blockEntity).transferLimit();
                case 12 -> diagnostic(blockEntity).estimatedDemand();
                case 13 -> diagnostic(blockEntity).issue().code();
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

    private static int kindOf(BlockEntity be) {
        if (be instanceof BatteryBankBlockEntity) return KIND_BATTERY_BANK;
        if (be instanceof SignalScannerBlockEntity) return KIND_SIGNAL_SCANNER;
        if (be instanceof FactoryControllerBlockEntity) return KIND_FACTORY_CONTROLLER;
        if (be instanceof AtmosphericScrubberBlockEntity) return KIND_ATMOSPHERIC_SCRUBBER;
        if (be instanceof FieldMedBayBlockEntity) return KIND_FIELD_MED_BAY;
        if (be instanceof AutofeedHopperBlockEntity) return KIND_AUTOFEED_HOPPER;
        if (be instanceof ContaminantCondenserBlockEntity) return KIND_CONTAMINANT_CONDENSER;
        if (be instanceof NexusCapacitorBlockEntity) return KIND_NEXUS_CAPACITOR;
        return 0;
    }

    private static boolean isActive(BlockEntity be) {
        if (be instanceof BatteryBankBlockEntity battery) return battery.getEnergyStored() > 0;
        if (be instanceof SignalScannerBlockEntity scanner) return scanner.isScanCooldownActive();
        if (be instanceof FactoryControllerBlockEntity controller) return controller.isNetworkEnabled();
        if (be instanceof AtmosphericScrubberBlockEntity scrubber) return scrubber.isActive();
        if (be instanceof FieldMedBayBlockEntity medBay) return medBay.isActive();
        if (be instanceof AutofeedHopperBlockEntity hopper) return hopper.isActive();
        if (be instanceof ContaminantCondenserBlockEntity condenser) return condenser.isActive();
        if (be instanceof NexusCapacitorBlockEntity capacitor) return capacitor.getEnergyStored() > 0;
        return false;
    }

    private static boolean hasPower(BlockEntity be) {
        if (be instanceof BatteryBankBlockEntity) return true;
        Level level = be != null ? be.getLevel() : null;
        return level != null && PowerNetwork.diagnose(level, be.getBlockPos()).isPowered();
    }

    private static boolean isJammed(BlockEntity be) {
        if (be == null || be.getLevel() == null) return false;
        return new MachineWearData(be.getLevel()).isJammed(be.getBlockPos());
    }

    private static int wearPercent(BlockEntity be) {
        if (be == null || be.getLevel() == null) return 0;
        return (int) (new MachineWearData(be.getLevel()).getWearPercent(be.getBlockPos()) * 100);
    }

    private static int value1(BlockEntity be) {
        if (be instanceof BatteryBankBlockEntity battery) return battery.getEnergyStored();
        if (be instanceof NexusCapacitorBlockEntity capacitor) return capacitor.getEnergyStored();
        if (be instanceof FactoryControllerBlockEntity controller) return controller.getConnectedMachines();
        if (be instanceof ContaminantCondenserBlockEntity condenser) return condenser.getBlocksProcessed();
        if (be instanceof SignalScannerBlockEntity scanner) return scanner.isScanCooldownActive() ? 1 : 0;
        if (be instanceof AtmosphericScrubberBlockEntity) return 16;
        if (be instanceof FieldMedBayBlockEntity) return 8;
        if (be instanceof AutofeedHopperBlockEntity) return 8;
        return 0;
    }

    private static int value2(BlockEntity be) {
        if (be instanceof BatteryBankBlockEntity battery) return battery.getMaxEnergyStored();
        if (be instanceof NexusCapacitorBlockEntity capacitor) return capacitor.getMaxEnergyStored();
        if (be instanceof FactoryControllerBlockEntity controller) return controller.getActiveMachines();
        if (be instanceof ContaminantCondenserBlockEntity) return 3;
        if (be instanceof SignalScannerBlockEntity) return 50;
        if (be instanceof AtmosphericScrubberBlockEntity) return 2;
        if (be instanceof FieldMedBayBlockEntity) return 2;
        if (be instanceof AutofeedHopperBlockEntity) return 10;
        return 0;
    }

    private static PowerDiagnostic diagnostic(BlockEntity be) {
        if (be == null || be.getLevel() == null) {
            return new PowerDiagnostic(0, 0, 0, 0, 0, 0,
                    com.knoxhack.echoashfallprotocol.block.entity.LoadDistributorBlockEntity.PriorityMode.BALANCED,
                    com.knoxhack.echoashfallprotocol.power.PowerIssue.NO_LINK);
        }
        int demand = isStorageOnly(be) ? 0 : Math.max(1, PowerNetwork.estimateDemand(be.getLevel(), be.getBlockPos()));
        return PowerNetwork.diagnose(be.getLevel(), be.getBlockPos(), demand);
    }

    private static boolean isStorageOnly(BlockEntity be) {
        return be instanceof BatteryBankBlockEntity
                || be instanceof NexusCapacitorBlockEntity
                || (be instanceof IEnergyStorage && PowerNetwork.canSupplyNetwork(be));
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        MachineMenuLayout.STATUS_PLAYER_INV_X + col * 18,
                        MachineMenuLayout.STATUS_PLAYER_INV_Y + row * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col,
                    MachineMenuLayout.STATUS_PLAYER_INV_X + col * 18, MachineMenuLayout.STATUS_HOTBAR_Y));
        }
    }
}
