package com.knoxhack.echopowergrid.integration.prime;

import com.knoxhack.echocore.api.prime.EchoPrimeIntegration;
import com.knoxhack.echocore.api.prime.EchoPrimeIntegrations;
import com.knoxhack.echocore.api.prime.PrimeHoloMapRegistry;
import com.knoxhack.echocore.api.prime.PrimeIndexRegistry;
import com.knoxhack.echocore.api.prime.PrimeIntegrationContext;
import com.knoxhack.echocore.api.prime.PrimeLensRegistry;
import com.knoxhack.echocore.api.prime.PrimeMissionRegistry;
import com.knoxhack.echocore.api.prime.PrimeRouteRegistry;
import com.knoxhack.echocore.api.prime.PrimeTerminalRegistry;
import com.knoxhack.echopowergrid.EchoPowerGrid;
import java.util.List;
import net.minecraft.resources.Identifier;

public final class PowerGridPrimeIntegration implements EchoPrimeIntegration {
    private static final PowerGridPrimeIntegration INSTANCE = new PowerGridPrimeIntegration();

    private PowerGridPrimeIntegration() {
    }

    public static void register() {
        EchoPrimeIntegrations.register(INSTANCE);
    }

    @Override
    public Identifier id() {
        return id("prime/powergrid");
    }

    @Override
    public boolean available(PrimeIntegrationContext context) {
        return context.moduleLoaded(EchoPowerGrid.MODID);
    }

    @Override
    public void registerPrime(PrimeIntegrationContext context) {
        Identifier route = prime("route/power");
        Identifier unlock = prime("powergrid_online");
        context.routeRegistry().registerRoute(new PrimeRouteRegistry.PrimeRoute(
                route,
                "PowerGrid",
                "Power cells, grid nodes, relay coils, generation, and stored energy telemetry.",
                unlock,
                List.of(EchoPowerGrid.MODID),
                20,
                0xFFFFD166));
        context.missionRegistry().registerMissionChain(new PrimeMissionRegistry.PrimeMissionChain(
                id("prime/mission_chain/power"),
                "Prime PowerGrid",
                "Bring a starter grid online and route weak signal hardware through stable power.",
                route,
                List.of(id("mission/first_cell"), id("mission/first_grid")),
                20));
        context.indexRegistry().registerCategory(new PrimeIndexRegistry.PrimeIndexCategory(
                id("prime/index/power"),
                "Prime Power",
                "Cells, coils, generators, batteries, and grid node recipes unlocked by Prime progress.",
                unlock,
                EchoPowerGrid.MODID,
                20));
        context.indexRegistry().registerRecipeHint(new PrimeIndexRegistry.PrimeRecipeHint(
                id("prime/recipe_hint/basic_power_cell"),
                id("prime/index/power"),
                "Basic Power Cell",
                "Route Prime Circuit and Relay Coil work into a starter PowerGrid cell.",
                prime("first_machine"),
                EchoPowerGrid.MODID,
                20));
        context.lensRegistry().registerScanData(new PrimeLensRegistry.PrimeScanData(
                id("prime/scan_data/power_node"),
                prime("scan/prime_power_node"),
                id("power_node").toString(),
                "Reports capacity, load, network identity, and route readiness.",
                "low",
                "PowerGrid components and repair salvage.",
                "A powered node can promote Prime from survival tools to route infrastructure.",
                EchoPowerGrid.MODID));
        context.holoMapRegistry().registerLayer(new PrimeHoloMapRegistry.PrimeMapLayer(
                id("prime/layer/powergrid_nodes"),
                "PowerGrid Nodes",
                "Generators, batteries, relays, and field power anchors.",
                0xFFFFD166,
                false,
                20));
        context.holoMapRegistry().registerMarkerType(new PrimeHoloMapRegistry.PrimeMarkerType(
                id("prime/marker/power_node"),
                id("prime/layer/powergrid_nodes"),
                "Power Node",
                "power_node",
                20));
        context.terminalRegistry().registerCard(new PrimeTerminalRegistry.PrimeTerminalCard(
                id("prime/card/power_status"),
                route,
                "PowerGrid Status",
                "Shows starter grid readiness, stored power, route blockers, and node warnings.",
                unlock,
                EchoPowerGrid.MODID,
                20));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoPowerGrid.MODID, path);
    }

    private static Identifier prime(String path) {
        return Identifier.fromNamespaceAndPath("echoprimecore", path);
    }
}
