package com.knoxhack.echoorbitalremnants;

import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echoorbitalremnants.event.Echo7RouteCommandHandler;
import com.knoxhack.echoorbitalremnants.faction.OrbitalOutpostSpawner;
import com.knoxhack.echoorbitalremnants.registry.ModBlocks;
import com.knoxhack.echoorbitalremnants.registry.ModBlockEntities;
import com.knoxhack.echoorbitalremnants.registry.ModCreativeTabs;
import com.knoxhack.echoorbitalremnants.registry.ModEntities;
import com.knoxhack.echoorbitalremnants.registry.ModItems;
import com.knoxhack.echoorbitalremnants.registry.ModMenus;
import com.knoxhack.echoorbitalremnants.registry.ModRecipes;
import com.knoxhack.echoorbitalremnants.registry.ModWorldgen;
import com.knoxhack.echoorbitalremnants.integration.AshfallCompat;
import com.knoxhack.echoorbitalremnants.integration.OrbitalIndexProvider;
import com.knoxhack.echoorbitalremnants.integration.OrbitalTerminalCommonIntegration;
import com.knoxhack.echoorbitalremnants.network.ModNetworking;
import com.knoxhack.echoorbitalremnants.item.ModTooltipEvents;
import com.knoxhack.echoorbitalremnants.suit.SuitEvents;
import com.knoxhack.echocore.api.EchoRuntimeModules;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

public class EchoOrbitalRemnants {
    public static final String MODID = "echoorbitalremnants";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    public EchoOrbitalRemnants(Object modEventBus) {
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModEntities.register(modEventBus);
        ModItems.register(modEventBus);
        ModMenus.register(modEventBus);
        ModRecipes.register(modEventBus);
        ModWorldgen.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        com.knoxhack.echoorbitalremnants.integration.prime.OrbitalPrimeIntegration.register();

        EchoBackendLifecycleBridge.registerModListener(modEventBus, this::commonSetup);
        EchoBackendLifecycleBridge.registerModListener(modEventBus, ModEntities::registerAttributes);
        EchoBackendLifecycleBridge.registerModListener(modEventBus, ModNetworking::registerPayloads);

        SuitEvents suitEvents = new SuitEvents();
        ModTooltipEvents tooltipEvents = new ModTooltipEvents();
        EchoBackendLifecycleBridge.registerGameEventHandler(suitEvents::onPlayerTick);
        EchoBackendLifecycleBridge.registerGameEventHandler(suitEvents::onRouteCacheOpen);
        EchoBackendLifecycleBridge.registerGameEventHandler(suitEvents::onClone);
        EchoBackendLifecycleBridge.registerGameEventHandler(tooltipEvents::onItemTooltip);
        EchoBackendLifecycleBridge.registerGameEventHandler(Echo7RouteCommandHandler::onRegisterCommands);
        EchoBackendLifecycleBridge.registerGameEventHandler(OrbitalOutpostSpawner::onPlayerTick);

        Config.registerEchoConfig();
    }

    private void commonSetup(Object event) {
        LOGGER.info("ECHO-7 orbital systems initialized. Quarantine route chain ready.");
        EchoBackendLifecycleBridge.runCommonSetupWork(event, () -> {
            AshfallCompat.registerAddonChapter();
            OrbitalIndexProvider.register();
            if (EchoRuntimeModules.isLoaded("echoterminal")) {
                OrbitalTerminalCommonIntegration.register();
            }
            if (EchoRuntimeModules.isLoaded("echomachinecore")) {
                tryInvoke("com.knoxhack.echoorbitalremnants.integration.OrbitalMachineCoreRuntimeProvider");
            }
        });
    }

    private static void tryInvoke(String className) {
        try {
            Class.forName(className).getMethod("register").invoke(null);
        } catch (ClassNotFoundException e) {
            LOGGER.debug("Optional integration {} not present.", className);
        } catch (ReflectiveOperationException | LinkageError e) {
            LOGGER.warn("Optional integration {} could not be registered.", className, e);
        }
    }
}
