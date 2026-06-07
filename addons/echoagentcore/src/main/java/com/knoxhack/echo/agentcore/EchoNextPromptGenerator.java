package com.knoxhack.echo.agentcore;

public interface EchoNextPromptGenerator {
    EchoAiNextPhasePrompt generateNextPhasePrompt(
            EchoAiTaskQueue queue,
            EchoAiPromptBundle promptBundle,
            EchoAiRunReport runReport
    );
}
