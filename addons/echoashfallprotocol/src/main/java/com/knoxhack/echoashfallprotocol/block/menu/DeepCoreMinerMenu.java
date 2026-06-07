package com.knoxhack.echoashfallprotocol.block.menu;

import com.knoxhack.echoashfallprotocol.block.entity.DeepCoreMinerBlockEntity;
import com.knoxhack.echoashfallprotocol.energy.EnergyAccess;
import com.knoxhack.echoashfallprotocol.registry.ModMenuTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public class DeepCoreMinerMenu extends AbstractContainerMenu {
    private final DeepCoreMinerBlockEntity blockEntity;
    private final ContainerData data;

    public DeepCoreMinerMenu(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(id, playerInventory,
                (DeepCoreMinerBlockEntity) playerInventory.player.level().getBlockEntity(buf.readBlockPos()),
                new SimpleContainerData(4));
    }

    public DeepCoreMinerMenu(int id, Inventory playerInventory, DeepCoreMinerBlockEntity entity, ContainerData data) {
        super(ModMenuTypes.DEEP_CORE_MINER.get(), id);
        this.blockEntity = entity;
        this.data = data;
        addDataSlots(data);

        var inv = entity.getInventory();
        this.addSlot(new Slot(inv, DeepCoreMinerBlockEntity.OUTPUT_SLOT, 244, 94));
        this.addSlot(new BatterySlot(inv, DeepCoreMinerBlockEntity.BATTERY_SLOT, 306, 94));

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    public int getProgress() { return data.get(0); }
    public int getMaxProgress() { return data.get(1); }
    public int getWearLevel() { return data.get(2); }
    public boolean isJammed() { return data.get(3) != 0; }
    public float getProgressPercent() {
        int max = getMaxProgress();
        return max == 0 ? 0 : (float) getProgress() / max;
    }

    public boolean isDeepEnough() {
        return blockEntity.getBlockPos().getY() <= DeepCoreMinerBlockEntity.MIN_Y_LEVEL;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack original = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack current = slot.getItem();
            original = current.copy();
            if (index < 2) {
                if (!this.moveItemStackTo(current, 2, 38, true)) return ItemStack.EMPTY;
            } else {
                if (!EnergyAccess.isEnergyItem(current) || !this.moveItemStackTo(current, 1, 2, false)) {
                    return ItemStack.EMPTY;
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
