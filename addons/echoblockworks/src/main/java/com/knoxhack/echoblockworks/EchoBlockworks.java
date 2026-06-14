package com.knoxhack.echoblockworks;

import com.echoplatform.echocore.api.EchoRuntimeModules;
import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echoblockworks.integration.BlockworksCoreIntegration;
import com.knoxhack.echoblockworks.integration.BlockworksIndexProvider;
import com.knoxhack.echoblockworks.integration.BlockworksMissionCoreIntegration;
import com.knoxhack.echoblockworks.registry.ModBlockEntities;
import com.knoxhack.echoblockworks.registry.ModBlocks;
import com.knoxhack.echoblockworks.registry.ModCreativeTabs;
import com.knoxhack.echoblockworks.registry.ModItems;
import com.knoxhack.echoblockworks.registry.ModMenus;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(EchoBlockworks.MODID)
public class EchoBlockworks {
   public static final String MODID = "echoblockworks";
   public static final Logger LOGGER = LogUtils.getLogger();
   private static final String COMMON_SETUP_EVENT = "net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent";
   private static final String SERVER_STARTED_EVENT = "net.neoforged.neoforge.event.server.ServerStartedEvent";

   public EchoBlockworks(IEventBus modEventBus) {
      ModBlocks.register(modEventBus);
      ModBlockEntities.register(modEventBus);
      ModItems.register(modEventBus);
      ModMenus.register(modEventBus);
      ModCreativeTabs.register(modEventBus);

      Config.registerEchoConfig();
      EchoBackendLifecycleBridge.registerModListener(modEventBus, COMMON_SETUP_EVENT, this::commonSetup);
      EchoBackendLifecycleBridge.registerGameEventHandler(SERVER_STARTED_EVENT, this::onServerStarted);
        com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge.bootstrapClientEntrypoint(modEventBus,
                "com.knoxhack.echoblockworks.EchoBlockworksClient");
}

   public static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath(MODID, path);
   }

   public void commonSetup(Object event) {
      EchoBackendLifecycleBridge.runCommonSetupWork(event, () -> {
         BlockworksCoreIntegration.registerAddonChapter();
         if (EchoRuntimeModules.isLoaded("echomissioncore")) {
            BlockworksMissionCoreIntegration.register();
         }
         if (EchoRuntimeModules.isLoaded("echoindex")) {
            BlockworksIndexProvider.register();
         }
         LOGGER.info("ECHO Blockworks catalog online.");
      });
   }

   private void onServerStarted(Object event) {
      if (EchoRuntimeModules.isLoaded("echomissioncore")) {
         BlockworksMissionCoreIntegration.registerWhenReady();
      }
   }
}
