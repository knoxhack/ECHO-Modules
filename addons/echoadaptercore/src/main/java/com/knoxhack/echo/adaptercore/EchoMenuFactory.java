package com.knoxhack.echo.adaptercore;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

@FunctionalInterface
public interface EchoMenuFactory<T extends AbstractContainerMenu> {
    T create(int windowId, Inventory inventory, RegistryFriendlyByteBuf buffer);
}
