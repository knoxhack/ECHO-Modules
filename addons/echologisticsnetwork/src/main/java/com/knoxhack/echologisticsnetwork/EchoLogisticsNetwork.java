package com.knoxhack.echologisticsnetwork;

import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echologisticsnetwork.content.LogisticsReloaders;
import com.knoxhack.echologisticsnetwork.integration.LogisticsCoreIntegration;
import com.knoxhack.echologisticsnetwork.integration.LogisticsIndustrialMachineEndpointProvider;
import com.knoxhack.echologisticsnetwork.integration.LogisticsMissionCoreIntegration;
import com.knoxhack.echologisticsnetwork.registry.ModBlockEntities;
import com.knoxhack.echologisticsnetwork.registry.ModBlocks;
import com.knoxhack.echologisticsnetwork.registry.ModCreativeTabs;
import com.knoxhack.echologisticsnetwork.registry.ModDataComponents;
import com.knoxhack.echologisticsnetwork.registry.ModEntities;
import com.knoxhack.echologisticsnetwork.registry.ModItems;
import com.knoxhack.echologisticsnetwork.registry.ModMenus;
import com.echoplatform.echocore.api.EchoRuntimeModules;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(EchoLogisticsNetwork.MODID)
public class EchoLogisticsNetwork {
   public static final String MODID = "echologisticsnetwork";
   public static final Logger LOGGER = LogUtils.getLogger();
   private static final String COMMON_SETUP_EVENT =
      "net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent";
   private static final String ENTITY_ATTRIBUTE_CREATION_EVENT =
      "net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent";
   private static final String ADD_SERVER_RELOAD_LISTENERS_EVENT =
      "net.neoforged.neoforge.event.AddServerReloadListenersEvent";

   EchoLogisticsNetwork() {
      this(null);
   }

   public EchoLogisticsNetwork(IEventBus modEventBus) {
      var runtime = Agent9LogisticsNetworkRuntimeAdapter.activateNativeHostEntrypoint();
      LOGGER.info("ECHO Logistics Network Agent 9 native host adapter {}.", runtime.get("status"));
      Config.registerEchoConfig();
      ModDataComponents.register(modEventBus);
      ModBlocks.register(modEventBus);
      ModBlockEntities.register(modEventBus);
      ModItems.register(modEventBus);
      ModMenus.register(modEventBus);
      ModEntities.register(modEventBus);
      ModCreativeTabs.register(modEventBus);
      com.knoxhack.echologisticsnetwork.integration.prime.LogisticsPrimeIntegration.register();
      EchoBackendLifecycleBridge.registerModListener(modEventBus, ENTITY_ATTRIBUTE_CREATION_EVENT,
         ModEntities::registerAttributes);
      EchoBackendLifecycleBridge.registerModListener(modEventBus, COMMON_SETUP_EVENT, this::commonSetup);
      EchoBackendLifecycleBridge.registerGameEventHandler(ADD_SERVER_RELOAD_LISTENERS_EVENT,
         LogisticsReloaders::addServerReloadListeners);
      EchoBackendLifecycleBridge.registerOptionalGameTests(modEventBus,
         "com.knoxhack.echologisticsnetwork.registry.ModGameTests");
   }

   private void commonSetup(Object event) {
      LOGGER.info("ECHO Logistics Network online. Supply chaos is now a routing problem.");
      EchoBackendLifecycleBridge.runCommonSetupWork(event, () -> {
         LogisticsCoreIntegration.registerAddonChapter();
         if (EchoRuntimeModules.isLoaded("echomissioncore")) {
            LogisticsMissionCoreIntegration.register();
         }
         if (EchoRuntimeModules.isLoaded("echomachinecore")) {
            registerMachineCoreIntegration();
         }
         if (EchoRuntimeModules.isLoaded("echoindustrialnexus")) {
            LogisticsIndustrialMachineEndpointProvider.register();
         }
         if (EchoRuntimeModules.isLoaded("echoterminal")) {
            registerTerminalIntegration();
         }
      });
   }

   private static void registerTerminalIntegration() {
      try {
         Class.forName("com.knoxhack.echologisticsnetwork.integration.LogisticsTerminalCommonIntegration")
            .getMethod("register")
            .invoke(null);
      } catch (ReflectiveOperationException exception) {
         LOGGER.warn("ECHO Logistics Network terminal integration could not be registered.", exception);
      }
   }

   private static void registerMachineCoreIntegration() {
      try {
         Class.forName("com.knoxhack.echologisticsnetwork.integration.MachineCoreLogisticsEndpointProvider")
            .getMethod("register")
            .invoke(null);
      } catch (ReflectiveOperationException | LinkageError exception) {
         LOGGER.warn("ECHO Logistics Network MachineCore integration could not be registered.", exception);
      }
   }
}
