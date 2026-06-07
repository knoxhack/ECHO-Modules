package com.knoxhack.echoritualcore.registry;

import com.knoxhack.echo.adaptercore.EchoBackendMenuBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoritualcore.EchoRitualCore;
import com.knoxhack.echoritualcore.menu.RitualAltarMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;

public final class ModMenus {
    private static final Object MENUS =
            EchoBackendRegistryBridge.create(Registries.MENU, EchoRitualCore.MODID);

    public static final EchoBackendRegistryEntry<MenuType<RitualAltarMenu>> RITUAL_ALTAR =
            EchoBackendRegistryBridge.register(MENUS, "ritual_altar",
                    () -> EchoBackendMenuBridge.extendedMenuType(RitualAltarMenu::new));

    private ModMenus() {
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(MENUS, eventBus);
    }
}
