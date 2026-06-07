package com.knoxhack.echonexusprotocol.registry;

import com.knoxhack.echo.adaptercore.EchoBackendMenuBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echonexusprotocol.EchoNexusProtocol;
import com.knoxhack.echonexusprotocol.menu.NexusMachineMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;

public final class ModMenus {
   private static final Object MENUS = EchoBackendRegistryBridge.create(Registries.MENU, EchoNexusProtocol.MODID);
   public static final EchoBackendRegistryEntry<MenuType<NexusMachineMenu>> NEXUS_MACHINE = EchoBackendRegistryBridge.register(
      MENUS, "nexus_machine", () -> EchoBackendMenuBridge.extendedMenuType(NexusMachineMenu::fromNetwork));
   private ModMenus() {}
   public static void register(Object eventBus) { EchoBackendRegistryBridge.registerEventBus(MENUS, eventBus); }
}
