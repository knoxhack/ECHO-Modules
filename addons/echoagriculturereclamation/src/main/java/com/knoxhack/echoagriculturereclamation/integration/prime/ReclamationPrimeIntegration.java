package com.knoxhack.echoagriculturereclamation.integration.prime;

import com.knoxhack.echoagriculturereclamation.EchoAgricultureReclamation;
import com.echoplatform.echocore.api.prime.EchoPrimeIntegration;
import com.echoplatform.echocore.api.prime.EchoPrimeIntegrations;
import com.echoplatform.echocore.api.prime.PrimeIndexRegistry;
import com.echoplatform.echocore.api.prime.PrimeIntegrationContext;
import com.echoplatform.echocore.api.prime.PrimeLensRegistry;
import com.echoplatform.echocore.api.prime.PrimeMissionRegistry;
import com.echoplatform.echocore.api.prime.PrimeRouteRegistry;
import com.echoplatform.echocore.api.prime.PrimeTerminalRegistry;
import java.util.List;
import net.minecraft.resources.Identifier;

public final class ReclamationPrimeIntegration implements EchoPrimeIntegration {
    private static final ReclamationPrimeIntegration INSTANCE = new ReclamationPrimeIntegration();

    private ReclamationPrimeIntegration() {
    }

    public static void register() {
        EchoPrimeIntegrations.register(INSTANCE);
    }

    @Override
    public Identifier id() {
        return id("prime/agriculture");
    }

    @Override
    public boolean available(PrimeIntegrationContext context) {
        return context.moduleLoaded(EchoAgricultureReclamation.MODID);
    }

    @Override
    public void registerPrime(PrimeIntegrationContext context) {
        Identifier route = prime("route/agriculture");
        Identifier unlock = prime("agriculture_route_open");
        context.routeRegistry().registerRoute(new PrimeRouteRegistry.PrimeRoute(
                route,
                "Agriculture",
                "Soil scans, crop recovery, food security, irrigation, and route-safe farms.",
                unlock,
                List.of(EchoAgricultureReclamation.MODID),
                45,
                0xFF8FDC73));
        context.missionRegistry().registerMissionChain(new PrimeMissionRegistry.PrimeMissionChain(
                id("prime/mission_chain/agriculture"),
                "Prime Agriculture",
                "Scan soil, reclaim food sources, and stabilize early survival supplies.",
                route,
                List.of(id("mission/scan_soil"), id("mission/first_plot")),
                45));
        context.indexRegistry().registerCategory(new PrimeIndexRegistry.PrimeIndexCategory(
                id("prime/index/agriculture"),
                "Prime Agriculture",
                "Food, soil amendments, planters, and reclamation recipes.",
                unlock,
                EchoAgricultureReclamation.MODID,
                45));
        context.indexRegistry().registerRecipeHint(new PrimeIndexRegistry.PrimeRecipeHint(
                id("prime/recipe_hint/field_rations"),
                id("prime/index/agriculture"),
                "Field Rations",
                "Use Agriculture Reclamation to make exploration supplies from stable crops.",
                prime("started"),
                EchoAgricultureReclamation.MODID,
                45));
        context.lensRegistry().registerScanData(new PrimeLensRegistry.PrimeScanData(
                id("prime/scan_data/reclaimable_soil"),
                prime("scan/prime_material"),
                id("reclaimable_soil").toString(),
                "Reports soil nutrients, growth risk, and food route readiness.",
                "none",
                "Seeds, compost, and crop samples.",
                "A food route makes Prime exploration less brittle.",
                EchoAgricultureReclamation.MODID));
        context.terminalRegistry().registerCard(new PrimeTerminalRegistry.PrimeTerminalCard(
                id("prime/card/agriculture_readiness"),
                route,
                "Agriculture Readiness",
                "Shows food security, soil scans, crop route tasks, and warnings.",
                unlock,
                EchoAgricultureReclamation.MODID,
                45));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoAgricultureReclamation.MODID, path);
    }

    private static Identifier prime(String path) {
        return Identifier.fromNamespaceAndPath("echoprimecore", path);
    }
}
