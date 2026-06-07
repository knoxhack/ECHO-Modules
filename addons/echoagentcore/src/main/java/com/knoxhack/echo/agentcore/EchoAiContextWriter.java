package com.knoxhack.echo.agentcore;

public interface EchoAiContextWriter {
    String writePromptBundle(EchoAiPromptBundle bundle);

    String writeProjectGraph(EchoAiProjectGraph graph);

    String writeRunReport(EchoAiRunReport report);
}
