package com.knoxhack.echo.agentcore;

import com.knoxhack.echo.schemacore.EchoSchemaDocumentKind;
import com.knoxhack.echo.schemacore.EchoSchemaId;
import com.knoxhack.echo.schemacore.EchoSchemaVersion;

import java.util.List;

public record EchoAiPromptBundle(
        String id,
        EchoSchemaId schema,
        EchoSchemaVersion schemaVersion,
        EchoSchemaDocumentKind documentKind,
        String title,
        String purpose,
        EchoAiProjectGraph projectGraph,
        List<EchoAiTask> tasks,
        List<EchoAiPromptSection> sections,
        List<EchoAiProtectedFileRule> protectedFileRules,
        List<EchoAiSafeCommand> safeCommands,
        String generatedBy,
        long createdAtEpochMillis
) {
    public EchoAiPromptBundle {
        id = AgentContractGuards.requireText(id, "prompt bundle id");
        schema = schema == null ? EchoSchemaId.of(EchoAgentConstants.PROMPT_BUNDLE_SCHEMA_ID) : schema;
        schemaVersion = schemaVersion == null ? EchoSchemaVersion.of(EchoAgentConstants.SCHEMA_VERSION_1) : schemaVersion;
        documentKind = documentKind == null ? EchoSchemaDocumentKind.ECHO_PROMPT_BUNDLE : documentKind;
        title = AgentContractGuards.requireText(title, "prompt bundle title");
        purpose = AgentContractGuards.optionalText(purpose);
        projectGraph = projectGraph == null ? EchoAiProjectGraph.empty(id + ".project_graph") : projectGraph;
        tasks = AgentContractGuards.immutableList(tasks);
        sections = AgentContractGuards.immutableList(sections);
        protectedFileRules = AgentContractGuards.immutableList(protectedFileRules);
        safeCommands = AgentContractGuards.immutableList(safeCommands);
        generatedBy = AgentContractGuards.optionalText(generatedBy);
        createdAtEpochMillis = AgentContractGuards.nonNegativeLong(createdAtEpochMillis, "created at epoch millis");
    }

    public boolean requiresHumanReview() {
        return tasks.stream().anyMatch(EchoAiTask::requiresHumanReview)
                || protectedFileRules.stream().anyMatch(EchoAiProtectedFileRule::requiresHumanReview)
                || safeCommands.stream().anyMatch(EchoAiSafeCommand::requiresConfirmation);
    }
}
