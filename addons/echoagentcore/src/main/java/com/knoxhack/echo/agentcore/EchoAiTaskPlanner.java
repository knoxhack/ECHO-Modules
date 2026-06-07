package com.knoxhack.echo.agentcore;

import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;

public interface EchoAiTaskPlanner {
    List<EchoAiTask> planTasks(EchoAiProjectGraph graph, List<EchoDiagnostic> diagnostics);
}
