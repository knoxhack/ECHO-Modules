package com.knoxhack.echoindustrialnexus.registry;

import com.knoxhack.echo.adaptercore.EchoBackendMenuBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoindustrialnexus.EchoIndustrialNexus;
import com.knoxhack.echoindustrialnexus.menu.IndustrialMachineMenu;
import com.knoxhack.echoindustrialnexus.menu.IndustrialMultiblockControllerMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;

public final class ModMenus {
   private static final Object MENUS = EchoBackendRegistryBridge.create(Registries.MENU, EchoIndustrialNexus.MODID);

   public static final EchoBackendRegistryEntry<MenuType<IndustrialMachineMenu>> INDUSTRIAL_MACHINE =
      EchoBackendRegistryBridge.register(MENUS, "industrial_machine", () -> EchoBackendMenuBridge.extendedMenuType(IndustrialMachineMenu::fromNetwork));

   public static final EchoBackendRegistryEntry<MenuType<IndustrialMultiblockControllerMenu>> INDUSTRIAL_MULTIBLOCK_CONTROLLER =
      EchoBackendRegistryBridge.register(MENUS, "industrial_multiblock_controller", () -> EchoBackendMenuBridge.extendedMenuType(IndustrialMultiblockControllerMenu::fromNetwork));

   private ModMenus() {
   }

   public static void register(Object eventBus) {
      EchoBackendRegistryBridge.registerEventBus(MENUS, eventBus);
   }
}
