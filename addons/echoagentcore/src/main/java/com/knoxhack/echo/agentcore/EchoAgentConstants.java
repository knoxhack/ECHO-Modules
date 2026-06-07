package com.knoxhack.echo.agentcore;

import java.util.Arrays;
import java.util.List;

public final class EchoAgentConstants {
    public static final String MOD_ID = "echoagentcore";
    public static final String AI_TASK_SCHEMA_ID = "echo.ai_task";
    public static final String PROMPT_BUNDLE_SCHEMA_ID = "echo.prompt_bundle";
    public static final String SCHEMA_VERSION_1 = "1.0.0";

    public static final List<EchoAiAgentLane> AGENT_LANES = List.copyOf(Arrays.asList(EchoAiAgentLane.values()));
    public static final List<EchoAiTaskSource> TASK_SOURCES = List.copyOf(Arrays.asList(EchoAiTaskSource.values()));
    public static final List<EchoAiTaskStatus> TASK_STATUSES = List.copyOf(Arrays.asList(EchoAiTaskStatus.values()));
    public static final List<EchoAiTaskPriority> TASK_PRIORITIES = List.copyOf(Arrays.asList(EchoAiTaskPriority.values()));
    public static final List<EchoAiAcceptanceCriterionType> ACCEPTANCE_CRITERIA = List.copyOf(Arrays.asList(EchoAiAcceptanceCriterionType.values()));
    public static final List<EchoAiCommandRisk> SAFE_COMMAND_RISKS = List.copyOf(Arrays.asList(EchoAiCommandRisk.values()));

    private EchoAgentConstants() {
    }
}
