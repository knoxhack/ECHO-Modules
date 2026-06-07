package com.knoxhack.echoashfallprotocol.gameplay;

import com.knoxhack.echoashfallprotocol.block.WorkshopBlock;
import com.knoxhack.echoashfallprotocol.endgame.PostNexusData;
import com.knoxhack.echoashfallprotocol.registry.ModBlocks;
import com.knoxhack.echoashfallprotocol.research.PerkEffectHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared gameplay math for machine processing.
 * Centralizes perk and workshop bonuses so machines stay in sync.
 */
public final class MachineGameplayHelper {

    private static final double OPERATOR_SEARCH_RADIUS = 16.0D;
    private static final Map<String, Long> NEXUS_SURGES = new ConcurrentHashMap<>();

    private MachineGameplayHelper() {
    }

    public static int getAdjustedProcessTime(Level level, BlockPos pos, int baseTicks) {
        float speedMultiplier = getMachineSpeedMultiplier(level, pos);
        return Math.max(1, Math.round(baseTicks / speedMultiplier));
    }

    public static int getAdjustedPowerCost(Level level, BlockPos pos, int baseCost) {
        double reduction = WorkshopBlock.getWorkshopPowerReduction(level, pos);
        ServerPlayer operator = getNearestOperator(level, pos);
        if (operator != null) {
            reduction += getPathPowerReduction(operator);
        }
        reduction = Math.min(0.45D, reduction);
        return Math.max(1, (int) Math.ceil(baseCost * (1.0D - reduction)));
    }

    public static float getMachineSpeedMultiplier(Level level, BlockPos pos) {
        float multiplier = 1.0F + (float) WorkshopBlock.getWorkshopSpeedBonus(level, pos);

        if (!level.isClientSide()) {
            ServerPlayer operator = getNearestOperator(level, pos);
            if (operator != null) {
                multiplier *= PerkEffectHandler.getMachineSpeedMultiplier(operator);
                multiplier *= getPathSpeedMultiplier(operator);
            }
        }

        if (isNexusSurged(level, pos)) {
            multiplier *= 1.35F;
        }

        return Math.max(0.1F, DifficultyProfile.scaleMachineSpeed(level, multiplier));
    }

    public static void addNexusSurge(Level level, BlockPos pos, long durationTicks) {
        if (level == null || level.isClientSide()) return;
        NEXUS_SURGES.put(surgeKey(level, pos), level.getGameTime() + durationTicks);
    }

    public static boolean isNexusSurged(Level level, BlockPos pos) {
        if (level == null || level.isClientSide()) return false;
        String key = surgeKey(level, pos);
        Long expires = NEXUS_SURGES.get(key);
        if (expires == null) return false;
        if (level.getGameTime() > expires) {
            NEXUS_SURGES.remove(key);
            return false;
        }
        return true;
    }

    private static String surgeKey(Level level, BlockPos pos) {
        return level.dimension().toString() + "|" + pos.asLong();
    }

    public static ServerPlayer getNearestOperator(Level level, BlockPos pos) {
        if (level.isClientSide()) {
            return null;
        }

        return level.getNearestPlayer(
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                OPERATOR_SEARCH_RADIUS,
                player -> player instanceof ServerPlayer
        ) instanceof ServerPlayer serverPlayer ? serverPlayer : null;
    }

    public static float getPathSpeedMultiplier(Player player) {
        PostNexusData.NexusPath path = PostNexusData.get(player).getSelectedPath();
        return switch (path) {
            case CONTROL -> 1.20F;
            case RESTORE -> 1.05F;
            default -> 1.0F;
        };
    }

    public static double getPathPowerReduction(Player player) {
        PostNexusData.NexusPath path = PostNexusData.get(player).getSelectedPath();
        return switch (path) {
            case RESTORE -> 0.15D;
            case CONTROL -> 0.05D;
            default -> 0.0D;
        };
    }

    public static boolean isMachineBlock(BlockState state) {
        return state.is(ModBlocks.HAND_RECYCLER.get()) ||
                state.is(ModBlocks.WATER_PURIFIER.get()) ||
                state.is(ModBlocks.THERMAL_BURNER.get()) ||
                state.is(ModBlocks.MICRO_GENERATOR.get()) ||
                state.is(ModBlocks.ORE_GRINDER.get()) ||
                state.is(ModBlocks.ISOTOPE_REFINER.get()) ||
                state.is(ModBlocks.CRYSTALLINE_SYNTHESIZER.get()) ||
                state.is(ModBlocks.DEEP_CORE_MINER.get()) ||
                state.is(ModBlocks.FILTER_WORKBENCH.get()) ||
                state.is(ModBlocks.SCRAP_PRESS.get()) ||
                state.is(ModBlocks.FIELD_MED_BAY.get()) ||
                state.is(ModBlocks.ATMOSPHERIC_SCRUBBER.get()) ||
                state.is(ModBlocks.CONTAMINANT_CONDENSER.get()) ||
                state.is(ModBlocks.AUTOFEED_HOPPER.get()) ||
                state.is(ModBlocks.THERMAL_ARRAY.get()) ||
                state.is(ModBlocks.RADIATION_CLEANSER.get()) ||
                state.is(ModBlocks.RESEARCH_LAB.get()) ||
                state.is(ModBlocks.SIGNAL_SCANNER.get()) ||
                state.is(ModBlocks.BATTERY_BANK.get());
    }
}
