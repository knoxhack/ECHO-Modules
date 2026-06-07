package com.knoxhack.echo.modulegraph;

import com.knoxhack.echo.platformcore.EchoModuleId;
import com.knoxhack.echo.validationcore.EchoDiagnostic;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public record EchoModuleGraph(
        List<EchoScannedModule> modules,
        List<EchoModuleGraphNode> nodes,
        List<EchoModuleGraphEdge> edges,
        EchoModuleGraphIndex index,
        EchoModuleRoleIndex roleIndex,
        EchoFeatureGraph featureGraph,
        List<EchoRoleConflict> roleConflicts,
        EchoLoadPlan loadPlan,
        List<EchoModuleGraphIssue> issues
) {
    public EchoModuleGraph {
        modules = ModuleGraphContractGuards.immutableList(modules);
        nodes = ModuleGraphContractGuards.immutableList(nodes);
        if (nodes.isEmpty()) {
            nodes = modules.stream()
                    .map(EchoModuleGraphNode::fromScannedModule)
                    .sorted(java.util.Comparator.comparing(node -> node.id().value()))
                    .toList();
        }
        index = index == null ? EchoModuleGraphIndex.fromModules(modules) : index;
        roleIndex = roleIndex == null ? EchoModuleRoleIndex.fromModules(modules) : roleIndex;
        edges = ModuleGraphContractGuards.immutableList(edges);
        if (edges.isEmpty()) {
            edges = new EchoModuleDependencyResolver().dependencyEdges(modules, index);
        }
        featureGraph = featureGraph == null ? EchoFeatureGraph.fromModules(modules) : featureGraph;
        roleConflicts = ModuleGraphContractGuards.immutableList(roleConflicts);
        if (roleConflicts.isEmpty()) {
            roleConflicts = new EchoRoleConflictDetector().detect(modules);
        }
        loadPlan = loadPlan == null || (loadPlan.loadOrder().isEmpty() && !modules.isEmpty())
                ? new EchoModuleDependencyResolver().resolve(modules)
                : loadPlan;
        List<EchoModuleGraphIssue> merged = new ArrayList<>();
        if (issues != null) {
            merged.addAll(issues);
        }
        merged.addAll(featureGraph.issues());
        roleConflicts.stream().map(EchoRoleConflict::toIssue).forEach(merged::add);
        merged.addAll(loadPlan.issues());
        issues = List.copyOf(merged);
    }

    public EchoModuleGraph(
            List<EchoScannedModule> modules,
            EchoFeatureGraph featureGraph,
            List<EchoRoleConflict> roleConflicts,
            EchoLoadPlan loadPlan,
            List<EchoModuleGraphIssue> issues
    ) {
        this(modules, List.of(), List.of(), null, null, featureGraph, roleConflicts, loadPlan, issues);
    }

    public Map<EchoModuleId, EchoScannedModule> modulesById() {
        return modules.stream().collect(Collectors.toMap(EchoScannedModule::moduleId, Function.identity(), (left, right) -> left));
    }

    public Set<EchoModuleId> duplicateModuleIds() {
        Set<EchoModuleId> seen = new HashSet<>();
        Set<EchoModuleId> duplicates = new HashSet<>();
        for (EchoScannedModule module : modules) {
            if (!seen.add(module.moduleId())) {
                duplicates.add(module.moduleId());
            }
        }
        return Set.copyOf(duplicates);
    }

    public List<EchoScannedModule> modulesMissingManifest() {
        return modules.stream().filter(module -> !module.manifestPresent()).toList();
    }

    public List<EchoDiagnostic> diagnostics() {
        return issues.stream().map(EchoModuleGraphIssue::toDiagnostic).toList();
    }

    public boolean valid() {
        return diagnostics().stream().noneMatch(EchoDiagnostic::blocking);
    }

    public static EchoModuleGraph of(List<EchoScannedModule> modules) {
        List<EchoScannedModule> safeModules = modules == null ? List.of() : modules;
        List<EchoModuleGraphIssue> issues = new ArrayList<>();
        Set<EchoModuleId> duplicates = new HashSet<>();
        Set<EchoModuleId> seen = new HashSet<>();
        for (EchoScannedModule module : safeModules) {
            if (!seen.add(module.moduleId())) {
                duplicates.add(module.moduleId());
            }
            issues.addAll(module.issues());
            if (!module.manifestPresent()) {
                issues.add(EchoModuleGraphIssue.of(
                        EchoModuleGraphIssueKind.MISSING_MANIFEST,
                        module.moduleId(),
                        "Module has no optional ECHO manifest; fallback metadata should be used."
                ));
            } else if (!module.manifestValid()) {
                issues.add(EchoModuleGraphIssue.of(
                        EchoModuleGraphIssueKind.INVALID_MANIFEST,
                        module.moduleId(),
                        "Module manifest was present but invalid."
                ));
            }
        }
        for (EchoModuleId duplicate : duplicates) {
            issues.add(EchoModuleGraphIssue.of(
                    EchoModuleGraphIssueKind.DUPLICATE_MODULE_ID,
                    duplicate,
                    "Multiple scanned modules declare the same module id."
            ));
        }
        return new EchoModuleGraph(safeModules, List.of(), List.of(), null, null, EchoFeatureGraph.fromModules(safeModules), List.of(), null, issues);
    }
}
