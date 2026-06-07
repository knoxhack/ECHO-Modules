package com.knoxhack.echoashfallprotocol.block.menu;

import com.knoxhack.echoashfallprotocol.block.entity.FilterWorkbenchBlockEntity;
import com.knoxhack.echoashfallprotocol.energy.EnergyAccess;
import com.knoxhack.echoashfallprotocol.registry.ModMenuTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class FilterWorkbenchMenu extends AbstractContainerMenu {
    private final FilterWorkbenchBlockEntity blockEntity;
    private final ContainerData data;

    public FilterWorkbenchMenu(int id, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(id, playerInventory,
                (FilterWorkbenchBlockEntity) playerInventory.player.level().getBlockEntity(buf.readBlockPos()),
                new SimpleContainerData(5));
    }

    public FilterWorkbenchMenu(int id, Inventory playerInventory, FilterWorkbenchBlockEntity entity, ContainerData data) {
        super(ModMenuTypes.FILTER_WORKBENCH.get(), id);
        this.blockEntity = entity;
        this.data = data;
        addDataSlots(data);

        var inv = entity.getInventory();
        this.addSlot(new Slot(inv, 0, 58, 94));
        this.addSlot(new Slot(inv, 1, 84, 94));
        this.addSlot(new Slot(inv, 2, 110, 94));
        this.addSlot(new Slot(inv, 3, 244, 94) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        this.addSlot(new BatterySlot(inv, FilterWorkbenchBlockEntity.BATTERY_SLOT, 306, 94));

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    public int getProgress() { return data.get(0); }
    public int getMaxProgress() { return data.get(1); }
    public boolean hasPower() { return data.get(2) != 0; }
    public boolean isJammed() { return data.get(3) != 0; }
    public int getWearPercent() { return data.get(4); }
    public float getProgressPercent() {
        int max = getMaxProgress();
        return max == 0 ? 0.0f : (float) getProgress() / max;
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
        if (wear < 30) return 0xFF42D67E;
        if (wear < 60) return 0xFFF0C94B;
        if (wear < 80) return 0xFFFFA94D;
        if (wear < 95) return 0xFFE25959;
        return 0xFFFF3333;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack original = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack current = slot.getItem();
            original = current.copy();
            if (index < 5) {
                if (!this.moveItemStackTo(current, 5, 41, true)) return ItemStack.EMPTY;
            } else {
                if (EnergyAccess.isEnergyItem(current) && this.moveItemStackTo(current, 4, 5, false)) {
                    if (current.isEmpty()) slot.set(ItemStack.EMPTY);
                    else slot.setChanged();
                    return original;
                }
                if (!this.moveItemStackTo(current, 0, 3, false)) return ItemStack.EMPTY;
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
