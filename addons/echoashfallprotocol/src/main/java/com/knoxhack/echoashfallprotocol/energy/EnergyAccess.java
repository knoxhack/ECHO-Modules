package com.knoxhack.echoashfallprotocol.energy;

import com.knoxhack.echo.adaptercore.EchoBackendEnergyBridge;
import com.knoxhack.echo.adaptercore.EchoEnergyHandler;
import com.knoxhack.echoashfallprotocol.capability.IEnergyStorage;
import com.knoxhack.echoashfallprotocol.block.entity.FactoryControllerBlockEntity;
import com.knoxhack.echoashfallprotocol.item.BatteryItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import com.knoxhack.echoashfallprotocol.power.PowerNetwork;

public final class EnergyAccess {
    private EnergyAccess() {
    }

    public static EchoEnergyHandler wrap(IEnergyStorage storage, Runnable onChanged) {
        return EchoBackendEnergyBridge.nativeHandler(storage, onChanged);
    }

    public static Object backendEnergyHandler(IEnergyStorage storage, Runnable onChanged) {
        return EchoBackendEnergyBridge.backendHandler(storage, onChanged);
    }

    public static boolean isEnergyItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.getItem() instanceof BatteryItem) {
            return true;
        }
        return EchoBackendEnergyBridge.itemEnergyHandler(stack) != null;
    }

    public static int insert(EchoEnergyHandler handler, int amount) {
        if (handler == null || amount <= 0) {
            return 0;
        }
        return handler.insertEnergy(amount);
    }

    public static int extract(EchoEnergyHandler handler, int amount) {
        if (handler == null || amount <= 0) {
            return 0;
        }
        return handler.extractEnergy(amount);
    }

    public static int simulateInsert(EchoEnergyHandler handler, int amount) {
        if (handler == null || amount <= 0) {
            return 0;
        }
        return handler.simulateInsertEnergy(amount);
    }

    public static int simulateExtract(EchoEnergyHandler handler, int amount) {
        if (handler == null || amount <= 0) {
            return 0;
        }
        return handler.simulateExtractEnergy(amount);
    }

    public static EchoEnergyHandler getItemEnergy(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        return EchoBackendEnergyBridge.itemEnergyHandler(stack);
    }

    public static EchoEnergyHandler getBlockEnergy(Level level, BlockPos pos, Direction side) {
        BlockEntity be = level.getBlockEntity(pos);
        EchoEnergyHandler handler = EchoBackendEnergyBridge.blockEnergyHandler(level, pos, side);
        if (handler != null) {
            return handler;
        }
        if (be instanceof IEnergyStorage storage) {
            return wrap(storage, be::setChanged);
        }
        return null;
    }

    public static int insertBlockEnergy(Level level, BlockPos pos, Direction side, int amount) {
        return insert(getBlockEnergy(level, pos, side), amount);
    }

    public static int extractBlockEnergy(Level level, BlockPos pos, Direction side, int amount) {
        return extract(getBlockEnergy(level, pos, side), amount);
    }

    public static int simulateInsertBlockEnergy(Level level, BlockPos pos, Direction side, int amount) {
        return simulateInsert(getBlockEnergy(level, pos, side), amount);
    }

    public static int simulateExtractBlockEnergy(Level level, BlockPos pos, Direction side, int amount) {
        return simulateExtract(getBlockEnergy(level, pos, side), amount);
    }

    public static int getBlockEnergyStored(Level level, BlockPos pos, Direction side) {
        EchoEnergyHandler handler = getBlockEnergy(level, pos, side);
        return handler == null ? 0 : handler.amountAsInt();
    }

    public static int getBlockEnergyCapacity(Level level, BlockPos pos, Direction side) {
        EchoEnergyHandler handler = getBlockEnergy(level, pos, side);
        return handler == null ? 0 : handler.capacityAsInt();
    }

    public static int transferFromStorageToBlock(IEnergyStorage source, Level level, BlockPos targetPos, Direction side, int maxAmount) {
        if (source == null || maxAmount <= 0 || !source.canExtract()) {
            return 0;
        }
        EchoEnergyHandler target = getBlockEnergy(level, targetPos, side);
        if (target == null) {
            return 0;
        }
        int movable = Math.min(maxAmount, source.extractEnergy(maxAmount, true));
        int accepted = simulateInsert(target, movable);
        int extracted = source.extractEnergy(accepted, false);
        int inserted = insert(target, extracted);
        if (inserted < extracted) {
            source.receiveEnergy(extracted - inserted, false);
        }
        return inserted;
    }

    public static boolean hasLocalOrNetworkPower(IEnergyStorage storage, Level level, BlockPos pos, int amount) {
        if (FactoryControllerBlockEntity.isMachinePausedByController(level, pos)) {
            return false;
        }
        if (storage != null && storage.extractEnergy(amount, true) >= amount) {
            return true;
        }
        return PowerNetwork.hasPowerAccess(level, pos);
    }

    public static boolean tryConsumeLocalOrNetworkPower(IEnergyStorage storage, Level level, BlockPos pos, int amount) {
        if (FactoryControllerBlockEntity.isMachinePausedByController(level, pos)) {
            return false;
        }
        if (storage != null && storage.extractEnergy(amount, true) >= amount) {
            storage.extractEnergy(amount, false);
            return true;
        }
        return PowerNetwork.tryConsumePower(level, pos, amount);
    }

    public static int dischargeBatteryToStorage(ItemStack stack, IEnergyStorage storage) {
        if (stack.isEmpty() || storage == null || !storage.canReceive()) {
            return 0;
        }
        EchoEnergyHandler battery = getItemEnergy(stack);
        if (battery == null) {
            return 0;
        }
        int space = Math.max(0, storage.getMaxEnergyStored() - storage.getEnergyStored());
        int extractable = simulateExtract(battery, space);
        int accepted = storage.receiveEnergy(extractable, true);
        int extracted = extract(battery, accepted);
        int inserted = storage.receiveEnergy(extracted, false);
        if (inserted < extracted) {
            insert(battery, extracted - inserted);
        }
        return inserted;
    }

    public static int chargeBatteryFromStorage(ItemStack stack, IEnergyStorage storage) {
        if (stack.isEmpty() || storage == null || !storage.canExtract()) {
            return 0;
        }
        EchoEnergyHandler battery = getItemEnergy(stack);
        if (battery == null) {
            return 0;
        }
        int accepted = simulateInsert(battery, storage.extractEnergy(Integer.MAX_VALUE, true));
        int extracted = storage.extractEnergy(accepted, false);
        int inserted = insert(battery, extracted);
        if (inserted < extracted) {
            storage.receiveEnergy(extracted - inserted, false);
        }
        return inserted;
    }

}
