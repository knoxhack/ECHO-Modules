package com.knoxhack.echoblackboxprotocol;

import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echoblackboxprotocol.integration.BlackboxCoreIntegration;
import com.knoxhack.echoblackboxprotocol.integration.BlackboxTerminalCommonIntegration;
import com.knoxhack.echoblackboxprotocol.registry.ModBlockEntities;
import com.knoxhack.echoblackboxprotocol.registry.ModBlocks;
import com.knoxhack.echoblackboxprotocol.registry.ModCreativeTabs;
import com.knoxhack.echoblackboxprotocol.registry.ModEntities;
import com.knoxhack.echoblackboxprotocol.registry.ModItems;
import com.knoxhack.echoblackboxprotocol.registry.ModMenus;
import com.knoxhack.echoblackboxprotocol.registry.ModRecipes;
import com.knoxhack.echoblackboxprotocol.registry.ModWorldgen;
import com.mojang.logging.LogUtils;
import com.knoxhack.echocore.api.EchoRuntimeModules;
import org.slf4j.Logger;

public class EchoBlackboxProtocol {
   public static final String MODID = "echoblackboxprotocol";
   public static final Logger LOGGER = LogUtils.getLogger();

   public EchoBlackboxProtocol(Object modEventBus) {
      ModBlocks.register(modEventBus);
      ModBlockEntities.register(modEventBus);
      ModItems.register(modEventBus);
      ModEntities.register(modEventBus);
      ModMenus.register(modEventBus);
      ModRecipes.register(modEventBus);
      ModWorldgen.register(modEventBus);
      ModCreativeTabs.register(modEventBus);
      com.knoxhack.echoblackboxprotocol.integration.prime.BlackboxPrimeIntegration.register();
      EchoBackendLifecycleBridge.registerModListener(modEventBus, this::commonSetup);
      EchoBackendLifecycleBridge.registerModListener(modEventBus, ModEntities::registerAttributes);
   }

   private void commonSetup(Object event) {
      LOGGER.info("ECHO-7 blackbox protocol initialized. Truth Engine cold-start accepted.");
      EchoBackendLifecycleBridge.runCommonSetupWork(event, () -> {
         BlackboxCoreIntegration.registerAddonChapter();
         if (EchoRuntimeModules.isLoaded("echoterminal")) {
            BlackboxTerminalCommonIntegration.register();
         }
      });
   }
}
