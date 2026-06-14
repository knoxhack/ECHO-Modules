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
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(EchoArmory.MODID)
public class EchoArmory {
   public static final String MODID = "echoarmory";
   public static final Logger LOGGER = LogUtils.getLogger();
   private static final String COMMON_SETUP_EVENT =
      "net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent";
   private static final String ADD_SERVER_RELOAD_LISTENERS_EVENT =
      "net.neoforged.neoforge.event.AddServerReloadListenersEvent";
   private static final String PLAYER_TICK_POST_EVENT =
      "net.neoforged.neoforge.event.tick.PlayerTickEvent$Post";
   private static final String LIVING_DAMAGE_PRE_EVENT =
      "net.neoforged.neoforge.event.entity.living.LivingDamageEvent$Pre";
   private static final String ITEM_CRAFTED_EVENT =
      "net.neoforged.neoforge.event.entity.player.PlayerEvent$ItemCraftedEvent";

   EchoArmory() {
      this(null);
}

   public EchoArmory(IEventBus modEventBus) {
      ModDataComponents.register(modEventBus);
      ModBlocks.register(modEventBus);
      ModBlockEntities.register(modEventBus);
      ModEntities.register(modEventBus);
      ModItems.register(modEventBus);
      ModMenus.register(modEventBus);
      ModCreativeTabs.register(modEventBus);
      com.knoxhack.echoarmory.integration.prime.ArmoryPrimeIntegration.register();
      EchoBackendLifecycleBridge.registerModListener(modEventBus, COMMON_SETUP_EVENT, this::commonSetup);
      EchoBackendLifecycleBridge.registerGameEventHandler(ADD_SERVER_RELOAD_LISTENERS_EVENT,
         ArmoryReloaders::addServerReloadListeners);
      EchoBackendLifecycleBridge.registerGameEventHandler(PLAYER_TICK_POST_EVENT, ArmoryEvents::onPlayerTick);
      EchoBackendLifecycleBridge.registerGameEventHandler(LIVING_DAMAGE_PRE_EVENT, ArmoryEvents::onLivingDamage);
      EchoBackendLifecycleBridge.registerGameEventHandler(ITEM_CRAFTED_EVENT, ArmoryEvents::onItemCrafted);
      EchoBackendLifecycleBridge.registerOptionalGameTests(modEventBus,
         "com.knoxhack.echoarmory.registry.ModGameTests");
      com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge.bootstrapClientEntrypoint(modEventBus,
           "com.knoxhack.echoarmory.EchoArmoryClient");
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
