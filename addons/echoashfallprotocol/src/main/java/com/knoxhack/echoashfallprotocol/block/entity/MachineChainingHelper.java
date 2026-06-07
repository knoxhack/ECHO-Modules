package com.knoxhack.echoashfallprotocol.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Helper class for machine chaining - pushing outputs to adjacent machines.
 * Reduces code duplication across machine block entities.
 */
public class MachineChainingHelper {

    /**
     * Try to push an item stack from a specific slot to adjacent HopperHandler machines.
     *
     * @param level The world level
     * @param pos The position of the source machine
     * @param inventory The machine's inventory
     * @param outputSlot The slot containing items to push
     * @return True if any items were transferred
     */
    public static boolean tryPushOutput(Level level, BlockPos pos, MachineInventory inventory, int outputSlot) {
        ItemStack output = inventory.getStackInSlot(outputSlot);
        if (output.isEmpty()) return false;

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            BlockEntity neighbor = level.getBlockEntity(neighborPos);

            if (neighbor instanceof HopperHandler handler && !(neighbor instanceof ItemPipeBlockEntity)) {
                int[] inputSlots = handler.getInputSlots(dir.getOpposite());
                for (int slot : inputSlots) {
                    if (handler.canInsertItem(slot, output)) {
                        ItemStack existing = handler.getInventory().getStackInSlot(slot);
                        if (existing.isEmpty()) {
                            // Move entire stack
                            handler.getInventory().setStackInSlot(slot, output.copy());
                            inventory.setStackInSlot(outputSlot, ItemStack.EMPTY);
                            handler.getInventory().setChanged();
                            inventory.setChanged();
                            return true;
                        } else if (ItemStack.isSameItemSameComponents(existing, output)) {
                            // Merge stacks
                            int space = existing.getMaxStackSize() - existing.getCount();
                            int toMove = Math.min(space, output.getCount());
                            if (toMove > 0) {
                                existing.grow(toMove);
                                output.shrink(toMove);
                                handler.getInventory().setChanged();
                                inventory.setChanged();
                                if (output.isEmpty()) return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Try to push multiple output slots to adjacent machines.
     *
     * @param level The world level
     * @param pos The position of the source machine
     * @param inventory The machine's inventory
     * @param outputSlots Array of slot indices to push
     * @return Number of slots that had items transferred
     */
    public static int tryPushMultipleOutputs(Level level, BlockPos pos, MachineInventory inventory, int[] outputSlots) {
        int transferredCount = 0;
        for (int slot : outputSlots) {
            if (tryPushOutput(level, pos, inventory, slot)) {
                transferredCount++;
            }
        }
        return transferredCount;
    }
}
