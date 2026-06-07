package com.knoxhack.echo.modulegraph;

import com.knoxhack.echo.platformcore.EchoModuleId;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class EchoDependencyCycleDetector {
    public List<List<EchoModuleId>> detect(Collection<EchoModuleGraphEdge> edges) {
        Map<EchoModuleId, List<EchoModuleId>> adjacency = new HashMap<>();
        for (EchoModuleGraphEdge edge : edges == null ? List.<EchoModuleGraphEdge>of() : edges) {
            if (edge.kind() == EchoModuleGraphEdgeKind.REQUIRES && edge.toModule() != null && edge.present()) {
                adjacency.computeIfAbsent(edge.fromModule(), ignored -> new ArrayList<>()).add(edge.toModule());
            }
        }
        adjacency.values().forEach(list -> list.sort(java.util.Comparator.comparing(EchoModuleId::value)));
        List<List<EchoModuleId>> cycles = new ArrayList<>();
        Set<EchoModuleId> visited = new HashSet<>();
        Set<EchoModuleId> visiting = new HashSet<>();
        ArrayDeque<EchoModuleId> stack = new ArrayDeque<>();
        adjacency.keySet().stream().sorted(java.util.Comparator.comparing(EchoModuleId::value))
                .forEach(node -> visit(node, adjacency, visited, visiting, stack, cycles));
        return cycles.stream()
                .distinct()
                .sorted(java.util.Comparator.comparing(cycle -> cycle.stream().map(EchoModuleId::value).collect(java.util.stream.Collectors.joining(">"))))
                .toList();
    }

    private void visit(
            EchoModuleId node,
            Map<EchoModuleId, List<EchoModuleId>> adjacency,
            Set<EchoModuleId> visited,
            Set<EchoModuleId> visiting,
            ArrayDeque<EchoModuleId> stack,
            List<List<EchoModuleId>> cycles
    ) {
        if (visited.contains(node)) {
            return;
        }
        if (!visiting.add(node)) {
            List<EchoModuleId> cycle = new ArrayList<>();
            boolean collect = false;
            for (EchoModuleId entry : stack) {
                if (entry.equals(node)) {
                    collect = true;
                }
                if (collect) {
                    cycle.add(entry);
                }
            }
            cycle.add(node);
            cycles.add(List.copyOf(cycle));
            return;
        }
        stack.addLast(node);
        for (EchoModuleId next : adjacency.getOrDefault(node, List.of())) {
            visit(next, adjacency, visited, visiting, stack, cycles);
        }
        stack.removeLast();
        visiting.remove(node);
        visited.add(node);
    }
}
