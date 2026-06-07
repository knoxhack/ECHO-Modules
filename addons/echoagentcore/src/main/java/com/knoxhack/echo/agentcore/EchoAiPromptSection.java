package com.knoxhack.echo.agentcore;

import java.util.List;

public record EchoAiPromptSection(
        String id,
        String title,
        String content,
        int order,
        boolean required,
        boolean redacted,
        List<String> sourceReferences
) {
    public EchoAiPromptSection {
        id = AgentContractGuards.requireText(id, "prompt section id");
        title = AgentContractGuards.requireText(title, "prompt section title");
        content = AgentContractGuards.optionalText(content);
        order = AgentContractGuards.nonNegative(order, "prompt section order");
        sourceReferences = AgentContractGuards.immutableList(sourceReferences);
    }
}
