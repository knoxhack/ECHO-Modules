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
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(EchoNexusProtocol.MODID)
public class EchoNexusProtocol {
   public static final String MODID = "echonexusprotocol";
   public static final Logger LOGGER = LogUtils.getLogger();
   private static final String COMMON_SETUP_EVENT =
      "net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent";
   private static final String ENTITY_ATTRIBUTE_CREATION_EVENT =
      "net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent";
   private static final String REGISTER_SPAWN_PLACEMENTS_EVENT =
      "net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent";
   private static final String REGISTER_CAPABILITIES_EVENT =
      "net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent";
   private static final String LEVEL_TICK_POST_EVENT =
      "net.neoforged.neoforge.event.tick.LevelTickEvent$Post";
   private static final String PLAYER_TICK_POST_EVENT =
      "net.neoforged.neoforge.event.tick.PlayerTickEvent$Post";
   private static final String LIVING_DAMAGE_PRE_EVENT =
      "net.neoforged.neoforge.event.entity.living.LivingDamageEvent$Pre";
   private static final String REGISTER_COMMANDS_EVENT =
      "net.neoforged.neoforge.event.RegisterCommandsEvent";
   private static final AtomicBoolean COMMON_SERVICES_REGISTERED = new AtomicBoolean(false);

   public static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath(MODID, path);
   }

   EchoNexusProtocol() {
      this(null);
}

   public EchoNexusProtocol(IEventBus modEventBus) {
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
      EchoBackendLifecycleBridge.registerModListener(modEventBus, COMMON_SETUP_EVENT, this::commonSetup);
      EchoBackendLifecycleBridge.registerModListener(modEventBus, ENTITY_ATTRIBUTE_CREATION_EVENT,
         ModEntities::registerAttributes);
      EchoBackendLifecycleBridge.registerModListener(modEventBus, REGISTER_SPAWN_PLACEMENTS_EVENT,
         ModEntities::registerSpawnPlacements);
      EchoBackendLifecycleBridge.registerModListener(modEventBus, REGISTER_CAPABILITIES_EVENT,
         ModEnergyCapabilities::register);
      NexusWorldEvents worldEvents = new NexusWorldEvents();
      NexusArmorEvents armorEvents = new NexusArmorEvents();
      NexusCommandHandler commands = new NexusCommandHandler();
      EchoBackendLifecycleBridge.registerGameEventHandler(LEVEL_TICK_POST_EVENT, worldEvents::onLevelTick);
      EchoBackendLifecycleBridge.registerGameEventHandler(PLAYER_TICK_POST_EVENT, armorEvents::onPlayerTick);
      EchoBackendLifecycleBridge.registerGameEventHandler(LIVING_DAMAGE_PRE_EVENT, armorEvents::onDamage);
      EchoBackendLifecycleBridge.registerGameEventHandler(REGISTER_COMMANDS_EVENT, commands::register);
      EchoBackendLifecycleBridge.registerOptionalGameTests(modEventBus,
         "com.knoxhack.echonexusprotocol.test.ModGameTests");
      Config.registerEchoConfig();
      com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge.bootstrapClientEntrypoint(modEventBus,
           "com.knoxhack.echonexusprotocol.EchoNexusProtocolClient");
}

   private void commonSetup(Object event) {
      EchoBackendLifecycleBridge.runCommonSetupWork(event, () -> registerCommonServices("neoforge_common_setup"));
   }

   public static boolean ensureCommonServicesRegisteredForNativeLoader() {
      return registerCommonServices("native_loader_module_ready");
   }

   private static boolean registerCommonServices(String source) {
      if (!COMMON_SERVICES_REGISTERED.compareAndSet(false, true)) {
         return false;
      }
      LOGGER.info("ECHO-7 Nexus systems initialized [{}]. Reality field telemetry online.", source);
      NexusCoreIntegration.register();
      registerTerminalCommonIntegration();
      if (EchoRuntimeModules.isLoaded("echomachinecore")) {
         tryInvoke("com.knoxhack.echonexusprotocol.integration.NexusMachineCoreRuntimeProvider");
      }
      return true;
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
