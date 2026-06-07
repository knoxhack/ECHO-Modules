package com.knoxhack.echomultiblockcore.menu;

import com.knoxhack.echomultiblockcore.block.entity.MultiblockCrateBlockEntity;
import com.knoxhack.echomultiblockcore.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class MultiblockCrateMenu extends AbstractContainerMenu {
    public static final int GUI_WIDTH = 176;
    public static final int GUI_HEIGHT = 166;
    private static final int CONTAINER_ROWS = 2;
    private static final int SLOTS_PER_ROW = 9;

    private final Container container;

    public MultiblockCrateMenu(int containerId, Inventory inventory, Container container) {
        super(ModMenus.CRATE.get(), containerId);
        this.container = container;
        checkContainerSize(container, MultiblockCrateBlockEntity.SLOT_COUNT);
        container.startOpen(inventory.player);

        for (int row = 0; row < CONTAINER_ROWS; row++) {
            for (int col = 0; col < SLOTS_PER_ROW; col++) {
                addSlot(new Slot(container, col + row * SLOTS_PER_ROW, 8 + col * 18, 18 + row * 18));
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < SLOTS_PER_ROW; col++) {
                addSlot(new Slot(inventory, col + row * SLOTS_PER_ROW + SLOTS_PER_ROW, 8 + col * 18, 84 + row * 18));
            }
        }

        for (int col = 0; col < SLOTS_PER_ROW; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
    }

    public static MultiblockCrateMenu fromNetwork(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        if (inventory.player.level().getBlockEntity(pos) instanceof MultiblockCrateBlockEntity crate) {
            return new MultiblockCrateMenu(containerId, inventory, crate);
        }
        return new MultiblockCrateMenu(containerId, inventory, new net.minecraft.world.SimpleContainer(MultiblockCrateBlockEntity.SLOT_COUNT));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(slotIndex);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (slotIndex < MultiblockCrateBlockEntity.SLOT_COUNT) {
                if (!moveItemStackTo(stack, MultiblockCrateBlockEntity.SLOT_COUNT, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, 0, MultiblockCrateBlockEntity.SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
    }
}
