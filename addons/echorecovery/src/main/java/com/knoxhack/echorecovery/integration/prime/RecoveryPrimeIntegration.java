package com.knoxhack.echorecovery.integration.prime;

import com.echoplatform.echocore.api.prime.EchoPrimeIntegration;
import com.echoplatform.echocore.api.prime.EchoPrimeIntegrations;
import com.echoplatform.echocore.api.prime.PrimeHoloMapRegistry;
import com.echoplatform.echocore.api.prime.PrimeIntegrationContext;
import com.echoplatform.echocore.api.prime.PrimeLensRegistry;
import com.echoplatform.echocore.api.prime.PrimeMissionRegistry;
import com.echoplatform.echocore.api.prime.PrimeRouteRegistry;
import com.echoplatform.echocore.api.prime.PrimeTerminalRegistry;
import com.knoxhack.echorecovery.EchoRecovery;
import java.util.List;
import net.minecraft.resources.Identifier;

public final class RecoveryPrimeIntegration implements EchoPrimeIntegration {
    private static final RecoveryPrimeIntegration INSTANCE = new RecoveryPrimeIntegration();

    private RecoveryPrimeIntegration() {
    }

    public static void register() {
        EchoPrimeIntegrations.register(INSTANCE);
    }

    @Override
    public Identifier id() {
        return id("prime/recovery");
    }

    @Override
    public boolean available(PrimeIntegrationContext context) {
        return context.moduleLoaded(EchoRecovery.MODID);
    }

    @Override
    public void registerPrime(PrimeIntegrationContext context) {
        Identifier route = prime("route/recovery");
        Identifier unlock = prime("started");
        context.routeRegistry().registerRoute(new PrimeRouteRegistry.PrimeRoute(
                route,
                "Recovery",
                "Death markers, grave scans, recovery objectives, and return-route safety.",
                unlock,
                List.of(EchoRecovery.MODID),
                15,
                0xFFB8C7D9));
        context.missionRegistry().registerMissionChain(new PrimeMissionRegistry.PrimeMissionChain(
                id("prime/mission_chain/recovery"),
                "Prime Recovery",
                "Recover dropped gear and record death markers on the Prime HoloMap.",
                route,
                List.of(id("mission/death_marker"), id("mission/grave_recovered")),
                15));
        context.lensRegistry().registerScanData(new PrimeLensRegistry.PrimeScanData(
                id("prime/scan_data/grave"),
                prime("scan/prime_structure"),
                id("grave").toString(),
                "Identifies grave owner, recovery status, and safe return hints.",
                "contextual",
                "Recovered player inventory.",
                "Prime marks recovery without changing normal overworld spawning.",
                EchoRecovery.MODID));
        context.holoMapRegistry().registerLayer(new PrimeHoloMapRegistry.PrimeMapLayer(
                id("prime/layer/death_recovery"),
                "Death Recovery",
                "Death markers, graves, and recovery route targets.",
                0xFFB8C7D9,
                true,
                15));
        context.holoMapRegistry().registerMarkerType(new PrimeHoloMapRegistry.PrimeMarkerType(
                id("prime/marker/death_marker"),
                id("prime/layer/death_recovery"),
                "Death Marker",
                "death_marker",
                15));
        context.terminalRegistry().registerCard(new PrimeTerminalRegistry.PrimeTerminalCard(
                id("prime/card/recovery"),
                route,
                "Recovery",
                "Shows active grave markers and recovery mission state.",
                unlock,
                EchoRecovery.MODID,
                15));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoRecovery.MODID, path);
    }

    private static Identifier prime(String path) {
        return Identifier.fromNamespaceAndPath("echoprimecore", path);
    }
}
