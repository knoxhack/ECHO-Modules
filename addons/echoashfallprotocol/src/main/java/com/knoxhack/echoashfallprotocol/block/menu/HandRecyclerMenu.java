package com.knoxhack.echoashfallprotocol.block.menu;

import com.knoxhack.echoashfallprotocol.block.entity.HandRecyclerBlockEntity;
import com.knoxhack.echoashfallprotocol.energy.EnergyAccess;
import com.knoxhack.echoashfallprotocol.registry.ModMenuTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public class HandRecyclerMenu extends AbstractContainerMenu {
    private final HandRecyclerBlockEntity blockEntity;
    private final ContainerData data;

    // Client constructor
    public HandRecyclerMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory,
                (HandRecyclerBlockEntity) playerInventory.player.level().getBlockEntity(buf.readBlockPos()),
                new SimpleContainerData(6));
    }

    // Server constructor
    public HandRecyclerMenu(int containerId, Inventory playerInventory, HandRecyclerBlockEntity entity, ContainerData data) {
        super(ModMenuTypes.HAND_RECYCLER.get(), containerId);
        this.blockEntity = entity;
        this.data = data;

        addDataSlots(data);

        var inv = entity.getInventory();
        // Input slot
        this.addSlot(new Slot(inv, 0, 68, 88));
        // Output slot
        this.addSlot(new Slot(inv, 1, 244, 88));
        // Upgrade slot (speed upgrade)
        this.addSlot(new Slot(inv, 2, 68, 126));
        this.addSlot(new BatterySlot(inv, HandRecyclerBlockEntity.BATTERY_SLOT, 306, 126));

        // Player inventory
        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    public int getProgress() { return data.get(0); }
    public int getMaxProgress() { return data.get(1); }
    public boolean hasPower() { return data.get(2) != 0; }
    public boolean isJammed() { return data.get(3) != 0; }
    public boolean hasSpeedUpgrade() { return data.get(4) != 0; }
    public int getWearPercent() { return data.get(5); }
    public float getProgressPercent() {
        int max = getMaxProgress();
        return max == 0 ? 0 : (float) getProgress() / max;
    }
    
    public String getWearStatus() {
        int wear = getWearPercent();
        if (wear < 30) return "Good";
        if (wear < 60) return "Worn";
        if (wear < 80) return "Degraded";
        if (wear < 95) return "Critical";
        return "Failing";
    }
    
    public int getWearColor() {
        int wear = getWearPercent();
        if (wear < 30) return 0xFF42D67E; // Green
        if (wear < 60) return 0xFFF0C94B; // Yellow
        if (wear < 80) return 0xFFFFA94D; // Orange
        if (wear < 95) return 0xFFFF3333; // Red
        return 0xFFFF0000; // Dark Red
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack original = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot.hasItem()) {
            ItemStack current = slot.getItem();
            original = current.copy();
            if (slotIndex < 4) {
                // Machine slots -> player inventory
                if (!this.moveItemStackTo(current, 4, 40, true)) return ItemStack.EMPTY;
            } else {
                // Player inventory -> machine slots
                if (EnergyAccess.isEnergyItem(current)) {
                    if (!this.moveItemStackTo(current, 3, 4, false)) return ItemStack.EMPTY;
                } else if (blockEntity.isUpgradeItem(current)) {
                    // Try upgrade slot first
                    if (!this.moveItemStackTo(current, 2, 3, false)) {
                        // Then try input slot
                        if (!this.moveItemStackTo(current, 0, 1, false)) return ItemStack.EMPTY;
                    }
                } else {
                    // Try input slot
                    if (!this.moveItemStackTo(current, 0, 1, false)) return ItemStack.EMPTY;
                }
            }
            if (current.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
                player, blockEntity.getBlockState().getBlock());
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; i++) {
            for (int l = 0; l < 9; l++) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9,
                        MachineMenuLayout.PLAYER_INV_X + l * 18, MachineMenuLayout.PLAYER_INV_Y + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(playerInventory, i,
                    MachineMenuLayout.PLAYER_INV_X + i * 18, MachineMenuLayout.HOTBAR_Y));
        }
    }
}
