package com.knoxhack.echo.agentcore;

import java.util.List;
import java.util.Map;

public record EchoAiTaskQueue(
        String id,
        String packId,
        String addonSet,
        EchoAiTaskStatus status,
        List<EchoAiTask> tasks,
        Map<String, Integer> tasksBySource,
        Map<String, Integer> tasksByLane,
        Map<String, Integer> tasksByPriority,
        List<String> reportInputs,
        List<String> safetyRules,
        long generatedAtEpochMillis
) {
    public EchoAiTaskQueue {
        id = AgentContractGuards.requireText(id, "AI task queue id");
        packId = AgentContractGuards.optionalText(packId);
        addonSet = AgentContractGuards.optionalText(addonSet);
        status = status == null ? EchoAiTaskStatus.PROPOSED : status;
        tasks = AgentContractGuards.immutableList(tasks);
        tasksBySource = AgentContractGuards.immutableMap(tasksBySource);
        tasksByLane = AgentContractGuards.immutableMap(tasksByLane);
        tasksByPriority = AgentContractGuards.immutableMap(tasksByPriority);
        reportInputs = AgentContractGuards.immutableList(reportInputs);
        safetyRules = AgentContractGuards.immutableList(safetyRules);
        generatedAtEpochMillis = AgentContractGuards.nonNegativeLong(generatedAtEpochMillis, "generated at epoch millis");
    }

    public boolean hasReadyTasks() {
        return tasks.stream().anyMatch(task -> !task.status().terminal());
    }
}
