package com.knoxhack.echoindustrialnexus.integration.prime;

import com.knoxhack.echocore.api.prime.EchoPrimeIntegration;
import com.knoxhack.echocore.api.prime.EchoPrimeIntegrations;
import com.knoxhack.echocore.api.prime.PrimeHoloMapRegistry;
import com.knoxhack.echocore.api.prime.PrimeIndexRegistry;
import com.knoxhack.echocore.api.prime.PrimeIntegrationContext;
import com.knoxhack.echocore.api.prime.PrimeLensRegistry;
import com.knoxhack.echocore.api.prime.PrimeMissionRegistry;
import com.knoxhack.echocore.api.prime.PrimeRouteRegistry;
import com.knoxhack.echocore.api.prime.PrimeTerminalRegistry;
import com.knoxhack.echoindustrialnexus.EchoIndustrialNexus;
import java.util.List;
import net.minecraft.resources.Identifier;

public final class IndustrialPrimeIntegration implements EchoPrimeIntegration {
    private static final IndustrialPrimeIntegration INSTANCE = new IndustrialPrimeIntegration();

    private IndustrialPrimeIntegration() {
    }

    public static void register() {
        EchoPrimeIntegrations.register(INSTANCE);
    }

    @Override
    public Identifier id() {
        return id("prime/industrial");
    }

    @Override
    public boolean available(PrimeIntegrationContext context) {
        return context.moduleLoaded(EchoIndustrialNexus.MODID);
    }

    @Override
    public void registerPrime(PrimeIntegrationContext context) {
        Identifier route = prime("route/tech");
        Identifier unlock = prime("industrial_route_open");
        context.routeRegistry().registerRoute(new PrimeRouteRegistry.PrimeRoute(
                route,
                "Industrial Nexus",
                "Machines, processing chains, factory markers, and first real automation.",
                unlock,
                List.of(EchoIndustrialNexus.MODID),
                30,
                0xFFB8D36B));
        context.missionRegistry().registerMissionChain(new PrimeMissionRegistry.PrimeMissionChain(
                id("prime/mission_chain/industrial"),
                "Prime Industry",
                "Build starter machines and turn relay salvage into infrastructure.",
                route,
                List.of(id("mission/first_machine"), id("mission/processing_chain")),
                30));
        context.indexRegistry().registerCategory(new PrimeIndexRegistry.PrimeIndexCategory(
                id("prime/index/machines"),
                "Prime Machines",
                "Industrial Nexus machines, processors, casings, and factory recipes.",
                unlock,
                EchoIndustrialNexus.MODID,
                30));
        context.lensRegistry().registerScanData(new PrimeLensRegistry.PrimeScanData(
                id("prime/scan_data/industrial_machine"),
                prime("scan/prime_machine"),
                id("machine").toString(),
                "Reports machine tier, inputs, route readiness, and hazard status.",
                "medium",
                "Machine parts, plates, and factory salvage.",
                "Industrial machines are Prime's first durable route multiplier.",
                EchoIndustrialNexus.MODID));
        context.holoMapRegistry().registerLayer(new PrimeHoloMapRegistry.PrimeMapLayer(
                id("prime/layer/factory_markers"),
                "Factory Markers",
                "Industrial machine sites, factory ruins, and processing route targets.",
                0xFFB8D36B,
                false,
                30));
        context.holoMapRegistry().registerMarkerType(new PrimeHoloMapRegistry.PrimeMarkerType(
                id("prime/marker/factory_node"),
                id("prime/layer/factory_markers"),
                "Factory Node",
                "factory_node",
                30));
        context.terminalRegistry().registerCard(new PrimeTerminalRegistry.PrimeTerminalCard(
                id("prime/card/industrial_route"),
                route,
                "Industrial Route",
                "Shows machine readiness, factory markers, and starter processing blockers.",
                unlock,
                EchoIndustrialNexus.MODID,
                30));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoIndustrialNexus.MODID, path);
    }

    private static Identifier prime(String path) {
        return Identifier.fromNamespaceAndPath("echoprimecore", path);
    }
}
