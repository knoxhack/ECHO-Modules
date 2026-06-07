package com.knoxhack.echoarcanacore.service;

import com.knoxhack.echoarcanacore.api.AetherPlayerData;
import com.knoxhack.echoarcanacore.api.AetherSignalType;
import com.knoxhack.echoarcanacore.api.AetherStorage;
import com.knoxhack.echoarcanacore.api.AetherStorageTarget;
import com.knoxhack.echoarcanacore.api.IAetherService;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public enum PersistentAetherService implements IAetherService {
    INSTANCE;

    private static final String ROOT = "echoarcanacore_aether";
    private static final double DEFAULT_MAX = 100.0D;

    @Override
    public double getAether(Player player, AetherSignalType type) {
        return root(player).getFloatOr(key("current", type), 0.0F);
    }

    @Override
    public double getMaxAether(Player player, AetherSignalType type) {
        return root(player).getFloatOr(key("max", type), (float) DEFAULT_MAX);
    }

    @Override
    public double addAether(Player player, double amount, AetherSignalType type) {
        if (player == null || amount <= 0.0D) {
            return 0.0D;
        }
        AetherSignalType safeType = safe(type);
        double current = getAether(player, safeType);
        double max = getMaxAether(player, safeType);
        double inserted = Math.min(amount, Math.max(0.0D, max - current));
        root(player).putFloat(key("current", safeType), (float) (current + inserted));
        root(player).putBoolean("unlocked_" + safeType.serializedName(), true);
        return inserted;
    }

    @Override
    public boolean consumeAether(Player player, double amount, AetherSignalType type) {
        if (player == null || amount < 0.0D) {
            return false;
        }
        AetherSignalType safeType = safe(type);
        double current = getAether(player, safeType);
        if (current < amount) {
            return false;
        }
        root(player).putFloat(key("current", safeType), (float) Math.max(0.0D, current - amount));
        root(player).putLong("last_cast_time", player.level().getGameTime());
        return true;
    }

    @Override
    public void setMaxAether(Player player, double amount, AetherSignalType type) {
        if (player == null) {
            return;
        }
        AetherSignalType safeType = safe(type);
        double max = Math.max(0.0D, amount);
        CompoundTag root = root(player);
        root.putFloat(key("max", safeType), (float) max);
        if (getAether(player, safeType) > max) {
            root.putFloat(key("current", safeType), (float) max);
        }
        root.putBoolean("unlocked_" + safeType.serializedName(), true);
    }

    @Override
    public AetherPlayerData playerData(Player player) {
        if (player == null) {
            return AetherPlayerData.empty();
        }
        CompoundTag root = root(player);
        EnumMap<AetherSignalType, Double> current = new EnumMap<>(AetherSignalType.class);
        EnumMap<AetherSignalType, Double> max = new EnumMap<>(AetherSignalType.class);
        java.util.LinkedHashSet<AetherSignalType> unlocked = new java.util.LinkedHashSet<>();
        for (AetherSignalType type : AetherSignalType.values()) {
            current.put(type, getAether(player, type));
            max.put(type, getMaxAether(player, type));
            if (root.getBooleanOr("unlocked_" + type.serializedName(), type == AetherSignalType.RAW_AETHER)) {
                unlocked.add(type);
            }
        }
        return new AetherPlayerData(current, max, root.getFloatOr("regen", 0.25F), unlocked,
                root.getFloatOr("contamination", 0.0F), root.getFloatOr("corruption", 0.0F),
                root.getLongOr("last_cast_time", 0L), java.util.List.of());
    }

    @Override
    public Optional<AetherStorage> getAetherStorage(ItemStack stack) {
        return stack != null && stack.getItem() instanceof AetherStorageTarget target
                ? Optional.ofNullable(target.aetherStorage())
                : Optional.empty();
    }

    @Override
    public Optional<AetherStorage> getAetherStorage(BlockEntity blockEntity) {
        return blockEntity instanceof AetherStorageTarget target
                ? Optional.ofNullable(target.aetherStorage())
                : Optional.empty();
    }

    @Override
    public double insertAether(Object target, double amount, AetherSignalType type) {
        if (!(target instanceof AetherStorageTarget storageTarget) || amount <= 0.0D) {
            return 0.0D;
        }
        AetherStorage storage = storageTarget.aetherStorage();
        if (storage == null || !storage.accepts(safe(type))) {
            return 0.0D;
        }
        double inserted = Math.min(amount, Math.max(0.0D, storage.maxStoredAmount() - storage.storedAmount()));
        return storageTarget.setAetherStorage(storage.withStoredAmount(storage.storedAmount() + inserted)) ? inserted : 0.0D;
    }

    @Override
    public double extractAether(Object target, double amount, AetherSignalType type) {
        if (!(target instanceof AetherStorageTarget storageTarget) || amount <= 0.0D) {
            return 0.0D;
        }
        AetherStorage storage = storageTarget.aetherStorage();
        if (storage == null || storage.outputType() != safe(type)) {
            return 0.0D;
        }
        double extracted = Math.min(amount, storage.storedAmount());
        return storageTarget.setAetherStorage(storage.withStoredAmount(storage.storedAmount() - extracted)) ? extracted : 0.0D;
    }

    @Override
    public AetherSignalType getAetherType(Object target) {
        return storage(target).map(AetherStorage::outputType).orElse(AetherSignalType.RAW_AETHER);
    }

    @Override
    public double getContamination(Object target) {
        if (target instanceof Player player) {
            return root(player).getFloatOr("contamination", 0.0F);
        }
        return storage(target).map(AetherStorage::contaminationLevel).orElse(0.0D);
    }

    @Override
    public void addContamination(Object target, double amount) {
        if (target instanceof Player player && amount > 0.0D) {
            CompoundTag root = root(player);
            root.putFloat("contamination", root.getFloatOr("contamination", 0.0F) + (float) amount);
            return;
        }
        if (!(target instanceof AetherStorageTarget storageTarget) || amount <= 0.0D) {
            return;
        }
        AetherStorage storage = storageTarget.aetherStorage();
        if (storage != null) {
            storageTarget.setAetherStorage(storage.withContamination(storage.contaminationLevel() + amount));
        }
    }

    @Override
    public void purifyAether(Object target) {
        if (target instanceof Player player) {
            root(player).putFloat("contamination", 0.0F);
            return;
        }
        if (target instanceof AetherStorageTarget storageTarget) {
            AetherStorage storage = storageTarget.aetherStorage();
            if (storage != null) {
                storageTarget.setAetherStorage(storage.withContamination(0.0D));
            }
        }
    }

    private static Optional<AetherStorage> storage(Object target) {
        return target instanceof AetherStorageTarget storageTarget
                ? Optional.ofNullable(storageTarget.aetherStorage())
                : Optional.empty();
    }

    private static CompoundTag root(Player player) {
        if (player == null) {
            return new CompoundTag();
        }
        CompoundTag root = player.getPersistentData().getCompoundOrEmpty(ROOT);
        player.getPersistentData().put(ROOT, root);
        return root;
    }

    private static String key(String prefix, AetherSignalType type) {
        return prefix + "_" + safe(type).serializedName();
    }

    private static AetherSignalType safe(AetherSignalType type) {
        return type == null ? AetherSignalType.RAW_AETHER : type;
    }
}
