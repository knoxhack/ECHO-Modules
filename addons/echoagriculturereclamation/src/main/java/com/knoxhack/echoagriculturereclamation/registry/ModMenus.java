package com.knoxhack.echoagriculturereclamation.registry;

import com.knoxhack.echo.adaptercore.EchoBackendMenuBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoagriculturereclamation.EchoAgricultureReclamation;
import com.knoxhack.echoagriculturereclamation.menu.HydroponicTrayMenu;
import com.knoxhack.echoagriculturereclamation.menu.ReclamationMachineMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;

public final class ModMenus {
   private static final Object MENUS = EchoBackendRegistryBridge.create(Registries.MENU, EchoAgricultureReclamation.MODID);

   public static final EchoBackendRegistryEntry<MenuType<ReclamationMachineMenu>> RECLAMATION_MACHINE =
      EchoBackendRegistryBridge.register(MENUS, "reclamation_machine", () -> EchoBackendMenuBridge.extendedMenuType(ReclamationMachineMenu::fromNetwork));
   public static final EchoBackendRegistryEntry<MenuType<HydroponicTrayMenu>> HYDROPONIC_TRAY =
      EchoBackendRegistryBridge.register(MENUS, "hydroponic_tray", () -> EchoBackendMenuBridge.extendedMenuType(HydroponicTrayMenu::fromNetwork));

   private ModMenus() {
   }

   public static void register(Object eventBus) {
      EchoBackendRegistryBridge.registerEventBus(MENUS, eventBus);
   }
}
