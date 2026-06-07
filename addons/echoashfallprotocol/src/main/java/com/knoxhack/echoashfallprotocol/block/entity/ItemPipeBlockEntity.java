package com.knoxhack.echoashfallprotocol.block.entity;

import com.knoxhack.echoashfallprotocol.block.ItemPipeBlock;
import com.knoxhack.echoashfallprotocol.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;

/**
 * Item Pipe Block Entity - transfers items between machines.
 * Pulls from the facing direction, distributes to other connected pipes/machines.
 */
public class ItemPipeBlockEntity extends BlockEntity {
    private static final int TRANSFER_COOLDOWN = 8; // Ticks between transfers
    private static final int MAX_ROUTE_DEPTH = 16;

    private int cooldown = 0;
    
    public ItemPipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ITEM_PIPE.get(), pos, state);
    }
    
    public static void serverTick(Level level, BlockPos pos, BlockState state, ItemPipeBlockEntity entity) {
        if (level.isClientSide()) return;
        
        if (entity.cooldown-- > 0) return;
        
        Direction facing = state.getValue(ItemPipeBlock.FACING);
        BlockPos sourcePos = pos.relative(facing);
        BlockEntity source = level.getBlockEntity(sourcePos);
        
        // Try to extract from source
        if (source instanceof HopperHandler sourceHandler) {
            int[] outputSlots = sourceHandler.getOutputSlots(facing.getOpposite());
            
            for (int slot : outputSlots) {
                if (!sourceHandler.canExtractItem(slot)) continue;
                
                ItemStack extracted = sourceHandler.getInventory().getStackInSlot(slot);
                if (extracted.isEmpty()) continue;
                
                // Try to route to a destination
                ItemStack remaining = entity.routeItem(level, pos, extracted.copy(), facing, new HashSet<>(), 0);

                if (remaining.getCount() < extracted.getCount()) {
                    // Successfully moved some items
                    int transferred = extracted.getCount() - remaining.getCount();
                    sourceHandler.getInventory().getItem(slot).shrink(transferred);
                    entity.cooldown = TRANSFER_COOLDOWN;
                    entity.setChanged();
                    source.setChanged();
                    return; // Only transfer one stack per tick
                }
            }
        }
    }
    
    /**
     * Route an item to the best available destination.
     * @return Remaining items that couldn't be transferred
     */
    private ItemStack routeItem(Level level, BlockPos pos, ItemStack stack, Direction fromFace, Set<BlockPos> visited, int depth) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        if (depth > MAX_ROUTE_DEPTH || !visited.add(pos.immutable())) {
            return stack;
        }
        
        // Priority 1: Direct machine input (not pipes)
        for (Direction dir : Direction.values()) {
            if (dir == fromFace) continue; // Don't route back to source
            
            BlockPos targetPos = pos.relative(dir);
            BlockEntity target = level.getBlockEntity(targetPos);
            
            if (target instanceof HopperHandler handler && !(target instanceof ItemPipeBlockEntity)) {
                ItemStack result = tryInsertIntoHandler(handler, dir.getOpposite(), stack);
                if (result.getCount() < stack.getCount()) {
                    return result; // Successfully inserted some
                }
            }
        }
        
        // Priority 2: Other pipes (for network routing)
        for (Direction dir : Direction.values()) {
            if (dir == fromFace) continue;

            BlockPos targetPos = pos.relative(dir);
            BlockEntity target = level.getBlockEntity(targetPos);
            if (target instanceof ItemPipeBlockEntity pipe) {
                ItemStack result = pipe.routeItem(level, targetPos, stack, dir.getOpposite(), new HashSet<>(visited), depth + 1);
                if (result.getCount() < stack.getCount()) {
                    return result;
                }
            }
        }
        
        return stack; // Couldn't route anywhere
    }
    
    /**
     * Try to insert items into a HopperHandler.
     * @return Remaining items that couldn't be inserted
     */
    private ItemStack tryInsertIntoHandler(HopperHandler handler, Direction fromDir, ItemStack stack) {
        int[] inputSlots = handler.getInputSlots(fromDir);
        ItemStack remaining = stack.copy();
        
        for (int slot : inputSlots) {
            if (!handler.canInsertItem(slot, remaining)) continue;
            
            ItemStack existing = handler.getInventory().getStackInSlot(slot);
            
            if (existing.isEmpty()) {
                int toInsert = Math.min(remaining.getCount(), remaining.getMaxStackSize());
                ItemStack inserted = remaining.copy();
                inserted.setCount(toInsert);
                handler.getInventory().setStackInSlot(slot, inserted);
                remaining.shrink(toInsert);
                handler.getInventory().setChanged();
                if (remaining.isEmpty()) return ItemStack.EMPTY;
            } else if (ItemStack.isSameItemSameComponents(existing, remaining)) {
                // Compatible stack - merge
                int maxStack = existing.getMaxStackSize();
                int space = maxStack - existing.getCount();
                int toAdd = Math.min(space, remaining.getCount());
                
                if (toAdd > 0) {
                    existing.grow(toAdd);
                    remaining.shrink(toAdd);
                    handler.getInventory().setChanged();
                    
                    if (remaining.isEmpty()) return ItemStack.EMPTY;
                }
            }
        }
        
        return remaining;
    }
}
