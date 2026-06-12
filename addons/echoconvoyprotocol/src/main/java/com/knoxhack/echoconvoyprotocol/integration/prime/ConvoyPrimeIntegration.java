package com.knoxhack.echoconvoyprotocol.integration.prime;

import com.knoxhack.echoconvoyprotocol.EchoConvoyProtocol;
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
import java.util.List;
import net.minecraft.resources.Identifier;

public final class ConvoyPrimeIntegration implements EchoPrimeIntegration {
    private static final ConvoyPrimeIntegration INSTANCE = new ConvoyPrimeIntegration();

    private ConvoyPrimeIntegration() {
    }

    public static void register() {
        EchoPrimeIntegrations.register(INSTANCE);
    }

    @Override
    public Identifier id() {
        return id("prime/convoy");
    }

    @Override
    public boolean available(PrimeIntegrationContext context) {
        return context.moduleLoaded(EchoConvoyProtocol.MODID);
    }

    @Override
    public void registerPrime(PrimeIntegrationContext context) {
        Identifier route = prime("route/logistics");
        Identifier unlock = prime("logistics_route_open");
        context.routeRegistry().registerRoute(new PrimeRouteRegistry.PrimeRoute(
                route,
                "Convoy Logistics",
                "Convoy wrecks, supply route missions, vehicle salvage, and logistics rewards.",
                unlock,
                List.of(EchoConvoyProtocol.MODID),
                58,
                0xFFE6B86E));
        context.missionRegistry().registerMissionChain(new PrimeMissionRegistry.PrimeMissionChain(
                id("prime/mission_chain/convoy"),
                "Prime Convoy",
                "Investigate a convoy wreck and turn supply routes into Prime rewards.",
                route,
                List.of(id("mission/convoy_wreck"), id("mission/supply_route")),
                58));
        context.indexRegistry().registerCategory(new PrimeIndexRegistry.PrimeIndexCategory(
                id("prime/index/convoy"),
                "Prime Convoy",
                "Convoy parts, repair kits, supply route contracts, and rewards.",
                unlock,
                EchoConvoyProtocol.MODID,
                58));
        context.lensRegistry().registerScanData(new PrimeLensRegistry.PrimeScanData(
                id("prime/scan_data/convoy_wreck"),
                prime("scan/prime_structure"),
                id("convoy_wreck").toString(),
                "Identifies wreck state, salvage quality, and linked supply missions.",
                "medium",
                "Vehicle parts, fuel, route crates, and logistics rewards.",
                "A convoy wreck can be a route, not just a pile of parts.",
                EchoConvoyProtocol.MODID));
        context.holoMapRegistry().registerLayer(new PrimeHoloMapRegistry.PrimeMapLayer(
                id("prime/layer/convoy_wrecks"),
                "Convoy Wrecks",
                "Wreck sites, supply routes, and logistics rewards.",
                0xFFE6B86E,
                false,
                58));
        context.holoMapRegistry().registerMarkerType(new PrimeHoloMapRegistry.PrimeMarkerType(
                id("prime/marker/convoy_wreck"),
                id("prime/layer/convoy_wrecks"),
                "Convoy Wreck",
                "convoy_wreck",
                58));
        context.lootRegistry().registerInjection(new PrimeLootRegistry.PrimeLootInjection(
                id("prime/loot/supply_crate"),
                prime("loot/prime_field_cache"),
                id("supply_crate"),
                1,
                1,
                6,
                EchoConvoyProtocol.MODID));
        context.terminalRegistry().registerCard(new PrimeTerminalRegistry.PrimeTerminalCard(
                id("prime/card/convoy_routes"),
                route,
                "Convoy Routes",
                "Shows wreck discoveries, supply route rewards, and logistics blockers.",
                unlock,
                EchoConvoyProtocol.MODID,
                58));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoConvoyProtocol.MODID, path);
    }

    private static Identifier prime(String path) {
        return Identifier.fromNamespaceAndPath("echoprimecore", path);
    }
}
