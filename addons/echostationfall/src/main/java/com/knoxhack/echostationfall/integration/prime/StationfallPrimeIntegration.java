package com.knoxhack.echostationfall.integration.prime;

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
import com.knoxhack.echostationfall.EchoStationfall;
import java.util.List;
import net.minecraft.resources.Identifier;

public final class StationfallPrimeIntegration implements EchoPrimeIntegration {
    private static final StationfallPrimeIntegration INSTANCE = new StationfallPrimeIntegration();

    private StationfallPrimeIntegration() {
    }

    public static void register() {
        EchoPrimeIntegrations.register(INSTANCE);
    }

    @Override
    public Identifier id() {
        return id("prime/stationfall");
    }

    @Override
    public boolean available(PrimeIntegrationContext context) {
        return context.moduleLoaded(EchoStationfall.MODID);
    }

    @Override
    public void registerPrime(PrimeIntegrationContext context) {
        Identifier route = prime("route/stationfall");
        Identifier unlock = prime("stationfall_trace_found");
        context.routeRegistry().registerRoute(new PrimeRouteRegistry.PrimeRoute(
                route,
                "Stationfall",
                "Station debris, distress logs, orbital handoffs, and late-game route cards.",
                unlock,
                List.of(EchoStationfall.MODID),
                84,
                0xFF9CA8FF));
        context.missionRegistry().registerMissionChain(new PrimeMissionRegistry.PrimeMissionChain(
                id("prime/mission_chain/stationfall"),
                "Prime Stationfall",
                "Resolve a station trace and decide whether Prime is ready for orbital fallout.",
                route,
                List.of(id("mission/station_debris"), id("mission/distress_log")),
                84));
        context.indexRegistry().registerCategory(new PrimeIndexRegistry.PrimeIndexCategory(
                id("prime/index/stationfall"),
                "Prime Stationfall",
                "Station debris, distress logs, salvage, and route readiness hints.",
                unlock,
                EchoStationfall.MODID,
                84));
        context.lensRegistry().registerScanData(new PrimeLensRegistry.PrimeScanData(
                id("prime/scan_data/station_trace"),
                prime("scan/prime_nexus_trace"),
                id("station_trace").toString(),
                "Identifies station debris, distress log state, and orbital escalation.",
                "high",
                "Station salvage, distress logs, and sealed route data.",
                "A Stationfall trace belongs near the end of Prime route discovery.",
                EchoStationfall.MODID));
        context.holoMapRegistry().registerLayer(new PrimeHoloMapRegistry.PrimeMapLayer(
                id("prime/layer/stationfall_traces"),
                "Stationfall Traces",
                "Station debris, distress logs, and orbital fallout markers.",
                0xFF9CA8FF,
                false,
                84));
        context.holoMapRegistry().registerMarkerType(new PrimeHoloMapRegistry.PrimeMarkerType(
                id("prime/marker/stationfall_trace"),
                id("prime/layer/stationfall_traces"),
                "Stationfall Trace",
                "stationfall_trace",
                84));
        context.lootRegistry().registerInjection(new PrimeLootRegistry.PrimeLootInjection(
                id("prime/loot/distress_log"),
                prime("loot/prime_nexus_cache"),
                id("distress_log"),
                1,
                1,
                5,
                EchoStationfall.MODID));
        context.terminalRegistry().registerCard(new PrimeTerminalRegistry.PrimeTerminalCard(
                id("prime/card/stationfall_route"),
                route,
                "Stationfall Route",
                "Shows station trace readiness, distress logs, and orbital handoff warnings.",
                unlock,
                EchoStationfall.MODID,
                84));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoStationfall.MODID, path);
    }

    private static Identifier prime(String path) {
        return Identifier.fromNamespaceAndPath("echoprimecore", path);
    }
}
