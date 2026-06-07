package com.knoxhack.echoprimecore.integration;

import com.knoxhack.echocore.api.EchoCoreServices;
import com.knoxhack.echocore.api.EchoDiagnosticBlocker;
import com.knoxhack.echocore.api.prime.PrimeAuditRegistry;
import com.knoxhack.echoprimecore.EchoPrimeCore;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public final class PrimeAuditService {
    private static PrimeIntegrationRegistry registry;

    private PrimeAuditService() {
    }

    public static void register(PrimeIntegrationRegistry source) {
        registry = source;
        EchoCoreServices.registerDiagnosticService(PrimeAuditService::diagnostics);
    }

    public static List<EchoDiagnosticBlocker> diagnostics(Player player) {
        PrimeIntegrationRegistry safeRegistry = registry == null ? PrimeIntegrationLoader.registry() : registry;
        List<EchoDiagnosticBlocker> result = new ArrayList<>();
        for (PrimeAuditRegistry.PrimeAuditDiagnostic diagnostic : safeRegistry.diagnostics()) {
            result.add(new EchoDiagnosticBlocker(
                    diagnostic.id(),
                    EchoPrimeCore.MODID,
                    severity(diagnostic.severity()),
                    diagnostic.title(),
                    diagnostic.detail(),
                    "Install the module or leave the Prime route dormant."));
        }
        if (safeRegistry.routes().isEmpty()) {
            result.add(new EchoDiagnosticBlocker(
                    EchoPrimeCore.id("audit/no_routes"),
                    EchoPrimeCore.MODID,
                    EchoDiagnosticBlocker.Severity.BLOCKED,
                    "No Prime routes registered",
                    "Prime Core has no route records. Integration loading likely failed.",
                    "Check the ECHO: Prime Core log during common setup."));
        }
        if (safeRegistry.cards().size() < safeRegistry.routes().size()) {
            result.add(new EchoDiagnosticBlocker(
                    EchoPrimeCore.id("audit/missing_route_cards"),
                    EchoPrimeCore.MODID,
                    EchoDiagnosticBlocker.Severity.WARNING,
                    "Missing route cards",
                    "Some Prime routes do not have matching Terminal cards.",
                    "Register a PrimeTerminalCard for every route provider."));
        }
        return List.copyOf(result);
    }

    private static EchoDiagnosticBlocker.Severity severity(PrimeAuditRegistry.Severity severity) {
        return switch (severity) {
            case WARNING -> EchoDiagnosticBlocker.Severity.WARNING;
            case BLOCKED -> EchoDiagnosticBlocker.Severity.BLOCKED;
            case CRITICAL -> EchoDiagnosticBlocker.Severity.CRITICAL;
            default -> EchoDiagnosticBlocker.Severity.INFO;
        };
    }
}
