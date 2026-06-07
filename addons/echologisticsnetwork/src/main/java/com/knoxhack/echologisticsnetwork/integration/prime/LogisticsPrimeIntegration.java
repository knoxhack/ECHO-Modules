package com.knoxhack.echologisticsnetwork.integration.prime;

import com.knoxhack.echocore.api.prime.EchoPrimeIntegration;
import com.knoxhack.echocore.api.prime.EchoPrimeIntegrations;
import com.knoxhack.echocore.api.prime.PrimeHoloMapRegistry;
import com.knoxhack.echocore.api.prime.PrimeIndexRegistry;
import com.knoxhack.echocore.api.prime.PrimeIntegrationContext;
import com.knoxhack.echocore.api.prime.PrimeLensRegistry;
import com.knoxhack.echocore.api.prime.PrimeMissionRegistry;
import com.knoxhack.echocore.api.prime.PrimeRouteRegistry;
import com.knoxhack.echocore.api.prime.PrimeTerminalRegistry;
import com.knoxhack.echologisticsnetwork.EchoLogisticsNetwork;
import java.util.List;
import net.minecraft.resources.Identifier;

public final class LogisticsPrimeIntegration implements EchoPrimeIntegration {
    private static final LogisticsPrimeIntegration INSTANCE = new LogisticsPrimeIntegration();

    private LogisticsPrimeIntegration() {
    }

    public static void register() {
        EchoPrimeIntegrations.register(INSTANCE);
    }

    @Override
    public Identifier id() {
        return id("prime/logistics");
    }

    @Override
    public boolean available(PrimeIntegrationContext context) {
        return context.moduleLoaded(EchoLogisticsNetwork.MODID);
    }

    @Override
    public void registerPrime(PrimeIntegrationContext context) {
        Identifier route = prime("route/storage");
        Identifier unlock = prime("storage_online");
        context.routeRegistry().registerRoute(new PrimeRouteRegistry.PrimeRoute(
                route,
                "Storage and Logistics",
                "Storage networks, route requests, courier support, and inventory dashboards.",
                unlock,
                List.of(EchoLogisticsNetwork.MODID),
                32,
                0xFF9AD7FF));
        context.missionRegistry().registerMissionChain(new PrimeMissionRegistry.PrimeMissionChain(
                id("prime/mission_chain/logistics"),
                "Prime Logistics",
                "Bring storage online and connect early supplies to mission routes.",
                route,
                List.of(id("mission/storage_online"), id("mission/logistics_route")),
                32));
        context.indexRegistry().registerCategory(new PrimeIndexRegistry.PrimeIndexCategory(
                id("prime/index/storage"),
                "Prime Storage",
                "Storage chips, crates, terminals, networks, and logistics recipes.",
                unlock,
                EchoLogisticsNetwork.MODID,
                32));
        context.lensRegistry().registerScanData(new PrimeLensRegistry.PrimeScanData(
                id("prime/scan_data/storage_node"),
                prime("scan/prime_storage_node"),
                id("storage_node").toString(),
                "Reports network identity, capacity, routing state, and mission-linked supply needs.",
                "none",
                "Storage chips, crates, conduits, and logistics salvage.",
                "Stable storage is the difference between exploring and constantly unpacking.",
                EchoLogisticsNetwork.MODID));
        context.holoMapRegistry().registerLayer(new PrimeHoloMapRegistry.PrimeMapLayer(
                id("prime/layer/storage_networks"),
                "Storage Networks",
                "Storage nodes, depots, courier points, and supply route targets.",
                0xFF9AD7FF,
                false,
                32));
        context.holoMapRegistry().registerMarkerType(new PrimeHoloMapRegistry.PrimeMarkerType(
                id("prime/marker/storage_node"),
                id("prime/layer/storage_networks"),
                "Storage Node",
                "storage_node",
                32));
        context.terminalRegistry().registerCard(new PrimeTerminalRegistry.PrimeTerminalCard(
                id("prime/card/storage_network"),
                route,
                "Storage Network",
                "Shows storage capacity, logistics route readiness, and network warnings.",
                unlock,
                EchoLogisticsNetwork.MODID,
                32));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoLogisticsNetwork.MODID, path);
    }

    private static Identifier prime(String path) {
        return Identifier.fromNamespaceAndPath("echoprimecore", path);
    }
}
