package com.knoxhack.echocursecore.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echocursecore.EchoCurseCore;
import com.knoxhack.echocursecore.menu.CurseContractMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import com.knoxhack.echo.adaptercore.EchoBackendMenuBridge;

public final class ModMenus {
    private static final Object MENUS =
            EchoBackendRegistryBridge.create(Registries.MENU, EchoCurseCore.MODID);

    public static final EchoBackendRegistryEntry<MenuType<CurseContractMenu>> CURSE_CONTRACT =
            EchoBackendRegistryBridge.register(MENUS, "curse_contract", () -> EchoBackendMenuBridge.extendedMenuType(CurseContractMenu::new));

    private ModMenus() {
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(MENUS, eventBus);
    }
}
