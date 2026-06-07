package com.knoxhack.echo.adaptercore;

import java.util.Objects;
import java.util.Set;

public record EchoPlatformAdapter(
        EchoAdapterId id,
        EchoAdapterKind kind,
        EchoAdapterRuntime runtime,
        String displayName,
        String summary,
        EchoAdapterStatus status,
        EchoAdapterCapabilities capabilities,
        EchoAdapterContext context,
        EchoCompatibilityMatrix compatibilityMatrix,
        boolean nativeLoaderSupported,
        Set<EchoAdapterDiagnostic> diagnostics
) {
    public EchoPlatformAdapter {
        Objects.requireNonNull(id, "id");
        kind = kind == null ? EchoAdapterKind.UNKNOWN : kind;
        runtime = runtime == null ? EchoAdapterRuntime.UNKNOWN : runtime;
        displayName = AdapterContractGuards.requireText(displayName, "display name");
        summary = AdapterContractGuards.optionalText(summary);
        status = status == null ? EchoAdapterStatus.UNKNOWN : status;
        Objects.requireNonNull(capabilities, "capabilities");
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(compatibilityMatrix, "compatibilityMatrix");
        diagnostics = AdapterContractGuards.immutableSet(diagnostics);
    }
}
