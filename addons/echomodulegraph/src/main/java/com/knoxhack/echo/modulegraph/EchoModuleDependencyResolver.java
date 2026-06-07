package com.knoxhack.echo.modulegraph;

import com.knoxhack.echo.metadatacore.EchoMetadataDependency;
import com.knoxhack.echo.platformcore.EchoModuleId;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class EchoModuleDependencyResolver implements EchoDependencyResolver {
    @Override
    public EchoLoadPlan resolve(Collection<EchoScannedModule> modules) {
        Collection<EchoScannedModule> safeModules = modules == null ? List.of() : modules;
        EchoModuleGraphIndex index = EchoModuleGraphIndex.fromModules(safeModules);
        List<EchoModuleGraphEdge> edges = dependencyEdges(safeModules, index);
        List<EchoModuleGraphIssue> issues = dependencyIssues(edges);
        List<List<EchoModuleId>> cycles = new EchoDependencyCycleDetector().detect(edges);
        for (List<EchoModuleId> cycle : cycles) {
            issues.add(new EchoModuleGraphIssue(
                    EchoModuleGraphIssueKind.CIRCULAR_DEPENDENCY,
                    null,
                    cycle.isEmpty() ? null : cycle.get(0),
                    cycle.size() > 1 ? cycle.get(1) : null,
                    null,
                    null,
                    "",
                    "Required dependency cycle detected: " + cycle.stream().map(EchoModuleId::value).collect(Collectors.joining(" -> ")),
                    "",
                    "Break the required dependency cycle or make one integration optional.",
                    false,
                    List.of("docs/echo/validation/ECHO_MODULE_GRAPH.md")
            ));
        }
        Map<EchoModuleId, List<EchoModuleId>> requiredEdges = edges.stream()
                .filter(edge -> edge.kind() == EchoModuleGraphEdgeKind.REQUIRES && edge.toModule() != null && edge.present())
                .collect(Collectors.groupingBy(
                        EchoModuleGraphEdge::fromModule,
                        Collectors.mapping(EchoModuleGraphEdge::toModule, Collectors.toList())
                ));
        requiredEdges.replaceAll((ignored, value) -> value.stream().sorted(Comparator.comparing(EchoModuleId::value)).toList());
        Set<EchoModuleId> blockedModules = edges.stream()
                .filter(edge -> edge.required() && !edge.present())
                .map(EchoModuleGraphEdge::fromModule)
                .collect(Collectors.toUnmodifiableSet());
        Set<EchoModuleId> degradedModules = edges.stream()
                .filter(edge -> !edge.required() && !edge.present())
                .map(EchoModuleGraphEdge::fromModule)
                .collect(Collectors.toUnmodifiableSet());
        return new EchoLoadPlan(loadOrder(safeModules, requiredEdges), requiredEdges, blockedModules, degradedModules, issues);
    }

    public List<EchoModuleGraphEdge> dependencyEdges(Collection<EchoScannedModule> modules, EchoModuleGraphIndex index) {
        Collection<EchoScannedModule> safeModules = modules == null ? List.of() : modules;
        EchoModuleGraphIndex safeIndex = index == null ? EchoModuleGraphIndex.fromModules(safeModules) : index;
        List<EchoModuleGraphEdge> edges = new ArrayList<>();
        for (EchoScannedModule module : safeModules.stream().sorted(Comparator.comparing(item -> item.moduleId().value())).toList()) {
            for (EchoMetadataDependency dependency : module.requiredDependencies()) {
                boolean present = safeIndex.contains(dependency.moduleId());
                edges.add(EchoModuleDependencyEdge.from(module.moduleId(), dependency, present, present ? "present" : "missing_required").toGraphEdge());
            }
            for (EchoMetadataDependency dependency : module.optionalDependencies()) {
                boolean present = safeIndex.contains(dependency.moduleId());
                edges.add(EchoOptionalDependencyEdge.from(module.moduleId(), dependency, present, present ? "present" : "missing_optional").toGraphEdge());
            }
        }
        return edges.stream()
                .sorted(Comparator.comparing((EchoModuleGraphEdge edge) -> edge.fromModule().value())
                        .thenComparing(edge -> edge.kind().serializedName())
                        .thenComparing(edge -> edge.toModule() == null ? "" : edge.toModule().value()))
                .toList();
    }

    private List<EchoModuleGraphIssue> dependencyIssues(List<EchoModuleGraphEdge> edges) {
        List<EchoModuleGraphIssue> issues = new ArrayList<>();
        for (EchoModuleGraphEdge edge : edges) {
            if (edge.kind() == EchoModuleGraphEdgeKind.REQUIRES && !edge.present()) {
                issues.add(new EchoModuleGraphIssue(
                        EchoModuleGraphIssueKind.MISSING_DEPENDENCY,
                        null,
                        edge.fromModule(),
                        edge.toModule(),
                        null,
                        null,
                        "",
                        "Required module is missing: " + edge.toModule().value(),
                        "",
                        "Add the required module or remove the hard dependency from metadata.",
                        false,
                        List.of("docs/echo/validation/ECHO_MODULE_GRAPH.md")
                ));
            } else if (edge.kind() == EchoModuleGraphEdgeKind.OPTIONAL && edge.present()) {
                issues.add(new EchoModuleGraphIssue(
                        EchoModuleGraphIssueKind.OPTIONAL_DEPENDENCY_PRESENT,
                        null,
                        edge.fromModule(),
                        edge.toModule(),
                        null,
                        null,
                        "",
                        "Optional module is present: " + edge.toModule().value(),
                        "",
                        "",
                        false,
                        List.of("docs/echo/validation/ECHO_MODULE_GRAPH.md")
                ));
            } else if (edge.kind() == EchoModuleGraphEdgeKind.OPTIONAL) {
                issues.add(new EchoModuleGraphIssue(
                        EchoModuleGraphIssueKind.OPTIONAL_DEPENDENCY_MISSING,
                        null,
                        edge.fromModule(),
                        edge.toModule(),
                        null,
                        null,
                        "",
                        "Optional module is not present: " + edge.toModule().value(),
                        "",
                        "No action required unless this pack expects the optional integration.",
                        false,
                        List.of("docs/echo/validation/ECHO_MODULE_GRAPH.md")
                ));
            }
        }
        return issues;
    }

    private List<EchoModuleId> loadOrder(Collection<EchoScannedModule> modules, Map<EchoModuleId, List<EchoModuleId>> dependencies) {
        Set<EchoModuleId> allModules = modules.stream().map(EchoScannedModule::moduleId).collect(Collectors.toCollection(HashSet::new));
        Set<EchoModuleId> visited = new HashSet<>();
        Set<EchoModuleId> visiting = new HashSet<>();
        ArrayDeque<EchoModuleId> order = new ArrayDeque<>();
        allModules.stream().sorted(Comparator.comparing(EchoModuleId::value))
                .forEach(moduleId -> visit(moduleId, dependencies, allModules, visited, visiting, order));
        return List.copyOf(order);
    }

    private void visit(
            EchoModuleId moduleId,
            Map<EchoModuleId, List<EchoModuleId>> dependencies,
            Set<EchoModuleId> allModules,
            Set<EchoModuleId> visited,
            Set<EchoModuleId> visiting,
            ArrayDeque<EchoModuleId> order
    ) {
        if (visited.contains(moduleId) || !visiting.add(moduleId)) {
            return;
        }
        for (EchoModuleId dependency : dependencies.getOrDefault(moduleId, List.of())) {
            if (allModules.contains(dependency)) {
                visit(dependency, dependencies, allModules, visited, visiting, order);
            }
        }
        visiting.remove(moduleId);
        visited.add(moduleId);
        order.addLast(moduleId);
    }
}
