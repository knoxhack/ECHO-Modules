package com.knoxhack.echoashfallprotocol.block.menu;

import com.knoxhack.echoashfallprotocol.block.entity.WaterPurifierBlockEntity;
import com.knoxhack.echoashfallprotocol.energy.EnergyAccess;
import com.knoxhack.echoashfallprotocol.registry.ModMenuTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public class WaterPurifierMenu extends AbstractContainerMenu {
    private final WaterPurifierBlockEntity blockEntity;
    private final ContainerData data;

    public WaterPurifierMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory,
                (WaterPurifierBlockEntity) playerInventory.player.level().getBlockEntity(buf.readBlockPos()),
                new SimpleContainerData(6));
    }

    public WaterPurifierMenu(int containerId, Inventory playerInventory, WaterPurifierBlockEntity entity, ContainerData data) {
        super(ModMenuTypes.WATER_PURIFIER.get(), containerId);
        this.blockEntity = entity;
        this.data = data;
        addDataSlots(data);

        // Water input slot
        this.addSlot(new Slot(entity.getInventory(), 0, 62, 88));
        // Filter slot
        this.addSlot(new Slot(entity.getInventory(), 1, 110, 126));
        // Output slot
        this.addSlot(new Slot(entity.getInventory(), 2, 244, 88));
        this.addSlot(new BatterySlot(entity.getInventory(), WaterPurifierBlockEntity.BATTERY_SLOT, 306, 112));

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    public int getProgress() { return data.get(0); }
    public int getMaxProgress() { return data.get(1); }
    public boolean hasPower() { return data.get(2) != 0; }
    public int getWearPercent() { return data.get(3); }
    public int getEnergy() { return data.get(4); }
    public int getMaxEnergy() { return data.get(5); }
    public boolean isJammed() { return blockEntity != null && new com.knoxhack.echoashfallprotocol.machine.MachineWearData(blockEntity.getLevel()).isJammed(blockEntity.getBlockPos()); }

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
        if (wear < 30) return 0xFF42D67E;
        if (wear < 60) return 0xFFF0C94B;
        if (wear < 80) return 0xFFFFA94D;
        if (wear < 95) return 0xFFFF3333;
        return 0xFFFF0000;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack original = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot.hasItem()) {
            ItemStack current = slot.getItem();
            original = current.copy();
            if (slotIndex < 4) {
                if (!this.moveItemStackTo(current, 4, 40, true)) return ItemStack.EMPTY;
            } else {
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
