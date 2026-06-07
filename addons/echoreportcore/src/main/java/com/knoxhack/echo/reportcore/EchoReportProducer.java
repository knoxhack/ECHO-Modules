package com.knoxhack.echo.reportcore;

import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;

public interface EchoReportProducer<T> {
    EchoReportDescriptor descriptor();

    T produce(EchoReportGenerationContext context);

    default List<EchoDiagnostic> preflight(EchoReportGenerationContext context) {
        return List.of();
    }
}
