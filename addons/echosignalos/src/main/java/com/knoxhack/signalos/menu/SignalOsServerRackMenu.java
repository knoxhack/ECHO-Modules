package com.knoxhack.signalos.menu;

import com.knoxhack.signalos.block.entity.SignalOsServerRackBlockEntity;
import com.knoxhack.signalos.item.SignalOsDataDriveItem;
import com.knoxhack.signalos.registry.ModBlocks;
import com.knoxhack.signalos.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class SignalOsServerRackMenu extends AbstractContainerMenu {
    public static final int GUI_WIDTH = 352;
    public static final int GUI_HEIGHT = 286;
    public static final int DRIVE_SLOT_COUNT = SignalOsServerRackBlockEntity.DRIVE_SLOTS;
    private static final int PLAYER_INV_START = DRIVE_SLOT_COUNT;
    private static final int PLAYER_INV_END = PLAYER_INV_START + 27;
    private static final int HOTBAR_END = PLAYER_INV_END + 9;

    private final Container drives;
    private final SignalOsServerRackBlockEntity rack;
    private final BlockPos blockPos;

    public SignalOsServerRackMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, rackAt(playerInventory, buffer.readBlockPos()));
    }

    public SignalOsServerRackMenu(int containerId, Inventory playerInventory, SignalOsServerRackBlockEntity rack) {
        this(containerId, playerInventory, rack == null ? new SimpleContainer(DRIVE_SLOT_COUNT) : rack.drives(), rack,
                rack == null ? BlockPos.ZERO : rack.getBlockPos());
    }

    private SignalOsServerRackMenu(int containerId, Inventory playerInventory, Container drives,
            SignalOsServerRackBlockEntity rack, BlockPos blockPos) {
        super(ModMenus.SERVER_RACK.get(), containerId);
        checkContainerSize(drives, DRIVE_SLOT_COUNT);
        this.drives = drives;
        this.rack = rack;
        this.blockPos = blockPos == null ? BlockPos.ZERO : blockPos;

        for (int i = 0; i < DRIVE_SLOT_COUNT; i++) {
            this.addSlot(new DriveSlot(drives, i, 22, 54 + i * 22));
        }
        this.addStandardInventorySlots(playerInventory, 97, 174);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack copy = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            copy = stack.copy();
            if (slotIndex < DRIVE_SLOT_COUNT) {
                if (!moveItemStackTo(stack, PLAYER_INV_START, HOTBAR_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!stack.is(ModBlocks.DATA_DRIVE.get()) || !SignalOsDataDriveItem.ensureV2Data(stack)
                    || !moveItemStackTo(stack, 0, DRIVE_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        if (rack == null || rack.getLevel() == null) {
            return false;
        }
        return stillValid(ContainerLevelAccess.create(rack.getLevel(), rack.getBlockPos()), player,
                ModBlocks.SERVER_RACK.get());
    }

    public SignalOsServerRackBlockEntity rack() {
        return rack;
    }

    public BlockPos blockPos() {
        return blockPos;
    }

    public ItemStack driveStack(int slot) {
        return slot < 0 || slot >= drives.getContainerSize() ? ItemStack.EMPTY : drives.getItem(slot);
    }

    private static SignalOsServerRackBlockEntity rackAt(Inventory inventory, BlockPos pos) {
        if (inventory == null || inventory.player == null || inventory.player.level() == null) {
            return null;
        }
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        return blockEntity instanceof SignalOsServerRackBlockEntity rack ? rack : null;
    }

    private static final class DriveSlot extends Slot {
        private DriveSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return SignalOsDataDriveItem.canInstall(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}
