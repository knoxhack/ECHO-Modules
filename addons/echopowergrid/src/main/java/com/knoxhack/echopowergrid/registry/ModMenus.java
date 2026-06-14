package com.knoxhack.echopowergrid.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echopowergrid.EchoPowerGrid;
import com.knoxhack.echopowergrid.menu.PowerNodeMenu;
import com.knoxhack.echopowergrid.menu.SubstationMenu;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.MenuType;

public final class ModMenus {
    private static final Object MENUS = EchoBackendRegistryBridge.create(BuiltInRegistries.MENU, EchoPowerGrid.MODID);

    public static final NativeRegistryHolder<MenuType<SubstationMenu>> SUBSTATION =
        tracked("substation", () -> new MenuType<>(SubstationMenu::new, null));

    public static final NativeRegistryHolder<MenuType<PowerNodeMenu>> POWER_NODE =
        tracked("power_node", () -> new MenuType<>(
                (containerId, inventory) -> new PowerNodeMenu(containerId, inventory, inventory.player.level(), BlockPos.ZERO),
                null));

    private ModMenus() {}

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(MENUS, eventBus);
    }

    private static <T extends MenuType<?>> NativeRegistryHolder<T> tracked(
            String name, java.util.function.Supplier<? extends T> menu) {
        EchoBackendRegistryEntry<T> entry = EchoBackendRegistryBridge.register(MENUS, name, menu);
        return NativeRegistryHolder.deferred(name, entry);
    }
}
