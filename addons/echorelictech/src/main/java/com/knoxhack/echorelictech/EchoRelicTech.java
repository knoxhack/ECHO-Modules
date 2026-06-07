package com.knoxhack.echorelictech;

import com.knoxhack.echo.adaptercore.EchoBackendCommandEventBridge;
import com.knoxhack.echo.adaptercore.EchoBackendLifecycleBridge;
import com.knoxhack.echo.adaptercore.EchoBackendWorldEventBridge;
import com.knoxhack.echorelictech.command.RelicTechCommands;
import com.knoxhack.echorelictech.config.RelicTechConfig;
import com.knoxhack.echorelictech.data.RelicDefinitionLoader;
import com.knoxhack.echorelictech.data.RelicFailureLoader;
import com.knoxhack.echorelictech.data.RelicVaultLoader;
import com.knoxhack.echorelictech.integration.RelicTechIntegrations;
import com.knoxhack.echorelictech.registry.*;
import com.knoxhack.echorelictech.server.RelicInstabilityManager;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import com.knoxhack.echocore.api.EchoRuntimeModules;
import org.slf4j.Logger;

public class EchoRelicTech {
    public static final String MODID = "echorelictech";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EchoRelicTech(Object modEventBus) {
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModItems.register(modEventBus);
        ModDataComponents.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        com.knoxhack.echorelictech.integration.prime.RelicTechPrimeIntegration.register();
        EchoBackendLifecycleBridge.registerModListener(modEventBus, this::commonSetup);
        EchoBackendLifecycleBridge.registerGameEventHandler(this::onAddReloadListeners);
        EchoBackendLifecycleBridge.registerGameEventHandler(this::onServerTick);
        EchoBackendLifecycleBridge.registerGameEventHandler(this::onRegisterCommands);
    }

    private void commonSetup(Object event) {
        LOGGER.info("ECHO: RelicTech online. Powerful enough to be exciting. Dangerous enough to respect.");
        EchoBackendLifecycleBridge.runCommonSetupWork(event, RelicTechIntegrations::registerOptional);
        registerInternalEventHooks();
    }

    private void registerInternalEventHooks() {
        com.knoxhack.echorelictech.api.event.RelicTechEvents.onAnalyze((e) -> {
            com.knoxhack.echorelictech.api.RelicTechApi.recordAnalyzedRelic(e.player());
            if (EchoRuntimeModules.isLoaded("echonexusprotocol")) {
                com.knoxhack.echorelictech.integration.nexus.RelicTechNexusIntegration.recordRelicResearch(
                        e.player(),
                        "echorelictech:analyzed_relic");
            }
        });
        com.knoxhack.echorelictech.api.event.RelicTechEvents.onUse((e) -> {
            com.knoxhack.echorelictech.api.RelicTechApi.recordRelicUse(e.player(), e.relicId());
            if (EchoRuntimeModules.isLoaded("echonexusprotocol")) {
                com.knoxhack.echorelictech.integration.nexus.RelicTechNexusIntegration.recordRelicResearch(
                        e.player(),
                        "echorelictech:used_" + e.relicId().getPath());
            }
        });
        com.knoxhack.echorelictech.api.event.RelicTechEvents.onVaultDiscover((e) -> {
            com.knoxhack.echorelictech.api.RelicTechApi.recordVaultDiscovery(e.player(), e.vaultId(), e.pos());
        });
    }

    private void onAddReloadListeners(Object event) {
        EchoBackendWorldEventBridge.addServerReloadListener(
                event, Identifier.fromNamespaceAndPath(MODID, "relic_definitions"), new RelicDefinitionLoader());
        EchoBackendWorldEventBridge.addServerReloadListener(
                event, Identifier.fromNamespaceAndPath(MODID, "relic_failures"), new RelicFailureLoader());
        EchoBackendWorldEventBridge.addServerReloadListener(
                event, Identifier.fromNamespaceAndPath(MODID, "relic_vaults"), new RelicVaultLoader());
    }

    private void onServerTick(Object event) {
        MinecraftServer server = EchoBackendWorldEventBridge.serverTickServer(event);
        if (server == null) {
            return;
        }
        RelicInstabilityManager.tickDecay(server.overworld());
        tickRelicCooldowns(server);
        com.knoxhack.echorelictech.item.EchoMirrorDecoyTracker.tick(server.overworld());
    }

    private void tickRelicCooldowns(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (!stack.isEmpty() && stack.has(ModDataComponents.RELIC_DATA.get())) {
                    var data = stack.get(ModDataComponents.RELIC_DATA.get());
                    if (data != null && data.cooldownRemaining() > 0) {
                        stack.set(ModDataComponents.RELIC_DATA.get(), data.withCooldown(data.cooldownRemaining() - 1));
                    }
                }
            }
        }
    }

    private void onRegisterCommands(Object event) {
        var dispatcher = EchoBackendCommandEventBridge.dispatcher(event);
        if (dispatcher != null) {
            RelicTechCommands.register(dispatcher,
                    EchoBackendCommandEventBridge.buildContext(event),
                    EchoBackendCommandEventBridge.commandSelection(event));
        }
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
