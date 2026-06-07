package com.knoxhack.echoaetherworks.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoaetherworks.EchoAetherWorks;
import com.knoxhack.echoaetherworks.menu.AetherMachineMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import com.knoxhack.echo.adaptercore.EchoBackendMenuBridge;

public final class ModMenus {
    private static final Object MENUS =
            EchoBackendRegistryBridge.create(Registries.MENU, EchoAetherWorks.MODID);

    public static final EchoBackendRegistryEntry<MenuType<AetherMachineMenu>> AETHER_MACHINE =
            EchoBackendRegistryBridge.register(MENUS, "aether_machine", () -> EchoBackendMenuBridge.extendedMenuType(AetherMachineMenu::new));

    private ModMenus() {
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(MENUS, eventBus);
    }
}
