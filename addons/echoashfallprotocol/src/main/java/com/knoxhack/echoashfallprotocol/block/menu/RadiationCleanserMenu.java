package com.knoxhack.echoashfallprotocol.block.menu;

import com.knoxhack.echoashfallprotocol.block.entity.RadiationCleanserBlockEntity;
import com.knoxhack.echoashfallprotocol.energy.EnergyAccess;
import com.knoxhack.echoashfallprotocol.registry.ModMenuTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public class RadiationCleanserMenu extends AbstractContainerMenu {
    private final RadiationCleanserBlockEntity blockEntity;
    private final ContainerData data;

    public RadiationCleanserMenu(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(id, playerInventory,
                (RadiationCleanserBlockEntity) playerInventory.player.level().getBlockEntity(buf.readBlockPos()),
                new SimpleContainerData(5));
    }

    public RadiationCleanserMenu(int id, Inventory playerInventory, RadiationCleanserBlockEntity entity, ContainerData data) {
        super(ModMenuTypes.RADIATION_CLEANSER.get(), id);
        this.blockEntity = entity;
        this.data = data;
        addDataSlots(data);

        var inv = entity.getInventory();
        // Input slot
        this.addSlot(new Slot(inv, RadiationCleanserBlockEntity.INPUT_SLOT, 62, 94));
        // Filter slot
        this.addSlot(new Slot(inv, RadiationCleanserBlockEntity.FILTER_SLOT, 142, 94));
        // Output slot
        this.addSlot(new Slot(inv, RadiationCleanserBlockEntity.OUTPUT_SLOT, 244, 94) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false; // Output only
            }
        });
        this.addSlot(new BatterySlot(inv, RadiationCleanserBlockEntity.BATTERY_SLOT, 306, 94));

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    public int getProgress() { return data.get(0); }
    public int getMaxProgress() { return data.get(1); }
    public int getWearLevel() { return data.get(2); }
    public int getEnergy() { return data.get(3); }
    public int getMaxEnergy() { return data.get(4); }
    public float getProgressPercent() {
        int max = getMaxProgress();
        return max == 0 ? 0 : (float) getProgress() / max;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack original = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack current = slot.getItem();
            original = current.copy();
            if (index < 4) {
                // Move from machine to inventory
                if (!this.moveItemStackTo(current, 4, 40, true)) return ItemStack.EMPTY;
            } else {
                // Move from inventory to machine
                if (EnergyAccess.isEnergyItem(current) && this.moveItemStackTo(current, 3, 4, false)) {
                    if (current.isEmpty()) slot.set(ItemStack.EMPTY);
                    else slot.setChanged();
                    return original;
                }
                if (!this.moveItemStackTo(current, 0, 2, false)) return ItemStack.EMPTY;
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
