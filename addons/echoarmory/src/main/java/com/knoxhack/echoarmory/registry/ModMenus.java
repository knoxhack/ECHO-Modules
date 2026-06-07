package com.knoxhack.echoarmory.registry;

import com.knoxhack.echo.adaptercore.EchoBackendMenuBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoarmory.EchoArmory;
import com.knoxhack.echoarmory.menu.ArmoryStationMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;

public final class ModMenus {
   private static final Object MENUS = EchoBackendRegistryBridge.create(Registries.MENU, EchoArmory.MODID);

   public static final EchoBackendRegistryEntry<MenuType<ArmoryStationMenu>> ARMORY_STATION =
      EchoBackendRegistryBridge.register(MENUS, "armory_station",
         () -> EchoBackendMenuBridge.extendedMenuType(ArmoryStationMenu::fromNetwork));

   private ModMenus() {
   }

   public static void register(Object eventBus) {
      EchoBackendRegistryBridge.registerEventBus(MENUS, eventBus);
   }
}
