package com.knoxhack.echo.modulegraph;

import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.platformcore.EchoModuleRole;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public record EchoModuleRoleIndex(Map<EchoModuleRole, List<EchoModuleId>> modulesByRole) {
    public EchoModuleRoleIndex {
        modulesByRole = modulesByRole == null ? Map.of() : modulesByRole.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> List.copyOf(entry.getValue())));
    }

    public List<EchoModuleId> modulesFor(EchoModuleRole role) {
        return modulesByRole.getOrDefault(role, List.of());
    }

    public static EchoModuleRoleIndex fromModules(Collection<EchoScannedModule> modules) {
        Collection<EchoScannedModule> safeModules = modules == null ? List.of() : modules;
        Map<EchoModuleRole, List<EchoModuleId>> grouped = safeModules.stream()
                .filter(Objects::nonNull)
                .map(EchoModuleGraphNode::fromScannedModule)
                .sorted(Comparator.comparing(node -> node.id().value()))
                .collect(Collectors.groupingBy(
                        EchoModuleGraphNode::role,
                        Collectors.mapping(EchoModuleGraphNode::id, Collectors.toUnmodifiableList())
                ));
        return new EchoModuleRoleIndex(grouped);
    }
}
