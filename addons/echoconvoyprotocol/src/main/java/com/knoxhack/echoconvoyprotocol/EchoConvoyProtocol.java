package com.knoxhack.echoconvoyprotocol;

import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echoconvoyprotocol.content.ConvoyReloaders;
import com.knoxhack.echoconvoyprotocol.command.ConvoyCommands;
import com.knoxhack.echoconvoyprotocol.event.ConvoyEvents;
import com.knoxhack.echoconvoyprotocol.event.ModTooltipEvents;
import com.knoxhack.echoconvoyprotocol.integration.ConvoyCoreIntegration;
import com.knoxhack.echoconvoyprotocol.integration.ConvoyHoloMapProvider;
import com.knoxhack.echoconvoyprotocol.integration.ConvoyIndexProvider;
import com.knoxhack.echoconvoyprotocol.integration.ConvoyLogisticsIntegration;
import com.knoxhack.echoconvoyprotocol.integration.ConvoyMultiblockProviders;
import com.knoxhack.echoconvoyprotocol.network.ModNetwork;
import com.knoxhack.echoconvoyprotocol.registry.ModBlocks;
import com.knoxhack.echoconvoyprotocol.registry.ModBlockEntities;
import com.knoxhack.echoconvoyprotocol.registry.ModCreativeTabs;
import com.knoxhack.echoconvoyprotocol.registry.ModEntities;
import com.knoxhack.echoconvoyprotocol.registry.ModItems;
import com.knoxhack.echoconvoyprotocol.registry.ModMenus;
import com.knoxhack.echoconvoyprotocol.registry.ModRecipes;
import com.knoxhack.echoconvoyprotocol.task.ConvoyMultiblockTasks;
import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.EchoRuntimeModules;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class EchoConvoyProtocol {
   public static final String MODID = "echoconvoyprotocol";
   public static final Logger LOGGER = LogUtils.getLogger();

   public EchoConvoyProtocol(Object modEventBus) {
      var runtime = Agent9ConvoyRuntimeAdapter.activateNativeHostEntrypoint();
      LOGGER.info("ECHO Convoy Protocol Agent 9 native host adapter {}.", runtime.get("status"));
      ModBlocks.register(modEventBus);
      ModBlockEntities.register(modEventBus);
      ModEntities.register(modEventBus);
      ModItems.register(modEventBus);
      ModMenus.register(modEventBus);
      ModRecipes.register(modEventBus);
      ModCreativeTabs.register(modEventBus);
      com.knoxhack.echoconvoyprotocol.integration.prime.ConvoyPrimeIntegration.register();
      EchoBackendLifecycleBridge.registerModListener(modEventBus, this::commonSetup);
      EchoBackendLifecycleBridge.registerModListener(modEventBus, ModNetwork::registerPayloads);
      EchoBackendLifecycleBridge.registerGameEventHandler(ConvoyReloaders::addServerReloadListeners);
      EchoBackendLifecycleBridge.registerGameEventHandler(event -> ConvoyCommands.register(event));
      EchoBackendLifecycleBridge.registerGameEventHandler(ConvoyEvents::onPlayerTick);
      EchoBackendLifecycleBridge.registerGameEventHandler(ModTooltipEvents::onItemTooltip);
   }

   private void commonSetup(Object event) {
      LOGGER.info("ECHO Convoy Protocol initialized. Road crews are improvising.");
      EchoBackendLifecycleBridge.runCommonSetupWork(event, () -> {
         ConvoyMultiblockProviders.register();
         ConvoyMultiblockTasks.register();
         ConvoyCoreIntegration.registerAddonChapter();
         Config.registerEchoConfig();
         ConvoyIndexProvider.register();
         ConvoyLogisticsIntegration.register();
         EchoCoreServices.registerMapDataProvider(ConvoyHoloMapProvider.INSTANCE);
         if (EchoRuntimeModules.isLoaded("echoterminal")) {
            registerTerminalIntegration();
         }
      });
   }

   private static void registerTerminalIntegration() {
      try {
         Class.forName("com.knoxhack.echoconvoyprotocol.integration.ConvoyTerminalCommonIntegration")
            .getMethod("register")
            .invoke(null);
      } catch (ReflectiveOperationException exception) {
         LOGGER.warn("ECHO Convoy Protocol terminal integration could not be registered.", exception);
      }
   }
}
