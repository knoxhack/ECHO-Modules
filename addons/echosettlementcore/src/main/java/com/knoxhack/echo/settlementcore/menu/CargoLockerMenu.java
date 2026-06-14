package com.knoxhack.echo.settlementcore.menu;

import com.knoxhack.echo.settlementcore.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class CargoLockerMenu extends AbstractContainerMenu {
    public CargoLockerMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory);
    }

    public CargoLockerMenu(int containerId, Inventory playerInventory) {
        super(ModMenus.CARGO_LOCKER.get(), containerId);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    public static CargoLockerMenu fromNetwork(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        return new CargoLockerMenu(containerId, playerInventory, extraData);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public MenuType<?> getType() {
        return ModMenus.CARGO_LOCKER.get();
    }
}
