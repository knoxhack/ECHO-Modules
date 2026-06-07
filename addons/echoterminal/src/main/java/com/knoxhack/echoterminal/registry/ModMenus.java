package com.knoxhack.echoterminal.registry;

import com.knoxhack.echo.adaptercore.EchoBackendMenuBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoterminal.EchoTerminal;
import com.knoxhack.echoterminal.menu.EchoTerminalMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;

public final class ModMenus {
    private static final Object MENUS =
            EchoBackendRegistryBridge.create(Registries.MENU, EchoTerminal.MODID);

    public static final EchoBackendRegistryEntry<MenuType<EchoTerminalMenu>> ECHO_TERMINAL =
            EchoBackendRegistryBridge.register(MENUS, "echo_terminal",
                    () -> EchoBackendMenuBridge.extendedMenuType(EchoTerminalMenu::new));

    private ModMenus() {
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(MENUS, eventBus);
    }
}
