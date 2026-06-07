package com.knoxhack.echo.adaptercore;

import com.knoxhack.echo.platformcore.EchoFeatureId;

public record EchoAdapterDiagnostic(
        String code,
        String title,
        String summary,
        EchoAdapterStatus status,
        EchoFeatureId affectedFeature,
        boolean blocking,
        String recommendation
) {
    public EchoAdapterDiagnostic {
        code = AdapterContractGuards.requireText(code, "diagnostic code");
        title = AdapterContractGuards.requireText(title, "diagnostic title");
        summary = AdapterContractGuards.optionalText(summary);
        status = status == null ? EchoAdapterStatus.UNKNOWN : status;
        recommendation = AdapterContractGuards.optionalText(recommendation);
    }
}
