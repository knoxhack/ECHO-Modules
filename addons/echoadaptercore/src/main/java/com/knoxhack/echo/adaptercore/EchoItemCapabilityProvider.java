package com.knoxhack.echo.adaptercore;

import net.minecraft.world.item.ItemStack;

@FunctionalInterface
public interface EchoItemCapabilityProvider {
    Object get(ItemStack stack, Object access);
}
