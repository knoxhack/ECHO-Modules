package com.knoxhack.echoarcanacore.api;

import java.util.Optional;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface IAetherService {
    default boolean available() {
        return true;
    }

    double getAether(Player player, AetherSignalType type);

    double getMaxAether(Player player, AetherSignalType type);

    double addAether(Player player, double amount, AetherSignalType type);

    boolean consumeAether(Player player, double amount, AetherSignalType type);

    default boolean canConsumeAether(Player player, double amount, AetherSignalType type) {
        return getAether(player, type) >= Math.max(0.0D, amount);
    }

    void setMaxAether(Player player, double amount, AetherSignalType type);

    AetherPlayerData playerData(Player player);

    Optional<AetherStorage> getAetherStorage(ItemStack stack);

    Optional<AetherStorage> getAetherStorage(BlockEntity blockEntity);

    double insertAether(Object target, double amount, AetherSignalType type);

    double extractAether(Object target, double amount, AetherSignalType type);

    AetherSignalType getAetherType(Object target);

    double getContamination(Object target);

    void addContamination(Object target, double amount);

    void purifyAether(Object target);
}
