package com.knoxhack.echodeepreachprotocol;

import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echo.hazardcore.api.HazardService;
import com.knoxhack.echodeepreachprotocol.hazard.DeepReachCorruptionSource;
import com.knoxhack.echodeepreachprotocol.hazard.DeepReachOxygenSource;
import com.knoxhack.echodeepreachprotocol.hazard.DeepReachPressureSource;
import com.knoxhack.echodeepreachprotocol.hazard.DeepReachThermalSource;
import com.knoxhack.echodeepreachprotocol.season.DeepReachSeasonManager;
import com.knoxhack.echodeepreachprotocol.command.DeepReachCommands;
import com.knoxhack.echodeepreachprotocol.registry.ModBiomes;
import com.knoxhack.echodeepreachprotocol.registry.ModBlocks;
import com.knoxhack.echodeepreachprotocol.registry.ModEntities;
import com.knoxhack.echodeepreachprotocol.registry.ModItems;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import org.slf4j.Logger;

/**
 * ECHO: Deep Reach Protocol
 * A pressure-suit survival overhaul set in the flooded caverns and abyssal ruins
 * beneath the ECHO world. Depth, oxygen, pressure, and suit integrity drive the loop.
 */
@Mod(EchoDeepReachProtocol.MODID)
public class EchoDeepReachProtocol {
    public static final String MODID = "echodeepreachprotocol";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final String COMMON_SETUP_EVENT = "net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent";
    private static final String ENTITY_ATTRIBUTE_CREATION_EVENT = "net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent";
    private static final String REGISTER_SPAWN_PLACEMENTS_EVENT = "net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent";
    private static final String REGISTER_COMMANDS_EVENT = "net.neoforged.neoforge.event.RegisterCommandsEvent";

    private EchoDeepReachProtocol() {
        this(null);
    }

    public EchoDeepReachProtocol(IEventBus modEventBus) {
        registerDeferredContent(modEventBus);
        registerOptionalGameTests(modEventBus, "com.knoxhack.echodeepreachprotocol.test.ModGameTests");
        EchoBackendLifecycleBridge.registerModListener(modEventBus, COMMON_SETUP_EVENT, this::commonSetup);
        EchoBackendLifecycleBridge.registerModListener(modEventBus, ENTITY_ATTRIBUTE_CREATION_EVENT, ModEntities::registerAttributes);
        EchoBackendLifecycleBridge.registerModListener(modEventBus, REGISTER_SPAWN_PLACEMENTS_EVENT, ModEntities::registerSpawnPlacements);
        EchoBackendLifecycleBridge.registerGameEventHandler(
                "net.neoforged.neoforge.event.tick.ServerTickEvent$Post",
                (Object event) -> DeepReachSeasonManager.INSTANCE.tick(extractServer(event)));
        EchoBackendLifecycleBridge.registerGameEventHandler(
                REGISTER_COMMANDS_EVENT,
                DeepReachCommands::register);
        registerOptionalClient(modEventBus);
    }

    private void registerDeferredContent(Object modEventBus) {
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModBiomes.register(modEventBus);
        ModEntities.register(modEventBus);
    }

    private void commonSetup(Object event) {
        EchoBackendLifecycleBridge.runCommonSetupWork(event, () -> {
            LOGGER.info("=== ECHO: Deep Reach Protocol ===");
            LOGGER.info("Pressure-suit survival pack root online.");
            LOGGER.info("Depth zones, suit integrity, habitats, and expedition routes scaffolded.");

            HazardService hazardService = HazardService.find();
            hazardService.registerSource(DeepReachPressureSource.INSTANCE);
            hazardService.registerSource(DeepReachOxygenSource.INSTANCE);
            hazardService.registerSource(DeepReachThermalSource.INSTANCE);
            hazardService.registerSource(DeepReachCorruptionSource.INSTANCE);
            LOGGER.info("Registered Deep Reach hazard sources.");
        });
    }

    private static void registerOptionalGameTests(IEventBus modEventBus, String className) {
        try {
            Class<?> gameTests = Class.forName(className);
            gameTests.getMethod("register", IEventBus.class).invoke(null, modEventBus);
            modEventBus.addListener((RegisterGameTestsEvent event) -> registerOptionalGameTestInstances(gameTests, event));
        } catch (ClassNotFoundException ignored) {
            // Production runtime does not include src/test classes.
        } catch (ReflectiveOperationException | LinkageError exception) {
            LOGGER.warn("Deep Reach GameTest registration is unavailable for {}.", className, exception);
        }
    }

    private static void registerOptionalGameTestInstances(Class<?> gameTests, RegisterGameTestsEvent event) {
        try {
            gameTests.getMethod("registerTests", RegisterGameTestsEvent.class).invoke(null, event);
        } catch (ReflectiveOperationException | LinkageError exception) {
            LOGGER.warn("Deep Reach GameTest instances could not be registered.", exception);
        }
    }

    private static net.minecraft.server.MinecraftServer extractServer(Object event) {
        try {
            return (net.minecraft.server.MinecraftServer) event.getClass().getMethod("getServer").invoke(event);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return null;
        }
    }

    private static void registerOptionalClient(IEventBus modEventBus) {
        try {
            Class<?> clientClass = Class.forName("com.knoxhack.echodeepreachprotocol.client.EchoDeepReachClient");
            clientClass.getMethod("register", IEventBus.class).invoke(null, modEventBus);
            LOGGER.debug("Deep Reach client-side registrations loaded.");
        } catch (ClassNotFoundException ignored) {
            // Dedicated server; client classes are not present.
        } catch (ReflectiveOperationException | LinkageError exception) {
            LOGGER.warn("Deep Reach client-side registrations could not be loaded.", exception);
        }
    }
}
