package com.knoxhack.echobasegrid.integration.prime;

import com.knoxhack.echobasegrid.EchoBaseGrid;
import com.knoxhack.echocore.api.prime.EchoPrimeIntegration;
import com.knoxhack.echocore.api.prime.EchoPrimeIntegrations;
import com.knoxhack.echocore.api.prime.PrimeHoloMapRegistry;
import com.knoxhack.echocore.api.prime.PrimeIndexRegistry;
import com.knoxhack.echocore.api.prime.PrimeIntegrationContext;
import com.knoxhack.echocore.api.prime.PrimeLensRegistry;
import com.knoxhack.echocore.api.prime.PrimeMissionRegistry;
import com.knoxhack.echocore.api.prime.PrimeRouteRegistry;
import com.knoxhack.echocore.api.prime.PrimeTerminalRegistry;
import java.util.List;
import net.minecraft.resources.Identifier;

public final class BaseGridPrimeIntegration implements EchoPrimeIntegration {
    private static final BaseGridPrimeIntegration INSTANCE = new BaseGridPrimeIntegration();

    private BaseGridPrimeIntegration() {
    }

    public static void register() {
        EchoPrimeIntegrations.register(INSTANCE);
    }

    @Override
    public Identifier id() {
        return id("prime/basegrid");
    }

    @Override
    public boolean available(PrimeIntegrationContext context) {
        return context.moduleLoaded(EchoBaseGrid.MODID);
    }

    @Override
    public void registerPrime(PrimeIntegrationContext context) {
        Identifier route = prime("route/base");
        Identifier unlock = prime("basegrid_online");
        context.routeRegistry().registerRoute(new PrimeRouteRegistry.PrimeRoute(
                route,
                "BaseGrid",
                "Base anchors, claims, recovery points, shelter status, and field operations.",
                unlock,
                List.of(EchoBaseGrid.MODID),
                40,
                0xFF90E28B));
        context.missionRegistry().registerMissionChain(new PrimeMissionRegistry.PrimeMissionChain(
                id("prime/mission_chain/base"),
                "Prime Base",
                "Establish a field base and connect it to Prime survival objectives.",
                route,
                List.of(id("mission/claim_anchor"), id("mission/base_online")),
                40));
        context.indexRegistry().registerCategory(new PrimeIndexRegistry.PrimeIndexCategory(
                id("prime/index/base"),
                "Prime Base",
                "Base anchors, utility blocks, workbench upgrades, and field storage support.",
                unlock,
                EchoBaseGrid.MODID,
                40));
        context.lensRegistry().registerScanData(new PrimeLensRegistry.PrimeScanData(
                id("prime/scan_data/base_anchor"),
                prime("scan/prime_basegrid_node"),
                id("base_anchor").toString(),
                "Identifies base ownership, range, protection state, and route blockers.",
                "none",
                "BaseGrid materials and repair supplies.",
                "A stable base turns Prime discoveries into durable infrastructure.",
                EchoBaseGrid.MODID));
        context.holoMapRegistry().registerLayer(new PrimeHoloMapRegistry.PrimeMapLayer(
                id("prime/layer/basegrid_locations"),
                "BaseGrid Locations",
                "Base anchors, shelter sites, claims, and return points.",
                0xFF90E28B,
                false,
                40));
        context.holoMapRegistry().registerMarkerType(new PrimeHoloMapRegistry.PrimeMarkerType(
                id("prime/marker/base_anchor"),
                id("prime/layer/basegrid_locations"),
                "Base Anchor",
                "base_anchor",
                40));
        context.terminalRegistry().registerCard(new PrimeTerminalRegistry.PrimeTerminalCard(
                id("prime/card/base_dashboard"),
                route,
                "Base Dashboard",
                "Shows anchor status, base readiness, route tasks, and shelter warnings.",
                unlock,
                EchoBaseGrid.MODID,
                40));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoBaseGrid.MODID, path);
    }

    private static Identifier prime(String path) {
        return Identifier.fromNamespaceAndPath("echoprimecore", path);
    }
}
