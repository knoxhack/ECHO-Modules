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
import com.echoplatform.echocore.api.EchoRuntimeModules;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(EchoOrbitalRemnants.MODID)
public class EchoOrbitalRemnants {
    public static final String MODID = "echoorbitalremnants";
    public static final Logger LOGGER = LogUtils.getLogger();
    private static final String COMMON_SETUP_EVENT =
            "net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent";
    private static final String ENTITY_ATTRIBUTE_CREATION_EVENT =
            "net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent";
    private static final String REGISTER_PAYLOAD_HANDLERS_EVENT =
            "net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent";
    private static final String PLAYER_TICK_POST_EVENT =
            "net.neoforged.neoforge.event.tick.PlayerTickEvent$Post";
    private static final String RIGHT_CLICK_BLOCK_EVENT =
            "net.neoforged.neoforge.event.entity.player.PlayerInteractEvent$RightClickBlock";
    private static final String PLAYER_CLONE_EVENT =
            "net.neoforged.neoforge.event.entity.player.PlayerEvent$Clone";
    private static final String ITEM_TOOLTIP_EVENT =
            "net.neoforged.neoforge.client.event.ItemTooltipEvent";
    private static final String REGISTER_COMMANDS_EVENT =
            "net.neoforged.neoforge.event.RegisterCommandsEvent";

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    EchoOrbitalRemnants() {
        this(null);
    }

    public EchoOrbitalRemnants(IEventBus modEventBus) {
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModEntities.register(modEventBus);
        ModItems.register(modEventBus);
        ModMenus.register(modEventBus);
        ModRecipes.register(modEventBus);
        ModWorldgen.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        com.knoxhack.echoorbitalremnants.integration.prime.OrbitalPrimeIntegration.register();

        EchoBackendLifecycleBridge.registerModListener(modEventBus, COMMON_SETUP_EVENT, this::commonSetup);
        EchoBackendLifecycleBridge.registerModListener(modEventBus, ENTITY_ATTRIBUTE_CREATION_EVENT,
                ModEntities::registerAttributes);
        EchoBackendLifecycleBridge.registerModListener(modEventBus, REGISTER_PAYLOAD_HANDLERS_EVENT,
                ModNetworking::registerPayloads);

        SuitEvents suitEvents = new SuitEvents();
        ModTooltipEvents tooltipEvents = new ModTooltipEvents();
        EchoBackendLifecycleBridge.registerGameEventHandler(PLAYER_TICK_POST_EVENT, suitEvents::onPlayerTick);
        EchoBackendLifecycleBridge.registerGameEventHandler(RIGHT_CLICK_BLOCK_EVENT, suitEvents::onRouteCacheOpen);
        EchoBackendLifecycleBridge.registerGameEventHandler(PLAYER_CLONE_EVENT, suitEvents::onClone);
        EchoBackendLifecycleBridge.registerGameEventHandler(ITEM_TOOLTIP_EVENT, tooltipEvents::onItemTooltip);
        EchoBackendLifecycleBridge.registerGameEventHandler(REGISTER_COMMANDS_EVENT,
                Echo7RouteCommandHandler::onRegisterCommands);
        EchoBackendLifecycleBridge.registerGameEventHandler(PLAYER_TICK_POST_EVENT, OrbitalOutpostSpawner::onPlayerTick);
        EchoBackendLifecycleBridge.registerOptionalGameTests(modEventBus,
                "com.knoxhack.echoorbitalremnants.test.ModGameTests");

        Config.registerEchoConfig();
    }

    private void commonSetup(Object event) {
        LOGGER.info("ECHO-7 orbital systems initialized. Quarantine route chain ready.");
        EchoBackendLifecycleBridge.runCommonSetupWork(event, () -> {
            AshfallCompat.registerAddonChapter();
            OrbitalIndexProvider.register();
            if (ModList.get().isLoaded("echoterminal")) {
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
