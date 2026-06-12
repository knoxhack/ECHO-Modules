package com.knoxhack.echoorbitalremnants.integration.prime;

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
import com.knoxhack.echoorbitalremnants.EchoOrbitalRemnants;
import java.util.List;
import net.minecraft.resources.Identifier;

public final class OrbitalPrimeIntegration implements EchoPrimeIntegration {
    private static final OrbitalPrimeIntegration INSTANCE = new OrbitalPrimeIntegration();

    private OrbitalPrimeIntegration() {
    }

    public static void register() {
        EchoPrimeIntegrations.register(INSTANCE);
    }

    @Override
    public Identifier id() {
        return id("prime/orbital");
    }

    @Override
    public boolean available(PrimeIntegrationContext context) {
        return context.moduleLoaded(EchoOrbitalRemnants.MODID);
    }

    @Override
    public void registerPrime(PrimeIntegrationContext context) {
        Identifier route = prime("route/orbital");
        Identifier unlock = prime("orbital_signal_found");
        context.routeRegistry().registerRoute(new PrimeRouteRegistry.PrimeRoute(
                route,
                "Orbital Signals",
                "Telemetry caches, debris traces, orbital distress markers, and late route handoffs.",
                unlock,
                List.of(EchoOrbitalRemnants.MODID),
                82,
                0xFF8DDCFF));
        context.missionRegistry().registerMissionChain(new PrimeMissionRegistry.PrimeMissionChain(
                id("prime/mission_chain/orbital"),
                "Prime Orbital",
                "Resolve an orbital signal and recover telemetry without leaving normal survival behind.",
                route,
                List.of(id("mission/telemetry_cache"), id("mission/orbital_signal")),
                82));
        context.indexRegistry().registerCategory(new PrimeIndexRegistry.PrimeIndexCategory(
                id("prime/index/orbital"),
                "Prime Orbital",
                "Telemetry, debris salvage, suit prep, and orbital route hints.",
                unlock,
                EchoOrbitalRemnants.MODID,
                82));
        context.lensRegistry().registerScanData(new PrimeLensRegistry.PrimeScanData(
                id("prime/scan_data/orbital_signal"),
                prime("scan/prime_nexus_trace"),
                id("orbital_signal").toString(),
                "Identifies telemetry caches and route escalation toward Stationfall or Nexus.",
                "medium",
                "Telemetry wafers, orbital salvage, and sealed caches.",
                "Prime can discover orbit from the overworld before any late-game jump.",
                EchoOrbitalRemnants.MODID));
        context.holoMapRegistry().registerLayer(new PrimeHoloMapRegistry.PrimeMapLayer(
                id("prime/layer/orbital_signals"),
                "Orbital Signals",
                "Telemetry, debris fields, and orbital route markers.",
                0xFF8DDCFF,
                false,
                82));
        context.holoMapRegistry().registerMarkerType(new PrimeHoloMapRegistry.PrimeMarkerType(
                id("prime/marker/orbital_signal"),
                id("prime/layer/orbital_signals"),
                "Orbital Signal",
                "orbital_signal",
                82));
        context.lootRegistry().registerInjection(new PrimeLootRegistry.PrimeLootInjection(
                id("prime/loot/telemetry_cache"),
                prime("loot/prime_nexus_cache"),
                id("telemetry_cache"),
                1,
                1,
                5,
                EchoOrbitalRemnants.MODID));
        context.terminalRegistry().registerCard(new PrimeTerminalRegistry.PrimeTerminalCard(
                id("prime/card/orbital_route"),
                route,
                "Orbital Route",
                "Shows telemetry cache readiness, orbital warnings, and Nexus handoff state.",
                unlock,
                EchoOrbitalRemnants.MODID,
                82));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoOrbitalRemnants.MODID, path);
    }

    private static Identifier prime(String path) {
        return Identifier.fromNamespaceAndPath("echoprimecore", path);
    }
}
