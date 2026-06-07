package com.knoxhack.echo.modulegraph;

import java.util.Collection;
import java.util.List;

public interface EchoModuleScanner {
    EchoModuleGraph scan(Collection<EchoScannedModule> modules);

    default EchoModuleGraph scan(EchoScannedModule... modules) {
        return scan(modules == null ? List.of() : List.of(modules));
    }
}
