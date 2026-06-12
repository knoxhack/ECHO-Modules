package com.knoxhack.echosoundcore.integration.prime;

import com.echoplatform.echocore.api.prime.EchoPrimeIntegration;
import com.echoplatform.echocore.api.prime.EchoPrimeIntegrations;
import com.echoplatform.echocore.api.prime.PrimeAuditRegistry;
import com.echoplatform.echocore.api.prime.PrimeIntegrationContext;
import com.echoplatform.echocore.api.prime.PrimeTerminalRegistry;
import com.knoxhack.echosoundcore.EchoSoundCore;
import net.minecraft.resources.Identifier;

public final class SoundCorePrimeIntegration implements EchoPrimeIntegration {
    private static final SoundCorePrimeIntegration INSTANCE = new SoundCorePrimeIntegration();

    private SoundCorePrimeIntegration() {
    }

    public static void register() {
        EchoPrimeIntegrations.register(INSTANCE);
    }

    @Override
    public Identifier id() {
        return id("prime/soundcore");
    }

    @Override
    public boolean available(PrimeIntegrationContext context) {
        return context.moduleLoaded(EchoSoundCore.MODID);
    }

    @Override
    public void registerPrime(PrimeIntegrationContext context) {
        context.terminalRegistry().registerCard(new PrimeTerminalRegistry.PrimeTerminalCard(
                id("prime/card/sound_profile"),
                prime("route/survival"),
                "Prime Sound Profile",
                "Enables stable overworld ambience, low-signal stingers, and route-aware audio cues.",
                prime("started"),
                EchoSoundCore.MODID,
                12));
        context.auditRegistry().registerDiagnostic(new PrimeAuditRegistry.PrimeAuditDiagnostic(
                id("prime/audit/sound_profile"),
                PrimeAuditRegistry.Severity.INFO,
                "Prime sound profile available",
                "SoundCore can supply Prime survival ambience and signal cue diagnostics.",
                EchoSoundCore.MODID));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoSoundCore.MODID, path);
    }

    private static Identifier prime(String path) {
        return Identifier.fromNamespaceAndPath("echoprimecore", path);
    }
}
