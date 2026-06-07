package com.knoxhack.echoorbitalremnants.registry;

import com.knoxhack.echo.adaptercore.EchoBackendMenuBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoorbitalremnants.EchoOrbitalRemnants;
import com.knoxhack.echoorbitalremnants.menu.OrbitalMachineMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;

public final class ModMenus {
    private static final Object MENUS =
            EchoBackendRegistryBridge.create(Registries.MENU, EchoOrbitalRemnants.MODID);

    public static final EchoBackendRegistryEntry<MenuType<OrbitalMachineMenu>> ORBITAL_MACHINE =
            EchoBackendRegistryBridge.register(MENUS, "orbital_machine",
                    () -> EchoBackendMenuBridge.extendedMenuType(OrbitalMachineMenu::fromNetwork));

    private ModMenus() {
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(MENUS, eventBus);
    }
}
