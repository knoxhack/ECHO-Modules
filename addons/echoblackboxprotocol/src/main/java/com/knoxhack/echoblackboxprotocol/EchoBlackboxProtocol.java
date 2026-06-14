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
import com.echoplatform.echocore.api.EchoRuntimeModules;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(EchoBlackboxProtocol.MODID)
public class EchoBlackboxProtocol {
   public static final String MODID = "echoblackboxprotocol";
   public static final Logger LOGGER = LogUtils.getLogger();
   private static final String COMMON_SETUP_EVENT =
      "net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent";
   private static final String ENTITY_ATTRIBUTE_CREATION_EVENT =
      "net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent";

   EchoBlackboxProtocol() {
      this(null);
}

   public EchoBlackboxProtocol(IEventBus modEventBus) {
      ModBlocks.register(modEventBus);
      ModBlockEntities.register(modEventBus);
      ModItems.register(modEventBus);
      ModEntities.register(modEventBus);
      ModMenus.register(modEventBus);
      ModRecipes.register(modEventBus);
      ModWorldgen.register(modEventBus);
      ModCreativeTabs.register(modEventBus);
      com.knoxhack.echoblackboxprotocol.integration.prime.BlackboxPrimeIntegration.register();
      EchoBackendLifecycleBridge.registerModListener(modEventBus, COMMON_SETUP_EVENT, this::commonSetup);
      EchoBackendLifecycleBridge.registerModListener(modEventBus, ENTITY_ATTRIBUTE_CREATION_EVENT,
         ModEntities::registerAttributes);
      EchoBackendLifecycleBridge.registerOptionalGameTests(modEventBus,
         "com.knoxhack.echoblackboxprotocol.test.ModGameTests");
      com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge.bootstrapClientEntrypoint(modEventBus,
           "com.knoxhack.echoblackboxprotocol.EchoBlackboxProtocolClient");
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
