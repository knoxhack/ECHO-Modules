package com.knoxhack.echoagriculturereclamation;

import com.knoxhack.echo.adaptercore.EchoBackendCommandEventBridge;
import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.echoplatform.echocore.api.EchoRuntimeModules;
import com.knoxhack.echoagriculturereclamation.command.ReclamationCommands;
import com.knoxhack.echoagriculturereclamation.content.ReclamationReloaders;
import com.knoxhack.echoagriculturereclamation.integration.ReclamationCoreIntegration;
import com.knoxhack.echoagriculturereclamation.registry.ModBlockEntities;
import com.knoxhack.echoagriculturereclamation.registry.ModBlocks;
import com.knoxhack.echoagriculturereclamation.registry.ModCreativeTabs;
import com.knoxhack.echoagriculturereclamation.registry.ModDataComponents;
import com.knoxhack.echoagriculturereclamation.registry.ModEntities;
import com.knoxhack.echoagriculturereclamation.registry.ModItems;
import com.knoxhack.echoagriculturereclamation.registry.ModMenus;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class EchoAgricultureReclamation {
   public static final String MODID = "echoagriculturereclamation";
   public static final Logger LOGGER = LogUtils.getLogger();

   public EchoAgricultureReclamation(Object modEventBus, Object modContainer) {
      ModBlocks.register(modEventBus);
      ModBlockEntities.register(modEventBus);
      ModEntities.register(modEventBus);
      ModMenus.register(modEventBus);
      ModDataComponents.register(modEventBus);
      ModItems.register(modEventBus);
      ModCreativeTabs.register(modEventBus);
      com.knoxhack.echoagriculturereclamation.integration.prime.ReclamationPrimeIntegration.register();
      EchoBackendLifecycleBridge.registerModListener(modEventBus, ModEntities::registerAttributes);
      EchoBackendLifecycleBridge.registerModListener(modEventBus, this::commonSetup);
      EchoBackendLifecycleBridge.registerGameEventHandler(this::registerCommands);
      EchoBackendLifecycleBridge.registerGameEventHandler(ReclamationReloaders::addServerReloadListeners);
        com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge.bootstrapClientEntrypoint(modEventBus,
                "com.knoxhack.echoagriculturereclamation.EchoAgricultureReclamationClient");
}

   private void commonSetup(Object event) {
      LOGGER.info("ECHO Agriculture Reclamation online. Dead worlds do not get the final word.");
      EchoBackendLifecycleBridge.runCommonSetupWork(event, () -> {
         ReclamationCoreIntegration.registerAddonChapter();
         if (EchoRuntimeModules.isLoaded("echoterminal")) {
            registerTerminalIntegration();
         }
         registerOptionalIntegration("echolens", "ReclamationLensIntegration");
         registerOptionalIntegration("echotutorialcore", "ReclamationTutorialIntegration");
         registerOptionalIntegration("echomissioncore", "ReclamationMissionContentIntegration");
         registerOptionalIntegration("echologisticsnetwork", "ReclamationLogisticsIntegration");
      });
   }

   private void registerCommands(Object event) {
      var dispatcher = EchoBackendCommandEventBridge.dispatcher(event);
      if (dispatcher != null) {
         ReclamationCommands.register(dispatcher, null);
      }
   }

   private static void registerTerminalIntegration() {
      try {
         Class.forName("com.knoxhack.echoagriculturereclamation.integration.ReclamationTerminalCommonIntegration")
            .getMethod("register")
            .invoke(null);
      } catch (ReflectiveOperationException | LinkageError exception) {
         LOGGER.warn("ECHO Agriculture Reclamation terminal integration could not be registered.", exception);
      }
   }

   private static void registerOptionalIntegration(String modId, String simpleClassName) {
      if (!EchoRuntimeModules.isLoaded(modId)) {
         return;
      }
      try {
         Class.forName("com.knoxhack.echoagriculturereclamation.integration." + simpleClassName)
            .getMethod("register")
            .invoke(null);
      } catch (ReflectiveOperationException | LinkageError exception) {
         LOGGER.warn("ECHO Agriculture Reclamation optional {} integration could not be registered.", modId, exception);
      }
   }
}
