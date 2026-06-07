package com.knoxhack.echo.healthcore;

import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;

public interface EchoHealthReporter {
    EchoRuntimeHealthReport currentReport();

    default EchoHealthSnapshot currentSnapshot() {
        return currentReport().snapshot();
    }

    default List<EchoDiagnostic> diagnostics() {
        return currentReport().allDiagnostics();
    }

    default boolean localOnly() {
        return true;
    }
}
