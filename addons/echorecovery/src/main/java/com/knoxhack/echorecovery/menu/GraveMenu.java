package com.knoxhack.echorecovery.menu;

import com.knoxhack.echorecovery.block.entity.GraveBlockEntity;
import com.knoxhack.echorecovery.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class GraveMenu extends AbstractContainerMenu {
    private static final int GRAVE_SLOT_COUNT = 54;
    private static final int INV_SLOT_COUNT = 27;
    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int TOTAL_SLOT_COUNT = GRAVE_SLOT_COUNT + INV_SLOT_COUNT + HOTBAR_SLOT_COUNT;

    private final GraveBlockEntity grave;
    private final BlockPos pos;

    public GraveMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory, resolveGrave(playerInventory.player, buf));
    }

    public GraveMenu(int containerId, Inventory playerInventory, GraveBlockEntity grave) {
        super(ModMenus.GRAVE.get(), containerId);
        this.grave = grave;
        this.pos = grave != null ? grave.getBlockPos() : BlockPos.ZERO;

        // Grave inventory slots (54 slots, 6 rows x 9 cols)
        if (grave != null) {
            for (int row = 0; row < 6; row++) {
                for (int col = 0; col < 9; col++) {
                    this.addSlot(new Slot(grave, col + row * 9, 8 + col * 18, 18 + row * 18));
                }
            }
        }

        // Player inventory (27 slots, 3 rows x 9 cols)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 140 + row * 18));
            }
        }

        // Player hotbar (9 slots)
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 198));
        }
    }

    private static GraveBlockEntity resolveGrave(Player player, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        if (player.level().getBlockEntity(pos) instanceof GraveBlockEntity entity) {
            return entity;
        }
        return null;
    }

    public GraveBlockEntity getGrave() {
        return grave;
    }

    public BlockPos getPos() {
        return pos;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack originalStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            originalStack = stack.copy();
            if (index < GRAVE_SLOT_COUNT) {
                // Move from grave to player inventory
                if (!this.moveItemStackTo(stack, GRAVE_SLOT_COUNT, TOTAL_SLOT_COUNT, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Move from player to grave
                if (!this.moveItemStackTo(stack, 0, GRAVE_SLOT_COUNT, false)) {
                    return ItemStack.EMPTY;
                }
            }
            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return originalStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return grave != null && grave.stillValid(player);
    }
}
