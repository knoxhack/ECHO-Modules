package com.knoxhack.echoblockworks;

import com.echoplatform.echocore.api.EchoRuntimeModules;
import com.knoxhack.echoblockworks.client.BlockworksTableScreen;
import com.knoxhack.echoblockworks.registry.ModMenus;
import java.util.List;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

public class EchoBlockworksClient {
   public EchoBlockworksClient() {
      if (EchoRuntimeModules.isLoaded("echorendercore")) {
         registerRenderCoreStaticSurfaces();
      }
   }

   public List<NativeScreenRegistration<?>> screenFactories() {
      return List.of(new NativeScreenRegistration<>(ModMenus.BLOCKWORKS_TABLE.id(), BlockworksTableScreen::new));
   }

   private static void registerRenderCoreStaticSurfaces() {
      try {
         Class.forName("com.knoxhack.echoblockworks.integration.BlockworksRenderCoreClientIntegration")
            .getMethod("registerStaticSurfaces")
            .invoke(null);
      } catch (ReflectiveOperationException | LinkageError exception) {
         EchoBlockworks.LOGGER.warn("ECHO Blockworks RenderCore static surface integration could not be registered.", exception);
      }
   }

   public record NativeScreenRegistration<T extends AbstractContainerMenu>(
      String menuId,
      NativeScreenFactory<T> factory) {
   }

   @FunctionalInterface
   public interface NativeScreenFactory<T extends AbstractContainerMenu> {
      Screen create(T menu, Inventory inventory, Component title);
   }
}
