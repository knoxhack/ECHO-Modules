package com.knoxhack.echoterminal.client.screen;

import com.knoxhack.echoterminal.menu.EchoTerminalMenu;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Client-only hook for mods that want the ECHO terminal block/keybind to open a
 * branded terminal while keeping the shared terminal menu and API available.
 */
public interface EchoTerminalScreenProvider {
    AbstractContainerScreen<EchoTerminalMenu> create(EchoTerminalMenu menu, Inventory playerInventory, Component title);

    boolean isTerminalScreen(Screen screen);
}
