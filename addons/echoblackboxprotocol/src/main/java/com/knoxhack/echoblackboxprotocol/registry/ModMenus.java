package com.knoxhack.echoblackboxprotocol.registry;

import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoblackboxprotocol.EchoBlackboxProtocol;
import com.knoxhack.echoblackboxprotocol.menu.BlackboxMachineMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import com.knoxhack.echo.adaptercore.EchoBackendMenuBridge;

public final class ModMenus {
   private static final Object MENUS = EchoBackendRegistryBridge.create(Registries.MENU, EchoBlackboxProtocol.MODID);

   public static final EchoBackendRegistryEntry<MenuType<BlackboxMachineMenu>> BLACKBOX_MACHINE =
      EchoBackendRegistryBridge.register(MENUS, "blackbox_machine", () -> EchoBackendMenuBridge.extendedMenuType(BlackboxMachineMenu::fromNetwork));

   private ModMenus() {
   }

   public static void register(Object eventBus) {
      EchoBackendRegistryBridge.registerEventBus(MENUS, eventBus);
   }
}
