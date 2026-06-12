package com.echoplatform.echocore.api.prime;

import java.util.List;
import net.minecraft.resources.Identifier;

public interface PrimeAuditRegistry {
    boolean registerDiagnostic(PrimeAuditDiagnostic diagnostic);

    List<PrimeAuditDiagnostic> diagnostics();

    enum Severity {
        INFO,
        WARNING,
        BLOCKED,
        CRITICAL
    }

    record PrimeAuditDiagnostic(
            Identifier id,
            Severity severity,
            String title,
            String detail,
            String sourceModule) {
    }
}
