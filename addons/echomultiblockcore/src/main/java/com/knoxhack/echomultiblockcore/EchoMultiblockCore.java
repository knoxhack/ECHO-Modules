package com.knoxhack.echomultiblockcore;

import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.echoplatform.echocore.api.EchoAddonChapter;
import com.echoplatform.echocore.api.EchoAddonRegistry;
import com.echoplatform.echocore.api.EchoCoreServices;
import com.echoplatform.echocore.api.EchoRuntimeModules;
import com.knoxhack.echomultiblockcore.api.MultiblockIntegrationServices;
import com.knoxhack.echomultiblockcore.command.MultiblockCommands;
import com.knoxhack.echomultiblockcore.content.MultiblockReloaders;
import com.knoxhack.echomultiblockcore.integration.MultiblockMapDataProvider;
import com.knoxhack.echomultiblockcore.integration.MultiblockIndexProvider;
import com.knoxhack.echomultiblockcore.integration.MultiblockMissionCoreIntegration;
import com.knoxhack.echomultiblockcore.network.ModNetwork;
import com.knoxhack.echomultiblockcore.registry.ModBlockEntities;
import com.knoxhack.echomultiblockcore.registry.ModBlocks;
import com.knoxhack.echomultiblockcore.registry.ModCreativeTabs;
import com.knoxhack.echomultiblockcore.registry.ModDataComponents;
import com.knoxhack.echomultiblockcore.registry.ModItems;
import com.knoxhack.echomultiblockcore.registry.ModMenus;
import com.knoxhack.echomultiblockcore.runtime.MultiblockRuntimeEvents;
import com.mojang.logging.LogUtils;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(EchoMultiblockCore.MODID)
public final class EchoMultiblockCore {
    public static final String MODID = "echomultiblockcore";
    public static final String CHAPTER_ID = "multiblock_core";
    private static final String COMMON_SETUP_EVENT = "net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent";
    private static final String ADD_SERVER_RELOAD_LISTENERS_EVENT =
            "net.neoforged.neoforge.event.AddServerReloadListenersEvent";
    public static final Logger LOGGER = LogUtils.getLogger();
    private static final AtomicBoolean COMMON_SERVICES_REGISTERED = new AtomicBoolean(false);

    EchoMultiblockCore() {
        this(null);
}

    public EchoMultiblockCore(IEventBus modEventBus) {
        var runtime = Agent9MultiblockRuntimeAdapter.activateNativeHostEntrypoint();
        LOGGER.info("ECHO MultiblockCore Agent 9 native host adapter {}.", runtime.get("status"));
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModDataComponents.register(modEventBus);
        ModItems.register(modEventBus);
        ModMenus.register(modEventBus);
        ModCreativeTabs.register(modEventBus);

        Config.registerEchoConfig();

        EchoBackendLifecycleBridge.registerModListener(modEventBus, ModNetwork::registerPayloads);
        EchoBackendLifecycleBridge.registerModListener(modEventBus, COMMON_SETUP_EVENT, this::commonSetup);
        EchoBackendLifecycleBridge.registerGameEventHandler(ADD_SERVER_RELOAD_LISTENERS_EVENT,
                MultiblockReloaders::addServerReloadListeners);
        EchoBackendLifecycleBridge.registerGameEventHandler(MultiblockCommands::register);
        EchoBackendLifecycleBridge.registerGameEventHandler(MultiblockRuntimeEvents::onServerTick);
        EchoBackendLifecycleBridge.registerGameEventHandler(MultiblockRuntimeEvents::onPlayerLoggedIn);
        EchoBackendLifecycleBridge.registerOptionalGameTests(modEventBus,
                "com.knoxhack.echomultiblockcore.registry.ModGameTests");
        com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge.bootstrapClientEntrypoint(modEventBus,
            "com.knoxhack.echomultiblockcore.EchoMultiblockCoreClient");
}

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
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
        registerAddonChapter();
        MultiblockIntegrationServices.registerDefaultProviders();
        EchoCoreServices.registerMapDataProvider(MultiblockMapDataProvider.INSTANCE);
        EchoCoreServices.registerIndexContentProvider(MultiblockIndexProvider.INSTANCE);
        if (EchoRuntimeModules.isLoaded("echomissioncore")) {
            MultiblockMissionCoreIntegration.register();
        }
        if (EchoRuntimeModules.isLoaded("echoterminal")) {
            registerTerminalBridge();
        }
        if (EchoRuntimeModules.isLoaded("echomachinecore")) {
            registerMachineCoreBridge();
        }
        LOGGER.info("ECHO MultiblockCore online [{}]. Facility runtime awaiting controllers.", source);
        return true;
    }

    private static void registerTerminalBridge() {
        try {
            Class.forName("com.knoxhack.echomultiblockcore.integration.terminal.MultiblockTerminalBridge")
                    .getMethod("register")
                    .invoke(null);
        } catch (ReflectiveOperationException | LinkageError exception) {
            LOGGER.warn("ECHO MultiblockCore terminal bridge could not be registered.", exception);
        }
    }

    private static void registerMachineCoreBridge() {
        try {
            Class.forName("com.knoxhack.echomultiblockcore.integration.MultiblockMachineCoreRuntimeProvider")
                    .getMethod("register")
                    .invoke(null);
        } catch (ReflectiveOperationException | LinkageError exception) {
            LOGGER.warn("ECHO MultiblockCore MachineCore bridge could not be registered.", exception);
        }
    }

    private static void registerAddonChapter() {
        if (EchoAddonRegistry.isRegistered(CHAPTER_ID)) {
            return;
        }
        EchoAddonRegistry.register(new EchoAddonChapter() {
            @Override
            public String id() {
                return CHAPTER_ID;
            }

            @Override
            public String modId() {
                return MODID;
            }

            @Override
            public String displayName() {
                return "ECHO: MultiblockCore";
            }

            @Override
            public String summary() {
                return "Shared multiblock, blueprint, robotic automation, and facility runtime framework.";
            }

            @Override
            public String statusLine(Player player) {
                return "MultiblockCore: " + com.knoxhack.echomultiblockcore.content.MultiblockContent.definitions().size()
                        + " definition(s), controllers online through world caches.";
            }
        });
    }
}
