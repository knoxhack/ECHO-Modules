package com.knoxhack.echo.adaptercore;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;

/**
 * AdapterCore backend bridge for extended menu constructors.
 */
public final class EchoBackendMenuBridge {
    private EchoBackendMenuBridge() {
    }

    public static <T extends AbstractContainerMenu> MenuType<T> extendedMenuType(EchoMenuFactory<T> factory) {
        return IMenuTypeExtension.create(factory::create);
    }
}
