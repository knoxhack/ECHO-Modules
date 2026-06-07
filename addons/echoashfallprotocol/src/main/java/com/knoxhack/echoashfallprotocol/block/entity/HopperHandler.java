package com.knoxhack.echoashfallprotocol.block.entity;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

/**
 * Interface for machines that support hopper automation.
 * Defines which slots accept input from which sides and which slots output.
 */
public interface HopperHandler {
    
    /**
     * Get input slots for a specific side (for hoppers to insert items).
     * @param side The direction the hopper is inserting from
     * @return Array of slot indices that accept input from this side
     */
    int[] getInputSlots(Direction side);
    
    /**
     * Get output slots for a specific side (for hoppers to extract items).
     * @param side The direction the hopper is extracting from  
     * @return Array of slot indices that can be extracted from this side
     */
    int[] getOutputSlots(Direction side);
    
    /**
     * Check if an item can be inserted into a specific slot.
     * @param slot The slot index
     * @param stack The item stack to insert
     * @return true if the item can be inserted
     */
    boolean canInsertItem(int slot, ItemStack stack);
    
    /**
     * Check if an item can be extracted from a specific slot.
     * @param slot The slot index
     * @return true if the item can be extracted
     */
    boolean canExtractItem(int slot);
    
    /**
     * Get the machine's inventory for slot access.
     * @return The MachineInventory instance
     */
    MachineInventory getInventory();
}
