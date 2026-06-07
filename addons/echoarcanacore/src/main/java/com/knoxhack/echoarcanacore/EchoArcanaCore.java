package com.knoxhack.echoarcanacore;

import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echocore.api.EchoAddonChapter;
import com.knoxhack.echocore.api.EchoAddonRegistry;
import com.knoxhack.echoarcanacore.api.ArcanaCoreServices;
import com.knoxhack.echoarcanacore.integration.ArcanaCoreMissionIntegration;
import com.knoxhack.echoarcanacore.service.PersistentAetherService;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import com.knoxhack.echocore.api.EchoRuntimeModules;
import org.slf4j.Logger;

public final class EchoArcanaCore {
    public static final String MODID = "echoarcanacore";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EchoArcanaCore(Object modEventBus) {
        com.knoxhack.echoarcanacore.integration.prime.ArcanaCorePrimeIntegration.register();
        EchoBackendLifecycleBridge.registerModListener(modEventBus, this::commonSetup);
    }

    private void commonSetup(Object event) {
        EchoBackendLifecycleBridge.runCommonSetupWork(event, () -> {
            EchoAddonRegistry.register(new EchoAddonChapter() {
                @Override
                public String id() {
                    return "arcana_core";
                }

                @Override
                public String modId() {
                    return MODID;
                }

                @Override
                public String displayName() {
                    return "ECHO: Arcana Core";
                }

                @Override
                public String summary() {
                    return "Shared Aether Signal, spell, ritual, curse, relic, and bridge contracts.";
                }

                @Override
                public String statusLine(net.minecraft.world.entity.player.Player player) {
                    return "Aether Signal contracts online.";
                }
            });
            ArcanaCoreServices.registerAetherService(PersistentAetherService.INSTANCE);
            ArcanaCoreServices.registerBuiltIns();
            registerOptionalIntegrations();
            if (EchoRuntimeModules.isLoaded("echomissioncore")) {
                ArcanaCoreMissionIntegration.register();
            }
            LOGGER.info("ECHO: Arcana Core online. Reality has an API now.");
        });
    }

    private static void registerOptionalIntegrations() {
        if (EchoRuntimeModules.isLoaded("arcanaveil")) {
            tryInvoke("com.knoxhack.echoarcanacore.integration.veilbound.ArcanaVeilboundBridgeIntegration");
            if (EchoRuntimeModules.isLoaded("echolens")) {
                tryInvoke("com.knoxhack.echoarcanacore.integration.veilbound.ArcanaVeilboundLensIntegration");
            }
        }
    }

    private static void tryInvoke(String className) {
        try {
            Class.forName(className).getMethod("register").invoke(null);
        } catch (ClassNotFoundException exception) {
            LOGGER.debug("Optional Arcana integration {} not present.", className);
        } catch (ReflectiveOperationException | LinkageError exception) {
            LOGGER.warn("Optional Arcana integration {} could not be registered.", className, exception);
        }
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
