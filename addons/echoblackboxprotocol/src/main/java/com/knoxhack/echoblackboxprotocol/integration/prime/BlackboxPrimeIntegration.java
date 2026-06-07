package com.knoxhack.echoblackboxprotocol.integration.prime;

import com.knoxhack.echoblackboxprotocol.EchoBlackboxProtocol;
import com.knoxhack.echocore.api.prime.EchoPrimeIntegration;
import com.knoxhack.echocore.api.prime.EchoPrimeIntegrations;
import com.knoxhack.echocore.api.prime.PrimeHoloMapRegistry;
import com.knoxhack.echocore.api.prime.PrimeIndexRegistry;
import com.knoxhack.echocore.api.prime.PrimeIntegrationContext;
import com.knoxhack.echocore.api.prime.PrimeLensRegistry;
import com.knoxhack.echocore.api.prime.PrimeLootRegistry;
import com.knoxhack.echocore.api.prime.PrimeMissionRegistry;
import com.knoxhack.echocore.api.prime.PrimeRouteRegistry;
import com.knoxhack.echocore.api.prime.PrimeTerminalRegistry;
import java.util.List;
import net.minecraft.resources.Identifier;

public final class BlackboxPrimeIntegration implements EchoPrimeIntegration {
    private static final BlackboxPrimeIntegration INSTANCE = new BlackboxPrimeIntegration();

    private BlackboxPrimeIntegration() {
    }

    public static void register() {
        EchoPrimeIntegrations.register(INSTANCE);
    }

    @Override
    public Identifier id() {
        return id("prime/blackbox");
    }

    @Override
    public boolean available(PrimeIntegrationContext context) {
        return context.moduleLoaded(EchoBlackboxProtocol.MODID);
    }

    @Override
    public void registerPrime(PrimeIntegrationContext context) {
        Identifier route = prime("route/blackbox");
        Identifier unlock = prime("first_blackbox");
        context.routeRegistry().registerRoute(new PrimeRouteRegistry.PrimeRoute(
                route,
                "Blackbox Logs",
                "Decoded logs, data unlocks, marker reveals, and Prime structure records.",
                unlock,
                List.of(EchoBlackboxProtocol.MODID),
                55,
                0xFF6FD6C9));
        context.missionRegistry().registerMissionChain(new PrimeMissionRegistry.PrimeMissionChain(
                id("prime/mission_chain/blackbox"),
                "Prime Blackbox",
                "Recover a blackbox log and use decoded data to reveal route markers.",
                route,
                List.of(id("mission/first_blackbox"), id("mission/decode_prime_log")),
                55));
        context.indexRegistry().registerCategory(new PrimeIndexRegistry.PrimeIndexCategory(
                id("prime/index/blackbox"),
                "Prime Blackbox",
                "Decoded data, log fragments, structure clues, and marker reveal hints.",
                unlock,
                EchoBlackboxProtocol.MODID,
                55));
        context.lensRegistry().registerScanData(new PrimeLensRegistry.PrimeScanData(
                id("prime/scan_data/blackbox_log"),
                prime("scan/prime_structure"),
                id("blackbox_log").toString(),
                "Identifies log integrity, decode requirements, and marker reveal potential.",
                "low",
                "Encoded data, log fragments, and route clues.",
                "Prime structures can hold blackbox truth without starting Ashfall.",
                EchoBlackboxProtocol.MODID));
        context.holoMapRegistry().registerLayer(new PrimeHoloMapRegistry.PrimeMapLayer(
                id("prime/layer/blackbox_locations"),
                "Blackbox Locations",
                "Decoded log sites, signal reveals, and hidden cache markers.",
                0xFF6FD6C9,
                false,
                55));
        context.holoMapRegistry().registerMarkerType(new PrimeHoloMapRegistry.PrimeMarkerType(
                id("prime/marker/blackbox_location"),
                id("prime/layer/blackbox_locations"),
                "Blackbox Location",
                "blackbox_location",
                55));
        context.lootRegistry().registerInjection(new PrimeLootRegistry.PrimeLootInjection(
                id("prime/loot/encoded_log"),
                prime("loot/prime_data_cache"),
                id("encoded_log"),
                1,
                1,
                7,
                EchoBlackboxProtocol.MODID));
        context.terminalRegistry().registerCard(new PrimeTerminalRegistry.PrimeTerminalCard(
                id("prime/card/blackbox_logs"),
                route,
                "Blackbox Logs",
                "Shows decoded logs, missing records, and marker reveal readiness.",
                unlock,
                EchoBlackboxProtocol.MODID,
                55));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoBlackboxProtocol.MODID, path);
    }

    private static Identifier prime(String path) {
        return Identifier.fromNamespaceAndPath("echoprimecore", path);
    }
}
