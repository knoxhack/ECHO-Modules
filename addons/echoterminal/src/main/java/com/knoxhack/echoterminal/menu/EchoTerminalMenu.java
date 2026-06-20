package com.knoxhack.echoterminal.menu;

import com.knoxhack.echoterminal.registry.ModMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

public class EchoTerminalMenu extends AbstractContainerMenu {
    private final ContainerLevelAccess access;
    private final boolean remoteAccess;

    public EchoTerminalMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        this(containerId, playerInventory);
    }

    public EchoTerminalMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, ContainerLevelAccess.NULL, true);
    }

    public EchoTerminalMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        this(containerId, playerInventory, access, false);
    }

    private EchoTerminalMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access, boolean remoteAccess) {
        super(menuType(), containerId);
        this.access = access == null ? ContainerLevelAccess.NULL : access;
        this.remoteAccess = remoteAccess;
    }

    @Override
    public boolean stillValid(Player player) {
        return remoteAccess
                || nativeLoaderActive()
                || stillValid(access, player, com.knoxhack.echoterminal.registry.ModBlocks.ECHO_TERMINAL_BLOCK.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    private static MenuType<?> menuType() {
        if (nativeLoaderActive()) {
            return null;
        }
        return ModMenus.ECHO_TERMINAL.get();
    }

    private static boolean nativeLoaderActive() {
        return Boolean.parseBoolean(System.getProperty("echo.native.addonsClasspath", "false"))
                || !System.getProperty("echo.native.packId", "").isBlank();
    }
}
