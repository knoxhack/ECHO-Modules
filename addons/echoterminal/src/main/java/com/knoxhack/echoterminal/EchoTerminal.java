package com.knoxhack.echoterminal;

import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.EchoRuntimeModules;
import com.knoxhack.echoterminal.api.mission.TerminalMissionActions;
import com.knoxhack.echoterminal.api.mission.TerminalMissionRegistry;
import com.knoxhack.echoterminal.mission.MainSurvivalQuestProvider;
import com.knoxhack.echoterminal.mission.VanillaJourneyProgression;
import com.knoxhack.echoterminal.mission.VanillaJourneyProvider;
import com.knoxhack.echoterminal.network.ModNetwork;
import com.knoxhack.echoterminal.registry.ModBlockEntities;
import com.knoxhack.echoterminal.registry.ModBlocks;
import com.knoxhack.echoterminal.registry.ModAttachments;
import com.knoxhack.echoterminal.registry.ModCreativeTabs;
import com.knoxhack.echoterminal.registry.ModItems;
import com.knoxhack.echoterminal.registry.ModMenus;
import com.knoxhack.echoterminal.service.EchoTerminalCoreServices;
import com.mojang.logging.LogUtils;
import java.util.concurrent.atomic.AtomicBoolean;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import org.slf4j.Logger;

@Mod(EchoTerminal.MODID)
public class EchoTerminal {
    public static final String MODID = "echoterminal";
    private static final String COMMON_SETUP_EVENT = "net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent";
    private static final String REGISTER_PAYLOAD_HANDLERS_EVENT =
            "net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent";
    private static final AtomicBoolean COMMON_SERVICES_REGISTERED = new AtomicBoolean(false);
    public static final Logger LOGGER = LogUtils.getLogger();

    public EchoTerminal(IEventBus modEventBus) {
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModAttachments.register(modEventBus);
        ModMenus.register(modEventBus);
        ModCreativeTabs.register(modEventBus);

        EchoBackendLifecycleBridge.registerModListener(modEventBus, REGISTER_PAYLOAD_HANDLERS_EVENT,
                ModNetwork::registerPayloads);
        EchoBackendLifecycleBridge.registerModListener(modEventBus, COMMON_SETUP_EVENT, this::commonSetup);
        VanillaJourneyProgression.register();
        registerOptionalGameTests(modEventBus, "com.knoxhack.echoterminal.test.ModGameTests");

        EchoBackendLifecycleBridge.bootstrapClientEntrypoint(modEventBus,
                "com.knoxhack.echoterminal.EchoTerminalClient");
    }

    private void commonSetup(Object event) {
        EchoBackendLifecycleBridge.runCommonSetupWork(event, () -> registerCommonServices("neoforge_common_setup"));
        LOGGER.info("ECHO: Terminal modular shell online.");
    }

    public static boolean ensureCommonServicesRegisteredForNativeLoader() {
        return registerCommonServices("native_loader_module_ready");
    }

    private static boolean registerCommonServices(String source) {
        if (!COMMON_SERVICES_REGISTERED.compareAndSet(false, true)) {
            return false;
        }
        EchoTerminalCoreServices.register();
        BuiltinTerminalCommonIntegration.register();
        if (EchoRuntimeModules.isLoaded("echomachinecore")) {
            registerMachineCoreIntegration();
        }
        TerminalMissionRegistry.registerIfAbsent(MainSurvivalQuestProvider.INSTANCE);
        TerminalMissionRegistry.registerIfAbsent(VanillaJourneyProvider.INSTANCE);
        TerminalMissionActions.registerForTab(MainSurvivalQuestProvider.TAB_ID);
        TerminalMissionActions.registerForTab(VanillaJourneyProvider.TAB_ID);
        registerMissionCoreIntegration();
        LOGGER.info("ECHO platform providers after Terminal setup [{}]: {}",
                source, EchoCoreServices.platformProviderSummary());
        return true;
    }

    private static void registerMachineCoreIntegration() {
        try {
            Class.forName("com.knoxhack.echoterminal.integration.MachineCoreTerminalIntegration")
                    .getMethod("register")
                    .invoke(null);
        } catch (ReflectiveOperationException | LinkageError exception) {
            LOGGER.warn("ECHO: Terminal MachineCore integration could not be registered.", exception);
        }
    }

    private static void registerMissionCoreIntegration() {
        try {
            Class.forName("com.knoxhack.echomissioncore.integration.MissionCoreTerminalIntegration")
                    .getMethod("register")
                    .invoke(null);
        } catch (ClassNotFoundException ignored) {
            // MissionCore is optional for non-ECHO packs.
        } catch (ReflectiveOperationException | LinkageError exception) {
            LOGGER.warn("ECHO: Terminal MissionCore integration could not be registered.", exception);
        }
    }

    private static void registerOptionalGameTests(IEventBus modEventBus, String className) {
        try {
            Class<?> gameTests = Class.forName(className);
            gameTests.getMethod("register", IEventBus.class).invoke(null, modEventBus);
            modEventBus.addListener((RegisterGameTestsEvent event) -> registerOptionalGameTestInstances(gameTests, event));
        } catch (ClassNotFoundException ignored) {
            // Production runtime does not include src/test classes.
        } catch (ReflectiveOperationException | LinkageError exception) {
            LOGGER.warn("ECHO: Terminal GameTest registration is unavailable for {}.", className, exception);
        }
    }

    private static void registerOptionalGameTestInstances(Class<?> gameTests, RegisterGameTestsEvent event) {
        try {
            gameTests.getMethod("registerTests", RegisterGameTestsEvent.class).invoke(null, event);
        } catch (ReflectiveOperationException | LinkageError exception) {
            LOGGER.warn("ECHO: Terminal GameTest instances could not be registered.", exception);
        }
    }
}
