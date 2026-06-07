package com.knoxhack.echoashfallprotocol.block.menu;

import com.knoxhack.echoashfallprotocol.block.entity.ThermalArrayBlockEntity;
import com.knoxhack.echoashfallprotocol.energy.EnergyAccess;
import com.knoxhack.echoashfallprotocol.registry.ModMenuTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Menu for Thermal Array generator - manages 3 fuel slots.
 */
public class ThermalArrayMenu extends AbstractContainerMenu {
    private final ThermalArrayBlockEntity blockEntity;
    private final ContainerData data;

    public ThermalArrayMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory,
                (ThermalArrayBlockEntity) playerInventory.player.level().getBlockEntity(buf.readBlockPos()),
                new SimpleContainerData(5));
    }

    public ThermalArrayMenu(int containerId, Inventory playerInventory, ThermalArrayBlockEntity entity, ContainerData data) {
        super(ModMenuTypes.THERMAL_ARRAY.get(), containerId);
        this.blockEntity = entity;
        this.data = data;
        addDataSlots(data);

        // 3 Fuel slots in a row
        this.addSlot(new Slot(entity.getInventory(), 0, 58, 86));
        this.addSlot(new Slot(entity.getInventory(), 1, 84, 86));
        this.addSlot(new Slot(entity.getInventory(), 2, 110, 86));
        this.addSlot(new BatterySlot(entity.getInventory(), ThermalArrayBlockEntity.BATTERY_SLOT, 306, 94));

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    public int getEnergy() { return data.get(0); }
    public int getMaxEnergy() { return data.get(1); }
    public int getBurnTimeRemaining() { return data.get(2); }
    public int getMaxBurnTime() { return data.get(3); }
    public boolean isFailed() { return data.get(4) != 0; }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack original = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot.hasItem()) {
            ItemStack current = slot.getItem();
            original = current.copy();
            
            if (slotIndex < 4) {
                // Move from fuel slots to player inventory
                if (!this.moveItemStackTo(current, 4, 40, true)) return ItemStack.EMPTY;
            } else {
                // Move from player inventory to fuel slots (only if fuel)
                if (EnergyAccess.isEnergyItem(current)) {
                    if (!this.moveItemStackTo(current, 3, 4, false)) return ItemStack.EMPTY;
                } else if (isFuel(current)) {
                    if (!this.moveItemStackTo(current, 0, 3, false)) return ItemStack.EMPTY;
                } else {
                    return ItemStack.EMPTY;
                }
            }
            
            if (current.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return original;
    }

    private boolean isFuel(ItemStack stack) {
        return stack.is(Items.COAL) || stack.is(Items.CHARCOAL) ||
               stack.is(Items.OAK_PLANKS) || stack.is(Items.SPRUCE_PLANKS) ||
               stack.is(Items.BIRCH_PLANKS) || stack.is(Items.DARK_OAK_PLANKS) ||
               stack.is(Items.STICK);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
                player, blockEntity.getBlockState().getBlock());
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; i++)
            for (int l = 0; l < 9; l++)
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9,
                        MachineMenuLayout.PLAYER_INV_X + l * 18, MachineMenuLayout.PLAYER_INV_Y + i * 18));
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; i++)
            this.addSlot(new Slot(playerInventory, i,
                    MachineMenuLayout.PLAYER_INV_X + i * 18, MachineMenuLayout.HOTBAR_Y));
    }
}
