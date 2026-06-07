package com.knoxhack.echo.modulegraph;

import java.util.Collection;

public interface EchoDependencyResolver {
    EchoLoadPlan resolve(Collection<EchoScannedModule> modules);

    default EchoLoadPlan resolve(EchoModuleGraph graph) {
        return resolve(graph == null ? java.util.List.of() : graph.modules());
    }
}
