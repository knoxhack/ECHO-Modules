package com.knoxhack.echorendercore.integration.prime;

import com.echoplatform.echocore.api.prime.EchoPrimeIntegration;
import com.echoplatform.echocore.api.prime.EchoPrimeIntegrations;
import com.echoplatform.echocore.api.prime.PrimeAuditRegistry;
import com.echoplatform.echocore.api.prime.PrimeIntegrationContext;
import com.echoplatform.echocore.api.prime.PrimeTerminalRegistry;
import com.knoxhack.echorendercore.EchoRenderCore;
import net.minecraft.resources.Identifier;

public final class RenderCorePrimeIntegration implements EchoPrimeIntegration {
    private static final RenderCorePrimeIntegration INSTANCE = new RenderCorePrimeIntegration();

    private RenderCorePrimeIntegration() {
    }

    public static void register() {
        EchoPrimeIntegrations.register(INSTANCE);
    }

    @Override
    public Identifier id() {
        return id("prime/rendercore");
    }

    @Override
    public boolean available(PrimeIntegrationContext context) {
        return context.moduleLoaded(EchoRenderCore.MODID);
    }

    @Override
    public void registerPrime(PrimeIntegrationContext context) {
        context.terminalRegistry().registerCard(new PrimeTerminalRegistry.PrimeTerminalCard(
                id("prime/card/visual_effects"),
                prime("route/survival"),
                "Prime Visual Effects",
                "Enables Prime signal particles, scanner feedback, and stable route visual hints.",
                prime("lens_online"),
                EchoRenderCore.MODID,
                13));
        context.auditRegistry().registerDiagnostic(new PrimeAuditRegistry.PrimeAuditDiagnostic(
                id("prime/audit/visual_effects"),
                PrimeAuditRegistry.Severity.INFO,
                "Prime visual effects available",
                "RenderCore can supply Prime scanner and relay signal effect hooks.",
                EchoRenderCore.MODID));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(EchoRenderCore.MODID, path);
    }

    private static Identifier prime(String path) {
        return Identifier.fromNamespaceAndPath("echoprimecore", path);
    }
}
