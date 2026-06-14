package com.knoxhack.echorecovery;

import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.echoplatform.echocore.api.EchoRuntimeModules;
import com.echoplatform.echocore.api.EchoCoreServices;
import com.knoxhack.echorecovery.command.GravesCommand;
import com.knoxhack.echorecovery.content.RecoveryReloaders;
import com.knoxhack.echorecovery.data.RecoveryWorldData;
import com.knoxhack.echorecovery.grave.DeathHandler;
import com.knoxhack.echorecovery.grave.RecoveryCoreService;
import com.knoxhack.echorecovery.integration.RecoveryIntegrationDispatcher;
import com.knoxhack.echorecovery.net.RecoveryPackets;
import com.knoxhack.echorecovery.registry.ModBlockEntities;
import com.knoxhack.echorecovery.registry.ModBlocks;
import com.knoxhack.echorecovery.registry.ModCreativeTabs;
import com.knoxhack.echorecovery.registry.ModDataComponents;
import com.knoxhack.echorecovery.registry.ModItems;
import com.knoxhack.echorecovery.registry.ModMenus;
import com.knoxhack.echorecovery.registry.ModSounds;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import org.slf4j.Logger;

@Mod(EchoRecovery.MODID)
public class EchoRecovery {
    public static final String MODID = "echorecovery";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static boolean ashfallLoaded = false;

    public EchoRecovery(IEventBus modEventBus) {
        EchoRuntimeModules.markLoaded(MODID, "ECHO Recovery", "");
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModItems.register(modEventBus);
        ModDataComponents.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModMenus.register(modEventBus);
        ModSounds.register(modEventBus);
        RecoveryPackets.register(modEventBus);
        com.knoxhack.echorecovery.integration.prime.RecoveryPrimeIntegration.register();
        EchoBackendLifecycleBridge.registerModListener(modEventBus, this::commonSetup);
        DeathHandler.register();
        EchoBackendLifecycleBridge.registerGameEventHandler(this::registerCommands);
        EchoBackendLifecycleBridge.registerGameEventHandler(RecoveryReloaders::addServerReloadListeners);
        EchoBackendLifecycleBridge.registerGameEventHandler(this::onServerStarted);
        EchoBackendLifecycleBridge.registerGameEventHandler(this::onServerStopping);
        registerOptionalGameTests(modEventBus, "com.knoxhack.echorecovery.test.ModGameTests");
        ashfallLoaded = EchoRuntimeModules.isLoaded("echoashfallprotocol");
    }

    private void commonSetup(Object event) {
        LOGGER.info("ECHO Recovery online. Standalone recovery enabled.");
        EchoBackendLifecycleBridge.runCommonSetupWork(event, () -> {
            EchoCoreServices.registerRecoveryService(RecoveryCoreService.INSTANCE);
            RecoveryIntegrationDispatcher.registerCommon();
        });
    }

    private void registerCommands(Object event) {
        CommandDispatcher<CommandSourceStack> dispatcher = commandDispatcher(event);
        Object buildContext = invoke(event, "getBuildContext");
        if (dispatcher != null && buildContext instanceof CommandBuildContext context) {
            GravesCommand.register(dispatcher, context);
        }
    }

    private void onServerStarted(Object event) {
        if (server(event) instanceof MinecraftServer server) {
            RecoveryWorldData.getOrCreate(server.overworld());
        }
    }

    private void onServerStopping(Object event) {
    }

    private static void registerOptionalGameTests(IEventBus modEventBus, String className) {
        try {
            Class<?> gameTests = Class.forName(className);
            gameTests.getMethod("register", IEventBus.class).invoke(null, modEventBus);
            modEventBus.addListener((RegisterGameTestsEvent event) -> registerOptionalGameTestInstances(gameTests, event));
        } catch (ClassNotFoundException ignored) {
            // Production runtime does not include src/test classes.
        } catch (ReflectiveOperationException | LinkageError exception) {
            LOGGER.warn("ECHO Recovery GameTest registration is unavailable for {}.", className, exception);
        }
    }

    private static void registerOptionalGameTestInstances(Class<?> gameTests, RegisterGameTestsEvent event) {
        try {
            gameTests.getMethod("registerTests", RegisterGameTestsEvent.class).invoke(null, event);
        } catch (ReflectiveOperationException | LinkageError exception) {
            LOGGER.warn("ECHO Recovery GameTest instances could not be registered.", exception);
        }
    }

    public static boolean isAshfallLoaded() {
        return ashfallLoaded;
    }

    public static String displayName() {
        return ashfallLoaded ? "Field Recovery" : "Graves";
    }

    @SuppressWarnings("unchecked")
    private static CommandDispatcher<CommandSourceStack> commandDispatcher(Object event) {
        Object dispatcher = invoke(event, "getDispatcher");
        return dispatcher instanceof CommandDispatcher<?> value
                ? (CommandDispatcher<CommandSourceStack>) value
                : null;
    }

    private static Object server(Object event) {
        return invoke(event, "getServer");
    }

    private static Object invoke(Object target, String methodName) {
        if (target == null) {
            return null;
        }
        try {
            return target.getClass().getMethod(methodName).invoke(target);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return null;
        }
    }
}
