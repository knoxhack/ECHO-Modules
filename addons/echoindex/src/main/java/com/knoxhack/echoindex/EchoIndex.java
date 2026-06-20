package com.knoxhack.echoindex;

import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.echoplatform.echocore.api.EchoAddonChapter;
import com.echoplatform.echocore.api.EchoAddonRegistry;
import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.EchoRuntimeModules;
import com.knoxhack.echoindex.command.IndexCommands;
import com.knoxhack.echoindex.content.IndexReloaders;
import com.knoxhack.echoindex.event.IndexEvents;
import com.knoxhack.echoindex.integration.IndexMissionCoreIntegration;
import com.knoxhack.echoindex.network.ModNetwork;
import com.knoxhack.echoindex.service.BuiltinIndexProvider;
import com.knoxhack.echoindex.service.IndexService;
import com.knoxhack.echoindex.service.IndexSourceRecipeProvider;
import com.knoxhack.echoindex.service.VanillaIndexRecipeProvider;
import com.mojang.logging.LogUtils;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import org.slf4j.Logger;

@Mod(EchoIndex.MODID)
public class EchoIndex {
    public static final String MODID = "echoindex";
    public static final Logger LOGGER = LogUtils.getLogger();
    private static final AtomicBoolean COMMON_SERVICES_REGISTERED = new AtomicBoolean(false);
    private static final String REGISTER_PAYLOAD_HANDLERS_EVENT =
            "net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent";

    public EchoIndex(IEventBus modEventBus) {
        Config.registerEchoConfig();
        EchoBackendLifecycleBridge.registerModListener(modEventBus, this::commonSetup);
        EchoBackendLifecycleBridge.registerModListener(modEventBus, REGISTER_PAYLOAD_HANDLERS_EVENT,
                ModNetwork::registerPayloads);
        EchoBackendLifecycleBridge.registerGameEventHandler(IndexReloaders::addServerReloadListeners);
        IndexCommands.register();
        IndexEvents.register();
        registerOptionalGameTests(modEventBus, "com.knoxhack.echoindex.test.ModGameTests");

        EchoBackendLifecycleBridge.bootstrapClientEntrypoint(modEventBus,
                "com.knoxhack.echoindex.EchoIndexClient");
    }

    private void commonSetup(Object event) {
        EchoBackendLifecycleBridge.runCommonSetupWork(event, () -> registerCommonServices("neoforge_common_setup"));
    }

    public static boolean ensureCommonServicesRegisteredForNativeLoader() {
        return ensureCommonServicesRegisteredForNativeLoader("native_loader_module_ready");
    }

    public static boolean ensureCommonServicesRegisteredForNativeLoader(String source) {
        if (!EchoCoreServices.itemStackComponentsBound()) {
            LOGGER.info("ECHO: Index common services deferred [{}]; item stack components are not bound yet.",
                    source);
            return false;
        }
        return registerCommonServices(source);
    }

    public static boolean commonServicesRegistered() {
        return COMMON_SERVICES_REGISTERED.get();
    }

    private static boolean registerCommonServices(String source) {
        if (!COMMON_SERVICES_REGISTERED.compareAndSet(false, true)) {
            return false;
        }
        try {
            LOGGER.info("ECHO: Index is assembling the shared archive [{}].", source);
            EchoAddonRegistry.register(new EchoAddonChapter() {
                @Override
                public String id() {
                    return "index";
                }

                @Override
                public String modId() {
                    return MODID;
                }

                @Override
                public String displayName() {
                    return "ECHO: Index";
                }

                @Override
                public String summary() {
                    return "Shared item, recipe, usage, and archive browser.";
                }

                @Override
                public String statusLine(net.minecraft.world.entity.player.Player player) {
                    return "Index online.";
                }
            });
            EchoCoreServices.registerIndexService(IndexService.INSTANCE);
            EchoCoreServices.registerIndexContentProvider(BuiltinIndexProvider.INSTANCE);
            EchoCoreServices.registerIndexRecipeProvider(VanillaIndexRecipeProvider.INSTANCE);
            EchoCoreServices.registerIndexRecipeProvider(IndexSourceRecipeProvider.INSTANCE);
            if (EchoRuntimeModules.isLoaded("echomissioncore")) {
                IndexMissionCoreIntegration.register();
            }
            if (EchoRuntimeModules.isLoaded("echomachinecore")) {
                registerMachineCoreIntegration();
            }
            if (EchoRuntimeModules.isLoaded("echoterminal")) {
                registerTerminalIntegration();
            }
            return true;
        } catch (RuntimeException | LinkageError exception) {
            COMMON_SERVICES_REGISTERED.set(false);
            LOGGER.warn("ECHO: Index common services could not register [{}]; will retry when components are bound.",
                    source,
                    exception);
            return false;
        }
    }

    private static void registerMachineCoreIntegration() {
        try {
            Class.forName("com.knoxhack.echoindex.integration.MachineCoreIndexIntegration")
                    .getMethod("register")
                    .invoke(null);
        } catch (ReflectiveOperationException | LinkageError exception) {
            LOGGER.warn("ECHO: Index MachineCore integration could not be registered.", exception);
        }
    }

    private static void registerTerminalIntegration() {
        try {
            Class.forName("com.knoxhack.echoindex.integration.IndexTerminalCommonIntegration")
                    .getMethod("register")
                    .invoke(null);
        } catch (ReflectiveOperationException | LinkageError exception) {
            LOGGER.warn("ECHO: Index terminal integration could not be registered.", exception);
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
            LOGGER.warn("ECHO: Index GameTest registration is unavailable for {}.", className, exception);
        }
    }

    private static void registerOptionalGameTestInstances(Class<?> gameTests, RegisterGameTestsEvent event) {
        try {
            gameTests.getMethod("registerTests", RegisterGameTestsEvent.class).invoke(null, event);
        } catch (ReflectiveOperationException | LinkageError exception) {
            LOGGER.warn("ECHO: Index GameTest instances could not be registered.", exception);
        }
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
