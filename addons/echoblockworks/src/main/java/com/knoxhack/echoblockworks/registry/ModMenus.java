package com.knoxhack.echoblockworks.registry;

import com.knoxhack.echo.adaptercore.EchoBackendMenuBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryBridge;
import com.knoxhack.echo.adaptercore.EchoBackendRegistryEntry;
import com.knoxhack.echoblockworks.EchoBlockworks;
import com.knoxhack.echoblockworks.menu.BlockworksTableMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;

public final class ModMenus {
   private static final Object MENUS = EchoBackendRegistryBridge.create(Registries.MENU, EchoBlockworks.MODID);

   public static final NativeRegistryHolder<MenuType<BlockworksTableMenu>> BLOCKWORKS_TABLE =
      register("blockworks_table", () -> EchoBackendMenuBridge.extendedMenuType(BlockworksTableMenu::fromNetwork));

   private ModMenus() {
   }

   public static void register(Object eventBus) {
      EchoBackendRegistryBridge.registerEventBus(MENUS, eventBus);
   }

   private static <T extends MenuType<?>> NativeRegistryHolder<T> register(String id, java.util.function.Supplier<T> factory) {
      EchoBackendRegistryEntry<T> entry = EchoBackendRegistryBridge.register(MENUS, id, factory);
      return NativeRegistryHolder.deferred(id, entry);
   }
}
