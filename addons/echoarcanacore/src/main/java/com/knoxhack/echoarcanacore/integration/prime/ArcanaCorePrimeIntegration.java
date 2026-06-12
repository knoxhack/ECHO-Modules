package com.knoxhack.echoarcanacore.integration.prime;

import com.knoxhack.echoarcanacore.EchoArcanaCore;
import com.echoplatform.echocore.api.prime.EchoPrimeIntegration;
import com.echoplatform.echocore.api.prime.EchoPrimeIntegrations;
import com.echoplatform.echocore.api.prime.PrimeHoloMapRegistry;
import com.echoplatform.echocore.api.prime.PrimeIndexRegistry;
import com.echoplatform.echocore.api.prime.PrimeIntegrationContext;
import com.echoplatform.echocore.api.prime.PrimeLensRegistry;
import com.echoplatform.echocore.api.prime.PrimeMissionRegistry;
import com.echoplatform.echocore.api.prime.PrimeRouteRegistry;
import com.echoplatform.echocore.api.prime.PrimeTerminalRegistry;
import java.util.List;
import net.minecraft.resources.Identifier;

public final class ArcanaCorePrimeIntegration implements EchoPrimeIntegration {
    private static final ArcanaCorePrimeIntegration INSTANCE = new ArcanaCorePrimeIntegration();

    private ArcanaCorePrimeIntegration() {
    }

    public static void register() {
        EchoPrimeIntegrations.register(INSTANCE);
    }

    @Override
    public Identifier id() {
        return id("prime/arcana");
    }

    @Override
    public boolean available(PrimeIntegrationContext context) {
        return context.moduleLoaded(EchoArcanaCore.MODID);
    }

    @Override
    public void registerPrime(PrimeIntegrationContext context) {
        Identifier route = prime("route/arcana");
        Identifier unlock = prime("arcana_route_open");
        context.routeRegistry().registerRoute(new PrimeRouteRegistry.PrimeRoute(
                route,
                "Arcana",
                "Aether traces, grimoires, rituals, spells, curses, familiars, and bridge hooks.",
                unlock,
                List.of(EchoArcanaCore.MODID),
                50,
                0xFFB68CFF));
        context.missionRegistry().registerMissionChain(new PrimeMissionRegistry.PrimeMissionChain(
                id("prime/mission_chain/arcana"),
                "Prime Arcana",
                "Find an aether trace and decide whether Prime routes should touch spellcraft.",
                route,
                List.of(id("mission/aether_trace"), id("mission/arcana_route")),
                50));
        context.indexRegistry().registerCategory(new PrimeIndexRegistry.PrimeIndexCategory(
                id("prime/index/arcana"),
                "Prime Arcana",
                "Aether Signal, ritual, spell, curse, familiar, and grimoire route hints.",
                unlock,
                EchoArcanaCore.MODID,
                50));
        context.lensRegistry().registerScanData(new PrimeLensRegistry.PrimeScanData(
                id("prime/scan_data/aether_trace"),
                prime("scan/prime_arcana"),
                id("aether_trace").toString(),
                "Classifies ambient aether, ritual suitability, and arcane route pressure.",
                "variable",
                "Aether residues, glyph fragments, and arcane traces.",
                "Aether can widen Prime routes without starting Ashfall.",
                EchoArcanaCore.MODID));
        context.holoMapRegistry().registerLayer(new PrimeHoloMapRegistry.PrimeMapLayer(
                id("prime/layer/arcana_rifts"),
                "Arcana Rifts",
                "Aether traces, ritual sites, and arcane route discoveries.",
                0xFFB68CFF,
                false,
                50));
        context.holoMapRegistry().registerMarkerType(new PrimeHoloMapRegistry.PrimeMarkerType(
                id("prime/marker/arcana_rift"),
                id("prime/layer/arcana_rifts"),
                "Arcana Rift",
                "arcana_rift",
                50));
        context.terminalRegistry().registerCard(new PrimeTerminalRegistry.PrimeTerminalCard(
                id("prime/card/arcana_route"),
                route,
                "Arcana Route",
                "Shows aether trace discovery, grimoire readiness, and ritual warnings.",
                unlock,
                EchoArcanaCore.MODID,
                50));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoArcanaCore.MODID, path);
    }

    private static Identifier prime(String path) {
        return Identifier.fromNamespaceAndPath("echoprimecore", path);
    }
}
