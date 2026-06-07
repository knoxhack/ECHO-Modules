package com.knoxhack.echopowergrid.registry;

import com.knoxhack.echopowergrid.menu.PowerNodeMenu;
import com.knoxhack.echopowergrid.menu.SubstationMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.MenuType;

public final class ModMenus {
    public static final NativeRegistryHolder<MenuType<SubstationMenu>> SUBSTATION =
        NativeRegistryHolder.of("substation", new MenuType<>(SubstationMenu::new, null));

    public static final NativeRegistryHolder<MenuType<PowerNodeMenu>> POWER_NODE =
        NativeRegistryHolder.of("power_node", new MenuType<>(
                (containerId, inventory) -> new PowerNodeMenu(containerId, inventory, inventory.player.level(), BlockPos.ZERO),
                null));

    private ModMenus() {}

    public static void register() {
    }
}
