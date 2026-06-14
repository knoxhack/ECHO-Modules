package com.knoxhack.echo.settlementcore.registry;

import com.knoxhack.echo.adaptercore.EchoBackendMenuBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echo.settlementcore.EchoSettlementCore;
import com.knoxhack.echo.settlementcore.menu.CargoLockerMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;

public final class ModMenus {
    private static final Object MENUS = EchoBackendRegistryBridge.create(Registries.MENU, EchoSettlementCore.MODID);

    public static final EchoBackendRegistryEntry<MenuType<CargoLockerMenu>> CARGO_LOCKER =
        EchoBackendRegistryBridge.register(MENUS, "cargo_locker",
            () -> EchoBackendMenuBridge.extendedMenuType(CargoLockerMenu::fromNetwork));

    private ModMenus() {
    }

    public static void register(Object eventBus) {
        EchoBackendRegistryBridge.registerEventBus(MENUS, eventBus);
    }
}
