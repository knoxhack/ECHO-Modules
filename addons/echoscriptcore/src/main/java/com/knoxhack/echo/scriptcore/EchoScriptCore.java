package com.knoxhack.echo.scriptcore;

import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echo.adaptercore.EchoBackendWorldEventBridge;
import com.knoxhack.echo.scriptcore.adapter.EchoScriptAdapterRegistry;
import com.knoxhack.echo.scriptcore.command.EchoScriptCommands;
import com.knoxhack.echo.scriptcore.config.ScriptCoreConfig;
import com.knoxhack.echo.scriptcore.loader.EchoScriptReloadListener;
import com.knoxhack.echo.scriptcore.loader.EchoScriptReloader;
import com.echoplatform.echocore.api.EchoAddonChapter;
import com.echoplatform.echocore.api.EchoAddonRegistry;
import com.echoplatform.echocore.api.EchoRuntimeModules;
import com.knoxhack.echocore.command.EchoCommandRegistry;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

public final class EchoScriptCore {
    public static final String MODID = "echoscriptcore";
    public static final String BRANDING = "ScriptCore by ECHO Labs";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EchoScriptCore(Object modEventBus) {
        ScriptCoreConfig.registerEchoConfig();
        EchoBackendLifecycleBridge.registerModListener(modEventBus, this::commonSetup);
        registerNetworkBridgeIfAvailable(modEventBus);
        EchoCommandRegistry.register(EchoScriptCommands.root());
        EchoBackendLifecycleBridge.registerGameEventHandler(this::onAddReloadListeners);
        EchoBackendLifecycleBridge.registerGameEventHandler(this::onServerStarted);
        com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge.bootstrapClientEntrypoint(modEventBus,
                "com.knoxhack.echo.scriptcore.client.EchoScriptCoreClient");
}

    private void commonSetup(Object event) {
        EchoBackendLifecycleBridge.runCommonSetupWork(event, () -> {
            EchoScriptAdapterRegistry.INSTANCE.registerDefaults();
            registerAddonChapter();
            LOGGER.info("ECHO: ScriptCore online. JSON-first campaign authoring framework ready.");
        });
    }

    private void onAddReloadListeners(Object event) {
        EchoBackendWorldEventBridge.addServerReloadListener(event, id("scripts"), new EchoScriptReloadListener());
    }

    private void onServerStarted(Object event) {
        if (EchoBackendWorldEventBridge.serverStartedServer(event) != null) {
            EchoScriptReloader.INSTANCE.reloadAll();
        }
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    private static void registerAddonChapter() {
        if (EchoAddonRegistry.isRegistered("scriptcore")) {
            return;
        }
        EchoAddonRegistry.register(new EchoAddonChapter() {
            @Override
            public String id() {
                return "scriptcore";
            }

            @Override
            public String modId() {
                return MODID;
            }

            @Override
            public String displayName() {
                return "ECHO: ScriptCore";
            }

            @Override
            public String summary() {
                return "JSON-first modpack campaign authoring framework.";
            }

            @Override
            public String statusLine(Player player) {
                return "ScriptCore: " + com.knoxhack.echo.scriptcore.registry.EchoScriptRegistry.INSTANCE.all().size()
                        + " definition(s), packs "
                        + com.knoxhack.echo.scriptcore.registry.EchoScriptRegistry.INSTANCE.countByPack().keySet();
            }
        });
    }

    private static void registerNetworkBridgeIfAvailable(Object modEventBus) {
        if (!EchoRuntimeModules.isLoaded("echonetcore") || !EchoRuntimeModules.isLoaded("echoscreencore")) {
            return;
        }
        try {
            Class.forName("com.knoxhack.echo.scriptcore.network.ScriptCoreNetworkBridge")
                    .getMethod("register", Object.class)
                    .invoke(null, modEventBus);
        } catch (ReflectiveOperationException | LinkageError exception) {
            LOGGER.warn("ECHO: ScriptCore NetCore bridge could not be registered.", exception);
        }
    }
}
