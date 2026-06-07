package com.knoxhack.echo.agentcore;

public interface EchoPromptContextCollector {
    EchoAiPromptBundle collectPromptBundle(EchoAiTaskQueue queue);
}
