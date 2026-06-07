package com.knoxhack.signalos.registry;

import com.knoxhack.echo.adaptercore.EchoBackendMenuBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.signalos.SignalOS;
import com.knoxhack.signalos.menu.SignalOsServerRackMenu;
import com.knoxhack.signalos.menu.SignalOsTerminalMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;

public final class ModMenus {
    private static final Object MENUS =
            EchoBackendRegistryBridge.create(Registries.MENU, SignalOS.MODID);

    public static final EchoBackendRegistryEntry<MenuType<SignalOsTerminalMenu>> TERMINAL =
            EchoBackendRegistryBridge.register(MENUS, "terminal",
                    () -> EchoBackendMenuBridge.extendedMenuType(SignalOsTerminalMenu::new));
    public static final EchoBackendRegistryEntry<MenuType<SignalOsServerRackMenu>> SERVER_RACK =
            EchoBackendRegistryBridge.register(MENUS, "server_rack",
                    () -> EchoBackendMenuBridge.extendedMenuType(SignalOsServerRackMenu::new));

    private ModMenus() {
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(MENUS, eventBus);
    }
}
