package com.knoxhack.echoashfallprotocol.block;

import com.knoxhack.echoashfallprotocol.gameplay.MachineGameplayHelper;
import com.knoxhack.echoashfallprotocol.research.PerkEffectHandler;
import com.knoxhack.echoashfallprotocol.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

/**
 * Workshop Designation Block - Creates a 9x9x3 workshop area.
 * Machines within this area get 20% faster processing and 10% reduced power consumption.
 */
public class WorkshopBlock extends Block {
    
    // Workshop dimensions
    public static final int WORKSHOP_WIDTH = 9;
    public static final int WORKSHOP_HEIGHT = 3;
    public static final int WORKSHOP_DEPTH = 9;
    
    // Efficiency bonuses
    public static final double SPEED_BONUS = 0.20;  // 20% faster
    public static final double POWER_REDUCTION = 0.10;  // 10% less power
    
    public WorkshopBlock(Properties properties) {
        super(properties);
    }
    
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        
        showWorkshopStatus(player, pos);
        return InteractionResult.SUCCESS;
    }

    private void showWorkshopStatus(Player player, BlockPos workshopPos) {
        player.sendSystemMessage(Component.literal("WORKSHOP FIELD STATUS")
            .withStyle(ChatFormatting.BOLD, ChatFormatting.GOLD));
        
        // Count machines in workshop
        int machineCount = countMachinesInWorkshop(player.level(), workshopPos);
        
        player.sendSystemMessage(Component.literal("Machines in range: " + machineCount)
            .withStyle(ChatFormatting.YELLOW));
        player.sendSystemMessage(Component.literal("Throughput bonus: +" + (int)(SPEED_BONUS*100) + "%")
            .withStyle(ChatFormatting.GREEN));
        player.sendSystemMessage(Component.literal("Power draw reduction: -" + (int)(POWER_REDUCTION*100) + "%")
            .withStyle(ChatFormatting.GREEN));
        if (player instanceof ServerPlayer serverPlayer) {
            float perkBonus = PerkEffectHandler.getMachineSpeedMultiplier(serverPlayer);
            if (perkBonus > 1.0F) {
                player.sendSystemMessage(Component.literal(
                    "Operator training multiplier: x" + String.format(java.util.Locale.ROOT, "%.2f", perkBonus)
                ).withStyle(ChatFormatting.AQUA));
            }
            float pathBonus = MachineGameplayHelper.getPathSpeedMultiplier(serverPlayer);
            if (pathBonus > 1.0F) {
                player.sendSystemMessage(Component.literal(
                    "Nexus automation multiplier: x" + String.format(java.util.Locale.ROOT, "%.2f", pathBonus)
                ).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }

        showCoverageSummary(player);
    }
    
    private int countMachinesInWorkshop(Level level, BlockPos workshopPos) {
        int count = 0;
        int halfWidth = WORKSHOP_WIDTH / 2;
        int halfHeight = WORKSHOP_HEIGHT / 2;
        int halfDepth = WORKSHOP_DEPTH / 2;
        
        for (int x = -halfWidth; x <= halfWidth; x++) {
            for (int y = -halfHeight; y <= halfHeight; y++) {
                for (int z = -halfDepth; z <= halfDepth; z++) {
                    BlockPos checkPos = workshopPos.offset(x, y, z);
                    // Check if this is a machine block
                    if (isMachineBlock(level, checkPos)) {
                        count++;
                    }
                }
            }
        }
        
        return count;
    }
    
    private boolean isMachineBlock(Level level, BlockPos pos) {
        return MachineGameplayHelper.isMachineBlock(level.getBlockState(pos));
    }
    
    private void showCoverageSummary(Player player) {
        player.sendSystemMessage(coverageSummaryMessage());
    }

    public static Component coverageSummaryMessage() {
        return Component.literal("Coverage: machines inside the field receive passive efficiency bonuses")
            .withStyle(ChatFormatting.YELLOW);
    }
    
    /**
     * Check if a position is within a placed workshop block's service area.
     */
    public static boolean isInWorkshop(Level level, BlockPos pos) {
        return findWorkshopFor(level, pos) != null;
    }

    private static BlockPos findWorkshopFor(Level level, BlockPos pos) {
        int halfWidth = WORKSHOP_WIDTH / 2;
        int halfHeight = WORKSHOP_HEIGHT / 2;
        int halfDepth = WORKSHOP_DEPTH / 2;

        for (int x = -halfWidth; x <= halfWidth; x++) {
            for (int y = -halfHeight; y <= halfHeight; y++) {
                for (int z = -halfDepth; z <= halfDepth; z++) {
                    BlockPos checkPos = pos.offset(x, y, z);
                    if (level.isLoaded(checkPos) && level.getBlockState(checkPos).is(ModBlocks.WORKSHOP_BLOCK.get())) {
                        return checkPos;
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * Get workshop bonus for a machine at position
     */
    public static double getWorkshopSpeedBonus(Level level, BlockPos pos) {
        if (isInWorkshop(level, pos)) {
            return SPEED_BONUS;
        }
        return 0.0;
    }
    
    public static double getWorkshopPowerReduction(Level level, BlockPos pos) {
        if (isInWorkshop(level, pos)) {
            return POWER_REDUCTION;
        }
        return 0.0;
    }
    
}
