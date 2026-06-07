package com.knoxhack.echorelictech.block;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.mission.MissionObjectiveType;
import com.knoxhack.echorelictech.EchoRelicTech;
import com.knoxhack.echorelictech.block.entity.NullBatteryDockBlockEntity;
import com.knoxhack.echorelictech.config.RelicTechConfig;
import com.knoxhack.echorelictech.registry.ModDataComponents;
import com.knoxhack.echorelictech.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class NullBatteryDockBlock extends Block implements EntityBlock {
    public NullBatteryDockBlock(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof NullBatteryDockBlockEntity be) {
            if (stack.is(ModItems.NULL_BATTERY.get())) {
                if (be.insertBattery(stack)) {
                    player.sendSystemMessage(Component.translatable("block.echorelictech.null_battery_dock.inserted"));
                } else {
                    player.sendSystemMessage(Component.translatable("block.echorelictech.null_battery_dock.full"));
                }
                return InteractionResult.SUCCESS;
            }
            if (stack.is(ModItems.NULL_CELL.get())) {
                if (be.insertCell(stack)) {
                    player.sendSystemMessage(Component.translatable("block.echorelictech.null_battery_dock.cell_inserted"));
                } else {
                    player.sendSystemMessage(Component.translatable("block.echorelictech.null_battery_dock.cell_full"));
                }
                return InteractionResult.SUCCESS;
            }
            // If empty hand, try to remove battery
            if (stack.isEmpty()) {
                ItemStack battery = be.removeBattery();
                if (!battery.isEmpty()) {
                    if (!player.getInventory().add(battery)) {
                        player.drop(battery, false);
                    }
                    player.sendSystemMessage(Component.translatable("block.echorelictech.null_battery_dock.removed"));
                    return InteractionResult.SUCCESS;
                }
                ItemStack cell = be.removeCell();
                if (!cell.isEmpty()) {
                    if (!player.getInventory().add(cell)) {
                        player.drop(cell, false);
                    }
                    player.sendSystemMessage(Component.translatable("block.echorelictech.null_battery_dock.cell_removed"));
                    return InteractionResult.SUCCESS;
                }
            }
        }
        player.sendSystemMessage(Component.translatable("block.echorelictech.null_battery_dock.hint"));
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof NullBatteryDockBlockEntity be) {
            ItemStack battery = be.getBattery();
            if (!battery.isEmpty()) {
                int charge = battery.getOrDefault(ModDataComponents.NULL_CHARGE.get(), 0);
                int max = RelicTechConfig.NULL_BATTERY_MAX_CHARGE.get();
                player.sendSystemMessage(Component.translatable("block.echorelictech.null_battery_dock.status", charge, max));
                if (charge > 0 && player instanceof ServerPlayer serverPlayer) {
                    EchoCoreServices.recordMissionObjective(
                            serverPlayer,
                            MissionObjectiveType.CUSTOM,
                            Identifier.fromNamespaceAndPath(EchoRelicTech.MODID, "null_battery_charged"),
                            1,
                            java.util.Map.of("source", EchoRelicTech.MODID));
                }
            } else {
                player.sendSystemMessage(Component.translatable("block.echorelictech.null_battery_dock.empty"));
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return (lvl, pos, st, be) -> {
            if (be instanceof NullBatteryDockBlockEntity dock) {
                NullBatteryDockBlockEntity.tick(lvl, pos, st, dock);
            }
        };
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NullBatteryDockBlockEntity(pos, state);
    }
}
