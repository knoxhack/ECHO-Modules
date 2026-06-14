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
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import org.slf4j.Logger;

@Mod(EchoIndex.MODID)
public class EchoIndex {
    public static final String MODID = "echoindex";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EchoIndex(IEventBus modEventBus) {
        Config.registerEchoConfig();
        EchoBackendLifecycleBridge.registerModListener(modEventBus, this::commonSetup);
        EchoBackendLifecycleBridge.registerModListener(modEventBus, ModNetwork::registerPayloads);
        EchoBackendLifecycleBridge.registerGameEventHandler(IndexReloaders::addServerReloadListeners);
        IndexCommands.register();
        IndexEvents.register();
        registerOptionalGameTests(modEventBus, "com.knoxhack.echoindex.test.ModGameTests");

        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            new EchoIndexClient(modEventBus);
        }
    }

    private void commonSetup(Object event) {
        LOGGER.info("ECHO: Index is assembling the shared archive.");
        EchoBackendLifecycleBridge.runCommonSetupWork(event, () -> {
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
        });
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
