package com.knoxhack.echoindustrialnexus;

import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.echoplatform.echocore.api.EchoRuntimeModules;
import com.knoxhack.echoindustrialnexus.integration.IndustrialCoreIntegration;
import com.knoxhack.echoindustrialnexus.integration.IndustrialIndexProvider;
import com.knoxhack.echoindustrialnexus.event.IndustrialMultiblockMissionEvents;
import com.knoxhack.echoindustrialnexus.event.IndustrialPoiGenerationHandler;
import com.knoxhack.echoindustrialnexus.network.ModNetwork;
import com.knoxhack.echoindustrialnexus.registry.ModBlockEntities;
import com.knoxhack.echoindustrialnexus.registry.ModBlocks;
import com.knoxhack.echoindustrialnexus.registry.ModCapabilities;
import com.knoxhack.echoindustrialnexus.registry.ModCreativeTabs;
import com.knoxhack.echoindustrialnexus.registry.ModEntities;
import com.knoxhack.echoindustrialnexus.registry.ModFluids;
import com.knoxhack.echoindustrialnexus.registry.ModItems;
import com.knoxhack.echoindustrialnexus.registry.ModMenus;
import com.knoxhack.echoindustrialnexus.registry.ModRecipes;
import com.knoxhack.echoindustrialnexus.registry.ModSounds;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(EchoIndustrialNexus.MODID)
public class EchoIndustrialNexus {
   public static final String MODID = "echoindustrialnexus";
   public static final Logger LOGGER = LogUtils.getLogger();
   private static final String ENTITY_ATTRIBUTE_CREATION_EVENT =
      "net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent";
   private static final String REGISTER_CAPABILITIES_EVENT =
      "net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent";
   private static final String REGISTER_PAYLOAD_HANDLERS_EVENT =
      "net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent";
   private static final String COMMON_SETUP_EVENT =
      "net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent";
   private static final String ROBOTIC_TASK_COMPLETED_EVENT =
      "com.knoxhack.echomultiblockcore.event.RoboticTaskCompletedEvent";

   public static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath(MODID, path);
   }

   EchoIndustrialNexus() {
      this(null);
   }

   public EchoIndustrialNexus(IEventBus modEventBus) {
      var runtime = Agent9IndustrialNexusRuntimeAdapter.activateNativeHostEntrypoint();
      LOGGER.info("ECHO Industrial Nexus Agent 9 native host adapter {}.", runtime.get("status"));
      ModBlocks.register(modEventBus);
      ModFluids.register(modEventBus);
      ModBlockEntities.register(modEventBus);
      ModEntities.register(modEventBus);
      ModItems.register(modEventBus);
      ModMenus.register(modEventBus);
      ModRecipes.register(modEventBus);
      ModSounds.register(modEventBus);
      ModCreativeTabs.register(modEventBus);
      com.knoxhack.echoindustrialnexus.integration.prime.IndustrialPrimeIntegration.register();
      EchoBackendLifecycleBridge.registerModListener(modEventBus, ENTITY_ATTRIBUTE_CREATION_EVENT,
         ModEntities::registerAttributes);
      EchoBackendLifecycleBridge.registerModListener(modEventBus, REGISTER_CAPABILITIES_EVENT,
         ModCapabilities::register);
      EchoBackendLifecycleBridge.registerModListener(modEventBus, REGISTER_PAYLOAD_HANDLERS_EVENT,
         ModNetwork::registerPayloads);
      EchoBackendLifecycleBridge.registerModListener(modEventBus, COMMON_SETUP_EVENT, this::commonSetup);
      EchoBackendLifecycleBridge.registerGameEventHandler(ROBOTIC_TASK_COMPLETED_EVENT,
         new IndustrialMultiblockMissionEvents()::onRoboticTaskCompleted);
      EchoBackendLifecycleBridge.registerGameEventHandler(IndustrialPoiGenerationHandler::onChunkLoad);
      EchoBackendLifecycleBridge.registerGameEventHandler(IndustrialPoiGenerationHandler::onLevelTick);
      EchoBackendLifecycleBridge.registerOptionalGameTests(modEventBus,
            "com.knoxhack.echoindustrialnexus.test.ModGameTests");
      Config.registerEchoConfig();
   }

   private void commonSetup(Object event) {
      LOGGER.info("ECHO Industrial Nexus online. Where survival becomes infrastructure.");
      EchoBackendLifecycleBridge.runCommonSetupWork(event, () -> {
         IndustrialCoreIntegration.registerAddonChapter();
         IndustrialIndexProvider.register();
         registerOptionalMultiblockIntegration();
         if (EchoRuntimeModules.isLoaded("echomachinecore")) {
            invokeOptionalRegister("com.knoxhack.echoindustrialnexus.integration.IndustrialMachineCoreRuntimeProvider");
         }
         if (EchoRuntimeModules.isLoaded("echolens")) {
            invokeOptionalRegister("com.knoxhack.echoindustrialnexus.integration.IndustrialLensIntegration");
         }
         if (EchoRuntimeModules.isLoaded("echoterminal")) {
            registerTerminalIntegration();
         }
      });
   }

   private static void registerOptionalMultiblockIntegration() {
      invokeOptionalRegister("com.knoxhack.echoindustrialnexus.multiblock.IndustrialMultiblockTasks");
      invokeOptionalRegister("com.knoxhack.echoindustrialnexus.integration.IndustrialMultiblockIntegrationProvider");
   }

   private static void invokeOptionalRegister(String className) {
      try {
         Class.forName(className)
            .getMethod("register")
            .invoke(null);
      } catch (ClassNotFoundException exception) {
         LOGGER.debug("Optional Industrial Nexus integration {} is not present.", className);
      } catch (ReflectiveOperationException | LinkageError exception) {
         LOGGER.warn("Optional Industrial Nexus integration {} could not be registered.", className, exception);
      }
   }

   private static void registerTerminalIntegration() {
      try {
         Class.forName("com.knoxhack.echoindustrialnexus.integration.IndustrialTerminalCommonIntegration")
            .getMethod("register")
            .invoke(null);
      } catch (ReflectiveOperationException exception) {
         LOGGER.warn("ECHO Industrial Nexus terminal integration could not be registered.", exception);
      }
   }
}
