package com.knoxhack.echomissioncore;

import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.EchoRuntimeModules;
import com.knoxhack.echo.adaptercore.EchoBackendCommandEventBridge;
import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echomissioncore.command.MissionCoreCommands;
import com.knoxhack.echomissioncore.content.MissionCoreNativeJsonBootstrap;
import com.knoxhack.echomissioncore.content.MissionCoreReloaders;
import com.knoxhack.echomissioncore.integration.MissionCoreDiagnostics;
import com.knoxhack.echomissioncore.integration.MissionCoreIndexProvider;
import com.knoxhack.echomissioncore.integration.MissionCoreRuntimeSpineConsumer;
import com.knoxhack.echomissioncore.integration.MissionCoreTerminalIntegration;
import com.knoxhack.echomissioncore.integration.MissionCoreWorldCoreConsumer;
import com.knoxhack.echomissioncore.registry.ModAttachments;
import com.knoxhack.echomissioncore.service.MissionCoreService;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import org.slf4j.Logger;

@Mod(EchoMissionCore.MODID)
public final class EchoMissionCore {
    public static final String MODID = "echomissioncore";
    private static final String COMMON_SETUP_EVENT = "net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent";
    private static final String REGISTER_COMMANDS_EVENT = "net.neoforged.neoforge.event.RegisterCommandsEvent";
    private static final String ADD_RELOAD_LISTENERS_EVENT = "net.neoforged.neoforge.event.AddServerReloadListenersEvent";
    private static final AtomicBoolean COMMON_SERVICES_REGISTERED = new AtomicBoolean(false);
    public static final Logger LOGGER = LogUtils.getLogger();

    public EchoMissionCore(IEventBus modEventBus) {
        ModAttachments.register();
        EchoBackendLifecycleBridge.registerModListener(modEventBus, COMMON_SETUP_EVENT, this::commonSetup);
        EchoBackendLifecycleBridge.registerGameEventHandler(REGISTER_COMMANDS_EVENT, this::registerCommands);
        EchoBackendLifecycleBridge.registerGameEventHandler(
                ADD_RELOAD_LISTENERS_EVENT,
                MissionCoreReloaders::addServerReloadListeners);
        registerOptionalGameTests(modEventBus, "com.knoxhack.echomissioncore.test.ModGameTests");
    }

    public void commonSetup(Object event) {
        EchoBackendLifecycleBridge.runCommonSetupWork(event, () -> registerCommonServices("neoforge_common_setup"));
    }

    public static boolean ensureCommonServicesRegisteredForNativeLoader() {
        boolean registered = registerCommonServices("native_loader_module_ready");
        if (!registered) {
            registerOptionalIntegrations("native_loader_module_ready_retry");
        }
        return registered;
    }

    private static boolean registerCommonServices(String source) {
        if (!COMMON_SERVICES_REGISTERED.compareAndSet(false, true)) {
            return false;
        }
        EchoCoreServices.registerMissionService(MissionCoreService.INSTANCE);
        MissionCoreService.INSTANCE.registerBuiltInContent();
        EchoCoreServices.registerDiagnosticService(MissionCoreDiagnostics.INSTANCE);
        EchoCoreServices.registerIndexContentProvider(MissionCoreIndexProvider.INSTANCE);
        MissionCoreRuntimeSpineConsumer.register();
        registerOptionalIntegrations(source);
        MissionCoreNativeJsonBootstrap.startBackgroundLoad(source);
        int missionCount = MissionCoreService.INSTANCE.missionDefinitions().size();
        if (missionCount == 0 && !EchoCoreServices.itemStackComponentsBound()) {
            LOGGER.info("ECHO: MissionCore online [{}]; built-in mission content deferred until item components are bound.",
                    source);
        } else {
            LOGGER.info("ECHO: MissionCore online [{}] with {} missions.", source, missionCount);
        }
        return true;
    }

    private static void registerOptionalIntegrations(String source) {
        if (EchoRuntimeModules.isLoaded("echoworldcore")) {
            MissionCoreWorldCoreConsumer.register();
        }
        if (EchoRuntimeModules.isLoaded("echoterminal") || classPresent("com.knoxhack.echoterminal.api.mission.TerminalMissionRegistry")) {
            try {
                MissionCoreTerminalIntegration.register();
            } catch (RuntimeException | LinkageError exception) {
                LOGGER.warn("ECHO: MissionCore Terminal integration could not be registered [{}].", source, exception);
            }
        }
    }

    private static boolean classPresent(String className) {
        try {
            Class.forName(className, false, EchoMissionCore.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError exception) {
            return false;
        }
    }

    private void registerCommands(Object event) {
        var dispatcher = EchoBackendCommandEventBridge.dispatcher(event);
        if (dispatcher != null) {
            registerCommands(
                    dispatcher,
                    EchoBackendCommandEventBridge.buildContext(event),
                    EchoBackendCommandEventBridge.commandSelection(event));
        }
    }

    public void registerCommands(
            com.mojang.brigadier.CommandDispatcher<CommandSourceStack> dispatcher,
            CommandBuildContext buildContext,
            Commands.CommandSelection selection) {
        MissionCoreCommands.register(dispatcher, buildContext, selection);
    }

    public List<MissionCoreReloaders.NativeReloadListenerRegistration> reloadListeners() {
        return MissionCoreReloaders.reloadListeners();
    }

    private static void registerOptionalGameTests(IEventBus modEventBus, String className) {
        try {
            Class<?> gameTests = Class.forName(className);
            gameTests.getMethod("register", IEventBus.class).invoke(null, modEventBus);
            modEventBus.addListener((RegisterGameTestsEvent event) -> registerOptionalGameTestInstances(gameTests, event));
        } catch (ClassNotFoundException ignored) {
            // Production runtime does not include src/test classes.
        } catch (ReflectiveOperationException | LinkageError exception) {
            LOGGER.warn("ECHO: MissionCore GameTest registration is unavailable for {}.", className, exception);
        }
    }

    private static void registerOptionalGameTestInstances(Class<?> gameTests, RegisterGameTestsEvent event) {
        try {
            gameTests.getMethod("registerTests", RegisterGameTestsEvent.class).invoke(null, event);
        } catch (ReflectiveOperationException | LinkageError exception) {
            LOGGER.warn("ECHO: MissionCore GameTest instances could not be registered.", exception);
        }
    }
}
