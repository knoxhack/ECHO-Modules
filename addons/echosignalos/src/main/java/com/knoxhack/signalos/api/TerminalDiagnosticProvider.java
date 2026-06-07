package com.knoxhack.signalos.api;

import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public interface TerminalDiagnosticProvider {
    Identifier id();

    List<Diagnostic> diagnostics(Player player);

    default int order() {
        return 0;
    }

    default SignalOsProviderStatus providerStatus(Player player) {
        return SignalOsProviderStatus.online(id(), "Diagnostic Provider");
    }

    record Diagnostic(Identifier id, String title, String detail, Severity severity) {
        public Diagnostic {
            id = TerminalIds.requireLowercase(id, "Terminal diagnostic");
            title = title == null || title.isBlank() ? id.getPath() : title.strip();
            detail = detail == null ? "" : detail.strip();
            severity = severity == null ? Severity.INFO : severity;
        }
    }

    enum Severity {
        INFO,
        WARNING,
        BLOCKED,
        CRITICAL
    }
}
