package com.knoxhack.echoconvoyprotocol.registry;

import com.knoxhack.echo.adaptercore.EchoBackendMenuBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoconvoyprotocol.EchoConvoyProtocol;
import com.knoxhack.echoconvoyprotocol.menu.ConvoyStationMenu;
import com.knoxhack.echoconvoyprotocol.menu.ConvoyUpgradeMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;

public final class ModMenus {
   private static final Object MENUS = EchoBackendRegistryBridge.create(Registries.MENU, EchoConvoyProtocol.MODID);

   public static final EchoBackendRegistryEntry<MenuType<ConvoyStationMenu>> CONVOY_STATION =
      EchoBackendRegistryBridge.register(MENUS, "convoy_station", () -> EchoBackendMenuBridge.extendedMenuType(ConvoyStationMenu::fromNetwork));

   public static final EchoBackendRegistryEntry<MenuType<ConvoyUpgradeMenu>> VEHICLE_UPGRADES =
      EchoBackendRegistryBridge.register(MENUS, "vehicle_upgrades", () -> EchoBackendMenuBridge.extendedMenuType(ConvoyUpgradeMenu::fromNetwork));

   private ModMenus() {
   }

   public static void register(Object eventBus) {
      EchoBackendRegistryBridge.registerEventBus(MENUS, eventBus);
   }
}
