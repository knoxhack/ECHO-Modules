package com.knoxhack.echo.npcore.integration.prime;

import com.knoxhack.echo.npcore.EchoNpcCore;
import com.echoplatform.echocore.api.prime.EchoPrimeIntegration;
import com.echoplatform.echocore.api.prime.EchoPrimeIntegrations;
import com.echoplatform.echocore.api.prime.PrimeIndexRegistry;
import com.echoplatform.echocore.api.prime.PrimeIntegrationContext;
import com.echoplatform.echocore.api.prime.PrimeMissionRegistry;
import com.echoplatform.echocore.api.prime.PrimeRouteRegistry;
import com.echoplatform.echocore.api.prime.PrimeTerminalRegistry;
import java.util.List;
import net.minecraft.resources.Identifier;

public final class NpcCorePrimeIntegration implements EchoPrimeIntegration {
    private static final NpcCorePrimeIntegration INSTANCE = new NpcCorePrimeIntegration();

    private NpcCorePrimeIntegration() {
    }

    public static void register() {
        EchoPrimeIntegrations.register(INSTANCE);
    }

    @Override
    public Identifier id() {
        return id("prime/npcore");
    }

    @Override
    public boolean available(PrimeIntegrationContext context) {
        return context.moduleLoaded(EchoNpcCore.MODID);
    }

    @Override
    public void registerPrime(PrimeIntegrationContext context) {
        Identifier route = prime("route/npc");
        context.routeRegistry().registerRoute(new PrimeRouteRegistry.PrimeRoute(
                route,
                "NPC Network",
                "Prime mission givers, traders, dialogue providers, and route-specific roles.",
                prime("started"),
                List.of(EchoNpcCore.MODID),
                25,
                0xFFE6C47A));
        context.missionRegistry().registerMissionChain(new PrimeMissionRegistry.PrimeMissionChain(
                id("prime/mission_chain/npc"),
                "Prime Contacts",
                "Meet Prime-aligned traders and mission contacts as routes come online.",
                route,
                List.of(id("mission/first_contact"), id("mission/route_contact")),
                25));
        context.indexRegistry().registerCategory(new PrimeIndexRegistry.PrimeIndexCategory(
                id("prime/index/npc"),
                "Prime Contacts",
                "Trader, role, dialogue, and mission-giver hints for Prime routes.",
                prime("started"),
                EchoNpcCore.MODID,
                25));
        context.terminalRegistry().registerCard(new PrimeTerminalRegistry.PrimeTerminalCard(
                id("prime/card/contacts"),
                route,
                "Prime Contacts",
                "Shows route-aware NPC roles, traders, and available mission givers.",
                prime("started"),
                EchoNpcCore.MODID,
                25));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoNpcCore.MODID, path);
    }

    private static Identifier prime(String path) {
        return Identifier.fromNamespaceAndPath("echoprimecore", path);
    }
}
