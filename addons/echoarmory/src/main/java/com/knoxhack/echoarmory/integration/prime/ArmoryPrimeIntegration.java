package com.knoxhack.echoarmory.integration.prime;

import com.knoxhack.echoarmory.EchoArmory;
import com.echoplatform.echocore.api.prime.EchoPrimeIntegration;
import com.echoplatform.echocore.api.prime.EchoPrimeIntegrations;
import com.echoplatform.echocore.api.prime.PrimeIndexRegistry;
import com.echoplatform.echocore.api.prime.PrimeIntegrationContext;
import com.echoplatform.echocore.api.prime.PrimeLensRegistry;
import com.echoplatform.echocore.api.prime.PrimeMissionRegistry;
import com.echoplatform.echocore.api.prime.PrimeRouteRegistry;
import com.echoplatform.echocore.api.prime.PrimeTerminalRegistry;
import java.util.List;
import net.minecraft.resources.Identifier;

public final class ArmoryPrimeIntegration implements EchoPrimeIntegration {
    private static final ArmoryPrimeIntegration INSTANCE = new ArmoryPrimeIntegration();

    private ArmoryPrimeIntegration() {
    }

    public static void register() {
        EchoPrimeIntegrations.register(INSTANCE);
    }

    @Override
    public Identifier id() {
        return id("prime/armory");
    }

    @Override
    public boolean available(PrimeIntegrationContext context) {
        return context.moduleLoaded(EchoArmory.MODID);
    }

    @Override
    public void registerPrime(PrimeIntegrationContext context) {
        Identifier route = prime("route/combat");
        Identifier unlock = prime("combat_route_open");
        context.routeRegistry().registerRoute(new PrimeRouteRegistry.PrimeRoute(
                route,
                "Combat",
                "Gear missions, modular upgrades, mob weakness scans, and combat readiness.",
                unlock,
                List.of(EchoArmory.MODID),
                48,
                0xFFE87575));
        context.missionRegistry().registerMissionChain(new PrimeMissionRegistry.PrimeMissionChain(
                id("prime/mission_chain/combat"),
                "Prime Combat",
                "Upgrade field gear and learn how Prime mobs can be countered.",
                route,
                List.of(id("mission/first_upgrade"), id("mission/combat_ready")),
                48));
        context.indexRegistry().registerCategory(new PrimeIndexRegistry.PrimeIndexCategory(
                id("prime/index/combat"),
                "Prime Combat",
                "Armor, tools, weapon modules, and combat upgrade recipes.",
                unlock,
                EchoArmory.MODID,
                48));
        context.lensRegistry().registerScanData(new PrimeLensRegistry.PrimeScanData(
                id("prime/scan_data/mob_weakness"),
                prime("scan/prime_mob"),
                id("mob_weakness").toString(),
                "Reports armor class, weakness tags, expected drops, and upgrade hints.",
                "contextual",
                "Combat salvage and upgrade parts.",
                "Scanning before fighting is a perfectly valid Prime survival instinct.",
                EchoArmory.MODID));
        context.terminalRegistry().registerCard(new PrimeTerminalRegistry.PrimeTerminalCard(
                id("prime/card/combat_readiness"),
                route,
                "Combat Readiness",
                "Shows gear upgrades, weakness scan coverage, and route risk.",
                unlock,
                EchoArmory.MODID,
                48));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoArmory.MODID, path);
    }

    private static Identifier prime(String path) {
        return Identifier.fromNamespaceAndPath("echoprimecore", path);
    }
}
