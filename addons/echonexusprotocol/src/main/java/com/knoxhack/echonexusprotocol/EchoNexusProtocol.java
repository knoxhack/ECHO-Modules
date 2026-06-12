package com.knoxhack.echonexusprotocol;

import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.echoplatform.echocore.api.EchoRuntimeModules;
import com.knoxhack.echonexusprotocol.command.NexusCommandHandler;
import com.knoxhack.echonexusprotocol.event.NexusArmorEvents;
import com.knoxhack.echonexusprotocol.event.NexusWorldEvents;
import com.knoxhack.echonexusprotocol.integration.NexusCoreIntegration;
import com.knoxhack.echonexusprotocol.integration.NexusTerminalCommonIntegration;
import com.knoxhack.echonexusprotocol.registry.ModAttachments;
import com.knoxhack.echonexusprotocol.registry.ModBlockEntities;
import com.knoxhack.echonexusprotocol.registry.ModBlocks;
import com.knoxhack.echonexusprotocol.registry.ModCreativeTabs;
import com.knoxhack.echonexusprotocol.registry.ModEnergyCapabilities;
import com.knoxhack.echonexusprotocol.registry.ModEntities;
import com.knoxhack.echonexusprotocol.registry.ModItems;
import com.knoxhack.echonexusprotocol.registry.ModMenus;
import com.knoxhack.echonexusprotocol.registry.ModRecipes;
import com.knoxhack.echonexusprotocol.registry.ModSounds;
import com.knoxhack.echonexusprotocol.registry.ModWorldgen;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

public class EchoNexusProtocol {
   public static final String MODID = "echonexusprotocol";
   public static final Logger LOGGER = LogUtils.getLogger();

   public static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath(MODID, path);
   }

   public EchoNexusProtocol(Object modEventBus) {
      ModBlocks.register(modEventBus);
      ModBlockEntities.register(modEventBus);
      ModEntities.register(modEventBus);
      ModItems.register(modEventBus);
      ModRecipes.register(modEventBus);
      ModMenus.register(modEventBus);
      ModSounds.register(modEventBus);
      ModWorldgen.register(modEventBus);
      ModAttachments.register(modEventBus);
      ModCreativeTabs.register(modEventBus);
      com.knoxhack.echonexusprotocol.integration.prime.NexusPrimeIntegration.register();
      EchoBackendLifecycleBridge.registerModListener(modEventBus, this::commonSetup);
      EchoBackendLifecycleBridge.registerModListener(modEventBus, ModEntities::registerAttributes);
      EchoBackendLifecycleBridge.registerModListener(modEventBus, ModEntities::registerSpawnPlacements);
      EchoBackendLifecycleBridge.registerModListener(modEventBus, ModEnergyCapabilities::register);
      NexusWorldEvents worldEvents = new NexusWorldEvents();
      NexusArmorEvents armorEvents = new NexusArmorEvents();
      NexusCommandHandler commands = new NexusCommandHandler();
      EchoBackendLifecycleBridge.registerGameEventHandler(worldEvents::onLevelTick);
      EchoBackendLifecycleBridge.registerGameEventHandler(armorEvents::onPlayerTick);
      EchoBackendLifecycleBridge.registerGameEventHandler(armorEvents::onDamage);
      EchoBackendLifecycleBridge.registerGameEventHandler(commands::register);
      Config.registerEchoConfig();
   }

   private void commonSetup(Object event) {
      LOGGER.info("ECHO-7 Nexus systems initialized. Reality field telemetry online.");
      EchoBackendLifecycleBridge.runCommonSetupWork(event, () -> {
         NexusCoreIntegration.register();
         registerTerminalCommonIntegration();
         if (EchoRuntimeModules.isLoaded("echomachinecore")) {
            tryInvoke("com.knoxhack.echonexusprotocol.integration.NexusMachineCoreRuntimeProvider");
         }
      });
   }

   private static void registerTerminalCommonIntegration() {
      if (!EchoRuntimeModules.isLoaded("echoterminal")) {
         return;
      }

      try {
         NexusTerminalCommonIntegration.register();
      } catch (LinkageError error) {
         LOGGER.warn("ECHO-7 Nexus Terminal common integration skipped because echoterminal APIs were unavailable.", error);
      }
   }

   private static void tryInvoke(String className) {
      try {
         Class.forName(className).getMethod("register").invoke(null);
      } catch (ReflectiveOperationException | LinkageError error) {
         LOGGER.warn("ECHO-7 optional integration {} could not be registered.", className, error);
      }
   }
}
