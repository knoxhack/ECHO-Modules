package com.knoxhack.echo.progressioncore;

import com.knoxhack.echo.platformcore.EchoGameModeId;
import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.platformcore.EchoPackId;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public record EchoUnlockGraph(
        EchoProgressionId id,
        String name,
        EchoModuleId ownerModule,
        EchoPackId packId,
        Set<EchoGameModeId> gameModes,
        List<EchoUnlockNode> nodes,
        List<EchoUnlockEdge> edges,
        List<EchoProgressionGate> gates,
        List<EchoObjectiveDefinition> objectives,
        List<EchoDiagnostic> diagnostics,
        Map<String, String> attributes
) {
    public EchoUnlockGraph {
        Objects.requireNonNull(id, "id");
        name = ProgressionContractGuards.requireText(name, "unlock graph name");
        gameModes = ProgressionContractGuards.immutableSet(gameModes);
        nodes = ProgressionContractGuards.immutableList(nodes);
        edges = ProgressionContractGuards.immutableList(edges);
        gates = ProgressionContractGuards.immutableList(gates);
        objectives = ProgressionContractGuards.immutableList(objectives);
        diagnostics = ProgressionContractGuards.immutableList(diagnostics);
        attributes = ProgressionContractGuards.immutableMap(attributes);
    }

    public Map<EchoUnlockNodeId, EchoUnlockNode> nodesById() {
        return nodes.stream().collect(Collectors.toUnmodifiableMap(EchoUnlockNode::id, Function.identity()));
    }

    public List<EchoUnlockNode> rootNodes() {
        Set<EchoUnlockNodeId> targets = edges.stream()
                .filter(EchoUnlockEdge::blocksWhenMissing)
                .map(EchoUnlockEdge::to)
                .collect(Collectors.toUnmodifiableSet());
        return nodes.stream()
                .filter(node -> !targets.contains(node.id()))
                .toList();
    }

    public boolean blocking() {
        return diagnostics.stream().anyMatch(EchoDiagnostic::blocking)
                || nodes.stream().anyMatch(EchoUnlockNode::blocksProgression)
                || gates.stream().anyMatch(EchoProgressionGate::blocking)
                || objectives.stream().anyMatch(EchoObjectiveDefinition::blocking);
    }
}
