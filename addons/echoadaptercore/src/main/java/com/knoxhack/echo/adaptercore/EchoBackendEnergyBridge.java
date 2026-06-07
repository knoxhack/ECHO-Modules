package com.knoxhack.echo.adaptercore;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.ItemAccessEnergyHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * AdapterCore backend bridge for mutable energy storage.
 */
public final class EchoBackendEnergyBridge {
    private EchoBackendEnergyBridge() {
    }

    public static EchoEnergyHandler nativeHandler(Object backendHandler) {
        if (backendHandler instanceof EchoEnergyHandler handler) {
            return handler;
        }
        if (backendHandler instanceof EnergyHandler handler) {
            return new BackendEnergyHandler(handler);
        }
        return null;
    }

    public static EchoEnergyHandler nativeHandler(EchoMutableEnergyStorage storage, Runnable onChanged) {
        return storage == null ? null : new MutableStorageBackendHandler(storage, onChanged);
    }

    public static Object backendHandler(EchoMutableEnergyStorage storage, Runnable onChanged) {
        return storage == null ? null : new MutableStorageBackendHandler(storage, onChanged);
    }

    public static EchoEnergyHandler itemEnergyHandler(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        return nativeHandler(Capabilities.Energy.ITEM.getCapability(stack, ItemAccess.forStack(stack)));
    }

    public static EchoEnergyHandler blockEnergyHandler(Level level, BlockPos pos, Direction side) {
        if (level == null || pos == null) {
            return null;
        }
        BlockState state = level.getBlockState(pos);
        return nativeHandler(Capabilities.Energy.BLOCK.getCapability(level, pos, state, level.getBlockEntity(pos), side));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void registerBlockEntityEnergy(Object event, Object blockEntityType,
            EchoBlockEntityCapabilityProvider provider) {
        if (event instanceof RegisterCapabilitiesEvent capabilitiesEvent
                && blockEntityType instanceof BlockEntityType type) {
            capabilitiesEvent.registerBlockEntity(Capabilities.Energy.BLOCK, type,
                    (blockEntity, side) -> (EnergyHandler) provider.get(blockEntity, side));
        }
    }

    public static void registerItemEnergy(Object event, EchoItemCapabilityProvider provider, Item... items) {
        if (event instanceof RegisterCapabilitiesEvent capabilitiesEvent) {
            capabilitiesEvent.registerItem(Capabilities.Energy.ITEM,
                    (stack, access) -> (EnergyHandler) provider.get(stack, access), items);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Object backendItemEnergyHandler(Object access, Object storedEnergyComponent, int capacity,
            int maxReceive, int maxExtract) {
        if (access instanceof ItemAccess itemAccess
                && storedEnergyComponent instanceof DataComponentType componentType) {
            return new ItemAccessEnergyHandler(itemAccess.oneByOne(), componentType, capacity, maxReceive, maxExtract);
        }
        return null;
    }

    private record BackendEnergyHandler(EnergyHandler delegate) implements EchoEnergyHandler {
        @Override
        public long amount() {
            return delegate.getAmountAsLong();
        }

        @Override
        public long capacity() {
            return delegate.getCapacityAsLong();
        }

        @Override
        public int insertEnergy(int amount) {
            try (Transaction transaction = Transaction.openRoot()) {
                int inserted = delegate.insert(amount, transaction);
                transaction.commit();
                return inserted;
            }
        }

        @Override
        public int extractEnergy(int amount) {
            try (Transaction transaction = Transaction.openRoot()) {
                int extracted = delegate.extract(amount, transaction);
                transaction.commit();
                return extracted;
            }
        }

        @Override
        public int simulateInsertEnergy(int amount) {
            try (Transaction transaction = Transaction.openRoot()) {
                return delegate.insert(amount, transaction);
            }
        }

        @Override
        public int simulateExtractEnergy(int amount) {
            try (Transaction transaction = Transaction.openRoot()) {
                return delegate.extract(amount, transaction);
            }
        }
    }

    private static final class MutableStorageBackendHandler extends SnapshotJournal<Integer>
            implements EnergyHandler, EchoEnergyHandler {
        private final EchoMutableEnergyStorage storage;
        private final Runnable onChanged;

        private MutableStorageBackendHandler(EchoMutableEnergyStorage storage, Runnable onChanged) {
            this.storage = storage;
            this.onChanged = onChanged == null ? () -> { } : onChanged;
        }

        @Override
        public long getAmountAsLong() {
            return storage.getEnergyStored();
        }

        @Override
        public long getCapacityAsLong() {
            return storage.getMaxEnergyStored();
        }

        @Override
        public long amount() {
            return getAmountAsLong();
        }

        @Override
        public long capacity() {
            return getCapacityAsLong();
        }

        @Override
        public int insertEnergy(int amount) {
            try (Transaction transaction = Transaction.openRoot()) {
                int inserted = insert(amount, transaction);
                transaction.commit();
                return inserted;
            }
        }

        @Override
        public int extractEnergy(int amount) {
            try (Transaction transaction = Transaction.openRoot()) {
                int extracted = extract(amount, transaction);
                transaction.commit();
                return extracted;
            }
        }

        @Override
        public int simulateInsertEnergy(int amount) {
            try (Transaction transaction = Transaction.openRoot()) {
                return insert(amount, transaction);
            }
        }

        @Override
        public int simulateExtractEnergy(int amount) {
            try (Transaction transaction = Transaction.openRoot()) {
                return extract(amount, transaction);
            }
        }

        @Override
        public int insert(int amount, TransactionContext transaction) {
            if (amount <= 0 || !storage.canReceive()) {
                return 0;
            }
            int accepted = storage.receiveEnergy(amount, true);
            if (accepted > 0) {
                updateSnapshots(transaction);
                storage.receiveEnergy(accepted, false);
            }
            return accepted;
        }

        @Override
        public int extract(int amount, TransactionContext transaction) {
            if (amount <= 0 || !storage.canExtract()) {
                return 0;
            }
            int extracted = storage.extractEnergy(amount, true);
            if (extracted > 0) {
                updateSnapshots(transaction);
                storage.extractEnergy(extracted, false);
            }
            return extracted;
        }

        @Override
        protected Integer createSnapshot() {
            return storage.getEnergyStored();
        }

        @Override
        protected void revertToSnapshot(Integer snapshot) {
            storage.setEnergyStored(snapshot);
        }

        @Override
        protected void onRootCommit(Integer snapshot) {
            onChanged.run();
        }
    }
}
