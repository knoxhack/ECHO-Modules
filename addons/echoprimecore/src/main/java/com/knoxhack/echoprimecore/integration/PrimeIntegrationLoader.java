package com.knoxhack.echoprimecore.integration;

import com.knoxhack.echocore.api.EchoAddonChapter;
import com.knoxhack.echocore.api.EchoAddonRegistry;
import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.prime.EchoPrimeIntegrations;
import com.knoxhack.echoprimecore.EchoPrimeCore;
import net.minecraft.world.entity.player.Player;

public final class PrimeIntegrationLoader {
    private static final PrimeIntegrationRegistry REGISTRY = new PrimeIntegrationRegistry();
    private static boolean loaded;

    private PrimeIntegrationLoader() {
    }

    public static PrimeIntegrationRegistry registry() {
        return REGISTRY;
    }

    public static synchronized void registerAll() {
        if (loaded) {
            return;
        }
        loaded = true;
        EchoAddonRegistry.register(new EchoAddonChapter() {
            @Override
            public String id() {
                return "prime";
            }

            @Override
            public String modId() {
                return EchoPrimeCore.MODID;
            }

            @Override
            public String displayName() {
                return "ECHO: Prime Core";
            }

            @Override
            public String summary() {
                return "Central survival spine and optional ECHO module integration layer.";
            }

            @Override
            public String statusLine(Player player) {
                return "Stable world. Prime routes collecting installed ECHO modules.";
            }
        });
        PrimeBuiltinContent.register(REGISTRY);
        int external = EchoPrimeIntegrations.applyTo(REGISTRY);
        PrimeMissionBridge.register(REGISTRY);
        PrimeIndexBridge.register(REGISTRY);
        PrimeLensBridge.register(REGISTRY);
        PrimeHoloMapBridge.register(REGISTRY);
        PrimeTerminalBridge.register(REGISTRY);
        PrimeAuditService.register(REGISTRY);
        EchoPrimeCore.LOGGER.info("ECHO: Prime collected {} routes, {} mission chains, {} Index hints, {} scan types, {} map layers, {} terminal cards, and {} external integrations. {}",
                REGISTRY.routes().size(),
                REGISTRY.missionChains().size(),
                REGISTRY.recipeHints().size(),
                REGISTRY.scanTypes().size(),
                REGISTRY.layers().size(),
                REGISTRY.cards().size(),
                external,
                EchoCoreServices.platformProviderSummary());
    }
}
