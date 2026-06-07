package com.knoxhack.echo.modulegraph;

import com.knoxhack.echo.platformcore.EchoModuleId;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public record EchoModuleGraphIndex(Map<EchoModuleId, EchoModuleGraphNode> nodesById) {
    public EchoModuleGraphIndex {
        nodesById = ModuleGraphContractGuards.immutableMap(nodesById);
    }

    public Optional<EchoModuleGraphNode> find(EchoModuleId moduleId) {
        return Optional.ofNullable(nodesById.get(moduleId));
    }

    public boolean contains(EchoModuleId moduleId) {
        return nodesById.containsKey(moduleId);
    }

    public static EchoModuleGraphIndex fromModules(Collection<EchoScannedModule> modules) {
        Collection<EchoScannedModule> safeModules = modules == null ? java.util.List.of() : modules;
        return new EchoModuleGraphIndex(safeModules.stream()
                .filter(Objects::nonNull)
                .map(EchoModuleGraphNode::fromScannedModule)
                .sorted(Comparator.comparing(node -> node.id().value()))
                .collect(Collectors.toMap(EchoModuleGraphNode::id, Function.identity(), (left, right) -> left)));
    }
}
