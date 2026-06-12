package com.knoxhack.echoarmory;

import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echoarmory.content.ArmoryReloaders;
import com.knoxhack.echoarmory.event.ArmoryEvents;
import com.knoxhack.echoarmory.integration.ArmoryCoreIntegration;
import com.knoxhack.echoarmory.integration.ArmoryIndexProvider;
import com.knoxhack.echoarmory.integration.ArmoryMissionCoreIntegration;
import com.knoxhack.echoarmory.integration.ArmoryOptionalIntegrations;
import com.knoxhack.echoarmory.registry.ModBlockEntities;
import com.knoxhack.echoarmory.registry.ModBlocks;
import com.knoxhack.echoarmory.registry.ModCreativeTabs;
import com.knoxhack.echoarmory.registry.ModDataComponents;
import com.knoxhack.echoarmory.registry.ModEntities;
import com.knoxhack.echoarmory.registry.ModItems;
import com.knoxhack.echoarmory.registry.ModMenus;
import com.echoplatform.echocore.api.EchoRuntimeModules;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class EchoArmory {
   public static final String MODID = "echoarmory";
   public static final Logger LOGGER = LogUtils.getLogger();

   public EchoArmory(Object modEventBus) {
      ModDataComponents.register(modEventBus);
      ModBlocks.register(modEventBus);
      ModBlockEntities.register(modEventBus);
      ModEntities.register(modEventBus);
      ModItems.register(modEventBus);
      ModMenus.register(modEventBus);
      ModCreativeTabs.register(modEventBus);
      com.knoxhack.echoarmory.integration.prime.ArmoryPrimeIntegration.register();
      EchoBackendLifecycleBridge.registerModListener(modEventBus, this::commonSetup);
      EchoBackendLifecycleBridge.registerGameEventHandler(ArmoryReloaders::addServerReloadListeners);
      EchoBackendLifecycleBridge.registerGameEventHandler(ArmoryEvents::onPlayerTick);
      EchoBackendLifecycleBridge.registerGameEventHandler(ArmoryEvents::onLivingDamage);
      EchoBackendLifecycleBridge.registerGameEventHandler(ArmoryEvents::onItemCrafted);
   }

   private void commonSetup(Object event) {
      LOGGER.info("ECHO Armory online. Modular survival is now mission-ready.");
      EchoBackendLifecycleBridge.runCommonSetupWork(event, () -> {
         ArmoryCoreIntegration.registerAddonChapter();
         ArmoryIndexProvider.register();
         ArmoryOptionalIntegrations.register();
         if (EchoRuntimeModules.isLoaded("echomissioncore")) {
            ArmoryMissionCoreIntegration.register();
         }
         if (EchoRuntimeModules.isLoaded("echoterminal")) {
            registerTerminalIntegration();
         }
      });
   }

   private static void registerTerminalIntegration() {
      try {
         Class.forName("com.knoxhack.echoarmory.integration.ArmoryTerminalCommonIntegration")
            .getMethod("register")
            .invoke(null);
      } catch (ReflectiveOperationException exception) {
         LOGGER.warn("ECHO Armory terminal integration could not be registered.", exception);
      }
   }
}
