package com.knoxhack.echorelictech.integration.prime;

import com.echoplatform.echocore.api.prime.EchoPrimeIntegration;
import com.echoplatform.echocore.api.prime.EchoPrimeIntegrations;
import com.echoplatform.echocore.api.prime.PrimeHoloMapRegistry;
import com.echoplatform.echocore.api.prime.PrimeIndexRegistry;
import com.echoplatform.echocore.api.prime.PrimeIntegrationContext;
import com.echoplatform.echocore.api.prime.PrimeLensRegistry;
import com.echoplatform.echocore.api.prime.PrimeLootRegistry;
import com.echoplatform.echocore.api.prime.PrimeMissionRegistry;
import com.echoplatform.echocore.api.prime.PrimeRouteRegistry;
import com.echoplatform.echocore.api.prime.PrimeTerminalRegistry;
import com.knoxhack.echorelictech.EchoRelicTech;
import java.util.List;
import net.minecraft.resources.Identifier;

public final class RelicTechPrimeIntegration implements EchoPrimeIntegration {
    private static final RelicTechPrimeIntegration INSTANCE = new RelicTechPrimeIntegration();

    private RelicTechPrimeIntegration() {
    }

    public static void register() {
        EchoPrimeIntegrations.register(INSTANCE);
    }

    @Override
    public Identifier id() {
        return id("prime/relictech");
    }

    @Override
    public boolean available(PrimeIntegrationContext context) {
        return context.moduleLoaded(EchoRelicTech.MODID);
    }

    @Override
    public void registerPrime(PrimeIntegrationContext context) {
        Identifier route = prime("route/relic");
        Identifier unlock = prime("relic_route_open");
        context.routeRegistry().registerRoute(new PrimeRouteRegistry.PrimeRoute(
                route,
                "RelicTech",
                "Relic scans, vault warnings, unstable artifacts, and late survival gambits.",
                unlock,
                List.of(EchoRelicTech.MODID),
                60,
                0xFFFF9F7A));
        context.missionRegistry().registerMissionChain(new PrimeMissionRegistry.PrimeMissionChain(
                id("prime/mission_chain/relic"),
                "Prime Relics",
                "Identify a relic, open a vault, and manage instability before Nexus escalation.",
                route,
                List.of(id("mission/analyze_relic"), id("mission/relic_vault")),
                60));
        context.indexRegistry().registerCategory(new PrimeIndexRegistry.PrimeIndexCategory(
                id("prime/index/relic"),
                "Prime RelicTech",
                "Relic analysis, vault safety, containment, and route warnings.",
                unlock,
                EchoRelicTech.MODID,
                60));
        context.lensRegistry().registerScanData(new PrimeLensRegistry.PrimeScanData(
                id("prime/scan_data/relic_trace"),
                prime("scan/prime_relic"),
                id("relic_trace").toString(),
                "Classifies relic instability, likely function, and containment risk.",
                "variable",
                "Relic fragments, vault components, and unstable artifacts.",
                "Relics are powerful enough to route toward Nexus and dangerous enough to respect.",
                EchoRelicTech.MODID));
        context.holoMapRegistry().registerLayer(new PrimeHoloMapRegistry.PrimeMapLayer(
                id("prime/layer/relic_vaults"),
                "Relic Vaults",
                "Vaults, unstable caches, and relic route markers.",
                0xFFFF9F7A,
                false,
                60));
        context.holoMapRegistry().registerMarkerType(new PrimeHoloMapRegistry.PrimeMarkerType(
                id("prime/marker/relic_vault"),
                id("prime/layer/relic_vaults"),
                "Relic Vault",
                "relic_vault",
                60));
        context.lootRegistry().registerInjection(new PrimeLootRegistry.PrimeLootInjection(
                id("prime/loot/relic_fragment"),
                prime("loot/prime_relic_cache"),
                id("relic_fragment"),
                1,
                2,
                8,
                EchoRelicTech.MODID));
        context.terminalRegistry().registerCard(new PrimeTerminalRegistry.PrimeTerminalCard(
                id("prime/card/relic_warning"),
                route,
                "Relic Warnings",
                "Shows relic instability, vault readiness, containment gaps, and Nexus pressure.",
                unlock,
                EchoRelicTech.MODID,
                60));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoRelicTech.MODID, path);
    }

    private static Identifier prime(String path) {
        return Identifier.fromNamespaceAndPath("echoprimecore", path);
    }
}
