package com.knoxhack.echorecovery.registry;

import com.knoxhack.echo.adaptercore.EchoBackendMenuBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echorecovery.EchoRecovery;
import com.knoxhack.echorecovery.menu.GraveMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;

public final class ModMenus {
    private static final Object MENUS =
        EchoBackendRegistryBridge.create(Registries.MENU, EchoRecovery.MODID);

    public static final EchoBackendRegistryEntry<MenuType<GraveMenu>> GRAVE =
        EchoBackendRegistryBridge.register(MENUS, "grave", () -> EchoBackendMenuBridge.extendedMenuType(GraveMenu::new));

    private ModMenus() {}

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(MENUS, eventBus);
    }
}
