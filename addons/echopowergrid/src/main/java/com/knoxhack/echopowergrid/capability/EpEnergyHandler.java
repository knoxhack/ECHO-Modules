package com.knoxhack.echopowergrid.capability;

import com.knoxhack.echopowergrid.api.EchoEnergyStorage;
import com.knoxhack.echopowergrid.config.PowerGridConfig;

public class EpEnergyHandler {
    private final EchoEnergyStorage storage;
    private final Runnable onChanged;
    private Long snapshot;

    public EpEnergyHandler(EchoEnergyStorage storage, Runnable onChanged) {
        this.storage = storage;
        this.onChanged = onChanged;
    }

    public int insert(int amount, Object transaction) {
        if (!PowerGridConfig.ENABLE_FE_BRIDGE.get() || !storage.canReceive() || amount <= 0) return 0;
        long epAmount = (long) (amount * PowerGridConfig.FE_TO_EP_RATIO.get());
        long receivedEp = storage.receiveEnergy(epAmount, true);
        int received = (int) Math.min(Integer.MAX_VALUE, receivedEp / PowerGridConfig.FE_TO_EP_RATIO.get());
        if (received > 0) {
            if (transaction != null) {
                snapshot = createSnapshot();
            }
            storage.receiveEnergy((long) (received * PowerGridConfig.FE_TO_EP_RATIO.get()), false);
            if (transaction == null && onChanged != null) {
                onChanged.run();
            }
        }
        return received;
    }

    public int extract(int amount, Object transaction) {
        if (!PowerGridConfig.ENABLE_FE_BRIDGE.get() || !storage.canExtract() || amount <= 0) return 0;
        long epAmount = (long) (amount * PowerGridConfig.FE_TO_EP_RATIO.get());
        long extractedEp = storage.extractEnergy(epAmount, true);
        int extracted = (int) Math.min(Integer.MAX_VALUE, extractedEp / PowerGridConfig.FE_TO_EP_RATIO.get());
        if (extracted > 0) {
            if (transaction != null) {
                snapshot = createSnapshot();
            }
            storage.extractEnergy((long) (extracted * PowerGridConfig.FE_TO_EP_RATIO.get()), false);
            if (transaction == null && onChanged != null) {
                onChanged.run();
            }
        }
        return extracted;
    }

    public int getEnergyStored() {
        return (int) Math.min(Integer.MAX_VALUE, storage.getEnergyStored() / PowerGridConfig.EP_TO_FE_RATIO.get());
    }

    public long getAmountAsLong() {
        return (long) (storage.getEnergyStored() / PowerGridConfig.EP_TO_FE_RATIO.get());
    }

    public long getCapacityAsLong() {
        return (long) (storage.getMaxEnergyStored() / PowerGridConfig.EP_TO_FE_RATIO.get());
    }

    public Long createSnapshot() {
        return storage.getEnergyStored();
    }

    public void revertToSnapshot(Long snapshot) {
        long target = Math.max(0L, Math.min(storage.getMaxEnergyStored(), snapshot == null ? 0L : snapshot));
        long current = storage.getEnergyStored();
        if (current > target) {
            drainDirect(current - target);
        } else if (current < target) {
            fillDirect(target - current);
        }
    }

    public void rollbackPendingSnapshot() {
        revertToSnapshot(snapshot);
        snapshot = null;
    }

    public void onRootCommit(Long snapshot) {
        if (onChanged != null) onChanged.run();
    }

    private void drainDirect(long amount) {
        long remaining = amount;
        int guard = 0;
        while (remaining > 0L && guard++ < 1024) {
            long extracted = storage.extractEnergy(remaining, false);
            if (extracted <= 0L) {
                break;
            }
            remaining -= extracted;
        }
    }

    private void fillDirect(long amount) {
        long remaining = amount;
        int guard = 0;
        while (remaining > 0L && guard++ < 1024) {
            long received = storage.receiveEnergy(remaining, false);
            if (received <= 0L) {
                break;
            }
            remaining -= received;
        }
    }
}
