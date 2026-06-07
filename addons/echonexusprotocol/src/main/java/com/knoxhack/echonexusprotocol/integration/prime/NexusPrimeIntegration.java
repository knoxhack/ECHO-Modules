package com.knoxhack.echonexusprotocol.integration.prime;

import com.knoxhack.echocore.api.prime.EchoPrimeIntegration;
import com.knoxhack.echocore.api.prime.EchoPrimeIntegrations;
import com.knoxhack.echocore.api.prime.PrimeHoloMapRegistry;
import com.knoxhack.echocore.api.prime.PrimeIndexRegistry;
import com.knoxhack.echocore.api.prime.PrimeIntegrationContext;
import com.knoxhack.echocore.api.prime.PrimeLensRegistry;
import com.knoxhack.echocore.api.prime.PrimeMissionRegistry;
import com.knoxhack.echocore.api.prime.PrimeRouteRegistry;
import com.knoxhack.echocore.api.prime.PrimeTerminalRegistry;
import com.knoxhack.echonexusprotocol.EchoNexusProtocol;
import java.util.List;
import net.minecraft.resources.Identifier;

public final class NexusPrimeIntegration implements EchoPrimeIntegration {
    private static final NexusPrimeIntegration INSTANCE = new NexusPrimeIntegration();

    private NexusPrimeIntegration() {
    }

    public static void register() {
        EchoPrimeIntegrations.register(INSTANCE);
    }

    @Override
    public Identifier id() {
        return id("prime/nexus");
    }

    @Override
    public boolean available(PrimeIntegrationContext context) {
        return context.moduleLoaded(EchoNexusProtocol.MODID);
    }

    @Override
    public void registerPrime(PrimeIntegrationContext context) {
        Identifier route = prime("route/nexus");
        Identifier unlock = prime("nexus_trace_found");
        context.routeRegistry().registerRoute(new PrimeRouteRegistry.PrimeRoute(
                route,
                "Nexus",
                "Late-game traces, anomaly markers, guardian readiness, and route convergence.",
                unlock,
                List.of(EchoNexusProtocol.MODID),
                90,
                0xFF7ACBFF));
        context.missionRegistry().registerMissionChain(new PrimeMissionRegistry.PrimeMissionChain(
                id("prime/mission_chain/nexus"),
                "Prime Nexus",
                "Collect enough route evidence to identify Nexus readiness and final escalation risks.",
                route,
                List.of(id("mission/nexus_trace"), id("mission/guardian_readiness")),
                90));
        context.indexRegistry().registerCategory(new PrimeIndexRegistry.PrimeIndexCategory(
                id("prime/index/nexus"),
                "Prime Nexus",
                "Nexus trace decoding, anomaly hardware, and late-game route recipes.",
                unlock,
                EchoNexusProtocol.MODID,
                90));
        context.lensRegistry().registerScanData(new PrimeLensRegistry.PrimeScanData(
                id("prime/scan_data/nexus_trace"),
                prime("scan/prime_nexus_trace"),
                id("nexus_trace").toString(),
                "Reports anomaly stability, route convergence, and guardian pressure.",
                "high",
                "Nexus fragments, corrupted data, and late-game components.",
                "Do not treat a Nexus trace like normal salvage.",
                EchoNexusProtocol.MODID));
        context.holoMapRegistry().registerLayer(new PrimeHoloMapRegistry.PrimeMapLayer(
                id("prime/layer/nexus_traces"),
                "Nexus Traces",
                "Anomalies, route convergence markers, and guardian readiness targets.",
                0xFF7ACBFF,
                false,
                90));
        context.holoMapRegistry().registerMarkerType(new PrimeHoloMapRegistry.PrimeMarkerType(
                id("prime/marker/nexus_trace"),
                id("prime/layer/nexus_traces"),
                "Nexus Trace",
                "nexus_trace",
                90));
        context.terminalRegistry().registerCard(new PrimeTerminalRegistry.PrimeTerminalCard(
                id("prime/card/nexus_readiness"),
                route,
                "Nexus Readiness",
                "Shows route prerequisites, anomaly warnings, and Prime Guardian readiness.",
                unlock,
                EchoNexusProtocol.MODID,
                90));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoNexusProtocol.MODID, path);
    }

    private static Identifier prime(String path) {
        return Identifier.fromNamespaceAndPath("echoprimecore", path);
    }
}
