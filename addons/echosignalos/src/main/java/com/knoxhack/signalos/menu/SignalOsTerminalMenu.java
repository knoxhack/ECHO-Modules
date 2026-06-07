package com.knoxhack.signalos.menu;

import com.knoxhack.signalos.block.entity.SignalOsTerminalBlockEntity;
import com.knoxhack.signalos.item.SignalOsDataDriveItem;
import com.knoxhack.signalos.registry.ModBlocks;
import com.knoxhack.signalos.registry.ModMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class SignalOsTerminalMenu extends AbstractContainerMenu {
    public static final int DRIVE_SLOT_COUNT = SignalOsTerminalBlockEntity.DRIVE_SLOTS;
    private static final int PLAYER_INV_START = DRIVE_SLOT_COUNT;
    private static final int PLAYER_INV_END = PLAYER_INV_START + 27;
    private static final int HOTBAR_END = PLAYER_INV_END + 9;

    private final ContainerLevelAccess access;
    private final boolean remoteAccess;
    private final Container bootDrive;
    private final SignalOsTerminalBlockEntity terminal;

    public SignalOsTerminalMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory);
    }

    public SignalOsTerminalMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(DRIVE_SLOT_COUNT), null,
                ContainerLevelAccess.NULL, true);
    }

    public SignalOsTerminalMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        this(containerId, playerInventory, new SimpleContainer(DRIVE_SLOT_COUNT), null, access, false);
    }

    public SignalOsTerminalMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access,
            SignalOsTerminalBlockEntity terminal) {
        this(containerId, playerInventory, terminal == null ? new SimpleContainer(DRIVE_SLOT_COUNT) : terminal.bootDrive(),
                terminal, access, false);
    }

    public static SignalOsTerminalMenu remote(int containerId, Inventory playerInventory,
            SignalOsTerminalBlockEntity terminal) {
        return new SignalOsTerminalMenu(containerId, playerInventory,
                terminal == null ? new SimpleContainer(DRIVE_SLOT_COUNT) : terminal.bootDrive(), terminal,
                ContainerLevelAccess.NULL, true);
    }

    private SignalOsTerminalMenu(int containerId, Inventory playerInventory, Container bootDrive,
            SignalOsTerminalBlockEntity terminal, ContainerLevelAccess access, boolean remoteAccess) {
        super(ModMenus.TERMINAL.get(), containerId);
        checkContainerSize(bootDrive, DRIVE_SLOT_COUNT);
        this.access = access == null ? ContainerLevelAccess.NULL : access;
        this.remoteAccess = remoteAccess;
        this.bootDrive = bootDrive;
        this.terminal = terminal;

        this.addSlot(new DriveSlot(bootDrive, 0, 12, 154));
        this.addStandardInventorySlots(playerInventory, 34, 154);
    }

    @Override
    public boolean stillValid(Player player) {
        return remoteAccess
                || stillValid(access, player, ModBlocks.TERMINAL.get())
                || stillValid(access, player, ModBlocks.WORKSTATION.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack copy = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            copy = stack.copy();
            if (index < DRIVE_SLOT_COUNT) {
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

    public SignalOsTerminalBlockEntity terminal() {
        return terminal;
    }

    public ItemStack driveStack() {
        return bootDrive.getItem(0);
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
