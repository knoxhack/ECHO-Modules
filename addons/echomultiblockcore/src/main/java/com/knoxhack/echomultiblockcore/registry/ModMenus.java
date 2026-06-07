package com.knoxhack.echomultiblockcore.registry;

import com.knoxhack.echo.adaptercore.EchoBackendMenuBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echomultiblockcore.EchoMultiblockCore;
import com.knoxhack.echomultiblockcore.menu.MultiblockControllerMenu;
import com.knoxhack.echomultiblockcore.menu.MultiblockCrateMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;

public final class ModMenus {
    private static final Object MENUS = EchoBackendRegistryBridge.create(Registries.MENU, EchoMultiblockCore.MODID);

    public static final EchoBackendRegistryEntry<MenuType<MultiblockControllerMenu>> CONTROLLER =
            EchoBackendRegistryBridge.register(MENUS, "controller",
                    () -> EchoBackendMenuBridge.extendedMenuType(MultiblockControllerMenu::fromNetwork));
    public static final EchoBackendRegistryEntry<MenuType<MultiblockCrateMenu>> CRATE =
            EchoBackendRegistryBridge.register(MENUS, "crate",
                    () -> EchoBackendMenuBridge.extendedMenuType(MultiblockCrateMenu::fromNetwork));

    private ModMenus() {
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(MENUS, eventBus);
    }
}
