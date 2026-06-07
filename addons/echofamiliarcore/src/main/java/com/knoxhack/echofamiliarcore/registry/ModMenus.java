package com.knoxhack.echofamiliarcore.registry;

import com.knoxhack.echo.adaptercore.EchoBackendMenuBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echofamiliarcore.EchoFamiliarCore;
import com.knoxhack.echofamiliarcore.menu.FamiliarCommandMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;

public final class ModMenus {
    private static final Object MENUS =
            EchoBackendRegistryBridge.create(Registries.MENU, EchoFamiliarCore.MODID);

    public static final EchoBackendRegistryEntry<MenuType<FamiliarCommandMenu>> FAMILIAR_COMMAND =
            EchoBackendRegistryBridge.register(MENUS, "familiar_command",
                    () -> EchoBackendMenuBridge.extendedMenuType(FamiliarCommandMenu::new));

    private ModMenus() {
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(MENUS, eventBus);
    }
}
